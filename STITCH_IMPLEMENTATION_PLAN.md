# Panduan Implementasi UI SpaceBridge (Stitch ke Jetpack Compose)

Dokumen ini menjadi acuan untuk menerjemahkan desain dari Stitch (Project ID: `13707136946999117000`) ke dalam kode Jetpack Compose di Android Studio.

---

## 1. Fondasi Arsitektur Data

1. **Autentikasi:** Menggunakan Google Sign-In via Credential Manager dan Firebase Auth. Sesi login bersifat tunggal per perangkat.
2. **Catatan (Teks & Checklist):** Disimpan di Cloud Firestore pada path `users/{uid}/catatan`. Menggunakan snapshot listener realtime.
3. **Berkas (File):** File fisik diunggah ke Google Drive pengguna menggunakan scope aman `drive.file`. Metadata berkas (nama, ukuran, MIME type, `driveFileId`, waktu) disimpan di Firestore pada path `users/{uid}/files`.
4. **Resiliensi Jaringan:** Firestore offline cache aktif secara default. Pengguna tetap bisa membaca dan menulis saat internet putus, lalu data otomatis tersinkron saat koneksi kembali.

---

## 2. Token Desain & Tema (Dark Cosmic)

Gunakan token berikut untuk memperbarui `ui/theme`:

- **Warna Utama (Primary):** `#C0C1FF` (Aksen tombol, teks primer aktif)
- **Container Primer:** `#6366F1` / `#8083FF` (Gradien pendaran tombol aksi)
- **Warna Sekunder (Cyan):** `#4CD7F6` / `#06B6D4` (Indikator sinkronisasi, status online)
- **Warna Tersier (Violet):** `#D0BCFF` / `#8B5CF6` (Aksen tag, folder)
- **Latar Belakang (Surface Base):** `#0F131D` (Kanvas utama)
- **Permukaan Kartu (Surface Container):** `#171B26` hingga `#1C1F2A`
- **Border Tipis:** `rgba(255, 255, 255, 0.07)`
- **Tipografi:** 
  - Judul / Headline: Space Grotesk (atau font sans tegas setara)
  - Teks Isi / Kontrol: Plus Jakarta Sans (atau Roboto)
- **Radius Lengkungan:**
  - Kartu & Kontainer: 16dp (`RoundedCornerShape(16.dp)`)
  - Tombol & Field Input: 8dp - 12dp
  - Chip, Badge, & FAB: Bentuk kapsul penuh (`RoundedCornerShape(50)`)

---

## 3. Pemetaan Layar (Apa yang Diadopsi vs Diabaikan)

### A. Layar Masuk (Login)
* **Yang diadopsi dari Stitch:**
  - Logo kosmik di bagian atas.
  - Judul "SpaceBridge" dan penjelasan singkat fungsi aplikasi.
  - Tombol utama "Masuk dengan Google".
* **Yang DIABAIKAN dari Stitch:**
  - Form input email dan kata sandi.
  - Tombol "Daftar", "Lupa Kata Sandi", dan layar reset OTP. Aplikasi ini khusus keluarga dan hanya memakai akun Google.

