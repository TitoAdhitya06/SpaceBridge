package com.example.spacebridgeapp.feature.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.spacebridgeapp.core.data.network.NetworkObserver
import com.example.spacebridgeapp.core.data.notes.NoteChanges
import com.example.spacebridgeapp.core.data.notes.NotesRepository
import com.example.spacebridgeapp.core.model.ChecklistItem
import com.example.spacebridgeapp.core.model.Note
import com.example.spacebridgeapp.core.model.NoteStatus
import com.example.spacebridgeapp.core.model.NoteType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

enum class EditorSaveStatus {
    SAVING,
    SAVED_CLOUD,
    SAVED_LOCAL,
}

data class EditorUiState(
    val noteId: String,
    val type: NoteType = NoteType.TEXT,
    val title: String = "",
    val body: String = "",
    val items: List<ChecklistItem> = emptyList(),
    val pinned: Boolean = false,
    val labelIds: List<String> = emptyList(),
    val attachmentIds: List<String> = emptyList(),
    val saveStatus: EditorSaveStatus = EditorSaveStatus.SAVED_CLOUD,
    val isNewNote: Boolean = false,
    val hasCreatedDocumentInFirestore: Boolean = false,
    val updatedAt: Instant? = null,
    val isOnline: Boolean = true,
    val isLoaded: Boolean = false,
    val focusedItemId: String? = null,
) {
    val wordCount: Int
        get() = if (body.isBlank()) 0 else body.trim().split("\\s+".toRegex()).size

    val charCount: Int
        get() = body.length
}

sealed interface EditorEvent {
    object NavigateBack : EditorEvent

    data class NoteTrashed(
        val noteId: String,
    ) : EditorEvent

    data class ShowToast(
        val message: String,
    ) : EditorEvent
}

