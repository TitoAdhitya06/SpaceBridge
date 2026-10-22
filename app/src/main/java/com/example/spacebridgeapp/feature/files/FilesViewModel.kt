package com.example.spacebridgeapp.feature.files

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.spacebridgeapp.core.data.auth.AuthRepository
import com.example.spacebridgeapp.core.data.drive.DriveAuthManager
import com.example.spacebridgeapp.core.data.drive.DriveAuthResult
import com.example.spacebridgeapp.core.data.files.FilesRepository
import com.example.spacebridgeapp.core.data.files.StorageSummary
import com.example.spacebridgeapp.core.data.network.NetworkObserver
import com.example.spacebridgeapp.core.model.DriveFile
import com.example.spacebridgeapp.core.model.FileCategory
import com.example.spacebridgeapp.core.model.FileStatus
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FilesUiState(
    val files: List<DriveFile> = emptyList(),
    val filteredFiles: List<DriveFile> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: FileCategory? = null,
    val uploadProgressMap: Map<String, Float> = emptyMap(),
    val activeUploadingFile: DriveFile? = null,
    val activeUploadProgress: Float = 0f,
    val storageSummary: StorageSummary = StorageSummary(0, 0L),
    val isDriveAuthorized: Boolean = true,
    val userEmail: String? = null,
    val isOnline: Boolean = true,
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val isOpeningLocalCopy: Boolean = false,
    val selectedFileForActions: DriveFile? = null,
    val fileToRename: DriveFile? = null,
    val fileToTrash: DriveFile? = null,
)

sealed interface FilesEvent {
    data class ShowSnackbar(
        val message: String,
    ) : FilesEvent

    data class OpenUri(
        val uri: Uri,
        val mimeType: String,
    ) : FilesEvent

    data class OpenWebLink(
        val url: String,
    ) : FilesEvent

    data class ShareLink(
        val url: String,
    ) : FilesEvent

    data class LaunchConsent(
        val intentSenderRequest: IntentSenderRequest,
    ) : FilesEvent
}

