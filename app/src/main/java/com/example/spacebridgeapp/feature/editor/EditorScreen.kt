package com.example.spacebridgeapp.feature.editor

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddToDrive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.spacebridgeapp.core.model.NoteType
import com.example.spacebridgeapp.feature.editor.components.ChecklistSection
import com.example.spacebridgeapp.ui.theme.SpaceBridgeTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onNoteTrashed: ((String) -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val colors = SpaceBridgeTheme.colors
    val clipboardManager = LocalClipboardManager.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var showMenu by remember { mutableStateOf(false) }
    var showLabelSheet by remember { mutableStateOf(false) }

    // Simpan otomatis saat back ditekan atau Activity onPause
    val handleExit = {
        viewModel.flushAndCleanup()
        onNavigateBack()
    }

    BackHandler {
        handleExit()
    }

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_PAUSE) {
                    viewModel.flushAndCleanup()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is EditorEvent.NavigateBack -> onNavigateBack()
                is EditorEvent.NoteTrashed -> {
                    if (onNoteTrashed != null) {
                        onNoteTrashed(event.noteId)
                    } else {
                        onNavigateBack()
                    }
                }
                is EditorEvent.ShowToast -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        topBar = {
            EditorTopAppBar(
                saveStatus = uiState.saveStatus,
                isPinned = uiState.pinned,
                onBackClick = handleExit,
                onTogglePin = viewModel::onTogglePin,
                onMenuClick = { showMenu = true },
                colors = colors,
            )
        },
        bottomBar = {
            EditorBottomBar(
                type = uiState.type,
                wordCount = uiState.wordCount,
                charCount = uiState.charCount,
                attachmentCount = uiState.attachmentIds.size,
                onConvertType = viewModel::onConvertType,
                onDriveClick = {
                    Toast.makeText(context, "Lampiran Drive akan tersedia di Fase 3", Toast.LENGTH_SHORT).show()
                },
                onCameraClick = {
                    Toast.makeText(context, "Fitur kamera segera hadir", Toast.LENGTH_SHORT).show()
                },
                colors = colors,
            )
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Input Judul Dokumen (Borderless font headline-md 24sp)
                BasicTextField(
                    value = uiState.title,
                    onValueChange = viewModel::onTitleChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle =
                        TextStyle(
                            color = colors.textPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.5).sp,
                        ),
                    cursorBrush = SolidColor(colors.primary),
                    singleLine = false,
                    maxLines = 3,
                    decorationBox = { innerTextField ->
                        if (uiState.title.isEmpty()) {
                            Text(
                                text = "Judul Catatan...",
                                color = colors.textTertiary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.5).sp,
                            )
                        }
                        innerTextField()
                    },
                )

                // Baris Metadata & Label
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Waktu Terakhir Disunting
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(vertical = 4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = colors.textTertiary,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = formatMetadataTimestamp(uiState.updatedAt),
                            color = colors.textTertiary,
                            fontSize = 12.sp,
                        )
                    }

                    // Label Chips
                    uiState.labelIds.forEach { label ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.primary50)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = label,
                                color = colors.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Hapus label",
                                tint = colors.primary,
                                modifier =
                                    Modifier
                                        .size(12.dp)
                                        .clickable { viewModel.onRemoveLabel(label) },
                            )
                        }
                    }

                    // Tombol Tambah Label
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.surfaceMuted)
                                .clickable { showLabelSheet = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = "Tambah Label",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                        )
                    }
                }

                // Lampiran Berkas Drive (jika ada)
                if (uiState.attachmentIds.isNotEmpty()) {
                    DriveAttachmentPreview(
                        attachmentIds = uiState.attachmentIds,
                        colors = colors,
                    )
                }

                // Kanvas Utama Catatan
                if (uiState.type == NoteType.TEXT) {
                    BasicTextField(
                        value = uiState.body,
                        onValueChange = viewModel::onBodyChange,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(400.dp),
                        textStyle =
                            TextStyle(
                                color = colors.textPrimary,
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                            ),
                        cursorBrush = SolidColor(colors.primary),
                        decorationBox = { innerTextField ->
                            if (uiState.body.isEmpty()) {
                                Text(
                                    text = "Mulai menulis catatan keluarga Anda...",
                                    color = colors.textTertiary,
                                    fontSize = 16.sp,
                                    lineHeight = 24.sp,
                                )
                            }
                            innerTextField()
                        },
                    )
                } else {
                    ChecklistSection(
                        items = uiState.items,
                        focusedItemId = uiState.focusedItemId,
                        onItemTextChange = viewModel::onItemTextChange,
                        onToggleItem = viewModel::onToggleItem,
                        onDeleteItem = viewModel::onDeleteItem,
                        onAddItem = viewModel::onAddItem,
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            // Dropdown Overflow Menu
            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier =
                        Modifier
                            .background(colors.surface)
                            .border(1.dp, colors.border, RoundedCornerShape(8.dp)),
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (uiState.type == NoteType.TEXT) "Ubah jadi Checklist" else "Ubah jadi Teks",
                                color = colors.textPrimary,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (uiState.type == NoteType.TEXT) Icons.Default.Checklist else Icons.Default.Description,
                                contentDescription = null,
                                tint = colors.primary,
                            )
                        },
                        onClick = {
                            showMenu = false
                            viewModel.onConvertType()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Beri Label", color = colors.textPrimary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Label,
                                contentDescription = null,
                                tint = colors.textSecondary,
                            )
                        },
                        onClick = {
                            showMenu = false
                            showLabelSheet = true
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Salin Seluruh Catatan", color = colors.textPrimary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = colors.textSecondary,
                            )
                        },
                        onClick = {
                            showMenu = false
                            val fullContent =
                                buildString {
                                    if (uiState.title.isNotBlank()) appendLine(uiState.title)
                                    if (uiState.type == NoteType.TEXT) {
                                        append(uiState.body)
                                    } else {
                                        uiState.items.forEach { item ->
                                            appendLine("${if (item.checked) "[x]" else "[ ]"} ${item.text}")
                                        }
                                    }
                                }
                            clipboardManager.setText(AnnotatedString(fullContent))
                            Toast.makeText(context, "Catatan disalin ke papan klip", Toast.LENGTH_SHORT).show()
                        },
                    )
                    HorizontalDivider(color = colors.border)
                    DropdownMenuItem(
                        text = { Text("Pindahkan ke Sampah", color = colors.danger) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = colors.danger,
                            )
                        },
                        onClick = {
                            showMenu = false
                            viewModel.onMoveToTrash()
                        },
                    )
                }
            }
        }
    }

    // Modal Bottom Sheet untuk Menambah Label
    if (showLabelSheet) {
        val sheetState = rememberModalBottomSheetState()
        var newLabelInput by remember { mutableStateOf("") }
        val presetLabels = listOf("Rumah", "Keluarga", "Belanja", "Material Toko", "Penting")

        ModalBottomSheet(
            onDismissRequest = { showLabelSheet = false },
            sheetState = sheetState,
            containerColor = colors.surface,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Pilih Label Catatan",
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )

                // Preset Chips
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    presetLabels.forEach { label ->
                        val isSelected = label in uiState.labelIds
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) colors.primary50 else colors.surfaceMuted)
                                    .border(
                                        1.dp,
                                        if (isSelected) colors.primary else colors.border,
                                        RoundedCornerShape(16.dp),
                                    ).clickable {
                                        if (isSelected) {
                                            viewModel.onRemoveLabel(label)
                                        } else {
                                            viewModel.onAddLabel(label)
                                        }
                                    }.padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                            Text(
                                text = label,
                                color = if (isSelected) colors.primary else colors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                    }
                }

                // Input Buat Label Baru
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.surfaceMuted)
                            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BasicTextField(
                        value = newLabelInput,
                        onValueChange = { newLabelInput = it },
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(color = colors.textPrimary, fontSize = 14.sp),
                        cursorBrush = SolidColor(colors.primary),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions =
                            KeyboardActions(
                                onDone = {
                                    if (newLabelInput.isNotBlank()) {
                                        viewModel.onAddLabel(newLabelInput.trim())
                                        newLabelInput = ""
                                    }
                                },
                            ),
                        decorationBox = { inner ->
                            if (newLabelInput.isEmpty()) {
                                Text(
                                    text = "Buat label kustom...",
                                    color = colors.textTertiary,
                                    fontSize = 14.sp,
                                )
                            }
                            inner()
                        },
                    )
                    Box(
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (newLabelInput.isNotBlank()) colors.primary else colors.border)
                                .clickable(enabled = newLabelInput.isNotBlank()) {
                                    viewModel.onAddLabel(newLabelInput.trim())
                                    newLabelInput = ""
                                }.padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = "Tambah",
                            color = if (newLabelInput.isNotBlank()) colors.textOnAccent else colors.textDisabled,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun EditorTopAppBar(
    saveStatus: EditorSaveStatus,
    isPinned: Boolean,
    onBackClick: () -> Unit,
    onTogglePin: () -> Unit,
    onMenuClick: () -> Unit,
    colors: com.example.spacebridgeapp.ui.theme.SpaceBridgeColorScheme,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = colors.textPrimary,
                )
            }
            Text(
                text = "Editor",
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Indikator status penyimpanan dokumen (F-NOTE-14)
            SaveStatusBadge(saveStatus = saveStatus, colors = colors)

            // Tombol Pin
            IconButton(
                onClick = onTogglePin,
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isPinned) colors.primary50 else colors.surface),
            ) {
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = if (isPinned) "Lepas sematan" else "Sematkan",
                    tint = if (isPinned) colors.primary else colors.textSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }

            // Tombol Menu Lainnya
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Pilihan lainnya",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun SaveStatusBadge(
    saveStatus: EditorSaveStatus,
    colors: com.example.spacebridgeapp.ui.theme.SpaceBridgeColorScheme,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier =
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceMuted)
                .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        when (saveStatus) {
            EditorSaveStatus.SAVING -> {
                CircularProgressIndicator(
                    strokeWidth = 1.5.dp,
                    color = colors.primary,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = "Menyimpan...",
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            EditorSaveStatus.SAVED_CLOUD -> {
                Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = null,
                    tint = colors.success600,
                    modifier = Modifier.size(13.dp),
                )
                Text(
                    text = "Tersimpan",
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            EditorSaveStatus.SAVED_LOCAL -> {
                Icon(
                    imageVector = Icons.Default.CloudQueue,
                    contentDescription = null,
                    tint = colors.warning500,
                    modifier = Modifier.size(13.dp),
                )
                Text(
                    text = "Di perangkat",
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun EditorBottomBar(
    type: NoteType,
    wordCount: Int,
    charCount: Int,
    attachmentCount: Int,
    onConvertType: () -> Unit,
    onDriveClick: () -> Unit,
    onCameraClick: () -> Unit,
    colors: com.example.spacebridgeapp.ui.theme.SpaceBridgeColorScheme,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .border(1.dp, colors.border)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Tombol Lampiran Drive
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surfaceMuted)
                        .clickable(onClick = onDriveClick)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.AddToDrive,
                    contentDescription = "Lampirkan dari Google Drive",
                    tint = colors.primary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "Drive",
                    color = colors.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
                if (attachmentCount > 0) {
                    Box(
                        modifier =
                            Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(colors.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = attachmentCount.toString(),
                            color = colors.textOnAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            // Tombol Kamera
            IconButton(
                onClick = onCameraClick,
                modifier =
                    Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surfaceMuted),
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = "Kamera atau Foto",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Tombol Konversi Jenis Catatan
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.primary50)
                        .clickable(onClick = onConvertType)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Icon(
                    imageVector = if (type == NoteType.TEXT) Icons.Default.Checklist else Icons.Default.Description,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = if (type == NoteType.TEXT) "Jadikan Checklist" else "Jadikan Teks",
                    color = colors.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            // Penghitung Kata & Karakter
            if (type == NoteType.TEXT) {
                Text(
                    text = "$wordCount kata • $charCount kar",
                    color = colors.textTertiary,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun DriveAttachmentPreview(
    attachmentIds: List<String>,
    colors: com.example.spacebridgeapp.ui.theme.SpaceBridgeColorScheme,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        attachmentIds.forEach { fileId ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceMuted)
                        .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Berkas_Tersimpan_${fileId.take(8)}.pdf",
                        color = colors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "Google Drive",
                            color = colors.primary,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }
    }
}

private fun formatMetadataTimestamp(instant: Instant?): String {
    if (instant == null) return "Disunting baru saja"
    val zoneId = ZoneId.systemDefault()
    val noteDate = instant.atZone(zoneId).toLocalDate()
    val today = LocalDate.now(zoneId)
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("id-ID"))
    val timeStr = instant.atZone(zoneId).format(timeFormatter)

    return when {
        noteDate == today -> "Disunting hari ini, $timeStr"
        noteDate == today.minusDays(1) -> "Disunting kemarin, $timeStr"
        else -> {
            val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.forLanguageTag("id-ID"))
            "Disunting ${instant.atZone(zoneId).format(dateFormatter)}"
        }
    }
}
