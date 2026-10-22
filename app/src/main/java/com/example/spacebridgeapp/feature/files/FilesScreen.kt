package com.example.spacebridgeapp.feature.files

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.spacebridgeapp.core.model.DriveFile
import com.example.spacebridgeapp.core.model.FileCategory
import com.example.spacebridgeapp.core.util.formatFileSize
import com.example.spacebridgeapp.core.util.formatRelativeDateTime
import com.example.spacebridgeapp.feature.files.components.FileActionsSheet
import com.example.spacebridgeapp.feature.files.components.getFileVisualMeta
import com.example.spacebridgeapp.ui.theme.SpaceBridgeTheme
import kotlinx.coroutines.flow.collectLatest

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    viewModel: FilesViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToTrash: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val colors = SpaceBridgeTheme.colors

    val consentLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartIntentSenderForResult(),
        ) { result ->
            viewModel.handleConsentResult(result.resultCode, result.data)
        }

    val filePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenMultipleDocuments(),
        ) { uris: List<Uri> ->
            for (uri in uris) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                } catch (_: Exception) {
                }
            }
            viewModel.uploadFiles(uris)
        }

    LaunchedEffect(viewModel.events) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is FilesEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is FilesEvent.LaunchConsent -> {
                    consentLauncher.launch(event.intentSenderRequest)
                }
                is FilesEvent.OpenUri -> {
                    val intent =
                        Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(event.uri, event.mimeType)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    try {
                        context.startActivity(Intent.createChooser(intent, "Buka Berkas"))
                    } catch (_: Exception) {
                        snackbarHostState.showSnackbar("Aplikasi pembaca berkas tidak ditemukan")
                    }
                }
                is FilesEvent.OpenWebLink -> {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(event.url))
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        snackbarHostState.showSnackbar("Gagal membuka tautan di browser")
                    }
                }
                is FilesEvent.ShareLink -> {
                    val shareIntent =
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, event.url)
                        }
                    context.startActivity(Intent.createChooser(shareIntent, "Bagikan Tautan Berkas"))
                }
            }
        }
    }

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isWideScreen = screenWidthDp >= 600
    val isCompactHeight = configuration.screenHeightDp < 500

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (isCompactHeight) {
                Surface(
                    color = colors.background,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f, fill = false),
                        ) {
                            Text(
                                text = "Berkas Drive",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                            )
                            val email = uiState.userEmail ?: "Menghubungkan..."
                            Text(
                                text = "• $email",
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation_compact")
                            val rotation by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec =
                                    infiniteRepeatable(
                                        animation = tween(1000, easing = LinearEasing),
                                    ),
                                label = "rotation",
                            )

                            IconButton(
                                onClick = { viewModel.refresh() },
                                enabled = !uiState.isSyncing,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Sinkronkan Berkas",
                                    tint = colors.textSecondary,
                                    modifier = if (uiState.isSyncing) Modifier.rotate(rotation) else Modifier,
                                )
                            }

                            IconButton(onClick = onNavigateToTrash) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "Buka Sampah",
                                    tint = colors.textSecondary,
                                )
                            }

                            IconButton(onClick = onNavigateToSettings) {
                                Box(
                                    modifier =
                                        Modifier
                                            .size(28.dp)
                                            .background(colors.primary, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Profil dan Pengaturan",
                                        tint = colors.textOnAccent,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Berkas Drive",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                            )
                            val email = uiState.userEmail ?: "Menghubungkan..."
                            Text(
                                text = email,
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    actions = {
                        val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
                        val rotation by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec =
                                infiniteRepeatable(
                                    animation = tween(1000, easing = LinearEasing),
                                ),
                            label = "rotation",
                        )

                        IconButton(
                            onClick = { viewModel.refresh() },
                            enabled = !uiState.isSyncing,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sinkronkan Berkas",
                                tint = colors.textSecondary,
                                modifier = if (uiState.isSyncing) Modifier.rotate(rotation) else Modifier,
                            )
                        }

                        IconButton(onClick = onNavigateToTrash) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Buka Sampah",
                                tint = colors.textSecondary,
                            )
                        }

                        IconButton(onClick = onNavigateToSettings) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(32.dp)
                                        .background(colors.primary, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Profil dan Pengaturan",
                                    tint = colors.textOnAccent,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = colors.background,
                        ),
                )
            }
        },
        floatingActionButton = {
            val fabPadding = if (isCompactHeight) 12.dp else 20.dp
            FloatingActionButton(
                onClick = {
                    if (!uiState.isDriveAuthorized) {
                        viewModel.connectDrive(context.findActivity())
                    } else {
                        filePickerLauncher.launch(arrayOf("*/*"))
                    }
                },
                containerColor = colors.primary,
                contentColor = colors.textOnAccent,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = fabPadding),
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = "Unggah Berkas ke Drive",
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .widthIn(max = 760.dp)
                        .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
                overscrollEffect = null,
            ) {
                // 1. Search Bar & Storage Quota
                if (isWideScreen) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SearchBarView(
                                query = uiState.searchQuery,
                                onQueryChange = { viewModel.onSearchQueryChanged(it) },
                                onClear = { viewModel.onSearchQueryChanged("") },
                                modifier = Modifier.weight(1.2f),
                            )
                            StorageQuotaCard(
                                fileCount = uiState.storageSummary.fileCount,
                                totalBytes = uiState.storageSummary.totalSizeBytes,
                                isAuthorized = uiState.isDriveAuthorized,
                                onConnectClick = {
                                    viewModel.connectDrive(context.findActivity())
                                },
                                compact = true,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                } else {
                    item {
                        SearchBarView(
                            query = uiState.searchQuery,
                            onQueryChange = { viewModel.onSearchQueryChanged(it) },
                            onClear = { viewModel.onSearchQueryChanged("") },
                        )
                    }

                    item {
                        StorageQuotaCard(
                            fileCount = uiState.storageSummary.fileCount,
                            totalBytes = uiState.storageSummary.totalSizeBytes,
                            isAuthorized = uiState.isDriveAuthorized,
                            onConnectClick = {
                                viewModel.connectDrive(context.findActivity())
                            },
                        )
                    }
                }

            // 3. Drive Auth Warning Banner
            if (!uiState.isDriveAuthorized) {
                item {
                    DriveAuthWarningBanner(
                        onConnectClick = {
                            viewModel.connectDrive(context.findActivity())
                        },
                    )
                }
            }

            // 4. Active Upload Queue Banner
            if (uiState.activeUploadingFile != null) {
                item {
                    ActiveUploadBanner(
                        file = uiState.activeUploadingFile!!,
                        progress = uiState.activeUploadProgress,
                        onCancel = { viewModel.cancelUpload(uiState.activeUploadingFile!!.id) },
                    )
                }
            }

            // 5. Category Filter Chips
            item {
                CategoryFilterCarousel(
                    selectedCategory = uiState.selectedCategory,
                    onSelect = { viewModel.onCategorySelected(it) },
                )
            }

            // 6. File List Items or Empty State
            if (uiState.filteredFiles.isEmpty()) {
                item {
                    EmptyFilesView(
                        onUploadClick = {
                            if (!uiState.isDriveAuthorized) {
                                viewModel.connectDrive(context.findActivity())
                            } else {
                                filePickerLauncher.launch(arrayOf("*/*"))
                            }
                        },
                    )
                }
            } else {
                items(
                    items = uiState.filteredFiles,
                    key = { it.id },
                ) { file ->
                    FileListItem(
                        file = file,
                        onClick = { viewModel.openLocalCopy(file) },
                        onMoreClick = { viewModel.onFileSelectedForActions(file) },
                    )
                }
            }
        }
    }
    }

    // Modal Actions Sheet
    uiState.selectedFileForActions?.let { file ->
        FileActionsSheet(
            file = file,
            onDismiss = { viewModel.onFileSelectedForActions(null) },
            onPreview = { viewModel.openLocalCopy(file) },
            onOpenInDrive = { viewModel.openInGoogleDrive(file) },
            onDownload = { viewModel.saveToDownloads(file.id) },
            onRename = {
                viewModel.onFileSelectedForActions(null)
                viewModel.onFileToRenameSelected(file)
            },
            onShare = { viewModel.shareFile(file.id) },
            onDelete = {
                viewModel.onFileSelectedForActions(null)
                viewModel.onFileToTrashSelected(file)
            },
        )
    }

    // Rename Dialog
    uiState.fileToRename?.let { file ->
        RenameFileDialog(
            initialName = file.name,
            onDismiss = { viewModel.onFileToRenameSelected(null) },
            onConfirm = { newName -> viewModel.renameFile(file.id, newName) },
        )
    }

    // Trash Confirmation Dialog
    uiState.fileToTrash?.let { file ->
        TrashConfirmDialog(
            fileName = file.name,
            onDismiss = { viewModel.onFileToTrashSelected(null) },
            onConfirm = { viewModel.moveToTrash(file.id) },
        )
    }

    // Local Copy Opening Overlay
    if (uiState.isOpeningLocalCopy) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surface,
                modifier = Modifier.padding(24.dp),
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    CircularProgressIndicator(
                        color = colors.primary,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                    )
                    Text(
                        text = "Mempersiapkan berkas...",
                        fontSize = 14.sp,
                        color = colors.textPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchBarView(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SpaceBridgeTheme.colors

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                text = "Cari nama berkas di Drive...",
                fontSize = 14.sp,
                color = colors.textTertiary,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(20.dp),
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = "Hapus pencarian",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface,
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.border,
            ),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun StorageQuotaCard(
    fileCount: Int,
    totalBytes: Long,
    isAuthorized: Boolean,
    onConnectClick: () -> Unit = {},
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = SpaceBridgeTheme.colors

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceMuted,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier.padding(
                    horizontal = if (compact) 12.dp else 14.dp,
                    vertical = if (compact) 8.dp else 14.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(if (compact) 26.dp else 32.dp)
                                .background(Color(0xFFE8F1FA), RoundedCornerShape(if (compact) 6.dp else 8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = Color(0xFF2F6FB0),
                            modifier = Modifier.size(if (compact) 16.dp else 20.dp),
                        )
                    }
                    Text(
                        text = "Folder: /SpaceBridge",
                        fontSize = if (compact) 12.5.sp else 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                    )
                }

                if (isAuthorized) {
                    Row(
                        modifier =
                            Modifier
                                .background(Color(0xFFE6F4EC), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF27774A).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = if (compact) 2.dp else 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(6.dp)
                                    .background(Color(0xFF27774A), CircleShape),
                        )
                        Text(
                            text = "Terhubung",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF27774A),
                        )
                    }
                } else {
                    Row(
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFEF3EB))
                                .clickable { onConnectClick() }
                                .padding(horizontal = 8.dp, vertical = if (compact) 2.dp else 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(6.dp)
                                    .background(Color(0xFFD97706), CircleShape),
                        )
                        Text(
                            text = "Perlu Sambungan",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFD97706),
                        )
                    }
                }
            }

            val sizeStr = formatFileSize(totalBytes)
            Text(
                text = "$fileCount berkas tersimpan • Total ukuran $sizeStr",
                fontSize = if (compact) 11.5.sp else 13.sp,
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
private fun DriveAuthWarningBanner(
    onConnectClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SpaceBridgeTheme.colors

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFEF3EB),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD97706).copy(alpha = 0.4f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFD97706),
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "Akses Google Drive perlu disambungkan ulang",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF92400E),
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = onConnectClick,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD97706),
                        contentColor = Color.White,
                    ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(text = "Sambungkan", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ActiveUploadBanner(
    file: DriveFile,
    progress: Float,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SpaceBridgeTheme.colors
    val percent = (progress * 100).toInt().coerceIn(0, 100)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.surfaceMuted,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.primary.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    CircularProgressIndicator(
                        color = colors.primary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp),
                    )
                    Column {
                        Text(
                            text = "SEDANG MENGUNGGAH",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary,
                        )
                        Text(
                            text = file.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Batalkan unggahan",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                color = colors.primary,
                trackColor = colors.border,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val sizeText = formatFileSize(file.sizeBytes)
                Text(
                    text = sizeText,
                    fontSize = 11.sp,
                    color = colors.textSecondary,
                )
                Text(
                    text = "$percent%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary,
                )
            }
        }
    }
}

