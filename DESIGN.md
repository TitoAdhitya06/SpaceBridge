# DESIGN.md

Acuan visual pribadi untuk proyek web dan mobile. Palet di bawah adalah titik awal, bukan harga mati. Warna lain boleh dipakai kalau bagian tertentu memang butuh, asal tetap solid, hangat atau netral, lolos kontras, dan tidak melanggar guardrail di bagian 7.

---

## 0. Cara memakai dokumen ini

Berlaku untuk manusia dan AI agent yang membangun UI.

1. Semua aturan di sini wajib, kecuali pengguna meminta lain secara eksplisit. Kalau proyek sudah punya design system sendiri, ikuti itu dan pakai dokumen ini untuk mengisi yang belum diatur.
2. Pakai token saja. Jangan menulis hex, spasi, radius, atau durasi di luar token. Palet bawaan framework seperti Tailwind `slate`, `indigo`, `violet`, tema default shadcn, atau Material purple tidak boleh dipakai mentah. Petakan dulu ke token di sini.
3. Mulai dari isi. Tentukan tugas utama halaman, siapa penggunanya, dan data apa yang tampil. Layout dan dekorasi menyusul.
4. Pakai konten realistis sesuai domain proyek. Tidak ada lorem ipsum, "John Doe", "Acme Inc", atau angka statistik karangan.
5. Setiap layar punya state lengkap: loading, kosong, error, dan sukses.
6. Sebelum menyerahkan hasil, jalankan audit di akhir bagian 7. Satu poin gagal berarti belum selesai.
7. Kalau ragu antara dua pilihan, ambil yang lebih sederhana dan lebih mudah dibaca.

---

## 1. Suasana visual

- Hangat, tenang, rapi. Rasanya seperti kertas krem di meja kayu, bukan layar putih yang dingin.
- Bersih tapi punya karakter. Karakternya datang dari warna hangat, tipografi tegas, sudut membulat, bayangan tipis, dan animasi halus.
- Satu fokus per layar. Mata langsung tahu elemen mana yang paling penting.
- Kepadatan mengikuti jenis halaman. Lihat bagian 8.

---

## 2. Palet warna

Semua pasangan teks dan latar di bawah sudah dicek minimal WCAG AA.

### Dasar

| Token | Hex | Fungsi |
|---|---|---|
| `bg` | `#F8F4EC` | Latar utama halaman (krem hangat) |
| `surface` | `#FFFDF9` | Kartu, modal, input, dropdown |
| `surface-muted` | `#F2ECE1` | Section sekunder, header tabel, area tenggelam |
| `surface-hover` | `#F5EFE4` | Hover baris tabel dan item daftar |
| `border` | `#E8E0D2` | Outline kartu, garis pemisah |
| `border-strong` | `#D3C7B3` | Border input, divider tegas |
| `overlay` | `rgba(46,42,37,0.45)` | Latar di belakang modal |

### Teks

| Token | Hex | Fungsi | Kontras di `bg` |
|---|---|---|---|
| `text-primary` | `#2E2A25` | Judul, isi utama | 13:1 |
| `text-secondary` | `#5C554C` | Deskripsi, label | 6.7:1 |
| `text-tertiary` | `#756D61` | Meta, caption, placeholder | 4.6:1 |
| `text-disabled` | `#A69E90` | Teks nonaktif | dekoratif |
| `text-on-accent` | `#FFFDF9` | Teks di atas tombol berwarna | |

### Aksen utama, oranye

| Token | Hex | Fungsi |
|---|---|---|
| `primary-50` | `#FDF1E8` | Latar tint, item navigasi aktif |
| `primary-100` | `#FADDC6` | Highlight, badge lembut |
| `primary-500` | `#E2702F` | Ikon, indikator, angka besar, ilustrasi |
| `primary-600` | `#BE531A` | **Tombol utama default** |
| `primary-700` | `#A54715` | Tombol utama hover, teks aktif |
| `primary-800` | `#8C3C12` | Tombol utama pressed |
| `focus-ring` | `rgba(226,112,47,0.35)` | Cincin fokus semua elemen |

