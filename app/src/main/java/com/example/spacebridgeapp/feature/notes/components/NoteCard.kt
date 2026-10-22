package com.example.spacebridgeapp.feature.notes.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spacebridgeapp.core.model.ChecklistItem
import com.example.spacebridgeapp.core.model.Note
import com.example.spacebridgeapp.core.model.NoteType
import com.example.spacebridgeapp.core.util.formatRelativeDateTime
import com.example.spacebridgeapp.ui.theme.SpaceBridgeTheme

@Composable
fun ModernNoteCard(
    note: Note,
    onClick: () -> Unit,
    onPinClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDeleteClick: (() -> Unit)? = null,
) {
    val colors = SpaceBridgeTheme.colors

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Header Meta: Kategori Tag & Pin Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val isChecklist = note.type == NoteType.CHECKLIST
                val tagBg = if (isChecklist) colors.primary50 else colors.surfaceMuted
                val tagTextColor = if (isChecklist) colors.primary700 else colors.textSecondary
                val tagLabel = if (isChecklist) "Checklist" else "Teks"

                Box(
                    modifier =
                        Modifier
                            .background(tagBg, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = tagLabel,
                        color = tagTextColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (onDeleteClick != null) {
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Pindahkan ke Sampah",
                                tint = colors.textTertiary,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }

                    IconButton(
                        onClick = onPinClick,
                        modifier = Modifier.size(24.dp),
                    ) {
                        if (note.pinned) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Lepas sematan",
                                tint = colors.primary600,
                                modifier = Modifier.size(15.dp),
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.PushPin,
                                contentDescription = "Sematkan catatan",
                                tint = colors.textTertiary,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                }
            }

            // Judul Catatan
            val displayTitle =
                note.title.ifBlank {
                    if (note.type == NoteType.CHECKLIST) "Daftar Checklist" else "Catatan Tanpa Judul"
                }

            Text(
                text = displayTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 19.sp,
            )

            // Konten: Mode Teks atau Mode Checklist
            if (note.type == NoteType.CHECKLIST) {
                val previewItems = note.items.take(3)
                if (previewItems.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        previewItems.forEach { item ->
                            MiniChecklistItemRow(item = item)
                        }
                    }
                }

                val completedCount = note.items.count { it.checked }
                val totalItems = note.items.size
                if (totalItems > 0) {
                    Box(
                        modifier =
                            Modifier
                                .background(colors.primary50, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "$completedCount/$totalItems selesai",
                            color = colors.primary700,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            } else {
                if (note.body.isNotBlank()) {
                    Text(
                        text = note.body,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 17.sp,
                    )
                }
            }

            Spacer(Modifier.height(2.dp))

            // Footer: Waktu & Status Sinkronisasi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatRelativeDateTime(note.updatedAt),
                    color = colors.textTertiary,
                    fontSize = 11.sp,
                )

                if (note.hasPendingWrites) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = "Tersimpan di perangkat",
                            tint = colors.textTertiary,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = "Lokal",
                            color = colors.textTertiary,
                            fontSize = 10.sp,
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = "Tersinkron ke Cloud",
                        tint = colors.success600,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniChecklistItemRow(
    item: ChecklistItem,
    modifier: Modifier = Modifier,
) {
    val colors = SpaceBridgeTheme.colors

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Kotak centang mini
        Box(
            modifier =
                Modifier
                    .size(13.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (item.checked) colors.primary600 else colors.surface)
                    .then(
                        if (!item.checked) {
                            Modifier.background(colors.surface)
                        } else {
                            Modifier
                        },
                    ),
            contentAlignment = Alignment.Center,
        ) {
            if (item.checked) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = colors.textOnAccent,
                    modifier = Modifier.size(9.dp),
                )
            }
        }

        Spacer(Modifier.width(6.dp))

        Text(
            text = item.text.ifBlank { "Tugas tanpa teks" },
            color = if (item.checked) colors.textTertiary else colors.textPrimary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
            lineHeight = 15.sp,
        )
    }
}
