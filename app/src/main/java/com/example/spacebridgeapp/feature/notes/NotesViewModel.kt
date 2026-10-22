package com.example.spacebridgeapp.feature.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.spacebridgeapp.core.data.auth.AuthRepository
import com.example.spacebridgeapp.core.data.migration.LegacyMigrator
import com.example.spacebridgeapp.core.data.network.NetworkObserver
import com.example.spacebridgeapp.core.data.notes.NotesRepository
import com.example.spacebridgeapp.core.model.AppUser
import com.example.spacebridgeapp.core.model.Note
import com.example.spacebridgeapp.core.model.NoteStatus
import com.example.spacebridgeapp.core.model.NoteType
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface NoteFilter {
    val displayName: String

    data object All : NoteFilter {
        override val displayName: String = "Semua"
    }

    data object Text : NoteFilter {
        override val displayName: String = "Catatan Teks"
    }

    data object Checklist : NoteFilter {
        override val displayName: String = "Checklist"
    }

    data class Label(
        val name: String,
    ) : NoteFilter {
        override val displayName: String = name
    }
}

data class NotesUiState(
    val pinnedNotes: List<Note> = emptyList(),
    val otherNotes: List<Note> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: NoteFilter = NoteFilter.All,
    val availableFilters: List<NoteFilter> = listOf(NoteFilter.All, NoteFilter.Text, NoteFilter.Checklist),
    val isGridLayout: Boolean = true,
    val isLoading: Boolean = true,
    val isOnline: Boolean = true,
    val user: AppUser? = null,
    val totalActiveNotes: Int = 0,
    val completedChecklistItems: Int = 0,
    val totalChecklistItems: Int = 0,
    val errorMessage: String? = null,
)

sealed interface NotesEvent {
    data class ShowSnackbar(
        val message: String,
    ) : NotesEvent

    data class ShowUndoSnackbar(
        val message: String,
        val noteId: String,
    ) : NotesEvent

    data class NavigateToEditor(
        val noteId: String?,
    ) : NotesEvent
}

