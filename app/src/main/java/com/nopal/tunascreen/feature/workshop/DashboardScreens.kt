package com.nopal.tunascreen.feature.workshop

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.nopal.tunascreen.core.notifications.AlertEvent
import com.nopal.tunascreen.core.notifications.NotificationPrefs
import com.nopal.tunascreen.ui.theme.NopalWarning
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nopal.tunascreen.core.model.Capability
import com.nopal.tunascreen.core.model.Machine
import com.nopal.tunascreen.core.model.MachineType
import com.nopal.tunascreen.feature.workshop.components.CameraStreamView
import com.nopal.tunascreen.feature.workshop.components.MachineListCard
import com.nopal.tunascreen.feature.workshop.components.WorkshopBottomNav
import com.nopal.tunascreen.feature.workshop.components.WorkshopTopBar
import com.nopal.tunascreen.ui.theme.NopalBackground
import com.nopal.tunascreen.ui.theme.NopalDanger
import com.nopal.tunascreen.ui.theme.NopalGreen
import com.nopal.tunascreen.ui.theme.NopalSurface
import com.nopal.tunascreen.ui.theme.NopalTextPrimary
import com.nopal.tunascreen.ui.theme.NopalTextSecondary
import com.nopal.tunascreen.ui.theme.TunaThemeMode
import com.nopal.tunascreen.ui.theme.DisplayModeOverride
import com.nopal.tunascreen.ui.theme.LocalDisplayProfile

