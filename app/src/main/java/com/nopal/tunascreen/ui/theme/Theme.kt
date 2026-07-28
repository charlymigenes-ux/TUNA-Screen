package com.nopal.tunascreen.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

enum class TunaThemeMode(
    val storageValue: String,
    val displayName: String,
    val previewColor: Color,
) {
    WHITE("white", "White", Color(0xFFF7F8FA)),
    BLACK("black", "Black", Color(0xFF050505)),
    NOPAL("nopal", "NOPAL", Color(0xFF22C55E)),
    RED("red", "Red", Color(0xFFE5484D)),
    DESERT("desert", "Desert", Color(0xFFC7AE78));

    companion object {
        fun fromStorage(value: String?): TunaThemeMode = when (value) {
            // Migración transparente de las tres opciones anteriores.
            "official" -> NOPAL
            "graphite", "high_contrast" -> BLACK
            else -> entries.firstOrNull { it.storageValue == value } ?: NOPAL
        }
    }
}

@Composable
fun TunaScreenTheme(
    themeMode: TunaThemeMode = TunaThemeMode.NOPAL,
    content: @Composable () -> Unit,
) {
    val colors = when (themeMode) {
        TunaThemeMode.WHITE -> WhiteColors
        TunaThemeMode.BLACK -> BlackColors
        TunaThemeMode.NOPAL -> OfficialNopalColors
        TunaThemeMode.RED -> RedColors
        TunaThemeMode.DESERT -> DesertColors
    }
    val materialColors = if (colors.isDark) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.black,
            secondary = colors.accent,
            onSecondary = colors.black,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.surfaceElevated,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.border,
            error = colors.danger,
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = colors.black,
            secondary = colors.accent,
            onSecondary = colors.black,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.surfaceElevated,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.border,
            error = colors.danger,
        )
    }

    CompositionLocalProvider(LocalTunaColors provides colors) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = Typography,
            content = content,
        )
    }
}
