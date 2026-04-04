package com.example.speakerapp.features.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.speakerapp.core.auth.TokenManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    tokenManager: TokenManager,
    onOpenAlerts: () -> Unit,
    onOpenMonitor: () -> Unit,
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var deviceRole by remember { mutableStateOf<String?>(null) }
    var acousticAlertsEnabled by remember { mutableStateOf(true) }
    var dailySummaryEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        scope.launch {
            deviceRole = tokenManager.getDeviceRole()
        }
    }

    Scaffold(
        containerColor = Color(0xFFF6FAFA),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Teal Sentinel",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = Color(0xFF134E4A)
                    )
                },
                actions = {
                    if (deviceRole == "child_device") {
                        IconButton(onClick = onOpenMonitor) {
                            Icon(
                                Icons.Default.GraphicEq,
                                contentDescription = "Open Monitor",
                                tint = Color(0xFF0F766E).copy(alpha = 0.8f)
                            )
                        }
                    }
                    IconButton(onClick = onOpenAlerts) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color(0xFF0F766E).copy(alpha = 0.6f))
                    }
                    AsyncImage(
                        model = "https://lh3.googleusercontent.com/aida-public/AB6AXuCNd_3uCSNN5PmZ_WbPw3m35YFMWge4eC1-B5XoCVDx-qBJtDmHogTlyQ1XTluRgp5yC5oS3N6wGG0gG1zLNNCpQlKffIxd-zs7s182uNT3IP_aJESuAj6hbsLAj0mU3D2nFSuwKTx5M7NrBL_kNbRJmTjyJWy2IggqSTWqgA3hv1I8Q5Kwydtpz4etk_ZVsjW2wQAxsOsVxaytors1dyQiVfSkpeojxi01Ust_nyvncMlS_p49O27DgXgSHRwxM_MWKZflhl_VEvyL",
                        contentDescription = "Profile",
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(32.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF6FAFA).copy(alpha = 0.8f)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Column(modifier = Modifier.padding(top = 24.dp, bottom = 32.dp)) {
                Text(
                    "APP SETTINGS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF004650),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    "Preferences",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 40.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = Color(0xFF181C1D)
                )
            }

            SettingsSectionTitle("Notifications")
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    ToggleRow(
                        icon = Icons.Default.NotificationsActive,
                        title = "Acoustic Alerts",
                        subtitle = "Instant push for high-decibel events",
                        checked = acousticAlertsEnabled,
                        onCheckedChange = { acousticAlertsEnabled = it }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF0F4F4))
                    ToggleRow(
                        icon = Icons.Default.Schedule,
                        title = "Daily Summary",
                        subtitle = "Morning report of nocturnal activity",
                        checked = dailySummaryEnabled,
                        onCheckedChange = { dailySummaryEnabled = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            SettingsSectionTitle("Permissions")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PermissionBadge(Icons.Default.Mic, "MICROPHONE", "ACTIVE", Modifier.weight(1f))
                PermissionBadge(Icons.Default.LocationOn, "LOCATION", "ACTIVE", Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))
            if (deviceRole == "child_device") {
                SettingsSectionTitle("Monitoring")
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Child Monitoring",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF181C1D)
                            )
                            Text(
                                "Jump back to live monitor controls",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF3F4949)
                            )
                        }
                        Button(
                            onClick = onOpenMonitor,
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF004650))
                        ) {
                            Icon(Icons.Default.GraphicEq, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Monitor", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            SettingsSectionTitle("Privacy & Data")
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF135F6B)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Retention Summary", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Acoustic data is encrypted locally. High-decibel triggers are stored for 30 days before automatic deletion. No raw audio is uploaded to the cloud without explicit emergency bypass.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f),
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { /* TODO */ },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF95D6E4)),
                        shape = RoundedCornerShape(999.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text("Request Data Export", color = Color(0xFF004650), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF004650))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            SettingsSectionTitle("About SafeEar")
            Surface(
                color = Color(0xFFF0F4F4),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    InfoRow("Version", "v2.4.12-pro")
                    HorizontalDivider(color = Color(0xFFDFE3E3))
                    InfoRow("Build Identity", "SENTINEL-AX-992", isMono = true)
                    HorizontalDivider(color = Color(0xFFDFE3E3))
                    InfoRow("Device Role", deviceRole ?: "Unknown")
                    HorizontalDivider(color = Color(0xFFDFE3E3))
                    InfoRow("Legal", "", hasChevron = true)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFBA1A1A)),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFBA1A1A).copy(alpha = 0.1f))
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Logout", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text(
                "© 2024 TEAL SENTINEL DOMESTIC MONITORING",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF3F4949).copy(alpha = 0.4f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleLarge,
        color = Color(0xFF181C1D),
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp)
    )
}

@Composable
fun ToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFF135F6B).copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF004650), modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color(0xFF181C1D))
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Color(0xFF3F4949))
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF004650),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFDFE3E3),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
fun PermissionBadge(icon: ImageVector, label: String, status: String, modifier: Modifier = Modifier) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = Color(0xFF006518))
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF3F4949), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Surface(
                color = Color(0xFF9DF898),
                shape = CircleShape
            ) {
                Text(
                    status,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF005312),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, isMono: Boolean = false, hasChevron: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF3F4949), fontWeight = FontWeight.Medium)
        if (hasChevron) {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF3F4949))
        } else {
            Surface(
                color = if (isMono) Color(0xFFEBEFEF) else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    value,
                    style = if (isMono) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF181C1D),
                    fontWeight = FontWeight.Bold,
                    modifier = if (isMono) Modifier.padding(horizontal = 4.dp, vertical = 2.dp) else Modifier
                )
            }
        }
    }
}
