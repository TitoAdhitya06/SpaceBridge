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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
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
        get() = injectedOkHttpClient ?: defaultHttpClient

    override suspend fun doWork(): Result =
        withContext(Dispatchers.IO) {
            val fileId = inputData.getString(KEY_FILE_ID) ?: return@withContext Result.failure()
            val uriString = inputData.getString(KEY_FILE_URI) ?: return@withContext Result.failure()
            val fileName = inputData.getString(KEY_FILE_NAME) ?: "Berkas"
            val mimeType = inputData.getString(KEY_MIME_TYPE) ?: "application/octet-stream"
            val totalBytes = inputData.getLong(KEY_TOTAL_BYTES, 0L)
            val userId = inputData.getString(KEY_USER_ID) ?: return@withContext Result.failure()

            val uri = Uri.parse(uriString)

            android.util.Log.d(
                "UploadWorker",
                "doWork started: fileId=$fileId, uri=$uriString, size=$totalBytes, user=$userId",
            )

            // Verifikasi izin baca berkas asli
            try {
                applicationContext.contentResolver.openInputStream(uri)?.close()
                android.util.Log.d("UploadWorker", "URI readable")
            } catch (e: Exception) {
                android.util.Log.e("UploadWorker", "URI not readable", e)
                updateFirestoreFileStatus(userId, fileId, "failed")
                return@withContext Result.failure()
            }

            // Tampilkan foreground notification awal
            val notificationId = fileId.hashCode()
            try {
                setForeground(createForegroundInfo(fileName, 0, notificationId))
            } catch (_: Exception) {
            }

            // Dapatkan atau inisiasi sesi resumable upload
            var sessionSuccess =
                when (val initResult = getOrInitiateSession(fileId, fileName, mimeType, totalBytes, userId)) {
                    is SessionInitResult.Success -> initResult
                    is SessionInitResult.Fatal -> {
                        android.util.Log.e("UploadWorker", "Fatal session init error: ${initResult.message}")
                        updateFirestoreFileStatus(userId, fileId, "failed")
                        showErrorNotification(initResult.message, notificationId)
                        return@withContext Result.failure()
                    }
                    is SessionInitResult.Retryable -> {
                        android.util.Log.w("UploadWorker", "Retryable session init error, attempt=$runAttemptCount")
                        if (runAttemptCount < 3) {
                            return@withContext Result.retry()
                        } else {
                            updateFirestoreFileStatus(userId, fileId, "failed")
                            showErrorNotification("Gagal mengunggah berkas ke Google Drive", notificationId)
                            return@withContext Result.failure()
                        }
                    }
                }

            var sessionUrl = sessionSuccess.sessionUrl
            android.util.Log.d("UploadWorker", "sessionUrl obtained: $sessionUrl, isResumed=${sessionSuccess.isResumed}")

            // Cek posisi byte terakhir hanya jika melanjutkan sesi lama
            var startByte =
                if (sessionSuccess.isResumed) {
                    queryCurrentPosition(sessionUrl, totalBytes)
                } else {
                    0L
                }

            if (startByte < 0L) {
                // Sesi lama sudah kedaluwarsa (404), buat sesi baru langsung tanpa menunggu backoff retry
                android.util.Log.w("UploadWorker", "startByte < 0, clearing expired session and creating new session")
                clearSavedSessionUrl(fileId)
                sessionSuccess =
                    when (val reinitResult = getOrInitiateSession(fileId, fileName, mimeType, totalBytes, userId)) {
                        is SessionInitResult.Success -> reinitResult
                        is SessionInitResult.Fatal -> {
                            updateFirestoreFileStatus(userId, fileId, "failed")
                            showErrorNotification(reinitResult.message, notificationId)
                            return@withContext Result.failure()
                        }
                        is SessionInitResult.Retryable -> return@withContext Result.retry()
                    }
                sessionUrl = sessionSuccess.sessionUrl
                startByte = 0L
            }

            val initialRatio =
                if (totalBytes > 0L) {
                    (startByte.toFloat() / totalBytes).coerceIn(0f, 0.99f)
                } else {
                    0f
                }
            setProgress(workDataOf("progress" to initialRatio))

            // Alirkan isi berkas secara berkesinambungan dalam satu stream PUT resumable
            var currentOffset = startByte
            while (currentOffset < totalBytes) {
                val chunkResult =
                    streamRemainingBytes(
                        uri = uri,
                        sessionUrl = sessionUrl,
                        startByte = currentOffset,
                        totalBytes = totalBytes,
                        mimeType = mimeType,
                        fileName = fileName,
                        notificationId = notificationId,
                    )

                when (chunkResult) {
                    is ChunkResult.Incomplete -> {
                        currentOffset = chunkResult.nextByte
                        val progressRatio = (currentOffset.toFloat() / totalBytes).coerceIn(0f, 0.99f)
                        val progressPercent = ((currentOffset * 100) / totalBytes).toInt().coerceIn(0, 99)
                        setProgress(workDataOf("progress" to progressRatio))
                        updateProgressNotification(fileName, progressPercent, notificationId)
                    }
                    is ChunkResult.Success -> {
                        setProgress(workDataOf("progress" to 1.0f))
                        updateFirestoreFileSuccess(userId, fileId, chunkResult.driveFileId, chunkResult.webViewLink)
                        clearSavedSessionUrl(fileId)
                        showSuccessNotification(fileName, notificationId)
                        return@withContext Result.success()
                    }
                    is ChunkResult.QuotaExceeded -> {
                        updateFirestoreFileStatus(userId, fileId, "failed")
                        showErrorNotification("Penyimpanan Google Drive penuh", notificationId)
                        return@withContext Result.failure()
                    }
                    is ChunkResult.NeedsConsent -> {
                        updateFirestoreFileStatus(userId, fileId, "failed")
                        showErrorNotification("Izin Google Drive dibutuhkan", notificationId)
                        return@withContext Result.failure()
                    }
                    is ChunkResult.Retryable -> {
                        val queriedOffset = queryCurrentPosition(sessionUrl, totalBytes)
                        if (queriedOffset > currentOffset && queriedOffset < totalBytes) {
                            currentOffset = queriedOffset
                            val progressRatio = (currentOffset.toFloat() / totalBytes).coerceIn(0f, 0.99f)
                            setProgress(workDataOf("progress" to progressRatio))
                        } else {
                            return@withContext Result.retry()
                        }
                    }
                    is ChunkResult.FatalError -> {
                        if (runAttemptCount < 3) {
                            return@withContext Result.retry()
                        }
                        updateFirestoreFileStatus(userId, fileId, "failed")
                        return@withContext Result.failure()
                    }
                }
            }

            setProgress(workDataOf("progress" to 1.0f))
            showSuccessNotification(fileName, notificationId)
            Result.success()
        }

    private sealed interface SessionInitResult {
        data class Success(
            val sessionUrl: String,
            val isResumed: Boolean,
        ) : SessionInitResult

        data class Fatal(
            val message: String,
        ) : SessionInitResult

        data object Retryable : SessionInitResult
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
            return SessionInitResult.Success(saved, isResumed = true)
        }

        val repository =
            driveRepository ?: run {
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
            return SessionInitResult.Success(sessionUrl, isResumed = false)
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

    private fun streamRemainingBytes(
        uri: Uri,
        sessionUrl: String,
        startByte: Long,
        totalBytes: Long,
        mimeType: String,
        fileName: String,
        notificationId: Int,
    ): ChunkResult {
        val endByte = totalBytes - 1
        val contentLength = totalBytes - startByte
        var lastReportedPercent = if (totalBytes > 0L) ((startByte * 100) / totalBytes).toInt() else 0
        var lastProgressUpdateMs = 0L
        var lastNotificationUpdateMs = 0L

        val body =
            object : RequestBody() {
                override fun contentType() = mimeType.toMediaType()

                override fun contentLength(): Long = contentLength

                override fun isOneShot(): Boolean = true

                override fun writeTo(sink: BufferedSink) {
                    val stream =
                        applicationContext.contentResolver.openInputStream(uri)
                            ?: throw IOException("Gagal membuka berkas untuk diunggah")
                    stream.use { input ->
                        if (!skipFully(input, startByte)) {
                            throw IOException("Gagal melompati posisi awal berkas")
                        }
                        val buffer = ByteArray(STREAM_BUFFER_SIZE)
                        var bytesSent = 0L
                        while (bytesSent < contentLength) {
                            val toRead = minOf(buffer.size.toLong(), contentLength - bytesSent).toInt()
                            val read = input.read(buffer, 0, toRead)
                            if (read == -1) break
                            sink.write(buffer, 0, read)
                            bytesSent += read

                            if (totalBytes > 0L) {
                                val currentTotal = startByte + bytesSent
                                val percent = ((currentTotal * 100) / totalBytes).toInt().coerceIn(0, 99)
                                val now = System.currentTimeMillis()
                                if (percent > lastReportedPercent && now - lastProgressUpdateMs >= PROGRESS_THROTTLE_MS) {
                                    lastReportedPercent = percent
                                    lastProgressUpdateMs = now
                                    val ratio = (currentTotal.toFloat() / totalBytes).coerceIn(0f, 0.99f)
                                    setProgressAsync(workDataOf("progress" to ratio))
                                    if (now - lastNotificationUpdateMs >= NOTIFICATION_THROTTLE_MS) {
                                        lastNotificationUpdateMs = now
                                        updateProgressNotification(fileName, percent, notificationId)
                                    }
                                }
                            }
                        }
                        sink.flush()
                        if (bytesSent < contentLength) {
                            throw IOException("Ukuran berkas yang terbaca kurang dari total ukuran yang diharapkan")
                        }
                    }
                }
            }

        val request =
            Request
                .Builder()
                .url(sessionUrl)
                .put(body)
                .header("Content-Range", "bytes $startByte-$endByte/$totalBytes")
                .header("Content-Length", contentLength.toString())
                .build()

        return try {
            httpClient.newCall(request).execute().use { response ->
                when (response.code) {
                    308 -> {
                        val range = response.header("Range")
                        val nextByte = parseNextByteFromRange(range).coerceAtLeast(startByte)
                        if (nextByte <= startByte) {
                            ChunkResult.Retryable
                        } else {
                            ChunkResult.Incomplete(nextByte)
                        }
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

    private fun updateProgressNotification(
        fileName: String,
        progressPercent: Int,
        notificationId: Int,
    ) {
        try {
            val channelId = NOTIFICATION_CHANNEL_ID
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notification =
                NotificationCompat
                    .Builder(applicationContext, channelId)
                    .setContentTitle("Mengunggah $fileName")
                    .setContentText("$progressPercent% selesai")
                    .setSmallIcon(android.R.drawable.stat_sys_upload)
                    .setProgress(100, progressPercent, false)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .build()
            manager.notify(notificationId, notification)
        } catch (_: Exception) {
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
        const val STREAM_BUFFER_SIZE = 64 * 1024 // 64 KB
        const val SKIP_BUFFER_SIZE = 32 * 1024 // 32 KB
        const val PROGRESS_THROTTLE_MS = 120L
        const val NOTIFICATION_THROTTLE_MS = 500L
        const val NOTIFICATION_CHANNEL_ID = "spacebridge_upload_channel"

        private val defaultHttpClient: OkHttpClient by lazy {
            OkHttpClient
                .Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()
        }

        fun skipFully(
            input: InputStream,
            bytesToSkip: Long,
        ): Boolean {
            if (bytesToSkip <= 0L) return true
            var remaining = bytesToSkip
            var skipBuffer: ByteArray? = null
            while (remaining > 0L) {
                val skipped = input.skip(remaining)
                if (skipped > 0L) {
                    remaining -= skipped
                } else {
                    val buf = skipBuffer ?: ByteArray(SKIP_BUFFER_SIZE).also { skipBuffer = it }
                    val toRead = minOf(buf.size.toLong(), remaining).toInt()
                    val read = input.read(buf, 0, toRead)
                    if (read == -1) return false
                    remaining -= read
                }
            }
            return true
        }

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