class FilesViewModel(
    private val filesRepository: FilesRepository,
    private val driveAuthManager: DriveAuthManager,
    private val authRepository: AuthRepository,
    private val networkObserver: NetworkObserver,
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow<FileCategory?>(null)
    private val isSyncing = MutableStateFlow(false)
    private val isOpeningLocalCopy = MutableStateFlow(false)
    private val selectedFileForActions = MutableStateFlow<DriveFile?>(null)
    private val fileToRename = MutableStateFlow<DriveFile?>(null)
    private val fileToTrash = MutableStateFlow<DriveFile?>(null)
    private val storageSummary = MutableStateFlow(StorageSummary(0, 0L))

    private val _events = Channel<FilesEvent>(Channel.BUFFERED)
    val events: Flow<FilesEvent> = _events.receiveAsFlow()

    init {
        refreshStorageSummary()
    }

    private val externalFlow =
        combine(
            filesRepository.observeFiles(trashed = false),
            filesRepository.observeUploadProgress(),
            driveAuthManager.isAuthorized,
            authRepository.currentUser,
            networkObserver.isConnected,
        ) { files, progressMap, isAuth, user, isOnline ->
            ExternalFilesState(
                files = files,
                progressMap = progressMap,
                isAuth = isAuth,
                userEmail = user?.email,
                isOnline = isOnline,
            )
        }

    private val filterFlow =
        combine(
            searchQuery,
            selectedCategory,
            storageSummary,
        ) { query, category, storage ->
            FilterFilesState(query, category, storage)
        }

    private val dialogFlow =
        combine(
            isSyncing,
            isOpeningLocalCopy,
            selectedFileForActions,
            fileToRename,
            fileToTrash,
        ) { isSync, isOpening, selectedActions, toRename, toTrash ->
            DialogFilesState(isSync, isOpening, selectedActions, toRename, toTrash)
        }

    val uiState: StateFlow<FilesUiState> =
        combine(
            externalFlow,
            filterFlow,
            dialogFlow,
        ) { external, filter, dialog ->
            val trimmedQuery = filter.query.trim().lowercase()
            val filtered =
                external.files.filter { file ->
                    val matchesQuery = trimmedQuery.isEmpty() || file.name.lowercase().contains(trimmedQuery)
                    val matchesCategory = filter.category == null || file.category == filter.category
                    matchesQuery && matchesCategory
                }

            val activeUpload =
                external.files.firstOrNull { file ->
                    file.status == FileStatus.UPLOADING && external.progressMap.containsKey(file.id)
                }
            val activeProgress = if (activeUpload != null) external.progressMap[activeUpload.id] ?: 0f else 0f

            FilesUiState(
                files = external.files,
                filteredFiles = filtered,
                searchQuery = filter.query,
                selectedCategory = filter.category,
                uploadProgressMap = external.progressMap,
                activeUploadingFile = activeUpload,
                activeUploadProgress = activeProgress,
                storageSummary = filter.storage,
                isDriveAuthorized = external.isAuth,
                userEmail = external.userEmail,
                isOnline = external.isOnline,
                isLoading = false,
                isSyncing = dialog.isSync,
                isOpeningLocalCopy = dialog.isOpening,
                selectedFileForActions = dialog.selectedActions,
                fileToRename = dialog.toRename,
                fileToTrash = dialog.toTrash,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FilesUiState(),
        )

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun onCategorySelected(category: FileCategory?) {
        selectedCategory.value = category
    }

    fun onFileSelectedForActions(file: DriveFile?) {
        selectedFileForActions.value = file
    }

    fun onFileToRenameSelected(file: DriveFile?) {
        fileToRename.value = file
    }

    fun onFileToTrashSelected(file: DriveFile?) {
        fileToTrash.value = file
    }

    fun uploadFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val result = filesRepository.enqueueUploads(uris)
            refreshStorageSummary()
            if (result.failedCount > 0) {
                val reasonMsg = result.failureReason?.let { " ($it)" } ?: ""
                _events.send(
                    FilesEvent.ShowSnackbar(
                        "${result.queuedCount} berkas dimasukkan antrean, ${result.failedCount} gagal$reasonMsg",
                    ),
                )
            } else {
                _events.send(FilesEvent.ShowSnackbar("${result.queuedCount} berkas dimasukkan antrean pengunggahan"))
            }
        }
    }

    fun cancelUpload(fileId: String) {
        viewModelScope.launch {
            filesRepository.cancelUpload(fileId)
            _events.send(FilesEvent.ShowSnackbar("Pengunggahan berkas dibatalkan"))
            refreshStorageSummary()
        }
    }

    fun retryUpload(fileId: String) {
        viewModelScope.launch {
            filesRepository.retryUpload(fileId)
            _events.send(FilesEvent.ShowSnackbar("Mencoba ulang pengunggahan berkas"))
        }
    }

    fun renameFile(
        fileId: String,
        newName: String,
    ) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            val result = filesRepository.rename(fileId, trimmed)
            fileToRename.value = null
            result
                .onSuccess {
                    _events.send(FilesEvent.ShowSnackbar("Nama berkas berhasil diperbarui"))
                    refreshStorageSummary()
                }.onFailure { err ->
                    _events.send(FilesEvent.ShowSnackbar(err.localizedMessage ?: "Gagal mengubah nama berkas"))
                }
        }
    }

    fun moveToTrash(fileId: String) {
        viewModelScope.launch {
            val result = filesRepository.moveToTrash(fileId)
            fileToTrash.value = null
            selectedFileForActions.value = null
            result
                .onSuccess {
                    _events.send(FilesEvent.ShowSnackbar("Berkas dipindahkan ke Sampah"))
                    refreshStorageSummary()
                }.onFailure { err ->
                    _events.send(FilesEvent.ShowSnackbar(err.localizedMessage ?: "Gagal memindahkan ke Sampah"))
                }
        }
    }

    fun shareFile(fileId: String) {
        viewModelScope.launch {
            val result = filesRepository.share(fileId)
            result
                .onSuccess { link ->
                    if (link.isNotBlank()) {
                        _events.send(FilesEvent.ShareLink(link))
                    } else {
                        _events.send(FilesEvent.ShowSnackbar("Tautan berbagi publik berhasil diaktifkan"))
                    }
                }.onFailure { err ->
                    _events.send(FilesEvent.ShowSnackbar(err.localizedMessage ?: "Gagal membagikan tautan"))
                }
        }
    }

    fun saveToDownloads(fileId: String) {
        viewModelScope.launch {
            val result = filesRepository.saveToDownloads(fileId)
            selectedFileForActions.value = null
            result
                .onSuccess {
                    _events.send(FilesEvent.ShowSnackbar("Salinan berkas disimpan ke folder Download"))
                }.onFailure { err ->
                    _events.send(FilesEvent.ShowSnackbar(err.localizedMessage ?: "Gagal mengunduh berkas"))
                }
        }
    }

    fun openLocalCopy(file: DriveFile) {
        viewModelScope.launch {
            isOpeningLocalCopy.value = true
            val result = filesRepository.openLocalCopy(file.id) { }
            isOpeningLocalCopy.value = false
            selectedFileForActions.value = null
            result
                .onSuccess { uri ->
                    _events.send(FilesEvent.OpenUri(uri, file.mimeType))
                }.onFailure { err ->
                    _events.send(FilesEvent.ShowSnackbar(err.localizedMessage ?: "Gagal membuka salinan lokal"))
                }
        }
    }

    fun openInGoogleDrive(file: DriveFile) {
        selectedFileForActions.value = null
        val link = file.webViewLink
        if (!link.isNullOrBlank()) {
            viewModelScope.launch {
                _events.send(FilesEvent.OpenWebLink(link))
            }
        } else {
            viewModelScope.launch {
                _events.send(FilesEvent.ShowSnackbar("Tautan Google Drive belum tersedia untuk berkas ini"))
            }
        }
    }

    fun connectDrive(activity: Activity? = null) {
        viewModelScope.launch {
            val result =
                if (activity != null) {
                    driveAuthManager.authorize(activity)
                } else {
                    driveAuthManager.authorize()
                }
            when (result) {
                is DriveAuthResult.Granted -> {
                    _events.send(FilesEvent.ShowSnackbar("Akses Google Drive berhasil terhubung"))
                    refresh()
                }
                is DriveAuthResult.NeedsConsent -> {
                    val request =
                        IntentSenderRequest
                            .Builder(result.pendingIntent)
                            .build()
                    _events.send(FilesEvent.LaunchConsent(request))
                }
                is DriveAuthResult.Failed -> {
                    _events.send(FilesEvent.ShowSnackbar("Gagal menghubungkan Google Drive: ${result.reason}"))
                }
            }
        }
    }

    fun handleConsentResult(
        resultCode: Int,
        data: Intent?,
    ) {
        if (resultCode != Activity.RESULT_OK) {
            viewModelScope.launch {
                _events.send(FilesEvent.ShowSnackbar("Izin akses Google Drive dibatalkan"))
            }
            return
        }
        viewModelScope.launch {
            when (val result = driveAuthManager.handleConsentResult(data)) {
                is DriveAuthResult.Granted -> {
                    _events.send(FilesEvent.ShowSnackbar("Akses Google Drive berhasil terhubung"))
                    refresh()
                }
                is DriveAuthResult.NeedsConsent -> {
                    val request =
                        IntentSenderRequest
                            .Builder(result.pendingIntent)
                            .build()
                    _events.send(FilesEvent.LaunchConsent(request))
                }
                is DriveAuthResult.Failed -> {
                    _events.send(FilesEvent.ShowSnackbar("Gagal menghubungkan Google Drive: ${result.reason}"))
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            isSyncing.value = true
            refreshStorageSummary()
            isSyncing.value = false
        }
    }

    private fun refreshStorageSummary() {
        viewModelScope.launch {
            val summary = filesRepository.storageSummary()
            storageSummary.value = summary
        }
    }

    private data class ExternalFilesState(
        val files: List<DriveFile> = emptyList(),
        val progressMap: Map<String, Float> = emptyMap(),
        val isAuth: Boolean = true,
        val userEmail: String? = null,
        val isOnline: Boolean = true,
    )

    private data class FilterFilesState(
        val query: String = "",
        val category: FileCategory? = null,
        val storage: StorageSummary = StorageSummary(0, 0L),
    )

    private data class DialogFilesState(
        val isSync: Boolean = false,
        val isOpening: Boolean = false,
        val selectedActions: DriveFile? = null,
        val toRename: DriveFile? = null,
        val toTrash: DriveFile? = null,
    )

    companion object {
        fun provideFactory(
            filesRepository: FilesRepository,
            driveAuthManager: DriveAuthManager,
            authRepository: AuthRepository,
            networkObserver: NetworkObserver,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    FilesViewModel(
                        filesRepository = filesRepository,
                        driveAuthManager = driveAuthManager,
                        authRepository = authRepository,
                        networkObserver = networkObserver,
                    ) as T
            }
    }
}
