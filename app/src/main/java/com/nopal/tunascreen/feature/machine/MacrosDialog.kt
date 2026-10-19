package com.nopal.tunascreen.feature.machine

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nopal.tunascreen.core.model.MachineMacro
import com.nopal.tunascreen.ui.theme.NopalBorder
import com.nopal.tunascreen.ui.theme.NopalGreen
import com.nopal.tunascreen.ui.theme.NopalSurface
import com.nopal.tunascreen.ui.theme.NopalSurfaceElevated
import com.nopal.tunascreen.ui.theme.NopalTextPrimary
import com.nopal.tunascreen.ui.theme.NopalTextSecondary
import com.nopal.tunascreen.ui.theme.NopalWarning

/** Mismo criterio que la web de NOPAL: "PRINT_START" se lee "Print Start". */
internal fun macroLabel(name: String): String =
    name.replace('_', ' ').lowercase().split(' ').joinToString(" ") { word ->
        word.replaceFirstChar { it.uppercase() }
    }

// Descripciones por patrón de nombre (las mismas que muestra la web de NOPAL
// cuando el macro no trae `description:` en el printer.cfg).
private val MACRO_DESCRIPTIONS: List<Pair<Regex, String>> = listOf(
    Regex("^PAUSE$") to "Pausa la impresión actual.",
    Regex("^RESUME$") to "Reanuda la impresión pausada.",
    Regex("CANCEL_PRINT") to "Cancela la impresión en curso.",
    Regex("UNLOAD_FILAMENT") to "Retira el filamento del extrusor.",
    Regex("LOAD_FILAMENT") to "Carga el filamento en el extrusor.",
    Regex("FILAMENT_CHANGE|^M600$") to "Pausa la impresión para cambiar el filamento.",
    Regex("PID_(TUNE|CALIBRATE)") to "Calibra el PID de temperatura.",
    Regex("BED_MESH") to "Calibra la malla de la cama.",
    Regex("Z_TILT") to "Ajusta la inclinación del eje Z.",
    Regex("QUAD_GANTRY_LEVEL|^G32$") to "Nivela el pórtico (gantry).",
    Regex("^(HOME_ALL|G28)$") to "Lleva todos los ejes a su posición de home.",
    Regex("PARK") to "Estaciona el cabezal en una posición segura.",
    Regex("PREHEAT") to "Precalienta la impresora.",
    Regex("COOL_?DOWN") to "Enfría la cama y el extrusor.",
    Regex("CLEAN_NOZZLE|NOZZLE_CLEAN") to "Limpia la boquilla.",
    Regex("START_PRINT|PRINT_START") to "Rutina de inicio de impresión.",
    Regex("END_PRINT|PRINT_END") to "Rutina de fin de impresión.",
    Regex("^BEEP$") to "Emite un sonido con el zumbador.",
    Regex("LIGHT") to "Enciende o apaga la iluminación.",
    Regex("SAVE_CONFIG") to "Guarda la configuración actual en printer.cfg.",
    Regex("TEST_SPEED") to "Corre una prueba de velocidad de movimiento.",
    Regex("CALIBRATE") to "Corre una rutina de calibración.",
)

internal fun macroDescription(macro: MachineMacro): String =
    macro.description.takeIf { it.isNotBlank() }
        ?: MACRO_DESCRIPTIONS.firstOrNull { (pattern, _) -> pattern.containsMatchIn(macro.name.uppercase()) }?.second
        ?: "Macro de Klipper."

/**
 * Macros de la impresora, los mismos que lista la web de NOPAL (los
 * `gcode_macro` de Klipper sin guion bajo inicial), en fichas con nombre y
 * descripción. Un macro puede mover o calentar la máquina, así que tocar una
 * ficha pide confirmar antes de ejecutarla.
 */
@Composable
internal fun MacrosDialog(viewModel: MachineViewModel, onDismiss: () -> Unit) {
    val macros by viewModel.macros.collectAsState()
    val loading by viewModel.macrosLoading.collectAsState()
    val failed by viewModel.macrosFailed.collectAsState()
    val machine by viewModel.machine.collectAsState()
    var pending by remember { mutableStateOf<MachineMacro?>(null) }
    LaunchedEffect(Unit) { viewModel.loadMacros() }

    pending?.let { macro ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("¿Ejecutar ${macroLabel(macro.name)}?") },
            text = { Text(macroDescription(macro) + "\n\nPuede mover o calentar la máquina.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.runMacro(macro.name)
                    pending = null
                    onDismiss()
                }) { Text("EJECUTAR") }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("NO EJECUTAR") } },
        )
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            color = NopalSurface,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, NopalGreen.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth(0.92f).widthIn(max = 720.dp).fillMaxHeight(0.92f),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Macros",
                            color = NopalTextPrimary,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "${machine?.name ?: "Impresora"} · ${macros.size} disponibles",
                            color = NopalTextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    IconButton(onClick = viewModel::loadMacros, enabled = !loading) {
                        Icon(Icons.Default.Refresh, "Actualizar macros", tint = NopalGreen)
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp).background(NopalWarning, CircleShape),
                    ) { Icon(Icons.Default.Close, "Cerrar", tint = NopalSurface, modifier = Modifier.size(18.dp)) }
                }
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    when {
                        loading && macros.isEmpty() -> CircularProgressIndicator(color = NopalGreen)
                        failed && macros.isEmpty() -> Text(
                            "No se pudieron leer los macros. Revisa que la impresora esté en línea y que NOPAL esté actualizado.",
                            color = NopalWarning,
                        )
                        macros.isEmpty() -> Text(
                            "Esta impresora no tiene macros en Klipper (los que empiezan con _ no se muestran).",
                            color = NopalTextSecondary,
                        )
                        else -> Column(
                            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            macros.chunked(2).forEach { row ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEach { macro ->
                                        MacroTile(macro, Modifier.weight(1f)) { pending = macro }
                                    }
                                    if (row.size == 1) Box(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroTile(macro: MachineMacro, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = NopalSurfaceElevated,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, NopalBorder),
        modifier = modifier.heightIn(min = 72.dp),
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Bolt, null, tint = NopalGreen, modifier = Modifier.size(16.dp))
                Text(
                    macroLabel(macro.name),
                    color = NopalTextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                macroDescription(macro),
                color = NopalTextSecondary,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