class NotesViewModel(
    private val notesRepository: NotesRepository,
    private val networkObserver: NetworkObserver,
    private val authRepository: AuthRepository,
    private val legacyMigrator: LegacyMigrator,
) : ViewModel() {
    private val searchQueryFlow = MutableStateFlow("")
    private val selectedFilterFlow = MutableStateFlow<NoteFilter>(NoteFilter.All)
    private val isGridLayoutFlow = MutableStateFlow(true)
    private val isOnlineFlow = MutableStateFlow(true)
    private val userFlow = MutableStateFlow<AppUser?>(authRepository.currentUser.value)

    private val _events = Channel<NotesEvent>(Channel.BUFFERED)
    val events: Flow<NotesEvent> = _events.receiveAsFlow()

    init {
        // Jalankan migrasi di latar belakang jika ada catatan v2 lama
        viewModelScope.launch {
            val currentUser = authRepository.currentUser.value
            if (currentUser != null) {
                legacyMigrator.migrateIfNeeded(currentUser.uid)
            }
        }

        // Pembersihan otomatis catatan sampah > 30 hari di latar belakang saat aplikasi dibuka
        viewModelScope.launch {
            try {
                val cutoff = java.time.Instant.now().minus(30, java.time.temporal.ChronoUnit.DAYS)
                notesRepository.purgeTrashOlderThan(cutoff)
            } catch (_: Exception) {
            }
        }

        // Pantau status koneksi internet
        viewModelScope.launch {
            networkObserver.isConnected.collect { connected ->
                isOnlineFlow.value = connected
            }
        }

        // Pantau profil pengguna login
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                userFlow.value = user
                if (user != null) {
                    legacyMigrator.migrateIfNeeded(user.uid)
                }
            }
        }
    }

    private data class ViewMetadata(
        val isGrid: Boolean,
        val isOnline: Boolean,
        val user: AppUser?,
    )

    private val viewMetadataFlow =
        combine(isGridLayoutFlow, isOnlineFlow, userFlow) { isGrid, online, user ->
            ViewMetadata(isGrid = isGrid, isOnline = online, user = user)
        }

    val uiState: StateFlow<NotesUiState> =
        combine(
            notesRepository.observeNotes(NoteStatus.ACTIVE),
            searchQueryFlow,
            selectedFilterFlow,
            viewMetadataFlow,
        ) { rawNotes, query, filter, metadata ->
            val totalActive = rawNotes.size
            val completedChecklists = rawNotes.sumOf { it.items.count { item -> item.checked } }
            val totalChecklists = rawNotes.sumOf { it.items.size }

            val dynamicLabels =
                rawNotes
                    .flatMap { it.labelIds }
                    .distinct()
                    .filter { it.isNotBlank() }
                    .map { NoteFilter.Label(it) }

            val allFilters = listOf(NoteFilter.All, NoteFilter.Text, NoteFilter.Checklist) + dynamicLabels

            val filteredByCategory =
                when (filter) {
                    NoteFilter.All -> rawNotes
                    NoteFilter.Text -> rawNotes.filter { it.type == NoteType.TEXT }
                    NoteFilter.Checklist -> rawNotes.filter { it.type == NoteType.CHECKLIST }
                    is NoteFilter.Label -> rawNotes.filter { it.labelIds.contains(filter.name) }
                }

            val trimmedQuery = query.trim()
            val filteredNotes =
                if (trimmedQuery.isEmpty()) {
                    filteredByCategory
                } else {
                    filteredByCategory.filter { note ->
                        note.title.contains(trimmedQuery, ignoreCase = true) ||
                            note.body.contains(trimmedQuery, ignoreCase = true) ||
                            note.items.any { it.text.contains(trimmedQuery, ignoreCase = true) } ||
                            note.labelIds.any { it.contains(trimmedQuery, ignoreCase = true) }
                    }
                }

            val pinned = filteredNotes.filter { it.pinned }
            val other = filteredNotes.filter { !it.pinned }

            NotesUiState(
                pinnedNotes = pinned,
                otherNotes = other,
                searchQuery = query,
                selectedFilter = filter,
                availableFilters = allFilters,
                isGridLayout = metadata.isGrid,
                isLoading = false,
                isOnline = metadata.isOnline,
                user = metadata.user,
                totalActiveNotes = totalActive,
                completedChecklistItems = completedChecklists,
                totalChecklistItems = totalChecklists,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NotesUiState(),
        )

    fun onSearchQueryChange(query: String) {
        searchQueryFlow.value = query
    }

    fun onFilterSelected(filter: NoteFilter) {
        selectedFilterFlow.value = filter
    }

    fun onToggleLayout() {
        isGridLayoutFlow.value = !isGridLayoutFlow.value
    }

    fun onTogglePin(
        noteId: String,
        currentPinned: Boolean,
    ) {
        notesRepository.setPinned(listOf(noteId), !currentPinned)
    }

    fun onDeleteNote(noteId: String) {
        notesRepository.setStatus(listOf(noteId), NoteStatus.TRASHED)
        viewModelScope.launch {
            _events.send(NotesEvent.ShowUndoSnackbar("Catatan dipindahkan ke Sampah", noteId))
        }
    }

    fun onNoteTrashedNotice(noteId: String) {
        viewModelScope.launch {
            _events.send(NotesEvent.ShowUndoSnackbar("Catatan dipindahkan ke Sampah", noteId))
        }
    }

    fun onRestoreNote(noteId: String) {
        notesRepository.setStatus(listOf(noteId), NoteStatus.ACTIVE)
        viewModelScope.launch {
            _events.send(NotesEvent.ShowSnackbar("Catatan berhasil dipulihkan"))
        }
    }

    companion object {
        fun provideFactory(
            notesRepository: NotesRepository,
            networkObserver: NetworkObserver,
            authRepository: AuthRepository,
            legacyMigrator: LegacyMigrator,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    NotesViewModel(
                        notesRepository = notesRepository,
                        networkObserver = networkObserver,
                        authRepository = authRepository,
                        legacyMigrator = legacyMigrator,
                    ) as T
            }
    }
}
