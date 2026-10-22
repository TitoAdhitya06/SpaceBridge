package com.example.spacebridgeapp.core.data.drive

import android.accounts.Account
import android.app.Activity
import android.content.Context
import android.content.Intent
import com.example.spacebridgeapp.core.data.prefs.PreferencesRepository
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.identity.AuthorizationClient
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class DriveAuthManagerImpl(
    private val context: Context,
    private val preferencesRepository: PreferencesRepository? = null,
    private val clientProvider: (Context) -> AuthorizationClient = { Identity.getAuthorizationClient(it) },
    coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
    private val userEmailProvider: () -> String?,
) : DriveAuthManager {
    constructor(
        context: Context,
        auth: FirebaseAuth,
        preferencesRepository: PreferencesRepository? = null,
        clientProvider: (Context) -> AuthorizationClient = { Identity.getAuthorizationClient(it) },
        coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
    ) : this(
        context = context,
        preferencesRepository = preferencesRepository,
        clientProvider = clientProvider,
        coroutineScope = coroutineScope,
        userEmailProvider = { auth.currentUser?.email },
    )

    private val _isAuthorized = MutableStateFlow(false)
    override val isAuthorized: StateFlow<Boolean> = _isAuthorized.asStateFlow()

    @Volatile
    private var lastKnownToken: String? = null

    init {
        if (preferencesRepository != null) {
            coroutineScope.launch {
                preferencesRepository.isDriveAuthorized.collect { authorized ->
                    _isAuthorized.value = authorized
                }
            }
        }
    }

    override suspend fun authorize(): DriveAuthResult =
        withContext(Dispatchers.IO) {
            authorizeInternal(context)
        }

    override suspend fun authorize(activity: Activity): DriveAuthResult =
        withContext(Dispatchers.IO) {
            authorizeInternal(activity)
        }

    private suspend fun authorizeInternal(clientContext: Context): DriveAuthResult {
        val email = userEmailProvider()
        if (email.isNullOrBlank()) {
            updateAuthorizedState(false)
            return DriveAuthResult.Failed("Pengguna belum login atau tidak memiliki email valid")
        }

        val request = buildAuthorizationRequest(email)
        val client = clientProvider(clientContext)

        return try {
            val result = client.authorize(request).await()
            if (result.hasResolution() && result.pendingIntent != null) {
                updateAuthorizedState(false)
                DriveAuthResult.NeedsConsent(result.pendingIntent!!)
            } else {
                val token = result.accessToken
                if (!token.isNullOrBlank()) {
                    lastKnownToken = token
                    updateAuthorizedState(true)
                    DriveAuthResult.Granted(token)
                } else {
                    updateAuthorizedState(false)
                    DriveAuthResult.Failed("Tidak ada token akses atau resolusi otorisasi")
                }
            }
        } catch (e: ApiException) {
            if (e.status.hasResolution() && e.status.resolution != null) {
                updateAuthorizedState(false)
                DriveAuthResult.NeedsConsent(e.status.resolution!!)
            } else {
                updateAuthorizedState(false)
                DriveAuthResult.Failed(e.message ?: "Otorisasi Play Services gagal (${e.statusCode})")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            updateAuthorizedState(false)
            DriveAuthResult.Failed(e.message ?: "Otorisasi Google Drive gagal")
        }
    }

    override suspend fun handleConsentResult(data: Intent?): DriveAuthResult =
        withContext(Dispatchers.IO) {
            if (data == null) {
                updateAuthorizedState(false)
                return@withContext DriveAuthResult.Failed("Tidak ada data izin yang diterima")
            }

            val client = clientProvider(context)
            try {
                val result = client.getAuthorizationResultFromIntent(data)
                val token = result.accessToken
                if (!token.isNullOrBlank()) {
                    lastKnownToken = token
                    updateAuthorizedState(true)
                    DriveAuthResult.Granted(token)
                } else {
                    updateAuthorizedState(false)
                    DriveAuthResult.Failed("Akses Google Drive tidak diberikan")
                }
            } catch (e: ApiException) {
                updateAuthorizedState(false)
                DriveAuthResult.Failed(e.message ?: "Gagal memproses hasil izin (${e.statusCode})")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                updateAuthorizedState(false)
                DriveAuthResult.Failed(e.message ?: "Gagal memproses hasil izin")
            }
        }

    override suspend fun getTokenSilently(): String? =
        withContext(Dispatchers.IO) {
            val email = userEmailProvider()
            if (email.isNullOrBlank()) {
                android.util.Log.e("DriveAuthManager", "getTokenSilently: email is null or blank!")
                return@withContext null
            }
            val request = buildAuthorizationRequest(email)
            val client = clientProvider(context)

            try {
                val result = client.authorize(request).await()
                val grantedScopesStr = result.grantedScopes.joinToString()
                android.util.Log.d("DriveAuthManager", "getTokenSilently result: hasResolution=${result.hasResolution()}, pendingIntent=${result.pendingIntent != null}, hasToken=${!result.accessToken.isNullOrBlank()}, scopes=[$grantedScopesStr]")
                if (!result.hasResolution() && !result.accessToken.isNullOrBlank()) {
                    val token = result.accessToken!!
                    lastKnownToken = token
                    updateAuthorizedState(true)
                    token
                } else {
                    android.util.Log.w("DriveAuthManager", "getTokenSilently hasResolution or null token")
                    updateAuthorizedState(false)
                    null
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("DriveAuthManager", "getTokenSilently exception: $e", e)
                updateAuthorizedState(false)
                null
            }
        }

    override suspend fun invalidateToken(token: String) =
        withContext(Dispatchers.IO) {
            if (lastKnownToken == token) {
                lastKnownToken = null
            }
            try {
                GoogleAuthUtil.clearToken(context, token)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }

    override suspend fun revoke() =
        withContext(Dispatchers.IO) {
            val token = lastKnownToken
            lastKnownToken = null
            updateAuthorizedState(false)
            if (token != null) {
                try {
                    GoogleAuthUtil.clearToken(context, token)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                }
            }
        }

    private suspend fun updateAuthorizedState(authorized: Boolean) {
        _isAuthorized.value = authorized
        try {
            preferencesRepository?.setDriveAuthorized(authorized)
        } catch (_: Exception) {
        }
    }

    private fun buildAuthorizationRequest(email: String): AuthorizationRequest =
        AuthorizationRequest
            .builder()
            .setRequestedScopes(listOf(Scope(DRIVE_FILE_SCOPE)))
            .setAccount(Account(email, "com.google"))
            .build()

    companion object {
        const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
    }
}
