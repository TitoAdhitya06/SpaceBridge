package com.example.spacebridgeapp.feature.settings

import android.app.Activity
import android.content.Intent
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.spacebridgeapp.core.data.auth.AuthRepository
import com.example.spacebridgeapp.core.data.cache.FileCacheManager
import com.example.spacebridgeapp.core.data.drive.DriveAuthManager
import com.example.spacebridgeapp.core.data.drive.DriveAuthResult
import com.example.spacebridgeapp.core.data.network.NetworkObserver
import com.example.spacebridgeapp.core.data.prefs.PreferencesRepository
import com.example.spacebridgeapp.core.model.AppUser
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface SettingsEvent {
    data class ShowSnackbar(
        val message: String,
    ) : SettingsEvent

    data class LaunchConsent(
        val intentSenderRequest: IntentSenderRequest,
    ) : SettingsEvent
}

enum class ThemeOption(val modeValue: String?, val label: String) {
    SYSTEM(null, "Ikuti Sistem"),
    LIGHT("light", "Terang"),
    DARK("dark", "Gelap"),
    ;

    companion object {
        fun fromModeValue(mode: String?): ThemeOption =
            when (mode) {
                "light" -> LIGHT
                "dark" -> DARK
                else -> SYSTEM
            }
    }
}

data class SettingsUiState(
    val currentUser: AppUser? = null,
    val isDriveAuthorized: Boolean = false,
    val isWifiOnlyUpload: Boolean = true,
    val isConnected: Boolean = true,
    val cacheSizeBytes: Long = 0L,
    val maxCacheSizeBytes: Long = 300 * 1024 * 1024L,
    val isClearingCache: Boolean = false,
    val isCacheClearedSuccess: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val deleteAccountError: String? = null,
    val themeOption: ThemeOption = ThemeOption.SYSTEM,
    val appVersion: String = "v3.0 (Build 30)",
    val isLoading: Boolean = false,
    val isSignedOut: Boolean = false,
)

