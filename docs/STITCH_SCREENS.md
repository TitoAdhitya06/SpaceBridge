# Spesifikasi layar Stitch: SpaceBridge v3

Dokumen ini adalah panduan desain untuk Stitch dan perancang antarmuka. Semua layar mengacu pada aturan visual di `DESIGN.md` dan kebutuhan fungsional di `docs/PRD.md`.

Prinsip desain wajib:
1. Warna dasar krem hangat (`#F8F4EC`), kartu dan lembar putih hangat (`#FFFDF9`), teks coklat gelap (`#2E2A25`), aksen utama oranye (`#BE531A` untuk tombol, `#E2702F` untuk ikon).
2. Dilarang menggunakan gradien, efek blur, glassmorphism, pendaran neon, dan rentang warna ungu atau indigo (hue 240° sampai 320°).
3. Tipografi memakai Plus Jakarta Sans. Semua teks berbahasa Indonesia nyata tanpa teks tiruan.
4. Setiap layar harus memuat state lengkap: konten normal, loading, kosong, dan error.

---

## 1. Daftar layar

| ID Layar | Nama Layar | Kepadatan | Fungsi Utama |
|---|---|---|---|
| SCR-01 | Layar Masuk (Login) | Lapang | Masuk menggunakan akun Google via Credential Manager |
| SCR-02 | Onboarding Google Drive | Lapang | Permintaan izin akses folder SpaceBridge di Google Drive |
| SCR-03 | Beranda Catatan | Padat | Daftar catatan aktif, pencarian, filter, dan status simpan |
| SCR-04 | Editor Catatan Teks | Sedang | Menulis catatan bebas dengan autosave |
| SCR-05 | Editor Daftar Checklist | Sedang | Menulis daftar tugas, progres, dan item selesai |
| SCR-06 | Bottom Sheet Tambah Cepat | Sedang | Menu pilihan cepat saat FAB ditekan |
| SCR-07 | Beranda File Google Drive | Padat | Pengelolaan berkas Drive, status upload, dan filter tipe |
| SCR-08 | Bottom Sheet Tindakan Berkas | Sedang | Tindakan detail berkas, buka, unduh, ganti nama, hapus |
| SCR-09 | Halaman Sampah | Padat | Memulihkan atau menghapus permanen catatan dan berkas |
| SCR-10 | Halaman Pengaturan dan Akun | Sedang | Profil akun, preferensi sinkronisasi, tema, dan hapus akun |
| SCR-11 | Dialog Konfirmasi dan Input | Sedang | Dialog ganti nama, konfirmasi hapus permanen, dan kelola label |

---

## 2. Rincian spesifikasi per layar

### SCR-01: Layar Masuk (Login)

Tujuan: Memberi pintu masuk yang bersih dan aman untuk akun keluarga tanpa form registrasi manual.

