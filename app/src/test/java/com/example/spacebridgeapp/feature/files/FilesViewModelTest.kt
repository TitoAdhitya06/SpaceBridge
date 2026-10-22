package com.example.spacebridgeapp.feature.files

import android.app.Activity
import android.content.Context
import android.net.Uri
import com.example.spacebridgeapp.core.data.auth.AuthRepository
import com.example.spacebridgeapp.core.data.drive.DriveAuthManager
import com.example.spacebridgeapp.core.data.files.FilesRepository
import com.example.spacebridgeapp.core.data.files.ReconcileReport
import com.example.spacebridgeapp.core.data.files.StorageSummary
import com.example.spacebridgeapp.core.data.files.UploadEnqueueResult
import com.example.spacebridgeapp.core.data.network.NetworkObserver
import com.example.spacebridgeapp.core.model.AppUser
import com.example.spacebridgeapp.core.model.DriveFile
import com.example.spacebridgeapp.core.model.FileCategory
import com.example.spacebridgeapp.core.model.FileSource
import com.example.spacebridgeapp.core.model.FileStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class FilesViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeFilesRepository: FakeFilesRepository
    private lateinit var fakeDriveAuthManager: FakeDriveAuthManager
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var fakeNetworkObserver: FakeNetworkObserver
    private lateinit var viewModel: FilesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeFilesRepository = FakeFilesRepository()
        fakeDriveAuthManager = FakeDriveAuthManager()
        fakeAuthRepository = FakeAuthRepository()
        fakeNetworkObserver = FakeNetworkObserver()

        viewModel =
            FilesViewModel(
                filesRepository = fakeFilesRepository,
                driveAuthManager = fakeDriveAuthManager,
                authRepository = fakeAuthRepository,
                networkObserver = fakeNetworkObserver,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test initial state reflects files, email, and authorization`() =
        runTest(testDispatcher) {
            val file1 = createFile(id = "f1", name = "dokumen.pdf", category = FileCategory.DOCUMENT)
            val file2 = createFile(id = "f2", name = "gambar.png", category = FileCategory.IMAGE)
            fakeFilesRepository.emitFiles(listOf(file1, file2))
            fakeFilesRepository.summary = StorageSummary(2, 3000L)
            viewModel.refresh()
            advanceUntilIdle()

            val collectJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.uiState.collect()
                }
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(2, state.files.size)
            assertEquals(2, state.filteredFiles.size)
            assertEquals("test@example.com", state.userEmail)
            assertTrue(state.isDriveAuthorized)
            assertEquals(2, state.storageSummary.fileCount)
            assertEquals(3000L, state.storageSummary.totalSizeBytes)

            collectJob.cancel()
        }

    @Test
    fun `test search query filters files case-insensitively`() =
        runTest(testDispatcher) {
            val file1 = createFile(id = "f1", name = "Laporan_Keuangan.pdf")
            val file2 = createFile(id = "f2", name = "Foto_Kegiatan.jpg")
            val file3 = createFile(id = "f3", name = "keuangan_revisi.xlsx")
            fakeFilesRepository.emitFiles(listOf(file1, file2, file3))

            val collectJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.uiState.collect()
                }
            advanceUntilIdle()

            viewModel.onSearchQueryChanged("keuangan")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(2, state.filteredFiles.size)
            assertTrue(state.filteredFiles.any { it.id == "f1" })
            assertTrue(state.filteredFiles.any { it.id == "f3" })

            collectJob.cancel()
        }

    @Test
    fun `test category filter filters by FileCategory`() =
        runTest(testDispatcher) {
            val file1 = createFile(id = "f1", name = "f1.pdf", category = FileCategory.DOCUMENT)
            val file2 = createFile(id = "f2", name = "f2.png", category = FileCategory.IMAGE)
            val file3 = createFile(id = "f3", name = "f3.xlsx", category = FileCategory.SPREADSHEET)
            fakeFilesRepository.emitFiles(listOf(file1, file2, file3))

            val collectJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.uiState.collect()
                }
            advanceUntilIdle()

            viewModel.onCategorySelected(FileCategory.DOCUMENT)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(1, state.filteredFiles.size)
            assertEquals("f1", state.filteredFiles.first().id)

            viewModel.onCategorySelected(null)
            advanceUntilIdle()
            assertEquals(3, viewModel.uiState.value.filteredFiles.size)

            collectJob.cancel()
        }

    @Test
    fun `test active upload progress and banner state`() =
        runTest(testDispatcher) {
            val uploadingFile =
                createFile(
                    id = "upload-1",
                    name = "video.mp4",
                    status = FileStatus.UPLOADING,
                )
            fakeFilesRepository.emitFiles(listOf(uploadingFile))
            fakeFilesRepository.emitProgress(mapOf("upload-1" to 0.75f))

            val collectJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.uiState.collect()
                }
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertNotNull(state.activeUploadingFile)
            assertEquals("upload-1", state.activeUploadingFile?.id)
            assertEquals(0.75f, state.activeUploadProgress)

            collectJob.cancel()
        }

    @Test
    fun `test cancelUpload calls repository and emits snackbar`() =
        runTest(testDispatcher) {
            val collectJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.uiState.collect()
                }
            advanceUntilIdle()

            var emittedEvent: FilesEvent? = null
            val eventJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.events.collect { emittedEvent = it }
                }

            viewModel.cancelUpload("upload-1")
            advanceUntilIdle()

            assertEquals("upload-1", fakeFilesRepository.lastCanceledId)
            assertTrue(emittedEvent is FilesEvent.ShowSnackbar)

            collectJob.cancel()
            eventJob.cancel()
        }

    @Test
    fun `test retryUpload calls repository and emits snackbar`() =
        runTest(testDispatcher) {
            val collectJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.uiState.collect()
                }
            advanceUntilIdle()

            var emittedEvent: FilesEvent? = null
            val eventJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.events.collect { emittedEvent = it }
                }

            viewModel.retryUpload("upload-failed")
            advanceUntilIdle()

            assertEquals("upload-failed", fakeFilesRepository.lastRetriedId)
            assertTrue(emittedEvent is FilesEvent.ShowSnackbar)

            collectJob.cancel()
            eventJob.cancel()
        }

    @Test
    fun `test renameFile updates name and closes rename state`() =
        runTest(testDispatcher) {
            val file = createFile(id = "f-rename", name = "old_name.pdf")
            fakeFilesRepository.emitFiles(listOf(file))

            val collectJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.uiState.collect()
                }
            advanceUntilIdle()

            viewModel.onFileToRenameSelected(file)
            advanceUntilIdle()
            assertEquals(file, viewModel.uiState.value.fileToRename)

            viewModel.renameFile("f-rename", "new_name.pdf")
            advanceUntilIdle()

            assertEquals("f-rename" to "new_name.pdf", fakeFilesRepository.lastRenamed)
            assertNull(viewModel.uiState.value.fileToRename)

            collectJob.cancel()
        }

    @Test
    fun `test moveToTrash calls repository and clears selection`() =
        runTest(testDispatcher) {
            val file = createFile(id = "f-trash", name = "trash_me.pdf")
            fakeFilesRepository.emitFiles(listOf(file))

            val collectJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.uiState.collect()
                }
            advanceUntilIdle()

            viewModel.onFileSelectedForActions(file)
            viewModel.onFileToTrashSelected(file)
            advanceUntilIdle()

            viewModel.moveToTrash("f-trash")
            advanceUntilIdle()

            assertEquals("f-trash", fakeFilesRepository.lastTrashedId)
            assertNull(viewModel.uiState.value.fileToTrash)
            assertNull(viewModel.uiState.value.selectedFileForActions)

            collectJob.cancel()
        }

    @Test
    fun `test shareFile emits ShareLink event with webViewLink`() =
        runTest(testDispatcher) {
            var emittedEvent: FilesEvent? = null
            val eventJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.events.collect { emittedEvent = it }
                }

            viewModel.shareFile("file-share-123")
            advanceUntilIdle()

            assertEquals("file-share-123", fakeFilesRepository.lastSharedId)
            assertTrue(emittedEvent is FilesEvent.ShareLink)
            assertEquals("https://drive.google.com/file/d/share-123/view", (emittedEvent as FilesEvent.ShareLink).url)

            eventJob.cancel()
        }

    @Test
    fun `test saveToDownloads calls repository and emits snackbar`() =
        runTest(testDispatcher) {
            var emittedEvent: FilesEvent? = null
            val eventJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.events.collect { emittedEvent = it }
                }

            viewModel.saveToDownloads("file-dl-456")
            advanceUntilIdle()

            assertEquals("file-dl-456", fakeFilesRepository.lastDownloadedId)
            assertTrue(emittedEvent is FilesEvent.ShowSnackbar)

            eventJob.cancel()
        }

    @Test
    fun `test openLocalCopy emits OpenUri on success`() =
        runTest(testDispatcher) {
            val file = createFile(id = "f-preview", name = "preview.pdf", mimeType = "application/pdf")
            var emittedEvent: FilesEvent? = null
            val eventJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.events.collect { emittedEvent = it }
                }

            viewModel.openLocalCopy(file)
            advanceUntilIdle()

            assertEquals("f-preview", fakeFilesRepository.lastOpenedId)
            assertTrue(emittedEvent is FilesEvent.OpenUri)
            val openEvent = emittedEvent as FilesEvent.OpenUri
            assertEquals("application/pdf", openEvent.mimeType)

            eventJob.cancel()
        }

    @Test
    fun `test openInGoogleDrive emits OpenWebLink event`() =
        runTest(testDispatcher) {
            val file =
                createFile(
                    id = "f-drive",
                    webViewLink = "https://drive.google.com/view/123",
                )
            var emittedEvent: FilesEvent? = null
            val eventJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.events.collect { emittedEvent = it }
                }

            viewModel.openInGoogleDrive(file)
            advanceUntilIdle()

            assertTrue(emittedEvent is FilesEvent.OpenWebLink)
            assertEquals("https://drive.google.com/view/123", (emittedEvent as FilesEvent.OpenWebLink).url)

            eventJob.cancel()
        }

    @Test
    fun `test connectDrive when granted emits success snackbar`() =
        runTest(testDispatcher) {
            fakeDriveAuthManager.authorizeResult =
                com.example.spacebridgeapp.core.data.drive.DriveAuthResult.Granted("access-token-123")

            var emittedEvent: FilesEvent? = null
            val eventJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.events.collect { event -> emittedEvent = event }
                }

            viewModel.connectDrive()
            advanceUntilIdle()

            assertTrue(emittedEvent is FilesEvent.ShowSnackbar)
            assertEquals("Akses Google Drive berhasil terhubung", (emittedEvent as FilesEvent.ShowSnackbar).message)

            eventJob.cancel()
        }

    @Test
    fun `test connectDrive when failed emits error snackbar`() =
        runTest(testDispatcher) {
            fakeDriveAuthManager.authorizeResult =
                com.example.spacebridgeapp.core.data.drive.DriveAuthResult.Failed("Koneksi gagal")

            var emittedEvent: FilesEvent? = null
            val eventJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.events.collect { event -> emittedEvent = event }
                }

            viewModel.connectDrive()
            advanceUntilIdle()

            assertTrue(emittedEvent is FilesEvent.ShowSnackbar)
            assertEquals("Gagal menghubungkan Google Drive: Koneksi gagal", (emittedEvent as FilesEvent.ShowSnackbar).message)

            eventJob.cancel()
        }

    @Test
    fun `test handleConsentResult when cancelled emits cancelled snackbar`() =
        runTest(testDispatcher) {
            var emittedEvent: FilesEvent? = null
            val eventJob =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.events.collect { event -> emittedEvent = event }
                }

            viewModel.handleConsentResult(Activity.RESULT_CANCELED, null)
            advanceUntilIdle()

            assertTrue(emittedEvent is FilesEvent.ShowSnackbar)
            assertEquals("Izin akses Google Drive dibatalkan", (emittedEvent as FilesEvent.ShowSnackbar).message)

            eventJob.cancel()
        }

    private fun createFile(
        id: String,
        name: String = "file.txt",
        category: FileCategory = FileCategory.OTHER,
        status: FileStatus = FileStatus.READY,
        mimeType: String = "text/plain",
        webViewLink: String? = null,
    ): DriveFile =
        DriveFile(
            id = id,
            name = name,
            mimeType = mimeType,
            sizeBytes = 1024L,
            category = category,
            source = FileSource.DRIVE,
            driveFileId = "drive-$id",
            webViewLink = webViewLink,
            externalUrl = null,
            status = status,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )

    private class FakeFilesRepository : FilesRepository {
        private val filesFlow = MutableStateFlow<List<DriveFile>>(emptyList())
        private val progressFlow = MutableStateFlow<Map<String, Float>>(emptyMap())
        var summary = StorageSummary(0, 0L)

        var lastCanceledId: String? = null
        var lastRetriedId: String? = null
        var lastRenamed: Pair<String, String>? = null
        var lastTrashedId: String? = null
        var lastSharedId: String? = null
        var lastDownloadedId: String? = null
        var lastOpenedId: String? = null

        fun emitFiles(files: List<DriveFile>) {
            filesFlow.value = files
        }

        fun emitProgress(progress: Map<String, Float>) {
            progressFlow.value = progress
        }

        override fun observeFiles(trashed: Boolean): Flow<List<DriveFile>> = filesFlow

        override fun observeFile(id: String): Flow<DriveFile?> = MutableStateFlow(filesFlow.value.find { it.id == id })

        override suspend fun enqueueUploads(uris: List<Uri>): UploadEnqueueResult = UploadEnqueueResult(uris.size, 0)

        override fun cancelUpload(id: String) {
            lastCanceledId = id
        }

        override fun retryUpload(id: String) {
            lastRetriedId = id
        }

        override suspend fun rename(
            id: String,
            newName: String,
        ): Result<Unit> {
            lastRenamed = id to newName
            return Result.success(Unit)
        }

        override suspend fun moveToTrash(id: String): Result<Unit> {
            lastTrashedId = id
            return Result.success(Unit)
        }

        override suspend fun restore(id: String): Result<Unit> = Result.success(Unit)

        override suspend fun deletePermanently(id: String): Result<Unit> = Result.success(Unit)

        override suspend fun share(id: String): Result<String> {
            lastSharedId = id
            return Result.success("https://drive.google.com/file/d/share-123/view")
        }

        override suspend fun unshare(id: String): Result<Unit> = Result.success(Unit)

        override suspend fun openLocalCopy(
            id: String,
            onProgress: (Float) -> Unit,
        ): Result<Uri> {
            lastOpenedId = id
            return Result.success(android.net.TestUri("content://test/preview.pdf"))
        }

        override suspend fun saveToDownloads(id: String): Result<Unit> {
            lastDownloadedId = id
            return Result.success(Unit)
        }

        override suspend fun reconcile(force: Boolean): Result<ReconcileReport> = Result.success(ReconcileReport(0, 0, 0))

        override fun observeUploadProgress(): Flow<Map<String, Float>> = progressFlow

        override suspend fun storageSummary(): StorageSummary = summary

        override suspend fun purgeTrashOlderThan(cutoff: Instant) {}
    }

    private class FakeDriveAuthManager : DriveAuthManager {
        private val _isAuthorized = MutableStateFlow(true)
        override val isAuthorized: StateFlow<Boolean> = _isAuthorized.asStateFlow()

        var authorizeResult: com.example.spacebridgeapp.core.data.drive.DriveAuthResult =
            com.example.spacebridgeapp.core.data.drive.DriveAuthResult.Granted("fake-token")

        var consentResult: com.example.spacebridgeapp.core.data.drive.DriveAuthResult =
            com.example.spacebridgeapp.core.data.drive.DriveAuthResult.Granted("fake-token")

        override suspend fun authorize(): com.example.spacebridgeapp.core.data.drive.DriveAuthResult = authorizeResult

        override suspend fun authorize(activity: Activity): com.example.spacebridgeapp.core.data.drive.DriveAuthResult = authorizeResult

        override suspend fun handleConsentResult(
            data: android.content.Intent?,
        ): com.example.spacebridgeapp.core.data.drive.DriveAuthResult = consentResult

        override suspend fun getTokenSilently(): String? = "fake-token"

        override suspend fun invalidateToken(token: String) {}

        override suspend fun revoke() {
            _isAuthorized.value = false
        }
    }

    private class FakeNetworkObserver : NetworkObserver {
        private val _isConnected = MutableStateFlow(true)
        override val isConnected: Flow<Boolean> = _isConnected.asStateFlow()
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