class SettingsViewModel(
    private val authRepository: AuthRepository,
    private val preferencesRepository: PreferencesRepository,
    private val fileCacheManager: FileCacheManager,
    private val networkObserver: NetworkObserver,
    private val driveAuthManager: DriveAuthManager? = null,
) : ViewModel() {

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events: Flow<SettingsEvent> = _events.receiveAsFlow()

    private val _localState = MutableStateFlow(LocalSettingsState())

    private data class LocalSettingsState(
        val cacheSizeBytes: Long = 0L,
        val isClearingCache: Boolean = false,
        val isCacheClearedSuccess: Boolean = false,
        val isDeletingAccount: Boolean = false,
        val deleteAccountError: String? = null,
        val isSignedOut: Boolean = false,
    )

    private val userAndDriveFlow =
        combine(
            authRepository.currentUser,
            preferencesRepository.isDriveAuthorized,
        ) { user, isDriveAuthorized ->
            Pair(user, isDriveAuthorized)
        }

    private val prefsFlow =
        combine(
            preferencesRepository.isWifiOnlyUpload,
            preferencesRepository.themeMode,
        ) { wifiOnly, themeMode ->
            Pair(wifiOnly, ThemeOption.fromModeValue(themeMode))
        }

    val uiState: StateFlow<SettingsUiState> =
        combine(
            userAndDriveFlow,
            prefsFlow,
            networkObserver.isConnected,
            _localState,
        ) { userAndDrive, prefs, isConnected, local ->
            SettingsUiState(
                currentUser = userAndDrive.first,
                isDriveAuthorized = userAndDrive.second,
                isWifiOnlyUpload = prefs.first,
                themeOption = prefs.second,
                isConnected = isConnected,
                cacheSizeBytes = local.cacheSizeBytes,
                isClearingCache = local.isClearingCache,
                isCacheClearedSuccess = local.isCacheClearedSuccess,
                isDeletingAccount = local.isDeletingAccount,
                deleteAccountError = local.deleteAccountError,
                isSignedOut = local.isSignedOut,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = SettingsUiState(),
        )

    init {
        refreshCacheSize()
    }

    fun onToggleWifiOnly(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setWifiOnlyUpload(enabled)
        }
    }

    fun onSelectTheme(option: ThemeOption) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(option.modeValue)
        }
    }

    fun refreshCacheSize() {
        viewModelScope.launch {
            val size = fileCacheManager.getCacheSizeBytes()
            _localState.update { it.copy(cacheSizeBytes = size) }
        }
    }

    fun onClearCache() {
        if (_localState.value.isClearingCache) return

        viewModelScope.launch {
            _localState.update { it.copy(isClearingCache = true, isCacheClearedSuccess = false) }
            fileCacheManager.clearCache()
            val newSize = fileCacheManager.getCacheSizeBytes()
            _localState.update {
                it.copy(
                    isClearingCache = false,
                    isCacheClearedSuccess = true,
                    cacheSizeBytes = newSize,
                )
            }
            delay(2000L)
            _localState.update { it.copy(isCacheClearedSuccess = false) }
        }
    }

    fun onSignOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _localState.update { it.copy(isSignedOut = true) }
        }
    }

    fun onDeleteAccount(
        activity: Activity,
        trashDriveFolder: Boolean,
    ) {
        if (_localState.value.isDeletingAccount) return

        viewModelScope.launch {
            _localState.update { it.copy(isDeletingAccount = true, deleteAccountError = null) }
            val result = authRepository.deleteAccount(activity, trashDriveFolder)
            if (result.isSuccess) {
                _localState.update {
                    it.copy(
                        isDeletingAccount = false,
                        isSignedOut = true,
                    )
                }
            } else {
                val error = result.exceptionOrNull()?.message ?: "Gagal menghapus akun"
                _localState.update {
                    it.copy(
                        isDeletingAccount = false,
                        deleteAccountError = error,
                    )
                }
            }
        }
    }

    fun clearDeleteAccountError() {
        _localState.update { it.copy(deleteAccountError = null) }
    }

    fun connectDrive(activity: Activity? = null) {
        val manager = driveAuthManager ?: return
        viewModelScope.launch {
            val result = if (activity != null) manager.authorize(activity) else manager.authorize()
            when (result) {
                is DriveAuthResult.Granted -> {
                    _events.send(SettingsEvent.ShowSnackbar("Akses Google Drive berhasil terhubung"))
                }
                is DriveAuthResult.NeedsConsent -> {
                    val request = IntentSenderRequest.Builder(result.pendingIntent).build()
                    _events.send(SettingsEvent.LaunchConsent(request))
                }
                is DriveAuthResult.Failed -> {
                    _events.send(SettingsEvent.ShowSnackbar("Gagal menghubungkan Google Drive: ${result.reason}"))
                }
            }
        }
    }

    fun handleConsentResult(
        resultCode: Int,
        data: Intent?,
    ) {
        val manager = driveAuthManager ?: return
        if (resultCode != Activity.RESULT_OK) {
            viewModelScope.launch {
                _events.send(SettingsEvent.ShowSnackbar("Izin akses Google Drive dibatalkan"))
            }
            return
        }
        viewModelScope.launch {
            when (val result = manager.handleConsentResult(data)) {
                is DriveAuthResult.Granted -> {
                    _events.send(SettingsEvent.ShowSnackbar("Akses Google Drive berhasil terhubung"))
                }
                is DriveAuthResult.NeedsConsent -> {
                    val request = IntentSenderRequest.Builder(result.pendingIntent).build()
                    _events.send(SettingsEvent.LaunchConsent(request))
                }
                is DriveAuthResult.Failed -> {
                    _events.send(SettingsEvent.ShowSnackbar("Gagal menghubungkan Google Drive: ${result.reason}"))
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            authRepository: AuthRepository,
            preferencesRepository: PreferencesRepository,
            fileCacheManager: FileCacheManager,
            networkObserver: NetworkObserver,
            driveAuthManager: DriveAuthManager? = null,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SettingsViewModel(
                        authRepository = authRepository,
                        preferencesRepository = preferencesRepository,
                        fileCacheManager = fileCacheManager,
                        networkObserver = networkObserver,
                        driveAuthManager = driveAuthManager,
                    ) as T
            }
    }
}