`primary-500` dengan teks putih hanya ~3:1, terlalu rendah untuk label tombol. Karena itu tombol pakai `primary-600` (4.6:1), sementara `primary-500` dipakai untuk elemen non-teks.

### Info, biru

Hue biru dijaga di 195° sampai 215° supaya tidak bergeser ke indigo.

| Token | Hex | Fungsi |
|---|---|---|
| `info-50` | `#E8F1FA` | Latar alert info |
| `info-500` | `#4A8AC9` | Ikon info |
| `info-600` | `#2F6FB0` | Tautan, teks info, tombol info |
| `info-700` | `#255A91` | Hover |
| `info-800` | `#1D4A78` | Pressed |

### Sukses dan aktif, hijau

| Token | Hex | Fungsi |
|---|---|---|
| `success-50` | `#E6F4EC` | Latar alert sukses, badge aktif |
| `success-500` | `#3A9B63` | Dot status online/aktif, ikon |
| `success-600` | `#27774A` | Teks sukses, tombol konfirmasi |
| `success-700` | `#1F6340` | Hover |
| `success-800` | `#18502F` | Pressed |

### Bahaya, merah

| Token | Hex | Fungsi |
|---|---|---|
| `danger-50` | `#FBEAE8` | Latar alert error |
| `danger-500` | `#D9534A` | Ikon error |
| `danger-600` | `#C23B32` | Tombol hapus, teks error, border input error |
| `danger-700` | `#A5302A` | Hover |
| `danger-800` | `#8A2722` | Pressed |

### Peringatan, amber

| Token | Hex | Fungsi |
|---|---|---|
| `warning-50` | `#FDF4E3` | Latar alert peringatan |
| `warning-500` | `#E9A23B` | Ikon, badge (pakai teks gelap) |
| `warning-700` | `#8F5D12` | Teks peringatan |

Amber sengaja lebih kuning dari oranye utama supaya tidak tertukar dengan tombol. Selalu sertakan ikon.

### Palet chart

Urutan pemakaian untuk grafik di dashboard. Tanpa ungu.

`#E2702F` oranye, `#2F6FB0` biru, `#3A9B63` hijau, `#E9A23B` amber, `#2A9D8F` teal, `#C23B32` merah bata, `#7FB3E0` biru langit, `#8A8275` abu hangat.

### Mode gelap (opsional)

| Token | Hex |
|---|---|
| `bg` | `#1C1915` |
| `surface` | `#26221D` |
| `surface-muted` | `#302B25` |
| `border` | `#3D3730` |
| `text-primary` | `#F2ECE3` |
| `text-secondary` | `#C4BBAE` |
| `text-tertiary` | `#968D80` |
| `primary` / hover / pressed | `#E8793A` / `#F08C52` / `#D96A2C`, teks tombol `#1C1915` |
| `info` / `success` / `danger` / `warning` | `#6AA5DE` / `#5CB987` / `#E8675D` / `#E9B04F` |

Di mode gelap, bayangan hampir tidak terlihat. Bedakan lapisan lewat warna `surface` yang lebih terang, bukan bayangan.

### Aturan pakai warna

- Proporsi kira-kira 60% krem dan putih, 30% teks dan netral, maksimal 10% aksen.
- Oranye untuk satu aksi utama per area. Biru hanya info dan tautan. Hijau hanya sukses dan aktif. Merah hanya error dan aksi destruktif.
- Warna status tidak dipakai untuk dekorasi.
- Status tidak boleh bergantung pada warna saja. Selalu ada ikon atau teks.
- Warna baru boleh ditambah kalau solid, hue-nya di luar 240° sampai 320°, dan lolos kontras.

---

## 3. Tipografi

