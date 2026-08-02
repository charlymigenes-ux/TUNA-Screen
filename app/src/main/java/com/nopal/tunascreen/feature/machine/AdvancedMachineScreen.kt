package com.nopal.tunascreen.feature.machine

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nopal.tunascreen.core.model.Machine
import com.nopal.tunascreen.core.model.MachineType
import com.nopal.tunascreen.feature.machine.components.NopalTopBar
import com.nopal.tunascreen.ui.theme.*

@Composable
fun AdvancedMachineScreen(
    machineId: String,
    onBack: () -> Unit,
    viewModel: MachineViewModel = hiltViewModel(),
) {
    val machine by viewModel.machine.collectAsState()
    val error by viewModel.actionError.collectAsState()
    val current = machine ?: return
    var command by remember { mutableStateOf<MachineCommand?>(null) }

    command?.let { selected ->
        MachineActionDialog(selected, viewModel) { command = null }
    }

    Scaffold(
        topBar = {
            NopalTopBar(
                machineName = current.name,
                machineSubtitle = "${current.type.advancedName()} · Modo avanzado",
                status = if (current.online) current.status.state else "OFFLINE",
                online = current.online,
                onListClick = onBack,
            )
        },
        containerColor = NopalBackground,
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(12.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            error?.let {
                Snackbar(action = { TextButton(onClick = viewModel::dismissError) { Text("OK") } }) { Text(it) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdvancedPanel("TELEMETRÍA", Modifier.weight(1f)) { Telemetry(current) }
                AdvancedPanel("CONTROL FINO", Modifier.weight(1.45f)) {
                    CommandGrid(current, viewModel) { command = it }
                }
                AdvancedPanel("SISTEMA", Modifier.weight(1f)) {
                    SystemControls(current) { command = it }
                }
            }
            AdvancedPanel("TRABAJO ACTUAL", Modifier.fillMaxWidth()) {
                val job = current.status.job
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(job?.filename ?: "Sin trabajo activo", color = NopalTextPrimary, fontWeight = FontWeight.Bold)
                        Text("Estado: ${current.status.state}", color = NopalTextSecondary)
                    }
                    if (job != null) {
                        LinearProgressIndicator(
                            progress = { (job.percent ?: 0) / 100f },
                            color = NopalGreen,
                            trackColor = NopalSurfaceElevated,
                            modifier = Modifier.weight(1.5f).padding(horizontal = 16.dp),
                        )
                        Text("${job.percent ?: 0}%", color = NopalGreen, style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdvancedPanel(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = NopalSurface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, NopalBorder),
        modifier = modifier,
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = NopalGreen, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun Telemetry(machine: Machine) {
    when (machine.type) {
        MachineType.PRINTER -> {
            Metric("Hotend", machine.status.hotend?.current?.let { "%.1f °C".format(it) } ?: "--")
            Metric("Cama", machine.status.bed?.current?.let { "%.1f °C".format(it) } ?: "--")
            Metric("Ventilador", machine.status.fanPercent?.let { "%.0f %%".format(it) } ?: "--")
            Metric("Velocidad", machine.status.speedFactor?.let { "%.0f %%".format(it) } ?: "--")
            Metric("Flujo", machine.status.flowFactor?.let { "%.0f %%".format(it) } ?: "--")
            Metric("Offset Z", machine.status.zOffset?.let { "%.3f mm".format(it) } ?: "--")
        }
        MachineType.LASER -> {
            Metric("Potencia", machine.status.laserPower?.let { "%.0f".format(it) } ?: "--")
            Metric("Avance", machine.status.feed?.let { "%.0f mm/min".format(it) } ?: "--")
            Metric("Air assist", machine.status.airAssist?.let { if (it) "ON" else "OFF" } ?: "--")
        }
        MachineType.CNC -> {
            Metric("Husillo", machine.status.spindleRpm?.let { "%.0f rpm".format(it) } ?: "--")
            Metric("Avance", machine.status.feed?.let { "%.0f mm/min".format(it) } ?: "--")
            Metric("Refrigerante", machine.status.coolant?.let { if (it) "ON" else "OFF" } ?: "--")
        }
    }
    val pos = machine.status.position
    Metric("Posición", "X ${pos?.x ?: "--"}  Y ${pos?.y ?: "--"}  Z ${pos?.z ?: "--"}")
}

@Composable
private fun Metric(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = NopalTextSecondary)
        Text(value, color = NopalTextPrimary, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CommandGrid(machine: Machine, viewModel: MachineViewModel, open: (MachineCommand) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        AdvancedButton("HOME", Icons.Default.Home, machine.supports("home"), { viewModel.home() }, Modifier.weight(1f))
        AdvancedButton("MOVER", Icons.Default.OpenWith, machine.supports("move"), { open(MachineCommand.MOVE) }, Modifier.weight(1f))
        when (machine.type) {
            MachineType.PRINTER -> AdvancedButton("EXTRUIR", Icons.Default.ArrowDownward, machine.supports("extrude"), { open(MachineCommand.EXTRUDE) }, Modifier.weight(1f))
            MachineType.LASER -> AdvancedButton("LÁSER", Icons.Default.LightMode, machine.supports("set_laser_power"), { open(MachineCommand.LASER_POWER) }, Modifier.weight(1f))
            MachineType.CNC -> AdvancedButton("HUSILLO", Icons.Default.PrecisionManufacturing, machine.supports("set_spindle"), { open(MachineCommand.SPINDLE) }, Modifier.weight(1f))
        }
    }
    if (machine.type == MachineType.PRINTER) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AdvancedButton("FAN", Icons.Default.Air, machine.supports("set_fan"), { open(MachineCommand.FAN) }, Modifier.weight(1f))
            AdvancedButton("VELOCIDAD", Icons.Default.Speed, machine.supports("set_speed_factor"), { open(MachineCommand.SPEED_FACTOR) }, Modifier.weight(1f))
            AdvancedButton("FLUJO", Icons.Default.WaterDrop, machine.supports("set_flow_factor"), { open(MachineCommand.FLOW_FACTOR) }, Modifier.weight(1f))
            AdvancedButton("OFFSET Z", Icons.Default.Height, machine.supports("set_z_offset"), { open(MachineCommand.Z_OFFSET) }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SystemControls(machine: Machine, open: (MachineCommand) -> Unit) {
    if (machine.supports("run_macro")) AdvancedWideButton("Macros Klipper", Icons.Default.Bolt) { open(MachineCommand.MACROS) }
    if (machine.supports("send_console_command")) AdvancedWideButton("Consola G-code", Icons.Default.Terminal) { open(MachineCommand.CONSOLE) }
    if (machine.supports("set_air_assist")) AdvancedWideButton("Air assist", Icons.Default.Air) { open(MachineCommand.AIR_ASSIST) }
    if (machine.supports("set_work_zero")) AdvancedWideButton("Cero de trabajo", Icons.Default.MyLocation) { open(MachineCommand.WORK_ZERO) }
    if (machine.supports("set_coolant")) AdvancedWideButton("Refrigerante", Icons.Default.WaterDrop) { open(MachineCommand.COOLANT) }
    if (machine.capabilities.any { it.name == "SPOOL" }) Metric("Material", "Vinculado con Spoolman")
    if (machine.capabilities.any { it.name == "CAMERA" }) Metric("Cámara", machine.status.camera?.name ?: "Disponible")
}

@Composable
private fun AdvancedButton(label: String, icon: ImageVector, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 56.dp), contentPadding = PaddingValues(6.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun AdvancedWideButton(label: String, icon: ImageVector, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

private fun MachineType.advancedName() = when (this) {
    MachineType.PRINTER -> "Impresora 3D"
    MachineType.LASER -> "Láser"
    MachineType.CNC -> "CNC"
}
