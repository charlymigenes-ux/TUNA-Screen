package com.nopal.tunascreen.feature.machine.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nopal.tunascreen.core.model.JobProgress
import com.nopal.tunascreen.core.model.TempPair

/** Un composable por capability -- MachineScreen decide cuáles mostrar
 * consultando `machine.has(Capability.X)`, nunca a partir de la marca. */

@Composable
fun TemperatureCard(hotend: TempPair?, bed: TempPair?) {
    if (hotend == null && bed == null) return
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Temperaturas", style = MaterialTheme.typography.titleMedium)
            hotend?.let { Text("Hotend: ${it.current ?: "--"}°C / ${it.target ?: "--"}°C") }
            bed?.let { Text("Cama: ${it.current ?: "--"}°C / ${it.target ?: "--"}°C") }
        }
    }
}

@Composable
fun JobProgressCard(job: JobProgress?) {
    if (job == null) return
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(job.filename ?: "Trabajo en curso", style = MaterialTheme.typography.titleMedium)
            val percent = (job.percent ?: 0).coerceIn(0, 100)
            LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxWidth())
            Text("$percent%")
        }
    }
}

@Composable
fun TransportControls(onPause: () -> Unit, onResume: () -> Unit, onCancel: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = onPause) { Text("Pausar") }
        OutlinedButton(onClick = onResume) { Text("Reanudar") }
        Button(onClick = onCancel) { Text("Cancelar") }
    }
}

@Composable
fun MovementControls(onHome: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = onHome) { Text("Home") }
    }
}
