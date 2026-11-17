package com.harunmaul.epelatihan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.harunmaul.epelatihan.core.designsystem.theme.EPelatihanTheme
import com.harunmaul.epelatihan.core.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            EPelatihanTheme {
                AppNavHost()
            }
        }
    }
}