- **Font utama:** Plus Jakarta Sans, untuk heading dan isi.
- **Fallback:** `system-ui, -apple-system, "Segoe UI", Roboto, sans-serif`.
- **Mono:** JetBrains Mono, untuk kode dan ID.
- **Mobile:** muat Plus Jakarta Sans lewat `expo-font` atau `google_fonts`. Kalau ukuran bundle jadi masalah, pakai font sistem (SF Pro, Roboto).
- Maksimal 2 family per proyek.

| Token | Ukuran / line-height | Weight | Letter-spacing | Pemakaian |
|---|---|---|---|---|
| `display` | 48 / 56 | 700 | -0.02em | Hero landing |
| `h1` | 36 / 44 | 700 | -0.02em | Judul landing |
| `h2` | 30 / 38 | 700 | -0.01em | Judul section landing |
| `h3` | 24 / 32 | 600 | -0.01em | Judul halaman app, angka KPI |
| `h4` | 20 / 28 | 600 | 0 | Sub-judul halaman |
| `h5` | 16-18 / 24-26 | 600 | 0 | Judul kartu |
| `body-lg` | 18 / 28 | 400 | 0 | Isi landing, artikel |
| `body` | 16 / 24 | 400 | 0 | Isi default |
| `body-sm` | 14 / 20 | 400 | 0 | Isi halaman padat, tabel |
| `caption` | 13 / 18 | 500 | 0 | Meta, label form, header tabel |
| `overline` | 12 / 16 | 600 | +0.06em, uppercase | Label section, badge |

Aturan:

- Maksimal 3 weight per layar, biasanya 400, 600, 700.
- Panjang baris teks bacaan 60 sampai 75 karakter.
- Angka di tabel dan statistik pakai `font-variant-numeric: tabular-nums`.
- Paragraf tidak pakai warna aksen.
- Ukuran minimum 12px untuk badge dan overline, 14px untuk teks yang dibaca, 16px untuk input di mobile supaya iOS tidak auto-zoom.

---

## 4. Spasi, sudut, dan bayangan

### Spasi

Basis 4px. Skala: `4 8 12 16 20 24 32 40 48 64 80 96`.

| Konteks | Nilai |
|---|---|
| Ikon ke teks, padding badge | 4-8 |
| Padding horizontal tombol dan input | 12-20 |
| Padding kartu | 16 padat, 20-24 normal, 32 lapang |
| Jarak antar item dalam satu kelompok | 8-12 |
| Jarak antar kartu atau kelompok | 24-32 |
| Jarak antar section | 48-64 di app, 80-96 di landing |
| Padding halaman | 16 mobile, 24 tablet, 32 desktop |

Prinsip inti: jarak di dalam kelompok selalu lebih kecil dari jarak antar kelompok.

Layout: container maksimal 1200px untuk landing, 1440px untuk dashboard. Grid 12 kolom gap 24 di desktop, 4 kolom gap 16 di mobile.

### Sudut membulat

| Token | Nilai | Pemakaian |
|---|---|---|
| `radius-xs` | 4px | Checkbox, tag kecil |
| `radius-sm` | 6px | Badge, tooltip |
| `radius-md` | 10px | Tombol, input, select, item navigasi |
| `radius-lg` | 14px | Kartu |
| `radius-xl` | 20px | Modal, bottom sheet, kartu hero |
| `radius-full` | 9999px | Pill, avatar, toggle |

Elemen di dalam kartu memakai radius lebih kecil dari kartunya. Rumusnya radius dalam = radius luar dikurangi padding, minimal 4px.

### Bayangan

Bayangan memakai tint coklat hangat dari `text-primary`, tidak pernah hitam murni.

| Token | Nilai | Pemakaian |
|---|---|---|
| `shadow-sm` | `0 1px 2px rgba(46,42,37,0.06)` | Kartu default, tombol |
| `shadow-md` | `0 4px 12px rgba(46,42,37,0.08), 0 1px 3px rgba(46,42,37,0.05)` | Kartu hover, dropdown |
| `shadow-lg` | `0 12px 32px rgba(46,42,37,0.12), 0 2px 6px rgba(46,42,37,0.06)` | Modal, popover |

