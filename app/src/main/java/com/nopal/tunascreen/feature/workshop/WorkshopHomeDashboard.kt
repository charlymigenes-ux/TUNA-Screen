package com.nopal.tunascreen.feature.workshop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nopal.tunascreen.core.model.Capability
import com.nopal.tunascreen.core.model.Machine
import com.nopal.tunascreen.core.model.MaterialsResponse
import com.nopal.tunascreen.core.model.AccessoriesResponse
import com.nopal.tunascreen.core.repository.ConnectionState
import com.nopal.tunascreen.ui.theme.NopalBorder
import com.nopal.tunascreen.ui.theme.NopalDanger
import com.nopal.tunascreen.ui.theme.NopalGreen
import com.nopal.tunascreen.ui.theme.NopalSurface
import com.nopal.tunascreen.ui.theme.NopalSurfaceElevated
import com.nopal.tunascreen.ui.theme.NopalTextPrimary
import com.nopal.tunascreen.ui.theme.NopalTextSecondary
import com.nopal.tunascreen.ui.theme.NopalWarning
import com.nopal.tunascreen.feature.workshop.components.CameraStreamView

@Composable
internal fun WorkshopHomeDashboard(
    machines: List<Machine>,
    connection: ConnectionState,
    onMachineSelected: (String) -> Unit,
    onNavigate: (String) -> Unit,
    onPause: (String) -> Unit,
    onCancel: (String) -> Unit,
    cameraBaseUrl: String?,
    cameraToken: String?,
    materials: MaterialsResponse,
    accessories: AccessoriesResponse,
) {
    val activeMachine = remember(machines) {
        machines.firstOrNull { it.status.state in setOf("printing", "running", "working", "paused") }
    }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        WorkshopOverview(machines, connection)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(machines, key = { it.id }) { machine ->
                HomeMachineCard(machine, onClick = { onMachineSelected(machine.id) })
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(230.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ActiveJobPanel(
                machine = activeMachine,
                onOpen = { activeMachine?.let { onMachineSelected(it.id) } },
                onPause = { activeMachine?.let { onPause(it.id) } },
                onCancel = { activeMachine?.let { onCancel(it.id) } },
                modifier = Modifier.weight(1f),
            )
            CameraOverview(
                machine = machines.firstOrNull { it.has(Capability.CAMERA) },
                onOpen = { onNavigate("cameras") },
                baseUrl = cameraBaseUrl,
                token = cameraToken,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(145.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MaterialOverview(activeMachine, materials, { onNavigate("materials") }, modifier = Modifier.weight(1f))
            AccessoriesOverview(accessories, { onNavigate("accessories") }, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun WorkshopOverview(machines: List<Machine>, connection: ConnectionState) {
    val printing = machines.count { it.status.state in setOf("printing", "running", "working") }
    val paused = machines.count { it.status.state == "paused" }
    val errors = machines.count { it.status.state == "error" }
    val ready = machines.count { it.online && it.status.state in setOf("idle", "ready", "standby") }
    DashboardPanel(title = "RESUMEN DEL TALLER", modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text("${machines.size}", color = NopalGreen, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("máquinas\nregistradas", color = NopalTextPrimary)
            SummaryValue(printing.toString(), "trabajando", Color(0xFF27BDEB))
            SummaryValue(ready.toString(), "listas", NopalGreen)
            SummaryValue(paused.toString(), "en pausa", NopalWarning)
            SummaryValue(errors.toString(), "error", NopalDanger)
            Spacer(Modifier.weight(1f))
            Text(
                if (connection == ConnectionState.LIVE) "NOPAL CONECTADO" else connection.label().uppercase(),
                color = if (connection == ConnectionState.LIVE) NopalGreen else NopalWarning,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SummaryValue(value: String, label: String, color: Color) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(value, color = color, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(" $label", color = NopalTextSecondary)
    }
}

@Composable
private fun HomeMachineCard(machine: Machine, onClick: () -> Unit) {
    val color = homeStatusColor(machine)
    Surface(
        onClick = onClick,
        color = NopalSurface,
        shape = RoundedCornerShape(9.dp),
        border = BorderStroke(1.dp, NopalBorder),
        modifier = Modifier.width(260.dp).height(112.dp),
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(Modifier.width(7.dp).fillMaxHeight().background(color))
            Column(Modifier.padding(10.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Power, null, tint = color, modifier = Modifier.size(20.dp))
                    Text("  ${machine.name}", color = NopalTextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(if (machine.online) machine.status.state.uppercase() else "OFFLINE", color = color, style = MaterialTheme.typography.labelSmall)
                }
                Text(machine.type.displayName(), color = NopalTextSecondary, style = MaterialTheme.typography.labelSmall)
                machine.status.job?.let { job ->
                    LinearProgressIndicator(
                        progress = { (job.percent ?: 0).coerceIn(0, 100) / 100f },
                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                        color = color,
                        trackColor = NopalSurfaceElevated,
                    )
                    Text("${job.percent ?: 0}%  ${job.filename.orEmpty()}", color = color, style = MaterialTheme.typography.labelSmall)
                } ?: Text(machineSummary(machine), color = NopalTextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ActiveJobPanel(
    machine: Machine?,
    onOpen: () -> Unit,
    onPause: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier,
) {
    DashboardPanel("TRABAJO ACTIVO", modifier) {
        if (machine?.status?.job == null) {
            EmptyDashboardMessage("No hay trabajos activos")
            return@DashboardPanel
        }
        val job = machine.status.job
        Text(machine.name, color = NopalTextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(job.filename ?: "Trabajo", color = NopalTextSecondary)
        Text("${job.percent ?: 0}%", color = Color(0xFF27BDEB), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        LinearProgressIndicator(
            progress = { (job.percent ?: 0).coerceIn(0, 100) / 100f },
            modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
            color = Color(0xFF27BDEB),
            trackColor = NopalSurfaceElevated,
        )
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onPause, enabled = machine.supports("pause")) { Icon(Icons.Default.Pause, null); Text(" Pausar") }
            OutlinedButton(onClick = onCancel, enabled = machine.supports("cancel")) { Icon(Icons.Default.Stop, null); Text(" Cancelar") }
            OutlinedButton(onClick = onOpen) { Text("Abrir") }
        }
    }
}

@Composable
private fun CameraOverview(
    machine: Machine?,
    onOpen: () -> Unit,
    baseUrl: String?,
    token: String?,
    modifier: Modifier,
) {
    DashboardPanel("CÁMARA", modifier) {
        CameraStreamView(
            streamUrl = machine?.status?.camera?.streamUrl,
            baseUrl = baseUrl,
            token = token,
            modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(8.dp)),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(if (machine?.online == true) NopalGreen else NopalDanger))
            Text("  ${machine?.status?.camera?.name ?: machine?.name ?: "Sin cámara vinculada"}", color = NopalTextPrimary, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = onOpen, enabled = machine != null) { Icon(Icons.Default.CameraAlt, null); Text(" Ver") }
        }
    }
}

@Composable
private fun MaterialOverview(machine: Machine?, response: MaterialsResponse, onOpen: () -> Unit, modifier: Modifier) {
    DashboardPanel("MATERIAL ACTIVO", modifier) {
        Icon(Icons.Default.Inventory2, null, tint = NopalGreen, modifier = Modifier.size(32.dp))
        val spoolId = machine?.let { response.links[it.id] }
        val spool = response.spools.firstOrNull { it.id == spoolId }
        Text(spool?.let { "${it.material} · ${it.name}" } ?: (response.reason ?: "Sin material vinculado"), color = NopalTextPrimary, fontWeight = FontWeight.Bold)
        Text(spool?.let { "${it.availableG.toInt()} g disponibles" } ?: "${response.spools.size} bobinas en inventario", color = NopalTextSecondary, style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = onOpen, enabled = response.available) { Text("Ver materiales") }
    }
}

@Composable
private fun AccessoriesOverview(response: AccessoriesResponse, onOpen: () -> Unit, modifier: Modifier) {
    DashboardPanel("ACCESORIOS", modifier) {
        Icon(Icons.Default.Power, null, tint = if (response.available) NopalGreen else NopalWarning, modifier = Modifier.size(30.dp))
        Text(if (response.available) "${response.accessories.count { it.on == true }} encendidos" else (response.reason ?: "No disponibles"), color = NopalTextPrimary, fontWeight = FontWeight.Bold)
        Text("${response.accessories.size} dispositivos · ${response.scenes.size} escenas", color = NopalTextSecondary, style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = onOpen, enabled = response.available) { Text("Controlar") }
    }
}

@Composable
private fun DashboardPanel(title: String, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = NopalSurface, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, NopalBorder), modifier = modifier) {
        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, color = NopalGreen, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun EmptyDashboardMessage(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(message, color = NopalTextSecondary) }
}

@Composable
private fun homeStatusColor(machine: Machine): Color = when {
    !machine.online || machine.status.state == "error" -> NopalDanger
    machine.status.state == "paused" -> NopalWarning
    machine.status.state in setOf("printing", "running", "working") -> Color(0xFF27BDEB)
    else -> NopalGreen
}

private fun machineSummary(machine: Machine): String = when (machine.type) {
    com.nopal.tunascreen.core.model.MachineType.PRINTER ->
        "${machine.status.hotend?.current?.toInt() ?: "--"}° / ${machine.status.bed?.current?.toInt() ?: "--"}°"
    com.nopal.tunascreen.core.model.MachineType.LASER -> "Potencia ${machine.status.laserPower?.toInt() ?: "--"}"
    com.nopal.tunascreen.core.model.MachineType.CNC -> "Husillo ${machine.status.spindleRpm?.toInt() ?: "--"} rpm"
}
