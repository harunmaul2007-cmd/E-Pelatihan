# Implementation Tasks — Aplikasi Manajemen Pelatihan Android

Dokumen ini mengacu pada #[[file:requirements.md]] dan #[[file:design.md]]. Kerjakan task secara berurutan karena beberapa task bergantung pada model dan infrastruktur dari task sebelumnya.

- [x] 1. Inisialisasi proyek Android
  - Buat proyek Kotlin Android dengan Jetpack Compose dan struktur package yang konsisten.
  - Tetapkan arsitektur berlapis untuk UI, domain, dan data.
  - Tambahkan dependency dasar untuk Compose Navigation, ViewModel, coroutine, Firebase, serialisasi, dan dependency injection.
  - Siapkan konfigurasi build untuk environment development dan release tanpa memasukkan secret ke repositori.
  - Pastikan aplikasi dapat dikompilasi dan menampilkan layar awal.
  - **Mencakup:** REQ-01 sampai REQ-13 sebagai fondasi.

- [x] 2. Konfigurasikan Firebase dan struktur data cloud
  - Hubungkan aplikasi Android dengan Firebase Authentication dan Cloud Firestore.
  - Definisikan collection untuk `users`, `trainings`, `participants`, `participantPhoneIndex`, `registrations`, `whatsappOpenLogs`, dan `auditLogs`, serta dokumen tunggal `appSettings/general`.
  - Gunakan server timestamp untuk waktu audit dan perubahan data.
  - Siapkan converter/mapping antara dokumen Firestore dan model aplikasi.
  - Dokumentasikan konfigurasi Firebase yang harus disediakan di luar repositori.
  - **Mencakup:** REQ-01, REQ-02, REQ-11, REQ-12, REQ-13.

- [ ] 3. Implementasikan model domain dan aturan validasi
  - Buat model `AppSettings`, `UserProfile`, `Training`, `Participant`, `Registration`, `WhatsAppOpenLog`, dan `AuditLog`.
  - Buat enum untuk role, metode pertemuan, skema harga, jenis harga, status pembayaran, dan status pelatihan.
  - Terapkan validasi jadwal, metode online/offline, URL HTTPS, nominal harga, serta hubungan harga early bird dan normal.
  - Simpan nominal uang sebagai bilangan bulat dan hindari floating point.
  - Buat normalisasi nomor WhatsApp Indonesia dengan hasil yang konsisten.
  - **Mencakup:** REQ-03, REQ-04, REQ-05, REQ-06, REQ-07, REQ-13.

- [ ] 4. Implementasikan login, sesi, dan pemulihan password
  - Buat layar login email/password.
  - Muat nama komunitas dari Firestore dan logo lokal pada layar login dengan fallback nama aplikasi.
  - Hubungkan login dan logout ke Firebase Authentication.
  - Muat profil, role, dan status aktif setelah autentikasi.
  - Tolak akses untuk akun tanpa profil aktif.
  - Tambahkan alur lupa/reset password.
  - Atur navigasi awal berdasarkan status login dan role.
  - **Mencakup:** REQ-01, REQ-13.

- [ ] 5. Implementasikan bootstrap Admin dan undangan pengguna
  - Tentukan mekanisme undangan yang kompatibel dengan keputusan tetap memakai Spark; Cloud Functions tidak dapat dideploy pada paket ini.
  - Jika tersedia backend berizin, implementasikan operasi Firebase Admin SDK di backend tersebut; jika tidak, gunakan proses pembuatan akun manual melalui Firebase Console.
  - Sediakan mekanisme terbatas untuk membuat Admin pertama.
  - Buat menu Admin untuk daftar, undangan, perubahan role, aktivasi, dan penonaktifan pengguna.
  - Kirim tautan penetapan password tanpa memperlihatkan password kepada Admin.
  - Cegah User mengakses atau memanggil operasi pengelolaan akun.
  - Catat perubahan role dan status pengguna dalam audit log.
  - **Mencakup:** REQ-02, REQ-12.

