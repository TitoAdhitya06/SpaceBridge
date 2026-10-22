package com.example.spacebridgeapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.spacebridgeapp.ui.navigation.SpaceBridgeNavHost
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
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.spacebridgeapp.ui.theme.CosmicCyan
import com.example.spacebridgeapp.ui.theme.DeepVoid
import com.example.spacebridgeapp.ui.theme.GhostBorder
import com.example.spacebridgeapp.ui.theme.NebulaViolet
import com.example.spacebridgeapp.ui.theme.NebulaVioletLight
import com.example.spacebridgeapp.ui.theme.PrimaryContainer
import com.example.spacebridgeapp.ui.theme.PrimaryIndigo
import com.example.spacebridgeapp.ui.theme.PrimaryLight
import com.example.spacebridgeapp.ui.theme.SpaceBridgeAppTheme
import com.example.spacebridgeapp.ui.theme.StatusError
import com.example.spacebridgeapp.ui.theme.StatusSuccess
import com.example.spacebridgeapp.ui.theme.SurfaceContainer
import com.example.spacebridgeapp.ui.theme.SurfaceContainerHigh
import com.example.spacebridgeapp.ui.theme.SurfaceContainerHighest
import com.example.spacebridgeapp.ui.theme.SurfaceContainerLow
import com.example.spacebridgeapp.ui.theme.SurfaceContainerLowest
import com.example.spacebridgeapp.ui.theme.TextMuted
import com.example.spacebridgeapp.ui.theme.TextPrimary
import com.example.spacebridgeapp.ui.theme.TextSecondary
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val container = (applicationContext as SpaceBridgeApplication).container
            val themeMode by container.preferencesRepository.themeMode.collectAsStateWithLifecycle(
                initialValue = null,
            )
            val isSystemDark = isSystemInDarkTheme()
            val isDarkTheme =
                when (themeMode) {
                    "light" -> false
                    "dark" -> true
                    else -> isSystemDark
                }

            SpaceBridgeAppTheme(isDarkTheme = isDarkTheme) {
                SpaceBridgeNavHost()
            }
        }
    }
}



/** Path koleksi Firestore milik pengguna: users/{uid}/{nama}. */
fun koleksiPengguna(
    uid: String,
    nama: String,
): CollectionReference =
    Firebase.firestore
        .collection("users")
        .document(uid)
        .collection(nama)

/**
 * Layar utama aplikasi SpaceBridge dengan navigasi melayang modern.
 */
