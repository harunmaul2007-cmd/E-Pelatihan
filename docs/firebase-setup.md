# Konfigurasi Firebase

Dokumen ini menjelaskan konfigurasi eksternal yang diperlukan oleh aplikasi E-Pelatihan. File credential/config environment tidak disimpan di repository.

## Identitas Android

- Application ID: `com.harunmaul.epelatihan`
- Minimum SDK: 26
- Target SDK: 36
- Paket Firebase: Spark

Build debug dan release memakai Application ID yang sama agar satu `google-services.json` dapat digunakan pada tahap awal. Build tetap dapat dibedakan melalui `BuildConfig.ENVIRONMENT` dan suffix versi debug.

## Langkah Firebase Console

1. Buat atau pilih Firebase project untuk lembaga.
2. Tambahkan Android app dengan package `com.harunmaul.epelatihan`.
3. Unduh `google-services.json`.
4. Simpan file tersebut secara lokal di `app/google-services.json`.
5. Jangan commit file tersebut. Pola `**/google-services.json` sudah masuk `.gitignore`.
6. Aktifkan Authentication dengan provider Email/Password.
7. Buat Cloud Firestore database pada region yang dipilih lembaga.

Setelah file tersedia, sinkronkan Gradle dan jalankan:

```powershell
.\gradlew.bat :app:assembleDebug
```

Build akan menerapkan plugin Google Services secara otomatis ketika `app/google-services.json` tersedia. `BuildConfig.FIREBASE_CONFIGURED` bernilai `true` hanya pada build yang dibuat dengan file tersebut.

## Struktur Cloud Firestore

```text
appSettings/general
users/{uid}
trainings/{trainingId}
participants/{participantId}
participantPhoneIndex/{phoneKey}
registrations/{registrationId}
whatsappOpenLogs/{logId}
auditLogs/{eventId}
```

Konstanta path berada di `data/firebase/FirestoreSchema.kt`. Model wire Firestore berada di `data/firebase/model/FirestoreDocuments.kt` dan mapper domain berada di `data/mapper/FirestoreMappers.kt`.

## Logo lokal

Project tetap menggunakan paket Firebase Spark dan tidak memakai Firebase Storage. Logo berasal dari `logo.png` yang dioptimasi menjadi 640×640 piksel, lalu dibundel di:

```text
app/src/main/res/drawable-nodpi/logo_epelatihan.png
```

Menu Pengaturan hanya menyimpan nama komunitas di Firestore. Aplikasi tidak mengunggah logo, PDF, rekaman, atau bukti pembayaran; fitur materi hanya menyimpan URL eksternal.

## Timestamp server

Field audit seperti `createdAt`, `updatedAt`, dan `openedAt` menggunakan server timestamp. Helper untuk update berbasis map tersedia di `ServerWriteMetadata`. Model dokumen menggunakan anotasi `@ServerTimestamp` untuk create berbasis object mapping.

## Konfigurasi yang tidak boleh masuk repository

- `google-services.json`
- Service account JSON
- Private key atau signing keystore
- Password SMTP/provider email
- Token Firebase CLI
- File `secrets.properties` dan `keystore.properties`

Credential backend nantinya disimpan melalui Secret Manager atau environment deployment, bukan dalam source code. Cloud Functions tidak dapat dideploy selama project tetap pada paket Spark; keputusan mekanisme undangan pengguna harus dibuat sebelum Task 5.

## Verifikasi koneksi Task 2

Task 2 dapat dinyatakan selesai setelah seluruh pemeriksaan berikut berhasil:

1. Build dengan `google-services.json` berhasil.
2. Task `processDebugGoogleServices` menghasilkan resource Firebase dan `BuildConfig.FIREBASE_CONFIGURED == true`.
3. Provider Email/Password merespons dan database Firestore `(default)` tersedia.
4. Project ID yang dihasilkan plugin sama dengan Firebase project milik lembaga.
5. Tidak ada file konfigurasi atau secret yang masuk Git.

Security Rules, indexes produksi, dan Cloud Functions diterapkan pada task berikutnya sesuai `tasks.md`.
