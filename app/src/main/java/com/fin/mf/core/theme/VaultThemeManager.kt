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
 * 4 Selectable bespoke credit card hardware finishes.
 */
enum class CardHardwareVariant(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val startColor: Color,
    val endColor: Color,
    val accentColor: Color
) {
    TITANIUM_BLACK(
        id = "titanium_black",
        displayName = "Titanium Black",
        subtitle = "Matte Carbon Slate // Brushed Silver",
        startColor = Color(0xFF1E293B),
        endColor = Color(0xFF0F172A),
        accentColor = Color(0xFF94A3B8)
    ),
    NEO_GOLD(
        id = "neo_gold",
        displayName = "Neo Gold",
        subtitle = "Rich Champagne Gold // Warm Specular",
        startColor = Color(0xFFD97706),
        endColor = Color(0xFF78350F),
        accentColor = Color(0xFFFDE68A)
    ),
    CYBER_EMERALD(
        id = "cyber_emerald",
        displayName = "Cyber Emerald",
        subtitle = "Deep Institutional Emerald // Neon Teal",
        startColor = Color(0xFF065F46),
        endColor = Color(0xFF022C22),
        accentColor = Color(0xFF2DD4BF)
    ),
    ROYAL_SAPPHIRE(
        id = "royal_sapphire",
        displayName = "Royal Sapphire",
        subtitle = "Classic Midnight Blue // Cobalt Sheen",
        startColor = Color(0xFF1E40AF),
        endColor = Color(0xFF0F172A),
        accentColor = Color(0xFF60A5FA)
    ),
    RUBY_OBSIDIAN(
        id = "ruby_obsidian",
        displayName = "Ruby Obsidian",
        subtitle = "Imperial Crimson Red // Obsidian Weave",
        startColor = Color(0xFF881337),
        endColor = Color(0xFF0F172A),
        accentColor = Color(0xFFFB7185)
    );

    val brush: Brush
        get() = Brush.linearGradient(listOf(startColor, endColor))

    companion object {
        val all: List<CardHardwareVariant> = entries.toList()

        fun fromId(id: String?): CardHardwareVariant =
            entries.find { it.id.equals(id, ignoreCase = true) } ?: TITANIUM_BLACK
    }
}

/**
 * App-wide static CompositionLocal holding the currently active card hardware variant.
 */
val LocalCardHardware = staticCompositionLocalOf { CardHardwareVariant.TITANIUM_BLACK }

/**
 * Persistent theme coordinator storing the selected preset in SharedPreferences
 * and exposing a reactive StateFlow with instant disk write.
 */
class VaultThemeManager private constructor(context: Context) {

    companion object {
        private const val PREFS_NAME = "finbaba_prefs"
        private const val KEY_VAULT_THEME = "KEY_VAULT_THEME"
        private const val KEY_CARD_HARDWARE = "KEY_CARD_HARDWARE"

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

    private val _currentHardware = MutableStateFlow(
        CardHardwareVariant.fromId(prefs.getString(KEY_CARD_HARDWARE, CardHardwareVariant.TITANIUM_BLACK.id))
    )
    val currentHardware: StateFlow<CardHardwareVariant> = _currentHardware.asStateFlow()

    fun setGradientPreset(preset: VaultGradient) {
        _currentGradient.value = preset
        prefs.edit().putString(KEY_VAULT_THEME, preset.id).apply()
    }

    fun setCardHardwareVariant(variant: CardHardwareVariant) {
        _currentHardware.value = variant
        prefs.edit().putString(KEY_CARD_HARDWARE, variant.id).apply()
    }
}

/**
 * Root composition provider binding LocalVaultGradient and LocalCardHardware to the active theme flow.
 */
@Composable
fun VaultThemeProvider(
    themeManager: VaultThemeManager,
    content: @Composable () -> Unit
) {
    val currentGradient by themeManager.currentGradient.collectAsState()
    val currentHardware by themeManager.currentHardware.collectAsState()
    CompositionLocalProvider(
        LocalVaultGradient provides currentGradient,
        LocalCardHardware provides currentHardware
    ) {
        content()
    }
}

