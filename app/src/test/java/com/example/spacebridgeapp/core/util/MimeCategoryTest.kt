package com.example.spacebridgeapp.core.util

import com.example.spacebridgeapp.core.model.FileCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class MimeCategoryTest {
    @Test
    fun categorizeMimeType_documentTypes_returnsDocument() {
        val documents =
            listOf(
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.template",
                "text/plain",
                "text/markdown",
                "text/html",
                "application/rtf",
                "application/vnd.oasis.opendocument.text",
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "application/vnd.oasis.opendocument.presentation",
                "application/vnd.google-apps.document",
                "application/vnd.google-apps.presentation",
            )

        for (mime in documents) {
            assertEquals("Expected DOCUMENT for $mime", FileCategory.DOCUMENT, categorizeMimeType(mime))
            assertEquals("Expected DOCUMENT for $mime (extension)", FileCategory.DOCUMENT, mime.toFileCategory())
        }
    }

    @Test
    fun categorizeMimeType_spreadsheetTypes_returnsSpreadsheet() {
        val spreadsheets =
            listOf(
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.template",
                "text/csv",
                "application/vnd.oasis.opendocument.spreadsheet",
                "application/vnd.google-apps.spreadsheet",
            )

        for (mime in spreadsheets) {
            assertEquals("Expected SPREADSHEET for $mime", FileCategory.SPREADSHEET, categorizeMimeType(mime))
        }
    }

    @Test
    fun categorizeMimeType_imageTypes_returnsImage() {
        val images =
            listOf(
                "image/jpeg",
                "image/png",
                "image/webp",
                "image/gif",
                "image/svg+xml",
            )

        for (mime in images) {
            assertEquals("Expected IMAGE for $mime", FileCategory.IMAGE, categorizeMimeType(mime))
        }
    }

    @Test
    fun categorizeMimeType_otherAndEmptyTypes_returnsOther() {
        val others =
            listOf(
                "application/zip",
                "application/x-rar-compressed",
                "audio/mpeg",
                "video/mp4",
                "application/octet-stream",
                "",
                "   ",
                null,
                "random/unknown",
            )

        for (mime in others) {
            assertEquals("Expected OTHER for $mime", FileCategory.OTHER, categorizeMimeType(mime))
        }
    }

    @Test
    fun toDisplayName_returnsIndonesianLabels() {
        assertEquals("Dokumen", FileCategory.DOCUMENT.toDisplayName())
        assertEquals("Gambar", FileCategory.IMAGE.toDisplayName())
        assertEquals("Spreadsheet", FileCategory.SPREADSHEET.toDisplayName())
        assertEquals("Lainnya", FileCategory.OTHER.toDisplayName())
    }
}
