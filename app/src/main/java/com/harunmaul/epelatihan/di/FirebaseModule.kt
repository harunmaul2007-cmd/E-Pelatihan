package com.harunmaul.epelatihan.di

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.harunmaul.epelatihan.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {
    @Provides
    @Singleton
    fun provideFirebaseApp(
        @ApplicationContext context: Context,
    ): FirebaseApp {
        check(BuildConfig.FIREBASE_CONFIGURED) {
            "Firebase belum dikonfigurasi. Tambahkan app/google-services.json."
        }

        return FirebaseApp.getApps(context).firstOrNull()
            ?: checkNotNull(FirebaseApp.initializeApp(context)) {
                "Firebase gagal diinisialisasi dari google-services.json."
            }
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(firebaseApp: FirebaseApp): FirebaseAuth =
        FirebaseAuth.getInstance(firebaseApp)

    @Provides
    @Singleton
    fun provideFirestore(firebaseApp: FirebaseApp): FirebaseFirestore =
        FirebaseFirestore.getInstance(firebaseApp)

    @Provides
    @Singleton
    fun provideFirebaseStorage(firebaseApp: FirebaseApp): FirebaseStorage =
        FirebaseStorage.getInstance(firebaseApp)
}
