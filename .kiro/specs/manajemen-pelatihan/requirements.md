# Requirements Document — Aplikasi Manajemen Pelatihan Android

## 1. Tujuan

Membangun aplikasi Android untuk satu lembaga yang mengelola pelatihan, peserta, pembayaran, penyampaian informasi melalui WhatsApp, pengguna aplikasi, serta laporan bulanan. Semua pengguna yang berwenang melihat data yang sama melalui penyimpanan cloud.

## 2. Ruang lingkup dan keputusan final

- Platform awal adalah Android.
- Aplikasi digunakan oleh satu lembaga.
- Data terstruktur disimpan di Cloud Firestore, bukan di file Google Drive.
- Google Drive atau layanan lain dapat menjadi asal link Zoom, rekaman, dan PDF, tetapi aplikasi hanya menyimpan URL.
- Logo E-Pelatihan berasal dari file lokal `logo.png`, dikompres, dan dibundel sebagai resource aplikasi; logo tidak dapat diunggah atau diubah dari aplikasi.
- Firebase menggunakan paket Spark dan aplikasi tidak bergantung pada Firebase Storage.
- Autentikasi menggunakan email dan password.
- Pengguna tidak mendaftar sendiri; Admin mengundang pengguna.
- Setiap pelatihan mempunyai satu jadwal dan satu metode pertemuan.
- Pertemuan dapat berupa online atau offline.
- Pelatihan dapat menggunakan harga normal saja, atau harga early bird dan harga normal.
- Jika early bird tersedia, jenis harga dipilih manual untuk setiap peserta saat pendaftaran.
- Pembayaran hanya memiliki status `BELUM_BAYAR` dan `LUNAS`.
- Tidak ada DP, diskon selain early bird, pembatalan pembayaran, pengembalian dana, atau unggahan bukti pembayaran.
- Pengiriman WhatsApp bersifat manual: aplikasi menyiapkan pesan dan membuka WhatsApp, lalu pengguna menekan tombol kirim.
- Laporan dapat diekspor ke PDF dan Excel/XLSX.

## 3. Istilah

- **Admin**: pengguna dengan akses penuh, termasuk pengelolaan akun dan perubahan data.
- **User**: pengguna operasional yang dapat melihat dan menambahkan data tertentu, tetapi tidak dapat mengubah data yang sudah disimpan atau mengubah pembayaran.
- **Pelatihan**: kegiatan yang memiliki satu jadwal, pemateri, metode, dan konfigurasi harga.
- **Peserta**: orang yang dapat didaftarkan ke satu atau lebih pelatihan.
- **Pendaftaran**: hubungan antara peserta dan pelatihan, termasuk harga yang dipilih dan status pembayaran.
- **Harga tersimpan**: salinan nominal harga pada saat pendaftaran agar riwayat tidak berubah ketika master harga diubah.

## 4. Matriks hak akses

| Kemampuan | Admin | User |
|---|---:|---:|
| Login dan logout | Ya | Ya |
| Melihat pelatihan | Ya | Ya |
| Menambahkan pelatihan | Ya | Ya |
| Mengubah pelatihan | Ya | Tidak |
| Menghapus pelatihan | Ya | Tidak |
| Melihat peserta dan pendaftaran | Ya | Ya |
| Menambahkan peserta dan pendaftaran | Ya | Ya |
| Mengubah atau menghapus peserta/pendaftaran | Ya | Tidak |
| Memilih early bird/normal saat pendaftaran baru | Ya | Ya |
| Memperbaiki jenis harga pendaftaran tersimpan | Ya | Tidak |
| Mengubah status pembayaran | Ya | Tidak |
| Membuka WhatsApp dengan pesan siap kirim | Ya | Ya |
| Melihat dan mengekspor laporan | Ya | Ya |
| Mengubah nama komunitas | Ya | Tidak |
| Mengundang dan mengelola pengguna aplikasi | Ya | Tidak |

Pembatasan harus diterapkan pada Firestore Security Rules dan backend, bukan hanya dengan menyembunyikan tombol di antarmuka.

