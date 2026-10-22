package com.example.spacebridgeapp.core.data.files

import android.net.Uri
import com.example.spacebridgeapp.core.model.DriveFile
import java.time.Instant
import kotlinx.coroutines.flow.Flow

data class UploadEnqueueResult(
    val queuedCount: Int,
    val failedCount: Int,
    val failureReason: String? = null,
)

data class ReconcileReport(
    val added: Int,
    val removed: Int,
    val updated: Int,
)

data class StorageSummary(
    val fileCount: Int,
    val totalSizeBytes: Long,
)

interface FilesRepository {
    fun observeFiles(trashed: Boolean): Flow<List<DriveFile>>

    fun observeFile(id: String): Flow<DriveFile?>

    suspend fun enqueueUploads(uris: List<Uri>): UploadEnqueueResult

    fun cancelUpload(id: String)

    fun retryUpload(id: String)

    suspend fun rename(
        id: String,
        newName: String,
    ): Result<Unit>

    suspend fun moveToTrash(id: String): Result<Unit>

    suspend fun restore(id: String): Result<Unit>

    suspend fun deletePermanently(id: String): Result<Unit>

    suspend fun share(id: String): Result<String>

    suspend fun unshare(id: String): Result<Unit>

    suspend fun openLocalCopy(
        id: String,
        onProgress: (Float) -> Unit,
    ): Result<Uri>

    suspend fun saveToDownloads(id: String): Result<Unit>

    suspend fun reconcile(force: Boolean = false): Result<ReconcileReport>

    fun observeUploadProgress(): Flow<Map<String, Float>>

    suspend fun storageSummary(): StorageSummary

    suspend fun purgeTrashOlderThan(cutoff: Instant)
}

