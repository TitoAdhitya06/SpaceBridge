package com.example.spacebridgeapp.feature.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.spacebridgeapp.core.data.auth.AuthRepository
import com.example.spacebridgeapp.core.data.files.FilesRepository
import com.example.spacebridgeapp.core.data.notes.NotesRepository
import com.example.spacebridgeapp.core.model.AppUser
import com.example.spacebridgeapp.core.model.DriveFile
import com.example.spacebridgeapp.core.model.Note
import com.example.spacebridgeapp.core.model.NoteStatus
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

enum class TrashTab {
    NOTES,
    DRIVE_FILES,
}

data class TrashUiState(
    val selectedTab: TrashTab = TrashTab.NOTES,
    val trashedNotes: List<Note> = emptyList(),
    val trashedFiles: List<DriveFile> = emptyList(),
    val notesCount: Int = 0,
    val filesCount: Int = 0,
    val isLoadingNotes: Boolean = true,
    val isLoadingFiles: Boolean = true,
    val user: AppUser? = null,
    val actionInProgressId: String? = null,
    val isPurging: Boolean = false,
)

sealed interface TrashEvent {
    data class ShowSnackbar(
        val message: String,
    ) : TrashEvent
}

class TrashViewModel(
    private val notesRepository: NotesRepository,
    private val filesRepository: FilesRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val selectedTabFlow = MutableStateFlow(TrashTab.NOTES)
    private val userFlow = MutableStateFlow<AppUser?>(authRepository.currentUser.value)
    private val actionInProgressIdFlow = MutableStateFlow<String?>(null)
    private val isPurgingFlow = MutableStateFlow(false)

    private val _events = Channel<TrashEvent>(Channel.BUFFERED)
    val events: Flow<TrashEvent> = _events.receiveAsFlow()

    init {
        // Pantau profil pengguna
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                userFlow.value = user
            }
        }

        // Pembersihan otomatis item sampah > 30 hari saat halaman sampah dimuat
        purgeExpiredTrash(days = 30)
    }

    private val trashedNotesFlow: Flow<List<Note>> =
        notesRepository.observeNotes(NoteStatus.TRASHED)

    private val trashedFilesFlow: Flow<List<DriveFile>> =
        filesRepository.observeFiles(trashed = true)

    private data class TrashedItems(
        val notes: List<Note>,
        val files: List<DriveFile>,
    )

    private val trashedItemsFlow: Flow<TrashedItems> =
        combine(trashedNotesFlow, trashedFilesFlow) { notes, files ->
            TrashedItems(notes, files)
        }

    val uiState: StateFlow<TrashUiState> =
        combine(
            selectedTabFlow,
            trashedItemsFlow,
            userFlow,
            actionInProgressIdFlow,
            isPurgingFlow,
        ) { tab, items, user, inProgressId, isPurging ->
            TrashUiState(
                selectedTab = tab,
                trashedNotes = items.notes,
                trashedFiles = items.files,
                notesCount = items.notes.size,
                filesCount = items.files.size,
                isLoadingNotes = false,
                isLoadingFiles = false,
                user = user,
                actionInProgressId = inProgressId,
                isPurging = isPurging,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TrashUiState(),
        )

    fun onSelectTab(tab: TrashTab) {
        selectedTabFlow.value = tab
    }

    fun onRestoreNote(
        noteId: String,
        noteTitle: String = "",
    ) {
        viewModelScope.launch {
            actionInProgressIdFlow.value = noteId
            try {
                notesRepository.setStatus(listOf(noteId), NoteStatus.ACTIVE)
                val label = noteTitle.trim().ifBlank { "Catatan" }
                _events.send(TrashEvent.ShowSnackbar("\"$label\" telah dipulihkan"))
            } catch (e: Exception) {
                _events.send(TrashEvent.ShowSnackbar("Gagal memulihkan catatan: ${e.localizedMessage ?: "Kesalahan tidak dikenal"}"))
            } finally {
                actionInProgressIdFlow.value = null
            }
        }
    }

    fun onDeleteNotePermanently(
        noteId: String,
        noteTitle: String = "",
    ) {
        viewModelScope.launch {
            actionInProgressIdFlow.value = noteId
            try {
                notesRepository.deletePermanently(listOf(noteId))
                val label = noteTitle.trim().ifBlank { "Catatan" }
                _events.send(TrashEvent.ShowSnackbar("\"$label\" dihapus permanen"))
            } catch (e: Exception) {
                _events.send(TrashEvent.ShowSnackbar("Gagal menghapus catatan permanen: ${e.localizedMessage ?: "Kesalahan tidak dikenal"}"))
            } finally {
                actionInProgressIdFlow.value = null
            }
        }
    }

    fun onRestoreFile(
        fileId: String,
        fileName: String = "",
    ) {
        viewModelScope.launch {
            actionInProgressIdFlow.value = fileId
            try {
                val result = filesRepository.restore(fileId)
                if (result.isSuccess) {
                    val label = fileName.trim().ifBlank { "Berkas" }
                    _events.send(TrashEvent.ShowSnackbar("\"$label\" telah dipulihkan"))
                } else {
                    _events.send(TrashEvent.ShowSnackbar("Gagal memulihkan berkas: ${result.exceptionOrNull()?.localizedMessage ?: "Kesalahan tidak dikenal"}"))
                }
            } catch (e: Exception) {
                _events.send(TrashEvent.ShowSnackbar("Gagal memulihkan berkas: ${e.localizedMessage ?: "Kesalahan tidak dikenal"}"))
            } finally {
                actionInProgressIdFlow.value = null
            }
        }
    }

    fun onDeleteFilePermanently(
        fileId: String,
        fileName: String = "",
    ) {
        viewModelScope.launch {
            actionInProgressIdFlow.value = fileId
            try {
                val result = filesRepository.deletePermanently(fileId)
                if (result.isSuccess) {
                    // Cabut ID file dari attachmentIds di seluruh catatan
                    notesRepository.removeAttachmentEverywhere(fileId)
                    val label = fileName.trim().ifBlank { "Berkas" }
                    _events.send(TrashEvent.ShowSnackbar("\"$label\" dihapus permanen"))
                } else {
                    _events.send(TrashEvent.ShowSnackbar("Gagal menghapus berkas: ${result.exceptionOrNull()?.localizedMessage ?: "Kesalahan tidak dikenal"}"))
                }
            } catch (e: Exception) {
                _events.send(TrashEvent.ShowSnackbar("Gagal menghapus berkas: ${e.localizedMessage ?: "Kesalahan tidak dikenal"}"))
            } finally {
                actionInProgressIdFlow.value = null
            }
        }
    }

    fun onEmptyTrash(currentTabOnly: Boolean = false) {
        viewModelScope.launch {
            isPurgingFlow.value = true
            try {
                val state = uiState.value
                if (currentTabOnly) {
                    when (selectedTabFlow.value) {
                        TrashTab.NOTES -> {
                            val ids = state.trashedNotes.map { it.id }
                            if (ids.isNotEmpty()) {
                                notesRepository.deletePermanently(ids)
                            }
                            _events.send(TrashEvent.ShowSnackbar("Semua catatan di Sampah telah dihapus permanen"))
                        }
                        TrashTab.DRIVE_FILES -> {
                            val files = state.trashedFiles
                            for (file in files) {
                                filesRepository.deletePermanently(file.id)
                                notesRepository.removeAttachmentEverywhere(file.id)
                            }
                            _events.send(TrashEvent.ShowSnackbar("Semua berkas di Sampah telah dihapus permanen"))
                        }
                    }
                } else {
                    // Kosongkan seluruh sampah (Catatan + Berkas)
                    val noteIds = state.trashedNotes.map { it.id }
                    if (noteIds.isNotEmpty()) {
                        notesRepository.deletePermanently(noteIds)
                    }

                    val files = state.trashedFiles
                    for (file in files) {
                        filesRepository.deletePermanently(file.id)
                        notesRepository.removeAttachmentEverywhere(file.id)
                    }
                    _events.send(TrashEvent.ShowSnackbar("Kotak sampah berhasil dikosongkan"))
                }
            } catch (e: Exception) {
                _events.send(TrashEvent.ShowSnackbar("Gagal mengosongkan sampah: ${e.localizedMessage ?: "Kesalahan tidak dikenal"}"))
            } finally {
                isPurgingFlow.value = false
            }
        }
    }

    fun purgeExpiredTrash(days: Long = 30) {
        viewModelScope.launch {
            try {
                val cutoff = Instant.now().minus(days, ChronoUnit.DAYS)
                notesRepository.purgeTrashOlderThan(cutoff)
                filesRepository.purgeTrashOlderThan(cutoff)
            } catch (_: Exception) {
            }
        }
    }

    companion object {
        fun provideFactory(
            notesRepository: NotesRepository,
            filesRepository: FilesRepository,
            authRepository: AuthRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TrashViewModel(
                        notesRepository = notesRepository,
                        filesRepository = filesRepository,
                        authRepository = authRepository,
                    ) as T
                }
            }
    }
}