class EditorViewModel(
    private val notesRepository: NotesRepository,
    private val networkObserver: NetworkObserver,
    initialNoteId: String?,
    initialTypeStr: String?,
) : ViewModel() {
    private val isNew = initialNoteId == null
    private val effectiveNoteId = initialNoteId ?: notesRepository.newId()
    private val resolvedInitialType =
        if (initialTypeStr.equals("checklist", ignoreCase = true)) {
            NoteType.CHECKLIST
        } else {
            NoteType.TEXT
        }

    private val _uiState =
        MutableStateFlow(
            EditorUiState(
                noteId = effectiveNoteId,
                type = resolvedInitialType,
                isNewNote = isNew,
                hasCreatedDocumentInFirestore = !isNew,
                isLoaded = isNew,
                items =
                    if (isNew && resolvedInitialType == NoteType.CHECKLIST) {
                        listOf(ChecklistItem(id = UUID.randomUUID().toString(), text = ""))
                    } else {
                        emptyList()
                    },
            ),
        )
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EditorEvent>()
    val events: SharedFlow<EditorEvent> = _events.asSharedFlow()

    private var autosaveJob: Job? = null
    private var hasPendingChanges = false

    init {
        // Pantau status online
        viewModelScope.launch {
            networkObserver.isConnected.collect { connected ->
                _uiState.update { current ->
                    current.copy(
                        isOnline = connected,
                        saveStatus =
                            if (current.saveStatus != EditorSaveStatus.SAVING) {
                                if (connected) EditorSaveStatus.SAVED_CLOUD else EditorSaveStatus.SAVED_LOCAL
                            } else {
                                current.saveStatus
                            },
                    )
                }
            }
        }

        // Jika mengedit catatan lama, pantau data dokumen Firestore
        if (!isNew) {
            viewModelScope.launch {
                notesRepository.observeNote(effectiveNoteId).collect { note ->
                    if (note != null) {
                        _uiState.update { current ->
                            if (!current.isLoaded) {
                                current.copy(
                                    type = note.type,
                                    title = note.title,
                                    body = note.body,
                                    items = note.items,
                                    pinned = note.pinned,
                                    labelIds = note.labelIds,
                                    attachmentIds = note.attachmentIds,
                                    updatedAt = note.updatedAt,
                                    isLoaded = true,
                                    saveStatus =
                                        if (note.hasPendingWrites) {
                                            EditorSaveStatus.SAVED_LOCAL
                                        } else {
                                            EditorSaveStatus.SAVED_CLOUD
                                        },
                                )
                            } else {
                                // Pembaruan metadata eksternal tanpa menimpa teks yang sedang diketik
                                current.copy(
                                    updatedAt = note.updatedAt ?: current.updatedAt,
                                    attachmentIds = note.attachmentIds,
                                    labelIds = note.labelIds,
                                    saveStatus =
                                        if (hasPendingChanges) {
                                            EditorSaveStatus.SAVING
                                        } else if (note.hasPendingWrites) {
                                            EditorSaveStatus.SAVED_LOCAL
                                        } else {
                                            EditorSaveStatus.SAVED_CLOUD
                                        },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun onTitleChange(newTitle: String) {
        _uiState.update { it.copy(title = newTitle) }
        scheduleAutosave()
    }

    fun onBodyChange(newBody: String) {
        _uiState.update { it.copy(body = newBody) }
        scheduleAutosave()
    }

    fun onTogglePin() {
        val currentPinned = _uiState.value.pinned
        _uiState.update { it.copy(pinned = !currentPinned) }
        scheduleAutosave()
    }

    fun onConvertType() {
        val currentState = _uiState.value
        val newType =
            if (currentState.type == NoteType.TEXT) {
                NoteType.CHECKLIST
            } else {
                NoteType.TEXT
            }

        if (newType == NoteType.CHECKLIST) {
            val lines =
                currentState.body
                    .lines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            val newItems =
                if (lines.isEmpty()) {
                    listOf(ChecklistItem(id = UUID.randomUUID().toString(), text = ""))
                } else {
                    lines.map { ChecklistItem(id = UUID.randomUUID().toString(), text = it) }
                }
            _uiState.update {
                it.copy(
                    type = NoteType.CHECKLIST,
                    body = "",
                    items = newItems,
                )
            }
        } else {
            val newBody = currentState.items.joinToString("\n") { it.text }
            _uiState.update {
                it.copy(
                    type = NoteType.TEXT,
                    body = newBody,
                    items = emptyList(),
                )
            }
        }
        scheduleAutosave()
    }

    fun onItemTextChange(
        itemId: String,
        newText: String,
    ) {
        _uiState.update { current ->
            current.copy(
                items =
                    current.items.map { item ->
                        if (item.id == itemId) item.copy(text = newText) else item
                    },
            )
        }
        scheduleAutosave()
    }

    fun onToggleItem(itemId: String) {
        _uiState.update { current ->
            current.copy(
                items =
                    current.items.map { item ->
                        if (item.id == itemId) item.copy(checked = !item.checked) else item
                    },
            )
        }
        scheduleAutosave()
    }

    fun onDeleteItem(itemId: String) {
        _uiState.update { current ->
            val activeItems = current.items.filter { !it.checked }
            val itemIndex = activeItems.indexOfFirst { it.id == itemId }
            val nextFocusId =
                if (current.focusedItemId == itemId) {
                    when {
                        itemIndex > 0 -> activeItems[itemIndex - 1].id
                        activeItems.size > 1 -> activeItems[1].id
                        else -> null
                    }
                } else {
                    current.focusedItemId
                }

            current.copy(
                items = current.items.filter { it.id != itemId },
                focusedItemId = nextFocusId,
            )
        }
        scheduleAutosave()
    }

    fun onAddItem(
        text: String = "",
        afterIndex: Int? = null,
    ) {
        val newItem = ChecklistItem(id = UUID.randomUUID().toString(), text = text)
        _uiState.update { current ->
            val mutable = current.items.toMutableList()
            if (afterIndex != null && afterIndex in 0..mutable.lastIndex) {
                // Cari posisi asli di dalam daftar utuh
                val activeItems = mutable.filter { !it.checked }
                val targetActiveItem = activeItems.getOrNull(afterIndex)
                if (targetActiveItem != null) {
                    val realIndex = mutable.indexOfFirst { it.id == targetActiveItem.id }
                    mutable.add(realIndex + 1, newItem)
                } else {
                    mutable.add(newItem)
                }
            } else {
                mutable.add(newItem)
            }
            current.copy(
                items = mutable,
                focusedItemId = newItem.id,
            )
        }
        scheduleAutosave()
    }

    fun onAddLabel(labelId: String) {
        val trimmed = labelId.trim()
        if (trimmed.isEmpty()) return
        _uiState.update { current ->
            if (trimmed !in current.labelIds) {
                current.copy(labelIds = current.labelIds + trimmed)
            } else {
                current
            }
        }
        scheduleAutosave()
    }

    fun onRemoveLabel(labelId: String) {
        _uiState.update { current ->
            current.copy(labelIds = current.labelIds.filter { it != labelId })
        }
        scheduleAutosave()
    }

    fun onMoveToTrash() {
        autosaveJob?.cancel()
        hasPendingChanges = false
        val currentState = _uiState.value
        val noteId = currentState.noteId
        if (currentState.hasCreatedDocumentInFirestore || !currentState.isNewNote) {
            notesRepository.setStatus(listOf(noteId), NoteStatus.TRASHED)
        }
        viewModelScope.launch {
            _events.emit(EditorEvent.NoteTrashed(noteId))
        }
    }

    fun flushAndCleanup() {
        autosaveJob?.cancel()
        val currentState = _uiState.value
        val isBlank =
            currentState.title.isBlank() &&
                currentState.body.isBlank() &&
                currentState.items.all { it.text.isBlank() }

        if (isBlank) {
            if (currentState.hasCreatedDocumentInFirestore || !currentState.isNewNote) {
                notesRepository.deletePermanently(listOf(currentState.noteId))
            }
            hasPendingChanges = false
        } else if (hasPendingChanges) {
            saveChanges()
        }
    }

    private fun scheduleAutosave() {
        hasPendingChanges = true
        _uiState.update { it.copy(saveStatus = EditorSaveStatus.SAVING) }
        autosaveJob?.cancel()
        autosaveJob =
            viewModelScope.launch {
                delay(800)
                saveChanges()
            }
    }

    private fun saveChanges() {
        val currentState = _uiState.value
        val isBlank =
            currentState.title.isBlank() &&
                currentState.body.isBlank() &&
                currentState.items.all { it.text.isBlank() }

        if (currentState.isNewNote && !currentState.hasCreatedDocumentInFirestore) {
            if (isBlank) {
                hasPendingChanges = false
                return
            }
            val noteToCreate =
                Note(
                    id = currentState.noteId,
                    type = currentState.type,
                    title = currentState.title,
                    body = if (currentState.type == NoteType.TEXT) currentState.body else "",
                    items = if (currentState.type == NoteType.CHECKLIST) currentState.items else emptyList(),
                    pinned = currentState.pinned,
                    labelIds = currentState.labelIds,
                    attachmentIds = currentState.attachmentIds,
                    status = NoteStatus.ACTIVE,
                )
            notesRepository.create(noteToCreate)
            _uiState.update {
                it.copy(
                    hasCreatedDocumentInFirestore = true,
                    saveStatus = if (it.isOnline) EditorSaveStatus.SAVED_CLOUD else EditorSaveStatus.SAVED_LOCAL,
                )
            }
            hasPendingChanges = false
        } else {
            val changes =
                NoteChanges(
                    type = currentState.type,
                    title = currentState.title,
                    body = if (currentState.type == NoteType.TEXT) currentState.body else "",
                    items = if (currentState.type == NoteType.CHECKLIST) currentState.items else emptyList(),
                    pinned = currentState.pinned,
                    labelIds = currentState.labelIds,
                    attachmentIds = currentState.attachmentIds,
                )
            notesRepository.update(currentState.noteId, changes)
            _uiState.update {
                it.copy(
                    saveStatus = if (it.isOnline) EditorSaveStatus.SAVED_CLOUD else EditorSaveStatus.SAVED_LOCAL,
                )
            }
            hasPendingChanges = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        flushAndCleanup()
    }

    companion object {
        fun provideFactory(
            notesRepository: NotesRepository,
            networkObserver: NetworkObserver,
            noteId: String?,
            initialType: String?,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    EditorViewModel(
                        notesRepository = notesRepository,
                        networkObserver = networkObserver,
                        initialNoteId = noteId,
                        initialTypeStr = initialType,
                    ) as T
            }
    }
}
