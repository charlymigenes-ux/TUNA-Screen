package com.nopal.tunascreen.feature.machine.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HotTub
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nopal.tunascreen.core.model.Machine
import com.nopal.tunascreen.core.model.TempPair
import com.nopal.tunascreen.core.model.ThermalPhase
import com.nopal.tunascreen.core.model.thermalPhaseOf
import com.nopal.tunascreen.core.repository.ConnectionState
import com.nopal.tunascreen.feature.machine.MachineCommand
import com.nopal.tunascreen.feature.machine.MachineViewModel
import com.nopal.tunascreen.ui.theme.NopalBorder
import com.nopal.tunascreen.ui.theme.NopalChrome
import com.nopal.tunascreen.ui.theme.NopalDanger
import com.nopal.tunascreen.ui.theme.NopalGreen
import com.nopal.tunascreen.ui.theme.NopalSurface
import com.nopal.tunascreen.ui.theme.NopalSurfaceElevated
import com.nopal.tunascreen.ui.theme.NopalTextPrimary
import com.nopal.tunascreen.ui.theme.NopalTextSecondary
import com.nopal.tunascreen.ui.theme.NopalWarning
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val PRINTING_STATES = setOf("printing", "running", "working")

// Klipper rechaza extruir por debajo de min_extrude_temp (170 °C por defecto);
// solo sirve para avisar antes de mandar la orden, el firmware decide.
private const val MIN_EXTRUDE_TEMP_C = 170.0

private const val HOTEND_MAX_C = 300.0
private const val BED_MAX_C = 120.0
private const val XY_FEED = 1500.0
private const val Z_FEED = 600.0
private const val EXTRUDE_FEED = 300.0
private const val CONSOLE_REFRESH_MS = 5_000L

// Tonos de estado: caliente (naranja), enfriando (azul), listo (verde).
private val HeatColor = Color(0xFFFF8A3D)
private val CoolColor = Color(0xFF27BDEB)
private val PauseNeon = Color(0xFFFFB020)
private val CancelNeon = Color(0xFFFF4D5E)
private val HeatNeon = Color(0xFF3DDC84)

private val PanelGap = 6.dp
private val ColumnGap = 8.dp

/**
 * Tablero horizontal de impresora (5″ a PC): barra de estado arriba, menú
 * lateral y tres columnas -- estado y trabajo / temperaturas, extrusión y
 * ventilador / movimiento y consola --, todo en una pantalla sin diálogos para
 * lo de uso frecuente. Solo se muestra lo que `machine.actions` declara; lo que
 * NOPAL no reporta (capa, miniatura, ventiladores del hotend y de la cámara,
 * clima) no se simula.
 */
@Composable
internal fun PrinterLandscapeDashboard(
    machine: Machine,
    viewModel: MachineViewModel,
    isJobActive: Boolean,
    error: String?,
    onDismissError: () -> Unit,
    onBack: () -> Unit,
    onCancelRequest: () -> Unit,
    onCommand: (MachineCommand) -> Unit,
    modifier: Modifier = Modifier,
) {
    val printing = machine.status.state in PRINTING_STATES
    val host by viewModel.serverHost.collectAsState()
    val connection by viewModel.connection.collectAsState()

    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(ColumnGap)) {
        PrinterStatusBar(machine, host, connection)
        error?.let { ErrorStrip(it, onDismissError) }
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(ColumnGap),
        ) {
            SideNav(machine, onBack, onCommand)
            ScrollColumn(Modifier.weight(1.15f)) {
                JobPanel(machine)
                QuickActions(machine, viewModel, isJobActive, onCancelRequest, onCommand)
            }
            ScrollColumn(Modifier.weight(1f)) {
                TemperaturesPanel(machine)
                ExtrusionPanel(machine, viewModel, printing)
                FanPanel(machine, viewModel)
            }
            ScrollColumn(Modifier.weight(1f)) {
                val hasMovement = machine.actions.contains("move") || machine.actions.contains("home")
                val hasConsole = machine.actions.contains("send_console_command")
                // Movimiento y consola comparten el espacio: se abre uno y el otro
                // se contrae a su encabezado, con transición.
                var open by remember { mutableStateOf(RightSection.MOVEMENT) }
                MovementPanel(
                    machine, viewModel, printing,
                    expanded = open == RightSection.MOVEMENT || !hasConsole,
                    onHeaderClick = if (hasConsole) ({ open = RightSection.MOVEMENT }) else null,
                )
                ConsolePanel(
                    machine, viewModel,
                    expanded = open == RightSection.CONSOLE || !hasMovement,
                    onHeaderClick = if (hasMovement) ({ open = RightSection.CONSOLE }) else null,
                )
            }
        }
    }
}