- [ ] 6. Terapkan Firestore Security Rules dan otorisasi backend
  - Izinkan pembacaan data operasional hanya untuk akun lembaga yang aktif.
  - Izinkan Admin dan User membuat pelatihan, peserta, dan pendaftaran.
  - Izinkan perubahan atau pengarsipan pelatihan, peserta, dan pendaftaran hanya untuk Admin.
  - Izinkan perubahan status pembayaran dan jenis harga tersimpan hanya untuk Admin.
  - Batasi pengelolaan pengguna dan audit sensitif hanya untuk Admin/backend.
  - Izinkan pembacaan publik hanya pada nama komunitas dan metadata non-sensitif yang dibutuhkan layar login.
  - Batasi perubahan dokumen pengaturan hanya untuk Admin aktif.
  - Pastikan field yang dilindungi tidak dapat diubah melalui update parsial oleh User.
  - **Mencakup:** REQ-02, REQ-03, REQ-05, REQ-06, REQ-07, REQ-12, REQ-13.

- [ ] 7. Implementasikan daftar dan detail pelatihan
  - Buat layar daftar pelatihan dengan status loading, kosong, gagal, dan offline.
  - Buat layar detail yang menampilkan jadwal, pemateri, metode, lokasi/link, dan harga.
  - Tampilkan aksi sesuai role: Admin dapat mengubah/mengarsipkan, User hanya melihat.
  - Gunakan listener Firestore agar perubahan terlihat pada perangkat lain.
  - **Mencakup:** REQ-03, REQ-11.

- [ ] 8. Implementasikan formulir tambah dan ubah pelatihan
  - Buat input nama, deskripsi, tanggal, jam mulai/selesai, dan pemateri.
  - Buat pilihan metode online/offline dengan field kondisional link Zoom atau lokasi.
  - Tambahkan link rekaman dan PDF/materi sebagai URL opsional.
  - Buat pilihan skema `NORMAL_ONLY` atau `EARLY_BIRD_AND_NORMAL`.
  - Tampilkan field harga yang sesuai dan validasi early bird lebih kecil dari normal.
  - Izinkan Admin dan User membuat; batasi perubahan serta pengarsipan hanya untuk Admin.
  - Simpan metadata pembuat dan pengubah serta audit perubahan.
  - **Mencakup:** REQ-03, REQ-04, REQ-12.

- [ ] 9. Implementasikan data peserta dan integrasi kontak Android
  - Buat daftar dan detail peserta.
  - Buat input peserta manual untuk nama, nomor WhatsApp, email opsional, dan catatan.
  - Integrasikan pemilih kontak Android dan ambil hanya kontak yang dipilih.
  - Normalisasi nomor sebelum pencarian dan penyimpanan.
  - Deteksi peserta dengan nomor yang sama dan tawarkan penggunaan data yang sudah ada.
  - Batasi perubahan dan pengarsipan data tersimpan hanya untuk Admin.
  - **Mencakup:** REQ-05, REQ-12.

- [ ] 10. Implementasikan pendaftaran peserta dan pemilihan harga manual
  - Buat daftar pendaftaran pada detail pelatihan.
  - Sediakan pemilihan peserta yang sudah ada atau pembuatan peserta baru.
  - Cegah peserta yang sama didaftarkan dua kali ke pelatihan yang sama.
  - Untuk harga normal saja, pilih normal secara otomatis.
  - Untuk pelatihan early bird, wajibkan pilihan manual `EARLY_BIRD` atau `NORMAL` dan tampilkan nominalnya.
  - Simpan `pricingType` serta salinan `priceAmount` pada pendaftaran.
  - Tetapkan status awal `BELUM_BAYAR`.
  - Izinkan hanya Admin memperbaiki data pendaftaran tersimpan dan catat perubahannya.
  - **Mencakup:** REQ-05, REQ-06, REQ-12.

- [ ] 11. Implementasikan pengelolaan status pembayaran
  - Tampilkan status `BELUM_BAYAR` dan `LUNAS` pada daftar serta detail pendaftaran.
  - Sembunyikan aksi perubahan pembayaran dari User.
  - Izinkan Admin mengubah status dengan konfirmasi.
  - Simpan `paidAt` dan `paidBy` ketika status menjadi lunas.
  - Bersihkan nilai pelunasan secara konsisten jika Admin mengoreksi status menjadi belum bayar.
  - Catat nilai sebelum dan sesudah pada audit log.
  - **Mencakup:** REQ-07, REQ-12.

