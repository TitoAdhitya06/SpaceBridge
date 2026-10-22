package com.example.spacebridgeapp.core.data.drive

import android.app.Activity
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class DriveRepositoryTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDriveApi: FakeDriveApi
    private lateinit var fakeDriveAuthManager: FakeDriveAuthManager
    private lateinit var repository: DriveRepositoryImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDriveApi = FakeDriveApi()
        fakeDriveAuthManager = FakeDriveAuthManager()
        repository =
            DriveRepositoryImpl(
                driveApi = fakeDriveApi,
                driveAuthManager = fakeDriveAuthManager,
                dispatcher = testDispatcher,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test findRootFolder returns folder id when found`() =
        runTest(testDispatcher) {
            fakeDriveApi.listFilesHandler = { auth, query, pageSize, fields, _ ->
                assertEquals("Bearer initial-token", auth)
                assertEquals("appProperties has { key='spacebridge' and value='root' } and trashed=false", query)
                assertEquals(1, pageSize)
                assertEquals("files(id)", fields)
                DriveFileList(files = listOf(DriveFileItem(id = "root-folder-123")))
            }

            val result = repository.findRootFolder()
            advanceUntilIdle()

            assertTrue(result.isSuccess)
            assertEquals("root-folder-123", result.getOrNull())
        }

    @Test
    fun `test findRootFolder returns null when not found`() =
        runTest(testDispatcher) {
            fakeDriveApi.listFilesHandler = { _, _, _, _, _ ->
                DriveFileList(files = emptyList())
            }

            val result = repository.findRootFolder()
            advanceUntilIdle()

            assertTrue(result.isSuccess)
            assertNull(result.getOrNull())
        }

    @Test
    fun `test getOrCreateRootFolder returns existing root folder if exists`() =
        runTest(testDispatcher) {
            var listCallCount = 0
            fakeDriveApi.listFilesHandler = { _, _, _, _, _ ->
                listCallCount++
                DriveFileList(files = listOf(DriveFileItem(id = "existing-folder-id")))
            }

            val result1 = repository.getOrCreateRootFolder()
            val result2 = repository.getOrCreateRootFolder()
            advanceUntilIdle()

            assertTrue(result1.isSuccess)
            assertEquals("existing-folder-id", result1.getOrNull())
            assertEquals("existing-folder-id", result2.getOrNull())
            assertEquals(1, listCallCount)
        }

    @Test
    fun `test getOrCreateRootFolder creates folder when none exists`() =
        runTest(testDispatcher) {
            fakeDriveApi.listFilesHandler = { _, _, _, _, _ ->
                DriveFileList(files = emptyList())
            }
            fakeDriveApi.createFileHandler = { auth, fields, request ->
                assertEquals("Bearer initial-token", auth)
                assertEquals("SpaceBridge", request.name)
                assertEquals("application/vnd.google-apps.folder", request.mimeType)
                assertEquals(mapOf("spacebridge" to "root"), request.appProperties)
                assertEquals("id,name,mimeType,appProperties", fields)
                DriveFileItem(id = "newly-created-folder-id", name = request.name)
            }

            val result = repository.getOrCreateRootFolder()
            advanceUntilIdle()

            assertTrue(result.isSuccess)
            assertEquals("newly-created-folder-id", result.getOrNull())
        }

    @Test
    fun `test listFilesInFolder calls driveApi with correct parents query`() =
        runTest(testDispatcher) {
            fakeDriveApi.listFilesHandler = { auth, query, pageSize, fields, pageToken ->
                assertEquals("Bearer initial-token", auth)
                assertEquals("'folder-abc' in parents", query)
                assertEquals(200, pageSize)
                assertEquals(
                    "nextPageToken,files(id,name,mimeType,size,md5Checksum,webViewLink,trashed,appProperties)",
                    fields,
                )
                assertEquals("token-page-2", pageToken)
                DriveFileList(
                    files =
                        listOf(
                            DriveFileItem(id = "f1", name = "Document.pdf", mimeType = "application/pdf"),
                            DriveFileItem(id = "f2", name = "Photo.jpg", mimeType = "image/jpeg"),
                        ),
                    nextPageToken = null,
                )
            }

            val result = repository.listFilesInFolder("folder-abc", pageToken = "token-page-2")
            advanceUntilIdle()

            assertTrue(result.isSuccess)
            val fileList = result.getOrNull()
            assertEquals(2, fileList?.files?.size)
            assertEquals("Document.pdf", fileList?.files?.get(0)?.name)
        }

    @Test
    fun `test getFile returns DriveFileItem`() =
        runTest(testDispatcher) {
            fakeDriveApi.getFileHandler = { auth, fileId, fields ->
                assertEquals("Bearer initial-token", auth)
                assertEquals("f100", fileId)
                assertEquals("id,name,mimeType,size,md5Checksum,webViewLink,trashed,appProperties", fields)
                DriveFileItem(id = "f100", name = "Sample.txt", size = 1024L)
            }

            val result = repository.getFile("f100")
            advanceUntilIdle()

            assertTrue(result.isSuccess)
            assertEquals("Sample.txt", result.getOrNull()?.name)
            assertEquals(1024L, result.getOrNull()?.size)
        }

    @Test
    fun `test createResumableUploadSession returns Location header`() =
        runTest(testDispatcher) {
            val expectedSessionUrl = "https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable&upload_id=xyz123"
            fakeDriveApi.initiateResumableUploadHandler = { auth, contentType, contentLength, request ->
                assertEquals("Bearer initial-token", auth)
                assertEquals("application/pdf", contentType)
                assertEquals(5000000L, contentLength)
                assertEquals("Doc.pdf", request.name)
                assertEquals(listOf("parent-folder-id"), request.parents)
                assertEquals(mapOf("spacebridgeFileId" to "firestore-file-id"), request.appProperties)
                Response.success(Unit, Headers.headersOf("Location", expectedSessionUrl))
            }

            val result =
                repository.createResumableUploadSession(
                    name = "Doc.pdf",
                    mimeType = "application/pdf",
                    totalBytes = 5000000L,
                    folderId = "parent-folder-id",
                    fileId = "firestore-file-id",
                )
            advanceUntilIdle()

            assertTrue(result.isSuccess)
            assertEquals(expectedSessionUrl, result.getOrNull())
        }

    @Test
    fun `test createResumableUploadSession fails when Location header missing`() =
        runTest(testDispatcher) {
            fakeDriveApi.initiateResumableUploadHandler = { _, _, _, _ ->
                Response.success(Unit, Headers.headersOf())
            }

            val result =
                repository.createResumableUploadSession(
                    name = "Doc.pdf",
                    mimeType = "application/pdf",
                    totalBytes = 1000L,
                    folderId = "folder-1",
                    fileId = "file-1",
                )
            advanceUntilIdle()

            assertTrue(result.isFailure)
        }

    @Test
    fun `test downloadFile returns response body`() =
        runTest(testDispatcher) {
            fakeDriveApi.downloadFileHandler = { auth, fileId ->
                assertEquals("Bearer initial-token", auth)
                assertEquals("file-dl", fileId)
                "konten berkas".toResponseBody("text/plain".toMediaType())
            }

            val result = repository.downloadFile("file-dl")
            advanceUntilIdle()

            assertTrue(result.isSuccess)
            assertEquals("konten berkas", result.getOrNull()?.string())
        }

    @Test
    fun `test renameFile calls updateFile with new name`() =
        runTest(testDispatcher) {
            fakeDriveApi.updateFileHandler = { auth, fileId, fields, request ->
                assertEquals("Bearer initial-token", auth)
                assertEquals("f-rename", fileId)
                assertEquals("id,name,trashed", fields)
                assertEquals("NewName.txt", request.name)
                DriveFileItem(id = fileId, name = request.name)
            }

            val result = repository.renameFile("f-rename", "NewName.txt")
            advanceUntilIdle()

            assertTrue(result.isSuccess)
            assertEquals("NewName.txt", result.getOrNull()?.name)
        }

    @Test
    fun `test setFileTrashed calls updateFile with trashed flag`() =
        runTest(testDispatcher) {
            fakeDriveApi.updateFileHandler = { auth, fileId, fields, request ->
                assertEquals("Bearer initial-token", auth)
                assertEquals("f-trash", fileId)
                assertEquals("id,name,trashed", fields)
                assertEquals(true, request.trashed)
                DriveFileItem(id = fileId, trashed = request.trashed)
            }

            val result = repository.setFileTrashed("f-trash", true)
            advanceUntilIdle()

            assertTrue(result.isSuccess)
            assertEquals(true, result.getOrNull()?.trashed)
        }

    @Test
    fun `test deleteFilePermanently calls deleteFile on api`() =
        runTest(testDispatcher) {
            var called = false
            fakeDriveApi.deleteFileHandler = { auth, fileId ->
                assertEquals("Bearer initial-token", auth)
                assertEquals("f-del", fileId)
                called = true
                Response.success(204, Unit)
            }

            val result = repository.deleteFilePermanently("f-del")
            advanceUntilIdle()

            assertTrue(result.isSuccess)
            assertTrue(called)
        }

    @Test
    fun `test shareFile creates anyone permission and returns permissionId`() =
        runTest(testDispatcher) {
            fakeDriveApi.createPermissionHandler = { auth, fileId, fields, request ->
                assertEquals("Bearer initial-token", auth)
                assertEquals("f-share", fileId)
                assertEquals("id", fields)
                assertEquals("anyone", request.type)
                assertEquals("reader", request.role)
                PermissionResponse(id = "anyoneWithLink")
            }

            val result = repository.shareFile("f-share")
            advanceUntilIdle()

            assertTrue(result.isSuccess)
            assertEquals("anyoneWithLink", result.getOrNull())
        }

    @Test
    fun `test unshareFile calls deletePermission on api`() =
        runTest(testDispatcher) {
            var called = false
            fakeDriveApi.deletePermissionHandler = { auth, fileId, permissionId ->
                assertEquals("Bearer initial-token", auth)
                assertEquals("f-share", fileId)
                assertEquals("perm-999", permissionId)
                called = true
                Response.success(204, Unit)
            }

            val result = repository.unshareFile("f-share", "perm-999")
            advanceUntilIdle()

            assertTrue(result.isSuccess)
            assertTrue(called)
        }

    @Test
    fun `test getStorageQuota returns storageQuota info`() =
        runTest(testDispatcher) {
            fakeDriveApi.getAboutHandler = { auth, fields ->
                assertEquals("Bearer initial-token", auth)
                assertEquals("storageQuota", fields)
                AboutResponse(storageQuota = StorageQuota(limit = 15000000000L, usage = 5000000000L))
            }

            val result = repository.getStorageQuota()
            advanceUntilIdle()

            assertTrue(result.isSuccess)
            assertEquals(15000000000L, result.getOrNull()?.limit)
            assertEquals(5000000000L, result.getOrNull()?.usage)
        }

    @Test
    fun `test withAuth retries on 401 and succeeds with new token`() =
        runTest(testDispatcher) {
            var callCount = 0
            fakeDriveApi.getFileHandler = { auth, fileId, _ ->
                callCount++
                if (callCount == 1) {
                    assertEquals("Bearer initial-token", auth)
                    val errorBody = "Unauthorized".toResponseBody("text/plain".toMediaType())
                    throw HttpException(Response.error<DriveFileItem>(401, errorBody))
                } else {
                    assertEquals("Bearer retried-token", auth)
                    DriveFileItem(id = fileId, name = "RecoveredFile.txt")
                }
            }

            val result = repository.getFile("f-retry")
            advanceUntilIdle()

            assertTrue(result.isSuccess)
            assertEquals("RecoveredFile.txt", result.getOrNull()?.name)
            assertEquals(2, callCount)
            assertEquals(listOf("initial-token"), fakeDriveAuthManager.invalidatedTokens)
        }

    @Test
    fun `test withAuth returns DriveNeedsConsentException when 401 persists`() =
        runTest(testDispatcher) {
            fakeDriveApi.getFileHandler = { _, _, _ ->
                val errorBody = "Unauthorized".toResponseBody("text/plain".toMediaType())
                throw HttpException(Response.error<DriveFileItem>(401, errorBody))
            }

            val result = repository.getFile("f-retry-failed")
            advanceUntilIdle()

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is DriveNeedsConsentException)
            assertEquals(listOf("initial-token"), fakeDriveAuthManager.invalidatedTokens)
        }

    @Test
    fun `test error mapping for 403 quota exceeded`() =
        runTest(testDispatcher) {
            fakeDriveApi.getFileHandler = { _, _, _ ->
                val errorBody =
                    """{"error":{"message":"The user's Drive storageQuotaExceeded limit."}}""".toResponseBody(
                        "application/json".toMediaType(),
                    )
                throw HttpException(Response.error<DriveFileItem>(403, errorBody))
            }

            val result = repository.getFile("f-quota")
            advanceUntilIdle()

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is DriveQuotaExceededException)
            assertEquals("Penyimpanan Google Drive penuh", result.exceptionOrNull()?.message)
        }

    @Test
    fun `test error mapping for 429 rate limit exceeded`() =
        runTest(testDispatcher) {
            fakeDriveApi.getFileHandler = { _, _, _ ->
                val errorBody = "Too Many Requests".toResponseBody("text/plain".toMediaType())
                throw HttpException(Response.error<DriveFileItem>(429, errorBody))
            }

            val result = repository.getFile("f-rate")
            advanceUntilIdle()

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is DriveRateLimitException)
            assertEquals("Batas kecepatan Google Drive tercapai", result.exceptionOrNull()?.message)
        }

    @Test
    fun `test error mapping for 404 file not found`() =
        runTest(testDispatcher) {
            fakeDriveApi.getFileHandler = { _, _, _ ->
                val errorBody = "File not found".toResponseBody("text/plain".toMediaType())
                throw HttpException(Response.error<DriveFileItem>(404, errorBody))
            }

            val result = repository.getFile("f-404")
            advanceUntilIdle()

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is DriveFileNotFoundException)
            assertEquals("File tidak ditemukan di Google Drive", result.exceptionOrNull()?.message)
        }

    @Test
    fun `test error mapping for 500 server error`() =
        runTest(testDispatcher) {
            fakeDriveApi.getFileHandler = { _, _, _ ->
                val errorBody = "Internal Server Error".toResponseBody("text/plain".toMediaType())
                throw HttpException(Response.error<DriveFileItem>(500, errorBody))
            }

            val result = repository.getFile("f-500")
            advanceUntilIdle()

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is DriveServerException)
            assertTrue(result.exceptionOrNull()?.message?.contains("500") == true)
        }
}

class FakeDriveAuthManager(
    var tokenToReturn: String? = "initial-token",
    var tokenAfterInvalidate: String? = "retried-token",
) : DriveAuthManager {
    val invalidatedTokens = mutableListOf<String>()
    private val _isAuthorized = MutableStateFlow(true)
    override val isAuthorized: StateFlow<Boolean> = _isAuthorized

    override suspend fun authorize(): DriveAuthResult = DriveAuthResult.Granted(tokenToReturn ?: "token")

    override suspend fun authorize(activity: Activity): DriveAuthResult = DriveAuthResult.Granted(tokenToReturn ?: "token")

    override suspend fun handleConsentResult(data: Intent?): DriveAuthResult = DriveAuthResult.Granted(tokenToReturn ?: "token")

    override suspend fun getTokenSilently(): String? = if (invalidatedTokens.isNotEmpty()) tokenAfterInvalidate else tokenToReturn

    override suspend fun invalidateToken(token: String) {
        invalidatedTokens.add(token)
    }

    override suspend fun revoke() {
        _isAuthorized.value = false
    }
}

class FakeDriveApi : DriveApi {
    var listFilesHandler: (auth: String, query: String?, pageSize: Int?, fields: String?, pageToken: String?) -> DriveFileList =
        { _, _, _, _, _ -> DriveFileList() }

    var createFileHandler: (auth: String, fields: String?, request: CreateFileRequest) -> DriveFileItem =
        { _, _, req -> DriveFileItem(id = "created-id", name = req.name, mimeType = req.mimeType) }

    var getFileHandler: (auth: String, fileId: String, fields: String?) -> DriveFileItem =
        { _, id, _ -> DriveFileItem(id = id, name = "file-$id") }

    var initiateResumableUploadHandler:
        (auth: String, contentType: String, contentLength: Long, request: CreateFileRequest) -> Response<Unit> =
        { _, _, _, _ ->
            Response.success(Unit, Headers.headersOf("Location", "https://upload.google.com/session-123"))
        }

    var downloadFileHandler: (auth: String, fileId: String) -> ResponseBody =
        { _, _ -> "file-content".toResponseBody("text/plain".toMediaType()) }

    var updateFileHandler: (auth: String, fileId: String, fields: String?, request: UpdateFileRequest) -> DriveFileItem =
        { _, id, _, req -> DriveFileItem(id = id, name = req.name, trashed = req.trashed) }

    var deleteFileHandler: (auth: String, fileId: String) -> Response<Unit> =
        { _, _ -> Response.success(204, Unit) }

    var createPermissionHandler: (auth: String, fileId: String, fields: String?, request: CreatePermissionRequest) -> PermissionResponse =
        { _, _, _, _ -> PermissionResponse(id = "perm-123") }

    var deletePermissionHandler: (auth: String, fileId: String, permissionId: String) -> Response<Unit> =
        { _, _, _ -> Response.success(204, Unit) }

    var getAboutHandler: (auth: String, fields: String?) -> AboutResponse =
        { _, _ -> AboutResponse(storageQuota = StorageQuota(limit = 1000L, usage = 200L)) }

    override suspend fun listFiles(
        auth: String,
        query: String?,
        pageSize: Int?,
        fields: String?,
        pageToken: String?,
    ): DriveFileList = listFilesHandler(auth, query, pageSize, fields, pageToken)

    override suspend fun createFile(
        auth: String,
        fields: String?,
        request: CreateFileRequest,
    ): DriveFileItem = createFileHandler(auth, fields, request)

    override suspend fun getFile(
        auth: String,
        fileId: String,
        fields: String?,
    ): DriveFileItem = getFileHandler(auth, fileId, fields)

    override suspend fun initiateResumableUpload(
        auth: String,
        uploadContentType: String,
        uploadContentLength: Long,
        request: CreateFileRequest,
    ): Response<Unit> = initiateResumableUploadHandler(auth, uploadContentType, uploadContentLength, request)

    override suspend fun downloadFile(
        auth: String,
        fileId: String,
    ): ResponseBody = downloadFileHandler(auth, fileId)

    override suspend fun updateFile(
        auth: String,
        fileId: String,
        fields: String?,
        request: UpdateFileRequest,
    ): DriveFileItem = updateFileHandler(auth, fileId, fields, request)

    override suspend fun deleteFile(
        auth: String,
        fileId: String,
    ): Response<Unit> = deleteFileHandler(auth, fileId)

    override suspend fun createPermission(
        auth: String,
        fileId: String,
        fields: String?,
        request: CreatePermissionRequest,
    ): PermissionResponse = createPermissionHandler(auth, fileId, fields, request)

    override suspend fun deletePermission(
        auth: String,
        fileId: String,
        permissionId: String,
    ): Response<Unit> = deletePermissionHandler(auth, fileId, permissionId)

    override suspend fun getAbout(
        auth: String,
        fields: String?,
    ): AboutResponse = getAboutHandler(auth, fields)
}
