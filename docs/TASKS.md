# Daftar tugas implementasi: SpaceBridge v3

Dokumen ini adalah panduan teknis berurutan untuk model pengembang atau programmer pelaksana. Setiap tugas dirancang mandiri, terdefinisi dengan masukan, keluaran, berkas target, dan kriteria pengujian yang jelas.

---

## 1. Aturan pelaksanaan untuk model implementer

1. Ikuti spesifikasi arsitektur di `docs/PRD.md` dan panduan visual di `DESIGN.md`.
2. Untuk setiap tugas antarmuka (UI Composable pada Fase 1 sampai Fase 4), struktur komponen, tata letak, dan salinan teks WAJIB mengacu pada prototipe Stitch yang dirinci di `docs/STITCH_SCREENS.md` (serta kode ekspor Stitch di `docs/stitch/` jika tersedia).
3. Jangan menambahkan library baru di luar tabel tech stack PRD tanpa alasan yang sangat kuat.
4. Jangan menggunakan warna ungu, nila, lavender, gradien, atau efek blur. Semua warna wajib mengambil token dari `DESIGN.md`.
5. Jangan menulis query Firestore langsung di dalam composable UI. Semua interaksi database wajib melalui Repository.
6. Jalankan `ktlint` dan pastikan kompilasi bebas error sebelum menandai tugas selesai.

---

## 2. Rincian tahapan implementasi

### Fase 0: Setup, Fondasi, Tema, dan Komponen Dasar

#### TASK-001: Pembersihan dependensi dan konfigurasi Gradle [SELESAI]
- Status: Selesai diverifikasi.
- Masukan: `app/build.gradle.kts`, `gradle/libs.versions.toml`.
- Deskripsi:
  1. Hapus dependensi `com.google.firebase:firebase-storage`.
  2. Tambahkan plugin `org.jetbrains.kotlin.plugin.serialization`.
  3. Tambahkan dependensi:
     - Navigation Compose (`androidx.navigation:navigation-compose:2.8.x` atau versi stabil terbaru).
     - AndroidX DataStore Preferences (`androidx.datastore:datastore-preferences:1.1.x`).
     - AndroidX WorkManager Kotlin + Coroutines (`androidx.work:work-runtime-ktx:2.10.x`).
     - Retrofit 2 (`com.squareup.retrofit2:retrofit:2.11.x`), OkHttp 3/4 logging interceptor, dan converter serialization `com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter`.
     - Google Play Services Auth (`com.google.android.gms:play-services-auth:21.x`).
  4. Aktifkan R8 pada `buildTypes.release` dengan `isMinifyEnabled = true` dan siapkan berkas `proguard-rules.pro`.
- Berkas target: `app/build.gradle.kts`, `gradle/libs.versions.toml`, `app/proguard-rules.pro`.
- Kriteria selesai: `gradlew assembleDebug` berhasil dikompilasi tanpa konflik dependensi.

---