private enum class RightSection { MOVEMENT, CONSOLE }

@Composable
private fun ScrollColumn(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.fillMaxHeight().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(ColumnGap),
        content = content,
    )
}

// ── Estado térmico ──

@Composable
private fun phaseColor(phase: ThermalPhase): Color = when (phase) {
    ThermalPhase.HEATING -> HeatColor
    ThermalPhase.COOLING -> CoolColor
    ThermalPhase.READY -> NopalGreen
    ThermalPhase.IDLE -> NopalTextSecondary
}

private data class ThermalSummary(val phase: ThermalPhase, val text: String)

private fun thermalSummary(machine: Machine): ThermalSummary? {
    val heaters = listOf("Extrusor" to machine.status.hotend, "Cama" to machine.status.bed)
        .map { (name, temp) -> Triple(name, temp, thermalPhaseOf(temp)) }

    fun detail(phase: ThermalPhase): String =
        heaters.filter { it.third == phase }.joinToString(" · ") { (name, temp, _) ->
            val current = temp?.current?.roundToInt()
            val target = temp?.target?.takeIf { it > 0 }?.roundToInt()
            if (target != null) "$name $current→$target°C" else "$name $current°C"
        }

    return when {
        heaters.any { it.third == ThermalPhase.HEATING } ->
            ThermalSummary(ThermalPhase.HEATING, "Calentando · ${detail(ThermalPhase.HEATING)}")
        heaters.any { it.third == ThermalPhase.COOLING } ->
            ThermalSummary(ThermalPhase.COOLING, "Enfriando · ${detail(ThermalPhase.COOLING)}")
        heaters.any { it.third == ThermalPhase.READY } ->
            ThermalSummary(ThermalPhase.READY, "Temperatura lista")
        else -> null
    }
}

