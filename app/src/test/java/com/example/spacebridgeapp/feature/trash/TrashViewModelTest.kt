package com.example.spacebridgeapp.feature.trash

import android.app.Activity
import android.content.Context
import android.net.Uri
import com.example.spacebridgeapp.core.data.auth.AuthRepository
import com.example.spacebridgeapp.core.data.files.FilesRepository
import com.example.spacebridgeapp.core.data.files.ReconcileReport
import com.example.spacebridgeapp.core.data.files.StorageSummary
import com.example.spacebridgeapp.core.data.files.UploadEnqueueResult
import com.example.spacebridgeapp.core.data.notes.NoteChanges
import com.example.spacebridgeapp.core.data.notes.NotesRepository
import com.example.spacebridgeapp.core.model.AppUser
import com.example.spacebridgeapp.core.model.DriveFile
import com.example.spacebridgeapp.core.model.FileCategory
import com.example.spacebridgeapp.core.model.FileSource
import com.example.spacebridgeapp.core.model.FileStatus
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
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalCoroutinesApi::class)
class TrashViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeNotesRepository: FakeNotesRepository
    private lateinit var fakeFilesRepository: FakeFilesRepository
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var viewModel: TrashViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeNotesRepository = FakeNotesRepository()
        fakeFilesRepository = FakeFilesRepository()
        fakeAuthRepository = FakeAuthRepository()

        viewModel =
            TrashViewModel(
                notesRepository = fakeNotesRepository,
                filesRepository = fakeFilesRepository,
                authRepository = fakeAuthRepository,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createTrashedNote(
        id: String,
        title: String,
    ): Note =
        Note(
            id = id,
            type = NoteType.TEXT,
            title = title,
            body = "Konten $title",
            items = emptyList(),
            pinned = false,
            labelIds = emptyList(),
            attachmentIds = emptyList(),
            status = NoteStatus.TRASHED,
            trashedAt = Instant.now().minus(2, ChronoUnit.DAYS),
            createdAt = Instant.now().minus(5, ChronoUnit.DAYS),
            updatedAt = Instant.now().minus(2, ChronoUnit.DAYS),
        )

    private fun createTrashedFile(
        id: String,
        name: String,
    ): DriveFile =
        DriveFile(
            id = id,
            name = name,
            mimeType = "application/pdf",
            sizeBytes = 2048L,
            category = FileCategory.DOCUMENT,
            source = FileSource.DRIVE,
            driveFileId = "drive-$id",
            webViewLink = "https://drive.google.com/file/d/drive-$id/view",
            status = FileStatus.TRASHED,
            trashedAt = Instant.now().minus(3, ChronoUnit.DAYS),
            createdAt = Instant.now().minus(10, ChronoUnit.DAYS),
            updatedAt = Instant.now().minus(3, ChronoUnit.DAYS),
        )

    @Test
    fun `test initial state reflects trashed items and defaults to notes tab`() =
        runTest(testDispatcher) {
            val note1 = createTrashedNote("n1", "Rencana Liburan")
            val note2 = createTrashedNote("n2", "Daftar Belanja")
            val file1 = createTrashedFile("f1", "Laporan.pdf")

            fakeNotesRepository.emitNotes(listOf(note1, note2))
            fakeFilesRepository.emitFiles(listOf(file1))

            val collectJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.uiState.collect()
                }
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(TrashTab.NOTES, state.selectedTab)
            assertEquals(2, state.trashedNotes.size)
            assertEquals(1, state.trashedFiles.size)
            assertEquals(2, state.notesCount)
            assertEquals(1, state.filesCount)
            assertEquals("test@example.com", state.user?.email)

            collectJob.cancel()
        }

    @Test
    fun `test tab switching updates selectedTab`() =
        runTest(testDispatcher) {
            val collectJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.uiState.collect()
                }
            advanceUntilIdle()

            viewModel.onSelectTab(TrashTab.DRIVE_FILES)
            advanceUntilIdle()
            assertEquals(TrashTab.DRIVE_FILES, viewModel.uiState.value.selectedTab)

            viewModel.onSelectTab(TrashTab.NOTES)
            advanceUntilIdle()
            assertEquals(TrashTab.NOTES, viewModel.uiState.value.selectedTab)

            collectJob.cancel()
        }

    @Test
    fun `test restore note changes status to ACTIVE and emits event`() =
        runTest(testDispatcher) {
            val events = mutableListOf<TrashEvent>()
            val eventJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.events.collect { events.add(it) }
                }

            viewModel.onRestoreNote("n1", "Catatan Kuliah")
            advanceUntilIdle()

            assertEquals(listOf("n1"), fakeNotesRepository.lastRestoredIds)
            assertEquals(NoteStatus.ACTIVE, fakeNotesRepository.lastRestoredStatus)
            assertEquals(1, events.size)
            assertTrue(events.first() is TrashEvent.ShowSnackbar)
            assertEquals("\"Catatan Kuliah\" telah dipulihkan", (events.first() as TrashEvent.ShowSnackbar).message)

            eventJob.cancel()
        }

    @Test
    fun `test delete note permanently calls repository and emits event`() =
        runTest(testDispatcher) {
            val events = mutableListOf<TrashEvent>()
            val eventJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.events.collect { events.add(it) }
                }

            viewModel.onDeleteNotePermanently("n2", "Draft Ide")
            advanceUntilIdle()

            assertEquals(listOf("n2"), fakeNotesRepository.lastPermanentlyDeletedIds)
            assertEquals(1, events.size)
            assertEquals("\"Draft Ide\" dihapus permanen", (events.first() as TrashEvent.ShowSnackbar).message)

            eventJob.cancel()
        }

    @Test
    fun `test restore file calls repository and emits event`() =
        runTest(testDispatcher) {
            val events = mutableListOf<TrashEvent>()
            val eventJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.events.collect { events.add(it) }
                }

            viewModel.onRestoreFile("f1", "Dokumen.pdf")
            advanceUntilIdle()

            assertEquals("f1", fakeFilesRepository.lastRestoredId)
            assertEquals(1, events.size)
            assertEquals("\"Dokumen.pdf\" telah dipulihkan", (events.first() as TrashEvent.ShowSnackbar).message)

            eventJob.cancel()
        }

    @Test
    fun `test delete file permanently deletes from Drive and removes attachments from notes`() =
        runTest(testDispatcher) {
            val events = mutableListOf<TrashEvent>()
            val eventJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.events.collect { events.add(it) }
                }

            viewModel.onDeleteFilePermanently("f2", "Foto.png")
            advanceUntilIdle()

            assertEquals("f2", fakeFilesRepository.lastPermanentlyDeletedId)
            assertEquals("f2", fakeNotesRepository.lastRemovedAttachmentId)
            assertEquals(1, events.size)
            assertEquals("\"Foto.png\" dihapus permanen", (events.first() as TrashEvent.ShowSnackbar).message)

            eventJob.cancel()
        }

    @Test
    fun `test empty trash for current notes tab deletes all trashed notes`() =
        runTest(testDispatcher) {
            val n1 = createTrashedNote("n1", "Note 1")
            val n2 = createTrashedNote("n2", "Note 2")
            fakeNotesRepository.emitNotes(listOf(n1, n2))

            val collectJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.uiState.collect()
                }
            advanceUntilIdle()

            viewModel.onSelectTab(TrashTab.NOTES)
            viewModel.onEmptyTrash(currentTabOnly = true)
            advanceUntilIdle()

            assertEquals(listOf("n1", "n2"), fakeNotesRepository.lastPermanentlyDeletedIds)

            collectJob.cancel()
        }

    @Test
    fun `test empty trash for current drive tab deletes all trashed files`() =
        runTest(testDispatcher) {
            val f1 = createTrashedFile("f1", "File 1")
            val f2 = createTrashedFile("f2", "File 2")
            fakeFilesRepository.emitFiles(listOf(f1, f2))

            val collectJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.uiState.collect()
                }
            advanceUntilIdle()

            viewModel.onSelectTab(TrashTab.DRIVE_FILES)
            advanceUntilIdle()
            viewModel.onEmptyTrash(currentTabOnly = true)
            advanceUntilIdle()

            assertEquals(listOf("f1", "f2"), fakeFilesRepository.allPermanentlyDeletedIds)
            assertEquals(listOf("f1", "f2"), fakeNotesRepository.allRemovedAttachmentIds)

            collectJob.cancel()
        }

    @Test
    fun `test empty all trash deletes both notes and files`() =
        runTest(testDispatcher) {
            val n1 = createTrashedNote("n1", "Note 1")
            val f1 = createTrashedFile("f1", "File 1")
            fakeNotesRepository.emitNotes(listOf(n1))
            fakeFilesRepository.emitFiles(listOf(f1))

            val collectJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.uiState.collect()
                }
            advanceUntilIdle()

            viewModel.onEmptyTrash(currentTabOnly = false)
            advanceUntilIdle()

            assertEquals(listOf("n1"), fakeNotesRepository.lastPermanentlyDeletedIds)
            assertEquals(listOf("f1"), fakeFilesRepository.allPermanentlyDeletedIds)

            collectJob.cancel()
        }

    @Test
    fun `test auto purge expired trash triggers 30 day cutoff on repositories`() =
        runTest(testDispatcher) {
            viewModel.purgeExpiredTrash(days = 30)
            advanceUntilIdle()

            assertNotNull(fakeNotesRepository.lastPurgeCutoff)
            assertNotNull(fakeFilesRepository.lastPurgeCutoff)

            val expectedApproxCutoff = Instant.now().minus(30, ChronoUnit.DAYS)
            val notesDelta = Math.abs(fakeNotesRepository.lastPurgeCutoff!!.epochSecond - expectedApproxCutoff.epochSecond)
            val filesDelta = Math.abs(fakeFilesRepository.lastPurgeCutoff!!.epochSecond - expectedApproxCutoff.epochSecond)

            assertTrue("Notes purge cutoff within 5 seconds", notesDelta < 5)
            assertTrue("Files purge cutoff within 5 seconds", filesDelta < 5)
        }

    // --- Fake implementations ---

    private class FakeNotesRepository : NotesRepository {
        private val notesFlow = MutableStateFlow<List<Note>>(emptyList())
        var lastRestoredIds: List<String>? = null
        var lastRestoredStatus: NoteStatus? = null
        var lastPermanentlyDeletedIds: List<String>? = null
        var lastRemovedAttachmentId: String? = null
        val allRemovedAttachmentIds = mutableListOf<String>()
        var lastPurgeCutoff: Instant? = null

        fun emitNotes(notes: List<Note>) {
            notesFlow.value = notes
        }

        override fun observeNotes(status: NoteStatus): Flow<List<Note>> = notesFlow

        override fun observeNote(id: String): Flow<Note?> = MutableStateFlow(null)

        override fun newId(): String = "note-id"

        override fun create(note: Note) {}

        override fun update(
            id: String,
            changes: NoteChanges,
        ) {}

        override fun setPinned(
            ids: List<String>,
            pinned: Boolean,
        ) {}

        override fun setStatus(
            ids: List<String>,
            status: NoteStatus,
        ) {
            lastRestoredIds = ids
            lastRestoredStatus = status
        }

        override fun deletePermanently(ids: List<String>) {
            lastPermanentlyDeletedIds = ids
        }

        override fun addLabel(
            ids: List<String>,
            labelId: String,
        ) {}

        override suspend fun purgeTrashOlderThan(cutoff: Instant) {
            lastPurgeCutoff = cutoff
        }

        override suspend fun removeAttachmentEverywhere(fileId: String) {
            lastRemovedAttachmentId = fileId
            allRemovedAttachmentIds.add(fileId)
        }

        override suspend fun removeLabelEverywhere(labelId: String) {}
    }

    private class FakeFilesRepository : FilesRepository {
        private val filesFlow = MutableStateFlow<List<DriveFile>>(emptyList())
        var lastRestoredId: String? = null
        var lastPermanentlyDeletedId: String? = null
        val allPermanentlyDeletedIds = mutableListOf<String>()
        var lastPurgeCutoff: Instant? = null

        fun emitFiles(files: List<DriveFile>) {
            filesFlow.value = files
        }

        override fun observeFiles(trashed: Boolean): Flow<List<DriveFile>> = filesFlow

        override fun observeFile(id: String): Flow<DriveFile?> = MutableStateFlow(null)

        override suspend fun enqueueUploads(uris: List<Uri>): UploadEnqueueResult = UploadEnqueueResult(0, 0)

        override fun cancelUpload(id: String) {}

        override fun retryUpload(id: String) {}

        override suspend fun rename(
            id: String,
            newName: String,
        ): Result<Unit> = Result.success(Unit)

        override suspend fun moveToTrash(id: String): Result<Unit> = Result.success(Unit)

        override suspend fun restore(id: String): Result<Unit> {
            lastRestoredId = id
            return Result.success(Unit)
        }

        override suspend fun deletePermanently(id: String): Result<Unit> {
            lastPermanentlyDeletedId = id
            allPermanentlyDeletedIds.add(id)
            return Result.success(Unit)
        }

        override suspend fun share(id: String): Result<String> = Result.success("")

        override suspend fun unshare(id: String): Result<Unit> = Result.success(Unit)

        override suspend fun openLocalCopy(
            id: String,
            onProgress: (Float) -> Unit,
        ): Result<Uri> = Result.failure(Exception())

        override suspend fun saveToDownloads(id: String): Result<Unit> = Result.success(Unit)

        override suspend fun reconcile(force: Boolean): Result<ReconcileReport> = Result.success(ReconcileReport(0, 0, 0))

        override fun observeUploadProgress(): Flow<Map<String, Float>> = MutableStateFlow(emptyMap())

        override suspend fun storageSummary(): StorageSummary = StorageSummary(0, 0L)

        override suspend fun purgeTrashOlderThan(cutoff: Instant) {
            lastPurgeCutoff = cutoff
        }
    }

    private class FakeAuthRepository : AuthRepository {
        private val _currentUser =
            MutableStateFlow<AppUser?>(
                AppUser("user-1", "Tito Pamungkas", "test@example.com", null),
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
