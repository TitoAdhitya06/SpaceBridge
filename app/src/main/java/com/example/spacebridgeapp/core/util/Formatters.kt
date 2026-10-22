package com.example.spacebridgeapp.core.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

val LocaleIndonesia: Locale = Locale.forLanguageTag("id-ID")

/**
 * Format ukuran berkas ke representasi B, KB, MB, atau GB.
 */
fun formatFileSize(bytes: Long?): String {
    if (bytes == null || bytes < 0) return "-"
    if (bytes < 1024L) return "$bytes B"

    val kb = bytes / 1024.0
    if (kb < 1024.0) {
        val formatted =
            if (bytes % 1024L == 0L) {
                "${bytes / 1024L}"
            } else {
                String.format(Locale.US, "%.1f", kb)
            }
        return "$formatted KB"
    }

    val mb = kb / 1024.0
    if (mb < 1024.0) {
        val formatted =
            if (bytes % (1024L * 1024L) == 0L) {
                "${bytes / (1024L * 1024L)}"
            } else {
                String.format(Locale.US, "%.1f", mb)
            }
        return "$formatted MB"
    }

    val gb = mb / 1024.0
    val formatted = String.format(Locale.US, "%.1f", gb)
    return "$formatted GB"
}

/**
 * Format tanggal Indonesia: "21 Okt 2026".
 */
fun formatDate(
    instant: Instant?,
    zoneId: ZoneId = ZoneId.systemDefault(),
): String {
    if (instant == null) return "-"
    val formatter = DateTimeFormatter.ofPattern("d MMM yyyy", LocaleIndonesia).withZone(zoneId)
    return formatter.format(instant)
}

/**
 * Format tanggal dan waktu Indonesia: "21 Okt 2026, 15:30".
 */
fun formatDateTime(
    instant: Instant?,
    zoneId: ZoneId = ZoneId.systemDefault(),
): String {
    if (instant == null) return "-"
    val formatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", LocaleIndonesia).withZone(zoneId)
    return formatter.format(instant)
}

/**
 * Format tanggal relatif untuk tampilan waktu catatan dan berkas:
 * - Hari yang sama: "Hari ini 10:15"
 * - Kemarin: "Kemarin 14:20"
 * - Tahun yang sama: "21 Okt, 14:20"
 * - Tahun berbeda: "21 Okt 2025, 14:20"
 */
fun formatRelativeDateTime(
    instant: Instant?,
    now: Instant = Instant.now(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): String {
    if (instant == null) return "-"

    val itemDate = instant.atZone(zoneId).toLocalDate()
    val today = now.atZone(zoneId).toLocalDate()
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", LocaleIndonesia).withZone(zoneId)
    val timeString = timeFormatter.format(instant)

    return when {
        itemDate == today -> "Hari ini $timeString"
        itemDate == today.minusDays(1) -> "Kemarin $timeString"
        itemDate.year == today.year -> {
            val formatter = DateTimeFormatter.ofPattern("d MMM, HH:mm", LocaleIndonesia).withZone(zoneId)
            formatter.format(instant)
        }
        else -> {
            val formatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", LocaleIndonesia).withZone(zoneId)
            formatter.format(instant)
        }
    }
}