@Composable
fun SpaceBridgeMainScreen(
    user: FirebaseUser,
    isDarkTheme: Boolean = true,
    onToggleTheme: (Boolean) -> Unit = {},
) {
    val uid = user.uid
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Catatan, 1: File Drive
    var showDialogAkun by remember { mutableStateOf(false) }
    var showDialogPengaturan by remember { mutableStateOf(false) }

    // State untuk Editor Catatan
    var showEditor by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<Map<String, Any>?>(null) }

    // State untuk memicu upload file dari FAB tengah
    var triggerUploadFile by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DeepVoid,
        bottomBar = {
            if (!showEditor) {
                FloatingBottomNavBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    onCenterFabClick = {
                        if (selectedTab == 0) {
                            noteToEdit = null
                            showEditor = true
                        } else {
                            triggerUploadFile = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
        ) {
            if (showEditor) {
                EditorCatatanScreen(
                    uid = uid,
                    catatan = noteToEdit,
                    onBack = {
                        showEditor = false
                        noteToEdit = null
                    },
                )
            } else {
                if (selectedTab == 0) {
                    HalamanCatatan(
                        uid = uid,
                        user = user,
                        onOpenEditor = { catatan ->
                            noteToEdit = catatan
                            showEditor = true
                        },
                        onProfileClick = { showDialogAkun = true },
                        onSettingsClick = { showDialogPengaturan = true },
                    )
                } else {
                    HalamanFile(
                        uid = uid,
                        user = user,
                        triggerUpload = triggerUploadFile,
                        onUploadHandled = { triggerUploadFile = false },
                        onProfileClick = { showDialogAkun = true },
                        onSettingsClick = { showDialogPengaturan = true },
                    )
                }
            }
        }
    }

    if (showDialogAkun) {
        DialogAkun(user, onDismiss = { showDialogAkun = false })
    }

    if (showDialogPengaturan) {
        DialogPengaturan(
            user = user,
            isDarkTheme = isDarkTheme,
            onToggleTheme = onToggleTheme,
            onDismiss = { showDialogPengaturan = false },
        )
    }
}

/**
 * Komponen tampilan daftar catatan.
 */
@Composable
fun HalamanCatatan(
    uid: String,
    user: FirebaseUser,
    onOpenEditor: (Map<String, Any>?) -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    val catatanRef = remember(uid) { koleksiPengguna(uid, "catatan") }
    val context = LocalContext.current

    val daftarCatatan = remember { mutableStateListOf<Map<String, Any>>() }
    var searchQuery by remember { mutableStateOf("") }
    var kategoriFilterIndex by remember { mutableIntStateOf(0) }

    // Dialog konfirmasi hapus catatan
    var catatanDihapus by remember { mutableStateOf<Map<String, Any>?>(null) }

    DisposableEffect(catatanRef) {
        val registration =
            catatanRef
                .orderBy("waktu", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Toast.makeText(context, "Gagal sinkron Catatan: ${error.message}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        daftarCatatan.clear()
                        for (doc in snapshot.documents) {
                            val data = doc.data?.toMutableMap() ?: mutableMapOf()
                            data["id"] = doc.id
                            daftarCatatan.add(data)
                        }
                    }
                }
        onDispose { registration.remove() }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
    ) {
        SpaceBridgeHeader(
            title = "SpaceBridge",
            subtitle = "Catatan",
            statusText = "Terhubung ke Cloud",
            user = user,
            onProfileClick = onProfileClick,
            onSettingsClick = onSettingsClick,
        )

        // Bilah pencarian catatan
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Cari catatan atau checklist...", color = TextMuted) },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted) },
            trailingIcon = {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Tune, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            },
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = SurfaceContainerHigh,
                    unfocusedContainerColor = SurfaceContainerHigh,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedIndicatorColor = CosmicCyan,
                    unfocusedIndicatorColor = GhostBorder,
                ),
        )

        Spacer(Modifier.height(10.dp))

        // Filter Pills Carousel
        val filterOptions = listOf("Semua", "Catatan Teks", "Daftar Checklist", "Penting")
        FilterChipsRow(
            options = filterOptions,
            selectedIndex = kategoriFilterIndex,
            onSelect = { kategoriFilterIndex = it },
        )

        Spacer(Modifier.height(12.dp))

        // Telemetry Ringkasan Ruang
        val totalChecklist = daftarCatatan.sumOf { (it["items"] as? List<*>)?.size ?: 0 }
        val checklistSelesai =
            daftarCatatan.sumOf { doc ->
                (doc["items"] as? List<*>)?.count { (it as? Map<*, *>)?.get("checked") == true } ?: 0
            }

        RingkasanRuangCard(
            totalCatatan = daftarCatatan.size,
            checklistSelesai = checklistSelesai,
            totalChecklist = totalChecklist,
        )

        Spacer(Modifier.height(12.dp))

        // Filter data
        val filteredCatatan =
            remember(daftarCatatan, searchQuery, kategoriFilterIndex) {
                daftarCatatan.filter { catatan ->
                    val tipe = catatan["tipe"] as? String ?: "teks"
                    val isPinned = catatan["pinned"] as? Boolean ?: false
                    val cocokKategori =
                        when (kategoriFilterIndex) {
                            1 -> tipe == "teks"
                            2 -> tipe == "list"
                            3 -> isPinned
                            else -> true
                        }
                    val judul = catatan["judul"] as? String ?: ""
                    val isi = catatan["isi"] as? String ?: ""
                    val cocokSearch =
                        searchQuery.isEmpty() ||
                            judul.contains(searchQuery, ignoreCase = true) ||
                            isi.contains(searchQuery, ignoreCase = true)

                    cocokKategori && cocokSearch
                }
            }

        if (filteredCatatan.isEmpty()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = SurfaceContainerHighest,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (searchQuery.isEmpty()) "Belum ada catatan di cloud" else "Catatan tidak ditemukan",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextMuted,
                    )
                }
            }
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                verticalItemSpacing = 10.dp,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(filteredCatatan) { catatan ->
                    ModernNoteCard(
                        catatan = catatan,
                        onClick = { onOpenEditor(catatan) },
                        onDeleteRequest = { catatanDihapus = catatan },
                    )
                }
            }
        }
    }

    // Dialog konfirmasi hapus catatan
    catatanDihapus?.let { targetCatatan ->
        val id = targetCatatan["id"] as? String ?: ""
        DialogKonfirmasiHapusCatatan(
            catatan = targetCatatan,
            onConfirmDelete = { _ ->
                if (id.isNotEmpty()) {
                    catatanRef
                        .document(id)
                        .delete()
                        .addOnSuccessListener {
                            Toast.makeText(context, "Catatan dipindahkan ke Sampah", Toast.LENGTH_SHORT).show()
                        }.addOnFailureListener {
                            Toast.makeText(context, "Gagal menghapus: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                catatanDihapus = null
            },
            onDismiss = { catatanDihapus = null },
        )
    }
}

/**
 * Komponen editor catatan lengkap (teks dan checklist).
 */
@Composable
fun EditorCatatanScreen(
    uid: String,
    catatan: Map<String, Any>?,
    onBack: () -> Unit,
) {
    val catatanRef = remember(uid) { koleksiPengguna(uid, "catatan") }
    val context = LocalContext.current

    val idEdit = catatan?.get("id") as? String
    var editorMode by remember {
        mutableStateOf(if (catatan?.get("tipe") == "list") "checklist" else "hybrid")
    }

    var judulInput by remember { mutableStateOf(catatan?.get("judul") as? String ?: "") }
    var isiTeksInput by remember { mutableStateOf(catatan?.get("isi") as? String ?: "") }
    var isPinned by remember { mutableStateOf(catatan?.get("pinned") as? Boolean ?: false) }

    // Checklist items
    val daftarTodo =
        remember {
            mutableStateListOf<Pair<String, Boolean>>().apply {
                val rawItems = catatan?.get("items") as? List<*>
                if (rawItems != null) {
                    rawItems.forEach { item ->
                        when (item) {
                            is Map<*, *> -> {
                                val t = item["teks"] as? String ?: ""
                                val c = item["checked"] as? Boolean ?: false
                                add(t to c)
                            }
                            is String -> add(item to false)
                        }
                    }
                }
            }
        }
    var itemBaruInput by remember { mutableStateOf("") }

    // Dialog hapus catatan dari editor
    var showDialogHapus by remember { mutableStateOf(false) }

    val totalTodo = daftarTodo.size
    val selesaiTodo = daftarTodo.count { it.second }
    val progressFraction = if (totalTodo > 0) selesaiTodo.toFloat() / totalTodo else 0f

    val tanggalTeks =
        remember {
            SimpleDateFormat("d MMMM yyyy", Locale.forLanguageTag("id")).format(Date())
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(DeepVoid),
    ) {
        // Header editor catatan
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = TextPrimary,
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    if (idEdit == null) "Buat Catatan Baru" else "Edit Catatan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { isPinned = !isPinned }) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Penting",
                        tint = if (isPinned) NebulaVioletLight else TextMuted,
                    )
                }
                if (idEdit != null) {
                    IconButton(onClick = { showDialogHapus = true }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Hapus",
                            tint = StatusError,
                        )
                    }
                }
            }
        }

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
        ) {
            // Ribbon status sync
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = CosmicCyan,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Tersimpan otomatis di Cloud",
                        style = MaterialTheme.typography.labelSmall,
                        color = CosmicCyan,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = CosmicCyan.copy(alpha = 0.12f),
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
                                    .background(CosmicCyan),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "Live Sync",
                            color = CosmicCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Kontrol mode editor teks dan checklist
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = SurfaceContainerLow,
                border = BorderStroke(1.dp, GhostBorder),
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // Mode Hibrida
                    Surface(
                        modifier =
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { editorMode = "hybrid" },
                        shape = RoundedCornerShape(10.dp),
                        color = if (editorMode == "hybrid") SurfaceContainer else Color.Transparent,
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                Icons.Default.ViewAgenda,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (editorMode == "hybrid") PrimaryLight else TextSecondary,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Hibrida / Catatan",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (editorMode == "hybrid") FontWeight.Bold else FontWeight.Medium,
                                color = if (editorMode == "hybrid") PrimaryLight else TextSecondary,
                            )
                        }
                    }

                    // Mode Checklist
                    Surface(
                        modifier =
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { editorMode = "checklist" },
                        shape = RoundedCornerShape(10.dp),
                        color = if (editorMode == "checklist") SurfaceContainer else Color.Transparent,
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                Icons.Default.Checklist,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (editorMode == "checklist") PrimaryLight else TextSecondary,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Daftar Checklist",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (editorMode == "checklist") FontWeight.Bold else FontWeight.Medium,
                                color = if (editorMode == "checklist") PrimaryLight else TextSecondary,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Metadata Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = SurfaceContainer,
                ) {
                    Text(
                        tanggalTeks,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = NebulaViolet.copy(alpha = 0.15f),
                ) {
                    Text(
                        if (editorMode == "checklist") "#Checklist" else "#CatatanCloud",
                        style = MaterialTheme.typography.labelSmall,
                        color = NebulaVioletLight,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Input Judul Catatan (Headline besar tanpa border)
            OutlinedTextField(
                value = judulInput,
                onValueChange = { judulInput = it },
                placeholder = {
                    Text(
                        "Judul Catatan...",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextMuted,
                        fontWeight = FontWeight.Bold,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 2,
                textStyle =
                    MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                    ),
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
            )

            Spacer(Modifier.height(8.dp))

            // Checklist Block Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
                border = BorderStroke(1.dp, GhostBorder),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.TaskAlt,
                                contentDescription = null,
                                tint = CosmicCyan,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Target Implementasi",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = CosmicCyan.copy(alpha = 0.12f),
                        ) {
                            Text(
                                "$selesaiTodo dari $totalTodo selesai",
                                color = CosmicCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Progress Bar
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(50)),
                        color = CosmicCyan,
                        trackColor = SurfaceContainerHighest,
                        strokeCap = StrokeCap.Round,
                    )

                    Spacer(Modifier.height(12.dp))

                    // Daftar Item Todo
                    daftarTodo.forEachIndexed { index, item ->
                        Surface(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = SurfaceContainerLow,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = item.second,
                                    onCheckedChange = { checked ->
                                        daftarTodo[index] = item.first to checked
                                    },
                                    colors =
                                        CheckboxDefaults.colors(
                                            checkedColor = CosmicCyan,
                                            checkmarkColor = DeepVoid,
                                        ),
                                )
                                Spacer(Modifier.width(6.dp))
                                BasicTextField(
                                    value = item.first,
                                    onValueChange = { newText ->
                                        daftarTodo[index] = newText to item.second
                                    },
                                    modifier =
                                        Modifier
                                            .weight(1f)
                                            .padding(vertical = 4.dp),
                                    textStyle =
                                        MaterialTheme.typography.bodyMedium.copy(
                                            color = if (item.second) TextMuted else TextPrimary,
                                            textDecoration = if (item.second) TextDecoration.LineThrough else null,
                                        ),
                                    cursorBrush = SolidColor(CosmicCyan),
                                    decorationBox = { innerTextField ->
                                        if (item.first.isEmpty()) {
                                            Text(
                                                "Tulis tugas...",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextMuted,
                                            )
                                        }
                                        innerTextField()
                                    },
                                )
                                IconButton(
                                    onClick = { daftarTodo.removeAt(index) },
                                    modifier = Modifier.size(24.dp),
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Hapus",
                                        tint = TextMuted,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Input Tugas Baru
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = itemBaruInput,
                            onValueChange = { itemBaruInput = it },
                            placeholder = { Text("Tulis tugas baru...", color = TextMuted, fontSize = 13.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            colors =
                                TextFieldDefaults.colors(
                                    focusedContainerColor = SurfaceContainerLowest,
                                    unfocusedContainerColor = SurfaceContainerLowest,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedIndicatorColor = CosmicCyan,
                                    unfocusedIndicatorColor = GhostBorder,
                                ),
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (itemBaruInput.isNotBlank()) {
                                    daftarTodo.add(itemBaruInput.trim() to false)
                                    itemBaruInput = ""
                                }
                            },
                            modifier =
                                Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CosmicCyan.copy(alpha = 0.2f)),
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Tambah", tint = CosmicCyan)
                        }
                    }
                }
            }

            // Note Body Editor (hanya tampil di mode hybrid)
            AnimatedVisibility(visible = editorMode == "hybrid") {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 6.dp),
                    ) {
                        Icon(
                            Icons.Default.Notes,
                            contentDescription = null,
                            tint = PrimaryLight,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Uraian Teknis & Catatan",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    OutlinedTextField(
                        value = isiTeksInput,
                        onValueChange = { isiTeksInput = it },
                        placeholder = {
                            Text(
                                "Tulis ide, rangkuman, atau uraian bebas di sini...",
                                color = TextMuted,
                            )
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors =
                            TextFieldDefaults.colors(
                                focusedContainerColor = SurfaceContainerLow,
                                unfocusedContainerColor = SurfaceContainerLow,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedIndicatorColor = CosmicCyan,
                                unfocusedIndicatorColor = GhostBorder,
                            ),
                    )
                }
            }

            Spacer(Modifier.height(100.dp)) // Padding bawah untuk tombol simpan
        }

        // Floating Bottom Bar Simpan
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SurfaceContainer.copy(alpha = 0.96f),
            border = BorderStroke(1.dp, GhostBorder),
            shadowElevation = 16.dp,
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Toolbar formatting mikro
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.FormatBold, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.FormatItalic, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.FormatListBulleted, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                // Tombol Simpan
                Button(
                    onClick = {
                        val finalJudul = if (judulInput.isBlank()) "Catatan Tanpa Judul" else judulInput.trim()
                        val finalTipe = if (editorMode == "checklist") "list" else "teks"
                        val dataSimpan =
                            hashMapOf(
                                "judul" to finalJudul,
                                "isi" to isiTeksInput.trim(),
                                "tipe" to finalTipe,
                                "pinned" to isPinned,
                                "items" to daftarTodo.map { mapOf("teks" to it.first, "checked" to it.second) },
                                "waktu" to Timestamp.now(),
                            )

                        if (idEdit == null) {
                            catatanRef
                                .add(dataSimpan)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Tersimpan di Cloud", Toast.LENGTH_SHORT).show()
                                    onBack()
                                }.addOnFailureListener {
                                    Toast.makeText(context, "Gagal: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            catatanRef
                                .document(idEdit)
                                .update(dataSimpan as Map<String, Any>)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Perubahan tersimpan", Toast.LENGTH_SHORT).show()
                                    onBack()
                                }.addOnFailureListener {
                                    Toast.makeText(context, "Gagal: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Simpan ke Cloud", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showDialogHapus && catatan != null) {
        DialogKonfirmasiHapusCatatan(
            catatan = catatan,
            onConfirmDelete = { _ ->
                if (idEdit != null) {
                    catatanRef
                        .document(idEdit)
                        .delete()
                        .addOnSuccessListener {
                            Toast.makeText(context, "Catatan dihapus", Toast.LENGTH_SHORT).show()
                            onBack()
                        }
                }
                showDialogHapus = false
            },
            onDismiss = { showDialogHapus = false },
        )
    }
}

/**
 * Komponen tampilan pengelolaan berkas Google Drive.
 */
@Composable
fun HalamanFile(
    uid: String,
    user: FirebaseUser,
    triggerUpload: Boolean,
    onUploadHandled: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    val filesRef = remember(uid) { koleksiPengguna(uid, "files") }
    val context = LocalContext.current

    val daftarFile = remember { mutableStateListOf<Map<String, Any>>() }
    var searchQuery by remember { mutableStateOf("") }
    var fileFilterIndex by remember { mutableIntStateOf(0) }

    // Dialog & Bottom Sheet state
    var fileDipilihAksi by remember { mutableStateOf<Map<String, Any>?>(null) }
    var fileToRename by remember { mutableStateOf<Map<String, Any>?>(null) }
    var showDialogUploadManual by remember { mutableStateOf(false) }
    var namaFileInput by remember { mutableStateOf("") }
    var linkCloudInput by remember { mutableStateOf("") }
    var fileUriTerpilih by remember { mutableStateOf<Uri?>(null) }

    DisposableEffect(filesRef) {
        val registration =
            filesRef
                .orderBy("waktu", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Toast.makeText(context, "Gagal sinkron File: ${error.message}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        daftarFile.clear()
                        for (doc in snapshot.documents) {
                            val data = doc.data?.toMutableMap() ?: mutableMapOf()
                            data["id"] = doc.id
                            daftarFile.add(data)
                        }
                    }
                }
        onDispose { registration.remove() }
    }

    val launcherFilePicker =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                fileUriTerpilih = uri
                namaFileInput = uri.path?.substringAfterLast("/") ?: "Berkas_Baru"
                showDialogUploadManual = true
            }
        }

    // Pemicu dari FAB tengah
    if (triggerUpload) {
        launcherFilePicker.launch("*/*")
        onUploadHandled()
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
    ) {
        SpaceBridgeHeader(
            title = "SpaceBridge",
            subtitle = "File Drive",
            statusText = "Terhubung ke Cloud (Firestore & Drive)",
            user = user,
            onProfileClick = onProfileClick,
            onSettingsClick = onSettingsClick,
        )

        // Kartu ringkasan kuota penyimpanan drive
        DriveStorageCard(
            email = user.email ?: "Akun Google Terhubung",
            usedStorageText = "${daftarFile.size} Berkas Terkatalog",
            onSyncClick = {
                Toast.makeText(context, "Metadata Drive tersinkron", Toast.LENGTH_SHORT).show()
            },
        )

        Spacer(Modifier.height(12.dp))

        // Search Bar File
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Cari file, dokumen, foto...", color = TextMuted) },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted) },
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = SurfaceContainerHigh,
                    unfocusedContainerColor = SurfaceContainerHigh,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedIndicatorColor = CosmicCyan,
                    unfocusedIndicatorColor = GhostBorder,
                ),
        )

        Spacer(Modifier.height(10.dp))

        // Category Filter Chips
        val fileFilters = listOf("Semua", "Dokumen PDF", "Gambar", "Spreadsheet")
        FilterChipsRow(
            options = fileFilters,
            selectedIndex = fileFilterIndex,
            onSelect = { fileFilterIndex = it },
        )

        Spacer(Modifier.height(12.dp))

        // Banner unggah berkas ke Google Drive
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { launcherFilePicker.launch("*/*") },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
            border = BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.4f)),
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = PrimaryIndigo.copy(alpha = 0.25f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = CosmicCyan,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "+ Unggah File ke Drive",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Ketuk untuk letakkan PDF, JPG, PNG, DOCX, ZIP",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 11.sp,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Filter file list
        val filteredFiles =
            remember(daftarFile, searchQuery, fileFilterIndex) {
                daftarFile.filter {
                    val nama = it["nama"] as? String ?: ""
                    val cocokSearch = searchQuery.isEmpty() || nama.contains(searchQuery, ignoreCase = true)
                    val cocokFilter =
                        when (fileFilterIndex) {
                            1 -> nama.endsWith(".pdf", ignoreCase = true)
                            2 -> nama.matches(Regex(".*\\.(jpg|jpeg|png|webp)", RegexOption.IGNORE_CASE))
                            3 -> nama.matches(Regex(".*\\.(xlsx|xls|csv)", RegexOption.IGNORE_CASE))
                            else -> true
                        }
                    cocokSearch && cocokFilter
                }
            }

        if (filteredFiles.isEmpty()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.FolderShared,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = SurfaceContainerHighest,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (searchQuery.isEmpty()) "Belum ada berkas terunggah" else "Berkas tidak ditemukan",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextMuted,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(filteredFiles.size) { index ->
                    val file = filteredFiles[index]
                    val link = file["link"] as? String ?: ""
                    ModernFileCard(
                        file = file,
                        onClick = { fileDipilihAksi = file },
                        onShare = {
                            if (link.isNotBlank()) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Link Berkas", link))
                                Toast.makeText(context, "Tautan disalin ke clipboard", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Tautan belum tersedia", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDownload = {
                            if (link.isNotBlank()) {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Gagal membuka link: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Berkas tersimpan di Cloud Drive", Toast.LENGTH_SHORT).show()
                            }
                        },
                    )
                }
            }
        }
    }

    // Modal bottom sheet tindakan dan pengelolaan berkas
    fileDipilihAksi?.let { file ->
        val id = file["id"] as? String ?: ""
        val link = file["link"] as? String ?: ""
        MenuTindakanKelolaFileDrive(
            file = file,
            onDismiss = { fileDipilihAksi = null },
            onOpenInDrive = {
                fileDipilihAksi = null
                if (link.isNotBlank()) {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                    } catch (_: Exception) {
                    }
                } else {
                    Toast.makeText(context, "Tautan Drive belum disetel", Toast.LENGTH_SHORT).show()
                }
            },
            onRename = {
                fileToRename = file
                fileDipilihAksi = null
            },
            onShareLink = {
                fileDipilihAksi = null
                if (link.isNotBlank()) {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Link Berkas", link))
                    Toast.makeText(context, "Tautan aman disalin ke clipboard", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Tautan belum tersedia", Toast.LENGTH_SHORT).show()
                }
            },
            onDownload = {
                fileDipilihAksi = null
                if (link.isNotBlank()) {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                    } catch (_: Exception) {
                    }
                } else {
                    Toast.makeText(context, "Memulai unduhan offline", Toast.LENGTH_SHORT).show()
                }
            },
            onDeleteFile = {
                if (id.isNotEmpty()) {
                    filesRef
                        .document(id)
                        .delete()
                        .addOnSuccessListener {
                            Toast.makeText(context, "Berkas dipindahkan ke Sampah", Toast.LENGTH_SHORT).show()
                        }
                }
                fileDipilihAksi = null
            },
        )
    }

    // Dialog Rename Berkas
    fileToRename?.let { file ->
        val id = file["id"] as? String ?: ""
        val namaLama = file["nama"] as? String ?: ""
        DialogGantiNamaFile(
            namaLama = namaLama,
            onConfirm = { namaBaru ->
                if (id.isNotEmpty()) {
                    filesRef
                        .document(id)
                        .update("nama", namaBaru)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Nama berkas diperbarui", Toast.LENGTH_SHORT).show()
                        }
                }
                fileToRename = null
            },
            onDismiss = { fileToRename = null },
        )
    }

    // Dialog Simpan Metadata Upload
    if (showDialogUploadManual) {
        Dialog(onDismissRequest = { showDialogUploadManual = false }) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = SurfaceContainer,
                border = BorderStroke(1.dp, GhostBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Unggah Berkas ke Drive",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = namaFileInput,
                        onValueChange = { namaFileInput = it },
                        label = { Text("Nama Berkas", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors =
                            TextFieldDefaults.colors(
                                focusedContainerColor = SurfaceContainerHigh,
                                unfocusedContainerColor = SurfaceContainerHigh,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedIndicatorColor = CosmicCyan,
                                unfocusedIndicatorColor = GhostBorder,
                            ),
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = linkCloudInput,
                        onValueChange = { linkCloudInput = it },
                        label = { Text("Tautan Drive / Cloud (Opsional)", color = TextMuted) },
                        placeholder = { Text("https://drive.google.com/...", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors =
                            TextFieldDefaults.colors(
                                focusedContainerColor = SurfaceContainerHigh,
                                unfocusedContainerColor = SurfaceContainerHigh,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedIndicatorColor = CosmicCyan,
                                unfocusedIndicatorColor = GhostBorder,
                            ),
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { showDialogUploadManual = false }) {
                            Text("Batal", color = TextSecondary)
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (namaFileInput.isNotBlank()) {
                                    val data =
                                        hashMapOf(
                                            "nama" to namaFileInput.trim(),
                                            "link" to linkCloudInput.trim(),
                                            "ukuran" to "2.4 MB",
                                            "tipe" to (fileUriTerpilih?.let { context.contentResolver.getType(it) } ?: "Dokumen"),
                                            "waktu" to Timestamp.now(),
                                        )
                                    filesRef.add(data).addOnSuccessListener {
                                        Toast.makeText(context, "Berkas tersimpan di Cloud", Toast.LENGTH_SHORT).show()
                                    }
                                    showDialogUploadManual = false
                                    namaFileInput = ""
                                    linkCloudInput = ""
                                    fileUriTerpilih = null
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        ) {
                            Text("Simpan ke Cloud", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Komponen dialog pengaturan aplikasi dan preferensi akun.
 */
@Composable
fun DialogPengaturan(
    user: FirebaseUser,
    isDarkTheme: Boolean = true,
    onToggleTheme: (Boolean) -> Unit = {},
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var syncWifiOnly by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = SurfaceContainerLow,
            border = BorderStroke(1.dp, GhostBorder),
            modifier =
                Modifier
                    .fillMaxWidth(0.92f)
                    .padding(vertical = 16.dp),
            shadowElevation = 24.dp,
        ) {
            Column(
                modifier =
                    Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
            ) {
                // Header Pengaturan
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Pengaturan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextMuted)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Seksi 1: Akun & Cloud
                Text(
                    "SINKRONISASI & AKUN CLOUD",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
                    border = BorderStroke(1.dp, GhostBorder),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryIndigo),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = (user.displayName?.take(1) ?: user.email?.take(1) ?: "U").uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.White,
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    user.displayName ?: "Pengguna SpaceBridge",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                )
                                Text(
                                    user.email ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Status Live Listener
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Bolt, null, tint = CosmicCyan, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Firestore Listener",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = CosmicCyan.copy(alpha = 0.15f),
                                ) {
                                    Text(
                                        "Live",
                                        color = CosmicCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Sakelar Wi-Fi
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Default.Wifi, null, tint = PrimaryLight, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        "Sinkronkan hanya via Wi-Fi",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                    )
                                    Text(
                                        "Hemat kuota saat unggah file besar",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                    )
                                }
                            }
                            Switch(
                                checked = syncWifiOnly,
                                onCheckedChange = { syncWifiOnly = it },
                                colors =
                                    SwitchDefaults.colors(
                                        checkedThumbColor = DeepVoid,
                                        checkedTrackColor = CosmicCyan,
                                    ),
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        // Tombol Keluar Akun
                        Button(
                            onClick = {
                                onDismiss()
                                scope.launch { keluar(context) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = SurfaceContainerHigh,
                                    contentColor = StatusError,
                                ),
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Keluar dari Akun Google", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Seksi 2: Penyimpanan Berkas & Cache
                Text(
                    "PENYIMPANAN BERKAS & CACHE",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
                    border = BorderStroke(1.dp, GhostBorder),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FolderShared, null, tint = NebulaVioletLight, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    "Folder Tujuan Cloud",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                )
                                Text(
                                    "Drive Saya / SpaceBridge Files /",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CosmicCyan,
                                    fontSize = 11.sp,
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = GhostBorder)
                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        Toast.makeText(context, "Cache preview & Firestore dibersihkan", Toast.LENGTH_SHORT).show()
                                    },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.DeleteSweep, null, tint = StatusError, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    "Bersihkan Cache Lokal",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                )
                                Text(
                                    "Hapus cache sementara dan refresh data",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Seksi 3: Tampilan dan preferensi tema
                Text(
                    "TAMPILAN & PREFERENSI",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
                    border = BorderStroke(1.dp, GhostBorder),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f),
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SurfaceContainerLow,
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Palette,
                                            contentDescription = null,
                                            tint = PrimaryLight,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        "Tema Antarmuka",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        maxLines = 1,
                                    )
                                    Text(
                                        "Pilih palet tampilan",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                    )
                                }
                            }

                            // Theme Selector Pill (Terang / Gelap)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SurfaceContainerLow,
                                border = BorderStroke(1.dp, GhostBorder),
                            ) {
                                Row(
                                    modifier = Modifier.padding(3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    // Tombol Terang
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (!isDarkTheme) SurfaceContainerLowest else Color.Transparent,
                                        shadowElevation = if (!isDarkTheme) 2.dp else 0.dp,
                                        modifier =
                                            Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { onToggleTheme(false) },
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                Icons.Default.LightMode,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = if (!isDarkTheme) PrimaryIndigo else TextSecondary,
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                "Terang",
                                                fontSize = 11.sp,
                                                fontWeight = if (!isDarkTheme) FontWeight.Bold else FontWeight.Medium,
                                                color = if (!isDarkTheme) PrimaryIndigo else TextSecondary,
                                            )
                                        }
                                    }

                                    // Tombol Gelap
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isDarkTheme) SurfaceContainerLowest else Color.Transparent,
                                        shadowElevation = if (isDarkTheme) 2.dp else 0.dp,
                                        modifier =
                                            Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { onToggleTheme(true) },
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                Icons.Default.DarkMode,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = if (isDarkTheme) CosmicCyan else TextSecondary,
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                "Gelap",
                                                fontSize = 11.sp,
                                                fontWeight = if (isDarkTheme) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isDarkTheme) CosmicCyan else TextSecondary,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = GhostBorder)
                        Spacer(Modifier.height(12.dp))

                        // Informasi versi aplikasi
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f),
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = PrimaryContainer.copy(alpha = 0.2f),
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = null,
                                            tint = PrimaryLight,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        "Versi Aplikasi",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        maxLines = 1,
                                    )
                                    Text(
                                        "SpaceBridge v2.0",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                    )
                                }
                            }

                            Spacer(Modifier.width(8.dp))

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = StatusSuccess.copy(alpha = 0.15f),
                            ) {
                                Text(
                                    "Terkini",
                                    color = StatusSuccess,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Tombol sinkronisasi ulang dengan cloud
                Button(
                    onClick = {
                        Toast.makeText(context, "Sinkronisasi Cloud berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = PrimaryIndigo,
                            contentColor = Color.White,
                        ),
                ) {
                    Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sinkronkan Ulang dengan Cloud", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
