package com.example.speakerapp.features.detection.ui

import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildMonitoringScreen(
    viewModel: ChildMonitoringViewModel = hiltViewModel(),
    onOpenSettings: () -> Unit = {},
    onOpenNotifications: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        containerColor = Color(0xFFF6FAFA),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFF004650), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Text(
                            "SafeEar",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp
                            ),
                            color = Color(0xFF004650)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenNotifications) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color(0xFF0F766E))
                    }
                    AsyncImage(
                        model = "https://lh3.googleusercontent.com/aida-public/AB6AXuBj_ryYmB2wGh1Y2N3axBWiqxNiUsSovI3PJaMIrzYDFPR0vXb8EpWFsEBDy0QjLb_AJ1dbKbf9mQYkivgPRSeiUQhWLLK6QSSaP7xEHNE492DZRbLuoSmIqznnuvo_wuBRdG8qpNfAlbKnbs3vbbkTrb0TjTNe33rPGo3s3wQz3bTHRd60FB-r_PNIoStTpNqZA0DSWXAMzcnJEb-3f3xK6ue6Dn6UMuzZgJYt_RZ0cVbB4Nvz8bWVa5B9oGohHasVRYun5xpg-3OS",
                        contentDescription = "Profile",
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color(0xFFDFE3E3), CircleShape),
                        contentScale = ContentScale.Crop
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF6FAFA).copy(alpha = 0.8f)
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                MonitoringHeader(
                    isRecording = uiState.isRecording,
                    batteryPercent = uiState.batteryPercent,
                    uploadLatencyMs = uiState.uploadLatencyMs
                )
            }

            item {
                BentoGrid(
                    isRecording = uiState.isRecording,
                    confidenceScore = uiState.confidenceScore,
                    onMicClick = { viewModel.toggleMonitoring() },
                    onOpenSettings = onOpenSettings
                )
            }

            item {
                ActivityLogsSection(
                    logs = uiState.activityLogs,
                    onExportCsv = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_SUBJECT, "safeear_activity_logs.csv")
                            putExtra(Intent.EXTRA_TEXT, buildLogsCsv(uiState.activityLogs))
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Export Activity Logs"))
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
fun MonitoringHeader(
    isRecording: Boolean,
    batteryPercent: Int?,
    uploadLatencyMs: Long?
) {
    val batteryLabel = batteryPercent?.let { "$it%" } ?: "--"
    val latencyLabel = uploadLatencyMs?.let { "${it}ms" } ?: "--"

    Column(modifier = Modifier.padding(top = 24.dp)) {
        Text(
            "MONITORING: LEO'S ROOM",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF4A6267),
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                if (isRecording) "SafeEar Active" else "SafeEar Idle",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1).sp
                ),
                color = Color(0xFF004650)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricBadge(
                icon = if ((batteryPercent ?: 0) > 20) Icons.Default.Battery5Bar else Icons.Default.BatteryAlert,
                label = "BATTERY",
                value = batteryLabel,
                iconColor = Color(0xFF004650)
            )
            MetricBadge(
                icon = Icons.Default.Speed,
                label = "LATENCY",
                value = latencyLabel,
                iconColor = Color(0xFF004A0F)
            )
        }
    }
}

