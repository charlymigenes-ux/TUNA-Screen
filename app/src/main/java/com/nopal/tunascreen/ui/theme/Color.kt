package com.nopal.tunascreen.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class TunaColors(
    val isDark: Boolean,
    val background: Color,
    val chrome: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val border: Color,
    val accent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val warning: Color,
    val danger: Color,
    val black: Color,
)

// Paletas alineadas con los temas del panel web de NOPAL.
internal val WhiteColors = TunaColors(
    isDark = false,
    background = Color(0xFFF7F8FA),
    chrome = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFF1F4F7),
    border = Color(0xFFD8DEE5),
    accent = Color(0xFF22C55E),
    textPrimary = Color(0xFF1E293B),
    textSecondary = Color(0xFF64748B),
    warning = Color(0xFFF59E0B),
    danger = Color(0xFFDC2626),
    black = Color(0xFF052E16),
)

internal val BlackColors = TunaColors(
    isDark = true,
    background = Color(0xFF050505),
    chrome = Color(0xFF050505),
    surface = Color(0xFF181818),
    surfaceElevated = Color(0xFF242424),
    border = Color(0xFF2F2F2F),
    accent = Color(0xFF22C55E),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFB8B8B8),
    warning = Color(0xFFF59E0B),
    danger = Color(0xFFFF453A),
    black = Color(0xFF04150A),
)

internal val OfficialNopalColors = TunaColors(
    isDark = true,
    background = Color(0xFF0F172A),
    chrome = Color(0xFF0B111B),
    surface = Color(0xFF171D23),
    surfaceElevated = Color(0xFF11161B),
    border = Color(0xFF2B3542),
    accent = Color(0xFF22C55E),
    textPrimary = Color(0xFFF5F7FA),
    textSecondary = Color(0xFFB7C1CC),
    warning = Color(0xFFF59E0B),
    danger = Color(0xFFFF453A),
    black = Color(0xFF04150A),
)

internal val RedColors = TunaColors(
    isDark = true,
    background = Color(0xFF120809),
    chrome = Color(0xFF120809),
    surface = Color(0xFF2E171A),
    surfaceElevated = Color(0xFF241214),
    border = Color(0xFF4A2326),
    accent = Color(0xFFE5484D),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFD2B8BA),
    warning = Color(0xFFF59E0B),
    danger = Color(0xFFFF6B70),
    black = Color(0xFFFFFFFF),
)

internal val DesertColors = TunaColors(
    isDark = false,
    background = Color(0xFFE8D8B5),
    chrome = Color(0xFFFDF8EC),
    surface = Color(0xFFF5EBD4),
    surfaceElevated = Color(0xFFD8C69D),
    border = Color(0xFF9A8258),
    accent = Color(0xFF687A32),
    textPrimary = Color(0xFF30291F),
    textSecondary = Color(0xFF6B5E49),
    warning = Color(0xFFB66A1F),
    danger = Color(0xFFA63D32),
    black = Color(0xFFFDF8EC),
)

internal val LocalTunaColors = staticCompositionLocalOf { OfficialNopalColors }

val NopalBackground: Color
    @Composable @ReadOnlyComposable get() = LocalTunaColors.current.background
val NopalChrome: Color
    @Composable @ReadOnlyComposable get() = LocalTunaColors.current.chrome
val NopalSurface: Color
    @Composable @ReadOnlyComposable get() = LocalTunaColors.current.surface
val NopalSurfaceElevated: Color
    @Composable @ReadOnlyComposable get() = LocalTunaColors.current.surfaceElevated
val NopalBorder: Color
    @Composable @ReadOnlyComposable get() = LocalTunaColors.current.border
val NopalGreen: Color
    @Composable @ReadOnlyComposable get() = LocalTunaColors.current.accent
val NopalTextPrimary: Color
    @Composable @ReadOnlyComposable get() = LocalTunaColors.current.textPrimary
val NopalTextSecondary: Color
    @Composable @ReadOnlyComposable get() = LocalTunaColors.current.textSecondary
val NopalWarning: Color
    @Composable @ReadOnlyComposable get() = LocalTunaColors.current.warning
val NopalDanger: Color
    @Composable @ReadOnlyComposable get() = LocalTunaColors.current.danger
val NopalBlack: Color
    @Composable @ReadOnlyComposable get() = LocalTunaColors.current.black
