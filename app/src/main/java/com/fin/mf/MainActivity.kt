package com.fin.mf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fin.mf.ui.FinBabaApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                scrim = android.graphics.Color.WHITE,
                darkScrim = android.graphics.Color.WHITE
            ),
            navigationBarStyle = SystemBarStyle.light(
                scrim = android.graphics.Color.WHITE,
                darkScrim = android.graphics.Color.WHITE
            )
        )
        setContent {
            FinBabaApp()
        }
    }
}