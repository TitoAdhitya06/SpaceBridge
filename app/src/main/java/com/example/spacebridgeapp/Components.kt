package com.example.spacebridgeapp

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.spacebridgeapp.ui.theme.SpaceBridgeTheme
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Top App Bar SpaceBridge bertema modern (adaptif Dark/Light Mode).
 */
@Composable
fun SpaceBridgeHeader(
    title: String,
    subtitle: String = "Catatan",
    statusText: String = "Terhubung ke Cloud",
    user: FirebaseUser?,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    val colors = SpaceBridgeTheme.colors
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "pulseAlpha",
    )

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(10.dp),
                color = colors.surfaceContainerHigh,
                border = BorderStroke(1.dp, colors.ghostBorder),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Hub,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = colors.cosmicCyan,
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "/ $subtitle",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier =
                            Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(colors.cosmicCyan.copy(alpha = alphaAnim)),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.cosmicCyan,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSettingsClick() },
                shape = RoundedCornerShape(12.dp),
                color = colors.surfaceContainerHigh,
                border = BorderStroke(1.dp, colors.ghostBorder),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Pengaturan",
                        modifier = Modifier.size(20.dp),
                        tint = colors.textSecondary,
                    )
                }
            }

            // Avatar Profil dengan ring gradien
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(colors.primaryIndigo, colors.cosmicCyan, colors.nebulaViolet),
                            ),
                        ).padding(2.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceContainer)
                        .clickable { onProfileClick() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = (user?.displayName?.take(1) ?: user?.email?.take(1) ?: "U").uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = colors.primaryLight,
                )
            }
        }
    }
}

/**
 * Floating Bottom Navigation Bar dengan Elevated FAB di tengah (adaptif Dark/Light Mode).
 */
@Composable
fun FloatingBottomNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onCenterFabClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SpaceBridgeTheme.colors

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(64.dp),
            shape = RoundedCornerShape(22.dp),
            color = colors.surfaceContainer.copy(alpha = 0.94f),
            border = BorderStroke(1.dp, colors.ghostBorder),
            shadowElevation = 12.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Tab Catatan
                Row(
                    modifier =
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onTabSelected(0) }
                            .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = "Catatan",
                        modifier = Modifier.size(20.dp),
                        tint = if (selectedTab == 0) colors.cosmicCyan else colors.textSecondary,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Catatan",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == 0) colors.cosmicCyan else colors.textSecondary,
                    )
                }

                Spacer(Modifier.width(56.dp)) // Ruang untuk elevated FAB

                // Tab File Drive
                Row(
                    modifier =
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onTabSelected(1) }
                            .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        Icons.Default.Cloud,
                        contentDescription = "File Drive",
                        modifier = Modifier.size(20.dp),
                        tint = if (selectedTab == 1) colors.cosmicCyan else colors.textSecondary,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "File Drive",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == 1) colors.cosmicCyan else colors.textSecondary,
                    )
                }
            }
        }

        // Elevated Center FAB
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(bottom = 12.dp)
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(colors.primaryIndigo, colors.nebulaViolet),
                        ),
                    ).clickable { onCenterFabClick() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Tambah",
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

/**
 * Ringkasan Ruang Telemetri (Catatan Aktif, Checklist Selesai, Status Sinkronisasi).
 */
@Composable
fun RingkasanRuangCard(
    totalCatatan: Int,
    checklistSelesai: Int,
    totalChecklist: Int,
) {
    val colors = SpaceBridgeTheme.colors

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
        border = BorderStroke(1.dp, colors.ghostBorder),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CloudSync,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = colors.cosmicCyan,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Ringkasan Ruang",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = colors.cosmicCyan.copy(alpha = 0.12f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(colors.cosmicCyan),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "Firestore Live",
                            color = colors.cosmicCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Kolom 1: Catatan Aktif
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = colors.surfaceContainerLow,
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            "Catatan Aktif",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "$totalCatatan",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryLight,
                        )
                    }
                }

                // Kolom 2: Checklist
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = colors.surfaceContainerLow,
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            "Checklist Selesai",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted,
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                "$checklistSelesai",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = colors.cosmicCyan,
                            )
                            Text(
                                "/$totalChecklist",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted,
                            )
                        }
                    }
                }

                // Kolom 3: Sinkronisasi
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = colors.surfaceContainerLow,
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            "Sinkronisasi",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Terkini",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.nebulaVioletLight,
                        )
                        Text(
                            "Baru saja",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Filter Chips horizontal untuk kategori catatan.
 */
@Composable
fun FilterChipsRow(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val colors = SpaceBridgeTheme.colors

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEachIndexed { index, label ->
            val isSelected = selectedIndex == index
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isSelected) colors.primaryContainer else colors.surfaceContainerHigh,
                border =
                    BorderStroke(
                        1.dp,
                        if (isSelected) colors.primaryIndigo else colors.ghostBorder,
                    ),
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onSelect(index) },
            ) {
                Text(
                    text = label,
                    color = if (isSelected) (if (colors.isDark) Color.White else colors.primaryIndigo) else colors.textSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }
    }
}

