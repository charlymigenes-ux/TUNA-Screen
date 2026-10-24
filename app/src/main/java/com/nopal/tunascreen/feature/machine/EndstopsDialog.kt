package com.nopal.tunascreen.feature.machine

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nopal.tunascreen.ui.theme.NopalBorder
import com.nopal.tunascreen.ui.theme.NopalGreen
import com.nopal.tunascreen.ui.theme.NopalSurface
import com.nopal.tunascreen.ui.theme.NopalSurfaceElevated
import com.nopal.tunascreen.ui.theme.NopalTextPrimary
import com.nopal.tunascreen.ui.theme.NopalTextSecondary
import com.nopal.tunascreen.ui.theme.NopalWarning
import kotlinx.coroutines.delay

private const val ENDSTOPS_REFRESH_MS = 1_000L

/** Klipper reporta "TRIGGERED" cuando el límite está presionado y "open" cuando no. */
internal fun isEndstopTriggered(state: String): Boolean = state.equals("triggered", ignoreCase = true)

/**
 * Revisión de límites de carrera: lee el estado real cada segundo mientras está
 * abierto, para poder presionar cada límite con la mano y ver que cambia. No
 * envía G-code ni mueve nada.
 */
@Composable
internal fun EndstopsDialog(viewModel: MachineViewModel, onDismiss: () -> Unit) {
    val endstops by viewModel.endstops.collectAsState()
    val failed by viewModel.endstopsFailed.collectAsState()
    val machine by viewModel.machine.collectAsState()
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.loadEndstops()
            delay(ENDSTOPS_REFRESH_MS)
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            color = NopalSurface,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, NopalGreen.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth(0.9f).widthIn(max = 560.dp),
        ) {
            Box {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        "Límites de carrera",
                        color = NopalTextPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "${machine?.name ?: "Impresora"} · se actualiza cada segundo",
                        color = NopalTextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    when {
                        endstops.isEmpty() && failed -> Text(
                            "No se pudo leer el estado. Revisa que la impresora esté en línea y que NOPAL esté actualizado.",
                            color = NopalWarning,
                        )
                        endstops.isEmpty() -> Text("Leyendo…", color = NopalTextSecondary)
                        else -> endstops.toSortedMap().forEach { (axis, state) ->
                            EndstopRow(axis.uppercase(), isEndstopTriggered(state))
                        }
                    }
                    Text(
                        "ACTIVADO significa que el límite está presionado. Con el eje en home debe salir " +
                            "ACTIVADO; presiona cada límite con la mano para comprobar que cambia.",
                        color = NopalTextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(36.dp)
                        .background(NopalWarning, CircleShape),
                ) { Icon(Icons.Default.Close, "Cerrar", tint = NopalSurface, modifier = Modifier.size(18.dp)) }
            }
        }
    }
}

@Composable
private fun EndstopRow(axis: String, triggered: Boolean) {
    val tone = if (triggered) NopalWarning else NopalGreen
    Surface(
        color = NopalSurfaceElevated,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (triggered) NopalWarning.copy(alpha = 0.7f) else NopalBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(12.dp).background(tone, CircleShape))
            Text(
                "Eje $axis",
                color = NopalTextPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text(
                if (triggered) "ACTIVADO" else "LIBRE",
                color = tone,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
