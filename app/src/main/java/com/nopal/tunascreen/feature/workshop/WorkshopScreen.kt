package com.nopal.tunascreen.feature.workshop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HotTub
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nopal.tunascreen.core.model.Capability
import com.nopal.tunascreen.core.model.Machine
import com.nopal.tunascreen.core.model.MachineType
import com.nopal.tunascreen.core.repository.ConnectionState
import com.nopal.tunascreen.feature.workshop.components.WorkshopBottomNav
import com.nopal.tunascreen.feature.workshop.components.WorkshopTopBar
import com.nopal.tunascreen.ui.theme.NopalBackground
import com.nopal.tunascreen.ui.theme.NopalBorder
import com.nopal.tunascreen.ui.theme.NopalDanger
import com.nopal.tunascreen.ui.theme.NopalGreen
import com.nopal.tunascreen.ui.theme.NopalSurface
import com.nopal.tunascreen.ui.theme.NopalSurfaceElevated
import com.nopal.tunascreen.ui.theme.NopalTextPrimary
import com.nopal.tunascreen.ui.theme.NopalTextSecondary
import com.nopal.tunascreen.ui.theme.NopalWarning
import com.nopal.tunascreen.ui.theme.DisplayProfile
import com.nopal.tunascreen.ui.theme.LocalDisplayProfile