- Tata letak: Rata tengah secara vertikal dan horizontal. Lebar kartu konten maksimal 420dp.
- Header:
  - Ikon aplikasi: Kotak solid `surface-muted` dengan sudut membulat `radius-xl` (20dp), memuat simbol dokumen dan awan berwarna `primary-500` (#E2702F). Tanpa pendaran neon.
  - Judul: `display` (36dp / 44dp, weight 700), teks "SpaceBridge".
  - Subjudul: `body` (16dp / 24dp), warna `text-secondary`, teks "Catatan pribadi keluarga dan arsip berkas aman di Google Drive Anda."
- Kontrol utama:
  - Tombol Masuk dengan Google: Tinggi 48dp, lebar penuh, latar `surface`, border 1dp `border-strong` (#D3C7B3), radius `radius-md` (10dp), bayangan `shadow-sm`.
  - Isi tombol: Simbol G resmi di kiri, teks "Lanjutkan dengan Google" (weight 600, warna `text-primary`).
- Catatan privasi:
  - Teks kecil `caption` (13dp) di bawah tombol: "Data catatan tersimpan di partisi pribadi Firebase. Hanya akun Anda yang dapat membaca dan mengubah catatan."
- Varian state:
  - Normal: Tombol aktif siap ditekan.
  - Loading: Tombol menampilkan spinner di tengah, label teks berganti menjadi "Menghubungkan akun...", tombol dinonaktifkan.
  - Error: Banner alert `danger-50` muncul di atas tombol dengan border 1dp `danger-600`, memuat ikon seru dan teks "Gagal masuk. Periksa koneksi internet Anda."

Prompt Stitch:
```text
Mobile app login screen for SpaceBridge personal note app. Clean warm aesthetic, paper-like background #F8F4EC, surface cards #FFFDF9 with subtle border #E8E0D2. Centered layout with clean app icon in rounded container, bold headline "SpaceBridge", descriptive subtitle about private family notes with Google Drive storage. Large single sign-on button "Lanjutkan dengan Google" with clean neutral border and warm text #2E2A25. Solid burnt orange accent #BE531A, no gradients, no glows, no purple, Plus Jakarta Sans typography.
```

---

### SCR-02: Onboarding Google Drive

Tujuan: Meminta izin otorisasi scope `drive.file` secara transparan setelah login pertama.

- Tata letak: Vertikal terstruktur, padding halaman 24dp.
- Konten:
  - Ikon ilustratif sederhana: Lingkaran solid `info-50` (#E8F1FA) dengan ikon folder penyimpanan Drive berwarna `info-600` (#2F6FB0).
  - Judul: `h3` (24dp / 32dp, weight 600), teks "Hubungkan Google Drive".
  - Uraian: `body` (16dp), warna `text-secondary`. "SpaceBridge akan membuat satu folder khusus bernama 'SpaceBridge' di Drive Anda. Aplikasi hanya dapat mengakses berkas yang Anda unggah melalui aplikasi ini, bukan berkas pribadi Anda yang lain."
  - Kotak rincian izin: Latar `surface`, border 1dp `border`, radius `radius-lg` (14dp), padding 16dp. Berisi 3 poin ringkas dengan ikon centang `success-600`:
    1. Menyimpan berkas lampiran dan dokumen secara utuh.
    2. Menggunakan kuota Google Drive akun Anda sendiri.
    3. Hak akses terbatas pada folder aplikasi SpaceBridge.
- Tombol aksi:
  - Tombol Utama: "Hubungkan Drive Sekarang", warna `primary-600` (#BE531A), teks putih `text-on-accent`, tinggi 48dp, radius `radius-md`.
  - Tombol Sekunder: "Lewati untuk Sekarang", tipe ghost button, teks `text-secondary`.
- Varian state:
  - Normal: Dua tombol aktif.
  - Loading: Tombol utama menampilkan spinner saat sistem memproses lembar persetujuan Google.
  - Error: Pesan teks peringatan `danger-600` di atas tombol jika izin ditolak pengguna.

Prompt Stitch:
```text
Clean mobile onboarding screen requesting Google Drive permission. Warm light background #F8F4EC, centered card container #FFFDF9 with 1dp border #E8E0D2. Informative illustration icon in blue tint #E8F1FA, clean title "Hubungkan Google Drive", paragraph explaining minimal scope drive.file limited to SpaceBridge folder. Feature checklist with green checkmarks. Solid primary button "Hubungkan Drive Sekarang" in rust orange #BE531A and ghost button "Lewati untuk Sekarang". Warm, trustworthy, strictly no gradients.
```

---

### SCR-03: Beranda Catatan

Tujuan: Menampilkan ringkasan catatan aktif, pencarian cepat, filter jenis catatan, dan akses ke editor.

- App bar atas:
  - Judul halaman "Catatan", `h3` (24dp, weight 600).
  - Status koneksi di bawah judul: Dot kecil hijau 6dp `success-500` disertai teks `caption` "Tersinkron ke Cloud". Jika offline, dot abu-abu disertai teks "Mode Offline".
  - Aksi kanan: Tombol toggle tampilan (ikon grid 2 kolom / list 1 baris) dan tombol avatar akun pengguna (foto profil bundar 36dp).
- Banner offline (hanya muncul saat internet putus):
  - Latar `warning-50`, border bawah 1dp `warning-500`, padding 10dp horizontal 16dp. Teks `caption` coklat amber: "Sedang offline. Perubahan tersimpan di perangkat dan akan disinkronkan saat terhubung."
- Kolom pencarian:
  - Tinggi 44dp, latar `surface`, border 1dp `border-strong`, radius `radius-md`.
  - Ikon kaca pembesar di kiri, teks placeholder "Cari judul, isi, atau label...", tombol silang di kanan saat ada teks.
- Baris chip filter:
  - Scroll horizontal, jarak antar chip 8dp.
  - Pilihan: "Semua", "Catatan Teks", "Checklist", dilanjutkan dengan label pengguna seperti "Belanja", "Rumah".
  - Chip aktif: Latar `primary-50` (#FDF1E8), border 1dp `primary-500` (#E2702F), teks `primary-700` (weight 600).
  - Chip tidak aktif: Latar `surface`, border 1dp `border`, teks `text-secondary`.
- Ringkasan ruang (kartu mini ringkas):
  - Latar `surface-muted`, border 1dp `border`, radius `radius-md`, padding 12dp.
  - Teks: "12 catatan aktif • 8 tugas checklist selesai".
- Konten utama (Grid 2 Kolom atau List):
  - Bagian "DISEMATKAN" (muncul jika ada catatan bertanda pin): Header huruf kecil tebal `overline`, teks `text-tertiary`.
  - Bagian "LAINNYA": Header `overline`.
  - Kartu catatan:
    - Latar `surface`, border 1dp `border`, radius `radius-lg` (14dp), padding 14dp, bayangan `shadow-sm`.
    - Atas kartu: Ikon pin kecil (jika disematkan), chip kategori teks atau checklist, dan tanggal ubah singkat (misal: "Hari ini 10:15").
    - Judul kartu: `h5` (16dp, weight 600, warna `text-primary`), potong maksimal 2 baris.
    - Isi kartu (mode teks): `body-sm` (14dp, warna `text-secondary`), cuplikan maksimal 4 baris.
    - Isi kartu (mode checklist): Daftar 3 item pertama dengan kotak centang mini, teks tugas yang selesai dicoret tipis, dan badge ringkasan di bawah "3/5 selesai".
    - Indikator status simpan: Titik kecil di sudut kanan bawah kartu. Jika ada perubahan lokal belum terkirim ke server, tampil ikon awan abu-abu kecil "Di perangkat".
- Tombol aksi melayang (FAB):
  - Posisi kanan bawah (atau tengah bar navigasi), bundar 56dp, latar solid `primary-600` (#BE531A), ikon tanda tambah putih 24dp, bayangan `shadow-md`.
- Varian state:
  - Normal: Tampil daftar kartu sesuai filter.
  - Kosong (tanpa catatan sama sekali): Ilustrasi garis memo bersih, judul "Belum ada catatan", teks "Ketuk tombol tambah untuk menulis ide atau daftar belanja pertama Anda."
  - Kosong hasil pencarian: Ikon pencarian kosong, teks "Tidak ditemukan catatan dengan kata kunci tersebut."
  - Loading: Kartu skeleton berlatar `surface-muted` dengan transisi opacity halus (tanpa kilau shimmer).

Prompt Stitch:
```text
Mobile app home screen for note taking app. Warm paper background #F8F4EC, surface cards #FFFDF9 with 1dp border #E8E0D2. Top bar with title "Catatan", green dot online status "Tersinkron ke Cloud", and user avatar. Clean search input with search icon. Filter chips row with active pill in warm terracotta tint #FDF1E8. Masonry 2-column note card layout displaying note title, snippet text, and checklist items with subtle checkboxes. Floating action button in solid burnt orange #BE531A. Clean, high contrast, readable, no gradients.
```

---

### SCR-04: Editor Catatan Teks

Tujuan: Ruang menulis tanpa gangguan dengan jaminan penyimpanan otomatis.

- Baris atas (App bar editor):
  - Tombol kembali (panah kiri) di kiri atas.
  - Indikator status autosave di tengah: Teks `caption` berubah dinamis: "Menyimpan...", "Tersimpan di Cloud", atau "Tersimpan di perangkat".
  - Aksi kanan: Ikon sematkan catatan (bintang atau pin), ikon menu tiga titik (Ubah jadi Checklist, Beri Label, Hapus).
- Bidang input judul:
  - Teks ukuran `h3` (24dp, weight 600, warna `text-primary`), tanpa border dan tanpa latar belakang, placeholder "Judul catatan...".
- Baris metadata:
  - Tanggal terakhir disunting (misal: "21 Oktober 2026, 11:30"), warna `text-tertiary`.
  - Chip label yang terpasang (jika ada), bisa diketuk untuk menambah atau menghapus label.
- Bidang isi catatan:
  - Area teks besar mengisi sisa layar, teks ukuran `body` (16dp / 24dp, warna `text-primary`), placeholder "Tulis catatan di sini...".
- Baris bawah (Toolbar pendukung ringkas):
  - Tinggi 48dp, latar `surface`, border atas 1dp `border`.
  - Tombol lampirkan berkas dari Drive, tombol ubah ke format checklist, jumlah kata atau karakter di kanan.

Prompt Stitch:
```text
Clean mobile note editor screen. Warm cream canvas #F8F4EC, distraction-free writing environment. Top navigation with back arrow, dynamic autosave indicator "Tersimpan di Cloud", pin icon, and overflow menu. Large borderless title input "Judul catatan...", date metadata row, and spacious body textarea with Plus Jakarta Sans typography. Bottom attachment bar with file attachment icon and convert-to-checklist button. Minimal, warm, solid colors, zero gradient.
```

---

### SCR-05: Editor Daftar Checklist

Tujuan: Mengelola daftar tugas dan belanja secara cepat dengan interaksi centang yang responsif.

- Baris atas: Sama dengan SCR-04.
- Bidang input judul: Sama dengan SCR-04.
- Kartu progres checklist:
  - Latar `surface-muted`, radius `radius-md`, padding 12dp.
  - Teks progres: "4 dari 7 selesai" (weight 600, warna `text-primary`).
  - Progress bar solid: Track warna `border`, bar terisi warna `primary-600` (#BE531A), tinggi 6dp, radius 3dp.
- Daftar item checklist aktif:
  - Setiap baris: Kotak centang di kiri (radius 4dp `radius-xs`), bidang teks tugas di tengah (`body`, warna `text-primary`), tombol hapus silang di kanan saat baris aktif.
  - Menekan tombol Enter pada keyboard langsung membuat baris checklist baru di bawahnya.
- Baris tambah tugas baru cepat:
  - Ikon tanda tambah oranye `primary-500`, teks placeholder "Tulis tugas baru...", tombol aksi cepat "Tambah".
- Bagian item yang selesai (dapat dilipat):
  - Header pemisah yang bisa diklik: "Selesai (4 item)", disertai ikon panah buka-tutup.
  - Item di dalam bagian ini: Kotak centang aktif terisi `primary-600`, teks tugas abu-abu `text-secondary` dengan garis coret (`line-through`).

Prompt Stitch:
```text
Mobile interactive checklist editor screen. Warm paper theme #F8F4EC, surface cards #FFFDF9. Clean progress card showing "4 dari 7 selesai" with a solid orange progress bar. List of interactive todo items with clean square checkboxes and inline text fields. Quick-add task input row at the bottom of active tasks. Collapsible section for completed tasks with strike-through text. Functional, tidy, clear touch targets of 48dp, no gradients.
```

---

### SCR-06: Bottom Sheet Tambah Cepat

Tujuan: Pilihan terarah saat tombol aksi melayang ditekan di beranda.

- Lembar modal: Muncul dari bawah, latar `surface`, radius atas `radius-xl` (20dp), border atas 1dp `border`, bayangan `shadow-lg`.
- Drag handle: Garis tumpul 36dp x 4dp warna `border-strong` di tengah atas.
- Judul sheet: `h5` (16dp, weight 600), teks "Buat Baru".
- Pilihan menu (3 baris tombol kartu):
  1. "Catatan Teks": Ikon dokumen oranye, judul "Catatan Biasa", keterangan "Tulis ide, rangkuman, atau draft bebas".
  2. "Daftar Checklist": Ikon kotak centang hijau, judul "Daftar Tugas / Belanja", keterangan "Daftar checklist dengan pemantau progres".
  3. "Unggah Berkas ke Drive": Ikon awan biru, judul "Unggah Berkas", keterangan "Simpan dokumen, gambar, atau berkas ke Google Drive".
- Setiap baris memiliki tinggi 60dp, berlatar transparan dengan efek hover/press `surface-hover`, berpemisah garis tipis `border`.

Prompt Stitch:
```text
Mobile modal bottom sheet for quick action creation. Warm cream surface #FFFDF9, rounded top corners 20dp, subtle drag handle. Three menu action rows: "Catatan Biasa" with document icon, "Daftar Tugas / Belanja" with checklist icon, and "Unggah Berkas" with cloud upload icon. Each row has clear bold title and secondary description. High clarity, warm accents, no gradients.
```

---

### SCR-07: Beranda File Google Drive

Tujuan: Mengelola berkas yang diunggah ke folder `SpaceBridge` di Google Drive pengguna.

- App bar atas:
  - Judul "Berkas Drive", subteks akun Google yang aktif (misal: "tito***@gmail.com").
  - Tombol sinkronisasi manual (refresh) dan tombol avatar akun.
- Kartu kuota dan status penyimpanan:
  - Latar `surface-muted`, border 1dp `border`, radius `radius-lg` (14dp), padding 16dp.
  - Baris atas: Ikon folder Google Drive warna `info-600` (#2F6FB0), teks "Folder Drive: /SpaceBridge", badge status hijau "Terhubung".
  - Baris ringkasan: "18 berkas tersimpan • Total ukuran 48.2 MB".
- Kolom pencarian berkas:
  - Pencarian berdasarkan nama berkas.
- Baris filter tipe berkas:
  - Chip: "Semua", "Dokumen", "Gambar", "Spreadsheet", "Lainnya".
- Banner antrean unggahan (muncul jika ada berkas yang sedang diunggah):
  - Latar `info-50`, border 1dp `info-600`, radius `radius-md`, padding 12dp.
  - Teks: "Mengunggah Laporan_Keuangan.pdf (4.2 MB) • 65%". Progress bar biru tipis dan tombol silang untuk membatalkan.
- Daftar berkas (LazyColumn berjarak 8dp):
  - Kartu berkas:
    - Latar `surface`, border 1dp `border`, radius `radius-md` (10dp), padding 12dp.
    - Ikon tipe berkas solid di kiri (merah untuk PDF, hijau untuk spreadsheet, biru untuk docx, oranye untuk gambar).
    - Kolom tengah: Nama berkas tebal (`h5` 15dp), baris meta di bawahnya berisi ukuran berkas ("2.4 MB"), tanggal unggah ("Kemarin 14:20"), dan badge kecil "Drive".
    - Kolom kanan: Tombol aksi titik tiga vertikal untuk membuka menu SCR-08.
- Varian state:
  - Normal: Daftar berkas tertata rapi.
  - Kosong: Ikon folder kosong, judul "Belum ada berkas di Drive", teks "Unggah berkas pertama Anda untuk disimpan dengan aman di Google Drive akun Anda."
  - Error otorisasi: Banner peringatan amber "Akses Google Drive perlu disambungkan ulang" dengan tombol "Sambungkan".

Prompt Stitch:
```text
Mobile Google Drive files management screen. Warm theme #F8F4EC, surface cards #FFFDF9. Storage summary card displaying SpaceBridge folder status, file count, and total used storage. Filter chip carousel for document types. Clean file list items showing distinct colored document icons (PDF red, Excel green, Images orange), file name, size, upload date, and overflow action buttons. Active upload banner showing progress bar. Crisp, utilitarian, warm palette, zero gradient.
```

---

### SCR-08: Bottom Sheet Tindakan Berkas

Tujuan: Memberikan aksi lengkap terhadap satu berkas Drive terpilih.

- Lembar modal bawah: Latar `surface`, radius atas `radius-xl`.
- Header berkas:
  - Ikon format berkas 40dp, nama berkas lengkap (bisa 2 baris), ukuran berkas, dan tanggal.
  - Badge hijau "Tersimpan di Google Drive".
- Daftar tindakan:
  1. "Buka Berkas": Membuka salinan lokal di aplikasi pembaca bawaan Android.
  2. "Buka di Google Drive": Membuka berkas langsung di aplikasi Drive atau browser web.
  3. "Simpan Salinan ke Download": Mengunduh berkas ke folder Download perangkat.
  4. "Ganti Nama": Membuka dialog ubah nama berkas.
  5. "Bagikan Tautan": Membuat tautan publik Drive dengan konfirmasi.
  6. "Pindahkan ke Sampah": Aksi destruktif dengan teks merah `danger-600` dan latar hover `danger-50`.

Prompt Stitch:
```text
Mobile file actions bottom sheet. Warm cream surface #FFFDF9, rounded top 20dp. Header card previewing file name, file type icon, size, and Google Drive status badge. Vertical menu list with clear icons and labels: "Buka Berkas", "Buka di Google Drive", "Simpan ke Download", "Ganti Nama", "Bagikan Tautan", and red destructive button "Pindahkan ke Sampah". High contrast, clean spacing, no gradients.
```

---

### SCR-09: Halaman Sampah

Tujuan: Mengelola berkas dan catatan yang dihapus sementara, serta membersihkan data permanen.

- App bar atas:
  - Tombol kembali di kiri, judul "Sampah".
  - Aksi kanan: Tombol teks merah "Kosongkan Sampah" (hanya aktif bila ada item).
- Banner informasi retensi:
  - Latar `surface-muted`, border 1dp `border`, padding 12dp.
  - Teks `caption`: "Item di Sampah akan dihapus otomatis secara permanen setelah 30 hari."
- Tab navigasi atas (2 tab):
  - "Catatan (3)" dan "Berkas Drive (2)".
  - Tab aktif bergaris bawah tebal `primary-600`.
- Konten tab Catatan / Berkas:
  - Daftar kartu item yang terhapus.
  - Pada tiap kartu terdapat keterangan "Dihapus pada 19 Okt 2026".
  - Dua tombol aksi di tiap baris: Tombol sekunder "Pulihkan" dan tombol ghost merah "Hapus Permanen".
- State kosong:
  - Ikon tempat sampah bersih, teks "Kotak Sampah Kosong".

Prompt Stitch:
```text
Mobile trash management screen with tabbed layout. Warm paper background #F8F4EC, cards #FFFDF9. Top info banner explaining 30-day auto-purge policy. Two tabs: "Catatan" and "Berkas Drive". Card list of deleted items showing item title, deletion date, "Pulihkan" restore button, and red "Hapus Permanen" action. Top right destructive action "Kosongkan Sampah". Clean, solid styling, no gradients.
```

---

### SCR-10: Halaman Pengaturan dan Akun

Tujuan: Memeriksa akun, mengelola kuota, mengatur jaringan sinkronisasi, dan memilih tema.

- App bar atas:
  - Tombol kembali dan judul "Pengaturan".
- Seksi 1: Akun Terhubung (Kartu padat):
  - Foto avatar pengguna, nama lengkap, dan email akun Google.
  - Status koneksi Firebase Auth dan Google Drive.
  - Tombol sekunder: "Ganti Akun" atau "Keluar".
- Seksi 2: Sinkronisasi dan Jaringan:
  - Sakelar (Switch): "Unggah hanya lewat Wi-Fi". Keterangan di bawahnya: "Menunda unggahan berkas besar saat menggunakan data seluler."
  - Sakelar aktif berwarna `primary-600` (#BE531A).
- Seksi 3: Penyimpanan dan Cache:
  - Info ukuran cache pratinjau lokal (misal: "Cache saat ini: 24.5 MB").
  - Tombol sekunder: "Bersihkan Cache Lokal".
- Seksi 4: Tampilan:
  - Pilihan tema antarmuka: Tiga tombol radio atau segmented pill: "Ikuti Sistem", "Terang", "Gelap".
- Seksi 5: Informasi Aplikasi:
  - Nama aplikasi: SpaceBridge v3.0 (Build 30).
  - Tautan: Kebijakan Privasi dan Lisensi Terbuka.
- Seksi 6: Zona Bahaya (Kartu berborder tipis merah):
  - Tombol teks merah `danger-600`: "Hapus Akun dan Seluruh Data".
  - Membuka alur konfirmasi bertingkat untuk menghapus data Firestore dan opsi membersihkan folder SpaceBridge di Google Drive.

Prompt Stitch:
```text
Mobile settings screen for SpaceBridge. Warm cream background #F8F4EC, surface cards #FFFDF9 with 1dp border #E8E0D2. User profile card with Google avatar, name, and email. Toggle setting for "Unggah hanya lewat Wi-Fi" with warm orange toggle switch. Cache storage clean-up button. Theme selection pills (Ikuti Sistem, Terang, Gelap). Bottom danger zone section with red outline and destructive button "Hapus Akun dan Seluruh Data". Utilitarian, accessible, no gradients.
```

---

### SCR-11: Dialog Konfirmasi dan Input

Tujuan: Dialog standar sistem untuk interaksi kritis.

1. Dialog Konfirmasi Hapus Catatan:
   - Judul: "Pindahkan ke Sampah?".
   - Pesan: "Catatan ini akan dipindahkan ke Sampah dan dapat dipulihkan dalam waktu 30 hari."
   - Tombol: "Batal" (Secondary) dan "Pindahkan ke Sampah" (Danger merah).

2. Dialog Ganti Nama Berkas:
   - Judul: "Ganti Nama Berkas".
   - Input teks dengan nama berkas saat ini terpilih, validasi ekstensi berkas tidak boleh dihilangkan.
   - Tombol: "Batal" dan "Simpan Nama".

3. Dialog Konfirmasi Bagikan Tautan:
   - Judul: "Bagikan Tautan Berkas?".
   - Pesan: "Tautan ini dapat diakses oleh siapa saja yang memiliki tautan melalui Google Drive Anda."
   - Tombol: "Batal" dan "Buat Tautan Publik".

---

## 3. Matriks panduan token warna Stitch

Gunakan padanan warna berikut saat mengonfigurasi komponen di Stitch:

| Komponen | Token Desain | Nilai Hex |
|---|---|---|
| Latar belakang layar | `bg` | `#F8F4EC` (Krem hangat) |
| Permukaan kartu, modal, input | `surface` | `#FFFDF9` (Putih gading) |
| Latar section sekunder & header | `surface-muted` | `#F2ECE1` (Krem lembut) |
| Garis batas kartu & pemisah | `border` | `#E8E0D2` |
| Garis batas input aktif | `border-strong` | `#D3C7B3` |
| Teks judul & teks utama | `text-primary` | `#2E2A25` (Coklat arang) |
| Teks deskripsi & label | `text-secondary` | `#5C554C` |
| Teks meta & placeholder | `text-tertiary` | `#756D61` |
| Tombol utama default | `primary-600` | `#BE531A` (Oranye bata) |
| Ikon aksen & status sorotan | `primary-500` | `#E2702F` |
| Latar chip aktif | `primary-50` | `#FDF1E8` |
| Status tersinkron & sukses | `success-600` | `#27774A` (Hijau daun) |
| Status info & folder Drive | `info-600` | `#2F6FB0` (Biru klasik) |
| Aksi destruktif & peringatan bahaya | `danger-600` | `#C23B32` (Merah hangat) |
| Peringatan offline & penundaan | `warning-500` | `#E9A23B` (Amber) |