## 5. Kebutuhan fungsional

### REQ-01 — Autentikasi

**User story:** Sebagai pengguna terdaftar, saya ingin login agar dapat mengakses data sesuai role saya.

#### Kriteria penerimaan

1. Aplikasi menyediakan login menggunakan email dan password.
2. Aplikasi menyediakan logout dan pemulihan password.
3. Pengguna yang dinonaktifkan tidak dapat mengakses data aplikasi.
4. Password tidak disimpan di dokumen Firestore, kode aplikasi, maupun log.
5. Setelah login, aplikasi memuat profil dan role pengguna.
6. Akses ditolak apabila akun tidak mempunyai profil pengguna lembaga yang aktif.

### REQ-02 — Undangan dan pengelolaan pengguna

**User story:** Sebagai Admin, saya ingin mengundang pengguna agar mereka dapat memakai aplikasi dengan role yang ditentukan.

#### Kriteria penerimaan

1. Hanya Admin yang dapat melihat menu pengelolaan pengguna.
2. Admin dapat memasukkan nama, email, dan role `ADMIN` atau `USER`.
3. Sistem mengirim undangan atau tautan penetapan password kepada email pengguna.
4. Pengguna menentukan password sendiri; Admin tidak mengetahui password tersebut.
5. Admin dapat mengubah role serta mengaktifkan atau menonaktifkan pengguna.
6. Sistem menyediakan mekanisme aman untuk membuat Admin pertama.
7. Operasi akun yang membutuhkan Firebase Admin SDK dijalankan melalui backend/Cloud Functions.

### REQ-03 — Master pelatihan

**User story:** Sebagai Admin atau User, saya ingin menambahkan pelatihan agar kegiatan dapat dikelola dan peserta dapat didaftarkan.

#### Data pelatihan

- Nama pelatihan.
- Deskripsi, opsional.
- Tanggal pelatihan.
- Jam mulai.
- Jam selesai.
- Nama pemateri.
- Metode `ONLINE` atau `OFFLINE`.
- Link Zoom untuk pelatihan online.
- Lokasi untuk pelatihan offline.
- Link rekaman, opsional.
- Link PDF/materi, opsional.
- Skema harga.
- Status pelatihan.
- Pembuat, waktu pembuatan, pengubah terakhir, dan waktu perubahan.

#### Kriteria penerimaan

1. Setiap pelatihan hanya mempunyai satu jadwal.
2. Jam selesai harus setelah jam mulai.
3. Pelatihan online mewajibkan link Zoom.
4. Pelatihan offline mewajibkan lokasi.
5. URL yang diisi harus mempunyai format URL HTTPS yang valid.
6. Admin dan User dapat menambahkan pelatihan.
7. Hanya Admin yang dapat mengubah atau menghapus pelatihan setelah tersimpan.
8. Penghapusan tidak boleh merusak riwayat laporan atau pendaftaran; data yang sudah dipakai harus dinonaktifkan/diarsipkan.

### REQ-04 — Konfigurasi harga pelatihan

**User story:** Sebagai pembuat pelatihan, saya ingin memilih skema harga agar pelatihan dapat memakai harga normal saja atau early bird dan normal.

#### Kriteria penerimaan

1. Skema `NORMAL_ONLY` mewajibkan satu harga normal.
2. Skema `EARLY_BIRD_AND_NORMAL` mewajibkan harga early bird dan harga normal.
3. Harga early bird harus lebih kecil daripada harga normal.
4. Semua nominal harus nol atau lebih dan disimpan sebagai bilangan bulat dalam satuan mata uang terkecil.
5. Early bird tidak mempunyai batas tanggal atau kuota otomatis.
6. Jenis harga dipilih manual saat setiap peserta didaftarkan.
7. Tidak tersedia diskon nominal, diskon persentase, atau perubahan harga bebas pada tingkat pendaftaran.

### REQ-05 — Data peserta dan kontak HP

