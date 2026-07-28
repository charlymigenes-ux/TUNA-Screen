package com.nopal.tunascreen.feature.machine

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HotTub
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nopal.tunascreen.core.model.Capability
import com.nopal.tunascreen.core.model.Machine
import com.nopal.tunascreen.core.model.MachineType
import com.nopal.tunascreen.feature.machine.components.JobProgressCard
import com.nopal.tunascreen.feature.machine.components.MachineControlButton
import com.nopal.tunascreen.feature.machine.components.MachineJobPreview
import com.nopal.tunascreen.feature.machine.components.NopalTopBar
import com.nopal.tunascreen.feature.machine.components.QuickActionButton
import com.nopal.tunascreen.feature.machine.components.TemperatureCard
import com.nopal.tunascreen.ui.theme.NopalBackground
import com.nopal.tunascreen.ui.theme.NopalChrome
import com.nopal.tunascreen.ui.theme.NopalGreen
import com.nopal.tunascreen.ui.theme.NopalSurface
import com.nopal.tunascreen.ui.theme.NopalTextPrimary
import com.nopal.tunascreen.ui.theme.NopalTextSecondary
import com.nopal.tunascreen.ui.theme.DisplayProfile
import com.nopal.tunascreen.ui.theme.LocalDisplayProfile
import com.nopal.tunascreen.ui.theme.NopalBorder
import com.nopal.tunascreen.ui.theme.NopalSurfaceElevated
import com.nopal.tunascreen.ui.theme.NopalDanger
import com.nopal.tunascreen.ui.theme.NopalWarning

@Composable
fun MachineScreen(
    machineId: String,
    onBack: () -> Unit,
    viewModel: MachineViewModel = hiltViewModel(),
) {
    val machine by viewModel.machine.collectAsState()
    val error by viewModel.actionError.collectAsState()
    val currentMachine = machine ?: return
    val isJobActive = currentMachine.status.state in setOf("printing", "paused", "working", "running")
    val displayProfile = LocalDisplayProfile.current.profile
    var pendingCommand by remember { mutableStateOf<MachineCommand?>(null) }

    pendingCommand?.let { command ->
        MachineActionDialog(
            command = command,
            viewModel = viewModel,
            onDismiss = { pendingCommand = null },
        )
    }

    Scaffold(
        topBar = {
            NopalTopBar(
                machineName = currentMachine.name,
                machineSubtitle = currentMachine.type.displayName(),
                status = if (currentMachine.online) currentMachine.status.state else "OFFLINE",
                online = currentMachine.online,
                onListClick = onBack,
            )
        },
        bottomBar = {
            MachineBottomControlBar(
                machine = currentMachine,
                viewModel = viewModel,
                isPaused = currentMachine.status.state == "paused",
                isJobActive = isJobActive,
                displayProfile = displayProfile,
                onCommand = { pendingCommand = it },
            )
        },
        containerColor = NopalBackground,
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp),
        ) {
            error?.let {
                Snackbar(
                    action = {
                        TextButton(onClick = viewModel::dismissError) { Text("OK", color = NopalGreen) }
                    },
                    modifier = Modifier.padding(bottom = 8.dp),
                ) { Text(it) }
            }

            if (displayProfile.isCompactMachine) {
                CompactMachineContent(currentMachine)
            } else {
                when (currentMachine.type) {
                    MachineType.PRINTER -> PrinterMachineContent(currentMachine) { pendingCommand = it }
                    MachineType.LASER -> LaserMachineContent(currentMachine) { pendingCommand = it }
                    MachineType.CNC -> CncMachineContent(currentMachine) { pendingCommand = it }
                }
            }
        }
    }
}

@Composable
private fun CompactMachineContent(machine: Machine) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CompactStatusPanel(machine = machine, modifier = Modifier.weight(1.05f))
        CompactTelemetryPanel(machine = machine, modifier = Modifier.weight(0.95f))
    }
}

@Composable
private fun CompactStatusPanel(machine: Machine, modifier: Modifier = Modifier) {
    val stateColor = machineStateColor(machine)
    val job = machine.status.job
    Surface(
        color = NopalSurface,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, NopalBorder),
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(machine.type.displayName().uppercase(), color = NopalTextSecondary, style = MaterialTheme.typography.labelMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(26.dp))
                        .background(stateColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (machine.online) Icons.Default.Power else Icons.Default.Stop,
                        null,
                        tint = stateColor,
                        modifier = Modifier.size(30.dp),
                    )
                }
                Spacer(modifier = Modifier.size(12.dp))
                Column {
                    Text(
                        if (machine.online) machine.status.state.uppercase() else "SIN CONEXIÓN",
                        color = stateColor,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (machine.online) "NOPAL conectado" else "Revisa la conexión",
                        color = NopalTextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Text(job?.filename ?: "Sin trabajo activo", color = NopalTextPrimary, fontWeight = FontWeight.Medium)
            LinearProgressIndicator(
                progress = { (job?.percent ?: 0).coerceIn(0, 100) / 100f },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = stateColor,
                trackColor = NopalSurfaceElevated,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${job?.percent ?: 0}%", color = stateColor, fontWeight = FontWeight.Bold)
                Text("Restante ${formatTime(job?.remainingS)}", color = NopalTextSecondary)
            }
        }
    }
}

