package com.example.spacebridgeapp.core.data.files

import com.example.spacebridgeapp.core.model.DriveFile
import com.example.spacebridgeapp.core.model.FileCategory
import com.example.spacebridgeapp.core.model.FileSource
import com.example.spacebridgeapp.core.model.FileStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class FileDtoTest {
    @Test
    fun `test DriveFile to FileDto and back to DriveFile`() {
        val now = Instant.ofEpochSecond(1710000000, 500000000)
        val file =
            DriveFile(
                id = "file-test-1",
                name = "document.pdf",
                mimeType = "application/pdf",
                sizeBytes = 1048576L,
                category = FileCategory.DOCUMENT,
                source = FileSource.DRIVE,
                driveFileId = "drive-id-999",
                webViewLink = "https://drive.google.com/file/d/drive-id-999/view",
                externalUrl = null,
                status = FileStatus.READY,
                uploadDeviceId = "device-123",
                pendingDriveOp = null,
                shared = true,
                trashedAt = null,
                createdAt = now,
                updatedAt = now,
                hasPendingWrites = false,
            )

        val dto = FileDto.fromDomain(file)
        assertEquals("document.pdf", dto.name)
        assertEquals("application/pdf", dto.mimeType)
        assertEquals(1048576L, dto.sizeBytes)
        assertEquals("document", dto.category)
        assertEquals("drive", dto.source)
        assertEquals("drive-id-999", dto.driveFileId)
        assertEquals("https://drive.google.com/file/d/drive-id-999/view", dto.webViewLink)
        assertNull(dto.externalUrl)
        assertEquals("ready", dto.status)
        assertEquals("device-123", dto.uploadDeviceId)
        assertNull(dto.pendingDriveOp)
        assertTrue(dto.shared)
        assertNull(dto.trashedAt)
        assertEquals(1, dto.schemaVersion)

        val restored = dto.toDomain(id = "file-test-1", hasPendingWrites = true)
        assertEquals("file-test-1", restored.id)
        assertEquals("document.pdf", restored.name)
        assertEquals("application/pdf", restored.mimeType)
        assertEquals(1048576L, restored.sizeBytes)
        assertEquals(FileCategory.DOCUMENT, restored.category)
        assertEquals(FileSource.DRIVE, restored.source)
        assertEquals("drive-id-999", restored.driveFileId)
        assertEquals("https://drive.google.com/file/d/drive-id-999/view", restored.webViewLink)
        assertNull(restored.externalUrl)
        assertEquals(FileStatus.READY, restored.status)
        assertEquals("device-123", restored.uploadDeviceId)
        assertNull(restored.pendingDriveOp)
        assertTrue(restored.shared)
        assertNull(restored.trashedAt)
        assertEquals(now, restored.createdAt)
        assertEquals(now, restored.updatedAt)
        assertTrue(restored.hasPendingWrites)
    }

    @Test
    fun `test toFirestoreMap outputs exact 18 fields required by Firestore rules`() {
        val now = Instant.ofEpochSecond(1710000000)
        val file =
            DriveFile(
                id = "file-rule-test",
                name = "image.png",
                mimeType = "image/png",
                sizeBytes = 2048L,
                category = FileCategory.IMAGE,
                source = FileSource.DRIVE,
                driveFileId = "drive-id-img",
                webViewLink = "https://drive.google.com/img",
                externalUrl = null,
                status = FileStatus.READY,
                uploadDeviceId = "dev-1",
                pendingDriveOp = null,
                shared = false,
                trashedAt = null,
                createdAt = now,
                updatedAt = now,
            )

        val map = file.toFirestoreMap()
        assertEquals(18, map.size)

        val expectedKeys =
            setOf(
                "name",
                "mimeType",
                "sizeBytes",
                "category",
                "source",
                "driveFileId",
                "webViewLink",
                "externalUrl",
                "md5Checksum",
                "status",
                "uploadDeviceId",
                "pendingDriveOp",
                "shared",
                "sharePermissionId",
                "trashedAt",
                "createdAt",
                "updatedAt",
                "schemaVersion",
            )
        assertEquals(expectedKeys, map.keys)
    }

    @Test
    fun `test toFirestoreMap truncates strings according to rules`() {
        val longName = "a".repeat(300)
        val longMime = "b".repeat(300)
        val longDriveId = "c".repeat(250)
        val longWebLink = "d".repeat(2500)
        val longExternalUrl = "e".repeat(2500)
        val longDeviceId = "f".repeat(150)

        val file =
            DriveFile(
                id = "truncate-test",
                name = longName,
                mimeType = longMime,
                sizeBytes = 100L,
                category = FileCategory.OTHER,
                source = FileSource.EXTERNAL_LINK,
                driveFileId = longDriveId,
                webViewLink = longWebLink,
                externalUrl = longExternalUrl,
                status = FileStatus.UPLOADING,
                uploadDeviceId = longDeviceId,
                pendingDriveOp = null,
                shared = false,
            )

        val map = file.toFirestoreMap()
        assertEquals(255, (map["name"] as String).length)
        assertEquals(255, (map["mimeType"] as String).length)
        assertEquals(200, (map["driveFileId"] as String).length)
        assertEquals(2000, (map["webViewLink"] as String).length)
        assertEquals(2000, (map["externalUrl"] as String).length)
        assertEquals(100, (map["uploadDeviceId"] as String).length)
    }

    @Test
    fun `test category and status mappings cover all enum variants`() {
        assertEquals("document", FileCategory.DOCUMENT.toFirestoreValue())
        assertEquals("image", FileCategory.IMAGE.toFirestoreValue())
        assertEquals("spreadsheet", FileCategory.SPREADSHEET.toFirestoreValue())
        assertEquals("other", FileCategory.OTHER.toFirestoreValue())

        assertEquals("drive", FileSource.DRIVE.toFirestoreValue())
        assertEquals("external_link", FileSource.EXTERNAL_LINK.toFirestoreValue())

        assertEquals("uploading", FileStatus.UPLOADING.toFirestoreValue())
        assertEquals("ready", FileStatus.READY.toFirestoreValue())
        assertEquals("failed", FileStatus.FAILED.toFirestoreValue())
        assertEquals("missing", FileStatus.MISSING.toFirestoreValue())
        assertEquals("trashed", FileStatus.TRASHED.toFirestoreValue())
    }

    @Test
    fun `test toDomain handles all category and status string variants`() {
        val categories =
            listOf(
                "document" to FileCategory.DOCUMENT,
                "DOCUMENT" to FileCategory.DOCUMENT,
                "image" to FileCategory.IMAGE,
                "spreadsheet" to FileCategory.SPREADSHEET,
                "unknown" to FileCategory.OTHER,
            )
        for ((catStr, expectedCat) in categories) {
            val dto = FileDto(category = catStr)
            assertEquals(expectedCat, dto.toDomain("id").category)
        }

        val statuses =
            listOf(
                "uploading" to FileStatus.UPLOADING,
                "ready" to FileStatus.READY,
                "failed" to FileStatus.FAILED,
                "missing" to FileStatus.MISSING,
                "trashed" to FileStatus.TRASHED,
                "other" to FileStatus.READY,
            )
        for ((statusStr, expectedStatus) in statuses) {
            val dto = FileDto(status = statusStr)
            assertEquals(expectedStatus, dto.toDomain("id").status)
        }
    }
}
