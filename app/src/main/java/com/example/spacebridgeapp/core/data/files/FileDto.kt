package com.example.spacebridgeapp.core.data.files

import com.example.spacebridgeapp.core.data.notes.toTimestamp
import com.example.spacebridgeapp.core.model.DriveFile
import com.example.spacebridgeapp.core.model.FileCategory
import com.example.spacebridgeapp.core.model.FileSource
import com.example.spacebridgeapp.core.model.FileStatus
import com.example.spacebridgeapp.core.util.categorizeMimeType
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class FileDto(
    @get:PropertyName("name") @set:PropertyName("name") var name: String = "",
    @get:PropertyName("mimeType") @set:PropertyName("mimeType") var mimeType: String = "",
    @get:PropertyName("sizeBytes") @set:PropertyName("sizeBytes") var sizeBytes: Long? = null,
    @get:PropertyName("category") @set:PropertyName("category") var category: String = "other",
    @get:PropertyName("source") @set:PropertyName("source") var source: String = "drive",
    @get:PropertyName("driveFileId") @set:PropertyName("driveFileId") var driveFileId: String? = null,
    @get:PropertyName("webViewLink") @set:PropertyName("webViewLink") var webViewLink: String? = null,
    @get:PropertyName("externalUrl") @set:PropertyName("externalUrl") var externalUrl: String? = null,
    @get:PropertyName("md5Checksum") @set:PropertyName("md5Checksum") var md5Checksum: String? = null,
    @get:PropertyName("status") @set:PropertyName("status") var status: String = "uploading",
    @get:PropertyName("uploadDeviceId") @set:PropertyName("uploadDeviceId") var uploadDeviceId: String? = null,
    @get:PropertyName("pendingDriveOp") @set:PropertyName("pendingDriveOp") var pendingDriveOp: String? = null,
    @get:PropertyName("shared") @set:PropertyName("shared") var shared: Boolean = false,
    @get:PropertyName("sharePermissionId") @set:PropertyName("sharePermissionId") var sharePermissionId: String? = null,
    @get:PropertyName("trashedAt") @set:PropertyName("trashedAt") var trashedAt: Timestamp? = null,
    @get:PropertyName("createdAt") @set:PropertyName("createdAt") @ServerTimestamp var createdAt: Timestamp? = null,
    @get:PropertyName("updatedAt") @set:PropertyName("updatedAt") @ServerTimestamp var updatedAt: Timestamp? = null,
    @get:PropertyName("schemaVersion") @set:PropertyName("schemaVersion") var schemaVersion: Int = 1,
) {
    fun toDomain(
        id: String,
        hasPendingWrites: Boolean = false,
    ): DriveFile {
        val domainCategory =
            when (category.lowercase()) {
                "document" -> FileCategory.DOCUMENT
                "image" -> FileCategory.IMAGE
                "spreadsheet" -> FileCategory.SPREADSHEET
                else -> FileCategory.OTHER
            }
        val domainSource =
            when (source.lowercase()) {
                "external_link" -> FileSource.EXTERNAL_LINK
                else -> FileSource.DRIVE
            }
        val domainStatus =
            when (status.lowercase()) {
                "uploading" -> FileStatus.UPLOADING
                "ready" -> FileStatus.READY
                "failed" -> FileStatus.FAILED
                "missing" -> FileStatus.MISSING
                "trashed" -> FileStatus.TRASHED
                else -> FileStatus.READY
            }

        return DriveFile(
            id = id,
            name = name,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            category = domainCategory,
            source = domainSource,
            driveFileId = driveFileId,
            webViewLink = webViewLink,
            externalUrl = externalUrl,
            status = domainStatus,
            uploadDeviceId = uploadDeviceId,
            pendingDriveOp = pendingDriveOp,
            shared = shared,
            trashedAt = trashedAt?.toInstant(),
            createdAt = createdAt?.toInstant(),
            updatedAt = updatedAt?.toInstant(),
            hasPendingWrites = hasPendingWrites,
        )
    }

    companion object {
        fun fromDomain(file: DriveFile): FileDto =
            FileDto(
                name = file.name,
                mimeType = file.mimeType,
                sizeBytes = file.sizeBytes,
                category = file.category.toFirestoreValue(),
                source = file.source.toFirestoreValue(),
                driveFileId = file.driveFileId,
                webViewLink = file.webViewLink,
                externalUrl = file.externalUrl,
                md5Checksum = null,
                status = file.status.toFirestoreValue(),
                uploadDeviceId = file.uploadDeviceId,
                pendingDriveOp = file.pendingDriveOp,
                shared = file.shared,
                sharePermissionId = null,
                trashedAt = file.trashedAt?.toTimestamp(),
                createdAt = file.createdAt?.toTimestamp(),
                updatedAt = file.updatedAt?.toTimestamp(),
                schemaVersion = 1,
            )
    }
}

