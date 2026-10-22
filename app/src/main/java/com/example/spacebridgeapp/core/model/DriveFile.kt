package com.example.spacebridgeapp.core.model

import java.time.Instant

enum class FileCategory {
    DOCUMENT,
    IMAGE,
    SPREADSHEET,
    OTHER,
}

enum class FileSource {
    DRIVE,
    EXTERNAL_LINK,
}

enum class FileStatus {
    UPLOADING,
    READY,
    FAILED,
    MISSING,
    TRASHED,
}

data class DriveFile(
    val id: String,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long?,
    val category: FileCategory,
    val source: FileSource = FileSource.DRIVE,
    val driveFileId: String? = null,
    val webViewLink: String? = null,
    val externalUrl: String? = null,
    val status: FileStatus = FileStatus.READY,
    val uploadDeviceId: String? = null,
    val pendingDriveOp: String? = null,
    val shared: Boolean = false,
    val trashedAt: Instant? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
    val hasPendingWrites: Boolean = false,
)
