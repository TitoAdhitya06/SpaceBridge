# SpaceBridge

SpaceBridge adalah aplikasi Android modern berbasis Jetpack Compose yang mengintegrasikan manajemen catatan (teks dan checklist) dengan sinkronisasi penyimpanan awan Google Drive dan Cloud Firestore. Aplikasi ini dirancang dengan prinsip *offline-first*, antarmuka adaptif untuk perangkat ponsel hingga tablet, serta sistem tema ganda (Light dan Dark Mode).

---

## Fitur Utama

- **Manajemen Catatan Hibrida**: Mendukung catatan teks naratif dan daftar tugas interaktif (*checklist*) dengan indikator progres penyelesaian.
- **Katalog dan Sinkronisasi Berkas Google Drive**: Mengelola berkas yang diunggah ke Google Drive dalam ruang folder `/SpaceBridge`, pemantauan kuota penyimpanan, serta opsi unduh lokal, bagikan, dan hapus berkas.
- **Unggah Latar Belakang Tangguh**: Menggunakan AndroidX WorkManager dengan metode *chunked resumable upload* (8 MB per bagian) sehingga proses unggah dapat dilanjutkan kembali jika koneksi terputus.
- **Offline-First dan Realtime Sync**: Menggunakan Cloud Firestore dengan *offline persistence*. Perubahan data saat offline akan otomatis disinkronkan ke server begitu perangkat terhubung kembali ke jaringan.
- **Antarmuka Responsif dan Adaptif**: Layout otomatis menyesuaikan orientasi potret, lanskap, maupun layar lebar/tablet dengan pilihan tampilan kisi bertingkat (*staggered grid*) atau daftar baris (*list view*).
- **Sistem Tema Warm Paper & Charcoal**: Mendukung mode terang (*warm paper*) dan mode gelap (*warm charcoal*) yang dapat mengikuti setelan sistem operasi atau dipilih secara manual.
- **Otentikasi Aman**: Masuk menggunakan akun Google via Firebase Authentication dan otorisasi bertahap (*incremental authorization*) untuk cakupan `drive.file`.

---

## Arsitektur dan Teknologi

Proyek ini dibangun menggunakan pendekatan **Clean Architecture** dan pola desain **Model-View-ViewModel (MVVM)** dengan *Unidirectional Data Flow* (UDF).

| Komponen | Teknologi |
|---|---|
| **Bahasa Pemrograman** | Kotlin 2.2.10 |
| **UI Toolkit** | Jetpack Compose (Compose BOM 2026.02.01) |
| **Design System** | Material 3 dengan custom design tokens |
| **Navigasi** | Jetpack Navigation Compose (Type-Safe Routes) |
| **Basis Data Cloud** | Firebase Cloud Firestore (Offline Cache Enabled) |
| **Otentikasi** | Firebase Auth & Google Identity Services |
| **API Cloud Storage** | Google Drive REST API v3 (Resumable Upload via OkHttp / Retrofit) |
| **Pekerjaan Latar Belakang** | AndroidX WorkManager (CoroutineWorker & Foreground Notification) |
| **Penyimpanan Preferensi** | Jetpack DataStore Preferences |
| **Injeksi Ketergantungan** | Manual Dependency Container (`SpaceBridgeContainer`) |

---

## Struktur Direktori

```text
app/src/main/java/com/example/spacebridgeapp/
├── core/
│   ├── data/
│   │   ├── auth/          # Implementasi otentikasi Firebase & Google
│   │   ├── drive/         # Layanan otorisasi dan token Google Drive
│   │   ├── files/         # Repositori dan DTO katalog berkas Drive
│   │   ├── network/       # Observer konektivitas jaringan realtime
│   │   ├── notes/         # Repositori Firestore untuk catatan dan checklist
│   │   └── prefs/         # Pengelolaan preferensi DataStore
│   ├── di/                # AppContainer dan penyedia dependensi aplikasi
│   ├── model/             # Model domain (Note, DriveFile, User, ChecklistItem)
│   ├── util/              # Utilitas format tanggal, ukuran berkas, dan MIME type
│   └── work/              # UploadWorker untuk upload berkas Google Drive bertahap
├── feature/
│   ├── auth/              # Layanan masuk (Login) dan onboarding Google Drive
│   ├── editor/            # Editor catatan, blok checklist, dan toolbar format
│   ├── files/             # Layanan penjelajah berkas Drive dan kartu kuota
│   ├── notes/             # Daftar catatan utama (Grid/List) dan bilah pencarian
│   ├── settings/          # Pengaturan tema, akun, dan pembersihan data
│   └── trash/             # Pengelolaan tempat sampah catatan yang dihapus
└── ui/
    ├── components/        # Komponen UI bersama (Floating Bottom Bar, Chips, dsb.)
    ├── navigation/        # SpaceBridgeNavHost dan definisi rute tipe aman
    └── theme/             # Token warna, tipografi, dan tema aplikasi
```

---

## Persiapan dan Konfigurasi Lingkungan

Sebelum menjalankan atau membangun aplikasi, beberapa konfigurasi kredensial perlu disiapkan karena berkas kredensial privat tidak disertakan pada repositori publik.

### 1. Prasyarat Sistem
- Android Studio (versi Ladybug / Koala atau lebih baru)
- JDK 17 atau JDK 21
- Android SDK dengan `minSdk 26` dan `targetSdk 35`

### 2. Konfigurasi Firebase
1. Buka [Firebase Console](https://console.firebase.google.com/) dan buat proyek baru.
2. Tambahkan aplikasi Android dengan nama paket: `com.example.spacebridgeapp`.
3. Daftarkan sidik jari sertifikat SHA-1 *debug* dan *release* mesin Anda ke proyek Firebase.
4. Aktifkan **Authentication** dengan metode masuk **Google**.
5. Aktifkan **Cloud Firestore** dan terapkan aturan keamanan yang disediakan pada berkas `firestore.rules`.
6. Unduh berkas `google-services.json` dari pengaturan proyek Firebase.
7. Letakkan berkas tersebut ke dalam direktori `app/google-services.json`. Template struktur dapat dilihat pada `app/google-services.json.example`.

### 3. Konfigurasi Google Cloud Console (Drive API)
1. Buka [Google Cloud Console](https://console.cloud.google.com/) untuk proyek yang terhubung dengan Firebase Anda.
2. Masuk ke menu **APIs & Services** > **Library**, lalu aktifkan **Google Drive API**.
3. Di menu **OAuth consent screen**, tambahkan cakupan (*scope*):
   - `https://www.googleapis.com/auth/drive.file`
4. Tambahkan email penguji (*Test Users*) jika status consent screen masih dalam tahap pengujian (*Testing*).

---

## Kompilasi dan Menjalankan Aplikasi

Jalankan perintah Gradle berikut melalui terminal pada direktori utama proyek:

```bash
# Memeriksa kompilasi kode Kotlin
./gradlew compileDebugKotlin

# Membangun file APK versi debug
./gradlew assembleDebug

# Memasang langsung ke perangkat fisik atau emulator yang terhubung
./gradlew installDebug
```

---

## Pengujian Keamanan Firestore

Aturan keamanan Firestore telah diuji secara komprehensif menggunakan Firebase Rules Unit Testing SDK. Untuk menjalankan pengujian keamanan lokal:

```bash
cd firestore-tests
npm test
```

---

## Lisensi

Proyek ini dilisensikan di bawah lisensi terbuka untuk keperluan pengembangan dan portofolio.
