package com.example.spacebridgeapp.feature.files.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spacebridgeapp.core.model.DriveFile
import com.example.spacebridgeapp.core.model.FileCategory
import com.example.spacebridgeapp.core.model.FileStatus
import com.example.spacebridgeapp.core.util.formatFileSize
import com.example.spacebridgeapp.core.util.formatRelativeDateTime
import com.example.spacebridgeapp.core.util.toDisplayName
import com.example.spacebridgeapp.ui.theme.SpaceBridgeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileActionsSheet(
    file: DriveFile,
    onDismiss: () -> Unit,
    onPreview: () -> Unit,
    onOpenInDrive: () -> Unit,
    onDownload: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = SpaceBridgeTheme.colors

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        dragHandle = {
            Box(
                modifier =
                    Modifier
                        .padding(top = 10.dp, bottom = 6.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .background(colors.borderStrong, RoundedCornerShape(2.dp)),
            )
        },
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // Sheet Header with Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Tindakan Berkas",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = colors.textSecondary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // File Snapshot Card
            FileSnapshotCard(file = file)

            Spacer(modifier = Modifier.height(16.dp))

            // Main Actions Group
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surfaceMuted,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    FileActionItem(
                        icon = Icons.Default.Visibility,
                        title = "Buka Berkas",
                        subtitle = "Buka salinan cepat di penampil dokumen Android",
                        onClick = onPreview,
                    )

                    HorizontalDivider(color = colors.border.copy(alpha = 0.5f))

                    FileActionItem(
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        iconTint = colors.primary,
                        title = "Buka di Google Drive",
                        subtitle = "Buka berkas langsung di aplikasi Drive atau browser",
                        enabled = !file.webViewLink.isNullOrBlank(),
                        onClick = onOpenInDrive,
                    )

                    HorizontalDivider(color = colors.border.copy(alpha = 0.5f))

                    FileActionItem(
                        icon = Icons.Default.FileDownload,
                        title = "Simpan Salinan ke Download",
                        subtitle = "Unduh berkas ke folder Download internal perangkat",
                        onClick = onDownload,
                    )

                    HorizontalDivider(color = colors.border.copy(alpha = 0.5f))

                    FileActionItem(
                        icon = Icons.Default.Edit,
                        title = "Ganti Nama",
                        subtitle = "Ubah nama berkas di Drive dan database",
                        onClick = onRename,
                    )

                    HorizontalDivider(color = colors.border.copy(alpha = 0.5f))

                    FileActionItem(
                        icon = Icons.Default.Link,
                        title = "Bagikan Tautan",
                        subtitle = "Buat tautan berbagi publik siapa saja yang memiliki tautan",
                        onClick = onShare,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Destructive Action: Move to Trash
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.danger50,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onDelete),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(36.dp)
                                .background(colors.danger.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = colors.danger,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pindahkan ke Sampah",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.danger,
                        )
                        Text(
                            text = "Berkas akan dipindahkan ke folder Sampah",
                            fontSize = 12.sp,
                            color = colors.danger.copy(alpha = 0.8f),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FileSnapshotCard(
    file: DriveFile,
    modifier: Modifier = Modifier,
) {
    val colors = SpaceBridgeTheme.colors
    val iconMeta = getFileVisualMeta(file.category, file.mimeType)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.surfaceMuted,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .background(iconMeta.backgroundColor, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = iconMeta.icon,
                    contentDescription = null,
                    tint = iconMeta.iconColor,
                    modifier = Modifier.size(26.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(3.dp))

                val sizeText = formatFileSize(file.sizeBytes)
                val dateText = formatRelativeDateTime(file.createdAt)
                val categoryText = file.category.toDisplayName()
                Text(
                    text = "$sizeText • $dateText • $categoryText",
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Status Badge
                StatusPill(status = file.status)
            }
        }
    }
}

@Composable
private fun StatusPill(status: FileStatus) {
    val colors = SpaceBridgeTheme.colors
    val (bgColor, dotColor, label) =
        when (status) {
            FileStatus.READY -> Triple(colors.success50, colors.success, "Tersimpan di Google Drive")
            FileStatus.UPLOADING -> Triple(colors.info50, colors.info, "Sedang Mengunggah")
            FileStatus.FAILED -> Triple(colors.danger50, colors.danger, "Gagal Diunggah")
            FileStatus.MISSING -> Triple(colors.warning50, colors.warning, "Berkas Tidak Ditemukan")
            FileStatus.TRASHED -> Triple(colors.danger50, colors.danger, "Di Sampah")
        }

    Row(
        modifier =
            Modifier
                .background(bgColor, RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(6.dp)
                    .background(dotColor, CircleShape),
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = dotColor,
        )
    }
}

@Composable
private fun FileActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = SpaceBridgeTheme.colors.textPrimary,
    enabled: Boolean = true,
) {
    val colors = SpaceBridgeTheme.colors
    val alpha = if (enabled) 1f else 0.4f

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .background(colors.surface, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint.copy(alpha = alpha),
                modifier = Modifier.size(20.dp),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary.copy(alpha = alpha),
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = colors.textSecondary.copy(alpha = alpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = colors.borderStrong.copy(alpha = alpha),
            modifier = Modifier.size(18.dp),
        )
    }
}

data class FileVisualMeta(
    val icon: ImageVector,
    val iconColor: Color,
    val backgroundColor: Color,
)

fun getFileVisualMeta(
    category: FileCategory,
    mimeType: String,
): FileVisualMeta =
    when (category) {
        FileCategory.DOCUMENT -> {
            if (mimeType.contains("pdf", ignoreCase = true)) {
                FileVisualMeta(
                    icon = Icons.Default.PictureAsPdf,
                    iconColor = Color(0xFFC23B32),
                    backgroundColor = Color(0xFFFBEAE8),
                )
            } else {
                FileVisualMeta(
                    icon = Icons.Default.Description,
                    iconColor = Color(0xFF2F6FB0),
                    backgroundColor = Color(0xFFE8F1FA),
                )
            }
        }
        FileCategory.SPREADSHEET -> {
            FileVisualMeta(
                icon = Icons.Default.TableChart,
                iconColor = Color(0xFF27774A),
                backgroundColor = Color(0xFFE6F4EC),
            )
        }
        FileCategory.IMAGE -> {
            FileVisualMeta(
                icon = Icons.Default.Image,
                iconColor = Color(0xFFD97706),
                backgroundColor = Color(0xFFFEF3EB),
            )
        }
        FileCategory.OTHER -> {
            FileVisualMeta(
                icon = Icons.AutoMirrored.Filled.InsertDriveFile,
                iconColor = Color(0xFF6B7280),
                backgroundColor = Color(0xFFF0EDE6),
            )
        }
    }
