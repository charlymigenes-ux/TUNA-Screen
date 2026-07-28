package com.nopal.tunascreen.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import kotlin.math.pow
import kotlin.math.sqrt

enum class DisplayModeOverride(val storageValue: String, val displayName: String) {
    AUTO("auto", "Automático"),
    MACHINE_35("machine_35", "3.5″ Máquina"),
    MACHINE_43("machine_43", "4.3″ Máquina"),
    ADVANCED_5("advanced_5", "5″ Avanzado"),
    MACHINE_7("machine_7", "7″ Máquina / multi"),
    WORKSHOP_10("workshop_10", "10″ Taller"),
    DESKTOP("desktop", "PC Dashboard");

    companion object {
        fun fromStorage(value: String?): DisplayModeOverride =
            entries.firstOrNull { it.storageValue == value } ?: AUTO
    }
}

enum class DisplayProfile {
    MACHINE_35,
    MACHINE_43,
    ADVANCED_5,
    MACHINE_7,
    WORKSHOP_10,
    DESKTOP;

    val isCompactMachine: Boolean get() = this == MACHINE_35 || this == MACHINE_43
    val usesAdvancedMachine: Boolean get() = !isCompactMachine
}

@Immutable
data class DisplayProfileInfo(
    val profile: DisplayProfile,
    val estimatedDiagonalInches: Double?,
)

val LocalDisplayProfile = staticCompositionLocalOf {
    DisplayProfileInfo(DisplayProfile.MACHINE_7, null)
}

@Composable
fun ProvideDisplayProfile(
    override: DisplayModeOverride,
    content: @Composable () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val metrics = LocalContext.current.resources.displayMetrics
    val info = remember(
        override,
        configuration.screenWidthDp,
        configuration.screenHeightDp,
        metrics.widthPixels,
        metrics.heightPixels,
        metrics.xdpi,
        metrics.ydpi,
    ) {
        val diagonal = if (metrics.xdpi > 50f && metrics.ydpi > 50f) {
            sqrt(
                (metrics.widthPixels / metrics.xdpi).toDouble().pow(2) +
                    (metrics.heightPixels / metrics.ydpi).toDouble().pow(2),
            )
        } else {
            null
        }
        val automatic = diagonal?.let(::profileForDiagonal)
            ?: profileForDp(minOf(configuration.screenWidthDp, configuration.screenHeightDp))
        val profile = when (override) {
            DisplayModeOverride.AUTO -> automatic
            DisplayModeOverride.MACHINE_35 -> DisplayProfile.MACHINE_35
            DisplayModeOverride.MACHINE_43 -> DisplayProfile.MACHINE_43
            DisplayModeOverride.ADVANCED_5 -> DisplayProfile.ADVANCED_5
            DisplayModeOverride.MACHINE_7 -> DisplayProfile.MACHINE_7
            DisplayModeOverride.WORKSHOP_10 -> DisplayProfile.WORKSHOP_10
            DisplayModeOverride.DESKTOP -> DisplayProfile.DESKTOP
        }
        DisplayProfileInfo(profile, diagonal)
    }

    CompositionLocalProvider(LocalDisplayProfile provides info, content = content)
}

private fun profileForDiagonal(inches: Double): DisplayProfile = when {
    inches < 3.9 -> DisplayProfile.MACHINE_35
    inches < 4.7 -> DisplayProfile.MACHINE_43
    inches < 6.0 -> DisplayProfile.ADVANCED_5
    inches < 8.5 -> DisplayProfile.MACHINE_7
    inches < 13.0 -> DisplayProfile.WORKSHOP_10
    else -> DisplayProfile.DESKTOP
}

private fun profileForDp(shortestDp: Int): DisplayProfile = when {
    shortestDp < 320 -> DisplayProfile.MACHINE_35
    shortestDp < 400 -> DisplayProfile.MACHINE_43
    shortestDp < 520 -> DisplayProfile.ADVANCED_5
    shortestDp < 720 -> DisplayProfile.MACHINE_7
    shortestDp < 1_000 -> DisplayProfile.WORKSHOP_10
    else -> DisplayProfile.DESKTOP
}