@Composable
private fun CompactTelemetryPanel(machine: Machine, modifier: Modifier = Modifier) {
    Surface(
        color = NopalSurface,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, NopalBorder),
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                when (machine.type) {
                    MachineType.PRINTER -> "TEMPERATURAS"
                    MachineType.LASER -> "LÁSER"
                    MachineType.CNC -> "MECANIZADO"
                },
                color = NopalGreen,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            when (machine.type) {
                MachineType.PRINTER -> {
                    CompactMetric("HOTEND", "${machine.status.hotend?.current?.toInt() ?: "--"} °C", Icons.Default.Thermostat)
                    CompactMetric("CAMA", "${machine.status.bed?.current?.toInt() ?: "--"} °C", Icons.Default.HotTub)
                    CompactMetric("POSICIÓN", positionText(machine), Icons.Default.MyLocation)
                }
                MachineType.LASER -> {
                    CompactMetric("POTENCIA S", "${machine.status.laserPower?.toInt() ?: "--"}", Icons.Default.LightMode)
                    CompactMetric("AVANCE", "${machine.status.feed?.toInt() ?: "--"} mm/min", Icons.Default.Speed)
                    CompactMetric("POSICIÓN", positionText(machine), Icons.Default.MyLocation)
                }
                MachineType.CNC -> {
                    CompactMetric("HUSILLO", "${machine.status.spindleRpm?.toInt() ?: "--"} rpm", Icons.Default.PrecisionManufacturing)
                    CompactMetric("AVANCE", "${machine.status.feed?.toInt() ?: "--"} mm/min", Icons.Default.Speed)
                    CompactMetric("POSICIÓN", positionText(machine), Icons.Default.MyLocation)
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.CompactMetric(label: String, value: String, icon: ImageVector) {
    Surface(color = NopalSurfaceElevated, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().weight(1f)) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = NopalGreen, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.size(10.dp))
            Column {
                Text(label, color = NopalTextSecondary, style = MaterialTheme.typography.labelSmall)
                Text(value, color = NopalGreen, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PrinterMachineContent(machine: Machine, onCommand: (MachineCommand) -> Unit) {
    MachineColumns(
        machine = machine,
        leftTitle = "TEMPERATURAS",
        left = {
            TemperatureCard(
                label = "HOTEND",
                current = "${machine.status.hotend?.current?.toInt() ?: "--"}",
                target = "${machine.status.hotend?.target?.toInt() ?: "--"}",
                icon = Icons.Default.Thermostat,
            )
            TemperatureCard(
                label = "CAMA",
                current = "${machine.status.bed?.current?.toInt() ?: "--"}",
                target = "${machine.status.bed?.target?.toInt() ?: "--"}",
                icon = Icons.Default.HotTub,
            )
        },
        rightTitle = "ACCIONES DE IMPRESIÓN",
        right = {
            SupportedAction(
                machine, "set_temperature", "Precalentar",
                Icons.Default.DeviceThermostat, MachineCommand.PREHEAT, onCommand,
            )
            SupportedAction(
                machine, "extrude", "Cargar o retraer filamento",
                Icons.Default.KeyboardDoubleArrowDown, MachineCommand.EXTRUDE, onCommand,
            )
        },
    )
}

@Composable
private fun LaserMachineContent(machine: Machine, onCommand: (MachineCommand) -> Unit) {
    MachineColumns(
        machine = machine,
        leftTitle = "LÁSER",
        left = {
            MachineMetricCard("POTENCIA", machine.status.laserPower?.toInt()?.toString() ?: "--", "S", Icons.Default.LightMode)
            MachineMetricCard("AVANCE", machine.status.feed?.toInt()?.toString() ?: "--", "mm/min", Icons.Default.Speed)
            MachineMetricCard("POSICIÓN", positionText(machine), "mm", Icons.Default.MyLocation)
        },
        rightTitle = "CONTROLES LÁSER",
        right = {
            SupportedAction(
                machine, "set_laser_power", "Potencia láser",
                Icons.Default.LightMode, MachineCommand.LASER_POWER, onCommand,
            )
            SupportedAction(
                machine, "set_air_assist", "Air assist",
                Icons.Default.Air, MachineCommand.AIR_ASSIST, onCommand,
            )
        },
    )
}

@Composable
private fun CncMachineContent(machine: Machine, onCommand: (MachineCommand) -> Unit) {
    MachineColumns(
        machine = machine,
        leftTitle = "MECANIZADO CNC",
        left = {
            MachineMetricCard("HUSILLO", machine.status.spindleRpm?.toInt()?.toString() ?: "--", "rpm", Icons.Default.PrecisionManufacturing)
            MachineMetricCard("AVANCE", machine.status.feed?.toInt()?.toString() ?: "--", "mm/min", Icons.Default.Speed)
            MachineMetricCard("POSICIÓN", positionText(machine), "mm", Icons.Default.MyLocation)
        },
        rightTitle = "CONTROLES CNC",
        right = {
            SupportedAction(
                machine, "set_work_zero", "Cero XYZ",
                Icons.Default.MyLocation, MachineCommand.WORK_ZERO, onCommand,
            )
            SupportedAction(
                machine, "set_spindle", "Husillo y RPM",
                Icons.Default.PrecisionManufacturing, MachineCommand.SPINDLE, onCommand,
            )
            SupportedAction(
                machine, "set_coolant", "Refrigerante",
                Icons.Default.WaterDrop, MachineCommand.COOLANT, onCommand,
            )
        },
    )
}

@Composable
private fun MachineColumns(
    machine: Machine,
    leftTitle: String,
    left: @Composable ColumnScope.() -> Unit,
    rightTitle: String,
    right: @Composable ColumnScope.() -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.weight(0.25f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionHeader(leftTitle)
            left()
        }
        Column(
            modifier = Modifier.weight(0.5f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionHeader(
                when (machine.type) {
                    MachineType.PRINTER -> "VISTA PREVIA DE IMPRESIÓN"
                    MachineType.LASER -> "VISTA PREVIA DEL GRABADO"
                    MachineType.CNC -> "VISTA PREVIA DEL MECANIZADO"
                },
            )
            MachineJobPreview(
                zPos = "%.2f".format(machine.status.position?.z ?: 0.0),
                yPos = "%.2f".format(machine.status.position?.y ?: 0.0),
                xPos = "%.2f".format(machine.status.position?.x ?: 0.0),
            )
            val job = machine.status.job
            JobProgressCard(
                filename = job?.filename ?: "Ningún trabajo activo",
                percent = job?.percent ?: 0,
                elapsed = formatTime(job?.elapsedS),
                remaining = formatTime(job?.remainingS),
            )
        }
        Column(
            modifier = Modifier.weight(0.25f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionHeader(rightTitle)
            right()
        }
    }
}

@Composable
private fun MachineMetricCard(
    label: String,
    value: String,
    unit: String,
    icon: ImageVector,
) {
    Surface(
        color = NopalSurface,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = NopalGreen, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.size(12.dp))
            Column {
                Text(label, color = NopalTextSecondary, style = MaterialTheme.typography.labelMedium)
                Text(
                    text = "$value $unit",
                    color = NopalGreen,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun SupportedAction(
    machine: Machine,
    action: String,
    label: String,
    icon: ImageVector,
    command: MachineCommand,
    onCommand: (MachineCommand) -> Unit,
) {
    if (machine.actions.contains(action)) {
        QuickActionButton(
            label = label,
            icon = icon,
            onClick = { onCommand(command) },
            enabled = machine.online,
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = NopalGreen,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = NopalTextSecondary,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun MachineBottomControlBar(
    machine: Machine,
    viewModel: MachineViewModel,
    isPaused: Boolean,
    isJobActive: Boolean,
    displayProfile: DisplayProfile,
    onCommand: (MachineCommand) -> Unit,
) {
    val type = machine.type
    val compact = displayProfile.isCompactMachine
    Surface(
        color = NopalChrome,
        modifier = Modifier.fillMaxWidth().height(if (compact) 84.dp else 100.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MachineControlButton(
                "HOME", Icons.Default.Home, { viewModel.home() },
                enabled = machine.supports("home"), modifier = Modifier.weight(1f),
            )
            if (compact) {
                if (displayProfile != DisplayProfile.MACHINE_35) {
                    MachineControlButton(
                        "MOVER", Icons.Default.OpenWith, { onCommand(MachineCommand.MOVE) },
                        enabled = machine.supports("move"), modifier = Modifier.weight(1f),
                    )
                }
                when (type) {
                    MachineType.PRINTER -> MachineControlButton(
                        "EXTRUIR", Icons.Default.KeyboardDoubleArrowDown, { onCommand(MachineCommand.EXTRUDE) },
                        enabled = machine.supports("extrude"), modifier = Modifier.weight(1f),
                    )
                    MachineType.LASER -> MachineControlButton(
                        "POTENCIA", Icons.Default.LightMode, { onCommand(MachineCommand.LASER_POWER) },
                        enabled = machine.supports("set_laser_power"), modifier = Modifier.weight(1f),
                    )
                    MachineType.CNC -> MachineControlButton(
                        "HUSILLO", Icons.Default.PrecisionManufacturing, { onCommand(MachineCommand.SPINDLE) },
                        enabled = machine.supports("set_spindle"), modifier = Modifier.weight(1f),
                    )
                }
                MachineControlButton(
                    label = if (isPaused) "REANUDAR" else "PAUSAR",
                    icon = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    onClick = { if (isPaused) viewModel.resume() else viewModel.pause() },
                    isSelected = true,
                    enabled = isJobActive && machine.supports(if (isPaused) "resume" else "pause"),
                    modifier = Modifier.weight(1f),
                )
                MachineControlButton(
                    "CANCELAR",
                    Icons.Default.Stop,
                    { viewModel.cancel() },
                    isDanger = true,
                    enabled = isJobActive && machine.supports("cancel"),
                    modifier = Modifier.weight(1f),
                )
                return@Row
            }

            MachineControlButton(
                "MOVER", Icons.Default.OpenWith, { onCommand(MachineCommand.MOVE) },
                enabled = machine.supports("move"), modifier = Modifier.weight(1f),
            )

            when (type) {
                MachineType.PRINTER -> {
                    MachineControlButton(
                        "EXTRUIR", Icons.Default.KeyboardDoubleArrowDown, { onCommand(MachineCommand.EXTRUDE) },
                        enabled = machine.supports("extrude"), modifier = Modifier.weight(1f),
                    )
                    MachineControlButton(
                        "PRECALENTAR", Icons.Default.DeviceThermostat, { onCommand(MachineCommand.PREHEAT) },
                        enabled = machine.supports("set_temperature"), modifier = Modifier.weight(1f),
                    )
                }
                MachineType.LASER -> {
                    MachineControlButton(
                        "POTENCIA", Icons.Default.LightMode, { onCommand(MachineCommand.LASER_POWER) },
                        enabled = machine.supports("set_laser_power"), modifier = Modifier.weight(1f),
                    )
                }
                MachineType.CNC -> {
                    MachineControlButton(
                        "CERO XYZ", Icons.Default.MyLocation, { onCommand(MachineCommand.WORK_ZERO) },
                        enabled = machine.supports("set_work_zero"), modifier = Modifier.weight(1f),
                    )
                }
            }

            MachineControlButton(
                label = if (isPaused) "REANUDAR" else "PAUSAR",
                icon = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                onClick = { if (isPaused) viewModel.resume() else viewModel.pause() },
                isSelected = true,
                enabled = isJobActive && machine.supports(if (isPaused) "resume" else "pause"),
                modifier = Modifier.weight(1f),
            )
            MachineControlButton(
                "CANCELAR",
                Icons.Default.Stop,
                { viewModel.cancel() },
                isDanger = true,
                enabled = isJobActive && machine.supports("cancel"),
                modifier = Modifier.weight(1f),
            )

            when (type) {
                MachineType.PRINTER -> Unit
                MachineType.LASER -> MachineControlButton(
                    "AIR ASSIST", Icons.Default.Air, { onCommand(MachineCommand.AIR_ASSIST) },
                    enabled = machine.supports("set_air_assist"), modifier = Modifier.weight(1f),
                )
                MachineType.CNC -> MachineControlButton(
                    "HUSILLO", Icons.Default.PrecisionManufacturing, { onCommand(MachineCommand.SPINDLE) },
                    enabled = machine.supports("set_spindle"), modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun machineStateColor(machine: Machine): Color = when {
    !machine.online -> NopalDanger
    machine.status.state == "paused" -> NopalWarning
    machine.status.state == "error" -> NopalDanger
    else -> NopalGreen
}

private fun MachineType.displayName(): String = when (this) {
    MachineType.PRINTER -> "Impresora 3D"
    MachineType.LASER -> "Láser"
    MachineType.CNC -> "CNC"
}

private fun positionText(machine: Machine): String {
    val position = machine.status.position
    return "X ${position?.x?.let { "%.1f".format(it) } ?: "--"}  " +
        "Y ${position?.y?.let { "%.1f".format(it) } ?: "--"}  " +
        "Z ${position?.z?.let { "%.1f".format(it) } ?: "--"}"
}

private fun formatTime(seconds: Long?): String {
    if (seconds == null) return "00:00:00"
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remainingSeconds = seconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, remainingSeconds)
}
