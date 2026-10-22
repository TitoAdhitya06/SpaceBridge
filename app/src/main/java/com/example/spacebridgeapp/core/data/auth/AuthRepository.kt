package com.example.spacebridgeapp.core.data.auth

import android.app.Activity
import android.content.Context
import com.example.spacebridgeapp.core.model.AppUser
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUser: StateFlow<AppUser?>

    suspend fun signIn(activity: Activity): Result<Unit>

    suspend fun signIn(context: Context): Result<Unit>

    suspend fun signOut()

    suspend fun deleteAccount(
        activity: Activity,
        trashDriveFolder: Boolean,
    ): Result<Unit>
}