---

## 5. Gaya komponen

### Tombol

| Ukuran | Tinggi | Padding X | Font |
|---|---|---|---|
| `sm` | 32px | 12px | 14 / 600 |
| `md` | 40px | 16px | 15 / 600 |
| `lg` | 48px | 20px | 16 / 600 |

Di mobile, area sentuh minimal 44px (iOS) atau 48dp (Android). Radius `radius-md`, ikon 16-20px dengan gap 8px.

| Varian | Default | Hover | Pressed |
|---|---|---|---|
| Primary | bg `#BE531A`, teks `#FFFDF9` | bg `#A54715` | bg `#8C3C12` |
| Secondary | bg `#FFFDF9`, border `#D3C7B3`, teks `#2E2A25` | bg `#F5EFE4` | bg `#F2ECE1` |
| Ghost | transparan, teks `#5C554C` | bg `#F5EFE4` | bg `#F2ECE1` |
| Danger | bg `#C23B32`, teks `#FFFDF9` | bg `#A5302A` | bg `#8A2722` |
| Link | teks `#2F6FB0` | teks `#255A91`, underline | teks `#1D4A78` |

- **Disabled.** bg `#E8E0D2`, teks `#A69E90`, tanpa bayangan dan animasi, `cursor: not-allowed`.
- **Loading.** Spinner menggantikan ikon atau label, lebar tombol tetap, tombol nonaktif.
- **Focus.** Hanya lewat `:focus-visible`, ring 3px `focus-ring` dengan offset 2px.
- Satu tombol primary per area. Label berupa kata kerja: "Simpan", "Kirim laporan", bukan "OK".

### Kartu

- bg `surface`, border 1px `border`, radius `radius-lg`, `shadow-sm`, padding 20px (16px di halaman padat).
- Header kartu berisi judul `h5` di kiri dan aksi kecil di kanan.
- Kartu yang bisa diklik mendapat efek hover. Kartu statis tidak.
- Jangan menaruh kartu di dalam kartu. Pakai `surface-muted` atau divider.

### Input

- Tinggi 40px di web, 48px di mobile. bg `surface`, border 1px `border-strong`, radius `radius-md`, padding X 12px.
- Label di atas input, 13 / 500 `text-secondary`, jarak 6px. Placeholder tidak boleh menggantikan label.
- Helper text 13px `text-tertiary` di bawah input.

| State | Tampilan |
|---|---|
| Hover | border `#BFB29C` |
| Focus | border `primary-500` + ring `focus-ring` |
| Error | border `danger-600` + ring `rgba(194,59,50,0.25)` + pesan `danger-600` dengan ikon |
| Disabled | bg `surface-muted`, teks `text-disabled` |

Checkbox, radio, dan toggle yang aktif memakai `primary-600`. Hijau tetap dipakai untuk badge status, bukan kontrol form.

### Navigasi

- **Sidebar web.** Lebar 240-264px, versi ciut 72px. Item tinggi 40px, radius `radius-md`, ikon 20px. Aktif: bg `primary-50`, teks `primary-700`, weight 600. Hover: bg `surface-hover`.
- **Topbar.** Tinggi 56-64px, bg `surface`, border bawah 1px `border`.
- **Navbar landing.** Tinggi 64-72px, menyatu dengan `bg`. Saat scroll, ganti ke bg `surface` dan tambah border bawah. Tanpa blur.
- **Bottom tab mobile.** 3 sampai 5 item, tinggi 56px ditambah safe area, ikon 24px dan label 12px. Aktif `primary-600`, tidak aktif `text-tertiary`.
- **Tabs.** Aktif diberi garis bawah 2px `primary-600` dan teks `text-primary`. Tidak aktif `text-secondary`.
- Breadcrumb dipakai kalau kedalaman halaman lebih dari 2 level.

### Komponen pendukung