**User story:** Sebagai Admin atau User, saya ingin memasukkan peserta secara manual atau dari kontak HP agar pendaftaran lebih cepat.

#### Kriteria penerimaan

1. Peserta dapat dibuat dengan nama dan nomor WhatsApp.
2. Email dan catatan peserta bersifat opsional.
3. Pengguna dapat memilih kontak melalui fasilitas kontak Android.
4. Aplikasi hanya menyimpan data kontak yang dipilih pengguna.
5. Nomor telepon dinormalisasi ke format yang konsisten sebelum disimpan.
6. Aplikasi mendeteksi nomor yang sudah terdaftar dan menawarkan penggunaan data peserta yang sudah ada.
7. Peserta yang sama dapat mengikuti beberapa pelatihan.
8. Peserta tidak boleh didaftarkan dua kali ke pelatihan yang sama.
9. Setelah data tersimpan, hanya Admin yang dapat memperbaiki atau mengarsipkannya.

### REQ-06 — Pendaftaran dan pemilihan harga

**User story:** Sebagai Admin atau User, saya ingin mendaftarkan peserta dan memilih harga yang berlaku agar tagihan tercatat dengan benar.

#### Kriteria penerimaan

1. Pendaftaran menghubungkan tepat satu peserta dengan satu pelatihan.
2. Jika pelatihan memakai `NORMAL_ONLY`, sistem otomatis memilih harga normal.
3. Jika pelatihan memakai `EARLY_BIRD_AND_NORMAL`, pengguna wajib memilih `EARLY_BIRD` atau `NORMAL`.
4. Sistem menampilkan nominal di setiap pilihan harga.
5. Sistem menyimpan `pricingType` dan `priceAmount` pada pendaftaran.
6. `priceAmount` merupakan salinan harga ketika pendaftaran dibuat.
7. Perubahan harga master tidak mengubah pendaftaran yang sudah ada.
8. Pendaftaran baru otomatis berstatus `BELUM_BAYAR`.
9. Admin dapat memperbaiki jenis harga pendaftaran; User tidak dapat mengubah pendaftaran yang sudah disimpan.
10. Perbaikan jenis harga memperbarui salinan nominal sesuai harga master yang berlaku dan dicatat dalam audit log.

### REQ-07 — Status pembayaran

**User story:** Sebagai Admin, saya ingin menandai pembayaran peserta agar akses pengiriman informasi dan laporan akurat.

#### Kriteria penerimaan

1. Status yang tersedia hanya `BELUM_BAYAR` dan `LUNAS`.
2. Hanya Admin yang dapat mengubah status pembayaran.
3. Ketika status berubah menjadi `LUNAS`, sistem menyimpan tanggal pembayaran dan Admin yang melakukan perubahan.
4. Perubahan status pembayaran dicatat dalam audit log.
5. Aplikasi tidak menyediakan DP, unggahan bukti, pembatalan pembayaran, atau pengembalian dana.

### REQ-08 — Pesan WhatsApp manual

**User story:** Sebagai Admin atau User, saya ingin mengirim informasi pelatihan kepada peserta yang sudah lunas melalui WhatsApp.

#### Kriteria penerimaan

1. Hanya pendaftaran berstatus `LUNAS` yang dapat dipilih untuk pengiriman.
2. Pesan mencantumkan nama peserta, nama pelatihan, tanggal, jam, pemateri, dan metode pertemuan.
3. Pesan menyertakan link Zoom, rekaman, dan PDF apabila tersedia dan relevan.
4. Untuk pelatihan offline, pesan menyertakan lokasi.
5. Aplikasi menyiapkan teks dan membuka WhatsApp ke nomor peserta.
6. Pengguna tetap memeriksa pesan dan menekan tombol kirim di WhatsApp.
7. Aplikasi tidak menggunakan WhatsApp Business Cloud API dan tidak mengirim pesan otomatis.
8. Jika WhatsApp tidak tersedia atau nomor tidak valid, aplikasi menampilkan pesan kesalahan yang jelas.
9. Aplikasi hanya mencatat bahwa WhatsApp dibuka; aplikasi tidak boleh mengklaim pesan telah terkirim karena status tersebut tidak dapat diverifikasi.

