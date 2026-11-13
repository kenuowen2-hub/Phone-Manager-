package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

enum class ThemeMode(val storageKey: String, val label: String) {
    SYSTEM("system", "System default"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark");

    companion object {
        fun fromKey(key: String?): ThemeMode {
            return entries.firstOrNull { it.storageKey.equals(key, ignoreCase = true) } ?: SYSTEM
        }
    }
}

data class ThemePreset(
    val id: String,
    val name: String,
    val subtitle: String,
    val lightPrimary: Color,
    val lightAccent: Color,
    val darkPrimary: Color,
    val darkAccent: Color
)

data class ThemeConfig(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val presetId: String = PRESET_OCEAN,
    val customPrimaryArgb: Int = DEFAULT_CUSTOM_PRIMARY_ARGB,
    val customAccentArgb: Int = DEFAULT_CUSTOM_ACCENT_ARGB
) {
    val isCustom: Boolean get() = presetId == PRESET_CUSTOM

    fun effectivePrimary(isDark: Boolean): Color {
        if (isCustom) return Color(customPrimaryArgb)
        val preset = CoordinatedThemes.findById(presetId)
        return if (isDark) preset.darkPrimary else preset.lightPrimary
    }

    fun effectiveAccent(isDark: Boolean): Color {
        if (isCustom) return Color(customAccentArgb)
        val preset = CoordinatedThemes.findById(presetId)
        return if (isDark) preset.darkAccent else preset.lightAccent
    }

    companion object {
        const val PRESET_OCEAN = "ocean"
        const val PRESET_INDIGO = "indigo"
        const val PRESET_EMERALD = "emerald"
        const val PRESET_CORAL = "coral"
        const val PRESET_AMETHYST = "amethyst"
        const val PRESET_SLATE = "slate"
        const val PRESET_CUSTOM = "custom"

        const val DEFAULT_CUSTOM_PRIMARY_ARGB = 0xFF0284C7.toInt()
        const val DEFAULT_CUSTOM_ACCENT_ARGB = 0xFF0D9488.toInt()

        val DEFAULT = ThemeConfig()
    }
}

object CoordinatedThemes {
    val presets: List<ThemePreset> = listOf(
        ThemePreset(
            id = ThemeConfig.PRESET_OCEAN,
            name = "Ocean Guardian",
            subtitle = "Crisp sky blue & teal",
            lightPrimary = Color(0xFF0284C7),
            lightAccent = Color(0xFF0D9488),
            darkPrimary = Color(0xFF38BDF8),
            darkAccent = Color(0xFF2DD4BF)
        ),
        ThemePreset(
            id = ThemeConfig.PRESET_INDIGO,
            name = "Midnight Indigo",
            subtitle = "Deep indigo & rose",
            lightPrimary = Color(0xFF4338CA),
            lightAccent = Color(0xFFBE185D),
            darkPrimary = Color(0xFF818CF8),
            darkAccent = Color(0xFFF472B6)
        ),
        ThemePreset(
            id = ThemeConfig.PRESET_EMERALD,
            name = "Forest Emerald",
            subtitle = "Botanical green & amber",
            lightPrimary = Color(0xFF047857),
            lightAccent = Color(0xFFB45309),
            darkPrimary = Color(0xFF34D399),
            darkAccent = Color(0xFFFBBF24)
        ),
        ThemePreset(
            id = ThemeConfig.PRESET_CORAL,
            name = "Sunset Coral",
            subtitle = "Warm crimson & terracotta",
            lightPrimary = Color(0xFFBE123C),
            lightAccent = Color(0xFFC2410C),
            darkPrimary = Color(0xFFFB7185),
            darkAccent = Color(0xFFFB923C)
        ),
        ThemePreset(
            id = ThemeConfig.PRESET_AMETHYST,
            name = "Royal Amethyst",
            subtitle = "Violet & electric cyan",
            lightPrimary = Color(0xFF6D28D9),
            lightAccent = Color(0xFF0E7490),
            darkPrimary = Color(0xFFA78BFA),
            darkAccent = Color(0xFF22D3EE)
        ),
        ThemePreset(
            id = ThemeConfig.PRESET_SLATE,
            name = "Slate Graphite",
            subtitle = "Minimal charcoal & cobalt",
            lightPrimary = Color(0xFF334155),
            lightAccent = Color(0xFF1D4ED8),
            darkPrimary = Color(0xFF94A3B8),
            darkAccent = Color(0xFF60A5FA)
        )
    )