@Composable
private fun ThermalChip(machine: Machine) {
    val summary = thermalSummary(machine) ?: return
    val color = phaseColor(summary.phase)
    val moving = summary.phase == ThermalPhase.HEATING || summary.phase == ThermalPhase.COOLING
    val pulse by rememberInfiniteTransition(label = "thermal").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "thermalPulse",
    )
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.7f)),
        modifier = Modifier.height(24.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                when (summary.phase) {
                    ThermalPhase.HEATING -> Icons.Default.LocalFireDepartment
                    ThermalPhase.COOLING -> Icons.Default.AcUnit
                    else -> Icons.Default.CheckCircle
                },
                null,
                tint = color,
                modifier = Modifier.size(14.dp).alpha(if (moving) pulse else 1f),
            )
            Text(
                summary.text,
                color = color,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

// ── Barra de estado ──

@Composable
private fun PrinterStatusBar(machine: Machine, host: String?, connection: ConnectionState) {
    val look = statusLook(machine)
    val time by produceState(initialValue = clockNow()) {
        while (true) {
            value = clockNow()
            delay(30_000)
        }
    }
    val date = remember(time) {
        LocalDate.now().format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale("es", "MX")))
    }
    val linked = connection == ConnectionState.LIVE
    Surface(
        color = NopalChrome,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, NopalBorder),
        modifier = Modifier.fillMaxWidth().height(32.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                if (linked) Icons.Default.Wifi else Icons.Default.WifiOff,
                if (linked) "Conectado a NOPAL" else "Sin conexión con NOPAL",
                tint = if (linked) NopalGreen else NopalWarning,
                modifier = Modifier.size(16.dp),
            )
            Text(host ?: "--", color = NopalTextPrimary, style = MaterialTheme.typography.labelMedium)
            Box(Modifier.width(1.dp).height(14.dp).background(NopalBorder))
            Icon(Icons.Default.Print, null, tint = look.color, modifier = Modifier.size(16.dp))
            Text(
                "${machine.name} · ${look.label.lowercase().replaceFirstChar { it.uppercase() }}",
                color = look.color,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { ThermalChip(machine) }
            Text(time, color = NopalTextPrimary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text(date, color = NopalTextSecondary, style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun clockNow(): String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

@Composable
private fun ErrorStrip(message: String, onDismiss: () -> Unit) {
    Surface(color = NopalDanger.copy(alpha = 0.18f), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(message, color = NopalTextPrimary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            TextButton(onClick = onDismiss) { Text("OK", color = NopalGreen) }
        }
    }
}

// ── Menú lateral ──

@Composable
private fun SideNav(machine: Machine, onBack: () -> Unit, onCommand: (MachineCommand) -> Unit) {
    var showAdjustments by remember { mutableStateOf(false) }
    val adjustments = listOf(
        Triple("set_speed_factor", "Velocidad ${machine.status.speedFactor?.roundToInt() ?: 100}%", Icons.Default.Speed) to MachineCommand.SPEED_FACTOR,
        Triple("set_flow_factor", "Flujo ${machine.status.flowFactor?.roundToInt() ?: 100}%", Icons.Default.Tune) to MachineCommand.FLOW_FACTOR,
        Triple("set_z_offset", "Offset Z ${"%.2f".format(machine.status.zOffset ?: 0.0)} mm", Icons.Default.KeyboardArrowDown) to MachineCommand.Z_OFFSET,
    ).filter { (spec, _) -> machine.actions.contains(spec.first) }

    if (showAdjustments) {
        AlertDialog(
            onDismissRequest = { showAdjustments = false },
            title = { Text("Ajustes de impresión") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    adjustments.forEach { (spec, command) ->
                        QuickActionButton(
                            label = spec.second,
                            icon = spec.third,
                            onClick = {
                                showAdjustments = false
                                onCommand(command)
                            },
                            enabled = machine.online,
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAdjustments = false }) { Text("CERRAR") } },
        )
    }

    Surface(
        color = NopalChrome,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, NopalBorder),
        modifier = Modifier.width(76.dp).fillMaxHeight(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(6.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            NavItem("TABLERO", Icons.Default.Home, selected = true) {}
            NavItem("TALLER", Icons.Default.GridView, selected = false, onClick = onBack)
            if (machine.actions.contains("run_macro")) {
                NavItem("MACROS", Icons.Default.Apps, selected = false) { onCommand(MachineCommand.MACROS) }
            }
            if (adjustments.isNotEmpty()) {
                NavItem("AJUSTES", Icons.Default.Settings, selected = false) { showAdjustments = true }
            }
        }
    }
}

@Composable
private fun NavItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (selected) NopalSurfaceElevated else Color.Transparent,
        shape = RoundedCornerShape(10.dp),
        border = if (selected) BorderStroke(1.dp, NopalGreen) else null,
        modifier = Modifier.fillMaxWidth().height(52.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, null, tint = if (selected) NopalGreen else NopalTextPrimary, modifier = Modifier.size(22.dp))
            Text(
                label,
                color = if (selected) NopalGreen else NopalTextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

// ── Paneles ──

@Composable
private fun Panel(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    expanded: Boolean = true,
    onHeaderClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        color = NopalSurface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (onHeaderClick != null && expanded) NopalGreen.copy(alpha = 0.6f) else NopalBorder),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 36.dp)
                    .then(if (onHeaderClick != null) Modifier.clickable(onClick = onHeaderClick) else Modifier),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, color = NopalTextPrimary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    trailing?.invoke()
                    if (onHeaderClick != null) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            if (expanded) "Contraer $title" else "Abrir $title",
                            tint = NopalTextSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(280)) + fadeIn(tween(280)),
                exit = shrinkVertically(tween(280)) + fadeOut(tween(180)),
            ) {
                Column(
                    modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(PanelGap),
                    content = content,
                )
            }
        }
    }
}

