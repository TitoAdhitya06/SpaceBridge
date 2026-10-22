package com.example.spacebridgeapp.feature.editor

import com.example.spacebridgeapp.core.data.network.NetworkObserver
import com.example.spacebridgeapp.core.data.notes.NoteChanges
import com.example.spacebridgeapp.core.data.notes.NotesRepository
import com.example.spacebridgeapp.core.model.Note
import com.example.spacebridgeapp.core.model.NoteStatus
import com.example.spacebridgeapp.core.model.NoteType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeNotesRepository: FakeNotesRepository
    private lateinit var fakeNetworkObserver: FakeNetworkObserver

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeNotesRepository = FakeNotesRepository()
        fakeNetworkObserver = FakeNetworkObserver()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test initial state for new text note`() =
        runTest(testDispatcher) {
            val viewModel =
                EditorViewModel(
                    notesRepository = fakeNotesRepository,
                    networkObserver = fakeNetworkObserver,
                    initialNoteId = null,
                    initialTypeStr = "text",
                )

            val state = viewModel.uiState.value
            assertEquals("generated-id", state.noteId)
            assertEquals(NoteType.TEXT, state.type)
            assertTrue(state.isNewNote)
            assertFalse(state.hasCreatedDocumentInFirestore)
            assertEquals("", state.title)
            assertEquals("", state.body)
            assertTrue(state.items.isEmpty())
        }

    @Test
    fun `test initial state for new checklist note creates initial item`() =
        runTest(testDispatcher) {
            val viewModel =
                EditorViewModel(
                    notesRepository = fakeNotesRepository,
                    networkObserver = fakeNetworkObserver,
                    initialNoteId = null,
                    initialTypeStr = "checklist",
                )

            val state = viewModel.uiState.value
            assertEquals(NoteType.CHECKLIST, state.type)
            assertEquals(1, state.items.size)
            assertEquals("", state.items[0].text)
            assertFalse(state.items[0].checked)
        }

    @Test
    fun `test autosave debounces 800ms before creating document`() =
        runTest(testDispatcher) {
            val viewModel =
                EditorViewModel(
                    notesRepository = fakeNotesRepository,
                    networkObserver = fakeNetworkObserver,
                    initialNoteId = null,
                    initialTypeStr = "text",
                )

            viewModel.onTitleChange("Judul Baru")
            assertEquals(EditorSaveStatus.SAVING, viewModel.uiState.value.saveStatus)
            assertNull(fakeNotesRepository.createdNote)

            // Belum 800ms
            advanceTimeBy(400)
            assertNull(fakeNotesRepository.createdNote)

            // Melewati 800ms
            advanceTimeBy(450)
            assertNotNull(fakeNotesRepository.createdNote)
            assertEquals("Judul Baru", fakeNotesRepository.createdNote?.title)
            assertTrue(viewModel.uiState.value.hasCreatedDocumentInFirestore)
            assertEquals(EditorSaveStatus.SAVED_CLOUD, viewModel.uiState.value.saveStatus)
        }

    @Test
    fun `test update existing note debounces 800ms`() =
        runTest(testDispatcher) {
            val existingNote =
                Note(
                    id = "note-existing",
                    type = NoteType.TEXT,
                    title = "Judul Lama",
                    body = "Isi Lama",
                )
            fakeNotesRepository.setExistingNote(existingNote)

            val viewModel =
                EditorViewModel(
                    notesRepository = fakeNotesRepository,
                    networkObserver = fakeNetworkObserver,
                    initialNoteId = "note-existing",
                    initialTypeStr = null,
                )
            advanceUntilIdle()

            viewModel.onBodyChange("Isi Diperbarui")
            assertEquals(EditorSaveStatus.SAVING, viewModel.uiState.value.saveStatus)
            assertNull(fakeNotesRepository.lastUpdateChanges)

            advanceTimeBy(400)
            assertNull(fakeNotesRepository.lastUpdateChanges)

            advanceTimeBy(450)
            assertNotNull(fakeNotesRepository.lastUpdateChanges)
            assertEquals("Isi Diperbarui", fakeNotesRepository.lastUpdateChanges?.body)
            assertEquals(EditorSaveStatus.SAVED_CLOUD, viewModel.uiState.value.saveStatus)
        }

    @Test
    fun `test flushAndCleanup saves immediately without waiting for debounce`() =
        runTest(testDispatcher) {
            val viewModel =
                EditorViewModel(
                    notesRepository = fakeNotesRepository,
                    networkObserver = fakeNetworkObserver,
                    initialNoteId = null,
                    initialTypeStr = "text",
                )

            viewModel.onTitleChange("Catatan Cepat")
            assertNull(fakeNotesRepository.createdNote)

            // Flush langsung dipanggil (misal back press atau onPause)
            viewModel.flushAndCleanup()

            assertNotNull(fakeNotesRepository.createdNote)
            assertEquals("Catatan Cepat", fakeNotesRepository.createdNote?.title)
        }

    @Test
    fun `test empty new note without edits is not created on exit`() =
        runTest(testDispatcher) {
            val viewModel =
                EditorViewModel(
                    notesRepository = fakeNotesRepository,
                    networkObserver = fakeNetworkObserver,
                    initialNoteId = null,
                    initialTypeStr = "text",
                )

            viewModel.flushAndCleanup()

            assertNull(fakeNotesRepository.createdNote)
            assertTrue(fakeNotesRepository.deletedNoteIds.isEmpty())
        }

    @Test
    fun `test empty note that was previously created gets deleted on exit`() =
        runTest(testDispatcher) {
            val viewModel =
                EditorViewModel(
                    notesRepository = fakeNotesRepository,
                    networkObserver = fakeNetworkObserver,
                    initialNoteId = null,
                    initialTypeStr = "text",
                )

            viewModel.onTitleChange("Draft Sementara")
            advanceTimeBy(850)
            assertNotNull(fakeNotesRepository.createdNote)

            // Pengguna menghapus seluruh judul
            viewModel.onTitleChange("")
            // Keluar dari editor
            viewModel.flushAndCleanup()

            assertTrue(fakeNotesRepository.deletedNoteIds.contains("generated-id"))
        }

    @Test
    fun `test convert between text and checklist modes`() =
        runTest(testDispatcher) {
            val viewModel =
                EditorViewModel(
                    notesRepository = fakeNotesRepository,
                    networkObserver = fakeNetworkObserver,
                    initialNoteId = null,
                    initialTypeStr = "text",
                )

            viewModel.onBodyChange("Beli beras\nBeli telur\nBeli susu")
            viewModel.onConvertType()

            val checklistState = viewModel.uiState.value
            assertEquals(NoteType.CHECKLIST, checklistState.type)
            assertEquals("", checklistState.body)
            assertEquals(3, checklistState.items.size)
            assertEquals("Beli beras", checklistState.items[0].text)
            assertEquals("Beli telur", checklistState.items[1].text)
            assertEquals("Beli susu", checklistState.items[2].text)

            // Konversi kembali ke teks
            viewModel.onConvertType()

            val textState = viewModel.uiState.value
            assertEquals(NoteType.TEXT, textState.type)
            assertEquals("Beli beras\nBeli telur\nBeli susu", textState.body)
            assertTrue(textState.items.isEmpty())
        }

    @Test
    fun `test checklist items manipulation and toggle`() =
        runTest(testDispatcher) {
            val viewModel =
                EditorViewModel(
                    notesRepository = fakeNotesRepository,
                    networkObserver = fakeNetworkObserver,
                    initialNoteId = null,
                    initialTypeStr = "checklist",
                )

            val firstItemId =
                viewModel.uiState.value.items[0]
                    .id
            viewModel.onItemTextChange(firstItemId, "Tugas pertama")

            // Tambah item kedua
            viewModel.onAddItem("Tugas kedua", afterIndex = 0)
            assertEquals(2, viewModel.uiState.value.items.size)
            assertEquals(
                "Tugas kedua",
                viewModel.uiState.value.items[1]
                    .text,
            )

            // Centang item pertama
            viewModel.onToggleItem(firstItemId)
            assertTrue(
                viewModel.uiState.value.items[0]
                    .checked,
            )
            assertFalse(
                viewModel.uiState.value.items[1]
                    .checked,
            )

            // Hapus item kedua
            val secondItemId =
                viewModel.uiState.value.items[1]
                    .id
            viewModel.onDeleteItem(secondItemId)
            assertEquals(1, viewModel.uiState.value.items.size)
            assertEquals(
                firstItemId,
                viewModel.uiState.value.items[0]
                    .id,
            )
        }

    @Test
    fun `test offline status results in SAVED_LOCAL`() =
        runTest(testDispatcher) {
            fakeNetworkObserver.setConnected(false)

            val viewModel =
                EditorViewModel(
                    notesRepository = fakeNotesRepository,
                    networkObserver = fakeNetworkObserver,
                    initialNoteId = null,
                    initialTypeStr = "text",
                )
            advanceUntilIdle()

            viewModel.onTitleChange("Catatan Offline")
            advanceTimeBy(850)

            assertEquals(EditorSaveStatus.SAVED_LOCAL, viewModel.uiState.value.saveStatus)
        }

    @Test
    fun `test offline checklist creation and editing saves locally without data loss`() =
        runTest(testDispatcher) {
            fakeNetworkObserver.setConnected(false)

            val viewModel =
                EditorViewModel(
                    notesRepository = fakeNotesRepository,
                    networkObserver = fakeNetworkObserver,
                    initialNoteId = null,
                    initialTypeStr = "checklist",
                )
            advanceUntilIdle()

            viewModel.onTitleChange("Daftar Belanja Offline")
            val firstItemId = viewModel.uiState.value.items[0].id
            viewModel.onItemTextChange(firstItemId, "Beras 5kg")
            viewModel.onAddItem("Minyak Goreng 2L", afterIndex = 0)
            val secondItemId = viewModel.uiState.value.items[1].id
            viewModel.onToggleItem(secondItemId)

            advanceTimeBy(850)

            assertEquals(EditorSaveStatus.SAVED_LOCAL, viewModel.uiState.value.saveStatus)
            assertNotNull(fakeNotesRepository.createdNote)
            val created = fakeNotesRepository.createdNote!!
            assertEquals("Daftar Belanja Offline", created.title)
            assertEquals(2, created.items.size)
            assertEquals("Beras 5kg", created.items[0].text)
            assertFalse(created.items[0].checked)
            assertEquals("Minyak Goreng 2L", created.items[1].text)
            assertTrue(created.items[1].checked)
        }

    @Test
    fun `test reconnecting online updates editor save status to SAVED_CLOUD`() =
        runTest(testDispatcher) {
            fakeNetworkObserver.setConnected(false)

            val viewModel =
                EditorViewModel(
                    notesRepository = fakeNotesRepository,
                    networkObserver = fakeNetworkObserver,
                    initialNoteId = null,
                    initialTypeStr = "text",
                )
            advanceUntilIdle()

            viewModel.onTitleChange("Catatan Offline ke Online")
            advanceTimeBy(850)
            assertEquals(EditorSaveStatus.SAVED_LOCAL, viewModel.uiState.value.saveStatus)

            // Jaringan aktif kembali (mode pesawat dimatikan)
            fakeNetworkObserver.setConnected(true)
            advanceUntilIdle()

            assertEquals(EditorSaveStatus.SAVED_CLOUD, viewModel.uiState.value.saveStatus)
        }

    private class FakeNotesRepository : NotesRepository {
        var createdNote: Note? = null
        var lastUpdateId: String? = null
        var lastUpdateChanges: NoteChanges? = null
        val deletedNoteIds = mutableListOf<String>()
        private val existingNotes = mutableMapOf<String, Note>()
        private val notesFlow = MutableStateFlow<Map<String, Note>>(emptyMap())

        fun setExistingNote(note: Note) {
            existingNotes[note.id] = note
            notesFlow.value = existingNotes
        }

        override fun observeNotes(status: NoteStatus): Flow<List<Note>> = MutableStateFlow(existingNotes.values.toList())

        override fun observeNote(id: String): Flow<Note?> = MutableStateFlow(existingNotes[id])

        override fun newId(): String = "generated-id"

        override fun create(note: Note) {
            createdNote = note
            existingNotes[note.id] = note
            notesFlow.value = existingNotes
        }

        override fun update(
            id: String,
            changes: NoteChanges,
        ) {
            lastUpdateId = id
            lastUpdateChanges = changes
        }

        override fun setPinned(
            ids: List<String>,
            pinned: Boolean,
        ) {}

        override fun setStatus(
            ids: List<String>,
            status: NoteStatus,
        ) {}

        override fun deletePermanently(ids: List<String>) {
            deletedNoteIds.addAll(ids)
        }

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
}