#### TASK-002: Implementasi tema Compose dari DESIGN.md
- Masukan: `DESIGN.md`, `app/src/main/java/com/example/spacebridgeapp/ui/theme/`.
- Deskripsi:
  1. Buat `Color.kt` baru berisi warna solid warm: `BgColor` (#F8F4EC), `SurfaceColor` (#FFFDF9), `SurfaceMuted` (#F2ECE1), `BorderColor` (#E8E0D2), `BorderStrong` (#D3C7B3), `TextPrimary` (#2E2A25), `TextSecondary` (#5C554C), `TextTertiary` (#756D61), `Primary600` (#BE531A), `Primary500` (#E2702F), `Primary50` (#FDF1E8), `Success600` (#27774A), `Info600` (#2F6FB0), `Danger600` (#C23B32), `Warning500` (#E9A23B).
  2. Tambahkan palet mode gelap warm sesuai bagian 2 `DESIGN.md` (Latar #1C1915, Permukaan #26221D, Aksen #E8793A).
  3. Hapus seluruh variabel warna kosmik lama (`CosmicCyan`, `NebulaViolet`, `DeepVoid`).
  4. Perbarui `Type.kt` menggunakan font Plus Jakarta Sans (dengan fallback sistem yang aman).
  5. Konfigurasikan `Theme.kt` dengan `SpaceBridgeTheme` yang menyediakan akses token warna yang konsisten.
- Berkas target:
  - `app/src/main/java/com/example/spacebridgeapp/ui/theme/Color.kt`
  - `app/src/main/java/com/example/spacebridgeapp/ui/theme/Type.kt`
  - `app/src/main/java/com/example/spacebridgeapp/ui/theme/Theme.kt`
- Kriteria selesai: Pratinjau Compose menampilkan palet krem hangat di mode terang dan abu arang di mode gelap tanpa warna ungu.

---

#### TASK-003: Model data domain dan utility
- Masukan: Bagian 7 PRD.
- Deskripsi:
  1. Buat enum `NoteType`, `NoteStatus`, `FileCategory`, `FileSource`, `FileStatus`.
  2. Buat data class domain: `ChecklistItem`, `Note`, `DriveFile`, `Label`, `AppUser`.
  3. Buat utility pemetaan MIME type ke `FileCategory` di `MimeCategory.kt`.
  4. Buat formatter ukuran berkas (B, KB, MB) dan tanggal lokal Indonesia di `Formatters.kt`.
- Berkas target:
  - `app/src/main/java/com/example/spacebridgeapp/core/model/Note.kt`
  - `app/src/main/java/com/example/spacebridgeapp/core/model/DriveFile.kt`
  - `app/src/main/java/com/example/spacebridgeapp/core/model/AppUser.kt`
  - `app/src/main/java/com/example/spacebridgeapp/core/util/MimeCategory.kt`
  - `app/src/main/java/com/example/spacebridgeapp/core/util/Formatters.kt`
- Kriteria selesai: Unit test fungsi formatter dan pemetaan MIME berjalan sukses 100%.

---

#### TASK-004: Wadah dependensi (AppContainer) dan Application class
- Masukan: Bagian 6 PRD.
- Deskripsi:
  1. Buat `AppContainer` yang menginisialisasi Firestore, FirebaseAuth, DataStore, NetworkObserver, dan Repository.
  2. Buat `SpaceBridgeApplication` yang menginstansiasi `AppContainer` saat aplikasi berjalan.
  3. Daftarkan Application class di `AndroidManifest.xml`.
- Berkas target:
  - `app/src/main/java/com/example/spacebridgeapp/di/AppContainer.kt`
  - `app/src/main/java/com/example/spacebridgeapp/SpaceBridgeApplication.kt`
  - `app/src/main/AndroidManifest.xml`
- Kriteria selesai: Aplikasi dapat di-launch dan menyediakan instance repository ke Activity.

---

#### TASK-005: Rute navigasi dan scaffold utama
- Masukan: `docs/STITCH_SCREENS.md`.
- Deskripsi:
  1. Definisikan sealed class / object rute `@Serializable`: `LoginRoute`, `DriveOnboardingRoute`, `NotesRoute`, `EditorRoute(val noteId: String?)`, `FilesRoute`, `TrashRoute`, `SettingsRoute`.
  2. Siapkan `SpaceBridgeNavHost` yang mengelola perpindahan layar dan backstack.
  3. Buat komponen `FloatingBottomBar` sederhana dengan 2 tab (Catatan dan Berkas) serta FAB tengah.
- Berkas target:
  - `app/src/main/java/com/example/spacebridgeapp/ui/navigation/Routes.kt`
  - `app/src/main/java/com/example/spacebridgeapp/ui/navigation/SpaceBridgeNavHost.kt`
  - `app/src/main/java/com/example/spacebridgeapp/ui/components/FloatingBottomBar.kt`
- Kriteria selesai: Pengguna dapat berpindah antar tab tanpa lag dan state halaman tersimpan.

---

### Fase 1: Autentikasi dan Isolasi Akun Pengguna

#### TASK-101: Repositori autentikasi Google dan profil Firestore
- Masukan: Bagian 4.1 dan 7.2 PRD.
- Deskripsi:
  1. Pindahkan logika login Google dari `Auth.kt` ke `AuthRepositoryImpl`.
  2. Saat login berhasil, lakukan upsert dokumen ke `users/{uid}` berisi data nama, email, avatar, dan timestamp login.
  3. Jangan izinkan membaca atau menulis path selain `users/{uid}`.
- Berkas target:
  - `app/src/main/java/com/example/spacebridgeapp/core/data/auth/AuthRepository.kt`
  - `app/src/main/java/com/example/spacebridgeapp/core/data/auth/AuthRepositoryImpl.kt`
- Kriteria selesai: Pengguna login mendapatkan dokumen valid di koleksi `users/{uid}`.

---

#### TASK-102: Prosedur keluar akun (Logout) bersih
- Masukan: Bagian 8.1 PRD.
- Deskripsi:
  1. Implementasikan `signOut()` lengkap:
     - Hentikan seluruh snapshot listener aktif.
     - Panggil `Firebase.firestore.terminate()` kemudian `Firebase.firestore.clearPersistence()`.
     - Bersihkan berkas sementara di `context.cacheDir`.
     - Batalkan seluruh pekerjaan upload yang berjalan di WorkManager.
     - Panggil `CredentialManager.clearCredentialState()` dan `Firebase.auth.signOut()`.
- Berkas target: `app/src/main/java/com/example/spacebridgeapp/core/data/auth/AuthRepositoryImpl.kt`.
- Kriteria selesai: Setelah logout dan ganti akun Google lain, tidak ada data milik akun lama yang tersisa di cache perangkat.

---

#### TASK-103: Layar Login dan Onboarding Google Drive (SCR-01 & SCR-02)
- Masukan: `docs/STITCH_SCREENS.md` SCR-01 & SCR-02.
- Deskripsi:
  1. Bangun composable `LoginScreen` dengan tombol tunggal "Lanjutkan dengan Google".
  2. Bangun composable `DriveOnboardingScreen` dengan penjelasan folder `/SpaceBridge`.
  3. Hubungkan dengan `AuthViewModel`.
- Berkas target:
  - `app/src/main/java/com/example/spacebridgeapp/feature/auth/LoginScreen.kt`
  - `app/src/main/java/com/example/spacebridgeapp/feature/auth/DriveOnboardingScreen.kt`
  - `app/src/main/java/com/example/spacebridgeapp/feature/auth/AuthViewModel.kt`
- Kriteria selesai: Pengguna baru berhasil masuk, melihat layar penjelasan Drive, lalu diarahkan ke Beranda.

---

### Fase 2: Fitur Catatan dan Checklist

#### TASK-201: Repositori Catatan dan Pemetaan DTO [SELESAI]
- Status: Selesai diverifikasi dengan unit test dan build sukses.
- Masukan: Bagian 7.3 PRD.
- Deskripsi:
  1. Buat `NoteDto` dengan anotasi `@PropertyName` dan mapper ke model domain `Note`.
  2. Implementasikan `NotesRepositoryImpl`:
     - `observeNotes(status)`: Menggunakan query `orderBy("updatedAt", DESCENDING)` dengan `MetadataChanges.INCLUDE`.
     - `create(note)`: Menulis dokumen via `set()`.
     - `update(id, changes)`: Menulis hanya field yang berubah via `update()`, sertakan `FieldValue.serverTimestamp()`.
     - Jangan pernah memanggil `await()` pada operasi mutasi di alur UI.
- Berkas target:
  - `app/src/main/java/com/example/spacebridgeapp/core/data/notes/NoteDto.kt`
  - `app/src/main/java/com/example/spacebridgeapp/core/data/notes/NotesRepository.kt`
  - `app/src/main/java/com/example/spacebridgeapp/core/data/notes/NotesRepositoryImpl.kt`
- Kriteria selesai: Dokumen tersimpan di `users/{uid}/notes/{id}` dan pembaruan lokal segera tercermin di StateFlow.

---

#### TASK-202: Migrasi data catatan v2 ke v3 (LegacyMigrator) [SELESAI]
- Status: Selesai diverifikasi dengan unit test dan build sukses.
- Masukan: Bagian 4.4 dan 13 PRD.
- Deskripsi:
  1. Buat fungsi migrasi yang membaca dokumen dari koleksi lama `users/{uid}/catatan`.
  2. Konversi setiap dokumen ke skema `NoteDto` v3 dan simpan ke `users/{uid}/notes`.
  3. Hapus dokumen lama dari `catatan` setelah berhasil dikonversi.
  4. Perbarui field `migratedV3At` pada dokumen `users/{uid}` agar proses ini hanya dijalankan satu kali.
- Berkas target: `app/src/main/java/com/example/spacebridgeapp/core/data/migration/LegacyMigrator.kt`.
- Kriteria selesai: Catatan dari aplikasi versi lama muncul utuh di versi baru tanpa duplikasi.

---

#### TASK-203: Beranda Catatan (SCR-03) [SELESAI]
- Status: Selesai diverifikasi dengan unit test dan build sukses.
- Masukan: `docs/STITCH_SCREENS.md` SCR-03.
- Deskripsi:
  1. Buat `NotesViewModel` yang menggabungkan aliran catatan aktif, kata kunci pencarian, dan filter chip terpilih.
  2. Buat kartu catatan `ModernNoteCard` yang mendukung mode teks dan cuplikan checklist.
  3. Terapkan pemisahan bagian "DISEMATKAN" dan "LAINNYA".
  4. Buat tampilan kosong (empty state) yang rapi saat daftar tidak memiliki item.
- Berkas target:
  - `app/src/main/java/com/example/spacebridgeapp/feature/notes/NotesScreen.kt`
  - `app/src/main/java/com/example/spacebridgeapp/feature/notes/NotesViewModel.kt`
  - `app/src/main/java/com/example/spacebridgeapp/feature/notes/components/NoteCard.kt`
- Kriteria selesai: Beranda menampilkan daftar catatan secara real-time, pencarian responsif, dan tombol pin bekerja instan.

---

#### TASK-204: Editor Catatan Teks dan Checklist dengan Autosave (SCR-04 & SCR-05) [SELESAI]
- Status: Selesai diimplementasikan dan diverifikasi dengan unit test (debounce, flush, cleanup draft kosong, konversi tipe) serta build debug sukses.
- Masukan: Bagian 4.2 PRD dan SCR-04/05.
- Deskripsi:
  1. Buat `EditorViewModel` dengan logika autosave:
     - Debounce 800ms setelah pengetikan berhenti.
     - Simpan langsung tanpa debounce saat tombol kembali ditekan atau Activity masuk ke background (`onPause`).
     - Hapus draft baru jika seluruh judul, isi, dan item checklist kosong saat keluar.
  2. Buat `EditorScreen` yang mendukung peralihan jenis teks dan checklist.
  3. Pada checklist:
     - Menekan tombol Enter pada keyboard membuat item baru di bawahnya.
     - Menekan tombol Backspace pada item kosong menghapus item tersebut dan mengembalikan kursor ke baris sebelumnya.
     - Item yang dicentang otomatis pindah ke grup "Selesai" yang dapat dilipat.
- Berkas target:
  - `app/src/main/java/com/example/spacebridgeapp/feature/editor/EditorScreen.kt`
  - `app/src/main/java/com/example/spacebridgeapp/feature/editor/EditorViewModel.kt`
  - `app/src/main/java/com/example/spacebridgeapp/feature/editor/components/ChecklistSection.kt`
- Kriteria selesai: Mengetik catatan lalu menutup aplikasi secara paksa tidak menyebabkan data hilang.

---

#### TASK-205: Alur Sampah Catatan dan Pembatalan (Undo) [SELESAI]
- Status: Selesai diimplementasikan dan diverifikasi dengan unit test alur sampah, pembatalan (undo), dan timeout 5 detik, serta build debug sukses.
- Masukan: Bagian 5.4 PRD.
- Deskripsi:
  1. Tombol hapus pada kartu catatan atau editor mengubah status catatan menjadi `NoteStatus.TRASHED` dengan `trashedAt = now()`.
  2. Tampilkan Snackbar selama 5 detik dengan aksi "Urungkan". Jika diketuk, kembalikan status menjadi `ACTIVE`.
- Berkas target: `app/src/main/java/com/example/spacebridgeapp/feature/notes/NotesViewModel.kt`.
- Kriteria selesai: Catatan terhapus menghilang dari beranda dan dapat dipulihkan melalui tombol undo.

---

### Fase 3: Integrasi Google Drive dan Pengelolaan Berkas

#### TASK-301: Manajemen otorisasi Google Drive (DriveAuthManager) [SELESAI]
- Status: Selesai diimplementasikan dan diverifikasi dengan unit test serta pengujian perangkat fisik. Alur consent dialog Google Play Services Identity dihubungkan langsung ke tombol "Sambungkan" pada banner peringatan, kartu kuota, tombol unggah, dan halaman Pengaturan, sehingga otorisasi scope `drive.file` langsung memicu popup izin akun dan mengubah status menjadi "Folder: /SpaceBridge • Terhubung".
- Masukan: Bagian 9.2 PRD.
- Deskripsi:
  1. Implementasikan otorisasi scope `https://www.googleapis.com/auth/drive.file` menggunakan `Identity.getAuthorizationClient(activity)`.
  2. Kunci akun pada email pengguna yang sedang login melalui `setAccount(Account(userEmail, "com.google"))`.
  3. Sediakan fungsi `getTokenSilently()` untuk kebutuhan background worker.
- Berkas target:
  - `app/src/main/java/com/example/spacebridgeapp/core/data/drive/DriveAuthManager.kt`
  - `app/src/main/java/com/example/spacebridgeapp/core/data/drive/DriveAuthManagerImpl.kt`
- Kriteria selesai: Token akses valid berhasil diperoleh tanpa popup interaktif berulang jika izin sudah pernah diberikan.

---

#### TASK-302: Klien REST API Google Drive v3 [SELESAI]
- Status: Selesai diimplementasikan dan diverifikasi dengan 20 unit test (pembuatan/pencarian folder SpaceBridge, resumable upload session, download, update metadata, move to trash/untrash, permanent delete, permission sharing, kuota storage, retry 401 dan pemetaan error PRD 9.5), lolos ktlint, serta build debug sukses.
- Masukan: Bagian 9.3 PRD.
- Deskripsi:
  1. Buat antarmuka Retrofit untuk endpoint Drive v3:
     - Mencari folder `SpaceBridge` melalui query `appProperties`.
     - Membuat folder baru jika belum ada.
     - Menginisiasi sesi resumable upload (`POST /upload/drive/v3/files?uploadType=resumable`).
     - Mengunduh berkas (`GET /drive/v3/files/{id}?alt=media`).
     - Memperbarui metadata, memindahkan ke sampah Drive, dan menghapus permanen.
- Berkas target:
  - `app/src/main/java/com/example/spacebridgeapp/core/data/drive/DriveApi.kt`
  - `app/src/main/java/com/example/spacebridgeapp/core/data/drive/DriveRepository.kt`
  - `app/src/main/java/com/example/spacebridgeapp/core/data/drive/DriveRepositoryImpl.kt`
- Kriteria selesai: Klien dapat membuat folder `SpaceBridge` di Drive pengguna dan membaca daftar isinya.

---

#### TASK-303: Background Uploader dengan WorkManager (UploadWorker) [SELESAI]
- Status: Selesai diimplementasikan dan diverifikasi dengan unit test (FileDtoTest, UploadWorkerTest, FilesRepositoryTest), verifikasi ktlintCheck, dan assembleDebug. Fitur mencakup CoroutineWorker dengan ForegroundService dataSync, chunked resumable upload 8 MB, penyimpanan sesi DataStore, network constraints (Wi-Fi saja atau data), penolakan berkas >100 MB, pemetaan Firestore exact 18 fields, serta integrasi AppContainer.
- Masukan: Bagian 9.4 PRD.
- Deskripsi:
  1. Buat `UploadWorker` bertipe CoroutineWorker dengan foreground service notification.
  2. Terapkan pengunggahan bertahap (chunked upload) per 8 MB ke URL sesi Drive.
  3. Pasang constraint jaringan (bisa diatur Wi-Fi saja atau jaringan apa pun).
  4. Perbarui status dokumen Firestore menjadi `ready`, isi `driveFileId` dan `webViewLink` saat unggahan selesai.
  5. Jika gagal karena koneksi, lakukan retry otomatis dengan backoff eksponensial.
- Berkas target:
  - `app/src/main/java/com/example/spacebridgeapp/core/work/UploadWorker.kt`
  - `app/src/main/java/com/example/spacebridgeapp/core/data/files/FilesRepositoryImpl.kt`
- Kriteria selesai: Mengunggah berkas 20 MB tetap berjalan hingga tuntas di background meskipun aplikasi ditutup.

---

#### TASK-304: Beranda Berkas dan Menu Tindakan Berkas (SCR-07 & SCR-08) [SELESAI]
- Status: Selesai diimplementasikan dan diverifikasi dengan unit test (FilesViewModelTest dengan 12 skenario pengujian), lolos ktlintCheck, serta assembleDebug. Fitur mencakup FilesScreen (SCR-07), FileActionsSheet (SCR-08), pemilih dokumen sistem dengan persistable URI permission, pencarian, filter kategori berkas, kartu kuota folder SpaceBridge, antrean unggahan aktif, pratinjau lokal via FileProvider, ganti nama, simpan ke download, bagikan tautan, dan pemindahan ke sampah.
- Masukan: `docs/STITCH_SCREENS.md` SCR-07 & SCR-08.
- Deskripsi:
  1. Buat `FilesScreen` yang menampilkan daftar berkas dari `users/{uid}/files`.
  2. Sediakan pemilih berkas sistem menggunakan `ActivityResultContracts.OpenMultipleDocuments` dan amankan hak akses dengan `takePersistableUriPermission`.
  3. Buat `FileActionsSheet` untuk aksi: Buka di Drive, Simpan ke Download perangkat, Ganti Nama, dan Hapus.
  4. Implementasikan pratinjau lokal: Unduh berkas ke cache aplikasi lalu luncurkan intent pembaca berkas Android via `FileProvider`.
- Berkas target:
  - `app/src/main/java/com/example/spacebridgeapp/feature/files/FilesScreen.kt`
  - `app/src/main/java/com/example/spacebridgeapp/feature/files/FilesViewModel.kt`
  - `app/src/main/java/com/example/spacebridgeapp/feature/files/components/FileActionsSheet.kt`
- Kriteria selesai: Berkas berhasil diunggah ke Google Drive dan dapat dibuka kembali langsung di dalam aplikasi.

---

### Fase 4: Halaman Sampah, Pengaturan, dan Penyelesaian Sistem

#### TASK-401: Halaman Sampah dan Pembersihan Otomatis 30 Hari (SCR-09) [SELESAI]
- Status: Selesai diimplementasikan dan diverifikasi dengan unit test (TrashViewModelTest dengan 10 skenario pengujian komprehensif), verifikasi ktlintCheck lolos tanpa error, dan build debug sukses. Fitur mencakup:
  1. `TrashScreen` (SCR-09) dengan dua tab ("Catatan" dan "Berkas Drive"), banner kebijakan retensi otomatis 30 hari, kartu pratinjau catatan teks/checklist dan berkas Drive, serta empty state bersih.
  2. Dialog konfirmasi destruktif (SCR-11) untuk hapus permanen catatan, hapus permanen berkas, dan kosongkan semua sampah.
  3. Pemulihan catatan ke status `ACTIVE` dan pemulihan berkas Drive ke status `ready` (`trashed=false`).
  4. Hapus permanen catatan dari Firestore serta hapus permanen berkas fisik dari Google Drive, pencabutan referensi `attachmentIds` di semua catatan, dan penghapusan dokumen Firestore.
  5. Pembersihan otomatis (`purgeExpiredTrash`) item sampah > 30 hari pada `NotesRepository` dan `FilesRepository` yang dipicu saat aplikasi dan halaman sampah dibuka.
  6. Navigasi penuh dari `NotesScreen` dan `FilesScreen` ke `TrashScreen` via `SpaceBridgeNavHost`.
- Masukan: Bagian 4.4 PRD dan SCR-09.
- Deskripsi:
  1. Buat `TrashScreen` dengan dua tab: "Catatan" dan "Berkas Drive".
  2. Buat fungsi pemulihan item kembali ke status aktif.
  3. Buat fungsi hapus permanen:
     - Catatan: Menghapus dokumen Firestore.
     - Berkas: Menghapus berkas fisik di Google Drive lalu menghapus dokumen Firestore.
  4. Jalankan fungsi `purgeExpiredTrash()` saat aplikasi dibuka untuk membersihkan item dengan umur sampah > 30 hari.
- Berkas target:
  - `app/src/main/java/com/example/spacebridgeapp/feature/trash/TrashScreen.kt`
  - `app/src/main/java/com/example/spacebridgeapp/feature/trash/TrashViewModel.kt`
- Kriteria selesai: Item yang dihapus permanen terhapus dari Firestore dan Google Drive.

---

#### TASK-402: Halaman Pengaturan Lengkap dan Pembersih Cache (SCR-10) [SELESAI]
- Status: Selesai diimplementasikan dan diverifikasi dengan unit test (SettingsViewModelTest dengan 7 pengujian komprehensif), verifikasi ktlintCheck lolos tanpa error, dan build debug sukses. Fitur mencakup:
  1. `SettingsScreen` (SCR-10) dengan 6 seksi sesuai desain Stitch: Kartu Akun Terhubung, Sinkronisasi Berkas, Penyimpanan Perangkat, Tema Antarmuka, Informasi Aplikasi, dan Zona Bahaya.
  2. Kartu akun menampilkan inisial avatar pengguna, nama, email, badge status Firebase Auth, badge status folder Google Drive, tombol "Ganti Akun", dan tombol "Keluar" dengan dialog konfirmasi.
  3. Sakelar pengaturan "Unggah hanya lewat Wi-Fi" yang tersimpan di DataStore melalui `PreferencesRepository` dan dipatuhi oleh WorkManager uploader.
  4. Indikator status jaringan aktif/offline yang terhubung dengan `NetworkObserver`.
  5. Pengelola cache lokal (`FileCacheManager`) yang menghitung ukuran direktori cache, menampilkan linear progress bar, dan tombol pembersih cache dengan umpan balik visual animasi sukses.
  6. Pemilih tema antarmuka (Segmented pill: Ikuti Sistem, Terang, Gelap) yang tersimpan di DataStore dan otomatis mengubah tema aplikasi di `MainActivity`.
  7. Seksi info aplikasi v3.0 (Build 30) dengan dialog interaktif untuk kebijakan privasi, lisensi open source, dan panduan penggunaan.
  8. Seksi zona bahaya dengan tombol destruktif "Hapus Akun dan Seluruh Data".
  9. Integrasi rute `SettingsRoute` di `SpaceBridgeNavHost`.
- Masukan: Bagian 4.4 PRD dan SCR-10.
- Deskripsi:
  1. Tampilkan informasi akun Google yang aktif.
  2. Buat sakelar pengaturan "Unggah hanya via Wi-Fi" yang tersimpan di DataStore dan otomatis memperbarui constraint WorkManager.
  3. Sediakan tombol "Bersihkan Cache Lokal" yang menghitung dan menghapus berkas pratinjau di direktori cache perangkat.
  4. Sediakan pemilih tema: Ikuti Sistem, Terang, Gelap.
- Berkas target:
  - `app/src/main/java/com/example/spacebridgeapp/feature/settings/SettingsScreen.kt`
  - `app/src/main/java/com/example/spacebridgeapp/feature/settings/SettingsViewModel.kt`
- Kriteria selesai: Pengaturan Wi-Fi ditaati oleh pengunggah berkas dan pembersihan cache mengembalikan ruang penyimpanan HP.

---

#### TASK-403: Alur Hapus Akun Mandiri [SELESAI]
- Status: Selesai diimplementasikan dan diverifikasi dengan unit test (SettingsViewModelTest dengan skenario sukses dan gagal hapus akun), verifikasi ktlintCheck lolos tanpa error, dan build debug sukses. Fitur mencakup:
  1. `DeleteAccountDialog` dengan tampilan kartu modal warm paper, penjelasan peringatan permanen, opsi checkbox "Hapus juga folder SpaceBridge di Google Drive", dan indikator proses penghapusan.
  2. Implementasi `deleteAccount` komprehensif di `AuthRepositoryImpl`:
     - Pemindahan folder `/SpaceBridge` di Google Drive ke Sampah jika opsi dicentang.
     - Pembersihan seluruh dokumen di subkoleksi `users/{uid}/notes`, `users/{uid}/files`, dan `users/{uid}/labels`.
     - Penghapusan dokumen profil `users/{uid}`.
     - Penghapusan akun pengguna di Firebase Authentication (dengan alur otomatis reautentikasi jika diperlukan sesi baru).
     - Pembersihan total sesi lokal, pembatalan antrean WorkManager, pembersihan cache perangkat, dan reset DataStore via `signOut()`.
  3. Integrasi penuh di `SettingsScreen` dan `SettingsViewModel`, serta pengalihan navigasi otomatis ke `LoginRoute` setelah akun berhasil dihapus.
- Masukan: Bagian 4.1 PRD (F-AUTH-5).
- Deskripsi:
  1. Sediakan tombol "Hapus Akun dan Seluruh Data" di bagian bawah pengaturan.
  2. Berikan dialog konfirmasi dengan opsi checkbox: "Hapus juga folder SpaceBridge di Google Drive".
  3. Hapus seluruh catatan dan berkas di Firestore, bersihkan folder Drive jika dicentang, lalu hapus akun Firebase Auth.
- Berkas target:
  - `app/src/main/java/com/example/spacebridgeapp/feature/settings/components/DeleteAccountDialog.kt`
  - `app/src/main/java/com/example/spacebridgeapp/core/data/auth/AuthRepositoryImpl.kt`
  - `app/src/main/java/com/example/spacebridgeapp/feature/settings/SettingsViewModel.kt`
  - `app/src/main/java/com/example/spacebridgeapp/feature/settings/SettingsScreen.kt`
- Kriteria selesai: Akun dan seluruh data pengguna terhapus bersih sesuai regulasi Google Play.

---

### Fase 5: Keamanan, Index Firestore, dan Uji Integrasi

#### TASK-501: Penerapan dan Pengujian Firestore Security Rules [SELESAI]
- Status: Selesai diimplementasikan dan diverifikasi dengan 19 skenario unit test berbasis Firebase Rules Emulator (@firebase/rules-unit-testing), 100% lulus (green), serta verifikasi build Android debug & ktlintCheck bersih. Rincian implementasi:
  1. `firestore.rules` versi v3 sesuai Bagian 8.3 PRD:
     - Isolasi data penuh per `users/{uid}` menggunakan fungsi pembantu `isOwner(uid)`.
     - Validasi skema ketat menggunakan `exactKeys`: profil pengguna (8 field), catatan (12 field), berkas (17 field), label (4 field).
     - Pembatasan muatan catatan (body maks 20.000 karakter, items checklist maks 500 baris).
     - Batasan ukuran berkas artifisial dihapus dari Firestore rules (hanya validasi non-negatif `sizeBytes >= 0`), sehingga ukuran berkas besar seperti 5 GB atau lebih dapat ditampung sesuai sisa kapasitas akun.
     - Proteksi anti-enumerasi (`allow list: if false` pada koleksi `users`).
     - Perlindungan integritas timestamp (`createdAt` tidak dapat diubah setelah dibuat, `updatedAt == request.time`).
     - Koleksi legacy `catatan` hanya dapat dibaca atau dihapus untuk alur migrasi, penulisan baru ditolak seluruhnya.
  2. `firestore.indexes.json` sesuai Bagian 8.4 PRD:
     - Composite index subkoleksi `notes`: status + pinned + updatedAt, status + pinned + title, status + trashedAt.
     - Composite index subkoleksi `files`: status + category + updatedAt, status + trashedAt.
  3. Konfigurasi emulator Firebase (`firebase.json`) dan test suite lengkap (`firestore-tests/test/rules.test.js`):
     - Uji isolasi antar pengguna (Alice tidak dapat membaca, menulis, atau menghapus data Bob).
     - Uji penolakan skema catatan tidak lengkap, tipe tidak valid, status ilegal, dan mutasi ilegal pada `createdAt`.
     - Uji penerimaan berkas berukuran besar (5 GB) dan penolakan `sizeBytes` negatif.
     - Uji penolakan total pengguna tanpa autentikasi (unauthenticated).
     - Uji proteksi daftar pengguna (anti-enumeration) dan penolakan field ilegal pada profil pengguna.
     - Uji validasi label dan pencegahan pembuatan baru pada dokumen legacy `catatan`.
  4. Logika kapasitas dinamis di aplikasi Android (`FilesRepositoryImpl`):
     - Menghitung sisa kapasitas akun Google Drive pengguna secara dinamis melalui `driveRepository.getStorageQuota()`.
     - Menolak pengunggahan berkas sebelum antrean dibuat hanya jika ukuran berkas melebihi sisa kapasitas akun Google Drive pengguna.
- Masukan: Bagian 8.3 PRD.
- Deskripsi:
  1. Terapkan berkas `firestore.rules` terbaru ke Firebase project.
  2. Buat unit test aturan keamanan (Firebase Rules Unit Testing) untuk memastikan:
     - Pengguna A tidak dapat membaca atau menulis path milik Pengguna B.
     - Pembuatan catatan dengan skema tidak lengkap ditolak.
     - Pengguna tidak terautentikasi ditolak seluruhnya.
- Berkas target: `firestore.rules`, `firestore.indexes.json`, `firebase.json`, `firestore-tests/test/rules.test.js`.
- Kriteria selesai: Semua skenario pengujian aturan keamanan berstatus lulus (green).

---

#### TASK-502: Uji coba menyeluruh offline dan sinkronisasi [SELESAI]
- Status: Selesai diimplementasikan dan diverifikasi secara komprehensif melalui unit test siklus hidup offline/online di `NotesViewModelTest` dan `EditorViewModelTest`, verifikasi visual rancangan antarmuka (banner peringatan offline, indikator awan status lokal/cloud), serta build APK debug siap uji fisik. Rincian verifikasi:
  1. Mekanisme mode offline (mode pesawat):
     - `NetworkConnectivityObserver` mendeteksi kehilangan koneksi jaringan secara reaktif via `ConnectivityManager.registerDefaultNetworkCallback`.
     - `OfflineBanner` tampil di bagian atas `NotesScreen` dengan pesan peringatan: "Sedang offline. Perubahan tersimpan di perangkat dan akan disinkronkan saat terhubung."
     - Catatan baru (teks maupun checklist) yang dibuat atau diedit saat offline langsung disimpan ke cache persisten Firestore lokal tanpa debounce tertunda saat keluar, dengan status `EditorSaveStatus.SAVED_LOCAL` (indikator "Di perangkat").
     - Kartu catatan pada `NotesScreen` menampilkan ikon awan oranye dan label "Lokal" ketika `note.hasPendingWrites == true`.
  2. Mekanisme pemulihan online dan sinkronisasi:
     - Saat koneksi jaringan aktif kembali, Firestore client otomatis mengirim antrean penulisan lokal ke backend Firestore (last write wins).
     - Status `saveStatus` di `EditorViewModel` otomatis bertransisi menjadi `EditorSaveStatus.SAVED_CLOUD` ("Tersimpan").
     - Indikator kartu catatan otomatis berubah menjadi ikon centang hijau `CloudDone` ("Tersinkron ke Cloud") saat `hasPendingWrites` berubah menjadi `false`.
     - `OfflineBanner` menghilang otomatis via animasi `fadeOut`.
  3. Pengujian terotomasi:
     - `NotesViewModelTest`: verifikasi transisi online-ke-offline dan offline-ke-online pada `isOnline` state, serta pemetaan dokumen berstatus `hasPendingWrites`.
     - `EditorViewModelTest`: verifikasi pembuatan dan penyuntingan checklist tanpa koneksi internet tanpa kehilangan data, serta pembaruan otomatis status simpan saat jaringan pulih.
- Masukan: Bagian 10 PRD.
- Deskripsi:
  1. Masuk ke mode pesawat (offline):
     - Buat catatan teks baru dan edit checklist.
     - Pastikan banner offline tampil dan catatan ditandai tersimpan lokal.
  2. Matikan mode pesawat (kembali online):
     - Pastikan seluruh perubahan terunggah ke Firestore secara otomatis tanpa ada yang hilang.
- Berkas target: `feature/notes/NotesScreen.kt`, `feature/editor/EditorScreen.kt`, `feature/notes/components/NoteCard.kt`, `feature/notes/NotesViewModelTest.kt`, `feature/editor/EditorViewModelTest.kt`.
- Kriteria selesai: Seluruh data lokal tersinkron sempurna ke cloud saat jaringan aktif kembali.