### B. Halaman Utama Catatan (Home)
* **Yang diadopsi dari Stitch:**
  - Status koneksi di app bar: dot cyan dengan teks "Terhubung ke Cloud" (atau "Mode Offline" saat terputus).
  - Kartu Ringkasan Ruang: jumlah catatan aktif, checklist selesai, dan waktu sinkron terakhir.
  - Input pencarian melayang dengan ikon search.
  - Filter chip kategori (Semua, Catatan Teks, Checklist).
  - Desain kartu catatan bertingkat (staggered) dengan badge kategori (#Kerja, #Pribadi).
* **Catatan teknis:**
  - Data kartu diambil langsung dari snapshot Firestore `users/{uid}/catatan`.

### C. Editor Catatan
* **Yang diadopsi dari Stitch:**
  - Status bar atas: teks "Tersimpan otomatis di Cloud".
  - Toggle tab jenis catatan: Catatan Teks atau Daftar Checklist.
  - Bidang input judul tebal dan textarea isi catatan transparan.
  - Checklist interaktif dengan kotak centang bergaya modern.
* **Yang DIABAIKAN dari Stitch:**
  - Teks dummy tentang "SQLite terenkripsi" dan "migrasi JSON snapshot".

### D. Halaman File (Google Drive)
* **Yang diadopsi dari Stitch:**
  - Kartu ringkasan kuota Google Drive pengguna (progres bar terpakai vs total kapasitas).
  - Tombol utama "Unggah Berkas ke Drive".
  - Daftar berkas dengan ikon tipe dokumen, ukuran file, tanggal, dan badge status "Tersinkron".
  - Tombol aksi cepat unduh dan bagikan tautan pada tiap kartu berkas.
* **Catatan teknis:**
  - Daftar ini membaca metadata dari `users/{uid}/files` di Firestore.
  - Mengetuk berkas akan membuka atau mengunduh langsung dari Google Drive via API.

### E. Dialog Profil & Akun
* **Yang diadopsi dari Stitch:**
  - Foto avatar Google, nama akun, dan alamat email.
  - Status badge: "Google Auth Aktif" dan "Firebase Cloud Sync: Terhubung".
  - Tombol aksi berwarna merah: "Keluar Akun (Sign Out)".
* **Yang DIABAIKAN dari Stitch:**
  - Daftar radio button alih akun (switch account). Pengguna keluar dulu jika ingin berganti akun Google.

### F. Halaman Pengaturan (Settings)
* **Yang diadopsi dari Stitch:**
  - Seksi Akun & Cloud: info profil aktif.
  - Pengaturan jaringan: sakelar "Sinkronkan hanya lewat Wi-Fi" (untuk file berukuran besar).
  - Seksi penyimpanan: pembersih cache preview lokal.
  - Pilihan tema: Mode Gelap / Terang.
* **Yang DIABAIKAN dari Stitch:**
  - Tombol manual "Sinkronkan Ulang dengan Cloud".
  - Opsi mengubah folder root Drive secara bebas. Folder penyimpanan di Drive dibuat otomatis bernama `SpaceBridge`.

### G. Dialog Konfirmasi Hapus Catatan
* **Diadopsi dari Stitch (`2d9caab91de845bf8ce54a9639e3277e`):**
  - Ikon peringatan kosmik merah dengan badge orbit "PERINGATAN".
  - Judul "Hapus Catatan Ini?".
  - Kartu ringkasan preview catatan yang akan dihapus (kategori, status cloud, checklist summary).
  - Opsi checkbox "Hapus lampiran dari Google Drive".
  - Tombol aksi destruktif merah "Pindahkan ke Sampah" dan tombol sekunder "Batal".

### H. Menu Tindakan & Kelola File Drive (Modal Bottom Sheet)
* **Diadopsi dari Stitch (`db6eb3c82c0e464db80d0c2461d4a4d1`):**
  - Drag handle bar di bagian atas sheet.
  - Kartu header berkas dengan badge format (PDF, IMG, XLS), ukuran, tanggal, dan status "Terhubung ke Drive".
  - Daftar tindakan: Buka di Drive, Ganti Nama Berkas, Bagikan Tautan Aman, dan Unduh Salinan Offline.
  - Seksi destruktif dengan kotak peringatan merah dan tombol "Hapus Berkas Ini" untuk menghapus berkas.

### I. Floating Bottom Navigation Bar
* **Diadopsi dari Stitch:**
  - Bar melayang di bagian bawah dengan radius melengkung dan efek glassmorphic.
  - Tab kiri untuk Catatan dan tab kanan untuk File Drive.
  - Tombol elevated circular FAB bergradien di tengah (membuat catatan baru atau mengunggah berkas).

---

## 4. Status Implementasi

1. **Langkah 1 (Theme & Token):** Palet Dark Cosmic selesai (`Color.kt` & `Theme.kt`).
2. **Langkah 2 (Komponen Stitch):** Reusable components selesai di [Components.kt](file:///c:/Users/titoa/AndroidStudioProjects/SpaceBridgeApp/app/src/main/java/com/example/spacebridgeapp/Components.kt).
3. **Langkah 3 (Alur Penghapusan & Menu Tindakan):** Dialog konfirmasi hapus catatan dan modal bottom sheet tindakan file selesai diimplementasikan.
4. **Langkah 4 (Halaman & Editor Catatan):** Layout Stitch Catatan dan EditorCatatanScreen hibrida/checklist selesai di [MainActivity.kt](file:///c:/Users/titoa/AndroidStudioProjects/SpaceBridgeApp/app/src/main/java/com/example/spacebridgeapp/MainActivity.kt).
5. **Langkah 5 (Halaman File & Pengaturan):** Meter storage Drive, filter, upload banner, dan dialog pengaturan sesuai Stitch selesai.
