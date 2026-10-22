package com.example.spacebridgeapp.core.data.files

import android.content.Context
import android.net.Uri
import com.example.spacebridgeapp.core.data.drive.DriveFileItem
import com.example.spacebridgeapp.core.data.drive.DriveFileList
import com.example.spacebridgeapp.core.data.drive.DriveRepository
import com.example.spacebridgeapp.core.data.drive.FakePreferencesRepository
import com.example.spacebridgeapp.core.data.drive.StorageQuota
import com.example.spacebridgeapp.core.model.DriveFile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import sun.misc.Unsafe

class FilesRepositoryTest {
    private lateinit var dummyContext: Context
    private lateinit var dummyAuth: FirebaseAuth
    private lateinit var fakePreferencesRepository: FakePreferencesRepository
    private lateinit var fakeDriveRepository: FakeDriveRepository
    private lateinit var repository: FilesRepositoryImpl

    @Before
    fun setUp() {
        val field = Unsafe::class.java.getDeclaredField("theUnsafe").apply { isAccessible = true }
        val unsafe = field.get(null) as Unsafe
        dummyContext = unsafe.allocateInstance(android.content.ContextWrapper::class.java) as Context
        dummyAuth = unsafe.allocateInstance(FirebaseAuth::class.java) as FirebaseAuth
        fakePreferencesRepository = FakePreferencesRepository()
        fakeDriveRepository = FakeDriveRepository()

        repository =
            FilesRepositoryImpl(
                context = dummyContext,
                auth = dummyAuth,
                firestoreProvider = { unsafe.allocateInstance(FirebaseFirestore::class.java) as FirebaseFirestore },
                preferencesRepository = fakePreferencesRepository,
                driveRepository = fakeDriveRepository,
            )
    }

    @Test
    fun `test calculateRemainingQuota computes available bytes correctly`() {
        val quotaNormal = StorageQuota(limit = 15L * 1024 * 1024 * 1024, usage = 5L * 1024 * 1024 * 1024)
        assertEquals(10L * 1024 * 1024 * 1024, FilesRepositoryImpl.calculateRemainingQuota(quotaNormal))

        val quotaFull = StorageQuota(limit = 1000L, usage = 1500L)
        assertEquals(0L, FilesRepositoryImpl.calculateRemainingQuota(quotaFull))

        val quotaUnlimited = StorageQuota(limit = null, usage = 1000L)
        assertNull(FilesRepositoryImpl.calculateRemainingQuota(quotaUnlimited))

        assertNull(FilesRepositoryImpl.calculateRemainingQuota(null))
    }

    @Test
    fun `test enqueueUploads returns zero queued when user is not authenticated`() =
        runTest {
            val uris =
                listOf(
                    Uri.parse("content://media/1"),
                    Uri.parse("content://media/2"),
                )
            val result = repository.enqueueUploads(uris)
            assertEquals(0, result.queuedCount)
            assertEquals(2, result.failedCount)
        }

    @Test
    fun `test observeFiles emits empty list when unauthenticated`() =
        runTest {
            val activeFiles = repository.observeFiles(trashed = false).first()
            assertEquals(emptyList<DriveFile>(), activeFiles)

            val trashedFiles = repository.observeFiles(trashed = true).first()
            assertEquals(emptyList<DriveFile>(), trashedFiles)
        }

    @Test
    fun `test observeFile emits null when unauthenticated`() =
        runTest {
            val file = repository.observeFile("file-123").first()
            assertNull(file)
        }

    @Test
    fun `test storageSummary returns zero when unauthenticated`() =
        runTest {
            val summary = repository.storageSummary()
            assertEquals(0, summary.fileCount)
            assertEquals(0L, summary.totalSizeBytes)
        }

    @Test
    fun `test cancelUpload does not crash when unauthenticated`() {
        repository.cancelUpload("file-123")
    }

    @Test
    fun `test retryUpload does not crash when unauthenticated`() {
        repository.retryUpload("file-123")
    }
}

class FakeDriveRepository : DriveRepository {
    override suspend fun getOrCreateRootFolder(): Result<String> = Result.success("root-id")

    override suspend fun findRootFolder(): Result<String?> = Result.success("root-id")

    override suspend fun listFilesInFolder(
        folderId: String,
        pageToken: String?,
    ): Result<DriveFileList> = Result.success(DriveFileList(files = emptyList()))

    override suspend fun getFile(fileId: String): Result<DriveFileItem> = Result.success(DriveFileItem(id = fileId, name = "file"))

    override suspend fun createResumableUploadSession(
        name: String,
        mimeType: String,
        totalBytes: Long,
        folderId: String,
        fileId: String,
    ): Result<String> = Result.success("https://drive.session.url/123")

    override suspend fun downloadFile(fileId: String): Result<ResponseBody> = Result.failure(UnsupportedOperationException())

    override suspend fun renameFile(
        fileId: String,
        newName: String,
    ): Result<DriveFileItem> = Result.success(DriveFileItem(id = fileId, name = newName))

    override suspend fun setFileTrashed(
        fileId: String,
        trashed: Boolean,
    ): Result<DriveFileItem> = Result.success(DriveFileItem(id = fileId, trashed = trashed))

    override suspend fun deleteFilePermanently(fileId: String): Result<Unit> = Result.success(Unit)

    override suspend fun shareFile(fileId: String): Result<String> = Result.success("perm-123")

    override suspend fun unshareFile(
        fileId: String,
        permissionId: String,
    ): Result<Unit> = Result.success(Unit)

    override suspend fun getStorageQuota(): Result<StorageQuota?> = Result.success(null)
}
