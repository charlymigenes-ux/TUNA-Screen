package com.nopal.tunascreen.feature.machine

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * TODO (fuera del alcance del MVP, ver plan): gráfica de temperaturas, jog
 * XYZ fino, flujo/velocidad, ventiladores, consola/eventos, macros,
 * sensores/relés/LEDs, material/Spoolman, telemetría, red y estado del
 * servidor. Solo el shell de navegación existe hoy -- se construye cuando
 * el MVP (Modo Taller + Modo Máquina base) esté validado en un taller real.
 */
@Composable
fun AdvancedMachineScreen(machineId: String, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = onBack) { Text("< Volver") }
        Text("Modo avanzado ($machineId)", style = MaterialTheme.typography.headlineSmall)
        Text("Todavía no implementado -- ver AdvancedMachineScreen.kt.")
    }
}
