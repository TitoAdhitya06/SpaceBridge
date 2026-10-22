package com.example.spacebridgeapp.core.data.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.work.WorkManager
import androidx.work.await
import com.example.spacebridgeapp.R
import com.example.spacebridgeapp.core.data.prefs.PreferencesRepository
import com.example.spacebridgeapp.core.model.AppUser
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import com.example.spacebridgeapp.core.data.drive.DriveRepository
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import java.util.concurrent.CopyOnWriteArrayList

class AuthRepositoryImpl(
    private val auth: FirebaseAuth,
    private val context: Context,
    private val preferencesRepository: PreferencesRepository? = null,
    private val firestoreProvider: () -> FirebaseFirestore = { Firebase.firestore },
    private val driveRepositoryProvider: (() -> DriveRepository)? = null,
) : AuthRepository {
    constructor(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore,
        context: Context,
        preferencesRepository: PreferencesRepository? = null,
        driveRepositoryProvider: (() -> DriveRepository)? = null,
    ) : this(
        auth = auth,
        context = context,
        preferencesRepository = preferencesRepository,
        firestoreProvider = { firestore },
        driveRepositoryProvider = driveRepositoryProvider,
    )

    private val _currentUser = MutableStateFlow<AppUser?>(auth.currentUser?.toAppUser())
    override val currentUser: StateFlow<AppUser?> = _currentUser.asStateFlow()

    private val activeListeners = CopyOnWriteArrayList<ListenerRegistration>()

    private val authStateListener =
        FirebaseAuth.AuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser?.toAppUser()
        }

    init {
        auth.addAuthStateListener(authStateListener)
    }

    fun registerListener(registration: ListenerRegistration) {
        activeListeners.add(registration)
    }

    fun unregisterListener(registration: ListenerRegistration) {
        activeListeners.remove(registration)
        try {
            registration.remove()
        } catch (_: Exception) {
        }
    }

    fun stopAllActiveListeners() {
        for (registration in activeListeners) {
            try {
                registration.remove()
            } catch (_: Exception) {
            }
        }
        activeListeners.clear()
    }

    override suspend fun signIn(activity: Activity): Result<Unit> =
        withContext(Dispatchers.IO) {
            signInInternal(activity)
        }

    override suspend fun signIn(context: Context): Result<Unit> =
        withContext(Dispatchers.IO) {
            signInInternal(context)
        }

    private suspend fun signInInternal(callContext: Context): Result<Unit> {
        return try {
            val webClientId = callContext.getString(R.string.default_web_client_id)
            val option = GetSignInWithGoogleOption.Builder(webClientId).build()
            val request =
                GetCredentialRequest
                    .Builder()
                    .addCredentialOption(option)
                    .build()

            val credentialManager = CredentialManager.create(callContext)
            val credentialResponse = credentialManager.getCredential(callContext, request)
            val credential = credentialResponse.credential

            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val authCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()

                val firebaseUser =
                    authResult.user
                        ?: return Result.failure(IllegalStateException("Pengguna Firebase tidak ditemukan setelah autentikasi Google"))

                upsertUserProfile(firebaseUser)
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("Jenis kredensial tidak dikenali"))
            }
        } catch (e: GetCredentialCancellationException) {
            Result.failure(CancellationException("Login dibatalkan oleh pengguna"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upsert dokumen profil pengguna ke users/{uid} sesuai skema PRD Bagian 7.2.
     * Mengisi 8 field persis sesuai Firestore security rules Bagian 8.3.
     */
    suspend fun upsertUserProfile(user: FirebaseUser) {
        val uid = user.uid
        val currentFirestore = firestoreProvider()
        val userDoc = currentFirestore.collection("users").document(uid)

        // Cek dokumen yang sudah ada (coba server dulu, fallback ke cache)
        val snapshot =
            try {
                userDoc.get(Source.DEFAULT).await()
            } catch (_: Exception) {
                try {
                    userDoc.get(Source.CACHE).await()
                } catch (_: Exception) {
                    null
                }
            }

        val displayName = (user.displayName ?: "").take(100)
        val email = (user.email ?: "").take(200)
        val photoUrl = user.photoUrl?.toString()?.take(2000)

        val profileData: Map<String, Any?> =
            if (snapshot != null && snapshot.exists()) {
                val existingCreatedAt = snapshot.getTimestamp("createdAt") ?: Timestamp.now()
                val existingDriveFolderId = snapshot.getString("driveFolderId")
                val existingMigratedV3At = snapshot.getTimestamp("migratedV3At")

                mapOf(
                    "displayName" to displayName,
                    "email" to email,
                    "photoUrl" to photoUrl,
                    "driveFolderId" to existingDriveFolderId,
                    "migratedV3At" to existingMigratedV3At,
                    "createdAt" to existingCreatedAt,
                    "lastSeenAt" to FieldValue.serverTimestamp(),
                    "schemaVersion" to 1,
                )
            } else {
                mapOf(
                    "displayName" to displayName,
                    "email" to email,
                    "photoUrl" to photoUrl,
                    "driveFolderId" to null,
                    "migratedV3At" to null,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "lastSeenAt" to FieldValue.serverTimestamp(),
                    "schemaVersion" to 1,
                )
            }

        // Tulis dokumen ke users/{uid}. Timeout 3 detik agar tidak menggantung UI saat offline
        withTimeoutOrNull(3000L) {
            userDoc.set(profileData).await()
        } ?: run {
            userDoc.set(profileData)
        }
    }

    override suspend fun signOut(): Unit =
        withContext(Dispatchers.IO) {
            // 1. Hentikan seluruh snapshot listener aktif
            stopAllActiveListeners()

            // 2. Tutup dan bersihkan persistence Firestore
            try {
                val currentFirestore = firestoreProvider()
                currentFirestore.terminate().await()
                currentFirestore.clearPersistence().await()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }

            // 3. Bersihkan berkas sementara di context.cacheDir dan externalCacheDir
            try {
                context.cacheDir.listFiles()?.forEach { file ->
                    file.deleteRecursively()
                }
                context.externalCacheDir?.listFiles()?.forEach { file ->
                    file.deleteRecursively()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }

            // 4. Batalkan seluruh pekerjaan upload yang berjalan di WorkManager
            try {
                WorkManager.getInstance(context).cancelAllWork().await()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }

            // 5. Bersihkan data preferensi pengguna
            try {
                preferencesRepository?.clearUserPreferences()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }

            // 6. Bersihkan kredensial akun dan panggil Firebase sign out
            try {
                CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
            auth.signOut()
            _currentUser.value = null
        }

    override suspend fun deleteAccount(
        activity: Activity,
        trashDriveFolder: Boolean,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val currentUser =
                    auth.currentUser
                        ?: return@withContext Result.failure(IllegalStateException("Tidak ada pengguna yang sedang login"))

                val uid = currentUser.uid
                val currentFirestore = firestoreProvider()

                // 1. Buang folder SpaceBridge di Google Drive ke Sampah jika diminta
                if (trashDriveFolder) {
                    try {
                        val driveRepo = driveRepositoryProvider?.invoke()
                        val folderId = driveRepo?.findRootFolder()?.getOrNull()
                        if (folderId != null) {
                            driveRepo.setFileTrashed(folderId, true)
                        }
                    } catch (_: Exception) {
                    }
                }

                // 2. Hapus seluruh dokumen catatan di users/{uid}/notes
                try {
                    val notesSnapshot =
                        currentFirestore
                            .collection("users")
                            .document(uid)
                            .collection("notes")
                            .get()
                            .await()
                    for (doc in notesSnapshot.documents) {
                        doc.reference.delete().await()
                    }
                } catch (_: Exception) {
                }

                // 3. Hapus seluruh dokumen berkas di users/{uid}/files
                try {
                    val filesSnapshot =
                        currentFirestore
                            .collection("users")
                            .document(uid)
                            .collection("files")
                            .get()
                            .await()
                    for (doc in filesSnapshot.documents) {
                        doc.reference.delete().await()
                    }
                } catch (_: Exception) {
                }

                // 4. Hapus seluruh dokumen label di users/{uid}/labels
                try {
                    val labelsSnapshot =
                        currentFirestore
                            .collection("users")
                            .document(uid)
                            .collection("labels")
                            .get()
                            .await()
                    for (doc in labelsSnapshot.documents) {
                        doc.reference.delete().await()
                    }
                } catch (_: Exception) {
                }

                // 5. Hapus dokumen profil users/{uid}
                currentFirestore
                    .collection("users")
                    .document(uid)
                    .delete()
                    .await()

                // 6. Hapus akun pengguna di Firebase Authentication
                try {
                    currentUser.delete().await()
                } catch (e: FirebaseAuthRecentLoginRequiredException) {
                    val reauth = signInInternal(activity)
                    if (reauth.isSuccess) {
                        auth.currentUser?.delete()?.await()
                    } else {
                        return@withContext Result.failure(e)
                    }
                }

                // 7. Bersihkan seluruh sesi, cache, WorkManager, dan preferensi lokal
                signOut()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun FirebaseUser.toAppUser(): AppUser =
        AppUser(
            uid = uid,
            displayName = displayName ?: "",
            email = email ?: "",
            photoUrl = photoUrl?.toString(),
        )
}
