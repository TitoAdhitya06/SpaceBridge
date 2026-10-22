package com.example.spacebridgeapp.feature.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.spacebridgeapp.core.data.auth.AuthRepository
import com.example.spacebridgeapp.core.data.drive.DriveAuthManager
import com.example.spacebridgeapp.core.data.drive.DriveAuthResult
import com.example.spacebridgeapp.core.data.prefs.PreferencesRepository
import com.example.spacebridgeapp.core.model.AppUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val currentUser: AppUser? = null,
    val isLoading: Boolean = false,
    val isDriveConnecting: Boolean = false,
    val hasSeenDriveOnboarding: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface AuthEvent {
    data class ShowSnackbar(
        val message: String,
    ) : AuthEvent

    data object NavigateToDriveOnboarding : AuthEvent

    data object NavigateToHome : AuthEvent

    data class LaunchConsent(
        val intentSenderRequest: IntentSenderRequest,
    ) : AuthEvent
}

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val preferencesRepository: PreferencesRepository,
    private val driveAuthManager: DriveAuthManager? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _events = Channel<AuthEvent>(Channel.BUFFERED)
    val events: Flow<AuthEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.update { it.copy(currentUser = user) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.hasSeenDriveOnboarding.collect { seen ->
                _uiState.update { it.copy(hasSeenDriveOnboarding = seen) }
            }
        }
    }

    fun signIn(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result =
                if (context is Activity) {
                    authRepository.signIn(context)
                } else {
                    authRepository.signIn(context)
                }

            result.fold(
                onSuccess = {
                    val hasSeen = preferencesRepository.hasSeenDriveOnboarding.first()
                    if (hasSeen) {
                        _events.send(AuthEvent.NavigateToHome)
                    } else {
                        _events.send(AuthEvent.NavigateToDriveOnboarding)
                    }
                },
                onFailure = { error ->
                    if (error is CancellationException) {
                        _uiState.update { it.copy(errorMessage = null) }
                    } else {
                        val message = error.message ?: "Gagal masuk. Periksa koneksi internet Anda."
                        _uiState.update { it.copy(errorMessage = message) }
                        _events.send(AuthEvent.ShowSnackbar(message))
                    }
                },
            )
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun completeDriveOnboarding(activity: Activity? = null) {
        val manager = driveAuthManager
        if (manager == null) {
            viewModelScope.launch {
                preferencesRepository.setSeenDriveOnboarding(true)
                _events.send(AuthEvent.NavigateToHome)
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isDriveConnecting = true) }
            val result = if (activity != null) manager.authorize(activity) else manager.authorize()
            when (result) {
                is DriveAuthResult.Granted -> {
                    preferencesRepository.setSeenDriveOnboarding(true)
                    _uiState.update { it.copy(isDriveConnecting = false) }
                    _events.send(AuthEvent.NavigateToHome)
                }
                is DriveAuthResult.NeedsConsent -> {
                    val request = IntentSenderRequest.Builder(result.pendingIntent).build()
                    _uiState.update { it.copy(isDriveConnecting = false) }
                    _events.send(AuthEvent.LaunchConsent(request))
                }
                is DriveAuthResult.Failed -> {
                    _uiState.update { it.copy(isDriveConnecting = false, errorMessage = result.reason) }
                    _events.send(AuthEvent.ShowSnackbar(result.reason))
                }
            }
        }
    }

    fun handleConsentResult(
        resultCode: Int,
        data: Intent?,
    ) {
        val manager = driveAuthManager
        viewModelScope.launch {
            preferencesRepository.setSeenDriveOnboarding(true)
            if (resultCode == Activity.RESULT_OK && manager != null) {
                manager.handleConsentResult(data)
            }
            _events.send(AuthEvent.NavigateToHome)
        }
    }

    fun skipDriveOnboarding() {
        viewModelScope.launch {
            preferencesRepository.setSeenDriveOnboarding(true)
            _events.send(AuthEvent.NavigateToHome)
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    companion object {
        fun provideFactory(
            authRepository: AuthRepository,
            preferencesRepository: PreferencesRepository,
            driveAuthManager: DriveAuthManager? = null,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AuthViewModel(
                        authRepository = authRepository,
                        preferencesRepository = preferencesRepository,
                        driveAuthManager = driveAuthManager,
                    ) as T
            }
    }
}
