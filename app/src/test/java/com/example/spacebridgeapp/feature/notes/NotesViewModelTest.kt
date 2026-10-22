package com.example.spacebridgeapp.feature.notes

import android.app.Activity
import android.content.Context
import com.example.spacebridgeapp.core.data.auth.AuthRepository
import com.example.spacebridgeapp.core.data.migration.LegacyMigrator
import com.example.spacebridgeapp.core.data.network.NetworkObserver
import com.example.spacebridgeapp.core.data.notes.NoteChanges
import com.example.spacebridgeapp.core.data.notes.NotesRepository
import com.example.spacebridgeapp.core.model.AppUser
import com.example.spacebridgeapp.core.model.ChecklistItem
import com.example.spacebridgeapp.core.model.Note
import com.example.spacebridgeapp.core.model.NoteStatus
import com.example.spacebridgeapp.core.model.NoteType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModelTest {
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var fakeNotesRepository: FakeNotesRepository
    private lateinit var fakeNetworkObserver: FakeNetworkObserver
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var fakeLegacyMigrator: LegacyMigrator

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        fakeNotesRepository = FakeNotesRepository()
        fakeNetworkObserver = FakeNetworkObserver()
        fakeAuthRepository = FakeAuthRepository()
        fakeLegacyMigrator = LegacyMigrator()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test initial state separates pinned and other notes`() =
        runTest(testDispatcher) {
            val note1 =
                Note(
                    id = "n1",
                    type = NoteType.TEXT,
                    title = "Catatan Penting",
                    body = "Isi penting",
                    pinned = true,
                )
            val note2 =
                Note(
                    id = "n2",
                    type = NoteType.CHECKLIST,
                    title = "Belanjaan",
                    body = "",
                    items = listOf(ChecklistItem("i1", "Susu", true), ChecklistItem("i2", "Roti", false)),
                    pinned = false,
                )

            fakeNotesRepository.emitNotes(listOf(note1, note2))

            val viewModel =
                NotesViewModel(
                    notesRepository = fakeNotesRepository,
                    networkObserver = fakeNetworkObserver,
                    authRepository = fakeAuthRepository,
                    legacyMigrator = fakeLegacyMigrator,
                )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(1, state.pinnedNotes.size)
            assertEquals("n1", state.pinnedNotes[0].id)
            assertEquals(1, state.otherNotes.size)
            assertEquals("n2", state.otherNotes[0].id)
            assertEquals(2, state.totalActiveNotes)
            assertEquals(1, state.completedChecklistItems)
            assertEquals(2, state.totalChecklistItems)
        }

    @Test
    fun `test search filtering filters by title and body`() =
        runTest(testDispatcher) {
            val note1 = Note(id = "n1", type = NoteType.TEXT, title = "Resep Kue", body = "Tepung dan gula")
            val note2 = Note(id = "n2", type = NoteType.TEXT, title = "Jadwal Kerja", body = "Meeting pagi")

            fakeNotesRepository.emitNotes(listOf(note1, note2))

            val viewModel =
                NotesViewModel(
                    notesRepository = fakeNotesRepository,
                    networkObserver = fakeNetworkObserver,
                    authRepository = fakeAuthRepository,
                    legacyMigrator = fakeLegacyMigrator,
                )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }
            advanceUntilIdle()

            viewModel.onSearchQueryChange("resep")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(1, state.otherNotes.size)
            assertEquals("n1", state.otherNotes[0].id)
        }

    @Test
    fun `test category filtering separates text and checklist`() =
        runTest(testDispatcher) {
            val note1 = Note(id = "n1", type = NoteType.TEXT, title = "Catatan")
            val note2 = Note(id = "n2", type = NoteType.CHECKLIST, title = "Checklist")

            fakeNotesRepository.emitNotes(listOf(note1, note2))

            val viewModel =
                NotesViewModel(
                    notesRepository = fakeNotesRepository,
                    networkObserver = fakeNetworkObserver,
                    authRepository = fakeAuthRepository,
                    legacyMigrator = fakeLegacyMigrator,
                )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }
            advanceUntilIdle()

            viewModel.onFilterSelected(NoteFilter.Text)
            advanceUntilIdle()
            assertEquals(1, viewModel.uiState.value.otherNotes.size)
            assertEquals(
                "n1",
                viewModel.uiState.value.otherNotes[0]
                    .id,
            )

            viewModel.onFilterSelected(NoteFilter.Checklist)
            advanceUntilIdle()
            assertEquals(1, viewModel.uiState.value.otherNotes.size)
            assertEquals(
                "n2",
                viewModel.uiState.value.otherNotes[0]
                    .id,
            )
        }

    @Test
    fun `test toggle layout toggles grid and list mode`() =
        runTest(testDispatcher) {
            val viewModel =
                NotesViewModel(
                    notesRepository = fakeNotesRepository,
                    networkObserver = fakeNetworkObserver,
                    authRepository = fakeAuthRepository,
                    legacyMigrator = fakeLegacyMigrator,
                )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isGridLayout)

            viewModel.onToggleLayout()
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.isGridLayout)

            viewModel.onToggleLayout()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isGridLayout)
        }

    @Test
    fun `test toggle pin calls repository setPinned`() =
        runTest(testDispatcher) {
            val viewModel =
                NotesViewModel(
                    notesRepository = fakeNotesRepository,
                    networkObserver = fakeNetworkObserver,
                    authRepository = fakeAuthRepository,
                    legacyMigrator = fakeLegacyMigrator,
                )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }
            advanceUntilIdle()

            viewModel.onTogglePin("note-1", currentPinned = false)
            advanceUntilIdle()

            assertEquals(listOf("note-1"), fakeNotesRepository.lastPinnedIds)
            assertEquals(true, fakeNotesRepository.lastPinnedValue)
        }

    @Test
    fun `test onDeleteNote moves note to trash and emits ShowUndoSnackbar`() =
        runTest(testDispatcher) {
            val viewModel =
                NotesViewModel(
                    notesRepository = fakeNotesRepository,
                    networkObserver = fakeNetworkObserver,
                    authRepository = fakeAuthRepository,
                    legacyMigrator = fakeLegacyMigrator,
                )

            val events = mutableListOf<NotesEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.events.collect { events.add(it) }
            }

            viewModel.onDeleteNote("note-to-trash")
            advanceUntilIdle()

            assertEquals(listOf("note-to-trash"), fakeNotesRepository.lastStatusIds)
            assertEquals(NoteStatus.TRASHED, fakeNotesRepository.lastStatusValue)
            assertEquals(1, events.size)
            assertTrue(events[0] is NotesEvent.ShowUndoSnackbar)
            val undoEvent = events[0] as NotesEvent.ShowUndoSnackbar
            assertEquals("note-to-trash", undoEvent.noteId)
        }

    @Test
    fun `test onRestoreNote restores note to active and emits confirmation`() =
        runTest(testDispatcher) {
            val viewModel =
                NotesViewModel(
                    notesRepository = fakeNotesRepository,
                    networkObserver = fakeNetworkObserver,
                    authRepository = fakeAuthRepository,
                    legacyMigrator = fakeLegacyMigrator,
                )

            val events = mutableListOf<NotesEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.events.collect { events.add(it) }
            }

            viewModel.onRestoreNote("note-to-restore")
            advanceUntilIdle()

            assertEquals(listOf("note-to-restore"), fakeNotesRepository.lastStatusIds)
            assertEquals(NoteStatus.ACTIVE, fakeNotesRepository.lastStatusValue)
            assertEquals(1, events.size)
            assertTrue(events[0] is NotesEvent.ShowSnackbar)
        }

    @Test
    fun `test onNoteTrashedNotice emits ShowUndoSnackbar`() =
        runTest(testDispatcher) {
            val viewModel =
                NotesViewModel(
                    notesRepository = fakeNotesRepository,
                    networkObserver = fakeNetworkObserver,
                    authRepository = fakeAuthRepository,
                    legacyMigrator = fakeLegacyMigrator,
                )

            val events = mutableListOf<NotesEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.events.collect { events.add(it) }
            }

            viewModel.onNoteTrashedNotice("note-from-editor")
            advanceUntilIdle()

            assertEquals(1, events.size)
            assertTrue(events[0] is NotesEvent.ShowUndoSnackbar)
            val undoEvent = events[0] as NotesEvent.ShowUndoSnackbar
            assertEquals("note-from-editor", undoEvent.noteId)
        }

    @Test
    fun `test network offline transition updates isOnline state for banner display`() =
        runTest(testDispatcher) {
            val viewModel =
                NotesViewModel(
                    notesRepository = fakeNotesRepository,
                    networkObserver = fakeNetworkObserver,
                    authRepository = fakeAuthRepository,
                    legacyMigrator = fakeLegacyMigrator,
                )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isOnline)

            // Masuk mode offline (mode pesawat)
            fakeNetworkObserver.setConnected(false)
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.isOnline)

            // Kembali online
            fakeNetworkObserver.setConnected(true)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isOnline)
        }

    @Test
    fun `test notes with hasPendingWrites are properly reflected in state`() =
        runTest(testDispatcher) {
            val localPendingNote =
                Note(
                    id = "local-1",
                    type = NoteType.TEXT,
                    title = "Catatan Offline",
                    body = "Ditulis saat pesawat terbang",
                    hasPendingWrites = true,
                )
            val syncedNote =
                Note(
                    id = "synced-1",
                    type = NoteType.TEXT,
                    title = "Catatan Cloud",
                    body = "Sudah tersinkron",
                    hasPendingWrites = false,
                )

            fakeNotesRepository.emitNotes(listOf(localPendingNote, syncedNote))

            val viewModel =
                NotesViewModel(
                    notesRepository = fakeNotesRepository,
                    networkObserver = fakeNetworkObserver,
                    authRepository = fakeAuthRepository,
                    legacyMigrator = fakeLegacyMigrator,
                )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(2, state.otherNotes.size)
            assertTrue(state.otherNotes.first { it.id == "local-1" }.hasPendingWrites)
            assertFalse(state.otherNotes.first { it.id == "synced-1" }.hasPendingWrites)
        }

    private class FakeNotesRepository : NotesRepository {
        private val notesFlow = MutableStateFlow<List<Note>>(emptyList())
        var lastPinnedIds: List<String>? = null
        var lastPinnedValue: Boolean? = null
        var lastStatusIds: List<String>? = null
        var lastStatusValue: NoteStatus? = null

        fun emitNotes(notes: List<Note>) {
            notesFlow.value = notes
        }

        override fun observeNotes(status: NoteStatus): Flow<List<Note>> = notesFlow

        override fun observeNote(id: String): Flow<Note?> = MutableStateFlow(notesFlow.value.find { it.id == id })

        override fun newId(): String = "id-123"

        override fun create(note: Note) {
            notesFlow.value = notesFlow.value + note
        }

        override fun update(
            id: String,
            changes: NoteChanges,
        ) {}

        override fun setPinned(
            ids: List<String>,
            pinned: Boolean,
        ) {
            lastPinnedIds = ids
            lastPinnedValue = pinned
        }

        override fun setStatus(
            ids: List<String>,
            status: NoteStatus,
        ) {
            lastStatusIds = ids
            lastStatusValue = status
        }

        override fun deletePermanently(ids: List<String>) {}

        override fun addLabel(
            ids: List<String>,
            labelId: String,
        ) {}

        override suspend fun purgeTrashOlderThan(cutoff: Instant) {}

        override suspend fun removeAttachmentEverywhere(fileId: String) {}

        override suspend fun removeLabelEverywhere(labelId: String) {}
    }

    private class FakeNetworkObserver : NetworkObserver {
        private val _isConnected = MutableStateFlow(true)
        override val isConnected: Flow<Boolean> = _isConnected.asStateFlow()

        fun setConnected(connected: Boolean) {
            _isConnected.value = connected
        }
    }

    private class FakeAuthRepository : AuthRepository {
        private val _currentUser =
            MutableStateFlow<AppUser?>(
                AppUser("user-1", "Test User", "test@example.com", null),
            )
        override val currentUser: StateFlow<AppUser?> = _currentUser.asStateFlow()

        override suspend fun signIn(activity: Activity): Result<Unit> = Result.success(Unit)

        override suspend fun signIn(context: Context): Result<Unit> = Result.success(Unit)

        override suspend fun signOut() {}

        override suspend fun deleteAccount(
            activity: Activity,
            trashDriveFolder: Boolean,
        ): Result<Unit> = Result.success(Unit)
    }
}
