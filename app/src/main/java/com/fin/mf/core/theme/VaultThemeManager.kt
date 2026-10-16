package com.fin.mf.core.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * High-contrast 2-color gradient definition for FinBaba brand identity.
 */
data class VaultGradient(
    val id: String,
    val displayName: String,
    val startColor: Color,
    val endColor: Color
) {
    val brush: Brush
        get() = Brush.linearGradient(listOf(startColor, endColor))

    val horizontalBrush: Brush
        get() = Brush.horizontalGradient(listOf(startColor, endColor))

    val subtleBorderBrush: Brush
        get() = Brush.linearGradient(listOf(startColor.copy(alpha = 0.5f), endColor.copy(alpha = 0.3f)))
}

/**
 * Curated High-Contrast Presets specified for FinBaba Studio.
 */
object VaultThemePresets {
    val COBALT_SKY = VaultGradient(
        id = "cobalt_sky",
        displayName = "Cobalt Sky",
        startColor = Color(0xFF1E56F0),
        endColor = Color(0xFF0A1633)
    )

    val ROYAL_VIOLET = VaultGradient(
        id = "royal_violet",
        displayName = "Royal Violet",
        startColor = Color(0xFF8B5CF6),
        endColor = Color(0xFF2E1065)
    )

    val CYBER_CYAN = VaultGradient(
        id = "cyber_cyan",
        displayName = "Cyber Cyan",
        startColor = Color(0xFF00C4D6),
        endColor = Color(0xFF083344)
    )

    val EMERALD_WEALTH = VaultGradient(
        id = "emerald_wealth",
        displayName = "Emerald Wealth",
        startColor = Color(0xFF10B981),
        endColor = Color(0xFF064E3B)
    )

    val SUNSET_AMBER = VaultGradient(
        id = "sunset_amber",
        displayName = "Sunset Amber",
        startColor = Color(0xFFF59E0B),
        endColor = Color(0xFF451A03)
    )

    val CRIMSON_TITAN = VaultGradient(
        id = "crimson_titan",
        displayName = "Crimson Titan",
        startColor = Color(0xFFE11D48),
        endColor = Color(0xFF4C0519)
    )

    val all: List<VaultGradient> = listOf(
        COBALT_SKY,
        ROYAL_VIOLET,
        CYBER_CYAN,
        EMERALD_WEALTH,
        SUNSET_AMBER,
        CRIMSON_TITAN
    )

    fun fromId(id: String?): VaultGradient {
        return all.find { it.id.equals(id, ignoreCase = true) } ?: COBALT_SKY
    }
}

/**
 * App-wide static CompositionLocal holding the currently active brand gradient.
 */
val LocalVaultGradient = staticCompositionLocalOf { VaultThemePresets.COBALT_SKY }

/**
 * Persistent theme coordinator storing the selected preset in SharedPreferences
 * and exposing a reactive StateFlow with instant disk write.
 */
class VaultThemeManager private constructor(context: Context) {

    companion object {
        private const val PREFS_NAME = "finbaba_prefs"
        private const val KEY_VAULT_THEME = "KEY_VAULT_THEME"

        @Volatile
        private var instance: VaultThemeManager? = null

        fun getInstance(context: Context): VaultThemeManager {
            return instance ?: synchronized(this) {
                instance ?: VaultThemeManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _currentGradient = MutableStateFlow(
        VaultThemePresets.fromId(prefs.getString(KEY_VAULT_THEME, VaultThemePresets.COBALT_SKY.id))
    )
    val currentGradient: StateFlow<VaultGradient> = _currentGradient.asStateFlow()

    fun setGradientPreset(preset: VaultGradient) {
        _currentGradient.value = preset
        prefs.edit().putString(KEY_VAULT_THEME, preset.id).apply()
    }
}

/**
 * Root composition provider binding LocalVaultGradient to the active theme flow.
 */
@Composable
fun VaultThemeProvider(
    themeManager: VaultThemeManager,
    content: @Composable () -> Unit
) {
    val currentGradient by themeManager.currentGradient.collectAsState()
    CompositionLocalProvider(LocalVaultGradient provides currentGradient) {
        content()
    }
}
