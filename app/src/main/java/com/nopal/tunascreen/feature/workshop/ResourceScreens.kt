package com.nopal.tunascreen.feature.workshop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Power
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nopal.tunascreen.core.model.Machine
import com.nopal.tunascreen.core.model.MachineType
import com.nopal.tunascreen.core.model.MaterialSpool
import com.nopal.tunascreen.ui.theme.*

@Composable
fun JobsScreen(
    onMachineSelected: (String) -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: WorkshopViewModel = hiltViewModel(),
) {
    val machines by viewModel.machines.collectAsState()
    val connection by viewModel.connectionState.collectAsState()
    val jobs = remember(machines) {
        machines.filter { it.status.job != null || it.status.state in setOf("printing", "paused", "working", "running") }
    }
    DashboardScaffold("Trabajos", "jobs", connection.label(), onNavigate) { padding ->
        if (jobs.isEmpty()) {
            EmptyDashboardState("No hay trabajos activos", "Los trabajos aparecerán aquí con el estado en vivo de NOPAL.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
            ) {
                items(jobs, key = { it.id }) { machine -> JobCard(machine) { onMachineSelected(machine.id) } }
            }
        }
    }
}

@Composable
private fun JobCard(machine: Machine, onClick: () -> Unit) {
    val job = machine.status.job
    Surface(
        onClick = onClick,
        color = NopalSurface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, NopalBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Assignment, null, tint = NopalGreen, modifier = Modifier.size(32.dp))
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(job?.filename ?: "Trabajo en ${machine.name}", color = NopalTextPrimary, fontWeight = FontWeight.Bold)
                Text("${machine.name} · ${machine.status.state}", color = NopalTextSecondary)
                LinearProgressIndicator(
                    progress = { (job?.percent ?: 0) / 100f },
                    color = NopalGreen,
                    trackColor = NopalSurfaceElevated,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
            Text("${job?.percent ?: 0}%", color = NopalGreen, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun MaterialsScreen(onNavigate: (String) -> Unit, viewModel: WorkshopViewModel = hiltViewModel()) {
    val response by viewModel.materials.collectAsState()
    val machines by viewModel.machines.collectAsState()
    val connection by viewModel.connectionState.collectAsState()
    var selectedSpool by remember { mutableStateOf<MaterialSpool?>(null) }
    val printers = machines.filter { it.type == MachineType.PRINTER && it.driver == "klipper" }

    DashboardScaffold("Materiales", "materials", connection.label(), onNavigate) { padding ->
        when {
            !response.available -> EmptyDashboardState("Spoolman no disponible", response.reason ?: "Configura Spoolman desde NOPAL.")
            response.spools.isEmpty() -> EmptyDashboardState("Inventario vacío", "Spoolman no reportó bobinas activas.")
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
            ) {
                items(response.spools, key = { it.id }) { spool ->
                    MaterialCard(spool, response.links.filterValues { it == spool.id }.keys.toList()) { selectedSpool = spool }
                }
            }
        }
    }

    selectedSpool?.let { spool ->
        AlertDialog(
            onDismissRequest = { selectedSpool = null },
            title = { Text("Asignar ${spool.material} ${spool.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (printers.isEmpty()) Text("No hay impresoras Klipper disponibles.")
                    printers.forEach { printer ->
                        val assigned = response.links[printer.id] == spool.id
                        OutlinedButton(
                            onClick = {
                                viewModel.assignMaterial(printer.id, if (assigned) null else spool.id)
                                selectedSpool = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(if (assigned) "Quitar de ${printer.name}" else "Asignar a ${printer.name}") }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { selectedSpool = null }) { Text("CERRAR") } },
        )
    }
}

@Composable
private fun MaterialCard(spool: MaterialSpool, assignedMachines: List<String>, onAssign: () -> Unit) {
    Surface(color = NopalSurface, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, NopalBorder)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            val rgb = spool.colorHex?.removePrefix("#")?.toLongOrNull(16)
            val swatch = rgb?.let { Color(0xFF000000 or it) } ?: NopalGreen
            Box(Modifier.size(42.dp).clip(CircleShape).background(swatch))
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text("${spool.material} · ${spool.name}", color = NopalTextPrimary, fontWeight = FontWeight.Bold)
                Text("Disponible: ${spool.availableG.toInt()} g · Reservado: ${spool.reservedG.toInt()} g", color = NopalTextSecondary)
                if (assignedMachines.isNotEmpty()) Text("Asignada a ${assignedMachines.joinToString()}", color = NopalGreen)
            }
            OutlinedButton(onClick = onAssign) { Text("ASIGNAR") }
        }
    }
}

@Composable
fun AccessoriesScreen(onNavigate: (String) -> Unit, viewModel: WorkshopViewModel = hiltViewModel()) {
    val response by viewModel.accessories.collectAsState()
    val connection by viewModel.connectionState.collectAsState()
    DashboardScaffold("Accesorios", "accessories", connection.label(), onNavigate) { padding ->
        if (!response.available) {
            EmptyDashboardState("Accesorios no disponibles", response.reason ?: "Activa el plugin en NOPAL.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
            ) {
                if (response.scenes.isNotEmpty()) {
                    item { Text("ESCENAS", color = NopalGreen, fontWeight = FontWeight.Bold) }
                    items(response.scenes, key = { "scene:${it.id}" }) { scene ->
                        Surface(color = NopalSurface, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, NopalBorder)) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Power, null, tint = NopalGreen)
                                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                    Text(scene.name, color = NopalTextPrimary, fontWeight = FontWeight.Bold)
                                    Text("${scene.actionCount} acciones", color = NopalTextSecondary)
                                }
                                Button(onClick = { viewModel.runScene(scene.id) }) { Text("EJECUTAR") }
                            }
                        }
                    }
                }
                item { Text("DISPOSITIVOS", color = NopalGreen, fontWeight = FontWeight.Bold) }
                if (response.accessories.isEmpty()) item { Text("No hay accesorios registrados.", color = NopalTextSecondary) }
                items(response.accessories, key = { it.id }) { accessory ->
                    Surface(color = NopalSurface, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, NopalBorder)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Power, null, tint = if (accessory.on == true) NopalGreen else NopalTextSecondary)
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(accessory.name, color = NopalTextPrimary, fontWeight = FontWeight.Bold)
                                Text("${accessory.kind} · ${if (accessory.online) "conectado" else "sin respuesta"}", color = if (accessory.online) NopalTextSecondary else NopalWarning)
                            }
                            Switch(
                                checked = accessory.on == true,
                                enabled = accessory.online,
                                onCheckedChange = { viewModel.setAccessoryPower(accessory.id, it) },
                            )
                        }
                    }
                }
            }
        }
    }
}