private data class StatusLook(val label: String, val hint: String, val color: Color, val icon: ImageVector)

@Composable
private fun statusLook(machine: Machine): StatusLook = when {
    !machine.online -> StatusLook("SIN CONEXIÓN", "NOPAL no ve esta impresora.", NopalDanger, Icons.Default.Warning)
    else -> when (machine.status.state) {
        "printing", "running", "working" ->
            StatusLook("IMPRIMIENDO", "Trabajo en curso.", NopalGreen, Icons.Default.CheckCircle)
        "paused" -> StatusLook("EN PAUSA", "El trabajo está detenido.", NopalWarning, Icons.Default.PauseCircle)
        "error" -> StatusLook("ERROR", "Revisa la impresora.", NopalDanger, Icons.Default.Warning)
        "completed" -> StatusLook("TERMINADO", "El trabajo terminó.", NopalGreen, Icons.Default.CheckCircle)
        "idle", "ready", "standby" -> StatusLook("LISTA", "Lista para trabajar.", NopalGreen, Icons.Default.CheckCircle)
        else -> StatusLook(machine.status.state.uppercase(), "", NopalTextSecondary, Icons.Default.CheckCircle)
    }
}

@Composable
private fun JobPanel(machine: Machine) {
    val job = machine.status.job
    val percent = (job?.percent ?: 0).coerceIn(0, 100)
    Panel("TRABAJO") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)).background(NopalSurfaceElevated),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.ViewInAr, null, tint = NopalTextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(32.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    job?.filename ?: "Ningún trabajo activo",
                    color = NopalTextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { percent / 100f },
                        modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape),
                        color = NopalGreen,
                        trackColor = NopalSurfaceElevated,
                    )
                    Text(
                        "$percent%",
                        color = NopalGreen,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                MetricLine("ALTURA Z", machine.status.position?.z?.let { "%.1f mm".format(it) })
                MetricLine("VELOCIDAD", machine.status.speedFactor?.let { "${it.roundToInt()}%" })
                MetricLine("FLUJO", machine.status.flowFactor?.let { "${it.roundToInt()}%" })
            }
        }
        Surface(color = NopalSurfaceElevated, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TimeBlock("TRANSCURRIDO", job?.elapsedS)
                Icon(Icons.Default.Schedule, null, tint = NopalTextSecondary, modifier = Modifier.size(18.dp))
                TimeBlock("RESTANTE", job?.remainingS)
            }
        }
    }
}

