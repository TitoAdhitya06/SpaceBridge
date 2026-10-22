package com.example.spacebridgeapp

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.spacebridgeapp.ui.theme.CosmicCyan
import com.example.spacebridgeapp.ui.theme.DeepVoid
import com.example.spacebridgeapp.ui.theme.GhostBorder
import com.example.spacebridgeapp.ui.theme.PrimaryIndigo
import com.example.spacebridgeapp.ui.theme.PrimaryLight
import com.example.spacebridgeapp.ui.theme.StatusSuccess
import com.example.spacebridgeapp.ui.theme.SurfaceContainer
import com.example.spacebridgeapp.ui.theme.SurfaceContainerHigh
import com.example.spacebridgeapp.ui.theme.TextMuted
import com.example.spacebridgeapp.ui.theme.TextPrimary
import com.example.spacebridgeapp.ui.theme.TextSecondary
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Login Google lalu tukar ID token ke Firebase Auth.
 * Mengembalikan pesan error, atau null kalau berhasil atau dibatalkan pengguna.
 */
suspend fun masukDenganGoogle(context: Context): String? {
    val option =
        GetSignInWithGoogleOption
            .Builder(context.getString(R.string.default_web_client_id))
            .build()
    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

    return try {
        val credential = CredentialManager.create(context).getCredential(context, request).credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            Firebase.auth
                .signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
                .await()
            null
        } else {
            "Jenis kredensial tidak dikenali"
        }
    } catch (_: GetCredentialCancellationException) {
        null
    } catch (e: GetCredentialException) {
        "Gagal login: ${e.message}"
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        "Gagal login: ${e.message}"
    }
}

/** Keluar dari Firebase dan bersihkan kredensial perangkat. */
suspend fun keluar(context: Context) {
    try {
        CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
    }
    Firebase.auth.signOut()
}

/** Pengguna yang sedang login, berubah otomatis saat login atau logout. */
@Composable
fun rememberFirebaseUser(): FirebaseUser? {
    var user by remember { mutableStateOf(Firebase.auth.currentUser) }
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { user = it.currentUser }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }
    return user
}

@Composable
fun HalamanLogin() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var pesanError by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DeepVoid,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Icon glow box
            Box(
                modifier =
                    Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(PrimaryIndigo.copy(alpha = 0.3f), CosmicCyan.copy(alpha = 0.2f)),
                            ),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Hub,
                    contentDescription = null,
                    modifier = Modifier.size(54.dp),
                    tint = PrimaryLight,
                )
            }

            Spacer(Modifier.height(24.dp))

            // Badge version
            Surface(
                shape = RoundedCornerShape(50),
                color = SurfaceContainerHigh,
                border = BorderStroke(1.dp, GhostBorder),
            ) {
                Text(
                    "v2.0 Cloud Sync",
                    color = CosmicCyan,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                "SpaceBridge",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Sinkronkan catatan realtime via Firebase Firestore dan kelola berkas aman di Google Drive.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(48.dp))

            // Tombol Card Google Sign-In
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(enabled = !loading) {
                            loading = true
                            pesanError = null
                            scope.launch {
                                pesanError = masukDenganGoogle(context)
                                loading = false
                            }
                        },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
                border = BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.5f)),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = PrimaryIndigo.copy(alpha = 0.2f),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (loading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = CosmicCyan,
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = PrimaryLight,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.width(16.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Masuk dengan Google",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                )
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = CosmicCyan.copy(alpha = 0.15f),
                                ) {
                                    Text(
                                        "Keluarga",
                                        color = CosmicCyan,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Kredensial Single Sign-On akun Google",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                            )
                        }
                    }
                }
            }

            pesanError?.let {
                Spacer(Modifier.height(16.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(32.dp))

            Text(
                "Sesi aman dengan Firebase Auth. Catatan terlindungi di akun Anda.",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun DialogAkun(
    user: FirebaseUser,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = SurfaceContainer,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "Akun Terpadu SpaceBridge",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = CosmicCyan.copy(alpha = 0.15f),
                ) {
                    Text(
                        "Google Auth",
                        color = CosmicCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                // Card Profil
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
                    border = BorderStroke(1.dp, GhostBorder),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier.size(46.dp),
                            shape = CircleShape,
                            color = PrimaryIndigo,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = (user.displayName?.take(1) ?: "U").uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = Color.White,
                                )
                            }
                        }

                        Spacer(Modifier.width(14.dp))

                        Column {
                            Text(
                                user.displayName ?: "Pengguna SpaceBridge",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                            )
                            Text(
                                user.email ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Status Sinkronisasi Realtime
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh.copy(alpha = 0.6f)),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = StatusSuccess,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Firebase Cloud Sync: Aktif",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.InsertDriveFile,
                                contentDescription = null,
                                tint = CosmicCyan,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Google Drive: Terhubung",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    scope.launch { keluar(context) }
                },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ExitToApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Keluar Akun", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup", color = TextSecondary)
            }
        },
    )
}
