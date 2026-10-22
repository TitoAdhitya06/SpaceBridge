package com.example.spacebridgeapp.core.data.drive

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import kotlinx.coroutines.flow.StateFlow

sealed interface DriveAuthResult {
    data class Granted(
        val accessToken: String,
    ) : DriveAuthResult

    data class NeedsConsent(
        val pendingIntent: PendingIntent,
    ) : DriveAuthResult

    data class Failed(
        val reason: String,
    ) : DriveAuthResult
}

interface DriveAuthManager {
    val isAuthorized: StateFlow<Boolean>

    suspend fun authorize(): DriveAuthResult

    suspend fun authorize(activity: Activity): DriveAuthResult

    suspend fun handleConsentResult(data: Intent?): DriveAuthResult

    suspend fun getTokenSilently(): String?

    suspend fun invalidateToken(token: String)

    suspend fun revoke()
}
