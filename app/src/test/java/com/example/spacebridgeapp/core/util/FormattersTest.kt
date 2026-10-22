package com.example.spacebridgeapp.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class FormattersTest {
    private val zoneJakarta: ZoneId = ZoneId.of("Asia/Jakarta")

    @Test
    fun formatFileSize_nullAndNegative_returnsDash() {
        assertEquals("-", formatFileSize(null))
        assertEquals("-", formatFileSize(-1L))
    }

    @Test
    fun formatFileSize_bytes_returnsCorrectFormat() {
        assertEquals("0 B", formatFileSize(0L))
        assertEquals("500 B", formatFileSize(500L))
        assertEquals("1023 B", formatFileSize(1023L))
    }

    @Test
    fun formatFileSize_kilobytes_returnsCorrectFormat() {
        assertEquals("1 KB", formatFileSize(1024L))
        assertEquals("1.5 KB", formatFileSize(1536L))
        assertEquals("2 KB", formatFileSize(2048L))
    }

    @Test
    fun formatFileSize_megabytes_returnsCorrectFormat() {
        assertEquals("1 MB", formatFileSize(1024L * 1024L))
        // 2.4 MB = 2.4 * 1024 * 1024 = 2516582.4
        val formatted = formatFileSize(2516582L)
        assertEquals("2.4 MB", formatted)
        assertEquals("48.2 MB", formatFileSize(50541363L))
    }

    @Test
    fun formatFileSize_gigabytes_returnsCorrectFormat() {
        assertEquals("1.0 GB", formatFileSize(1024L * 1024L * 1024L))
        assertEquals("2.5 GB", formatFileSize((2.5 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun formatDate_validInstant_formatsInIndonesian() {
        val instant = ZonedDateTime.of(2026, 10, 21, 14, 30, 0, 0, zoneJakarta).toInstant()
        val result = formatDate(instant, zoneJakarta)
        // Indonesia format: "21 Okt 2026"
        assertTrue("Expected to contain 21 and 2026, got: $result", result.contains("21") && result.contains("2026"))
        assertEquals("-", formatDate(null, zoneJakarta))
    }

    @Test
    fun formatDateTime_validInstant_formatsDateAndTime() {
        val instant = ZonedDateTime.of(2026, 10, 21, 14, 30, 0, 0, zoneJakarta).toInstant()
        val result = formatDateTime(instant, zoneJakarta)
        assertTrue("Expected to contain 14:30, got: $result", result.contains("14:30"))
        assertEquals("-", formatDateTime(null, zoneJakarta))
    }

    @Test
    fun formatRelativeDateTime_today_returnsHariIni() {
        val now = ZonedDateTime.of(2026, 10, 21, 15, 0, 0, 0, zoneJakarta).toInstant()
        val earlierToday = ZonedDateTime.of(2026, 10, 21, 10, 15, 0, 0, zoneJakarta).toInstant()

        val result = formatRelativeDateTime(earlierToday, now, zoneJakarta)
        assertEquals("Hari ini 10:15", result)
    }

    @Test
    fun formatRelativeDateTime_yesterday_returnsKemarin() {
        val now = ZonedDateTime.of(2026, 10, 21, 15, 0, 0, 0, zoneJakarta).toInstant()
        val yesterday = ZonedDateTime.of(2026, 10, 20, 14, 20, 0, 0, zoneJakarta).toInstant()

        val result = formatRelativeDateTime(yesterday, now, zoneJakarta)
        assertEquals("Kemarin 14:20", result)
    }

    @Test
    fun formatRelativeDateTime_sameYearEarlier_returnsShortDateAndTime() {
        val now = ZonedDateTime.of(2026, 10, 21, 15, 0, 0, 0, zoneJakarta).toInstant()
        val earlierThisYear = ZonedDateTime.of(2026, 8, 15, 9, 30, 0, 0, zoneJakarta).toInstant()

        val result = formatRelativeDateTime(earlierThisYear, now, zoneJakarta)
        assertTrue("Expected short date and time, got: $result", result.contains("15") && result.contains("09:30"))
    }

    @Test
    fun formatRelativeDateTime_previousYear_returnsFullDateAndTime() {
        val now = ZonedDateTime.of(2026, 10, 21, 15, 0, 0, 0, zoneJakarta).toInstant()
        val lastYear = ZonedDateTime.of(2025, 8, 15, 9, 30, 0, 0, zoneJakarta).toInstant()

        val result = formatRelativeDateTime(lastYear, now, zoneJakarta)
        assertTrue("Expected full date with year, got: $result", result.contains("2025") && result.contains("09:30"))
    }

    @Test
    fun formatRelativeDateTime_null_returnsDash() {
        assertEquals("-", formatRelativeDateTime(null, Instant.now(), zoneJakarta))
    }
}