    fun findById(id: String?): ThemePreset {
        return presets.firstOrNull { it.id == id } ?: presets.first()
    }
}

object ThemeColorEngine {

    private val darkInk = Color(0xFF0B1220)
    private val lightInk = Color(0xFFFFFFFF)

    fun relativeLuminance(color: Color): Double {
        fun linearize(channel: Float): Double {
            val c = channel.toDouble().coerceIn(0.0, 1.0)
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        val r = linearize(color.red)
        val g = linearize(color.green)
        val b = linearize(color.blue)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    fun contrastRatio(foreground: Color, background: Color): Double {
        val l1 = relativeLuminance(foreground)
        val l2 = relativeLuminance(background)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    fun bestOnColor(background: Color): Color {
        val whiteContrast = contrastRatio(lightInk, background)
        val darkContrast = contrastRatio(darkInk, background)
        return if (whiteContrast >= darkContrast) lightInk else darkInk
    }

    /**
     * Adjusts [color] toward white (in dark mode) or toward deep ink (in light mode)
     * until it achieves at least [minRatio] contrast against [background].
     */
    fun ensureContrastAgainst(
        color: Color,
        background: Color,
        preferLighter: Boolean,
        minRatio: Double = 4.5
    ): Color {
        if (contrastRatio(color, background) >= minRatio) return color
        val target = if (preferLighter) Color.White else darkInk
        var low = 0f
        var high = 1f
        var best = target
        repeat(18) {
            val mid = (low + high) / 2f
            val candidate = lerp(color, target, mid)
            if (contrastRatio(candidate, background) >= minRatio) {
                best = candidate
                high = mid
            } else {
                low = mid
            }
        }
        return best
    }

    fun colorToHex(color: Color): String {
        val rgb = color.toArgb() and 0x00FFFFFF
        return String.format(java.util.Locale.US, "#%06X", rgb)
    }

    fun parseHexColor(raw: String): Color? {
        val cleaned = raw.trim().removePrefix("#")
        if (cleaned.length != 6) return null
        val rgb = cleaned.toIntOrNull(16) ?: return null
        return Color(0xFF000000.toInt() or rgb)
    }

    fun hsvToColor(hue: Float, saturation: Float, value: Float): Color {
        val h = ((hue % 360f) + 360f) % 360f
        val s = saturation.coerceIn(0f, 1f)
        val v = value.coerceIn(0f, 1f)
        val argb = android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))
        return Color(argb)
    }

    fun colorToHsv(color: Color): FloatArray {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
        return hsv
    }

    fun buildColorScheme(config: ThemeConfig, isDark: Boolean): ColorScheme {
        val rawPrimary = config.effectivePrimary(isDark)
        val rawAccent = config.effectiveAccent(isDark)

        return if (isDark) {
            val background = Color(0xFF0D1321)
            val surface = lerp(Color(0xFF151D2E), rawPrimary, 0.04f)
            val surfaceVariant = lerp(Color(0xFF232E45), rawPrimary, 0.08f)
            val surfaceHigh = lerp(Color(0xFF1D273B), rawPrimary, 0.06f)

            // Guarantee primary & secondary have >= 4.5:1 contrast when used as text/icons on dark surfaces
            val primary = ensureContrastAgainst(rawPrimary, surface, preferLighter = true, minRatio = 4.6)
            val onPrimary = bestOnColor(primary)
            val primaryContainer = lerp(surface, rawPrimary, 0.34f)
            val onPrimaryContainer = ensureContrastAgainst(
                lerp(rawPrimary, Color.White, 0.78f),
                primaryContainer,
                preferLighter = true,
                minRatio = 4.8
            )

            val secondary = ensureContrastAgainst(rawAccent, surface, preferLighter = true, minRatio = 4.6)
            val onSecondary = bestOnColor(secondary)
            val secondaryContainer = lerp(surface, rawAccent, 0.32f)
            val onSecondaryContainer = ensureContrastAgainst(
                lerp(rawAccent, Color.White, 0.78f),
                secondaryContainer,
                preferLighter = true,
                minRatio = 4.8
            )

            val tertiary = ensureContrastAgainst(Color(0xFFFBBF24), surface, preferLighter = true, minRatio = 4.5)
            val onTertiary = bestOnColor(tertiary)
            val tertiaryContainer = lerp(surface, tertiary, 0.28f)
            val onTertiaryContainer = bestOnColor(tertiaryContainer)

            val error = Color(0xFFFF6B81)
            val onError = bestOnColor(error)
            val errorContainer = Color(0xFF5C1524)
            val onErrorContainer = Color(0xFFFFD9DF)

            darkColorScheme(
                primary = primary,
                onPrimary = onPrimary,
                primaryContainer = primaryContainer,
                onPrimaryContainer = onPrimaryContainer,
                secondary = secondary,
                onSecondary = onSecondary,
                secondaryContainer = secondaryContainer,
                onSecondaryContainer = onSecondaryContainer,
                tertiary = tertiary,
                onTertiary = onTertiary,
                tertiaryContainer = tertiaryContainer,
                onTertiaryContainer = onTertiaryContainer,
                background = background,
                onBackground = Color(0xFFF1F5F9),
                surface = surface,
                onSurface = Color(0xFFF8FAFC),
                surfaceVariant = surfaceVariant,
                onSurfaceVariant = Color(0xFFCBD5E1),
                surfaceContainerLowest = background,
                surfaceContainerLow = surface,
                surfaceContainer = surfaceHigh,
                surfaceContainerHigh = surfaceVariant,
                surfaceContainerHighest = lerp(surfaceVariant, Color.White, 0.06f),
                outline = Color(0xFF64748B),
                outlineVariant = Color(0xFF334155),
                error = error,
                onError = onError,
                errorContainer = errorContainer,
                onErrorContainer = onErrorContainer
            )
        } else {
            val background = lerp(Color(0xFFF6F8FC), rawPrimary, 0.025f)
            val surface = Color(0xFFFFFFFF)
            val surfaceVariant = lerp(Color(0xFFEBF0F7), rawPrimary, 0.06f)
            val surfaceHigh = lerp(Color(0xFFF1F5FA), rawPrimary, 0.04f)

            // Guarantee primary & secondary have >= 4.5:1 contrast when used as text/icons on light surfaces
            val primary = ensureContrastAgainst(rawPrimary, surface, preferLighter = false, minRatio = 4.6)
            val onPrimary = bestOnColor(primary)
            val primaryContainer = lerp(Color.White, rawPrimary, 0.16f)
            val onPrimaryContainer = ensureContrastAgainst(
                lerp(rawPrimary, darkInk, 0.55f),
                primaryContainer,
                preferLighter = false,
                minRatio = 5.0
            )

            val secondary = ensureContrastAgainst(rawAccent, surface, preferLighter = false, minRatio = 4.6)
            val onSecondary = bestOnColor(secondary)
            val secondaryContainer = lerp(Color.White, rawAccent, 0.16f)
            val onSecondaryContainer = ensureContrastAgainst(
                lerp(rawAccent, darkInk, 0.55f),
                secondaryContainer,
                preferLighter = false,
                minRatio = 5.0
            )

            val tertiary = ensureContrastAgainst(Color(0xFFD97706), surface, preferLighter = false, minRatio = 4.5)
            val onTertiary = bestOnColor(tertiary)
            val tertiaryContainer = Color(0xFFFEF3C7)
            val onTertiaryContainer = Color(0xFF78350F)

            val error = Color(0xFFDC2626)
            val onError = Color.White
            val errorContainer = Color(0xFFFEE2E2)
            val onErrorContainer = Color(0xFF7F1D1D)

            lightColorScheme(
                primary = primary,
                onPrimary = onPrimary,
                primaryContainer = primaryContainer,
                onPrimaryContainer = onPrimaryContainer,
                secondary = secondary,
                onSecondary = onSecondary,
                secondaryContainer = secondaryContainer,
                onSecondaryContainer = onSecondaryContainer,
                tertiary = tertiary,
                onTertiary = onTertiary,
                tertiaryContainer = tertiaryContainer,
                onTertiaryContainer = onTertiaryContainer,
                background = background,
                onBackground = Color(0xFF0F172A),
                surface = surface,
                onSurface = Color(0xFF111827),
                surfaceVariant = surfaceVariant,
                onSurfaceVariant = Color(0xFF334155),
                surfaceContainerLowest = surface,
                surfaceContainerLow = background,
                surfaceContainer = surfaceHigh,
                surfaceContainerHigh = surfaceVariant,
                surfaceContainerHighest = lerp(surfaceVariant, darkInk, 0.05f),
                outline = Color(0xFF64748B),
                outlineVariant = Color(0xFFCBD5E1),
                error = error,
                onError = onError,
                errorContainer = errorContainer,
                onErrorContainer = onErrorContainer
            )
        }
    }
}
