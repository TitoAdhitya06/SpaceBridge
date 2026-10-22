package com.example.spacebridgeapp.feature.trash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.spacebridgeapp.core.model.DriveFile
import com.example.spacebridgeapp.core.model.Note
import com.example.spacebridgeapp.core.model.NoteType
import com.example.spacebridgeapp.core.util.formatDateTime
import com.example.spacebridgeapp.core.util.formatFileSize
import com.example.spacebridgeapp.feature.files.components.getFileVisualMeta
import com.example.spacebridgeapp.ui.theme.SpaceBridgeTheme
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    viewModel: TrashViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val colors = SpaceBridgeTheme.colors

    var notePendingDelete by remember { mutableStateOf<Note?>(null) }
    var filePendingDelete by remember { mutableStateOf<DriveFile?>(null) }
    var showEmptyTrashDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is TrashEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    val totalItemsCount = uiState.notesCount + uiState.filesCount
    val canEmptyTrash = totalItemsCount > 0 && !uiState.isPurging

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Sampah",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = colors.textPrimary,
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { showEmptyTrashDialog = true },
                        enabled = canEmptyTrash,
                    ) {
                        Text(
                            text = "Kosongkan Sampah",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (canEmptyTrash) colors.danger600 else colors.textDisabled,
                        )
                    }

                    Box(
                        modifier =
                            Modifier
                                .padding(end = 12.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(colors.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        val initials =
                            uiState.user
                                ?.displayName
                                ?.take(2)
                                ?.uppercase()
                                ?: uiState.user
                                    ?.email
                                    ?.take(2)
                                    ?.uppercase()
                        if (!initials.isNullOrBlank()) {
                            Text(
                                text = initials,
                                color = colors.textOnAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = colors.textOnAccent,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.surface,
                    ),
                modifier =
                    Modifier.shadow(
                        elevation = 2.dp,
                        ambientColor = colors.textPrimary.copy(alpha = 0.04f),
                        spotColor = colors.textPrimary.copy(alpha = 0.08f),
                    ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
        ) {
            // Banner Retensi Kebijakan 30 Hari
            RetentionPolicyBanner(
                modifier = Modifier.padding(top = 12.dp),
            )

            // Segmented Tab Navigation
            TrashSegmentedTabs(
                selectedTab = uiState.selectedTab,
                notesCount = uiState.notesCount,
                filesCount = uiState.filesCount,
                onTabSelected = viewModel::onSelectTab,
                modifier = Modifier.padding(vertical = 12.dp),
            )

            // Konten Tab
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
            ) {
                when (uiState.selectedTab) {
                    TrashTab.NOTES -> {
                        if (uiState.isLoadingNotes && uiState.trashedNotes.isEmpty()) {
                            LoadingState()
                        } else if (uiState.trashedNotes.isEmpty()) {
                            EmptyTrashState(
                                message = "Tidak ada catatan di folder Sampah.",
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                items(uiState.trashedNotes, key = { it.id }) { note ->
                                    val isActing = uiState.actionInProgressId == note.id
                                    TrashedNoteCard(
                                        note = note,
                                        isProcessing = isActing,
                                        onRestore = {
                                            viewModel.onRestoreNote(note.id, note.title)
                                        },
                                        onDeletePermanently = {
                                            notePendingDelete = note
                                        },
                                    )
                                }
                            }
                        }
                    }
                    TrashTab.DRIVE_FILES -> {
                        if (uiState.isLoadingFiles && uiState.trashedFiles.isEmpty()) {
                            LoadingState()
                        } else if (uiState.trashedFiles.isEmpty()) {
                            EmptyTrashState(
                                message = "Tidak ada berkas di folder Sampah.",
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                items(uiState.trashedFiles, key = { it.id }) { file ->
                                    val isActing = uiState.actionInProgressId == file.id
                                    TrashedFileCard(
                                        file = file,
                                        isProcessing = isActing,
                                        onRestore = {
                                            viewModel.onRestoreFile(file.id, file.name)
                                        },
                                        onDeletePermanently = {
                                            filePendingDelete = file
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog Konfirmasi Hapus Permanen Catatan
    notePendingDelete?.let { note ->
        val title = note.title.trim().ifBlank { "Catatan Tanpa Judul" }
        DestructiveConfirmDialog(
            title = "Hapus Permanen?",
            message = "Catatan ‘$title’ akan dihapus secara permanen dari penyimpanan. Tindakan ini tidak dapat dibatalkan.",
            confirmButtonLabel = "Hapus Permanen",
            onDismiss = { notePendingDelete = null },
            onConfirm = {
                viewModel.onDeleteNotePermanently(note.id, note.title)
                notePendingDelete = null
            },
        )
    }

    // Dialog Konfirmasi Hapus Permanen Berkas
    filePendingDelete?.let { file ->
        val name = file.name.trim().ifBlank { "Berkas" }
        DestructiveConfirmDialog(
            title = "Hapus Permanen?",
            message = "Berkas ‘$name’ akan dihapus permanen dari Google Drive dan aplikasi. Tindakan ini tidak dapat dibatalkan.",
            confirmButtonLabel = "Hapus Permanen",
            onDismiss = { filePendingDelete = null },
            onConfirm = {
                viewModel.onDeleteFilePermanently(file.id, file.name)
                filePendingDelete = null
            },
        )
    }

    // Dialog Konfirmasi Kosongkan Sampah
    if (showEmptyTrashDialog) {
        DestructiveConfirmDialog(
            title = "Kosongkan Sampah?",
            message = "Semua catatan dan berkas di Sampah akan dihapus secara permanen dari Firestore dan Google Drive. Tindakan ini tidak dapat dibatalkan.",
            confirmButtonLabel = "Kosongkan Semua",
            onDismiss = { showEmptyTrashDialog = false },
            onConfirm = {
                viewModel.onEmptyTrash(currentTabOnly = false)
                showEmptyTrashDialog = false
            },
        )
    }
}

@Composable
private fun RetentionPolicyBanner(
    modifier: Modifier = Modifier,
) {
    val colors = SpaceBridgeTheme.colors
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colors.surfaceMuted, RoundedCornerShape(12.dp))
                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                .padding(12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(colors.primary50),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = colors.primary500,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = "Item di Sampah akan dihapus otomatis secara permanen setelah 30 hari.",
            fontSize = 12.sp,
            color = colors.textSecondary,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TrashSegmentedTabs(
    selectedTab: TrashTab,
    notesCount: Int,
    filesCount: Int,
    onTabSelected: (TrashTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SpaceBridgeTheme.colors

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colors.surfaceMuted, RoundedCornerShape(10.dp))
                .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SegmentedTabItem(
            selected = selectedTab == TrashTab.NOTES,
            icon = Icons.Default.Description,
            title = "Catatan",
            badgeCount = notesCount,
            onClick = { onTabSelected(TrashTab.NOTES) },
            modifier = Modifier.weight(1f),
        )
        SegmentedTabItem(
            selected = selectedTab == TrashTab.DRIVE_FILES,
            icon = Icons.Default.Folder,
            title = "Berkas Drive",
            badgeCount = filesCount,
            onClick = { onTabSelected(TrashTab.DRIVE_FILES) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SegmentedTabItem(
    selected: Boolean,
    icon: ImageVector,
    title: String,
    badgeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SpaceBridgeTheme.colors

    val backgroundColor = if (selected) colors.surface else Color.Transparent
    val contentColor = if (selected) colors.primary else colors.textSecondary
    val badgeBg = if (selected) colors.primary50 else colors.border
    val badgeTextColor = if (selected) colors.primary else colors.textSecondary

    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(8.dp))
                .background(backgroundColor)
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = contentColor,
        )
        Spacer(Modifier.width(6.dp))
        Box(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(badgeBg)
                    .padding(horizontal = 6.dp, vertical = 1.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = badgeCount.toString(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = badgeTextColor,
            )
        }
    }
}

@Composable
private fun TrashedNoteCard(
    note: Note,
    isProcessing: Boolean,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SpaceBridgeTheme.colors
    val deletedDateText = formatDateTime(note.trashedAt ?: note.updatedAt)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.surface,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Header Baris: Tipe Catatan + Tanggal Dihapus
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val isChecklist = note.type == NoteType.CHECKLIST
                val tagText = if (isChecklist) "Checklist" else "Catatan Teks"
                val tagIcon = if (isChecklist) Icons.Default.Checklist else Icons.AutoMirrored.Filled.Article

                Row(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isChecklist) colors.surfaceMuted else colors.primary50)
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = tagIcon,
                        contentDescription = null,
                        tint = if (isChecklist) colors.textSecondary else colors.primary,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = tagText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isChecklist) colors.textSecondary else colors.primary,
                    )
                }

                Text(
                    text = "Dihapus $deletedDateText",
                    fontSize = 11.sp,
                    color = colors.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Judul Catatan
            Text(
                text = note.title.trim().ifBlank { "(Tanpa judul)" },
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            // Cuplikan Isi Catatan
            if (note.type == NoteType.TEXT && note.body.isNotBlank()) {
                Text(
                    text = note.body.trim(),
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            } else if (note.type == NoteType.CHECKLIST && note.items.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    note.items.take(3).forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(colors.borderStrong),
                            )
                            Text(
                                text = item.text.trim().ifBlank { "Item tugas" },
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            // Tombol Aksi: Pulihkan & Hapus Permanen
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Tombol Pulihkan
                Row(
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surfaceMuted)
                            .clickable(enabled = !isProcessing, onClick = onRestore)
                            .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.RestoreFromTrash,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Pulihkan",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                    )
                }

                // Tombol Hapus Permanen
                Row(
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.danger50)
                            .clickable(enabled = !isProcessing, onClick = onDeletePermanently)
                            .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = colors.danger600,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Hapus Permanen",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.danger600,
                    )
                }
            }
        }
    }
}

@Composable
private fun TrashedFileCard(
    file: DriveFile,
    isProcessing: Boolean,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SpaceBridgeTheme.colors
    val deletedDateText = formatDateTime(file.trashedAt ?: file.updatedAt)
    val visualMeta = getFileVisualMeta(file.category, file.mimeType)
    val sizeText = formatFileSize(file.sizeBytes)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.surface,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Header Baris: Tipe Format Berkas + Tanggal Dihapus
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(visualMeta.backgroundColor)
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = visualMeta.icon,
                        contentDescription = null,
                        tint = visualMeta.iconColor,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = file.category.name.lowercase().replaceFirstChar { it.uppercase() },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = visualMeta.iconColor,
                    )
                }

                Text(
                    text = "Dihapus $deletedDateText",
                    fontSize = 11.sp,
                    color = colors.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Info Berkas: Ikon + Nama Berkas + Ukuran
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(visualMeta.backgroundColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = visualMeta.icon,
                        contentDescription = null,
                        tint = visualMeta.iconColor,
                        modifier = Modifier.size(24.dp),
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "$sizeText • Google Drive",
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                    )
                }
            }

            // Tombol Aksi: Pulihkan & Hapus Permanen
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Tombol Pulihkan
                Row(
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surfaceMuted)
                            .clickable(enabled = !isProcessing, onClick = onRestore)
                            .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.RestoreFromTrash,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Pulihkan",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                    )
                }

                // Tombol Hapus Permanen
                Row(
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.danger50)
                            .clickable(enabled = !isProcessing, onClick = onDeletePermanently)
                            .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = colors.danger600,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Hapus Permanen",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.danger600,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTrashState(
    message: String,
    modifier: Modifier = Modifier,
) {
    val colors = SpaceBridgeTheme.colors
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceMuted),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Kotak Sampah Kosong",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = message,
            fontSize = 13.sp,
            color = colors.textSecondary,
        )
    }
}

@Composable
private fun LoadingState(
    modifier: Modifier = Modifier,
) {
    val colors = SpaceBridgeTheme.colors
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = colors.primary,
            modifier = Modifier.size(36.dp),
            strokeWidth = 3.dp,
        )
    }
}

@Composable
private fun DestructiveConfirmDialog(
    title: String,
    message: String,
    confirmButtonLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = SpaceBridgeTheme.colors

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth(0.92f)
                    .navigationBarsPadding(),
            shape = RoundedCornerShape(20.dp),
            color = colors.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
            shadowElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(colors.danger50),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = colors.danger600,
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                        )
                        Text(
                            text = message,
                            fontSize = 13.sp,
                            color = colors.textSecondary,
                            lineHeight = 19.sp,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Tombol Batal
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.surfaceMuted)
                                .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Batal",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                        )
                    }

                    // Tombol Konfirmasi Destruktif
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.danger600)
                                .clickable(onClick = onConfirm),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = confirmButtonLabel,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textOnAccent,
                        )
                    }
                }
            }
        }
    }
}
