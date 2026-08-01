package com.nopal.tunascreen.feature.machine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp

internal enum class MachineCommand {
    MOVE,
    EXTRUDE,
    PREHEAT,
    LASER_POWER,
    AIR_ASSIST,
    WORK_ZERO,
    SPINDLE,
    COOLANT,
    FAN,
    SPEED_FACTOR,
    FLOW_FACTOR,
    Z_OFFSET,
    MACROS,
    CONSOLE,
}

@Composable
internal fun MachineActionDialog(
    command: MachineCommand,
    viewModel: MachineViewModel,
    onDismiss: () -> Unit,
) {
    when (command) {
        MachineCommand.MOVE -> MoveDialog(viewModel, onDismiss)
        MachineCommand.EXTRUDE -> ExtrudeDialog(viewModel, onDismiss)
        MachineCommand.PREHEAT -> PreheatDialog(viewModel, onDismiss)
        MachineCommand.LASER_POWER -> PowerDialog(
            title = "Potencia láser",
            label = "Potencia S (0–100000)",
            initial = "500",
            safetyText = "Verifica que el área esté protegida antes de encender el láser.",
            onEnable = { viewModel.setLaserPower(true, it) },
            onDisable = { viewModel.setLaserPower(false, 0.0) },
            onDismiss = onDismiss,
        )
        MachineCommand.AIR_ASSIST -> ToggleDialog(
            title = "Air assist",
            body = "Controla la salida M8/M9 configurada para asistencia de aire.",
            onEnable = { viewModel.setAirAssist(true) },
            onDisable = { viewModel.setAirAssist(false) },
            onDismiss = onDismiss,
        )
        MachineCommand.WORK_ZERO -> ConfirmDialog(
            title = "Establecer cero XYZ",
            body = "La posición actual se guardará como cero de trabajo para X, Y y Z.",
            confirmLabel = "ESTABLECER",
            onConfirm = { viewModel.setWorkZero() },
            onDismiss = onDismiss,
        )
        MachineCommand.SPINDLE -> PowerDialog(
            title = "Husillo CNC",
            label = "RPM",
            initial = "10000",
            safetyText = "Confirma que la herramienta y la pieza estén bien sujetas.",
            onEnable = { viewModel.setSpindle(true, it) },
            onDisable = { viewModel.setSpindle(false, 0.0) },
            onDismiss = onDismiss,
        )
        MachineCommand.COOLANT -> ToggleDialog(
            title = "Refrigerante",
            body = "Controla la salida M8/M9 configurada para refrigerante.",
            onEnable = { viewModel.setCoolant(true) },
            onDisable = { viewModel.setCoolant(false) },
            onDismiss = onDismiss,
        )
        MachineCommand.FAN -> ValueDialog(
            title = "Ventilador de capa",
            label = "Potencia (0–100 %)",
            initial = "100",
            range = 0.0..100.0,
            confirmLabel = "APLICAR",
            onConfirm = viewModel::setFan,
            onDismiss = onDismiss,
        )
        MachineCommand.SPEED_FACTOR -> ValueDialog(
            title = "Velocidad de impresión",
            label = "Factor (10–300 %)",
            initial = "100",
            range = 10.0..300.0,
            confirmLabel = "APLICAR",
            onConfirm = viewModel::setSpeedFactor,
            onDismiss = onDismiss,
        )
        MachineCommand.FLOW_FACTOR -> ValueDialog(
            title = "Flujo de material",
            label = "Factor (10–300 %)",
            initial = "100",
            range = 10.0..300.0,
            confirmLabel = "APLICAR",
            onConfirm = viewModel::setFlowFactor,
            onDismiss = onDismiss,
        )
        MachineCommand.Z_OFFSET -> ValueDialog(
            title = "Offset Z",
            label = "Offset absoluto (-10 a 10 mm)",
            initial = "0.00",
            range = -10.0..10.0,
            confirmLabel = "MOVER Z",
            onConfirm = viewModel::setZOffset,
            onDismiss = onDismiss,
        )
        MachineCommand.MACROS -> MacrosDialog(viewModel, onDismiss)
        MachineCommand.CONSOLE -> ConsoleDialog(viewModel, onDismiss)
    }
}

@Composable
private fun MoveDialog(viewModel: MachineViewModel, onDismiss: () -> Unit) {
    var axis by remember { mutableStateOf("X") }
    var distance by remember { mutableStateOf("10") }
    var feed by remember { mutableStateOf("1000") }
    val distanceValue = distance.toDoubleOrNull()
    val feedValue = feed.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mover eje") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("X", "Y", "Z").forEach {
                        FilterChip(selected = axis == it, onClick = { axis = it }, label = { Text(it) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("X", "Y", "Z").forEach { homeAxis ->
                        TextButton(onClick = { viewModel.home(homeAxis) }) {
                            Text("HOME $homeAxis")
                        }
                    }
                }
                NumberField(distance, { distance = it }, "Distancia (mm)")
                NumberField(feed, { feed = it }, "Velocidad (mm/min)")
            }
        },
        confirmButton = {
            Button(
                enabled = distanceValue != null && feedValue != null,
                onClick = {
                    viewModel.move(axis, distanceValue ?: return@Button, feedValue ?: return@Button)
                    onDismiss()
                },
            ) { Text("MOVER") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } },
    )
}