- [ ] 12. Implementasikan pesan WhatsApp manual
  - Buat pembentuk pesan dari peserta, pelatihan, jadwal, metode, serta link yang tersedia.
  - Aktifkan aksi hanya untuk peserta berstatus `LUNAS`.
  - Buka WhatsApp ke nomor yang sudah dinormalisasi dengan teks yang sudah terisi.
  - Tangani kondisi nomor tidak valid atau aplikasi WhatsApp tidak tersedia.
  - Catat peristiwa `WHATSAPP_OPENED` tanpa menyatakan pesan sudah terkirim.
  - Pastikan tidak ada WhatsApp API atau pengiriman otomatis.
  - **Mencakup:** REQ-08.

- [ ] 13. Implementasikan laporan bulanan
  - Buat filter bulan dan tahun berdasarkan tanggal jadwal pelatihan.
  - Hitung jumlah pelatihan, seluruh pendaftar, peserta lunas, dan peserta belum bayar.
  - Hitung jumlah pendaftar early bird dan normal.
  - Hitung total tagihan, total pemasukan, dan sisa belum dibayar dari `Registration.priceAmount`.
  - Tampilkan ringkasan dan rincian per pelatihan.
  - Pastikan Admin dan User dapat mengakses laporan.
  - Tangani loading, data kosong, error, dan pembaruan data cloud.
  - **Mencakup:** REQ-09, REQ-11.

- [ ] 14. Implementasikan ekspor PDF dan Excel/XLSX
  - Bentuk dokumen PDF dari filter dan hasil laporan aktif.
  - Bentuk workbook XLSX dengan sheet ringkasan dan rincian per pelatihan.
  - Gunakan Android Storage Access Framework agar pengguna memilih lokasi file.
  - Terapkan nama file yang memuat bulan dan tahun laporan.
  - Laporkan keberhasilan hanya setelah penulisan file selesai.
  - Tangani pembatalan pemilih file serta kegagalan penulisan.
  - **Mencakup:** REQ-10.

- [ ] 15. Implementasikan menu Pengaturan dan identitas komunitas
  - Buat menu Pengaturan yang hanya terlihat dan dapat dibuka oleh Admin.
  - Buat formulir nama komunitas dengan validasi wajib dan maksimal 100 karakter.
  - Simpan nama komunitas dan metadata perubahan pada dokumen `appSettings/general`.
  - Tampilkan nama komunitas dari Firestore dan logo lokal pada layar login.
  - Gunakan nama `E-Pelatihan` sebagai fallback jika pengaturan belum tersedia.
  - Jangan menyediakan pemilih, upload, perubahan, atau penghapusan logo.
  - Catat perubahan nama komunitas dalam audit log.
  - **Mencakup:** REQ-11, REQ-12, REQ-13.

- [ ] 16. Lengkapi sinkronisasi, state aplikasi, dan audit
  - Pastikan semua layar memakai sumber data Firestore yang konsisten.
  - Tangani perubahan data dari perangkat lain dan konflik penulisan secara aman.
  - Tampilkan status proses dan error tanpa menyatakan operasi gagal sebagai sukses.
  - Pastikan operasi penting menghasilkan audit log yang diwajibkan.
  - Pastikan data lama tetap konsisten setelah harga master atau pelatihan diubah.
  - **Mencakup:** REQ-06, REQ-09, REQ-11, REQ-12, REQ-13.

- [ ] 17. Verifikasi penerimaan dan siapkan build aplikasi
  - Periksa setiap kriteria REQ-01 sampai REQ-13 pada akun Admin dan User.
  - Verifikasi User tidak dapat melakukan operasi Admin melalui UI maupun request langsung.
  - Verifikasi nama komunitas dan logo lokal tampil pada layar login sebelum autentikasi.
  - Verifikasi hanya Admin yang dapat mengubah nama komunitas dan tidak tersedia fitur perubahan logo.
  - Verifikasi data yang dibuat dari satu HP muncul pada HP lain dengan akun berbeda.
  - Verifikasi harga tersimpan tidak berubah ketika master harga diperbarui.
  - Verifikasi hanya peserta lunas yang dapat membuka pesan WhatsApp.
  - Verifikasi hasil perhitungan layar, PDF, dan XLSX sama untuk filter yang identik.
  - Periksa tidak ada secret, password, atau token backend di repositori dan log.
  - Buat build release setelah seluruh kriteria yang relevan terpenuhi.
