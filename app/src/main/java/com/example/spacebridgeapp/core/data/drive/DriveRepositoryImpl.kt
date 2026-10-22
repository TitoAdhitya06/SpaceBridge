package com.example.spacebridgeapp.core.data.drive

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.HttpException

class DriveRepositoryImpl(
    private val driveApi: DriveApi,
    private val driveAuthManager: DriveAuthManager,
    private val auth: FirebaseAuth? = null,
    private val firestoreProvider: (() -> FirebaseFirestore)? = null,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : DriveRepository {
    @Volatile
    private var cachedRootFolderId: String? = null

    @Volatile
    private var cachedRootFolderUid: String? = null

    private suspend fun <T> withAuth(block: suspend (authHeader: String) -> T): Result<T> =
        withContext(dispatcher) {
            val token =
                driveAuthManager.getTokenSilently()
                    ?: return@withContext Result.failure(DriveNeedsConsentException("Token otorisasi Drive tidak tersedia"))

            try {
                Result.success(block("Bearer $token"))
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    driveAuthManager.invalidateToken(token)
                    val retryToken =
                        driveAuthManager.getTokenSilently()
                            ?: return@withContext Result.failure(DriveNeedsConsentException("Izin Google Drive dibutuhkan"))

                    try {
                        Result.success(block("Bearer $retryToken"))
                    } catch (retryHttp: HttpException) {
                        if (retryHttp.code() == 401) {
                            Result.failure(DriveNeedsConsentException("Izin Google Drive dibutuhkan"))
                        } else {
                            Result.failure(mapDriveException(retryHttp))
                        }
                    } catch (retryEx: CancellationException) {
                        throw retryEx
                    } catch (retryEx: Exception) {
                        Result.failure(mapDriveException(retryEx))
                    }
                } else {
                    Result.failure(mapDriveException(e))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(mapDriveException(e))
            }
        }

    override suspend fun findRootFolder(): Result<String?> =
        withAuth { authHeader ->
            val query = "appProperties has { key='spacebridge' and value='root' } and trashed=false"
            val response =
                driveApi.listFiles(
                    auth = authHeader,
                    query = query,
                    pageSize = 1,
                    fields = "files(id)",
                )
            response.files.firstOrNull()?.id
        }

    override suspend fun getOrCreateRootFolder(): Result<String> =
        withContext(dispatcher) {
            val currentUid = auth?.currentUser?.uid
            val cachedId = cachedRootFolderId
            if (!cachedId.isNullOrBlank() && cachedRootFolderUid == currentUid) {
                return@withContext Result.success(cachedId)
            }

            val findResult = findRootFolder()
            findResult.fold(
                onSuccess = { existingId ->
                    if (!existingId.isNullOrBlank()) {
                        cachedRootFolderId = existingId
                        cachedRootFolderUid = currentUid
                        updateFirestoreFolderId(existingId)
                        Result.success(existingId)
                    } else {
                        val createResult =
                            withAuth { authHeader ->
                                val request =
                                    CreateFileRequest(
                                        name = "SpaceBridge",
                                        mimeType = "application/vnd.google-apps.folder",
                                        appProperties = mapOf("spacebridge" to "root"),
                                    )
                                val created =
                                    driveApi.createFile(
                                        auth = authHeader,
                                        fields = "id,name,mimeType,appProperties",
                                        request = request,
                                    )
                                created.id
                            }
                        createResult.onSuccess { newId ->
                            cachedRootFolderId = newId
                            cachedRootFolderUid = currentUid
                            updateFirestoreFolderId(newId)
                        }
                        createResult
                    }
                },
                onFailure = { error ->
                    Result.failure(error)
                },
            )
        }

    override suspend fun listFilesInFolder(
        folderId: String,
        pageToken: String?,
    ): Result<DriveFileList> =
        withAuth { authHeader ->
            val query = "'$folderId' in parents"
            driveApi.listFiles(
                auth = authHeader,
                query = query,
                pageSize = 200,
                fields = "nextPageToken,files(id,name,mimeType,size,md5Checksum,webViewLink,trashed,appProperties)",
                pageToken = pageToken,
            )
        }

    override suspend fun getFile(fileId: String): Result<DriveFileItem> =
        withAuth { authHeader ->
            driveApi.getFile(
                auth = authHeader,
                fileId = fileId,
                fields = "id,name,mimeType,size,md5Checksum,webViewLink,trashed,appProperties",
            )
        }

    override suspend fun createResumableUploadSession(
        name: String,
        mimeType: String,
        totalBytes: Long,
        folderId: String,
        fileId: String,
    ): Result<String> =
        withAuth { authHeader ->
            val request =
                CreateFileRequest(
                    name = name,
                    parents = listOf(folderId),
                    appProperties = mapOf("spacebridgeFileId" to fileId),
                )
            val response =
                driveApi.initiateResumableUpload(
                    auth = authHeader,
                    uploadContentType = mimeType,
                    uploadContentLength = totalBytes,
                    request = request,
                )
            val location = response.headers()["Location"]
            if (!response.isSuccessful || location.isNullOrBlank()) {
                throw HttpException(response)
            }
            location
        }

    override suspend fun downloadFile(fileId: String): Result<ResponseBody> =
        withAuth { authHeader ->
            driveApi.downloadFile(
                auth = authHeader,
                fileId = fileId,
            )
        }

    override suspend fun renameFile(
        fileId: String,
        newName: String,
    ): Result<DriveFileItem> =
        withAuth { authHeader ->
            driveApi.updateFile(
                auth = authHeader,
                fileId = fileId,
                fields = "id,name,trashed",
                request = UpdateFileRequest(name = newName),
            )
        }

    override suspend fun setFileTrashed(
        fileId: String,
        trashed: Boolean,
    ): Result<DriveFileItem> =
        withAuth { authHeader ->
            driveApi.updateFile(
                auth = authHeader,
                fileId = fileId,
                fields = "id,name,trashed",
                request = UpdateFileRequest(trashed = trashed),
            )
        }

    override suspend fun deleteFilePermanently(fileId: String): Result<Unit> =
        withAuth { authHeader ->
            val response =
                driveApi.deleteFile(
                    auth = authHeader,
                    fileId = fileId,
                )
            if (!response.isSuccessful && response.code() != 204 && response.code() != 404) {
                throw HttpException(response)
            }
        }

    override suspend fun shareFile(fileId: String): Result<String> =
        withAuth { authHeader ->
            val response =
                driveApi.createPermission(
                    auth = authHeader,
                    fileId = fileId,
                    fields = "id",
                    request = CreatePermissionRequest(type = "anyone", role = "reader"),
                )
            response.id
        }

    override suspend fun unshareFile(
        fileId: String,
        permissionId: String,
    ): Result<Unit> =
        withAuth { authHeader ->
            val response =
                driveApi.deletePermission(
                    auth = authHeader,
                    fileId = fileId,
                    permissionId = permissionId,
                )
            if (!response.isSuccessful && response.code() != 204 && response.code() != 404) {
                throw HttpException(response)
            }
        }

    override suspend fun getStorageQuota(): Result<StorageQuota?> =
        withAuth { authHeader ->
            driveApi
                .getAbout(
                    auth = authHeader,
                    fields = "storageQuota",
                ).storageQuota
        }

    private suspend fun updateFirestoreFolderId(folderId: String) {
        try {
            val uid = auth?.currentUser?.uid ?: return
            val currentFirestore = firestoreProvider?.invoke() ?: return
            currentFirestore.collection("users").document(uid).update("driveFolderId", folderId)
        } catch (_: Exception) {
        }
    }

    private fun mapDriveException(throwable: Throwable): Throwable {
        if (throwable !is HttpException) return throwable
        val code = throwable.code()
        val errorBody =
            try {
                throwable
                    .response()
                    ?.errorBody()
                    ?.string()
                    .orEmpty()
            } catch (_: Exception) {
                ""
            }

        android.util.Log.e("DriveRepository", "HTTP error $code from Google Drive API: $errorBody")

        return when {
            code == 401 -> DriveNeedsConsentException("Izin Google Drive dibutuhkan")
            code == 403 &&
                (
                    errorBody.contains("SERVICE_DISABLED", ignoreCase = true) ||
                        errorBody.contains("has not been used in project", ignoreCase = true) ||
                        errorBody.contains("accessNotConfigured", ignoreCase = true)
                ) ->
                DriveApiDisabledException("Google Drive API belum diaktifkan pada konsol pengembang")
            code == 403 &&
                (errorBody.contains("storageQuotaExceeded", ignoreCase = true) || errorBody.contains("quota", ignoreCase = true)) ->
                DriveQuotaExceededException("Penyimpanan Google Drive penuh")
            code == 403 &&
                (
                    errorBody.contains("rateLimitExceeded", ignoreCase = true) ||
                        errorBody.contains("userRateLimitExceeded", ignoreCase = true)
                ) ->
                DriveRateLimitException("Batas kecepatan Google Drive tercapai")
            code == 429 -> DriveRateLimitException("Batas kecepatan Google Drive tercapai")
            code == 404 -> DriveFileNotFoundException("File tidak ditemukan di Google Drive")
            code in 500..599 -> DriveServerException("Server Google Drive mengalami masalah ($code)")
            else -> DriveException(throwable.message().takeIf { it.isNotBlank() } ?: "Kesalahan Google Drive ($code)", throwable)
        }
    }
}