/**
 * Kartu Catatan Modern dengan Category badge, Status Cloud, dan Menu Tindakan.
 */
@Composable
fun ModernNoteCard(
    catatan: Map<String, Any>,
    onClick: () -> Unit,
    onDeleteRequest: () -> Unit,
) {
    val colors = SpaceBridgeTheme.colors
    val judul = catatan["judul"] as? String ?: "Tanpa Judul"
    val tipe = catatan["tipe"] as? String ?: "teks"
    val isi = catatan["isi"] as? String ?: ""
    val isPinned = catatan["pinned"] as? Boolean ?: false
    val rawItems = catatan["items"] as? List<*>

    var showMenu by remember { mutableStateOf(false) }

    val itemsList =
        remember(rawItems) {
            rawItems?.mapNotNull {
                when (it) {
                    is Map<*, *> -> {
                        val teks = it["teks"] as? String
                        val checked = it["checked"] as? Boolean ?: false
                        if (teks != null) teks to checked else null
                    }
                    is String -> it to false
                    else -> null
                }
            } ?: emptyList()
        }

    val checkedCount = itemsList.count { it.second }
    val waktu = catatan["waktu"] as? Timestamp
    val waktuTeks =
        remember(waktu) {
            waktu?.toDate()?.let {
                SimpleDateFormat("d MMM HH:mm", Locale.forLanguageTag("id")).format(it)
            } ?: "Baru saja"
        }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
        border = BorderStroke(1.dp, colors.ghostBorder),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header kartu: Badge kategori, cloud status & menu titik tiga
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    if (isPinned) {
                        Surface(
                            modifier = Modifier.size(22.dp),
                            shape = CircleShape,
                            color = colors.nebulaViolet.copy(alpha = 0.2f),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = "Penting",
                                    tint = colors.nebulaVioletLight,
                                    modifier = Modifier.size(13.dp),
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = colors.nebulaViolet.copy(alpha = 0.15f),
                    ) {
                        Text(
                            if (tipe == "list") "#Checklist" else "#Catatan",
                            color = colors.nebulaVioletLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = colors.cosmicCyan.copy(alpha = 0.12f),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Icon(
                                Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = colors.cosmicCyan,
                                modifier = Modifier.size(11.dp),
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                "Cloud",
                                color = colors.cosmicCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Menu Opsi",
                            tint = colors.textMuted,
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        containerColor = colors.surfaceContainerHigh,
                    ) {
                        DropdownMenuItem(
                            text = { Text("Buka / Edit", color = colors.textPrimary) },
                            leadingIcon = { Icon(Icons.Default.Edit, null, tint = colors.cosmicCyan) },
                            onClick = {
                                showMenu = false
                                onClick()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Hapus Catatan", color = colors.statusError) },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = colors.statusError) },
                            onClick = {
                                showMenu = false
                                onDeleteRequest()
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Judul Catatan
            Text(
                judul,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(6.dp))

            // Konten
            if (tipe == "list") {
                if (itemsList.isNotEmpty()) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surfaceContainerLow)
                                .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        itemsList.take(3).forEach { item ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier =
                                        Modifier
                                            .size(13.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(if (item.second) colors.cosmicCyan else colors.surfaceContainerHighest)
                                            .padding(1.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (item.second) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (colors.isDark) Color.Black else Color.White,
                                            modifier = Modifier.size(10.dp),
                                        )
                                    }
                                }
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    item.first,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (item.second) colors.textMuted else colors.textPrimary,
                                    textDecoration = if (item.second) TextDecoration.LineThrough else null,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "$checkedCount/${itemsList.size} selesai",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.cosmicCyan,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            } else {
                if (isi.isNotBlank()) {
                    Text(
                        isi,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = colors.textMuted,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        waktuTeks,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted,
                    )
                }
                if (isPinned) {
                    Text(
                        "Disematkan",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.nebulaVioletLight,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

/**
 * Dialog Konfirmasi Hapus Catatan dengan opsi hapus lampiran terkait di Google Drive.
 */
@Composable
fun DialogKonfirmasiHapusCatatan(
    catatan: Map<String, Any>,
    onConfirmDelete: (hapusLampiranDrive: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = SpaceBridgeTheme.colors
    val judul = catatan["judul"] as? String ?: "Catatan"
    val tipe = catatan["tipe"] as? String ?: "teks"
    val rawItems = catatan["items"] as? List<*>
    val totalItems = rawItems?.size ?: 0
    val selesaiItems = rawItems?.count { (it as? Map<*, *>)?.get("checked") == true } ?: 0

    var hapusLampiranDrive by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = colors.surfaceContainerLow,
            border = BorderStroke(1.dp, colors.ghostBorder),
            shadowElevation = 24.dp,
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Glow badge peringatan
                Box(
                    modifier = Modifier.size(64.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(colors.errorContainer.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = colors.statusError,
                            modifier = Modifier.size(30.dp),
                        )
                    }

                    // Orbiting badge pill "PERINGATAN"
                    Surface(
                        modifier =
                            Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 2.dp, bottom = 2.dp),
                        shape = RoundedCornerShape(50),
                        color = colors.surfaceContainerHigh,
                        border = BorderStroke(1.dp, colors.statusError.copy(alpha = 0.4f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(colors.statusError),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "PERINGATAN",
                                color = colors.statusError,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    "Hapus Catatan Ini?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "Catatan \"$judul\" beserta seluruh daftarnya akan dipindahkan ke Sampah dan dihapus dari Firebase Firestore.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    lineHeight = 18.sp,
                )

                Spacer(Modifier.height(16.dp))

                // Kartu pratinjau ringkasan catatan
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
                    border = BorderStroke(1.dp, colors.ghostBorder),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = colors.nebulaViolet.copy(alpha = 0.2f),
                            ) {
                                Text(
                                    if (tipe == "list") "#Checklist" else "#Catatan",
                                    color = colors.nebulaVioletLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = colors.cosmicCyan,
                                    modifier = Modifier.size(13.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Cloud Aktif",
                                    color = colors.cosmicCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }

                        Spacer(Modifier.height(6.dp))

                        Text(
                            judul,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Spacer(Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            if (tipe == "list") {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = colors.primaryLight,
                                        modifier = Modifier.size(13.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "$selesaiItems dari $totalItems selesai",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textSecondary,
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Attachment,
                                    contentDescription = null,
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(13.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Google Drive",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textMuted,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Opsi hapus lampiran Drive
                Surface(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { hapusLampiranDrive = !hapusLampiranDrive },
                    color = colors.surfaceContainerHigh.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = hapusLampiranDrive,
                            onCheckedChange = { hapusLampiranDrive = it },
                            colors =
                                CheckboxDefaults.colors(
                                    checkedColor = colors.statusError,
                                    checkmarkColor = colors.onError,
                                ),
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "Hapus lampiran dari Google Drive",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                            )
                            Text(
                                "Termasuk berkas aset & cadangan cloud",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted,
                                fontSize = 11.sp,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Tombol aksi
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { onConfirmDelete(hapusLampiranDrive) },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = colors.errorContainer,
                                contentColor = if (colors.isDark) colors.statusError else colors.onError,
                            ),
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Pindahkan ke Sampah",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = colors.surfaceContainerHighest,
                                contentColor = colors.textSecondary,
                            ),
                    ) {
                        Text("Batal", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/**
 * Menu Tindakan dan Kelola Berkas Google Drive (Modal Bottom Sheet).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuTindakanKelolaFileDrive(
    file: Map<String, Any>,
    onDismiss: () -> Unit,
    onOpenInDrive: () -> Unit,
    onRename: () -> Unit,
    onShareLink: () -> Unit,
    onDownload: () -> Unit,
    onDeleteFile: () -> Unit,
) {
    val colors = SpaceBridgeTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val nama = file["nama"] as? String ?: "Berkas_Drive"
    val ukuran = file["ukuran"] as? String ?: "Ukuran tidak diketahui"
    val waktu = file["waktu"] as? Timestamp
    val waktuTeks =
        remember(waktu) {
            waktu?.toDate()?.let {
                SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("id")).format(it)
            } ?: "Terkini"
        }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surfaceContainerLow,
        dragHandle = {
            Box(
                modifier =
                    Modifier
                        .padding(vertical = 10.dp)
                        .width(44.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceContainerHighest),
            )
        },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            // Kartu header informasi berkas
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
                border = BorderStroke(1.dp, colors.ghostBorder),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Ikon format berkas
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = colors.surfaceContainerHigh,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                when {
                                    nama.endsWith(".pdf", ignoreCase = true) -> Icons.Default.PictureAsPdf
                                    nama.matches(Regex(".*\\.(jpg|png|webp|jpeg)", RegexOption.IGNORE_CASE)) -> Icons.Default.Image
                                    nama.matches(Regex(".*\\.(xls|xlsx|csv)", RegexOption.IGNORE_CASE)) -> Icons.Default.TableChart
                                    else -> Icons.Default.Description
                                },
                                contentDescription = null,
                                tint = colors.cosmicCyan,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            nama,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "$ukuran • $waktuTeks • Google Drive SpaceBridge",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = colors.cosmicCyan.copy(alpha = 0.12f),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier =
                                        Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(colors.cosmicCyan),
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    "Terhubung ke Drive",
                                    color = colors.cosmicCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Action Items List
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ActionItemRow(
                    icon = Icons.Default.OpenInNew,
                    title = "Buka Berkas di Drive",
                    subtitle = "Pratinjau langsung via web atau viewer resmi",
                    iconColor = colors.primaryLight,
                    onClick = onOpenInDrive,
                )
                ActionItemRow(
                    icon = Icons.Default.Edit,
                    title = "Ganti Nama Berkas (Rename)",
                    subtitle = "Ubah metadata berkas di Google Drive cloud",
                    iconColor = colors.nebulaVioletLight,
                    onClick = onRename,
                )
                ActionItemRow(
                    icon = Icons.Default.Share,
                    title = "Bagikan Tautan Aman",
                    subtitle = "Salin URL berizin akses terenkripsi",
                    iconColor = colors.cosmicCyan,
                    onClick = onShareLink,
                )
                ActionItemRow(
                    icon = Icons.Default.Download,
                    title = "Unduh Salinan Offline",
                    subtitle = "Simpan duplikat ke penyimpanan lokal",
                    iconColor = colors.primaryLight,
                    onClick = onDownload,
                )
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = colors.surfaceContainerHighest.copy(alpha = 0.6f))
            Spacer(Modifier.height(14.dp))

            // Bagian tindakan destruktif (hapus berkas)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = colors.errorContainer.copy(alpha = 0.3f),
                border = BorderStroke(1.dp, colors.statusError.copy(alpha = 0.25f)),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier =
                                Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.errorContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.DeleteForever,
                                contentDescription = null,
                                tint = colors.statusError,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "Hapus dari Google Drive",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.statusError,
                            )
                            Text(
                                "Berkas akan dipindahkan ke Sampah Google Drive Anda.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = onDeleteFile,
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = colors.statusError,
                                    contentColor = colors.onError,
                                ),
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Hapus Berkas Ini", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = colors.surfaceContainerHigh,
                                    contentColor = colors.textPrimary,
                                ),
                        ) {
                            Text("Batal", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun ActionItemRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color,
    onClick: () -> Unit,
) {
    val colors = SpaceBridgeTheme.colors

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onClick() },
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = colors.surfaceContainerHigh,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                    )
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * Kartu Penyimpanan Kuota Google Drive.
 */
@Composable
fun DriveStorageCard(
    email: String,
    usedStorageText: String,
    onSyncClick: () -> Unit = {},
) {
    val colors = SpaceBridgeTheme.colors

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
        border = BorderStroke(1.dp, colors.ghostBorder),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = colors.surfaceContainerHigh,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = colors.cosmicCyan,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "Penyimpanan Terpadu",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            email,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.surfaceContainerHigh,
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSyncClick() },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = null,
                            tint = colors.cosmicCyan,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Sinkronkan",
                            color = colors.cosmicCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        usedStorageText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "/ 15 GB Kuota Drive",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                    )
                }
                Text(
                    "Aktif",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.cosmicCyan,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(8.dp))

            // Gauge progress bar
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(colors.surfaceContainerHighest),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(0.35f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.horizontalGradient(listOf(colors.cosmicCyan, colors.primaryIndigo)),
                            ),
                )
            }

            Spacer(Modifier.height(10.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = colors.surfaceContainerLow,
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = colors.cosmicCyan,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Metadata tersimpan realtime di Firestore dan berkas fisik aman di Google Drive Anda.",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                    )
                }
            }
        }
    }
}

/**
 * Item Kartu Berkas dengan aksi unduh, bagikan, dan menu tindakan.
 */
@Composable
fun ModernFileCard(
    file: Map<String, Any>,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onDownload: () -> Unit,
) {
    val colors = SpaceBridgeTheme.colors
    val nama = file["nama"] as? String ?: "Berkas_Baru"
    val ukuran = file["ukuran"] as? String ?: "Drive File"

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
        border = BorderStroke(1.dp, colors.ghostBorder),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = colors.surfaceContainerHigh,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            when {
                                nama.endsWith(".pdf", ignoreCase = true) -> Icons.Default.PictureAsPdf
                                nama.matches(Regex(".*\\.(jpg|png|webp|jpeg)", RegexOption.IGNORE_CASE)) -> Icons.Default.Image
                                nama.matches(Regex(".*\\.(xls|xlsx|csv)", RegexOption.IGNORE_CASE)) -> Icons.Default.TableChart
                                else -> Icons.Default.Description
                            },
                            contentDescription = null,
                            tint = colors.cosmicCyan,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column {
                    Text(
                        nama,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            ukuran,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                        )
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier =
                                Modifier
                                    .size(3.dp)
                                    .clip(CircleShape)
                                    .background(colors.textMuted),
                        )
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = colors.cosmicCyan.copy(alpha = 0.12f),
                        ) {
                            Text(
                                "Tersinkron",
                                color = colors.cosmicCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onDownload,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = "Unduh",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }

                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Bagikan",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

/**
 * Dialog Ganti Nama Berkas
 */
@Composable
fun DialogGantiNamaFile(
    namaLama: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = SpaceBridgeTheme.colors
    var namaBaru by remember { mutableStateOf(namaLama) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = colors.surfaceContainer,
            border = BorderStroke(1.dp, colors.ghostBorder),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Ganti Nama Berkas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = namaBaru,
                    onValueChange = { namaBaru = it },
                    label = { Text("Nama Berkas Baru", color = colors.textMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors =
                        TextFieldDefaults.colors(
                            focusedContainerColor = colors.surfaceContainerHigh,
                            unfocusedContainerColor = colors.surfaceContainerHigh,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedIndicatorColor = colors.cosmicCyan,
                            unfocusedIndicatorColor = colors.ghostBorder,
                        ),
                )
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal", color = colors.textSecondary)
                    }
                    Button(
                        onClick = {
                            if (namaBaru.isNotBlank()) onConfirm(namaBaru.trim())
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primaryIndigo),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text("Simpan", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
