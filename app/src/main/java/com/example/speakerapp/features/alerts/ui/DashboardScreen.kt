package com.example.speakerapp.features.alerts.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.example.speakerapp.features.alerts.data.AlertItem
import com.example.speakerapp.features.devices.data.MonitoredDevice
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

private const val DASHBOARD_REFRESH_INTERVAL_MS = 30_000L
private const val DEVICE_ACTIVITY_ONLINE_WINDOW_MS = 10 * 60 * 1000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: AlertsViewModel,
    onViewAllAlerts: () -> Unit,
    onNotificationClick: () -> Unit,
    onOpenMonitor: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val dismissedDashboardAlertIds = remember { mutableStateListOf<String>() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        while (true) {
            delay(DASHBOARD_REFRESH_INTERVAL_MS)
            viewModel.loadAlerts(limit = 50, offset = 0)
            viewModel.loadDevices()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "SafeEar",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = Color(0xFF004650)
                    )
                },
                actions = {
                    IconButton(onClick = onNotificationClick) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = Color(0xFF004650)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF6FAFA).copy(alpha = 0.8f)
                )
            )
        },
        containerColor = Color(0xFFF6FAFA)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                Column(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)) {
                    Text(
                        "CONTROL CENTER",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF4A6267),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        "Dashboard",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 45.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 1.sp
                        ),
                        color = Color(0xFF004650)
                    )
                }
            }

            // Monitored Devices
            item {
                SectionHeader(title = "Monitored Devices")
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (uiState.isLoadingDevices) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (uiState.devices.isEmpty()) {
                        Text(
                            "No monitored devices found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        uiState.devices.forEach { device ->
                            val derivedOnline = resolveDeviceOnlineState(device, uiState.alerts)
                            val latestLocation = latestAlertWithLocation(device.id, uiState.alerts)
                            DeviceItem(
                                device = device,
                                isOnline = derivedOnline,
                                isToggling = uiState.togglingDeviceId == device.id,
                                onMonitoringToggle = { enabled ->
                                    viewModel.setMonitoringEnabled(device.id, enabled)
                                },
                                canOpenLiveLocation = latestLocation != null,
                                onLiveLocationClick = {
                                    val lat = latestLocation?.mapLat
                                    val lng = latestLocation?.mapLng
                                    if (lat != null && lng != null) {
                                        val geoIntent = Intent(Intent.ACTION_VIEW, "geo:$lat,$lng?q=$lat,$lng".toUri())
                                        geoIntent.setPackage("com.google.android.apps.maps")
                                        val webIntent = Intent(
                                            Intent.ACTION_VIEW,
                                            "https://www.google.com/maps/search/?api=1&query=$lat,$lng".toUri()
                                        )
                                        try {
                                            context.startActivity(geoIntent)
                                        } catch (_: ActivityNotFoundException) {
                                            context.startActivity(webIntent)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Recent Detections
            item {
                SectionHeader(
                    title = "Recent Detections",
                    actionText = "View All",
                    onActionClick = onViewAllAlerts
                )
            }

            val dashboardAlerts = uiState.alerts.take(3)

            if (dashboardAlerts.isEmpty()) {
                item {
                    Text(
                        "No recent detections",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(dashboardAlerts, key = { it.id }) { alert ->
                    AnimatedVisibility(
                        visible = !dismissedDashboardAlertIds.contains(alert.id),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically() + slideOutVertically(targetOffsetY = { it / 2 })
                    ) {
                        DetectionCard(
                            alert = alert,
                            onDismiss = { dismissedDashboardAlertIds.add(alert.id) }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF004650),
            fontWeight = FontWeight.Bold
        )
        if (actionText != null && onActionClick != null) {
            TextButton(
                onClick = onActionClick,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    actionText.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF4A6267),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun DeviceItem(
    device: MonitoredDevice,
    isOnline: Boolean,
    isToggling: Boolean,
    onMonitoringToggle: (Boolean) -> Unit,
    canOpenLiveLocation: Boolean,
    onLiveLocationClick: () -> Unit
) {
    val battery = device.batteryPercent?.coerceIn(0, 100)
    val batteryLabel = battery?.let { "$it%" } ?: "--"
    val isBatteryHealthy = battery != null && battery > 20
    val icon = if (device.deviceName.contains("ipad", ignoreCase = true) || device.deviceName.contains("tablet", ignoreCase = true)) {
        Icons.Default.TabletMac
    } else {
        Icons.Default.Smartphone
    }
    val opacity = if (isOnline) 1f else 0.6f
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FBFB)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0xFFD8EEF2).copy(alpha = opacity), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = Color(0xFF004650).copy(alpha = opacity),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            device.deviceName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF181C1D).copy(alpha = opacity)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (isOnline) Color(0xFF006518) else Color(0xFF6F7979),
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (isOnline) "ONLINE" else "OFFLINE",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4A6267),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp
                            )
                        }
                    }
                }

                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(
                            modifier = Modifier.padding(start = 10.dp, top = 8.dp, bottom = 8.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                batteryLabel,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF181C1D).copy(alpha = opacity)
                            )
                            Text(
                                "BATTERY",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4A6267),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.4.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            if (isBatteryHealthy) Icons.Default.Battery5Bar else Icons.Default.BatteryAlert,
                            contentDescription = null,
                            tint = when {
                                battery == null -> Color(0xFF6F7979).copy(alpha = opacity)
                                isBatteryHealthy -> Color(0xFF006518).copy(alpha = opacity)
                                else -> Color(0xFFBA1A1A).copy(alpha = opacity)
                            },
                            modifier = Modifier.padding(end = 10.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Monitoring",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF4A6267),
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = device.monitoringEnabled,
                            onCheckedChange = onMonitoringToggle,
                            enabled = !isToggling
                        )
                    }
                }

                TextButton(
                    onClick = onLiveLocationClick,
                    enabled = canOpenLiveLocation,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = if (canOpenLiveLocation) Color(0xFF004650) else Color(0xFF6F7979),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "LIVE LOCATION",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (canOpenLiveLocation) Color(0xFF004650) else Color(0xFF6F7979),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DetectionCard(
    alert: AlertItem,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEBEFEF))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFFFFDAD6), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFF93000A))
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Stranger Detected",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF181C1D)
                    )
                    Text(
                        toRelativeTime(alert),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF4A6267),
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (alert.confidenceScore > 0.9) {
                        "High-confidence anomaly detected. Clip archived for immediate review."
                    } else {
                        "Potential anomaly detected. Clip archived for review."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF3F4949),
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            "DISMISS",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF004650),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun toRelativeTime(alert: AlertItem): String {
    val instant = when {
        alert.timestampMs != null -> Instant.ofEpochMilli(alert.timestampMs)
        else -> runCatching { Instant.parse(alert.timestamp) }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(alert.timestamp).toInstant() }.getOrNull()
            ?: runCatching {
                LocalDateTime.parse(alert.timestamp).atZone(ZoneOffset.UTC).toInstant()
            }.getOrNull()
    } ?: return "Time unavailable"

    val diffMinutes =
        ((System.currentTimeMillis() - instant.toEpochMilli()).coerceAtLeast(0L) / 60000L)
    if (diffMinutes < 60) {
        return "${diffMinutes}m ago"
    }

    val diffHours = diffMinutes / 60L
    if (diffHours < 24L) {
        return "${diffHours}h ago"
    }

    return "${diffHours / 24L}d ago"
}

private fun resolveDeviceOnlineState(device: MonitoredDevice, alerts: List<AlertItem>): Boolean {
    if (device.isOnline == true) return true
    if (device.monitoringEnabled) return true

    val latestAlert = alerts.firstOrNull { it.deviceId == device.id } ?: return false
    val alertMs = latestAlert.timestampMs ?: parseTimestampMillis(latestAlert.timestamp) ?: return false
    return System.currentTimeMillis() - alertMs <= DEVICE_ACTIVITY_ONLINE_WINDOW_MS
}

private fun latestAlertWithLocation(deviceId: String, alerts: List<AlertItem>): AlertItem? {
    return alerts
        .asSequence()
        .filter { it.deviceId == deviceId && it.mapLat != null && it.mapLng != null }
        .maxByOrNull { it.timestampMs ?: parseTimestampMillis(it.timestamp) ?: 0L }
}

private fun parseTimestampMillis(timestamp: String): Long? {
    return runCatching { Instant.parse(timestamp).toEpochMilli() }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(timestamp).toInstant().toEpochMilli() }.getOrNull()
        ?: runCatching { LocalDateTime.parse(timestamp).atZone(ZoneOffset.UTC).toInstant().toEpochMilli() }.getOrNull()
}