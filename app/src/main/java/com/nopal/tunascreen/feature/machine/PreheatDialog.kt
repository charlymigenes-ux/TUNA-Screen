package com.nopal.tunascreen.feature.machine

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Thermostat
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
import com.nopal.tunascreen.core.model.TemperaturePreset
import com.nopal.tunascreen.ui.theme.NopalBorder
import com.nopal.tunascreen.ui.theme.NopalGreen
import com.nopal.tunascreen.ui.theme.NopalSurface
import com.nopal.tunascreen.ui.theme.NopalSurfaceElevated
import com.nopal.tunascreen.ui.theme.NopalTextPrimary
import com.nopal.tunascreen.ui.theme.NopalTextSecondary
import com.nopal.tunascreen.ui.theme.NopalWarning
import kotlin.math.roundToInt

/**
 * Precalentar por material, igual que el popup de la web de NOPAL: cada ficha
 * aplica de una vez los perfiles configurados en NOPAL (sin "Aplicar" ni
 * "Apagar"; enfriar es una ficha más con 0°/0°).
 */
@Composable
internal fun PreheatDialog(viewModel: MachineViewModel, onDismiss: () -> Unit) {
    val presets by viewModel.presets.collectAsState()
    val machine by viewModel.machine.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadPresets() }

    val tiles = presets.map { Triple(it.name, it.extruder, it.heaterBed) } +
        Triple("ENFRIAR", 0.0, 0.0)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            color = NopalSurface,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, NopalGreen.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth(0.9f).widthIn(max = 520.dp),
        ) {
            Box {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier.size(36.dp).background(NopalSurfaceElevated, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Default.Thermostat, null, tint = NopalWarning, modifier = Modifier.size(20.dp)) }
                    Text(
                        "Precalentar por material",
                        color = NopalTextPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "${machine?.name ?: "Impresora"} · selecciona el material cargado",
                        color = NopalTextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    tiles.chunked(2).forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { (name, extruder, bed) ->
                                MaterialTile(name, extruder, bed, Modifier.weight(1f)) {
                                    viewModel.preheat(extruder, bed)
                                    onDismiss()
                                }
                            }
                            if (row.size == 1) Box(Modifier.weight(1f))
                        }
                    }
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
private fun MaterialTile(
    name: String,
    extruder: Double,
    bed: Double,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = NopalSurfaceElevated,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, NopalBorder),
        modifier = modifier.height(58.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(name, color = NopalWarning, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${extruder.roundToInt()}° boquilla", color = NopalTextPrimary, style = MaterialTheme.typography.labelSmall)
                Text("${bed.roundToInt()}° cama", color = NopalTextPrimary, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
