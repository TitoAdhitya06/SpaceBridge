package com.example.spacebridgeapp.feature.notes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.spacebridgeapp.feature.notes.components.ModernNoteCard
import com.example.spacebridgeapp.ui.theme.SpaceBridgeTheme

@Composable
fun NotesScreen(
    viewModel: NotesViewModel,
    onNoteClick: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTrash: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val colors = SpaceBridgeTheme.colors

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is NotesEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is NotesEvent.ShowUndoSnackbar -> {
                    kotlinx.coroutines.withTimeoutOrNull(5000L) {
                        val result =
                            snackbarHostState.showSnackbar(
                                message = event.message,
                                actionLabel = "Urungkan",
                                duration = SnackbarDuration.Indefinite,
                            )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.onRestoreNote(event.noteId)
                        }
                    }
                    snackbarHostState.currentSnackbarData?.dismiss()
                }
                is NotesEvent.NavigateToEditor -> {
                    onNoteClick(event.noteId ?: "")
                }
            }
        }
    }

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isWideScreen = screenWidthDp >= 600

    val gridColumns =
        remember(screenWidthDp) {
            when {
                screenWidthDp >= 1200 -> StaggeredGridCells.Fixed(5)
                screenWidthDp >= 840 -> StaggeredGridCells.Fixed(4)
                screenWidthDp >= 600 -> StaggeredGridCells.Fixed(3)
                else -> StaggeredGridCells.Fixed(2)
            }
        }

    val horizontalPadding = if (screenWidthDp >= 840) 24.dp else 16.dp

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                color = colors.background,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    AnimatedVisibility(
                        visible = !uiState.isOnline,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        OfflineBanner()
                    }

                    NotesTopAppBar(
                        isOnline = uiState.isOnline,
                        isGridLayout = uiState.isGridLayout,
                        userInitials =
                            uiState.user
                                ?.displayName
                                ?.take(2)
                                ?.uppercase()
                                ?: uiState.user
                                    ?.email
                                    ?.take(2)
                                    ?.uppercase() ?: "SB",
                        onToggleLayout = viewModel::onToggleLayout,
                        onTrashClick = onNavigateToTrash,
                        onAvatarClick = onNavigateToSettings,
                    )
                }
            }
        },
    ) { paddingValues ->
        if (uiState.isGridLayout) {
            LazyVerticalStaggeredGrid(
                columns = gridColumns,
                verticalItemSpacing = 10.dp,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding =
                    PaddingValues(
                        start = horizontalPadding,
                        end = horizontalPadding,
                        top = 4.dp,
                        bottom = 24.dp,
                    ),
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                overscrollEffect = null,
            ) {
                item(key = "notes_header_controls", span = StaggeredGridItemSpan.FullLine) {
                    NotesHeaderControls(
                        uiState = uiState,
                        onQueryChange = viewModel::onSearchQueryChange,
                        onFilterSelected = viewModel::onFilterSelected,
                        isWideScreen = isWideScreen,
                    )
                }

                when {
                    uiState.isLoading && uiState.pinnedNotes.isEmpty() && uiState.otherNotes.isEmpty() -> {
                        item(key = "loading_indicator", span = StaggeredGridItemSpan.FullLine) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    color = colors.primary,
                                    modifier = Modifier.size(36.dp),
                                    strokeWidth = 3.dp,
                                )
                            }
                        }
                    }

                    uiState.pinnedNotes.isEmpty() && uiState.otherNotes.isEmpty() -> {
                        item(key = "empty_notes_view", span = StaggeredGridItemSpan.FullLine) {
                            EmptyNotesView(
                                isSearching = uiState.searchQuery.isNotBlank(),
                                searchQuery = uiState.searchQuery,
                            )
                        }
                    }

                    else -> {
                        if (uiState.pinnedNotes.isNotEmpty()) {
                            item(key = "pinned_section_header", span = StaggeredGridItemSpan.FullLine) {
                                SectionHeader(title = "Disematkan", hasPin = true)
                            }
                            items(uiState.pinnedNotes, key = { it.id }) { note ->
                                ModernNoteCard(
                                    note = note,
                                    onClick = { onNoteClick(note.id) },
                                    onPinClick = { viewModel.onTogglePin(note.id, note.pinned) },
                                    onDeleteClick = { viewModel.onDeleteNote(note.id) },
                                )
                            }
                        }

                        if (uiState.otherNotes.isNotEmpty()) {
                            if (uiState.pinnedNotes.isNotEmpty()) {
                                item(key = "other_section_header", span = StaggeredGridItemSpan.FullLine) {
                                    SectionHeader(title = "Lainnya", hasPin = false)
                                }
                            }
                            items(uiState.otherNotes, key = { it.id }) { note ->
                                ModernNoteCard(
                                    note = note,
                                    onClick = { onNoteClick(note.id) },
                                    onPinClick = { viewModel.onTogglePin(note.id, note.pinned) },
                                    onDeleteClick = { viewModel.onDeleteNote(note.id) },
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                contentAlignment = Alignment.TopCenter,
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding =
                        PaddingValues(
                            start = horizontalPadding,
                            end = horizontalPadding,
                            top = 4.dp,
                            bottom = 24.dp,
                        ),
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .widthIn(max = 760.dp),
                    overscrollEffect = null,
                ) {
                    item(key = "notes_header_controls") {
                        NotesHeaderControls(
                            uiState = uiState,
                            onQueryChange = viewModel::onSearchQueryChange,
                            onFilterSelected = viewModel::onFilterSelected,
                            isWideScreen = isWideScreen,
                        )
                    }

                    when {
                        uiState.isLoading && uiState.pinnedNotes.isEmpty() && uiState.otherNotes.isEmpty() -> {
                            item(key = "loading_indicator") {
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 48.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(
                                        color = colors.primary,
                                        modifier = Modifier.size(36.dp),
                                        strokeWidth = 3.dp,
                                    )
                                }
                            }
                        }

                        uiState.pinnedNotes.isEmpty() && uiState.otherNotes.isEmpty() -> {
                            item(key = "empty_notes_view") {
                                EmptyNotesView(
                                    isSearching = uiState.searchQuery.isNotBlank(),
                                    searchQuery = uiState.searchQuery,
                                )
                            }
                        }

                        else -> {
                            if (uiState.pinnedNotes.isNotEmpty()) {
                                item(key = "pinned_section_header") {
                                    SectionHeader(title = "Disematkan", hasPin = true)
                                }
                                items(uiState.pinnedNotes, key = { it.id }) { note ->
                                    ModernNoteCard(
                                        note = note,
                                        onClick = { onNoteClick(note.id) },
                                        onPinClick = { viewModel.onTogglePin(note.id, note.pinned) },
                                        onDeleteClick = { viewModel.onDeleteNote(note.id) },
                                    )
                                }
                            }

                            if (uiState.otherNotes.isNotEmpty()) {
                                if (uiState.pinnedNotes.isNotEmpty()) {
                                    item(key = "other_section_header") {
                                        SectionHeader(title = "Lainnya", hasPin = false)
                                    }
                                }
                                items(uiState.otherNotes, key = { it.id }) { note ->
                                    ModernNoteCard(
                                        note = note,
                                        onClick = { onNoteClick(note.id) },
                                        onPinClick = { viewModel.onTogglePin(note.id, note.pinned) },
                                        onDeleteClick = { viewModel.onDeleteNote(note.id) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotesHeaderControls(
    uiState: NotesUiState,
    onQueryChange: (String) -> Unit,
    onFilterSelected: (NoteFilter) -> Unit,
    isWideScreen: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        if (isWideScreen) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = onQueryChange,
                    modifier = Modifier.weight(1.3f),
                )
                SpaceSummaryCard(
                    totalActive = uiState.totalActiveNotes,
                    completedChecklists = uiState.completedChecklistItems,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = onQueryChange,
                modifier = Modifier.padding(vertical = 4.dp),
            )
            SpaceSummaryCard(
                totalActive = uiState.totalActiveNotes,
                completedChecklists = uiState.completedChecklistItems,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }

        FilterChipsRow(
            filters = uiState.availableFilters,
            selectedFilter = uiState.selectedFilter,
            onFilterSelected = onFilterSelected,
            modifier = Modifier.padding(vertical = 6.dp),
        )

        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun NotesTopAppBar(
    isOnline: Boolean,
    isGridLayout: Boolean,
    userInitials: String,
    onToggleLayout: () -> Unit,
    onTrashClick: () -> Unit,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SpaceBridgeTheme.colors
    val configuration = LocalConfiguration.current
    val verticalPadding = if (configuration.screenHeightDp < 500) 4.dp else 8.dp

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = verticalPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "Catatan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = colors.textPrimary,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isOnline) colors.success500 else colors.textDisabled),
                )
                Text(
                    text = if (isOnline) "Tersinkron ke Cloud" else "Mode Offline",
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Tombol Toggle Tampilan (Grid / List)
            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                        .clickable(onClick = onToggleLayout),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isGridLayout) Icons.Default.ViewAgenda else Icons.Default.GridView,
                    contentDescription = if (isGridLayout) "Ubah ke Tampilan List" else "Ubah ke Tampilan Grid",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }

            // Tombol Sampah
            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                        .clickable(onClick = onTrashClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Buka Sampah",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }

            // Avatar Pengguna
            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(colors.surface)
                        .border(1.5.dp, colors.borderStrong, CircleShape)
                        .clickable(onClick = onAvatarClick)
                        .padding(2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(colors.primary100),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = userInitials,
                        color = colors.primary700,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun OfflineBanner() {
    val colors = SpaceBridgeTheme.colors

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(colors.warning50)
                .border(BorderStroke(1.dp, colors.warning500))
                .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = colors.warning700,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = "Sedang offline. Perubahan tersimpan di perangkat dan akan disinkronkan saat terhubung.",
            color = colors.warning700,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SpaceBridgeTheme.colors

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.surface)
                .border(1.dp, colors.borderStrong, RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Cari",
                tint = colors.textTertiary,
                modifier = Modifier.size(18.dp),
            )

            Spacer(Modifier.width(10.dp))

            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = "Cari judul, isi, atau label...",
                        color = colors.textTertiary,
                        fontSize = 14.sp,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle =
                        TextStyle(
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                        ),
                    cursorBrush = SolidColor(colors.primary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Hapus pencarian",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChipsRow(
    filters: List<NoteFilter>,
    selectedFilter: NoteFilter,
    onFilterSelected: (NoteFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SpaceBridgeTheme.colors

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        filters.forEach { filter ->
            val isSelected = filter == selectedFilter
            val bg = if (isSelected) colors.primary50 else colors.surface
            val borderColor = if (isSelected) colors.primary500 else colors.border
            val textColor = if (isSelected) colors.primary700 else colors.textSecondary
            val fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium

            Box(
                modifier =
                    Modifier
                        .height(34.dp)
                        .clip(CircleShape)
                        .background(bg)
                        .border(1.dp, borderColor, CircleShape)
                        .clickable { onFilterSelected(filter) }
                        .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = filter.displayName,
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = fontWeight,
                )
            }
        }
    }
}

@Composable
private fun SpaceSummaryCard(
    totalActive: Int,
    completedChecklists: Int,
    modifier: Modifier = Modifier,
) {
    val colors = SpaceBridgeTheme.colors

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(colors.surfaceMuted)
                .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = colors.primary500,
                modifier = Modifier.size(15.dp),
            )
            Text(
                text = "$totalActive catatan aktif • $completedChecklists checklist selesai",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textSecondary,
            )
        }

        Box(
            modifier =
                Modifier
                    .background(colors.info50, RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Text(
                text = "Drive OK",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.info600,
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    hasPin: Boolean,
) {
    val colors = SpaceBridgeTheme.colors

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    ) {
        if (hasPin) {
            Icon(
                imageVector = Icons.Default.PushPin,
                contentDescription = null,
                tint = colors.primary500,
                modifier = Modifier.size(13.dp),
            )
        }
        Text(
            text = title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textTertiary,
            letterSpacing = 0.5.sp,
        )
    }
}

@Composable
private fun EmptyNotesView(
    isSearching: Boolean,
    searchQuery: String,
) {
    val colors = SpaceBridgeTheme.colors

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(
                imageVector = if (isSearching) Icons.Default.Search else Icons.Default.Description,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = colors.borderStrong,
            )
            Text(
                text = if (isSearching) "Catatan tidak ditemukan" else "Belum ada catatan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text =
                    if (isSearching) {
                        "Tidak ditemukan catatan dengan kata kunci \"$searchQuery\"."
                    } else {
                        "Ketuk tombol tambah untuk menulis ide atau daftar belanja pertama Anda."
                    },
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
            )
        }
    }
}
