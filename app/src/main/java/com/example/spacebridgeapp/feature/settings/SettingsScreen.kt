package com.example.spacebridgeapp.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.spacebridgeapp.core.model.AppUser
import com.example.spacebridgeapp.core.util.formatFileSize
import com.example.spacebridgeapp.feature.settings.components.DeleteAccountDialog
import com.example.spacebridgeapp.ui.theme.SpaceBridgeTheme

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onSignedOut: () -> Unit,
    onDeleteAccountClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }
    val consentLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartIntentSenderForResult(),
        ) { result ->
            viewModel.handleConsentResult(result.resultCode, result.data)
        }

    LaunchedEffect(uiState.isSignedOut) {
        if (uiState.isSignedOut) {
            onSignedOut()
        }
    }

    LaunchedEffect(viewModel.events) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is SettingsEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is SettingsEvent.LaunchConsent -> {
                    consentLauncher.launch(event.intentSenderRequest)
                }
            }
        }
    }

    var showSignOutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var infoDialogTitle by remember { mutableStateOf<String?>(null) }
    var infoDialogBody by remember { mutableStateOf<String?>(null) }

    val colors = SpaceBridgeTheme.colors

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SettingsTopBar(
                user = uiState.currentUser,
                onNavigateBack = onNavigateBack,
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Seksi 1: Akun Terhubung
            ConnectedAccountSection(
                user = uiState.currentUser,
                isDriveAuthorized = uiState.isDriveAuthorized,
                onChangeAccount = { showSignOutDialog = true },
                onSignOut = { showSignOutDialog = true },
                onConnectDrive = { viewModel.connectDrive(context.findActivity()) },
            )

            // Seksi 2: Sinkronisasi dan Jaringan
            SyncAndNetworkSection(
                isWifiOnly = uiState.isWifiOnlyUpload,
                isConnected = uiState.isConnected,
                onToggleWifiOnly = viewModel::onToggleWifiOnly,
            )

            // Seksi 3: Penyimpanan Perangkat
            DeviceStorageSection(
                cacheSizeBytes = uiState.cacheSizeBytes,
                maxCacheSizeBytes = uiState.maxCacheSizeBytes,
                isClearing = uiState.isClearingCache,
                isSuccess = uiState.isCacheClearedSuccess,
                onClearCache = viewModel::onClearCache,
            )

            // Seksi 4: Tampilan & Tema
            ThemeSection(
                selectedTheme = uiState.themeOption,
                onSelectTheme = viewModel::onSelectTheme,
            )

            // Seksi 5: Informasi Aplikasi
            AppInfoSection(
                appVersion = uiState.appVersion,
                onShowPrivacyPolicy = {
                    infoDialogTitle = "Kebijakan Privasi"
                    infoDialogBody =
                        "SpaceBridge menjaga privasi data Anda. Semua catatan dan lampiran berkas hanya disimpan pada ruang penyimpanan Firebase Firestore dan folder /SpaceBridge di Google Drive akun Anda sendiri. Aplikasi tidak membaca atau membagikan data kepada pihak ketiga."
                },
                onShowOpenSourceLicenses = {
                    infoDialogTitle = "Lisensi Open Source"
                    infoDialogBody =
                        "Aplikasi ini menggunakan berbagai komponen pustaka terbuka termasuk Jetpack Compose, Kotlinx Coroutines, AndroidX DataStore, WorkManager, Firebase Android SDK, Retrofit, dan OkHttp di bawah lisensi Apache 2.0."
                },
                onShowUserGuide = {
                    infoDialogTitle = "Panduan Penggunaan"
                    infoDialogBody =
                        "1. Catatan: Buat catatan teks atau daftar checklist. Data tersimpan secara otomatis dan tersinkronisasi realtime.\n2. Berkas Drive: Unggah dokumen dan gambar ke folder /SpaceBridge di Google Drive Anda.\n3. Sampah: Item yang dihapus akan disimpan di kotak Sampah selama 30 hari sebelum dibersihkan permanen."
                },
            )

            // Seksi 6: Zona Bahaya
            DangerZoneSection(
                onDeleteAccountClick = {
                    if (onDeleteAccountClick != null) {
                        onDeleteAccountClick()
                    } else {
                        showDeleteAccountDialog = true
                    }
                },
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = {
                Text(
                    text = "Keluar dari Akun?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
            },
            text = {
                Text(
                    text = "Sesi login Anda akan diakhiri. Berkas cache lokal akan dibersihkan dan Anda perlu masuk kembali menggunakan Akun Google.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        viewModel.onSignOut()
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.textOnAccent,
                        ),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("Keluar", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Batal", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(14.dp),
        )
    }

    if (showDeleteAccountDialog) {
        val activity = context.findActivity()
        DeleteAccountDialog(
            isDeleting = uiState.isDeletingAccount,
            errorMessage = uiState.deleteAccountError,
            onConfirmDelete = { trashDriveFolder ->
                if (activity != null) {
                    viewModel.onDeleteAccount(activity, trashDriveFolder)
                }
            },
            onDismiss = {
                showDeleteAccountDialog = false
                viewModel.clearDeleteAccountError()
            },
        )
    }

    infoDialogTitle?.let { title ->
        AlertDialog(
            onDismissRequest = {
                infoDialogTitle = null
                infoDialogBody = null
            },
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
            },
            text = {
                Text(
                    text = infoDialogBody ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        infoDialogTitle = null
                        infoDialogBody = null
                    },
                ) {
                    Text("Tutup", color = colors.primary, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(14.dp),
        )
    }
}

private fun android.content.Context.findActivity(): android.app.Activity? {
    var current = this
    while (current is android.content.ContextWrapper) {
        if (current is android.app.Activity) return current
        current = current.baseContext
    }
    return null
}

@Composable
private fun SettingsTopBar(
    user: AppUser?,
    onNavigateBack: () -> Unit,
) {
    val colors = SpaceBridgeTheme.colors
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .border(width = 1.dp, color = colors.border)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = colors.textPrimary,
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Pengaturan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            )
        }

        Box(
            modifier =
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(colors.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            val initials = getInitials(user?.displayName.orEmpty(), user?.email.orEmpty())
            if (initials.isNotEmpty()) {
                Text(
                    text = initials,
                    color = colors.primary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Avatar Pengguna",
                    tint = colors.textOnAccent,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun ConnectedAccountSection(
    user: AppUser?,
    isDriveAuthorized: Boolean,
    onChangeAccount: () -> Unit,
    onSignOut: () -> Unit,
    onConnectDrive: () -> Unit = {},
) {
    val colors = SpaceBridgeTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surface,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(colors.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    val initials = getInitials(user?.displayName.orEmpty(), user?.email.orEmpty())
                    Text(
                        text = initials,
                        color = colors.primary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user?.displayName?.ifBlank { "Pengguna SpaceBridge" } ?: "Pengguna SpaceBridge",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = user?.email?.ifBlank { "Akun belum terhubung" } ?: "Akun belum terhubung",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Badges status
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Firebase Auth status badge
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surfaceMuted)
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(if (user != null) colors.success600 else colors.danger600),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Firebase Auth: ",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                    )
                    Text(
                        text = if (user != null) "Terautentikasi" else "Tidak Terhubung",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (user != null) colors.success600 else colors.danger600,
                    )
                }

                // Google Drive status badge
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surfaceMuted)
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = if (isDriveAuthorized) colors.primary else colors.textTertiary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Google Drive: ",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                    )
                    Text(
                        text = if (isDriveAuthorized) "Folder /SpaceBridge Terhubung" else "Belum Diizinkan",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDriveAuthorized) colors.primary else colors.textTertiary,
                        modifier = Modifier.weight(1f),
                    )
                    if (!isDriveAuthorized) {
                        TextButton(
                            onClick = onConnectDrive,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        ) {
                            Text(
                                text = "Hubungkan",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.primary,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tombol Aksi Akun
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onChangeAccount,
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(42.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = colors.surfaceMuted,
                            contentColor = colors.textPrimary,
                        ),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.SwitchAccount,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ganti Akun",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Button(
                    onClick = onSignOut,
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(42.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = colors.surfaceMuted,
                            contentColor = colors.textSecondary,
                        ),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Keluar",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncAndNetworkSection(
    isWifiOnly: Boolean,
    isConnected: Boolean,
    onToggleWifiOnly: (Boolean) -> Unit,
) {
    val colors = SpaceBridgeTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surface,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Sinkronisasi Berkas",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Unggah hanya lewat Wi-Fi",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Menunda unggahan berkas besar saat menggunakan data seluler untuk menghemat kuota.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Switch(
                    checked = isWifiOnly,
                    onCheckedChange = onToggleWifiOnly,
                    colors =
                        SwitchDefaults.colors(
                            checkedThumbColor = colors.surface,
                            checkedTrackColor = colors.primary,
                            uncheckedThumbColor = colors.textSecondary,
                            uncheckedTrackColor = colors.surfaceMuted,
                        ),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surfaceMuted)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = if (isConnected) colors.success600 else colors.warning500,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Status Jaringan: ",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
                Text(
                    text = if (isConnected) "Terhubung ke Internet" else "Terputus (Mode Offline)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isConnected) colors.textPrimary else colors.warning500,
                )
            }
        }
    }
}

@Composable
private fun DeviceStorageSection(
    cacheSizeBytes: Long,
    maxCacheSizeBytes: Long,
    isClearing: Boolean,
    isSuccess: Boolean,
    onClearCache: () -> Unit,
) {
    val colors = SpaceBridgeTheme.colors
    val progress = (cacheSizeBytes.toFloat() / maxCacheSizeBytes.toFloat()).coerceIn(0f, 1f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surface,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Penyimpanan Perangkat",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Cache pratinjau lokal",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                )
                Box(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(colors.surfaceMuted)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = "${formatFileSize(cacheSizeBytes)} / ${formatFileSize(maxCacheSizeBytes)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                color = colors.primary,
                trackColor = colors.surfaceMuted,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Menyimpan salinan sementara dokumen dan gambar yang dibuka agar dapat dibaca kembali saat offline.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onClearCache,
                enabled = !isClearing,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = colors.surfaceMuted,
                        contentColor = colors.textPrimary,
                        disabledContainerColor = colors.surfaceMuted,
                        disabledContentColor = colors.textTertiary,
                    ),
                shape = RoundedCornerShape(8.dp),
            ) {
                if (isClearing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = colors.primary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Membersihkan cache...",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                } else if (isSuccess) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = colors.success600,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Berhasil Dibersihkan",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.success600,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Bersihkan Cache Lokal",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeSection(
    selectedTheme: ThemeOption,
    onSelectTheme: (ThemeOption) -> Unit,
) {
    val colors = SpaceBridgeTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surface,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Tema Antarmuka",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Segmented Pill Selector
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surfaceMuted)
                        .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ThemeOption.entries.forEach { option ->
                    val isSelected = option == selectedTheme
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) colors.primary else Color.Transparent)
                                .clickable { onSelectTheme(option) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) colors.textOnAccent else colors.textSecondary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Mode gelap menggunakan palet arang hangat tanpa kontras tinggi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun AppInfoSection(
    appVersion: String,
    onShowPrivacyPolicy: () -> Unit,
    onShowOpenSourceLicenses: () -> Unit,
    onShowUserGuide: () -> Unit,
) {
    val colors = SpaceBridgeTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surface,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surfaceMuted)
                        .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier =
                            Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderCopy,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "SpaceBridge",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = appVersion,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary,
                        )
                    }
                }

                Box(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.surface)
                            .border(1.dp, colors.border, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = "Stabil",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            AppInfoRow(title = "Kebijakan Privasi", onClick = onShowPrivacyPolicy)
            AppInfoRow(title = "Lisensi Open Source", onClick = onShowOpenSourceLicenses)
            AppInfoRow(title = "Panduan Penggunaan", onClick = onShowUserGuide)
        }
    }
}

@Composable
private fun AppInfoRow(
    title: String,
    onClick: () -> Unit,
) {
    val colors = SpaceBridgeTheme.colors
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textPrimary,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun DangerZoneSection(
    onDeleteAccountClick: () -> Unit,
) {
    val colors = SpaceBridgeTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.danger50,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.danger.copy(alpha = 0.35f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = colors.danger600,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Zona Bahaya",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.danger600,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Penghapusan akun bersifat permanen. Seluruh catatan Firestore dan opsi penghapusan folder SpaceBridge di Google Drive akan dijalankan.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                lineHeight = 18.sp,
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onDeleteAccountClick,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = colors.danger600,
                        contentColor = Color.White,
                    ),
                shape = RoundedCornerShape(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Hapus Akun dan Seluruh Data",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

fun getInitials(
    name: String,
    email: String,
): String {
    val clean = name.trim()
    if (clean.isNotEmpty()) {
        val parts = clean.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (parts.size >= 2) {
            return "${parts[0].first().uppercase()}${parts[1].first().uppercase()}"
        }
        return clean.take(2).uppercase()
    }
    val emailPart = email.substringBefore("@").trim()
    if (emailPart.isNotEmpty()) {
        return emailPart.take(2).uppercase()
    }
    return "SB"
}