fun FileCategory.toFirestoreValue(): String =
    when (this) {
        FileCategory.DOCUMENT -> "document"
        FileCategory.IMAGE -> "image"
        FileCategory.SPREADSHEET -> "spreadsheet"
        FileCategory.OTHER -> "other"
    }

fun FileSource.toFirestoreValue(): String =
    when (this) {
        FileSource.DRIVE -> "drive"
        FileSource.EXTERNAL_LINK -> "external_link"
    }

fun FileStatus.toFirestoreValue(): String =
    when (this) {
        FileStatus.UPLOADING -> "uploading"
        FileStatus.READY -> "ready"
        FileStatus.FAILED -> "failed"
        FileStatus.MISSING -> "missing"
        FileStatus.TRASHED -> "trashed"
    }

fun DriveFile.toFirestoreMap(): Map<String, Any?> =
    mapOf(
        "name" to name.take(255),
        "mimeType" to mimeType.take(255),
        "sizeBytes" to sizeBytes,
        "category" to category.toFirestoreValue(),
        "source" to source.toFirestoreValue(),
        "driveFileId" to driveFileId?.take(200),
        "webViewLink" to webViewLink?.take(2000),
        "externalUrl" to externalUrl?.take(2000),
        "md5Checksum" to null,
        "status" to status.toFirestoreValue(),
        "uploadDeviceId" to uploadDeviceId?.take(100),
        "pendingDriveOp" to pendingDriveOp,
        "shared" to shared,
        "sharePermissionId" to null,
        "trashedAt" to trashedAt?.toTimestamp(),
        "createdAt" to (createdAt?.toTimestamp() ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp(),
        "schemaVersion" to 1,
    )

fun DocumentSnapshot.toDriveFile(): DriveFile? {
    if (!exists()) return null
    return try {
        val dto = toObject(FileDto::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        dto?.toDomain(id = id, hasPendingWrites = metadata.hasPendingWrites()) ?: toDriveFileFallback(this)
    } catch (_: Exception) {
        toDriveFileFallback(this)
    }
}

private fun toDriveFileFallback(snapshot: DocumentSnapshot): DriveFile {
    val name = snapshot.getString("name") ?: ""
    val mimeType = snapshot.getString("mimeType") ?: "application/octet-stream"
    val sizeBytes = snapshot.getLong("sizeBytes")
    val catStr = snapshot.getString("category")
    val category =
        when (catStr?.lowercase()) {
            "document" -> FileCategory.DOCUMENT
            "image" -> FileCategory.IMAGE
            "spreadsheet" -> FileCategory.SPREADSHEET
            "other" -> FileCategory.OTHER
            else -> categorizeMimeType(mimeType)
        }
    val sourceStr = snapshot.getString("source")
    val source =
        if (sourceStr.equals("external_link", ignoreCase = true)) {
            FileSource.EXTERNAL_LINK
        } else {
            FileSource.DRIVE
        }
    val driveFileId = snapshot.getString("driveFileId")
    val webViewLink = snapshot.getString("webViewLink")
    val externalUrl = snapshot.getString("externalUrl")
    val statusStr = snapshot.getString("status")
    val status =
        when (statusStr?.lowercase()) {
            "uploading" -> FileStatus.UPLOADING
            "ready" -> FileStatus.READY
            "failed" -> FileStatus.FAILED
            "missing" -> FileStatus.MISSING
            "trashed" -> FileStatus.TRASHED
            else -> FileStatus.READY
        }
    val uploadDeviceId = snapshot.getString("uploadDeviceId")
    val pendingDriveOp = snapshot.getString("pendingDriveOp")
    val shared = snapshot.getBoolean("shared") ?: false
    val trashedAt = snapshot.getTimestamp("trashedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toInstant()
    val createdAt = snapshot.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toInstant()
    val updatedAt = snapshot.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toInstant()

    return DriveFile(
        id = snapshot.id,
        name = name,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        category = category,
        source = source,
        driveFileId = driveFileId,
        webViewLink = webViewLink,
        externalUrl = externalUrl,
        status = status,
        uploadDeviceId = uploadDeviceId,
        pendingDriveOp = pendingDriveOp,
        shared = shared,
        trashedAt = trashedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        hasPendingWrites = snapshot.metadata.hasPendingWrites(),
    )
}
