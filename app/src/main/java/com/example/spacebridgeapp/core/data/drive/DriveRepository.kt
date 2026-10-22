package com.example.spacebridgeapp.core.data.drive

import okhttp3.ResponseBody

interface DriveRepository {
    suspend fun getOrCreateRootFolder(): Result<String>

    suspend fun findRootFolder(): Result<String?>

    suspend fun listFilesInFolder(
        folderId: String,
        pageToken: String? = null,
    ): Result<DriveFileList>

    suspend fun getFile(fileId: String): Result<DriveFileItem>

    suspend fun createResumableUploadSession(
        name: String,
        mimeType: String,
        totalBytes: Long,
        folderId: String,
        fileId: String,
    ): Result<String>

    suspend fun downloadFile(fileId: String): Result<ResponseBody>

    suspend fun renameFile(
        fileId: String,
        newName: String,
    ): Result<DriveFileItem>

    suspend fun setFileTrashed(
        fileId: String,
        trashed: Boolean,
    ): Result<DriveFileItem>

    suspend fun deleteFilePermanently(fileId: String): Result<Unit>

    suspend fun shareFile(fileId: String): Result<String>

    suspend fun unshareFile(
        fileId: String,
        permissionId: String,
    ): Result<Unit>

    suspend fun getStorageQuota(): Result<StorageQuota?>
}
