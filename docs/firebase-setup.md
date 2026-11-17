# Konfigurasi Firebase

Dokumen ini menjelaskan konfigurasi eksternal yang diperlukan oleh aplikasi E-Pelatihan. File credential/config environment tidak disimpan di repository.

## Identitas Android

- Application ID: `com.harunmaul.epelatihan`
- Minimum SDK: 26
- Target SDK: 36

Build debug dan release memakai Application ID yang sama agar satu `google-services.json` dapat digunakan pada tahap awal. Build tetap dapat dibedakan melalui `BuildConfig.ENVIRONMENT` dan suffix versi debug.

## Langkah Firebase Console

1. Buat atau pilih Firebase project untuk lembaga.
2. Tambahkan Android app dengan package `com.harunmaul.epelatihan`.
3. Unduh `google-services.json`.
4. Simpan file tersebut secara lokal di `app/google-services.json`.
5. Jangan commit file tersebut. Pola `**/google-services.json` sudah masuk `.gitignore`.
6. Aktifkan Authentication dengan provider Email/Password.
7. Buat Cloud Firestore database pada region yang dipilih lembaga.
8. Aktifkan Firebase Storage pada region yang sesuai.

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

## Firebase Storage

Logo komunitas disimpan di:

```text
branding/logo/{nama-file-unik}
```

Tidak ada PDF, rekaman, atau bukti pembayaran yang diunggah aplikasi. Fitur tersebut hanya menyimpan URL eksternal.

## Timestamp server

Field audit seperti `createdAt`, `updatedAt`, dan `openedAt` menggunakan server timestamp. Helper untuk update berbasis map tersedia di `ServerWriteMetadata`. Model dokumen menggunakan anotasi `@ServerTimestamp` untuk create berbasis object mapping.

## Konfigurasi yang tidak boleh masuk repository

- `google-services.json`
- Service account JSON
- Private key atau signing keystore
- Password SMTP/provider email
- Token Firebase CLI
- File `secrets.properties` dan `keystore.properties`

Credential Cloud Functions nantinya disimpan melalui Secret Manager atau environment deployment, bukan dalam source code.

## Verifikasi koneksi Task 2

Task 2 dapat dinyatakan selesai setelah seluruh pemeriksaan berikut berhasil:

1. Build dengan `google-services.json` berhasil.
2. `FirebaseApp` berhasil diinisialisasi pada perangkat/emulator.
3. Instance Firebase Auth, Firestore, dan Storage dapat dibuat.
4. Project ID yang terbaca aplikasi sama dengan Firebase project milik lembaga.
5. Tidak ada file konfigurasi atau secret yang masuk Git.

Security Rules, indexes produksi, dan Cloud Functions diterapkan pada task berikutnya sesuai `tasks.md`.
