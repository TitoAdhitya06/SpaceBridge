package com.example.spacebridgeapp.feature.editor.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spacebridgeapp.core.model.ChecklistItem
import com.example.spacebridgeapp.ui.theme.SpaceBridgeTheme

@Composable
fun ChecklistSection(
    items: List<ChecklistItem>,
    focusedItemId: String?,
    onItemTextChange: (itemId: String, text: String) -> Unit,
    onToggleItem: (itemId: String) -> Unit,
    onDeleteItem: (itemId: String) -> Unit,
    onAddItem: (text: String, afterIndex: Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SpaceBridgeTheme.colors
    val completedItems = remember(items) { items.filter { it.checked } }
    val activeItems = remember(items) { items.filter { !it.checked } }
    val totalCount = items.size
    val completedCount = completedItems.size
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f
    val percentage = (progress * 100).toInt()

    var isCompletedExpanded by remember { mutableStateOf(true) }
    var quickAddText by remember { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Kartu progres penyelesaian checklist
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceMuted)
                    .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                    .padding(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.FactCheck,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "$completedCount dari $totalCount selesai",
                            color = colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text(
                        text = "$percentage%",
                        color = colors.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                // Progress Bar Solid
                LinearProgressIndicator(
                    progress = { progress },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                    color = colors.primary,
                    trackColor = colors.border,
                )
            }
        }

        // Bagian Item Aktif
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "TUGAS AKTIF",
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                )
                Text(
                    text = "${activeItems.size} item tersisa",
                    color = colors.textTertiary,
                    fontSize = 12.sp,
                )
            }

            activeItems.forEachIndexed { index, item ->
                ActiveChecklistItemRow(
                    item = item,
                    isFocused = item.id == focusedItemId,
                    onTextChange = { newText -> onItemTextChange(item.id, newText) },
                    onToggle = { onToggleItem(item.id) },
                    onDelete = { onDeleteItem(item.id) },
                    onEnterPressed = { onAddItem("", index) },
                    onBackspaceOnEmpty = {
                        onDeleteItem(item.id)
                    },
                )
            }

            // Baris Tambah Tugas Cepat
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceMuted)
                        .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(20.dp),
                )
                BasicTextField(
                    value = quickAddText,
                    onValueChange = { quickAddText = it },
                    modifier = Modifier.weight(1f),
                    textStyle =
                        TextStyle(
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                        ),
                    cursorBrush = SolidColor(colors.primary),
                    singleLine = true,
                    keyboardOptions =
                        KeyboardOptions(
                            imeAction = ImeAction.Done,
                            capitalization = KeyboardCapitalization.Sentences,
                        ),
                    keyboardActions =
                        KeyboardActions(
                            onDone = {
                                if (quickAddText.isNotBlank()) {
                                    onAddItem(quickAddText.trim(), null)
                                    quickAddText = ""
                                }
                            },
                        ),
                    decorationBox = { innerTextField ->
                        if (quickAddText.isEmpty()) {
                            Text(
                                text = "Tulis tugas baru...",
                                color = colors.textTertiary,
                                fontSize = 14.sp,
                            )
                        }
                        innerTextField()
                    },
                )
                Box(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (quickAddText.isNotBlank()) colors.primary else colors.border)
                            .clickable(enabled = quickAddText.isNotBlank()) {
                                onAddItem(quickAddText.trim(), null)
                                quickAddText = ""
                            }.padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = "Tambah",
                        color = if (quickAddText.isNotBlank()) colors.textOnAccent else colors.textDisabled,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        // Bagian Item Selesai (Dapat Dilipat)
        if (completedItems.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isCompletedExpanded = !isCompletedExpanded }
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = if (isCompletedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Selesai (${completedItems.size} item)",
                            color = colors.textSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text(
                        text = if (isCompletedExpanded) "Sembunyikan" else "Tampilkan",
                        color = colors.textTertiary,
                        fontSize = 12.sp,
                    )
                }

                AnimatedVisibility(
                    visible = isCompletedExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        completedItems.forEach { item ->
                            CompletedChecklistItemRow(
                                item = item,
                                onToggle = { onToggleItem(item.id) },
                                onDelete = { onDeleteItem(item.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveChecklistItemRow(
    item: ChecklistItem,
    isFocused: Boolean,
    onTextChange: (String) -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEnterPressed: () -> Unit,
    onBackspaceOnEmpty: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SpaceBridgeTheme.colors
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            focusRequester.requestFocus()
        }
    }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Kotak Centang Kustom
        Box(
            modifier =
                Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .border(1.5.dp, colors.borderStrong, RoundedCornerShape(4.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggle,
                    ),
            contentAlignment = Alignment.Center,
        ) {}

        // Input Teks Tugas
        BasicTextField(
            value = item.text,
            onValueChange = onTextChange,
            modifier =
                Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            if (keyEvent.key == Key.Enter) {
                                onEnterPressed()
                                true
                            } else if (keyEvent.key == Key.Backspace && item.text.isEmpty()) {
                                onBackspaceOnEmpty()
                                true
                            } else {
                                false
                            }
                        } else {
                            false
                        }
                    },
            textStyle =
                TextStyle(
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                ),
            cursorBrush = SolidColor(colors.primary),
            singleLine = true,
            keyboardOptions =
                KeyboardOptions(
                    imeAction = ImeAction.Next,
                    capitalization = KeyboardCapitalization.Sentences,
                ),
            keyboardActions =
                KeyboardActions(
                    onNext = { onEnterPressed() },
                ),
            decorationBox = { innerTextField ->
                if (item.text.isEmpty()) {
                    Text(
                        text = "Ketik item...",
                        color = colors.textTertiary,
                        fontSize = 14.sp,
                    )
                }
                innerTextField()
            },
        )

        // Tombol Hapus Baris
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Hapus item",
                tint = colors.textTertiary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun CompletedChecklistItemRow(
    item: ChecklistItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SpaceBridgeTheme.colors

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceMuted)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Kotak Centang Aktif
        Box(
            modifier =
                Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.primary)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggle,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = colors.textOnAccent,
                modifier = Modifier.size(14.dp),
            )
        }

        // Teks Coret
        Text(
            text = item.text,
            modifier = Modifier.weight(1f),
            color = colors.textSecondary,
            fontSize = 14.sp,
            textDecoration = TextDecoration.LineThrough,
        )

        // Tombol Hapus Baris
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Hapus item",
                tint = colors.textTertiary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
