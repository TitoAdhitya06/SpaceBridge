package com.example.spacebridgeapp.core.data.drive

open class DriveException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

class DriveNeedsConsentException(
    message: String = "Izin Google Drive dibutuhkan",
) : DriveException(message)

class DriveQuotaExceededException(
    message: String = "Penyimpanan Google Drive penuh",
) : DriveException(message)

class DriveRateLimitException(
    message: String = "Batas kecepatan Google Drive tercapai",
) : DriveException(message)

class DriveFileNotFoundException(
    message: String = "File tidak ditemukan di Google Drive",
) : DriveException(message)

class DriveServerException(
    message: String,
) : DriveException(message)

class DriveApiDisabledException(
    message: String = "Google Drive API belum diaktifkan di Google Cloud Console",
) : DriveException(message)