### REQ-09 — Laporan bulanan

**User story:** Sebagai Admin atau User, saya ingin melihat laporan bulanan agar dapat memantau pelatihan, pendaftar, dan pemasukan.

#### Kriteria penerimaan

1. Pengguna dapat memilih bulan dan tahun laporan.
2. Bulan laporan didasarkan pada tanggal jadwal pelatihan.
3. Laporan menampilkan jumlah pelatihan.
4. Laporan menampilkan jumlah seluruh pendaftar, pendaftar lunas, dan pendaftar belum bayar.
5. Laporan menampilkan jumlah pendaftar early bird dan harga normal.
6. Total tagihan adalah jumlah `priceAmount` seluruh pendaftaran pada pelatihan dalam bulan tersebut.
7. Total pemasukan adalah jumlah `priceAmount` pendaftaran berstatus `LUNAS` pada pelatihan dalam bulan tersebut.
8. Sisa belum dibayar adalah total tagihan dikurangi total pemasukan.
9. Laporan menampilkan rincian per pelatihan.
10. Admin dan User dapat melihat laporan yang sama sesuai data cloud terbaru.

### REQ-10 — Ekspor laporan

**User story:** Sebagai Admin atau User, saya ingin mengekspor laporan agar dapat mencetak atau mengolahnya di aplikasi lain.

#### Kriteria penerimaan

1. Laporan dapat diekspor menjadi PDF.
2. Laporan dapat diekspor menjadi Excel/XLSX.
3. Isi ekspor mengikuti filter bulan dan tahun yang sedang ditampilkan.
4. Ekspor memuat ringkasan dan rincian per pelatihan.
5. Pengguna memilih lokasi penyimpanan melalui pemilih dokumen Android.
6. Aplikasi menampilkan hasil berhasil atau gagal tanpa menganggap file tersimpan sebelum operasi selesai.

### REQ-11 — Penyimpanan dan sinkronisasi cloud

**User story:** Sebagai pengguna, saya ingin data tersinkron antar-HP agar seluruh petugas melihat data yang sama.

#### Kriteria penerimaan

1. Pelatihan, peserta, pendaftaran, profil pengguna, pengaturan aplikasi, dan audit log disimpan di Cloud Firestore.
2. Perubahan yang berhasil disimpan terlihat pada perangkat pengguna lain.
3. Aplikasi menampilkan status loading, kosong, gagal, dan tidak ada koneksi secara jelas.
4. Penulisan data yang gagal tidak boleh ditampilkan sebagai sukses.
5. Semua data operasional berada dalam ruang data satu lembaga.
6. Link eksternal disimpan sebagai URL; aplikasi tidak mengunggah logo, PDF, rekaman, atau bukti pembayaran.

### REQ-12 — Keamanan dan audit

**User story:** Sebagai pengelola lembaga, saya ingin data dan operasi penting dilindungi agar perubahan dapat dipertanggungjawabkan.

#### Kriteria penerimaan

1. Firestore Security Rules membatasi operasi berdasarkan akun aktif dan role.
2. Operasi khusus Admin tetap ditolak jika dipanggil langsung oleh User di luar antarmuka aplikasi.
3. Token, credential backend, dan secret tidak disimpan di repositori atau aplikasi klien.
4. Audit log mencatat pelaku, waktu, jenis operasi, referensi data, serta nilai lama dan baru untuk perubahan penting.
5. Audit minimal mencakup perubahan role/status pengguna, pelatihan, jenis harga pendaftaran, status pembayaran, dan pengaturan aplikasi.
6. Data dari kontak HP hanya digunakan setelah dipilih oleh pengguna.

### REQ-13 — Pengaturan identitas komunitas

**User story:** Sebagai Admin, saya ingin mengatur nama komunitas agar layar login menampilkan identitas penyedia pelatihan bersama logo aplikasi.

