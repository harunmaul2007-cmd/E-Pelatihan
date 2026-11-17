package com.harunmaul.epelatihan

import android.app.Application
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.FIREBASE_CONFIGURED && FirebaseApp.getApps(this).isEmpty()) {
            checkNotNull(FirebaseApp.initializeApp(this)) {
                "Firebase gagal diinisialisasi dari google-services.json."
            }
        }
    }
}
