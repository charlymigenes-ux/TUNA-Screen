package com.nopal.tunascreen.feature.workshop.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Settings
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
fun WorkshopTopBar(
    title: String = "Taller",
    connectionStatus: String,
    onlineSummary: String? = null,
    onSettingsClick: (() -> Unit)? = null
) {
    val time by produceState(initialValue = workshopCurrentTime()) {
        while (true) {
            value = workshopCurrentTime()
            delay(30_000)
        }
    }
    val statusColor = when (connectionStatus) {
        "Conectado" -> NopalGreen
        "Conectando" -> NopalWarning
        else -> NopalDanger
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NopalChrome)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.nopal_logo),
            contentDescription = "NOPAL",
            modifier = Modifier.size(width = 120.dp, height = 40.dp),
            contentScale = ContentScale.Fit
        )
        
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .width(1.dp)
                .height(24.dp)
                .background(NopalBorder)
        )
        
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = NopalTextPrimary
        )

        if (onlineSummary != null) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .width(1.dp)
                    .height(24.dp)
                    .background(NopalBorder)
            )
            Text(
                text = onlineSummary,
                style = MaterialTheme.typography.labelLarge,
                color = NopalTextPrimary,
                fontWeight = FontWeight.Bold,
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = connectionStatus,
                style = MaterialTheme.typography.bodyMedium,
                color = statusColor
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = time,
            color = NopalTextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.width(12.dp))
        
        if (onSettingsClick != null) {
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Outlined.Settings, "Ajustes", tint = NopalTextPrimary)
            }
        }
    }
}

private val workshopClockFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun workshopCurrentTime(): String = LocalTime.now().format(workshopClockFormatter)

@Composable
fun WorkshopSummaryCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        color = NopalSurface,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.height(64.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = NopalGreen, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = NopalTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = NopalTextSecondary
                )
            }
        }
    }
}

@Composable
fun MachineListCard(
    name: String,
    type: String,
    status: String,
    percent: Int?,
    online: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = NopalSurface,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Machine Thumbnail Placeholder
            Box(
                modifier = Modifier
                    .size(width = 80.dp, height = 50.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(NopalBlack)
            ) {
                Icon(
                    Icons.Filled.PrecisionManufacturing,
                    null,
                    modifier = Modifier.align(Alignment.Center),
                    tint = NopalSurfaceElevated
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (type.contains("Láser", true)) Icons.Filled.LightMode else Icons.Filled.Print,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = NopalGreen
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = NopalTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = type,
                    style = MaterialTheme.typography.labelSmall,
                    color = NopalTextSecondary
                )
                
                if (percent != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = status,
                            style = MaterialTheme.typography.labelSmall,
                            color = NopalGreen
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$percent%",
                            style = MaterialTheme.typography.labelSmall,
                            color = NopalGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    LinearProgressIndicator(
                        progress = { percent / 100f },
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(4.dp)
                            .clip(CircleShape),
                        color = NopalGreen,
                        trackColor = NopalSurfaceElevated
                    )
                } else {
                    Text(
                        text = status,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (online) NopalTextPrimary else NopalTextSecondary
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(NopalSurfaceElevated)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = status.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (percent != null) NopalGreen else NopalTextSecondary,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = NopalTextSecondary
            )
        }
    }
}

@Composable
fun CameraCard(
    machineName: String,
    online: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = NopalSurface,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16 / 9f)
                    .background(NopalBlack)
            ) {
                Surface(
                    color = if (online) NopalGreen.copy(alpha = 0.16f) else NopalSurfaceElevated,
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        text = if (online) "DISPONIBLE" else "SIN CONEXIÓN",
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (online) NopalGreen else NopalTextSecondary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Icon(
                    Icons.Filled.Videocam,
                    null,
                    modifier = Modifier.align(Alignment.Center),
                    tint = NopalSurfaceElevated
                )
            }
            
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = machineName,
                        style = MaterialTheme.typography.bodySmall,
                        color = NopalTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Cámara declarada por NOPAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = NopalTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun QuickAccessButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = NopalSurface,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.aspectRatio(1f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = NopalGreen, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = NopalTextPrimary
            )
        }
    }
}

@Composable
fun WorkshopBottomNav(
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    Surface(
        color = NopalChrome,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            NavItem("Taller", Icons.Filled.Home, selectedTab == "workshop", Modifier.weight(1f)) { onTabSelected("workshop") }
            NavItem("Máquinas", Icons.Filled.PrecisionManufacturing, selectedTab == "machines", Modifier.weight(1f)) { onTabSelected("machines") }
            NavItem("Trabajos", Icons.Filled.Assignment, selectedTab == "jobs", Modifier.weight(1f)) { onTabSelected("jobs") }
            NavItem("Cámaras", Icons.Filled.Videocam, selectedTab == "cameras", Modifier.weight(1f)) { onTabSelected("cameras") }
            NavItem("Materiales", Icons.Filled.Inventory2, selectedTab == "materials", Modifier.weight(1f)) { onTabSelected("materials") }
            NavItem("Accesorios", Icons.Filled.Power, selectedTab == "accessories", Modifier.weight(1f)) { onTabSelected("accessories") }
            NavItem("Ajustes", Icons.Filled.Settings, selectedTab == "settings", Modifier.weight(1f)) { onTabSelected("settings") }
        }
    }
}

@Composable
private fun NavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) NopalSurface else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.fillMaxHeight(),
        border = if (isSelected) BorderStroke(1.dp, NopalGreen) else null
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                null,
                tint = if (isSelected) NopalGreen else NopalTextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = if (isSelected) NopalGreen else NopalTextSecondary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