@Composable
private fun CategoryFilterCarousel(
    selectedCategory: FileCategory?,
    onSelect: (FileCategory?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SpaceBridgeTheme.colors
    val categories =
        listOf(
            null to "Semua",
            FileCategory.DOCUMENT to "Dokumen",
            FileCategory.IMAGE to "Gambar",
            FileCategory.SPREADSHEET to "Spreadsheet",
            FileCategory.OTHER to "Lainnya",
        )

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for ((cat, label) in categories) {
            val isSelected = selectedCategory == cat
            val bgColor = if (isSelected) colors.primary50 else colors.surface
            val borderColor = if (isSelected) colors.primary else colors.border
            val textColor = if (isSelected) colors.primary else colors.textSecondary

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = bgColor,
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onSelect(cat) },
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = textColor,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }
    }
}

@Composable
private fun FileListItem(
    file: DriveFile,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SpaceBridgeTheme.colors
    val iconMeta = getFileVisualMeta(file.category, file.mimeType)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .background(iconMeta.backgroundColor, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = iconMeta.icon,
                    contentDescription = null,
                    tint = iconMeta.iconColor,
                    modifier = Modifier.size(22.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(2.dp))

                val sizeText = formatFileSize(file.sizeBytes)
                val dateText = formatRelativeDateTime(file.createdAt)
                Text(
                    text = "$sizeText • $dateText",
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                )
            }

            IconButton(onClick = onMoreClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Tindakan berkas",
                    tint = colors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun EmptyFilesView(
    onUploadClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SpaceBridgeTheme.colors

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(64.dp)
                    .background(colors.surfaceMuted, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(32.dp),
            )
        }

        Text(
            text = "Belum ada berkas di Drive",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
        )

        Text(
            text = "Unggah berkas pertama Anda untuk disimpan dengan aman di Google Drive akun Anda.",
            fontSize = 13.sp,
            color = colors.textSecondary,
            modifier = Modifier.padding(horizontal = 32.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = onUploadClick,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.textOnAccent,
                ),
            shape = RoundedCornerShape(10.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "Unggah Berkas", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun RenameFileDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    val colors = SpaceBridgeTheme.colors

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Ganti Nama Berkas", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama Berkas") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank() && name != initialName,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = colors.textSecondary)
            }
        },
    )
}

@Composable
private fun TrashConfirmDialog(
    fileName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = SpaceBridgeTheme.colors

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Pindahkan ke Sampah?", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                text = "Berkas '$fileName' akan dipindahkan ke Sampah dan dapat dipulihkan dalam 30 hari.",
                fontSize = 14.sp,
                color = colors.textSecondary,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = colors.danger),
            ) {
                Text("Pindahkan", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = colors.textSecondary)
            }
        },
    )
}