@Composable
private fun ValueDialog(
    title: String,
    label: String,
    initial: String,
    range: ClosedFloatingPointRange<Double>,
    confirmLabel: String,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    val numeric = value.toDoubleOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { NumberField(value, { value = it }, label) },
        confirmButton = {
            Button(
                enabled = numeric != null && numeric in range,
                onClick = {
                    onConfirm(numeric ?: return@Button)
                    onDismiss()
                },
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } },
    )
}

@Composable
private fun MacrosDialog(viewModel: MachineViewModel, onDismiss: () -> Unit) {
    val macros by viewModel.macros.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadMacros() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Macros Klipper") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (macros.isEmpty()) {
                    Text("No hay macros disponibles.")
                } else {
                    macros.forEach { macro ->
                        TextButton(
                            onClick = {
                                viewModel.runMacro(macro.name)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(macro.name)
                                if (macro.description.isNotBlank()) Text(macro.description)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = viewModel::loadMacros) { Text("ACTUALIZAR") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CERRAR") } },
    )
}

@Composable
private fun ConsoleDialog(viewModel: MachineViewModel, onDismiss: () -> Unit) {
    val messages by viewModel.console.collectAsState()
    var command by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { viewModel.loadConsole() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Consola G-code") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 240.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (messages.isEmpty()) Text("Sin mensajes recientes.")
                    messages.takeLast(30).forEach { message ->
                        Text(message.message.ifBlank { "(${message.type ?: "evento"})" })
                    }
                }
                OutlinedTextField(
                    value = command,
                    onValueChange = { command = it },
                    label = { Text("Comando") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                enabled = command.isNotBlank(),
                onClick = {
                    viewModel.sendConsoleCommand(command.trim())
                    command = ""
                },
            ) { Text("ENVIAR") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { viewModel.loadConsole() }) { Text("ACTUALIZAR") }
                TextButton(onClick = onDismiss) { Text("CERRAR") }
            }
        },
    )
}

@Composable
private fun ExtrudeDialog(viewModel: MachineViewModel, onDismiss: () -> Unit) {
    var distance by remember { mutableStateOf("10") }
    var feed by remember { mutableStateOf("300") }
    val distanceValue = distance.toDoubleOrNull()
    val feedValue = feed.toDoubleOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Extruir filamento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Usa una distancia negativa para retraer.")
                NumberField(distance, { distance = it }, "Distancia (mm)")
                NumberField(feed, { feed = it }, "Velocidad (mm/min)")
            }
        },
        confirmButton = {
            Button(
                enabled = distanceValue != null && feedValue != null,
                onClick = {
                    viewModel.extrude(distanceValue ?: return@Button, feedValue ?: return@Button)
                    onDismiss()
                },
            ) { Text("EJECUTAR") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } },
    )
}

@Composable
private fun PreheatDialog(viewModel: MachineViewModel, onDismiss: () -> Unit) {
    var hotend by remember { mutableStateOf("200") }
    var bed by remember { mutableStateOf("60") }
    val hotendValue = hotend.toDoubleOrNull()
    val bedValue = bed.toDoubleOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Precalentar") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberField(hotend, { hotend = it }, "Hotend (°C)")
                NumberField(bed, { bed = it }, "Cama (°C)")
            }
        },
        confirmButton = {
            Button(
                enabled = hotendValue != null && bedValue != null,
                onClick = {
                    viewModel.setTemperature("extruder", hotendValue ?: return@Button)
                    viewModel.setTemperature("heater_bed", bedValue ?: return@Button)
                    onDismiss()
                },
            ) { Text("APLICAR") }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    viewModel.setTemperature("extruder", 0.0)
                    viewModel.setTemperature("heater_bed", 0.0)
                    onDismiss()
                },
            ) { Text("APAGAR") }
        },
    )
}

@Composable
private fun PowerDialog(
    title: String,
    label: String,
    initial: String,
    safetyText: String,
    onEnable: (Double) -> Unit,
    onDisable: () -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    val numeric = value.toDoubleOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(safetyText)
                NumberField(value, { value = it }, label)
            }
        },
        confirmButton = {
            Button(
                enabled = numeric != null,
                onClick = {
                    onEnable(numeric ?: return@Button)
                    onDismiss()
                },
            ) { Text("ENCENDER") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    onDisable()
                    onDismiss()
                }) { Text("APAGAR") }
                TextButton(onClick = onDismiss) { Text("CANCELAR") }
            }
        },
    )
}

@Composable
private fun ToggleDialog(
    title: String,
    body: String,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            Button(onClick = {
                onEnable()
                onDismiss()
            }) { Text("ENCENDER") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    onDisable()
                    onDismiss()
                }) { Text("APAGAR") }
                TextButton(onClick = onDismiss) { Text("CANCELAR") }
            }
        },
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            Button(onClick = {
                onConfirm()
                onDismiss()
            }) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } },
    )
}

@Composable
private fun NumberField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}
