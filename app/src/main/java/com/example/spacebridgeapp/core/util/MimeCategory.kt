package com.example.spacebridgeapp.core.util

import com.example.spacebridgeapp.core.model.FileCategory

/**
 * Pemetaan MIME type ke FileCategory domain sesuai spesifikasi Bagian 7.6 PRD.
 */
fun categorizeMimeType(mimeType: String?): FileCategory {
    if (mimeType.isNullOrBlank()) return FileCategory.OTHER

    val normalized = mimeType.trim().lowercase()

    // 1. Gambar
    if (normalized.startsWith("image/")) {
        return FileCategory.IMAGE
    }

    // 2. Spreadsheet (harus dicek sebelum text/* agar text/csv masuk kategori spreadsheet)
    if (normalized == "text/csv" ||
        normalized == "application/vnd.ms-excel" ||
        normalized == "application/vnd.oasis.opendocument.spreadsheet" ||
        normalized == "application/vnd.google-apps.spreadsheet" ||
        normalized.startsWith("application/vnd.openxmlformats-officedocument.spreadsheetml.")
    ) {
        return FileCategory.SPREADSHEET
    }

    // 3. Dokumen teks & presentasi
    if (normalized == "application/pdf" ||
        normalized == "application/msword" ||
        normalized == "application/rtf" ||
        normalized == "application/vnd.oasis.opendocument.text" ||
        normalized == "application/vnd.ms-powerpoint" ||
        normalized == "application/vnd.oasis.opendocument.presentation" ||
        normalized == "application/vnd.google-apps.document" ||
        normalized == "application/vnd.google-apps.presentation" ||
        normalized.startsWith("application/vnd.openxmlformats-officedocument.wordprocessingml.") ||
        normalized.startsWith("application/vnd.openxmlformats-officedocument.presentationml.") ||
        normalized.startsWith("text/")
    ) {
        return FileCategory.DOCUMENT
    }

    // 4. Lainnya
    return FileCategory.OTHER
}

fun String?.toFileCategory(): FileCategory = categorizeMimeType(this)

fun FileCategory.toDisplayName(): String =
    when (this) {
        FileCategory.DOCUMENT -> "Dokumen"
        FileCategory.IMAGE -> "Gambar"
        FileCategory.SPREADSHEET -> "Spreadsheet"
        FileCategory.OTHER -> "Lainnya"
    }
