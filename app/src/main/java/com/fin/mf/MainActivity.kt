package com.fin.mf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fin.mf.core.theme.VaultThemeManager
import com.fin.mf.core.theme.VaultThemeProvider
import com.fin.mf.navigation.FinBabaNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val themeManager = VaultThemeManager.getInstance(applicationContext)
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
            VaultThemeProvider(themeManager = themeManager) {
                FinBabaNavHost(themeManager = themeManager)
            }
        }
    }
}