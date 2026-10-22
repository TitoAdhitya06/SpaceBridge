package com.example.spacebridgeapp.core.work

import androidx.work.BackoffPolicy
import androidx.work.NetworkType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

class UploadWorkerTest {
    @Test
    fun `test parseNextByteFromRange handles bytes equal format`() {
        val nextByte = UploadWorker.parseNextByteFromRange("bytes=0-8388607")
        assertEquals(8388608L, nextByte)
    }

    @Test
    fun `test parseNextByteFromRange handles bytes space format`() {
        val nextByte = UploadWorker.parseNextByteFromRange("bytes 0-1048575")
        assertEquals(1048576L, nextByte)
    }

    @Test
    fun `test parseNextByteFromRange handles range with total size`() {
        val nextByte = UploadWorker.parseNextByteFromRange("bytes=0-1048575/5000000")
        assertEquals(1048576L, nextByte)
    }

    @Test
    fun `test parseNextByteFromRange handles plain range without prefix`() {
        val nextByte = UploadWorker.parseNextByteFromRange("0-999")
        assertEquals(1000L, nextByte)
    }

    @Test
    fun `test parseNextByteFromRange returns zero for null or invalid inputs`() {
        assertEquals(0L, UploadWorker.parseNextByteFromRange(null))
        assertEquals(0L, UploadWorker.parseNextByteFromRange(""))
        assertEquals(0L, UploadWorker.parseNextByteFromRange("   "))
        assertEquals(0L, UploadWorker.parseNextByteFromRange("bytes */5000000"))
        assertEquals(0L, UploadWorker.parseNextByteFromRange("invalid-range"))
    }

    @Test
    fun `test createWorkRequest creates valid request with wifi only constraint`() {
        val request =
            UploadWorker.createWorkRequest(
                fileId = "test-file-id-123",
                fileUriString = "content://media/external/file/123",
                fileName = "sample_doc.pdf",
                mimeType = "application/pdf",
                totalBytes = 1024L,
                userId = "user-abc-456",
                isWifiOnly = true,
            )

        val spec = request.workSpec
        assertEquals(NetworkType.UNMETERED, spec.constraints.requiredNetworkType)
        assertEquals(BackoffPolicy.EXPONENTIAL, spec.backoffPolicy)
        assertEquals(TimeUnit.SECONDS.toMillis(15), spec.backoffDelayDuration)
        assertTrue(request.tags.contains("upload-test-file-id-123"))
        assertTrue(request.tags.contains(UploadWorker.TAG_UPLOAD))

        val data = spec.input
        assertEquals("test-file-id-123", data.getString(UploadWorker.KEY_FILE_ID))
        assertEquals("content://media/external/file/123", data.getString(UploadWorker.KEY_FILE_URI))
        assertEquals("sample_doc.pdf", data.getString(UploadWorker.KEY_FILE_NAME))
        assertEquals("application/pdf", data.getString(UploadWorker.KEY_MIME_TYPE))
        assertEquals(1024L, data.getLong(UploadWorker.KEY_TOTAL_BYTES, 0L))
        assertEquals("user-abc-456", data.getString(UploadWorker.KEY_USER_ID))
    }

    @Test
    fun `test createWorkRequest creates valid request with connected network constraint when not wifi only`() {
        val request =
            UploadWorker.createWorkRequest(
                fileId = "test-file-id-789",
                fileUriString = "content://media/external/file/456",
                fileName = "photo.jpg",
                mimeType = "image/jpeg",
                totalBytes = 2048L,
                userId = "user-def-789",
                isWifiOnly = false,
            )

        val spec = request.workSpec
        assertEquals(NetworkType.CONNECTED, spec.constraints.requiredNetworkType)
        assertTrue(request.tags.contains("upload-test-file-id-789"))
    }

    @Test
    fun `test skipFully advances stream even when skip returns zero`() {
        val data = ByteArray(100) { it.toByte() }
        val zeroSkipStream =
            object : InputStream() {
                private val delegate = ByteArrayInputStream(data)

                override fun read(): Int = delegate.read()

                override fun read(
                    b: ByteArray,
                    off: Int,
                    len: Int,
                ): Int = delegate.read(b, off, len)

                override fun skip(n: Long): Long = 0L
            }

        assertTrue(UploadWorker.skipFully(zeroSkipStream, 40L))
        assertEquals(40, zeroSkipStream.read())
    }

    @Test
    fun `test skipFully returns false when stream ends before requested offset`() {
        val shortStream = ByteArrayInputStream(ByteArray(10))
        assertFalse(UploadWorker.skipFully(shortStream, 25L))
    }
}