- **Badge.** Tinggi 22-24px, radius `radius-full`, teks 12 / 600. Warna pakai pasangan `*-50` untuk latar dan `*-700` atau `*-600` untuk teks.
- **Tabel.** Header bg `surface-muted`, teks 13 / 600 `text-secondary`, sticky. Baris 44px (versi compact 36px), divider `border`, hover `surface-hover`. Angka rata kanan dengan tabular-nums. Pakai zebra atau divider, jangan keduanya.
- **Alert.** bg `*-50`, border 1px warna yang sama sedikit lebih gelap, ikon `*-600`, radius `radius-md`.
- **Toast.** Kanan bawah di web, atas atau bawah di mobile. Hilang otomatis setelah 4-5 detik, kecuali error.
- **Modal.** Radius `radius-xl`, `shadow-lg`, lebar maksimal 480px atau 640px. Di mobile berubah jadi bottom sheet.
- **Empty state.** Satu ikon atau ilustrasi sederhana, satu kalimat penjelasan, satu tombol aksi.
- **Skeleton.** bg `surface-muted` dengan pulse opacity 1 ke 0.6. Tanpa shimmer, karena shimmer adalah gradient.
- **Ikon.** Lucide atau Phosphor, stroke 1.75, ukuran 16, 20, atau 24. Satu set ikon per proyek.

---

## 6. Aturan animasi

### Token

| Token | Nilai | Pemakaian |
|---|---|---|
| `duration-fast` | 150ms | Hover, perubahan warna, focus |
| `duration-base` | 200ms | Press, toggle, dropdown |
| `duration-slow` | 300ms | Halaman, modal, bottom sheet |
| `ease-out` | `cubic-bezier(0.2, 0.8, 0.2, 1)` | Elemen masuk |
| `ease-in` | `cubic-bezier(0.4, 0, 1, 1)` | Elemen keluar |
| `ease-standard` | `cubic-bezier(0.4, 0, 0.2, 1)` | Perubahan state |

### Interaksi

| Elemen | Gerakan | Durasi |
|---|---|---|
| Tombol hover | `translateY(-1px)`, bg ke warna hover, `shadow-sm` | 150ms |
| Tombol pressed | `translateY(0) scale(0.98)`, bg ke warna pressed | 100-150ms |
| Kartu hover (bisa diklik) | `shadow-sm` ke `shadow-md`, boleh `translateY(-2px)` | 200ms |
| Masuk halaman | opacity 0 ke 1, `translateY(8px)` ke 0 | 250ms `ease-out` |
| Keluar halaman | opacity 1 ke 0 | 150ms `ease-in` |
| Elemen muncul / daftar | opacity + `translateY(12px)`, stagger 40ms, maksimal 6 item | 300ms |
| Modal | overlay fade 200ms, panel opacity + `scale(0.97)` ke 1 | 250ms |
| Bottom sheet | `translateY(100%)` ke 0 | 300ms |
| Dropdown, tooltip | opacity + `translateY(4px)` | 150ms |
| Toast | geser 16px + fade | 200ms |
| Press di mobile | `scale(0.97)`, haptic ringan opsional | 100ms |

### Aturan

- Animasikan `transform` dan `opacity`. Untuk hover boleh juga `color`, `background-color`, `box-shadow`. Jangan animasikan `width`, `height`, `top`, `left`, atau `margin`.
- Animasi keluar lebih cepat dari animasi masuk.
- Umpan balik interaksi harus mulai dalam 100ms.
- Animasi muncul saat scroll hanya diputar sekali.
- Tanpa bounce, elastic, parallax, atau loop otomatis.
- Teks yang sedang dibaca dan angka tabel saat refresh tidak dianimasikan.
- Efek hover hanya aktif di perangkat yang punya hover. Bungkus dengan `@media (hover: hover)` supaya efek angkat tidak "nyangkut" setelah disentuh di HP.
- Efek hover hanya untuk elemen yang bisa diklik.
- Hormati `prefers-reduced-motion` dan setting Reduce Motion di mobile. Gerakan dimatikan, perubahan warna dan state langsung terjadi.

