package com.example.spacebridgeapp.core.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.spacebridgeapp.SpaceBridgeApplication
import com.example.spacebridgeapp.core.data.drive.DriveApiDisabledException
import com.example.spacebridgeapp.core.data.drive.DriveAuthManager
import com.example.spacebridgeapp.core.data.drive.DriveNeedsConsentException
import com.example.spacebridgeapp.core.data.drive.DriveQuotaExceededException
import com.example.spacebridgeapp.core.data.drive.DriveRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

private val Context.uploadSessionDataStore by preferencesDataStore(name = "spacebridge_upload_sessions")

class UploadWorker @JvmOverloads constructor(
    appContext: Context,
    workerParams: WorkerParameters,
    private val injectedDriveRepository: DriveRepository? = null,
    private val injectedDriveAuthManager: DriveAuthManager? = null,
    private val injectedFirestore: FirebaseFirestore? = null,
    private val injectedOkHttpClient: OkHttpClient? = null,
) : CoroutineWorker(appContext, workerParams) {
    private val appContainer get() = (applicationContext as? SpaceBridgeApplication)?.container

    private val driveRepository: DriveRepository?
        get() = injectedDriveRepository ?: appContainer?.driveRepository

    private val driveAuthManager: DriveAuthManager?
        get() = injectedDriveAuthManager ?: appContainer?.driveAuthManager

    private val firestore: FirebaseFirestore?
        get() = injectedFirestore ?: appContainer?.firestore

    private val httpClient: OkHttpClient
        get() =
            injectedOkHttpClient ?: OkHttpClient
                .Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()

    override suspend fun doWork(): Result {
        val fileId = inputData.getString(KEY_FILE_ID) ?: return Result.failure()
        val uriString = inputData.getString(KEY_FILE_URI) ?: return Result.failure()
        val fileName = inputData.getString(KEY_FILE_NAME) ?: "Berkas"
        val mimeType = inputData.getString(KEY_MIME_TYPE) ?: "application/octet-stream"
        val totalBytes = inputData.getLong(KEY_TOTAL_BYTES, 0L)
        val userId = inputData.getString(KEY_USER_ID) ?: return Result.failure()

        val uri = Uri.parse(uriString)

        android.util.Log.d("UploadWorker", "doWork started: fileId=$fileId, uri=$uriString, size=$totalBytes, user=$userId")

        // Verifikasi izin baca berkas asli
        try {
            applicationContext.contentResolver.openInputStream(uri)?.close()
            android.util.Log.d("UploadWorker", "URI readable")
        } catch (e: Exception) {
            android.util.Log.e("UploadWorker", "URI not readable", e)
            updateFirestoreFileStatus(userId, fileId, "failed")
            return Result.failure()
        }

        // Tampilkan foreground notification awal
        val notificationId = fileId.hashCode()
        try {
            setForeground(createForegroundInfo(fileName, 0, notificationId))
        } catch (_: Exception) {
        }
        setProgress(workDataOf("progress" to 0.05f))

        // Dapatkan atau inisiasi sesi resumable upload
        val sessionUrl =
            when (val initResult = getOrInitiateSession(fileId, fileName, mimeType, totalBytes, userId)) {
                is SessionInitResult.Success -> initResult.sessionUrl
                is SessionInitResult.Fatal -> {
                    android.util.Log.e("UploadWorker", "Fatal session init error: ${initResult.message}")
                    updateFirestoreFileStatus(userId, fileId, "failed")
                    showErrorNotification(initResult.message, notificationId)
                    return Result.failure()
                }
                is SessionInitResult.Retryable -> {
                    android.util.Log.w("UploadWorker", "Retryable session init error, attempt=$runAttemptCount")
                    if (runAttemptCount < 3) {
                        return Result.retry()
                    } else {
                        updateFirestoreFileStatus(userId, fileId, "failed")
                        showErrorNotification("Gagal mengunggah berkas ke Google Drive", notificationId)
                        return Result.failure()
                    }
                }
            }

        android.util.Log.d("UploadWorker", "sessionUrl obtained: $sessionUrl")

        // Cek posisi byte terakhir yang tersimpan pada sesi
        val startByte = queryCurrentPosition(sessionUrl, totalBytes)
        if (startByte < 0) {
            // Sesi 404 atau rusak, hapus sesi lama dan coba ulang
            android.util.Log.w("UploadWorker", "startByte < 0, clearing session and retrying")
            clearSavedSessionUrl(fileId)
            return Result.retry()
        }

        // Unggah bertahap (chunked upload) per 8 MB
        var currentOffset = startByte
        while (currentOffset < totalBytes) {
            val endOffset = minOf(currentOffset + CHUNK_SIZE - 1, totalBytes - 1)
            val chunkLength = (endOffset - currentOffset + 1).toInt()

            val chunkBytes = readChunk(uri, currentOffset, chunkLength)
            if (chunkBytes == null || chunkBytes.size != chunkLength) {
                updateFirestoreFileStatus(userId, fileId, "failed")
                return Result.failure()
            }

            val chunkResult =
                uploadChunk(
                    sessionUrl = sessionUrl,
                    startByte = currentOffset,
                    endByte = endOffset,
                    totalBytes = totalBytes,
                    chunk = chunkBytes,
                    mimeType = mimeType,
                )

            when (chunkResult) {
                is ChunkResult.Incomplete -> {
                    currentOffset = chunkResult.nextByte
                    val progressRatio = (currentOffset.toFloat() / totalBytes).coerceIn(0.05f, 0.95f)
                    val progressPercent = ((currentOffset.toFloat() / totalBytes) * 100).toInt()
                    setProgress(workDataOf("progress" to progressRatio))
                    try {
                        setForeground(createForegroundInfo(fileName, progressPercent, notificationId))
                    } catch (_: Exception) {
                    }
                }
                is ChunkResult.Success -> {
                    setProgress(workDataOf("progress" to 1.0f))
                    updateFirestoreFileSuccess(userId, fileId, chunkResult.driveFileId, chunkResult.webViewLink)
                    clearSavedSessionUrl(fileId)
                    showSuccessNotification(fileName, notificationId)
                    return Result.success()
                }
                is ChunkResult.QuotaExceeded -> {
                    updateFirestoreFileStatus(userId, fileId, "failed")
                    showErrorNotification("Penyimpanan Google Drive penuh", notificationId)
                    return Result.failure()
                }
                is ChunkResult.NeedsConsent -> {
                    updateFirestoreFileStatus(userId, fileId, "failed")
                    showErrorNotification("Izin Google Drive dibutuhkan", notificationId)
                    return Result.failure()
                }
                is ChunkResult.Retryable -> {
                    return Result.retry()
                }
                is ChunkResult.FatalError -> {
                    if (runAttemptCount < 3) {
                        return Result.retry()
                    }
                    updateFirestoreFileStatus(userId, fileId, "failed")
                    return Result.failure()
                }
            }
        }

        showSuccessNotification(fileName, notificationId)
        return Result.success()
    }

    private sealed interface SessionInitResult {
        data class Success(val sessionUrl: String) : SessionInitResult
        data class Fatal(val message: String) : SessionInitResult
        object Retryable : SessionInitResult
    }

    private suspend fun getOrInitiateSession(
        fileId: String,
        fileName: String,
        mimeType: String,
        totalBytes: Long,
        userId: String,
    ): SessionInitResult {
        val saved = getSavedSessionUrl(fileId)
        if (!saved.isNullOrBlank()) {
            return SessionInitResult.Success(saved)
        }

        val repository = driveRepository ?: run {
            android.util.Log.e("UploadWorker", "getOrInitiateSession: driveRepository is null!")
            return SessionInitResult.Retryable
        }
        val rootFolderResult = repository.getOrCreateRootFolder()
        val rootFolderId = rootFolderResult.getOrNull()
        if (rootFolderId.isNullOrBlank()) {
            val ex = rootFolderResult.exceptionOrNull()
            android.util.Log.e("UploadWorker", "getOrCreateRootFolder failed: $ex", ex)
            return when (ex) {
                is DriveApiDisabledException -> SessionInitResult.Fatal("Google Drive API belum diaktifkan pada konsol pengembang")
                is DriveQuotaExceededException -> SessionInitResult.Fatal("Penyimpanan Google Drive penuh")
                is DriveNeedsConsentException -> SessionInitResult.Fatal("Izin Google Drive dibutuhkan")
                else -> SessionInitResult.Retryable
            }
        }
        android.util.Log.d("UploadWorker", "rootFolderId = $rootFolderId")

        val sessionResult =
            repository.createResumableUploadSession(
                name = fileName,
                mimeType = mimeType,
                totalBytes = totalBytes,
                folderId = rootFolderId,
                fileId = fileId,
            )

        val sessionUrl = sessionResult.getOrNull()
        if (!sessionUrl.isNullOrBlank()) {
            saveSessionUrl(fileId, sessionUrl)
            return SessionInitResult.Success(sessionUrl)
        } else {
            val ex = sessionResult.exceptionOrNull()
            android.util.Log.e("UploadWorker", "createResumableUploadSession failed: $ex", ex)
            return when (ex) {
                is DriveApiDisabledException -> SessionInitResult.Fatal("Google Drive API belum diaktifkan pada konsol pengembang")
                is DriveQuotaExceededException -> SessionInitResult.Fatal("Penyimpanan Google Drive penuh")
                is DriveNeedsConsentException -> SessionInitResult.Fatal("Izin Google Drive dibutuhkan")
                else -> SessionInitResult.Retryable
            }
        }
    }

    private fun queryCurrentPosition(
        sessionUrl: String,
        totalBytes: Long,
    ): Long {
        val emptyBody = ByteArray(0).toRequestBody(null)
        val request =
            Request
                .Builder()
                .url(sessionUrl)
                .put(emptyBody)
                .header("Content-Range", "bytes */$totalBytes")
                .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                return when (response.code) {
                    308 -> {
                        val range = response.header("Range")
                        parseNextByteFromRange(range)
                    }
                    200, 201 -> totalBytes
                    404 -> -1L
                    else -> 0L
                }
            }
        } catch (_: IOException) {
            return 0L
        }
    }

    private fun uploadChunk(
        sessionUrl: String,
        startByte: Long,
        endByte: Long,
        totalBytes: Long,
        chunk: ByteArray,
        mimeType: String,
    ): ChunkResult {
        val body = chunk.toRequestBody(mimeType.toMediaType())
        val request =
            Request
                .Builder()
                .url(sessionUrl)
                .put(body)
                .header("Content-Range", "bytes $startByte-$endByte/$totalBytes")
                .header("Content-Length", chunk.size.toString())
                .build()

        return try {
            httpClient.newCall(request).execute().use { response ->
                when (response.code) {
                    308 -> {
                        val range = response.header("Range")
                        val nextByte = parseNextByteFromRange(range).coerceAtLeast(endByte + 1)
                        ChunkResult.Incomplete(nextByte)
                    }
                    200, 201 -> {
                        val bodyStr = response.body?.string().orEmpty()
                        val json = JSONObject(bodyStr)
                        val driveId = json.optString("id")
                        val link = json.optString("webViewLink")
                        ChunkResult.Success(driveId, link)
                    }
                    401 -> ChunkResult.NeedsConsent
                    403 -> {
                        val errorBody = response.body?.string().orEmpty()
                        if (errorBody.contains("storageQuotaExceeded", ignoreCase = true) ||
                            errorBody.contains("quota", ignoreCase = true)
                        ) {
                            ChunkResult.QuotaExceeded
                        } else {
                            ChunkResult.Retryable
                        }
                    }
                    429 -> ChunkResult.Retryable
                    in 500..599 -> ChunkResult.Retryable
                    else -> ChunkResult.FatalError
                }
            }
        } catch (_: IOException) {
            ChunkResult.Retryable
        }
    }

    private fun readChunk(
        uri: Uri,
        offset: Long,
        length: Int,
    ): ByteArray? =
        try {
            applicationContext.contentResolver.openInputStream(uri)?.use { stream ->
                var skipped = 0L
                while (skipped < offset) {
                    val actual = stream.skip(offset - skipped)
                    if (actual <= 0) break
                    skipped += actual
                }
                val buffer = ByteArray(length)
                var totalRead = 0
                while (totalRead < length) {
                    val read = stream.read(buffer, totalRead, length - totalRead)
                    if (read == -1) break
                    totalRead += read
                }
                if (totalRead == length) buffer else buffer.copyOf(totalRead)
            }
        } catch (_: Exception) {
            null
        }

    private suspend fun getSavedSessionUrl(fileId: String): String? {
        val key = stringPreferencesKey("session_$fileId")
        return applicationContext.uploadSessionDataStore.data
            .map { it[key] }
            .firstOrNull()
    }

    private suspend fun saveSessionUrl(
        fileId: String,
        url: String,
    ) {
        val key = stringPreferencesKey("session_$fileId")
        applicationContext.uploadSessionDataStore.edit { it[key] = url }
    }

    private suspend fun clearSavedSessionUrl(fileId: String) {
        val key = stringPreferencesKey("session_$fileId")
        applicationContext.uploadSessionDataStore.edit { it.remove(key) }
    }

    private suspend fun updateFirestoreFileStatus(
        userId: String,
        fileId: String,
        status: String,
    ) {
        try {
            val firestoreInstance = firestore ?: return
            firestoreInstance
                .collection("users")
                .document(userId)
                .collection("files")
                .document(fileId)
                .update(
                    mapOf(
                        "status" to status,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                ).await()
        } catch (_: Exception) {
        }
    }

    private suspend fun updateFirestoreFileSuccess(
        userId: String,
        fileId: String,
        driveId: String,
        link: String,
    ) {
        try {
            val firestoreInstance = firestore ?: return
            firestoreInstance
                .collection("users")
                .document(userId)
                .collection("files")
                .document(fileId)
                .update(
                    mapOf(
                        "status" to "ready",
                        "driveFileId" to driveId,
                        "webViewLink" to link,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                ).await()
        } catch (_: Exception) {
        }
    }

    private fun createForegroundInfo(
        fileName: String,
        progressPercent: Int,
        notificationId: Int,
    ): ForegroundInfo {
        val channelId = NOTIFICATION_CHANNEL_ID
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    channelId,
                    "Pengunggahan Berkas SpaceBridge",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Progres pengunggahan berkas ke Google Drive"
                }
            manager.createNotificationChannel(channel)
        }

        val notification =
            NotificationCompat
                .Builder(applicationContext, channelId)
                .setContentTitle("Mengunggah $fileName")
                .setContentText("$progressPercent% selesai")
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setProgress(100, progressPercent, false)
                .setOngoing(true)
                .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    private fun showErrorNotification(
        message: String,
        notificationId: Int,
    ) {
        val channelId = NOTIFICATION_CHANNEL_ID
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification =
            NotificationCompat
                .Builder(applicationContext, channelId)
                .setContentTitle("Unggahan Gagal")
                .setContentText(message)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setAutoCancel(true)
                .build()
        manager.notify(notificationId, notification)
    }

    private fun showSuccessNotification(
        fileName: String,
        notificationId: Int,
    ) {
        val channelId = NOTIFICATION_CHANNEL_ID
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    channelId,
                    "Pengunggahan Berkas SpaceBridge",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Progres pengunggahan berkas ke Google Drive"
                }
            manager.createNotificationChannel(channel)
        }

        val notification =
            NotificationCompat
                .Builder(applicationContext, channelId)
                .setContentTitle("Unggahan Berhasil")
                .setContentText("$fileName berhasil diunggah ke Google Drive")
                .setSmallIcon(android.R.drawable.stat_sys_upload_done)
                .setAutoCancel(true)
                .build()

        try {
            manager.notify(notificationId, notification)
        } catch (_: Exception) {
        }
    }

    private sealed interface ChunkResult {
        data class Incomplete(
            val nextByte: Long,
        ) : ChunkResult

        data class Success(
            val driveFileId: String,
            val webViewLink: String,
        ) : ChunkResult

        data object QuotaExceeded : ChunkResult

        data object NeedsConsent : ChunkResult

        data object Retryable : ChunkResult

        data object FatalError : ChunkResult
    }

    companion object {
        const val TAG_UPLOAD = "spacebridge_upload"
        const val KEY_FILE_ID = "file_id"
        const val KEY_FILE_URI = "file_uri"
        const val KEY_FILE_NAME = "file_name"
        const val KEY_MIME_TYPE = "mime_type"
        const val KEY_TOTAL_BYTES = "total_bytes"
        const val KEY_USER_ID = "user_id"
        const val CHUNK_SIZE = 8 * 1024 * 1024 // 8 MB
        const val NOTIFICATION_CHANNEL_ID = "spacebridge_upload_channel"

        fun createWorkRequest(
            fileId: String,
            fileUri: Uri,
            fileName: String,
            mimeType: String,
            totalBytes: Long,
            userId: String,
            isWifiOnly: Boolean,
        ): OneTimeWorkRequest =
            createWorkRequest(
                fileId = fileId,
                fileUriString = fileUri.toString(),
                fileName = fileName,
                mimeType = mimeType,
                totalBytes = totalBytes,
                userId = userId,
                isWifiOnly = isWifiOnly,
            )

        fun createWorkRequest(
            fileId: String,
            fileUriString: String,
            fileName: String,
            mimeType: String,
            totalBytes: Long,
            userId: String,
            isWifiOnly: Boolean,
        ): OneTimeWorkRequest {
            val constraints =
                Constraints
                    .Builder()
                    .setRequiredNetworkType(if (isWifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                    .build()

            val data =
                workDataOf(
                    KEY_FILE_ID to fileId,
                    KEY_FILE_URI to fileUriString,
                    KEY_FILE_NAME to fileName,
                    KEY_MIME_TYPE to mimeType,
                    KEY_TOTAL_BYTES to totalBytes,
                    KEY_USER_ID to userId,
                )

            return OneTimeWorkRequestBuilder<UploadWorker>()
                .setConstraints(constraints)
                .setInputData(data)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .addTag(TAG_UPLOAD)
                .addTag("upload-$fileId")
                .build()
        }

        fun parseNextByteFromRange(rangeHeader: String?): Long {
            if (rangeHeader.isNullOrBlank()) return 0L
            val trimmed = rangeHeader.trim()
            val cleaned =
                when {
                    trimmed.startsWith("bytes=", ignoreCase = true) -> trimmed.substringAfter("=")
                    trimmed.startsWith("bytes ", ignoreCase = true) -> trimmed.substringAfter(" ")
                    else -> trimmed
                }
            val parts = cleaned.substringBefore("/").split("-")
            if (parts.size >= 2) {
                val end = parts[1].trim().toLongOrNull()
                if (end != null) return end + 1
            }
            return 0L
        }
    }
}