@Composable
fun MachinesScreen(
    onMachineSelected: (String) -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: WorkshopViewModel = hiltViewModel(),
) {
    val machines by viewModel.machines.collectAsState()
    val connection by viewModel.connectionState.collectAsState()
    var filter by remember { mutableStateOf<MachineType?>(null) }
    val visibleMachines = remember(machines, filter) {
        filter?.let { type -> machines.filter { it.type == type } } ?: machines
    }

    DashboardScaffold(
        title = "Máquinas",
        selectedTab = "machines",
        connectionStatus = connection.label(),
        onNavigate = onNavigate,
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MachineFilterChip("Todas", filter == null) { filter = null }
                MachineFilterChip("Impresoras", filter == MachineType.PRINTER) { filter = MachineType.PRINTER }
                MachineFilterChip("Láser", filter == MachineType.LASER) { filter = MachineType.LASER }
                MachineFilterChip("CNC", filter == MachineType.CNC) { filter = MachineType.CNC }
            }

            if (visibleMachines.isEmpty()) {
                EmptyDashboardState(
                    title = "No hay máquinas en esta categoría",
                    detail = "La lista se actualiza con los equipos reportados por NOPAL.",
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    items(visibleMachines, key = { it.id }) { machine ->
                        MachineListCard(
                            name = machine.name,
                            type = machine.type.displayName(),
                            status = if (machine.online) machine.status.state else "offline",
                            percent = machine.status.job?.percent,
                            online = machine.online,
                            onClick = { onMachineSelected(machine.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CamerasScreen(
    onNavigate: (String) -> Unit,
    viewModel: WorkshopViewModel = hiltViewModel(),
) {
    val machines by viewModel.machines.collectAsState()
    val connection by viewModel.connectionState.collectAsState()
    val session by viewModel.session.collectAsState()
    val cameraMachines = remember(machines) { machines.filter { it.has(Capability.CAMERA) } }

    DashboardScaffold(
        title = "Cámaras",
        selectedTab = "cameras",
        connectionStatus = connection.label(),
        onNavigate = onNavigate,
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                color = NopalSurface,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Transmisiones vinculadas por NOPAL. Las cámaras externas se abren " +
                        "sin compartir el token del servidor.",
                    color = NopalTextSecondary,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (cameraMachines.isEmpty()) {
                EmptyDashboardState(
                    title = "No hay cámaras disponibles",
                    detail = "NOPAL no reportó la capacidad camera en ninguna máquina.",
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    items(cameraMachines, key = { it.id }) { machine ->
                        Surface(
                            color = NopalSurface,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                CameraStreamView(
                                    streamUrl = machine.status.camera?.streamUrl,
                                    baseUrl = session.baseUrl,
                                    token = session.token,
                                    modifier = Modifier.fillMaxWidth().height(190.dp)
                                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp)),
                                )
                                Text(
                                    text = machine.status.camera?.name ?: machine.name,
                                    color = NopalTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                                Text(machine.name, color = NopalTextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class SettingsTab(val title: String) {
    CONNECTION("CONEXIÓN"),
    APPEARANCE("APARIENCIA"),
    SCREEN("PANTALLA"),
    NOTIFICATIONS("NOTIFICACIONES"),
}

@Composable
fun SettingsScreen(
    onNavigate: (String) -> Unit,
    onPairingRemoved: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val session by viewModel.session.collectAsState()
    val connection by viewModel.connectionState.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val displayMode by viewModel.displayMode.collectAsState()
    val notificationPrefs by viewModel.notificationPrefs.collectAsState()
    val detectedProfile = LocalDisplayProfile.current
    var confirmForget by remember { mutableStateOf(false) }
    var tab by rememberSaveable { mutableStateOf(SettingsTab.CONNECTION) }

    DashboardScaffold(
        title = "Ajustes",
        selectedTab = "settings",
        connectionStatus = connection.label(),
        onNavigate = onNavigate,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(
                selectedTabIndex = tab.ordinal,
                containerColor = NopalBackground,
                contentColor = NopalGreen,
                edgePadding = 8.dp,
            ) {
                SettingsTab.entries.forEach { option ->
                    Tab(
                        selected = tab == option,
                        onClick = { tab = option },
                        text = {
                            Text(
                                option.title,
                                fontWeight = FontWeight.Bold,
                                color = if (tab == option) NopalGreen else NopalTextSecondary,
                            )
                        },
                    )
                }
            }
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (tab) {
                    SettingsTab.CONNECTION -> {
                        SettingsCard {
                            SettingRow("Estado", connection.label())
                            SettingRow("Servidor", session.baseUrl ?: "Sin configurar")
                            SettingRow("Dispositivo", session.deviceId?.takeIf { it.isNotBlank() } ?: "Sin identificador")
                        }
                        OutlinedButton(
                            onClick = { confirmForget = true },
                            enabled = session.isPaired,
                        ) {
                            androidx.compose.material3.Icon(Icons.Default.LinkOff, null, tint = NopalDanger)
                            Text("Desvincular este TUNA-Screen", color = NopalDanger, modifier = Modifier.padding(start = 8.dp))
                        }
                    }

                    SettingsTab.APPEARANCE -> SettingsCard {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(TunaThemeMode.entries) { option ->
                                FilterChip(
                                    selected = themeMode == option,
                                    onClick = { viewModel.setThemeMode(option) },
                                    label = { Text(option.displayName) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = NopalSurface,
                                        labelColor = NopalTextSecondary,
                                        selectedContainerColor = NopalGreen.copy(alpha = 0.18f),
                                        selectedLabelColor = NopalTextPrimary,
                                    ),
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier.size(12.dp)
                                                .clip(CircleShape)
                                                .background(option.previewColor),
                                        )
                                    },
                                )
                            }
                        }
                    }

                    SettingsTab.SCREEN -> SettingsCard {
                        Text(
                            text = "Detectado: ${detectedProfile.profile.name.replace('_', ' ')}" +
                                detectedProfile.estimatedDiagonalInches?.let { " · %.1f″".format(it) }.orEmpty(),
                            color = NopalTextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(DisplayModeOverride.entries) { option ->
                                FilterChip(
                                    selected = displayMode == option,
                                    onClick = { viewModel.setDisplayMode(option) },
                                    label = { Text(option.displayName) },
                                )
                            }
                        }
                    }

                    SettingsTab.NOTIFICATIONS -> NotificationsTab(notificationPrefs, viewModel)
                }
            }
        }
    }

    if (confirmForget) {
        AlertDialog(
            onDismissRequest = { confirmForget = false },
            title = { Text("¿Desvincular TUNA-Screen?") },
            text = { Text("Se eliminarán de este dispositivo el servidor y el token guardados.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmForget = false
                        viewModel.forgetPairing(onPairingRemoved)
                    },
                ) { Text("Desvincular") }
            },
            dismissButton = {
                TextButton(onClick = { confirmForget = false }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = NopalSurface,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

@Composable
private fun NotificationsTab(prefs: NotificationPrefs, viewModel: SettingsViewModel) {
    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(viewModel.hasNotificationPermission()) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionGranted = granted
    }

    if (!permissionGranted) {
        SettingsCard {
            Text(
                "Android necesita tu permiso para mostrar avisos.",
                color = NopalWarning,
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
            ) { Text("PERMITIR NOTIFICACIONES") }
        }
    }

    SettingsCard {
        CheckRow(
            title = "Activar notificaciones",
            detail = "Interruptor general de todos los avisos.",
            checked = prefs.enabled,
            onCheckedChange = viewModel::setNotificationsEnabled,
        )
    }

    Text("CÓMO AVISAR", color = NopalGreen, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    SettingsCard {
        CheckRow("Visuales", "Notificación en pantalla.", prefs.visual, viewModel::setVisual, enabled = prefs.enabled)
        CheckRow("Sonoras", "Sonido de notificación.", prefs.sound, viewModel::setSound, enabled = prefs.enabled)
        CheckRow("Vibrar", "Vibración corta.", prefs.vibrate, viewModel::setVibrate, enabled = prefs.enabled)
    }

    Text("AVISAR CUANDO", color = NopalGreen, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    SettingsCard {
        AlertEvent.entries.forEach { event ->
            CheckRow(
                title = event.title,
                detail = event.detail,
                checked = event in prefs.events,
                onCheckedChange = { viewModel.setEvent(event, it) },
                enabled = prefs.enabled,
            )
        }
    }

    OutlinedButton(onClick = { viewModel.sendTestNotification() }, enabled = prefs.enabled) {
        Text("ENVIAR AVISO DE PRUEBA")
    }
    Text(
        "Los avisos llegan mientras TUNA-Screen está abierta o en segundo plano; con la app cerrada no hay conexión con NOPAL.",
        color = NopalTextSecondary,
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun CheckRow(
    title: String,
    detail: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = CheckboxDefaults.colors(checkedColor = NopalGreen),
        )
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(title, color = if (enabled) NopalTextPrimary else NopalTextSecondary, fontWeight = FontWeight.Medium)
            Text(detail, color = NopalTextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
internal fun DashboardScaffold(
    title: String,
    selectedTab: String,
    connectionStatus: String,
    onNavigate: (String) -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            WorkshopTopBar(
                title = title,
                connectionStatus = connectionStatus,
                onSettingsClick = if (selectedTab == "settings") null else ({ onNavigate("settings") }),
            )
        },
        bottomBar = {
            WorkshopBottomNav(selectedTab = selectedTab, onTabSelected = onNavigate)
        },
        containerColor = NopalBackground,
        content = content,
    )
}

@Composable
private fun MachineFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}

@Composable
internal fun EmptyDashboardState(title: String, detail: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = NopalTextPrimary, style = MaterialTheme.typography.titleMedium)
            Text(detail, color = NopalTextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SettingRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = NopalTextSecondary)
        Text(value, color = NopalTextPrimary, fontWeight = FontWeight.Medium)
    }
}

internal fun MachineType.displayName(): String = when (this) {
    MachineType.PRINTER -> "Impresora 3D"
    MachineType.LASER -> "Láser"
    MachineType.CNC -> "CNC"
}
