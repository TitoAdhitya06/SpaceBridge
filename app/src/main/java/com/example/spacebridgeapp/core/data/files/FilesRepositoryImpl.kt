package com.example.spacebridgeapp.core.data.files

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.spacebridgeapp.core.data.drive.DriveRepository
import com.example.spacebridgeapp.core.data.drive.StorageQuota
import com.example.spacebridgeapp.core.data.prefs.PreferencesRepository
import com.example.spacebridgeapp.core.model.DriveFile
import com.example.spacebridgeapp.core.model.FileSource
import com.example.spacebridgeapp.core.model.FileStatus
import com.example.spacebridgeapp.core.util.categorizeMimeType
import com.example.spacebridgeapp.core.work.UploadWorker
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.util.UUID

class FilesRepositoryImpl(
    private val context: Context,
    private val auth: FirebaseAuth,
    private val firestoreProvider: () -> FirebaseFirestore,
    private val preferencesRepository: PreferencesRepository,
    private val driveRepository: DriveRepository,
    workManager: WorkManager? = null,
) : FilesRepository {
    private val localUriPrefs by lazy {
        context.getSharedPreferences("spacebridge_local_file_uris", Context.MODE_PRIVATE)
    }
    private val firestore get() = firestoreProvider()
    private val workManager: WorkManager by lazy {
        workManager ?: WorkManager.getInstance(context)
    }

    override fun observeFiles(trashed: Boolean): Flow<List<DriveFile>> =
        callbackFlow {
            val uid = auth.currentUser?.uid
            if (uid.isNullOrBlank()) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }

            val query =
                if (trashed) {
                    firestore
                        .collection("users")
                        .document(uid)
                        .collection("files")
                        .whereEqualTo("status", "trashed")
                } else {
                    firestore
                        .collection("users")
                        .document(uid)
                        .collection("files")
                        .whereIn("status", listOf("ready", "uploading", "failed", "missing"))
                }

            val registration =
                query.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("FilesRepository", "Firestore observeFiles error: ${error.message}", error)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val files = snapshot.documents.mapNotNull { it.toDriveFile() }
                        for (file in files) {
                            if (file.status == FileStatus.UPLOADING) {
                                reconcileStalledUpload(file.id, uid)
                            }
                        }
                        val sorted =
                            if (trashed) {
                                files.sortedWith(compareByDescending(nullsLast()) { it.trashedAt })
                            } else {
                                files.sortedWith(compareByDescending(nullsLast()) { it.createdAt })
                            }
                        trySend(sorted)
                    }
                }

            awaitClose {
                registration.remove()
            }
        }

    override fun observeFile(id: String): Flow<DriveFile?> =
        callbackFlow {
            val uid = auth.currentUser?.uid
            if (uid.isNullOrBlank()) {
                trySend(null)
                close()
                return@callbackFlow
            }

            val registration =
                firestore
                    .collection("users")
                    .document(uid)
                    .collection("files")
                    .document(id)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            return@addSnapshotListener
                        }
                        trySend(snapshot?.toDriveFile())
                    }

            awaitClose {
                registration.remove()
            }
        }

    override suspend fun enqueueUploads(uris: List<Uri>): UploadEnqueueResult {
        val uid = auth.currentUser?.uid ?: return UploadEnqueueResult(0, uris.size)
        val isWifiOnly = preferencesRepository.isWifiOnlyUpload.first()

        // Periksa sisa kapasitas Google Drive pengguna
        val quota = driveRepository.getStorageQuota().getOrNull()
        var remainingBytes = calculateRemainingQuota(quota)

        var queued = 0
        var failed = 0
        var quotaExceeded = false

        for (uri in uris) {
            val fileMeta = queryFileMetadata(uri)
            if (fileMeta == null) {
                failed++
                continue
            }

            // Tolak berkas jika ukuran melebihi sisa kapasitas Google Drive
            val currentRemaining = remainingBytes
            if (currentRemaining != null && fileMeta.sizeBytes > currentRemaining) {
                failed++
                quotaExceeded = true
                continue
            }

            // Kurangi estimasi sisa kuota untuk berkas berikutnya dalam antrean ini
            if (remainingBytes != null) {
                remainingBytes = (remainingBytes - fileMeta.sizeBytes).coerceAtLeast(0L)
            }

            val fileId = UUID.randomUUID().toString()
            val category = categorizeMimeType(fileMeta.mimeType)

            val newFile =
                DriveFile(
                    id = fileId,
                    name = fileMeta.name,
                    mimeType = fileMeta.mimeType,
                    sizeBytes = fileMeta.sizeBytes,
                    category = category,
                    source = FileSource.DRIVE,
                    status = FileStatus.UPLOADING,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                )

            try {
                localUriPrefs.edit().putString(fileId, uri.toString()).apply()
                firestore
                    .collection("users")
                    .document(uid)
                    .collection("files")
                    .document(fileId)
                    .set(newFile.toFirestoreMap())
                    .await()

                val workRequest =
                    UploadWorker.createWorkRequest(
                        fileId = fileId,
                        fileUri = uri,
                        fileName = fileMeta.name,
                        mimeType = fileMeta.mimeType,
                        totalBytes = fileMeta.sizeBytes,
                        userId = uid,
                        isWifiOnly = isWifiOnly,
                    )

                workManager.enqueueUniqueWork(
                    "upload-$fileId",
                    ExistingWorkPolicy.KEEP,
                    workRequest,
                )
                queued++
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                failed++
            }
        }

        val failureReason = if (quotaExceeded) "melebihi kapasitas Google Drive" else null
        return UploadEnqueueResult(queued, failed, failureReason)
    }

    override fun cancelUpload(id: String) {
        val uid = auth.currentUser?.uid ?: return
        workManager.cancelUniqueWork("upload-$id")
        firestore
            .collection("users")
            .document(uid)
            .collection("files")
            .document(id)
            .update(
                mapOf(
                    "status" to "failed",
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
    }

    override fun retryUpload(id: String) {
        val uid = auth.currentUser?.uid ?: return
        val docRef =
            firestore
                .collection("users")
                .document(uid)
                .collection("files")
                .document(id)

        docRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) return@addOnSuccessListener
            val localUriStr = localUriPrefs.getString(id, null)
            val fileName = snapshot.getString("name") ?: "file"
            val mimeType = snapshot.getString("mimeType") ?: "application/octet-stream"
            val totalBytes = snapshot.getLong("sizeBytes") ?: 0L

            docRef.update(
                mapOf(
                    "status" to "uploading",
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )

            if (!localUriStr.isNullOrBlank()) {
                val workRequest =
                    UploadWorker.createWorkRequest(
                        fileId = id,
                        fileUriString = localUriStr,
                        fileName = fileName,
                        mimeType = mimeType,
                        totalBytes = totalBytes,
                        userId = uid,
                        isWifiOnly = false,
                    )
                workManager.enqueueUniqueWork(
                    "upload-$id",
                    ExistingWorkPolicy.REPLACE,
                    workRequest,
                )
            }
        }
    }

    override suspend fun rename(
        id: String,
        newName: String,
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Belum login"))
        return try {
            val docRef =
                firestore
                    .collection("users")
                    .document(uid)
                    .collection("files")
                    .document(id)
            val snapshot = docRef.get().await()
            val driveFileId = snapshot.getString("driveFileId")

            docRef
                .update(
                    mapOf(
                        "name" to newName,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                ).await()

            if (!driveFileId.isNullOrBlank()) {
                driveRepository.renameFile(driveFileId, newName)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun moveToTrash(id: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Belum login"))
        return try {
            val docRef =
                firestore
                    .collection("users")
                    .document(uid)
                    .collection("files")
                    .document(id)
            val snapshot = docRef.get().await()
            val driveFileId = snapshot.getString("driveFileId")

            docRef
                .update(
                    mapOf(
                        "status" to "trashed",
                        "trashedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                ).await()

            if (!driveFileId.isNullOrBlank()) {
                driveRepository.setFileTrashed(driveFileId, true)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restore(id: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Belum login"))
        return try {
            val docRef =
                firestore
                    .collection("users")
                    .document(uid)
                    .collection("files")
                    .document(id)
            val snapshot = docRef.get().await()
            val driveFileId = snapshot.getString("driveFileId")

            docRef
                .update(
                    mapOf(
                        "status" to "ready",
                        "trashedAt" to null,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                ).await()

            if (!driveFileId.isNullOrBlank()) {
                driveRepository.setFileTrashed(driveFileId, false)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deletePermanently(id: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Belum login"))
        return try {
            val docRef =
                firestore
                    .collection("users")
                    .document(uid)
                    .collection("files")
                    .document(id)
            val snapshot = docRef.get().await()
            val driveFileId = snapshot.getString("driveFileId")

            if (!driveFileId.isNullOrBlank()) {
                driveRepository.deleteFilePermanently(driveFileId)
            }
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun share(id: String): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Belum login"))
        return try {
            val docRef =
                firestore
                    .collection("users")
                    .document(uid)
                    .collection("files")
                    .document(id)
            val snapshot = docRef.get().await()
            val driveFileId =
                snapshot.getString("driveFileId")
                    ?: return Result.failure(IllegalStateException("Berkas belum siap di Google Drive"))

            val shareResult = driveRepository.shareFile(driveFileId)
            val permissionId = shareResult.getOrThrow()

            docRef
                .update(
                    mapOf(
                        "shared" to true,
                        "sharePermissionId" to permissionId,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                ).await()

            val webViewLink = snapshot.getString("webViewLink").orEmpty()
            Result.success(webViewLink)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unshare(id: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Belum login"))
        return try {
            val docRef =
                firestore
                    .collection("users")
                    .document(uid)
                    .collection("files")
                    .document(id)
            val snapshot = docRef.get().await()
            val driveFileId = snapshot.getString("driveFileId")
            val permissionId = snapshot.getString("sharePermissionId")

            if (!driveFileId.isNullOrBlank() && !permissionId.isNullOrBlank()) {
                driveRepository.unshareFile(driveFileId, permissionId)
            }

            docRef
                .update(
                    mapOf(
                        "shared" to false,
                        "sharePermissionId" to null,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                ).await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun openLocalCopy(
        id: String,
        onProgress: (Float) -> Unit,
    ): Result<Uri> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Belum login"))
        return try {
            val snapshot =
                firestore
                    .collection("users")
                    .document(uid)
                    .collection("files")
                    .document(id)
                    .get()
                    .await()
            val driveFileId =
                snapshot.getString("driveFileId")
                    ?: return Result.failure(IllegalStateException("Berkas belum tersedia di Drive"))
            val fileName = snapshot.getString("name") ?: "temp_file"

            val responseBody = driveRepository.downloadFile(driveFileId).getOrThrow()
            val cacheFile = File(context.cacheDir, fileName)

            responseBody.byteStream().use { input ->
                FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output)
                }
            }

            val uri =
                try {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        cacheFile,
                    )
                } catch (_: Exception) {
                    Uri.fromFile(cacheFile)
                }
            Result.success(uri)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveToDownloads(id: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Belum login"))
        return try {
            val snapshot =
                firestore
                    .collection("users")
                    .document(uid)
                    .collection("files")
                    .document(id)
                    .get()
                    .await()
            val driveFileId =
                snapshot.getString("driveFileId")
                    ?: return Result.failure(IllegalStateException("Berkas belum tersedia di Drive"))
            val fileName = snapshot.getString("name") ?: "downloaded_file"

            val responseBody = driveRepository.downloadFile(driveFileId).getOrThrow()
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val destFile = File(downloadsDir, fileName)

            responseBody.byteStream().use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun reconcile(force: Boolean): Result<ReconcileReport> {
        // Implementasi rekonsiliasi P1 akan disempurnakan di tahap rekonsiliasi berkas
        return Result.success(ReconcileReport(0, 0, 0))
    }

    override fun observeUploadProgress(): Flow<Map<String, Float>> =
        workManager
            .getWorkInfosByTagFlow(UploadWorker.TAG_UPLOAD)
            .map { workInfos ->
                val progressMap = mutableMapOf<String, Float>()
                for (info in workInfos) {
                    val fileIdTag = info.tags.firstOrNull { it.startsWith("upload-") }?.removePrefix("upload-")
                    if (fileIdTag != null) {
                        if (info.state == WorkInfo.State.RUNNING || info.state == WorkInfo.State.ENQUEUED) {
                            val progress = info.progress.getFloat("progress", 0.05f)
                            progressMap[fileIdTag] = progress
                        }
                    }
                }
                progressMap
            }

    private fun reconcileStalledUpload(fileId: String, uid: String) {
        val future = workManager.getWorkInfosForUniqueWork("upload-$fileId")
        future.addListener({
            try {
                val workInfos = future.get()
                val isWorking = workInfos.any {
                    it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED
                }
                if (!isWorking && workInfos.isNotEmpty()) {
                    firestore
                        .collection("users")
                        .document(uid)
                        .collection("files")
                        .document(fileId)
                        .update(
                            mapOf(
                                "status" to "failed",
                                "updatedAt" to FieldValue.serverTimestamp(),
                            ),
                        )
                }
            } catch (_: Exception) {
            }
        }, ContextCompat.getMainExecutor(context))
    }

    override suspend fun storageSummary(): StorageSummary {
        val uid = auth.currentUser?.uid ?: return StorageSummary(0, 0L)
        return try {
            val snapshot =
                firestore
                    .collection("users")
                    .document(uid)
                    .collection("files")
                    .whereIn("status", listOf("ready", "uploading"))
                    .get()
                    .await()

            var count = 0
            var totalBytes = 0L
            for (doc in snapshot.documents) {
                count++
                val size = doc.getLong("sizeBytes") ?: 0L
                totalBytes += size
            }
            StorageSummary(count, totalBytes)
        } catch (_: Exception) {
            StorageSummary(0, 0L)
        }
    }

    override suspend fun purgeTrashOlderThan(cutoff: Instant) =
        withContext(Dispatchers.IO) {
            val uid = auth.currentUser?.uid ?: return@withContext
            val cutoffTs = Timestamp(cutoff.epochSecond, cutoff.nano)
            try {
                val snapshot =
                    firestore
                        .collection("users")
                        .document(uid)
                        .collection("files")
                        .whereEqualTo("status", "trashed")
                        .get()
                        .await()

                for (doc in snapshot.documents) {
                    val trashedTs = doc.getTimestamp("trashedAt")
                    if (trashedTs != null && trashedTs <= cutoffTs) {
                        deletePermanently(doc.id)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }

    private fun queryFileMetadata(uri: Uri): LocalFileMeta? {
        val contentResolver = context.contentResolver
        var name = "file"
        var sizeBytes = 0L
        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"

        return try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex != -1) {
                        name = cursor.getString(nameIndex) ?: name
                    }
                    if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                        sizeBytes = cursor.getLong(sizeIndex)
                    }
                }
            }
            LocalFileMeta(name, mimeType, sizeBytes)
        } catch (_: Exception) {
            null
        }
    }

    private data class LocalFileMeta(
        val name: String,
        val mimeType: String,
        val sizeBytes: Long,
    )

    companion object {
        fun calculateRemainingQuota(quota: StorageQuota?): Long? {
            val limit = quota?.limit ?: return null
            val usage = quota.usage ?: return null
            return (limit - usage).coerceAtLeast(0L)
        }
    }
}
