package com.nopal.tunascreen.feature.machine.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nopal.tunascreen.R
import com.nopal.tunascreen.ui.theme.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

@Composable
fun NopalTopBar(
    machineName: String,
    machineSubtitle: String,
    status: String,
    online: Boolean,
    onListClick: () -> Unit = {},
    onAdvancedClick: (() -> Unit)? = null,
) {
    val time by produceState(initialValue = currentTime()) {
        while (true) {
            value = currentTime()
            delay(30_000)
        }
    }
    Surface(
        color = NopalChrome,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.nopal_logo),
                contentDescription = "NOPAL",
                modifier = Modifier.size(width = 120.dp, height = 40.dp),
                contentScale = ContentScale.Fit,
            )
            IconButton(onClick = onListClick) {
                Icon(Icons.Default.ArrowBack, "Volver a máquinas", tint = NopalTextPrimary)
            }

            VerticalDivider()

            // Machine Info
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = machineName,
                    style = MaterialTheme.typography.titleMedium,
                    color = NopalTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = machineSubtitle.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = NopalTextSecondary
                )
            }

            // Status Capsule
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (online) NopalGreen else NopalTextSecondary)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (online) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = NopalBlack
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = status.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = NopalBlack,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            onAdvancedClick?.let { openAdvanced ->
                TextButton(onClick = openAdvanced) {
                    Icon(Icons.Default.Tune, null, tint = NopalGreen, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("AVANZADO", color = NopalGreen, style = MaterialTheme.typography.labelMedium)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Right side info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Wifi,
                    "Estado de red",
                    tint = if (online) NopalGreen else NopalDanger,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, tint = NopalTextSecondary, modifier = Modifier.size(16.dp))
                    Text(text = time, color = NopalTextPrimary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

private val clockFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun currentTime(): String = LocalTime.now().format(clockFormatter)

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .width(1.dp)
            .fillMaxHeight(0.5f)
            .background(NopalBorder)
    )
}

@Composable
fun TemperatureCard(
    label: String,
    current: String,
    target: String,
    icon: ImageVector
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NopalSurface),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = NopalGreen, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(label.uppercase(), color = NopalTextSecondary, style = MaterialTheme.typography.labelMedium)
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "$current°C",
                        color = NopalGreen,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("ACTUAL", color = NopalTextSecondary, style = MaterialTheme.typography.labelSmall)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "/ $target°C",
                        color = NopalTextSecondary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text("OBJETIVO", color = NopalTextSecondary, style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(NopalGreen))
        }
    }
}

@Composable
fun FanCard(
    percent: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NopalSurface),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Air, null, tint = NopalGreen, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("VENTILADOR", color = NopalTextSecondary, style = MaterialTheme.typography.labelMedium)
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "$percent%",
                        color = NopalGreen,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("ACTUAL", color = NopalTextSecondary, style = MaterialTheme.typography.labelSmall)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "/ $percent%",
                        color = NopalTextSecondary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text("OBJETIVO", color = NopalTextSecondary, style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(NopalGreen))
        }
    }
}

@Composable
fun MachineJobPreview(
    modifier: Modifier = Modifier,
    zPos: String = "48.20",
    yPos: String = "120.50",
    xPos: String = "98.40"
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16/9f)
            .clip(RoundedCornerShape(4.dp))
            .background(NopalSurfaceElevated)
            .border(1.dp, NopalBorder, RoundedCornerShape(4.dp))
    ) {
        // Placeholder for 3D view
        Icon(
            Icons.Default.ViewInAr, 
            null, 
            modifier = Modifier.align(Alignment.Center).size(64.dp),
            tint = NopalTextSecondary.copy(alpha = 0.32f)
        )
        
        // Position info
        Column(modifier = Modifier.padding(12.dp).align(Alignment.TopStart)) {
            PositionItem("Z", zPos, NopalTextSecondary)
            PositionItem("Y", yPos, NopalTextSecondary)
            PositionItem("X", xPos, NopalDanger)
        }
    }
}

@Composable
private fun PositionItem(label: String, value: String, labelColor: Color) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, color = labelColor, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text(value, color = NopalTextPrimary, style = MaterialTheme.typography.bodyMedium)
        Text("mm", color = NopalTextSecondary, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun JobProgressCard(
    filename: String,
    percent: Int,
    elapsed: String,
    remaining: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NopalSurface),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("TRABAJO ACTUAL", color = NopalTextSecondary, style = MaterialTheme.typography.labelMedium)
            Text(filename, color = NopalGreen, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { percent / 100f },
                    modifier = Modifier.weight(1f).height(8.dp).clip(CircleShape),
                    color = NopalGreen,
                    trackColor = NopalSurfaceElevated
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("$percent%", color = NopalGreen, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TimeItem("TRANSCURRIDO", elapsed)
                Spacer(modifier = Modifier.width(24.dp))
                TimeItem("RESTANTE", remaining)
            }
        }
    }
}

@Composable
private fun TimeItem(label: String, time: String) {
    Column {
        Text(label, color = NopalTextSecondary, style = MaterialTheme.typography.labelSmall)
        Text(time, color = NopalTextPrimary, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun QuickActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = NopalSurface,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.fillMaxWidth().height(48.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                null,
                tint = if (enabled) NopalGreen else NopalTextSecondary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                label.uppercase(),
                color = if (enabled) NopalTextPrimary else NopalTextSecondary,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
fun MachineControlButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isSelected: Boolean = false,
    isDanger: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val borderColor = when {
        isDanger -> NopalDanger
        isSelected -> NopalGreen
        else -> NopalBorder
    }
    
    val contentColor = when {
        isDanger -> NopalDanger
        isSelected -> NopalGreen
        else -> NopalGreen
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        color = NopalSurface,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .height(80.dp)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                null,
                tint = if (enabled) contentColor else NopalTextSecondary.copy(alpha = 0.45f),
                modifier = Modifier.size(32.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label.uppercase(),
                color = when {
                    !enabled -> NopalTextSecondary.copy(alpha = 0.55f)
                    isDanger -> NopalDanger
                    else -> NopalTextPrimary
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