@Composable
private fun MetricLine(label: String, value: String?) {
    if (value == null) return
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = NopalTextSecondary, style = MaterialTheme.typography.labelSmall)
        Text(value, color = NopalTextPrimary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TimeBlock(label: String, seconds: Long?) {
    Column {
        Text(label, color = NopalTextSecondary, style = MaterialTheme.typography.labelSmall)
        Text(clock(seconds), color = NopalTextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

private fun clock(seconds: Long?): String {
    if (seconds == null) return "--:--:--"
    return "%02d:%02d:%02d".format(seconds / 3600, (seconds % 3600) / 60, seconds % 60)
}

@Composable
private fun QuickActions(
    machine: Machine,
    viewModel: MachineViewModel,
    isJobActive: Boolean,
    onCancelRequest: () -> Unit,
    onCommand: (MachineCommand) -> Unit,
) {
    val paused = machine.status.state == "paused"
    Panel("ACCIONES RÁPIDAS") {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ActionTile(
                label = if (paused) "REANUDAR" else "PAUSAR",
                icon = if (paused) Icons.Default.PlayArrow else Icons.Default.Pause,
                tint = PauseNeon,
                enabled = isJobActive && machine.supports(if (paused) "resume" else "pause"),
                modifier = Modifier.weight(1f),
            ) { if (paused) viewModel.resume() else viewModel.pause() }
            ActionTile(
                label = "CANCELAR",
                icon = Icons.Default.Stop,
                tint = CancelNeon,
                enabled = isJobActive && machine.supports("cancel"),
                modifier = Modifier.weight(1f),
                onClick = onCancelRequest,
            )
            ActionTile(
                label = "CALENTAR",
                icon = Icons.Default.DeviceThermostat,
                tint = HeatNeon,
                enabled = machine.supports("set_temperature"),
                modifier = Modifier.weight(1f),
            ) { onCommand(MachineCommand.PREHEAT) }
            ActionTile(
                label = "ENFRIAR",
                icon = Icons.Default.AcUnit,
                tint = CoolColor,
                enabled = machine.supports("set_temperature"),
                modifier = Modifier.weight(1f),
            ) {
                viewModel.setTemperature("hotend", 0.0)
                viewModel.setTemperature("bed", 0.0)
            }
        }
    }
}

@Composable
private fun ActionTile(
    label: String,
    icon: ImageVector,
    tint: Color,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    // Cada acción conserva su color aunque esté deshabilitada (solo se atenúa),
    // para que se reconozcan de un vistazo; habilitada lleva brillo neón.
    val color = if (enabled) tint else tint.copy(alpha = 0.38f)
    val shape = RoundedCornerShape(10.dp)
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = tint.copy(alpha = if (enabled) 0.12f else 0.05f),
        shape = shape,
        border = BorderStroke(1.dp, color.copy(alpha = if (enabled) 0.85f else 0.45f)),
        modifier = modifier
            .height(56.dp)
            .then(if (enabled) Modifier.shadow(6.dp, shape, ambientColor = tint, spotColor = tint) else Modifier),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Text(label, color = color, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            Box(
                Modifier
                    .padding(top = 3.dp)
                    .width(28.dp)
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

@Composable
private fun TemperaturesPanel(machine: Machine) {
    Panel("TEMPERATURAS") {
        TemperatureRow("EXTRUSOR", Icons.Default.Thermostat, machine.status.hotend, HOTEND_MAX_C)
        TemperatureRow("CAMA", Icons.Default.HotTub, machine.status.bed, BED_MAX_C)
    }
}

@Composable
private fun TemperatureRow(label: String, icon: ImageVector, temp: TempPair?, maxC: Double) {
    val current = temp?.current
    val tone = phaseColor(thermalPhaseOf(temp))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = tone, modifier = Modifier.size(26.dp))
        Column(modifier = Modifier.width(78.dp)) {
            Text(label, color = NopalTextSecondary, style = MaterialTheme.typography.labelSmall)
            Text(
                current?.let { "${it.roundToInt()}°C" } ?: "--°C",
                color = tone,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("OBJETIVO", color = NopalTextSecondary, style = MaterialTheme.typography.labelSmall)
                Text(
                    temp?.target?.let { "${it.roundToInt()}°C" } ?: "--",
                    color = NopalTextPrimary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            LinearProgressIndicator(
                progress = { ((current ?: 0.0) / maxC).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                color = tone,
                trackColor = NopalSurfaceElevated,
            )
        }
    }
}

@Composable
private fun StepChips(options: List<Double>, selected: Double, onSelect: (Double) -> Unit) {
    // Fichas compactas que se reparten el ancho: con 4 opciones en una columna
    // angosta los FilterChip estándar se aplastan.
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { step ->
            val isSelected = selected == step
            Surface(
                onClick = { onSelect(step) },
                color = if (isSelected) NopalGreen.copy(alpha = 0.18f) else NopalSurfaceElevated,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (isSelected) NopalGreen else NopalBorder),
                modifier = Modifier.weight(1f).height(34.dp),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        "${if (step % 1.0 == 0.0) step.toInt().toString() else step.toString()} mm",
                        color = if (isSelected) NopalGreen else NopalTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExtrusionPanel(machine: Machine, viewModel: MachineViewModel, printing: Boolean) {
    if (!machine.actions.contains("extrude")) return
    var length by remember { mutableStateOf(10.0) }
    val hotend = machine.status.hotend?.current
    val cold = hotend != null && hotend < MIN_EXTRUDE_TEMP_C
    val enabled = machine.supports("extrude") && !printing && !cold

    Panel("EXTRUSIÓN") {
        StepChips(listOf(1.0, 5.0, 10.0, 25.0), length) { length = it }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(
                onClick = { viewModel.extrude(-length, EXTRUDE_FEED) },
                enabled = enabled,
                contentPadding = PaddingValues(horizontal = 4.dp),
                modifier = Modifier.weight(1f).height(40.dp),
            ) {
                Icon(Icons.Default.KeyboardDoubleArrowUp, null, modifier = Modifier.size(18.dp))
                Text(" RETRAER", fontSize = 12.sp)
            }
            Button(
                onClick = { viewModel.extrude(length, EXTRUDE_FEED) },
                enabled = enabled,
                contentPadding = PaddingValues(horizontal = 4.dp),
                modifier = Modifier.weight(1f).height(40.dp),
            ) {
                Icon(Icons.Default.KeyboardDoubleArrowDown, null, modifier = Modifier.size(18.dp))
                Text(" EXTRUIR", fontSize = 12.sp)
            }
        }
        val hint = when {
            printing -> "Pausa el trabajo para extruir."
            cold -> "Calienta el hotend a ${MIN_EXTRUDE_TEMP_C.roundToInt()} °C o más para extruir (ahora ${hotend?.roundToInt()} °C)."
            else -> null
        }
        hint?.let { Text(it, color = NopalWarning, style = MaterialTheme.typography.labelSmall) }
    }
}

@Composable
private fun FanPanel(machine: Machine, viewModel: MachineViewModel) {
    if (!machine.actions.contains("set_fan")) return
    val reported = (machine.status.fanPercent ?: 0.0).toFloat().coerceIn(0f, 100f)
    var dragged by remember { mutableFloatStateOf(Float.NaN) }
    // El valor local vive hasta que NOPAL reporta un porcentaje nuevo; así el
    // slider no salta al valor viejo durante los ~2 s del siguiente snapshot.
    LaunchedEffect(reported) { dragged = Float.NaN }
    val shown = if (dragged.isNaN()) reported else dragged

    Panel("VENTILADOR") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Air, null, tint = NopalTextSecondary, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                Text("PIEZA", color = NopalTextSecondary, style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = shown,
                    onValueChange = { dragged = it },
                    onValueChangeFinished = {
                        if (!dragged.isNaN()) viewModel.setFan(dragged.roundToInt().toDouble())
                    },
                    valueRange = 0f..100f,
                    enabled = machine.online,
                    modifier = Modifier.height(28.dp),
                )
            }
            Text(
                "${shown.roundToInt()}%",
                color = NopalTextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun MovementPanel(
    machine: Machine,
    viewModel: MachineViewModel,
    printing: Boolean,
    expanded: Boolean,
    onHeaderClick: (() -> Unit)?,
) {
    if (!machine.actions.contains("move") && !machine.actions.contains("home")) return
    var step by remember { mutableStateOf(10.0) }
    val canMove = machine.supports("move") && !printing
    val canHome = machine.supports("home") && !printing
    val pos = machine.status.position

    Panel(
        "MOVIMIENTO",
        expanded = expanded,
        onHeaderClick = onHeaderClick,
        trailing = {
            Text(
                "X ${pos?.x?.let { "%.1f".format(it) } ?: "--"}  Y ${pos?.y?.let { "%.1f".format(it) } ?: "--"}  Z ${pos?.z?.let { "%.1f".format(it) } ?: "--"}",
                color = NopalTextSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
        },
    ) {
        StepChips(listOf(0.1, 1.0, 10.0, 50.0), step) { step = it }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                JogButton(Icons.Default.KeyboardArrowUp, "Y positivo", canMove) { viewModel.move("Y", step, XY_FEED) }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    JogButton(Icons.Default.KeyboardArrowLeft, "X negativo", canMove) { viewModel.move("X", -step, XY_FEED) }
                    JogButton(Icons.Default.Home, "Home de todos los ejes", canHome) { viewModel.home() }
                    JogButton(Icons.Default.KeyboardArrowRight, "X positivo", canMove) { viewModel.move("X", step, XY_FEED) }
                }
                JogButton(Icons.Default.KeyboardArrowDown, "Y negativo", canMove) { viewModel.move("Y", -step, XY_FEED) }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                JogLabelButton("Z+", canMove) { viewModel.move("Z", step, Z_FEED) }
                JogLabelButton("Z−", canMove) { viewModel.move("Z", -step, Z_FEED) }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("X", "Y", "Z").forEach { axis ->
                OutlinedButton(
                    onClick = { viewModel.home(axis) },
                    enabled = canHome,
                    contentPadding = PaddingValues(horizontal = 2.dp),
                    modifier = Modifier.weight(1f).height(32.dp),
                ) { Text("HOME $axis", fontSize = 11.sp) }
            }
        }
        if (printing) {
            Text("Pausa el trabajo para mover los ejes.", color = NopalWarning, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun JogButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.size(44.dp),
    ) { Icon(icon, description) }
}

@Composable
private fun JogLabelButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.size(44.dp),
    ) { Text(label, fontWeight = FontWeight.Bold) }
}

@Composable
private fun ConsolePanel(
    machine: Machine,
    viewModel: MachineViewModel,
    expanded: Boolean,
    onHeaderClick: (() -> Unit)?,
) {
    if (!machine.actions.contains("send_console_command")) return
    val messages by viewModel.console.collectAsState()
    var command by remember { mutableStateOf("") }
    // Solo consulta la consola mientras está abierta.
    LaunchedEffect(machine.id, expanded) {
        if (!expanded) return@LaunchedEffect
        while (true) {
            viewModel.loadConsole()
            delay(CONSOLE_REFRESH_MS)
        }
    }

    Panel("CONSOLA", expanded = expanded, onHeaderClick = onHeaderClick) {
        Surface(
            color = NopalSurfaceElevated,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp, max = 140.dp),
        ) {
            Column(
                modifier = Modifier.padding(6.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                if (messages.isEmpty()) {
                    Text("Sin mensajes recientes.", color = NopalTextSecondary, style = MaterialTheme.typography.labelSmall)
                }
                messages.takeLast(30).forEach { message ->
                    Text(
                        message.message.ifBlank { "(${message.type ?: "evento"})" },
                        color = NopalTextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            OutlinedTextField(
                value = command,
                onValueChange = { command = it },
                placeholder = { Text("Enviar comando…", fontSize = 12.sp) },
                singleLine = true,
                enabled = machine.online,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            )
            IconButton(
                onClick = {
                    viewModel.sendConsoleCommand(command.trim())
                    command = ""
                },
                enabled = machine.online && command.isNotBlank(),
            ) { Icon(Icons.AutoMirrored.Filled.Send, "Enviar comando", tint = NopalGreen) }
        }
    }
}
