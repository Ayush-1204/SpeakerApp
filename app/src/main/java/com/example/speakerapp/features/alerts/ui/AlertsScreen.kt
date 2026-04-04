package com.example.speakerapp.features.alerts.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.speakerapp.features.alerts.data.AlertItem
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private fun formatAlertTimestamp(timestampMs: Long?, rawTimestamp: String?): String {
    val instant = when {
        timestampMs != null -> Instant.ofEpochMilli(timestampMs)
        !rawTimestamp.isNullOrBlank() -> parseAlertInstant(rawTimestamp)
        else -> null
    } ?: return "Time unavailable"

    val diffMs = (System.currentTimeMillis() - instant.toEpochMilli()).coerceAtLeast(0L)
    val diffMinutes = diffMs / 60000L
    if (diffMinutes < 60L) return "${diffMinutes}m ago"

    val diffHours = diffMinutes / 60L
    if (diffHours < 24L) return "${diffHours}h ago"

    return "${diffHours / 24L}d ago"
}

private fun parseAlertInstant(rawTimestamp: String): Instant? {
    return runCatching { Instant.parse(rawTimestamp) }
        .getOrNull()
        ?: runCatching { OffsetDateTime.parse(rawTimestamp).toInstant() }.getOrNull()
    ?: runCatching { LocalDateTime.parse(rawTimestamp).atZone(ZoneOffset.UTC).toInstant() }.getOrNull()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    viewModel: AlertsViewModel,
    onPlayClip: (String) -> Unit,
    onShowMap: (Double, Double) -> Unit,
    onFlagAsFamiliar: (String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptics = LocalHapticFeedback.current
    val showClearConfirm = remember { mutableStateOf(false) }
    val selectedAlertId = remember { mutableStateOf<String?>(null) }
    val familiarName = remember { mutableStateOf("") }

    Scaffold(
        containerColor = Color(0xFFF6FAFA),
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
                    TextButton(onClick = { showClearConfirm.value = true }) {
                        Text(
                            "CLEAR ALL",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF0F766E),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    IconButton(onClick = { viewModel.loadAlerts() }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color(0xFF0F766E))
                    }
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)) {
                    Text(
                        "SECURITY LOGS",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF4A6267),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        "Alerts",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 60.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 1.sp
                        ),
                        color = Color(0xFF004650)
                    )
                }
            }

            items(
                items = uiState.alerts,
                key = { it.id }
            ) { alert ->
                AlertFeedCard(
                    alert = alert,
                    isDeleting = uiState.deletingAlertId == alert.id,
                    onAck = { viewModel.acknowledgeAlert(alert.id) },
                    onPlay = { onPlayClip(alert.id) },
                    onFlagAsFamiliar = {
                        selectedAlertId.value = alert.id
                        familiarName.value = ""
                    },
                    onDelete = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.deleteAlert(alert.id)
                    },
                    onMap = {
                        val lat = alert.mapLat
                        val lng = alert.mapLng
                        if (lat != null && lng != null) {
                            onShowMap(lat, lng)
                        }
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }

        if (selectedAlertId.value != null) {
            AlertDialog(
                onDismissRequest = { selectedAlertId.value = null },
                title = { Text("Flag as Familiar") },
                text = {
                    OutlinedTextField(
                        value = familiarName.value,
                        onValueChange = { familiarName.value = it },
                        label = { Text("Familiar Name") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val alertId = selectedAlertId.value ?: return@TextButton
                        onFlagAsFamiliar(alertId, familiarName.value)
                        selectedAlertId.value = null
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedAlertId.value = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showClearConfirm.value) {
            AlertDialog(
                onDismissRequest = { showClearConfirm.value = false },
                title = { Text("Clear All Alerts?") },
                text = { Text("This will permanently delete all alerts.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteAllAlerts()
                        showClearConfirm.value = false
                    }) {
                        Text("Clear All", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirm.value = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertFeedCard(
    alert: AlertItem,
    isDeleting: Boolean,
    onAck: () -> Unit,
    onPlay: () -> Unit,
    onFlagAsFamiliar: () -> Unit,
    onDelete: () -> Unit,
    onMap: () -> Unit
) {
    val clipDurationMs = 12_000L
    var isPlaying by remember(alert.id) { mutableStateOf(false) }
    var playbackMs by remember(alert.id) { mutableStateOf(0L) }
    var isDragging by remember(alert.id) { mutableStateOf(false) }

    LaunchedEffect(isPlaying, isDragging, alert.id) {
        if (!isPlaying || isDragging) return@LaunchedEffect

        var lastFrameNanos = withFrameNanos { it }

        while (isPlaying && !isDragging && playbackMs < clipDurationMs) {
            val frameNanos = withFrameNanos { it }
            val deltaMs = ((frameNanos - lastFrameNanos) / 1_000_000L).coerceAtLeast(0L)
            lastFrameNanos = frameNanos

            playbackMs = (playbackMs + deltaMs).coerceAtMost(clipDurationMs)
        }

        if (playbackMs >= clipDurationMs) {
            isPlaying = false
        }
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled && !isDeleting) {
                onDelete()
            }
            true
        }
    )

    AnimatedVisibility(
        visible = !isDeleting,
        exit = shrinkVertically() + fadeOut()
    ) {
        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromEndToStart = true,
            enableDismissFromStartToEnd = false,
            backgroundContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFBA1A1A)),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.padding(end = 24.dp)
                    )
                }
            }
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (!alert.isAcknowledged) Color.White else Color(0xFFEBEFEF)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        if (alert.confidenceScore > 0.9) Color(0xFFBA1A1A) else Color(0xFF004650),
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Potential Stranger",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF181C1D)
                                )
                                Text(
                                    formatAlertTimestamp(alert.timestampMs, alert.timestamp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF6F7979),
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                        Surface(
                            shape = CircleShape,
                            color = if (alert.confidenceScore > 0.9) Color(0xFF9DF898) else Color(0xFFDFE3E3)
                        ) {
                            Text(
                                "${formatConfidencePercent(alert.confidenceScore)}% CONFIDENCE",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold,
                                color = if (alert.confidenceScore > 0.9) Color(0xFF002204) else Color(0xFF3F4949)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Audio Player Bar
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF0F4F4)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (isPlaying) {
                                        isPlaying = false
                                    } else {
                                        if (playbackMs >= clipDurationMs) {
                                            playbackMs = 0L
                                        }
                                        onPlay()
                                        isPlaying = true
                                    }
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFF004650), CircleShape)
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.White
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Slider(
                                    value = playbackMs.toFloat(),
                                    onValueChange = { value ->
                                        isDragging = true
                                        playbackMs = value.roundToLong().coerceIn(0L, clipDurationMs)
                                    },
                                    onValueChangeFinished = {
                                        isDragging = false
                                        if (playbackMs >= clipDurationMs) isPlaying = false
                                    },
                                    valueRange = 0f..clipDurationMs.toFloat(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF004650),
                                        activeTrackColor = Color(0xFF004650),
                                        inactiveTrackColor = Color(0xFFE5E9E9)
                                    )
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        formatClipTime((playbackMs / 1000L).toInt()),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF6F7979),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        formatClipTime((clipDurationMs / 1000L).toInt()),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF6F7979),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Actions
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionButton(
                            text = "View Map",
                            icon = Icons.Default.LocationOn,
                            onClick = onMap,
                            containerColor = Color(0xFFCDE7ED),
                            contentColor = Color(0xFF051F23)
                        )
                        ActionButton(
                            text = "Mark as Read",
                            onClick = onAck,
                            border = true
                        )
                        Surface(
                            onClick = onFlagAsFamiliar,
                            shape = CircleShape,
                            color = Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBEC8C9)),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.PersonAdd,
                                    contentDescription = "Add Familiar",
                                    tint = Color(0xFF004650),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionButton(
    text: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    containerColor: Color = Color.Transparent,
    contentColor: Color = Color(0xFF004650),
    border: Boolean = false
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = containerColor,
        border = if (border) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBEC8C9)) else null,
        modifier = Modifier.height(36.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = contentColor)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

private fun formatClipTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(mins, secs)
}

private fun formatConfidencePercent(score: Double): Int {
    return if (score > 1.0) {
        score.roundToInt().coerceIn(0, 100)
    } else {
        (score * 100.0).roundToInt().coerceIn(0, 100)
    }
}