#### Kriteria penerimaan

1. Aplikasi menyediakan menu Pengaturan yang hanya dapat diakses Admin.
2. Menu Pengaturan hanya menyediakan input nama komunitas penyedia pelatihan.
3. Nama komunitas wajib diisi, mengabaikan spasi di awal/akhir, dan dibatasi maksimal 100 karakter.
4. Nama komunitas disimpan pada dokumen `appSettings/general` di Cloud Firestore.
5. Nama komunitas tampil pada layar login di semua perangkat setelah tersinkron.
6. Logo pada layar login selalu menggunakan resource lokal hasil optimasi dari `logo.png`.
7. Aplikasi tidak menyediakan pilihan, upload, perubahan, atau penghapusan logo.
8. Dokumen pengaturan dapat dibaca sebelum autentikasi dan hanya memuat nama komunitas serta metadata perubahan tanpa data sensitif.
9. Hanya Admin yang dapat mengubah nama komunitas.
10. Jika nama komunitas belum dikonfigurasi atau gagal dimuat, layar login menampilkan nama aplikasi `E-Pelatihan`.
11. Perubahan nama komunitas dicatat dalam audit log.

## 6. Model data konseptual

### AppSettings

- `communityName`
- `updatedBy`
- `updatedAt`

### UserProfile

- `id`
- `name`
- `email`
- `role`: `ADMIN | USER`
- `active`
- `createdAt`
- `updatedAt`

### Training

- `id`
- `name`
- `description`
- `date`
- `startTime`
- `endTime`
- `speaker`
- `meetingMethod`: `ONLINE | OFFLINE`
- `zoomUrl`
- `location`
- `recordingUrl`
- `materialUrl`
- `pricingScheme`: `NORMAL_ONLY | EARLY_BIRD_AND_NORMAL`
- `normalPrice`
- `earlyBirdPrice`
- `status`
- `active`
- `createdBy`
- `createdAt`
- `updatedBy`
- `updatedAt`

### Participant

- `id`
- `name`
- `phoneNormalized`
- `phoneDisplay`
- `email`
- `notes`
- `active`
- `createdBy`
- `createdAt`
- `updatedBy`
- `updatedAt`

### Registration

- `id`
- `trainingId`
- `participantId`
- `pricingType`: `EARLY_BIRD | NORMAL`
- `priceAmount`
- `paymentStatus`: `BELUM_BAYAR | LUNAS`
- `paidAt`
- `paidBy`
- `createdBy`
- `createdAt`
- `updatedBy`
- `updatedAt`

### WhatsAppOpenLog

- `id`
- `registrationId`
- `recipientPhone`
- `openedBy`
- `openedAt`
- `messageSnapshot`

### AuditLog

- `id`
- `actorId`
- `action`
- `entityType`
- `entityId`
- `before`
- `after`
- `createdAt`

## 7. Aturan perhitungan

```text
jumlahPendaftar = jumlah seluruh pendaftaran dalam filter laporan

totalTagihan = jumlah priceAmount seluruh pendaftaran

totalPemasukan = jumlah priceAmount dengan paymentStatus LUNAS

sisaBelumDibayar = totalTagihan - totalPemasukan
```

Perhitungan menggunakan `priceAmount` pada pendaftaran, bukan harga terbaru pada master pelatihan.

## 8. Di luar ruang lingkup versi awal

- Aplikasi iOS atau web.
- Dukungan banyak lembaga/tenant.
- Beberapa jadwal atau sesi untuk satu pelatihan.
- WhatsApp otomatis atau WhatsApp Business Cloud API.
- Pembayaran online/payment gateway.
- DP, cicilan, refund, atau bukti pembayaran.
- Diskon selain harga early bird.
- Batas waktu atau kuota otomatis early bird.
- Upload file melalui aplikasi, termasuk logo, PDF, rekaman, dan bukti pembayaran.
- Presensi, sertifikat, ujian, dan penilaian peserta.
