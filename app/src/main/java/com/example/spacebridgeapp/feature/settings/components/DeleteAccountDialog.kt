package com.example.spacebridgeapp.feature.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spacebridgeapp.ui.theme.SpaceBridgeTheme

@Composable
fun DeleteAccountDialog(
    isDeleting: Boolean,
    errorMessage: String? = null,
    onConfirmDelete: (trashDriveFolder: Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var trashDriveFolder by remember { mutableStateOf(false) }
    val colors = SpaceBridgeTheme.colors

    AlertDialog(
        onDismissRequest = {
            if (!isDeleting) {
                onDismiss()
            }
        },
        modifier = modifier,
        containerColor = colors.surface,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.danger50),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = colors.danger600,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    text = "Hapus Akun dan Data",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Tindakan ini permanen. Seluruh profil, catatan, checklist, dan riwayat berkas Anda di Firestore akan dihapus secara permanen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    lineHeight = 20.sp,
                )

                // Opsi checkbox: Hapus juga folder SpaceBridge di Google Drive
                SurfaceCard(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isDeleting) {
                                trashDriveFolder = !trashDriveFolder
                            },
                    backgroundColor = colors.surfaceMuted,
                    borderColor = colors.border,
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Checkbox(
                            checked = trashDriveFolder,
                            onCheckedChange = { trashDriveFolder = it },
                            enabled = !isDeleting,
                            colors =
                                CheckboxDefaults.colors(
                                    checkedColor = colors.primary,
                                    uncheckedColor = colors.textSecondary,
                                ),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Hapus juga folder SpaceBridge di Google Drive",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Memindahkan folder /SpaceBridge ke kotak Sampah Google Drive Anda.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                            )
                        }
                    }
                }

                // Kotak informasi peringatan keluar otomatis
                SurfaceCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = colors.danger50,
                    borderColor = colors.danger.copy(alpha = 0.25f),
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = colors.danger600,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Anda akan langsung dikeluarkan dari aplikasi setelah penghapusan akun tuntas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.danger600,
                        )
                    }
                }

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.danger600,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmDelete(trashDriveFolder) },
                enabled = !isDeleting,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = colors.danger600,
                        contentColor = Color.White,
                        disabledContainerColor = colors.danger600.copy(alpha = 0.5f),
                        disabledContentColor = Color.White.copy(alpha = 0.7f),
                    ),
                shape = RoundedCornerShape(8.dp),
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Menghapus...",
                        fontWeight = FontWeight.Bold,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Hapus Akun Permanen",
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDeleting,
            ) {
                Text(
                    text = "Batal",
                    color = colors.textSecondary,
                )
            }
        },
    )
}

@Composable
private fun SurfaceCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    borderColor: Color,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(10.dp))
                .background(backgroundColor)
                .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(10.dp)),
    ) {
        content()
    }
}