@Composable
fun WorkshopScreen(
    onMachineSelected: (String) -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: WorkshopViewModel = hiltViewModel(),
) {
    val machines by viewModel.machines.collectAsState()
    val connection by viewModel.connectionState.collectAsState()
    var filter by remember { mutableStateOf<MachineType?>(null) }
    val onlineCount = remember(machines) { machines.count { it.online } }
    val visibleMachines = remember(machines, filter) {
        filter?.let { type -> machines.filter { it.type == type } } ?: machines
    }
    val cameraMachines = remember(machines) { machines.filter { it.has(Capability.CAMERA) } }
    val activeJobs = remember(machines) { machines.filter { it.status.job != null } }
    val displayProfile = LocalDisplayProfile.current.profile
    val compactMachineSelector = displayProfile in setOf(
        DisplayProfile.MACHINE_35,
        DisplayProfile.MACHINE_43,
        DisplayProfile.ADVANCED_5,
    )

    Scaffold(
        topBar = {
            WorkshopTopBar(
                connectionStatus = connection.label(),
                onlineSummary = "$onlineCount/${machines.size} EN LÍNEA",
                onSettingsClick = { onNavigate("settings") },
            )
        },
        bottomBar = {
            if (!compactMachineSelector) {
                WorkshopBottomNav(selectedTab = "workshop", onTabSelected = onNavigate)
            }
        },
        containerColor = NopalBackground,
    ) { padding ->
        if (compactMachineSelector) {
            if (visibleMachines.isEmpty()) {
                EmptyPanel("NOPAL no reporta máquinas")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(visibleMachines, key = { it.id }) { machine ->
                        WorkshopMachineCard(machine = machine, onClick = { onMachineSelected(machine.id) })
                    }
                }
            }
        } else if (displayProfile in setOf(DisplayProfile.WORKSHOP_10, DisplayProfile.DESKTOP)) {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                WorkshopHomeDashboard(
                    machines = machines,
                    connection = connection,
                    onMachineSelected = onMachineSelected,
                    onNavigate = onNavigate,
                    onPause = viewModel::pause,
                    onCancel = viewModel::cancel,
                )
            }
        } else {
            Row(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
            Column(modifier = Modifier.weight(0.7f).fillMaxHeight()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionTitle("MÁQUINAS")
                    Spacer(modifier = Modifier.weight(1f))
                    MachineFilter("Todas", filter == null) { filter = null }
                    Spacer(modifier = Modifier.width(6.dp))
                    MachineFilter("3D", filter == MachineType.PRINTER) { filter = MachineType.PRINTER }
                    Spacer(modifier = Modifier.width(6.dp))
                    MachineFilter("Láser", filter == MachineType.LASER) { filter = MachineType.LASER }
                    Spacer(modifier = Modifier.width(6.dp))
                    MachineFilter("CNC", filter == MachineType.CNC) { filter = MachineType.CNC }
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (visibleMachines.isEmpty()) {
                    EmptyPanel("NOPAL no reporta máquinas en esta categoría")
                } else {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val columns = when (displayProfile) {
                            DisplayProfile.MACHINE_7 -> 2
                            DisplayProfile.WORKSHOP_10 -> 3
                            DisplayProfile.DESKTOP -> 4
                            else -> if (maxWidth >= 720.dp) 3 else 2
                        }
                        val gridState = remember(filter) { LazyGridState() }
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            state = gridState,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 8.dp),
                        ) {
                            items(visibleMachines, key = { it.id }) { machine ->
                                WorkshopMachineCard(machine = machine, onClick = { onMachineSelected(machine.id) })
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.weight(0.3f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SidePanel(title = "CÁMARAS", modifier = Modifier.weight(1f)) {
                    if (cameraMachines.isEmpty()) {
                        PanelMessage("Sin cámaras reportadas")
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(cameraMachines.take(3), key = { it.id }) { machine ->
                                CameraStatusRow(machine)
                            }
                        }
                    }
                }

                SidePanel(title = "TRABAJOS ACTIVOS", modifier = Modifier.weight(1f)) {
                    if (activeJobs.isEmpty()) {
                        PanelMessage("No hay trabajos activos")
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(activeJobs, key = { it.id }) { machine ->
                                ActiveJobRow(machine)
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
private fun WorkshopMachineCard(machine: Machine, onClick: () -> Unit) {
    val statusColor = machine.statusColor()
    Surface(
        onClick = onClick,
        color = NopalSurface,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, NopalBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(machine.type.icon(), null, tint = NopalGreen, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(machine.name, color = NopalTextPrimary, fontWeight = FontWeight.Bold)
                    Text(machine.type.displayName(), color = NopalTextSecondary, style = MaterialTheme.typography.labelSmall)
                }
                StatusBadge(if (machine.online) machine.status.state else "offline", statusColor)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(width = 82.dp, height = 66.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(NopalBackground),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(machine.type.icon(), null, tint = NopalGreen.copy(alpha = 0.5f), modifier = Modifier.size(38.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MachineMetrics(machine)
                }
            }

            machine.status.job?.let { job ->
                LinearProgressIndicator(
                    progress = { (job.percent ?: 0).coerceIn(0, 100) / 100f },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = statusColor,
                    trackColor = NopalSurfaceElevated,
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(job.filename ?: "Trabajo activo", color = NopalTextSecondary, style = MaterialTheme.typography.labelSmall)
                    Text("${job.percent ?: 0}%", color = statusColor, fontWeight = FontWeight.Bold)
                }
            } ?: Text(
                text = if (machine.online) "Lista para trabajar" else "Sin conexión",
                color = NopalTextSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun MachineMetrics(machine: Machine) {
    when (machine.type) {
        MachineType.PRINTER -> {
            MetricLine(Icons.Default.Thermostat, "${machine.status.hotend?.current?.toInt() ?: "--"} °C")
            MetricLine(Icons.Default.HotTub, "${machine.status.bed?.current?.toInt() ?: "--"} °C")
        }
        MachineType.LASER -> {
            MetricLine(Icons.Default.LightMode, "Potencia S ${machine.status.laserPower?.toInt() ?: "--"}")
            MetricLine(Icons.Default.PrecisionManufacturing, "Avance ${machine.status.feed?.toInt() ?: "--"}")
        }
        MachineType.CNC -> {
            val position = machine.status.position
            MetricLine(Icons.Default.PrecisionManufacturing, "RPM ${machine.status.spindleRpm?.toInt() ?: "--"}")
            MetricLine(Icons.Default.MyLocation, "X ${position?.x?.let { "%.1f".format(it) } ?: "--"}")
            MetricLine(Icons.Default.MyLocation, "Y ${position?.y?.let { "%.1f".format(it) } ?: "--"}")
        }
    }
}

@Composable
private fun MetricLine(icon: ImageVector, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = NopalGreen, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(value, color = NopalTextPrimary, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun CameraStatusRow(machine: Machine) {
    Surface(color = NopalSurfaceElevated, shape = RoundedCornerShape(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Videocam, null, tint = NopalGreen)
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(machine.name, color = NopalTextPrimary, fontWeight = FontWeight.Bold)
                Text("Transmisión pendiente de API", color = NopalTextSecondary, style = MaterialTheme.typography.labelSmall)
            }
            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(if (machine.online) NopalGreen else NopalDanger))
        }
    }
}

@Composable
private fun ActiveJobRow(machine: Machine) {
    val job = machine.status.job ?: return
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(machine.name, color = NopalTextPrimary, modifier = Modifier.weight(0.3f), style = MaterialTheme.typography.labelMedium)
        Text(job.filename ?: "Trabajo", color = NopalTextSecondary, modifier = Modifier.weight(0.5f), style = MaterialTheme.typography.labelSmall)
        Text("${job.percent ?: 0}%", color = machine.statusColor(), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SidePanel(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        color = NopalBackground,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, NopalBorder),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            SectionTitle(title)
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, color = NopalGreen, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
}

@Composable
private fun MachineFilter(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
private fun StatusBadge(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.14f), shape = RoundedCornerShape(4.dp)) {
        Text(
            text.uppercase(),
            color = color,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun EmptyPanel(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = NopalTextSecondary)
    }
}

@Composable
private fun PanelMessage(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = NopalTextSecondary, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Machine.statusColor(): Color = when {
    !online -> NopalDanger
    status.state in setOf("printing", "running", "working") -> NopalGreen
    status.state == "paused" -> NopalWarning
    status.state == "error" -> NopalDanger
    else -> NopalGreen
}

private fun MachineType.icon(): ImageVector = when (this) {
    MachineType.PRINTER -> Icons.Default.Print
    MachineType.LASER -> Icons.Default.LightMode
    MachineType.CNC -> Icons.Default.PrecisionManufacturing
}

internal fun ConnectionState.label(): String = when (this) {
    ConnectionState.LIVE -> "Conectado"
    ConnectionState.CONNECTING -> "Conectando"
    ConnectionState.DISCONNECTED -> "Desconectado"
}