```css
.btn { transition: transform 150ms var(--ease-out), background-color 150ms, box-shadow 150ms; }
@media (hover: hover) {
  .btn:hover { transform: translateY(-1px); }
}
.btn:active { transform: translateY(0) scale(0.98); transition-duration: 100ms; }

@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
    scroll-behavior: auto !important;
  }
}
```

---

## 7. Guardrail anti-slop

UI slop adalah tampilan yang jelas dibuat otomatis tanpa keputusan desain. Ciri-cirinya generik, simetris tanpa alasan, penuh dekorasi, dan isinya karangan. Daftar di bawah memblokir pola yang paling sering muncul.

### Warna dan efek

- **Ungu dan sekitarnya.** Ungu, violet, lavender, indigo, magenta, ungu-pink. Rentang hue 240° sampai 320° tidak boleh dipakai di mana pun, termasuk chart, ilustrasi, avatar, badge, syntax highlight, dan tint bayangan. Biru yang condong ke indigo seperti `#4F46E5` atau `#6366F1` (default Tailwind) juga dilarang.
- **Gradient dalam bentuk apa pun.** Latar, tombol, teks, border, overlay, mesh, blob, shimmer skeleton. Semua warna solid.
- **Efek dekoratif.** Glassmorphism, `backdrop-filter: blur`, glow, neon, blob blur bercahaya, tekstur noise, pola grid atau titik di latar hero.
- Ikon sparkle sebagai penanda fitur "pintar". Kartu atau alert dengan border kiri tebal berwarna.
- Hitam murni `#000` untuk teks dan putih murni `#FFF` sebagai latar halaman.
- Bayangan hitam pekat atau tebal seperti `shadow-2xl`. Border lebih dari 1px, kecuali focus ring dan indikator aktif.

### Layout

- Semua isi rata tengah. Rata tengah hanya untuk hero landing, empty state, dan halaman login/register. Sisanya rata kiri.
- Hero formula, yaitu badge pill di atas headline ("Baru: ..."), satu kata di headline diberi warna aksen, dua tombol berdampingan, dan mockup miring. Pakai paling banyak satu dari elemen ini.
- Grid tiga kartu fitur identik berisi ikon dalam kotak berwarna, judul, dan dua baris teks. Ganti dengan daftar fitur plus screenshot, dua kolom teks dan gambar, atau tabel perbandingan.
- Bento grid yang ukuran kotaknya tidak ditentukan oleh isi.
- Struktur section yang sama (judul tengah, subjudul, grid kartu) berulang lebih dari dua kali berturut-turut.
- Overline uppercase di atas setiap judul section. Maksimal satu per halaman.
- Semua hal dibungkus kartu. Kalau isi sudah jelas dengan judul dan spasi, kartu tidak perlu.
- Satu radius besar untuk semua elemen, misalnya `rounded-2xl` di mana-mana. Ikuti skala radius per jenis elemen.
- Nilai spasi acak di luar skala 4px.

### Konten dan copy

- Copy pemasaran kosong seperti "Unlock", "Elevate", "Seamless", "Revolutionize", "Supercharge", "Next-generation", "All-in-one solution", "Tingkatkan produktivitas ke level berikutnya". Tulis apa yang produk lakukan dengan kalimat biasa.
- Statistik, testimoni, logo klien, dan rating bintang karangan. Kalau datanya belum ada, hapus section-nya.
- Emoji di judul, tombol, atau sebagai ikon.
- Ikon di samping setiap label dan judul. Ikon dipakai kalau membantu mengenali atau memindai.
- Lorem ipsum, "John Doe", "Acme", "Your Company".
- Tombol generik seperti "Get Started" atau "Learn More". Label tombol menyebut hasilnya, misalnya "Buat invoice" atau "Lihat laporan Juni".