@Composable
fun MetricBadge(icon: ImageVector, label: String, value: String, iconColor: Color) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.height(60.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = iconColor)
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF6F7979), fontWeight = FontWeight.Bold)
                Text(value, style = MaterialTheme.typography.bodySmall, color = Color(0xFF181C1D), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun BentoGrid(
    isRecording: Boolean,
    confidenceScore: Double?,
    onMicClick: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        // Hero Mic Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Box(modifier = Modifier.padding(32.dp)) {
                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    Surface(
                        color = if (isRecording) Color(0xFF9DF898) else Color(0xFFDFE3E3),
                        shape = CircleShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.size(8.dp).background(if (isRecording) Color(0xFF004A0F) else Color(0xFF6F7979), CircleShape))
                            Text(
                                if (isRecording) "LIVE STREAM" else "OFFLINE",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isRecording) Color(0xFF005312) else Color(0xFF3F4949),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Mic Animation
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = if (isRecording) 1.15f else 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )

                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(192.dp)
                                .graphicsLayer {
                                    scaleX = pulseScale
                                    scaleY = pulseScale
                                }
                                .background(Color(0xFF004650).copy(alpha = 0.05f), CircleShape)
                        )
                        Surface(
                            onClick = onMicClick,
                            modifier = Modifier.size(144.dp),
                            shape = CircleShape,
                            color = if (isRecording) Color(0xFF004650) else Color(0xFF6F7979)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    if (isRecording) Icons.Default.Mic else Icons.Default.MicOff,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Text(
                        if (confidenceScore != null) "${(confidenceScore * 100).toInt()}%" else "--%",
                        style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color(0xFF004650)
                    )
                    Text("Acoustic Consistency Score", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4A6267), fontWeight = FontWeight.Medium)
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Waveform
                    Row(
                        modifier = Modifier.height(48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        WaveBar(4, active = isRecording)
                        WaveBar(8, active = isRecording)
                        WaveBar(12, true, active = isRecording)
                        WaveBar(6, active = isRecording)
                        WaveBar(10, true, active = isRecording)
                        WaveBar(4, active = isRecording)
                        WaveBar(8, true, active = isRecording)
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            // Child Profile Card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF135F6B))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp).background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ChildCare, contentDescription = null, tint = Color.White)
                        }
                        IconButton(onClick = onOpenSettings, modifier = Modifier.background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Child Profile", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Update SafeEar listening sensitivity for Leo.", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onOpenSettings,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Edit Settings", color = Color(0xFF004650), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Neural Net Confidence
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F4))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("ACOUSTIC ANALYSIS", style = MaterialTheme.typography.labelSmall, color = Color(0xFF6F7979), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            LinearProgressIndicator(
                                progress = { confidenceScore?.toFloat() ?: 0f },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                                color = Color(0xFF004650),
                                trackColor = Color(0xFFE5E9E9)
                            )
                            Text("Neural Net Confidence", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4A6267), fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            if (confidenceScore != null) "${(confidenceScore * 100).toInt()}%" else "--%",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color(0xFF004650),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            if (isRecording) Icons.Default.CheckCircle else Icons.Default.PauseCircle,
                            contentDescription = null,
                            tint = if (isRecording) Color(0xFF006518) else Color(0xFF6F7979),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            if (isRecording) "No anomalous patterns" else "Monitoring paused",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF181C1D),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WaveBar(height: Int, primary: Boolean = false, active: Boolean) {
    val barHeight by animateDpAsState(
        targetValue = if (active) (height.dp * 4) else 4.dp,
        animationSpec = tween(300),
        label = "height"
    )
    Box(
        modifier = Modifier
            .width(6.dp)
            .height(barHeight)
            .background(if (primary) Color(0xFF004650) else Color(0xFF135F6B), CircleShape)
    )
}

@Composable
fun ActivityLogsSection(logs: List<DetectionLogEntry>, onExportCsv: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Recent Activity Logs", style = MaterialTheme.typography.titleLarge, color = Color(0xFF004650), fontWeight = FontWeight.Bold)
            TextButton(onClick = onExportCsv) {
                Text("EXPORT CSV", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4A6267), fontWeight = FontWeight.Bold)
            }
        }
        
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                LogHeaderRow()
                HorizontalDivider(color = Color(0xFFF0F4F4))
                if (logs.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No logs yet", style = MaterialTheme.typography.bodySmall, color = Color(0xFF6F7979))
                    }
                } else {
                    logs.forEachIndexed { index, log ->
                        LogRow(log.confidence, log.streak, log.decision, log.alertId, log.isSuccess)
                        if (index < logs.size - 1) {
                            HorizontalDivider(color = Color(0xFFF0F4F4))
                        }
                    }
                }
            }
        }
    }
}

private fun buildLogsCsv(logs: List<DetectionLogEntry>): String {
    val header = "confidence,streak,decision,alert_id,status"
    val rows = logs.map { log ->
        val status = if (log.isSuccess) "success" else "flagged"
        "${log.confidence},${log.streak},${log.decision},${log.alertId},${status}"
    }
    return (listOf(header) + rows).joinToString("\n")
}

@Composable
fun LogHeaderRow() {
    Row(
        modifier = Modifier.background(Color(0xFFF0F4F4)).padding(16.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        LogHeaderText("CONFIDENCE", Modifier.weight(1f))
        LogHeaderText("STREAK", Modifier.weight(1.2f))
        LogHeaderText("DECISION", Modifier.weight(1.5f))
        LogHeaderText("ALERT ID", Modifier.weight(1f))
    }
}

@Composable
fun LogHeaderText(text: String, modifier: Modifier) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = Color(0xFF6F7979), fontWeight = FontWeight.Bold, modifier = modifier)
}

@Composable
fun LogRow(confidence: String, streak: String, decision: String, id: String, success: Boolean) {
    Row(
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(confidence, style = MaterialTheme.typography.bodySmall, color = if (success) Color(0xFF181C1D) else Color(0xFFD97706), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(streak, style = MaterialTheme.typography.bodySmall, color = Color(0xFF4A6267), modifier = Modifier.weight(1.2f))
        Box(modifier = Modifier.weight(1.5f)) {
            Surface(
                color = if (success) Color(0xFFDCF2E1) else Color(0xFFFEF3C7),
                shape = CircleShape
            ) {
                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(4.dp).background(if (success) Color(0xFF16A34A) else Color(0xFFD97706), CircleShape))
                    Text(decision, style = MaterialTheme.typography.labelSmall, color = if (success) Color(0xFF166534) else Color(0xFF92400E), fontWeight = FontWeight.Bold)
                }
            }
        }
        Surface(
            color = if (success) Color(0xFFEBEFEF) else Color(0xFFFFDAD6),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(id, style = MaterialTheme.typography.labelSmall, color = if (success) Color(0xFF3F4949) else Color(0xFF93000A), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
        }
    }
}
