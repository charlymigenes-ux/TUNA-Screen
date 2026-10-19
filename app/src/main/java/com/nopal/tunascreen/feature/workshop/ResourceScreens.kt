package com.nopal.tunascreen.feature.workshop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import com.nopal.tunascreen.core.model.WorkshopAccessory
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Air
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
            val on = response.accessories.count { it.on == true }
            val silent = response.accessories.count { !it.online }
            // Mosaico de mini-fichas verticales: cuantas quepan por ancho.
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 118.dp),
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 10.dp),
            ) {
                if (response.scenes.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SectionLabel("ESCENAS", "${response.scenes.size}")
                    }
                    gridItems(response.scenes, key = { "scene:${it.id}" }) { scene ->
                        SceneTile(scene.name, scene.actionCount) { viewModel.runScene(scene.id) }
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionLabel(
                        "DISPOSITIVOS",
                        buildString {
                            append("$on encendidos")
                            if (silent > 0) append(" · $silent sin respuesta")
                        },
                    )
                }
                if (response.accessories.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text("No hay accesorios registrados.", color = NopalTextSecondary)
                    }
                }
                gridItems(response.accessories, key = { it.id }) { accessory ->
                    AccessoryTile(accessory) { viewModel.setAccessoryPower(accessory.id, accessory.on != true) }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(title: String, detail: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, color = NopalGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
        Text(detail, color = NopalTextSecondary, style = MaterialTheme.typography.labelMedium)
    }
}

private val SceneTone = Color(0xFF27BDEB)

private fun accessoryIcon(kind: String): ImageVector {
    val k = kind.lowercase()
    return when {
        "light" in k || "lamp" in k || "led" in k || "luz" in k -> Icons.Default.Lightbulb
        "fan" in k || "vent" in k || "air" in k -> Icons.Default.Air
        "heat" in k || "calef" in k -> Icons.Default.LocalFireDepartment
        else -> Icons.Default.Power
    }
}

/**
 * Ficha vertical de un accesorio. Toda la ficha enciende o apaga (objetivo
 * táctil grande) y el estado se lee también como texto, no solo por color:
 * ENCENDIDO con brillo verde, APAGADO neutro y SIN RESPUESTA atenuado.
 */
@Composable
private fun AccessoryTile(accessory: WorkshopAccessory, onToggle: () -> Unit) {
    val isOn = accessory.on == true
    val tone = when {
        !accessory.online -> NopalWarning
        isOn -> NopalGreen
        else -> NopalTextSecondary
    }
    val stateText = when {
        !accessory.online -> "SIN RESPUESTA"
        isOn -> "ENCENDIDO"
        else -> "APAGADO"
    }
    val shape = RoundedCornerShape(14.dp)
    Surface(
        onClick = onToggle,
        enabled = accessory.online,
        color = if (isOn && accessory.online) NopalGreen.copy(alpha = 0.10f) else NopalSurface,
        shape = shape,
        border = BorderStroke(1.dp, if (isOn && accessory.online) NopalGreen.copy(alpha = 0.85f) else NopalBorder),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 148.dp)
            .then(if (isOn && accessory.online) Modifier.shadow(8.dp, shape, ambientColor = NopalGreen, spotColor = NopalGreen) else Modifier)
            .alpha(if (accessory.online) 1f else 0.7f),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(tone.copy(alpha = if (isOn && accessory.online) 0.22f else 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(accessoryIcon(accessory.kind), null, tint = tone, modifier = Modifier.size(28.dp))
            }
            Text(
                accessory.name,
                color = NopalTextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                accessory.kind,
                color = NopalTextSecondary,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(tone))
                Text(stateText, color = tone, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            Switch(
                checked = isOn,
                enabled = accessory.online,
                onCheckedChange = null,
            )
        }
    }
}

@Composable
private fun SceneTile(name: String, actionCount: Int, onRun: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Surface(
        onClick = onRun,
        color = SceneTone.copy(alpha = 0.08f),
        shape = shape,
        border = BorderStroke(1.dp, SceneTone.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth().heightIn(min = 148.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(SceneTone.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Default.PlayArrow, null, tint = SceneTone, modifier = Modifier.size(30.dp)) }
            Text(
                name,
                color = NopalTextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "$actionCount acciones",
                color = NopalTextSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
            Surface(color = SceneTone.copy(alpha = 0.18f), shape = RoundedCornerShape(20.dp)) {
                Text(
                    "EJECUTAR",
                    color = SceneTone,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                )
            }
        }
    }
}