### Dashboard

- Empat kartu KPI identik dengan panah hijau "+12.5%" tanpa konteks. Setiap angka butuh pembanding yang jelas, misalnya periode sebelumnya atau target.
- Grafik dekoratif. Setiap grafik menjawab satu pertanyaan, judulnya menyebut kesimpulan atau pertanyaan itu, dan sumbunya berlabel.
- Sidebar berisi belasan menu sejajar. Kelompokkan, maksimal sekitar 7 item per grup.
- Teks abu-abu pucat di mana-mana demi kesan bersih. Ikuti aturan kontras di bagian 8.

### Komponen dan gerak

- Lebih dari satu tombol primary dalam satu area.
- Lebih dari 2 font family. Teks di bawah 12px.
- Semua elemen fade-in saat scroll. Animasi muncul hanya untuk elemen yang memang perlu diperhatikan.
- Efek hover di elemen yang tidak bisa diklik.
- Animasi interaksi lebih dari 400ms, atau animasi yang menghalangi input.
- Placeholder sebagai pengganti label.

### Yang dilakukan sebagai gantinya

- Hierarki ukuran yang tegas. Judul halaman jelas lebih besar dari isi, dan isi jelas lebih besar dari meta.
- Satu warna aksen yang konsisten. Oranye muncul di tempat yang sama untuk fungsi yang sama.
- Asimetri yang beralasan, misalnya kolom utama lebar dengan kolom samping sempit.
- Visual dari isi asli seperti screenshot produk, data nyata, atau foto nyata. Kalau belum ada, pakai blok `surface-muted` berlabel jelas. Jangan pakai ilustrasi 3D generik.
- Detail kecil yang dikerjakan: elemen sejajar ke grid, angka tabular, focus ring, empty state yang memberi tahu langkah berikutnya.

### Audit sebelum selesai

Semua poin harus dijawab "ya".

1. Tidak ada warna dengan hue 240° sampai 320°, termasuk warna bawaan library chart dan komponen.
2. Tidak ada `linear-gradient`, `radial-gradient`, `conic-gradient`, `bg-gradient-*`, atau `backdrop-filter`.
3. Semua warna, spasi, radius, dan durasi diambil dari token.
4. Teks normal kontrasnya minimal 4.5:1.
5. Satu tombol primary per area, dan labelnya kata kerja yang spesifik.
6. Semua elemen interaktif punya state hover, pressed, focus-visible, dan disabled.
7. Ada state loading, kosong, dan error.
8. Tidak ada konten karangan seperti lorem ipsum, nama palsu, statistik, testimoni, atau logo klien.
9. Tidak ada copy pemasaran kosong dan tidak ada emoji di UI.
10. Isi utama rata kiri dan struktur section tidak berulang sama lebih dari dua kali.
11. Halaman padat lolos aturan di bagian 8.
12. `prefers-reduced-motion` sudah ditangani dan hover dibungkus `@media (hover: hover)`.
13. Tes tutup logo. Kalau logo dan nama produk ditutup, tampilan ini masih bisa dibedakan dari template AI lain. Kalau tidak, ubah hierarki, layout, atau visualnya.

---

## 8. Aturan kepadatan

### Kapan lapang, kapan padat

| Mode | Contoh halaman | Spasi | Teks isi |
|---|---|---|---|
| Lapang | Landing, login, register, onboarding, halaman sukses, empty state | Section 80-96px (48-64px di mobile), kolom teks maksimal 640-720px, satu CTA per section | 16-18px |
| Sedang | Form, halaman detail, profil, pengaturan | Padding kartu 24px, gap 24px | 16px |
| Padat | Dashboard, tabel, daftar, panel admin, laporan, riwayat transaksi | Padding halaman 24px (16px mobile), gap kartu 24px (minimal 16px), padding kartu 16-20px, item dalam kartu 8-12px | 14px |

### Menjaga keterbacaan di halaman padat

1. **Kelompokkan.** Info yang berhubungan masuk ke satu kartu atau section berjudul. Satu kartu, satu topik.
2. **Atur jarak.** Jarak antar kelompok minimal dua kali jarak di dalam kelompok, misalnya 24px dan 12px. Pakai ruang kosong dulu, garis pemisah hanya kalau masih perlu.
3. **Hierarki 3 level per kartu.**
   - Judul: 16-18 / 600, `text-primary`.
   - Isi atau nilai: 14-16 / 400-600, `text-primary`. Angka KPI 24-30 / 700.
   - Meta: 12-13 / 500, `text-tertiary`.
4. **Titik mulai yang jelas.** Ringkasan dan KPI di kiri atas, detail di bawahnya. Dashboard mengikuti pola baca F.
5. **Ukuran teks minimum.** Isi 14px (14-15px di mobile), meta dan label 12px, input mobile 16px.
6. **Kontras.** Teks normal minimal 4.5:1. Teks besar (24px ke atas, atau 18px ke atas tebal) dan ikon minimal 3:1. `text-tertiary` hanya untuk meta, jangan untuk isi utama.
7. **Warna hemat.** Di halaman padat, warna hanya untuk status dan aksi. Sisanya netral.
8. **Mobile.** Tabel lebar diubah jadi daftar kartu bertumpuk. Info sekunder disembunyikan di halaman detail atau expand.
9. **Teks panjang.** Potong dengan ellipsis dan tampilkan versi lengkap lewat tooltip atau halaman detail.

---

## Token siap pakai

```css
:root {
  /* dasar */
  --bg: #F8F4EC; --surface: #FFFDF9; --surface-muted: #F2ECE1; --surface-hover: #F5EFE4;
  --border: #E8E0D2; --border-strong: #D3C7B3;
  /* teks */
  --text-primary: #2E2A25; --text-secondary: #5C554C; --text-tertiary: #756D61;
  --text-disabled: #A69E90; --text-on-accent: #FFFDF9;
  /* aksen */
  --primary-50: #FDF1E8; --primary-100: #FADDC6; --primary-500: #E2702F;
  --primary-600: #BE531A; --primary-700: #A54715; --primary-800: #8C3C12;
  --info-50: #E8F1FA; --info-600: #2F6FB0; --info-700: #255A91; --info-800: #1D4A78;
  --success-50: #E6F4EC; --success-500: #3A9B63; --success-600: #27774A; --success-700: #1F6340;
  --danger-50: #FBEAE8; --danger-600: #C23B32; --danger-700: #A5302A; --danger-800: #8A2722;
  --warning-50: #FDF4E3; --warning-500: #E9A23B; --warning-700: #8F5D12;
  --focus-ring: rgba(226,112,47,0.35);
  /* bentuk */
  --radius-xs: 4px; --radius-sm: 6px; --radius-md: 10px; --radius-lg: 14px; --radius-xl: 20px;
  --shadow-sm: 0 1px 2px rgba(46,42,37,0.06);
  --shadow-md: 0 4px 12px rgba(46,42,37,0.08), 0 1px 3px rgba(46,42,37,0.05);
  --shadow-lg: 0 12px 32px rgba(46,42,37,0.12), 0 2px 6px rgba(46,42,37,0.06);
  /* gerak */
  --duration-fast: 150ms; --duration-base: 200ms; --duration-slow: 300ms;
  --ease-out: cubic-bezier(0.2, 0.8, 0.2, 1);
  --ease-in: cubic-bezier(0.4, 0, 1, 1);
  --ease-standard: cubic-bezier(0.4, 0, 0.2, 1);
  /* font */
  --font-sans: "Plus Jakarta Sans", system-ui, -apple-system, "Segoe UI", Roboto, sans-serif;
  --font-mono: "JetBrains Mono", ui-monospace, monospace;
}
```

Untuk Flutter, React Native, atau Tailwind, pakai nama token dan nilai yang sama supaya konsisten di semua platform.
