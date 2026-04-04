package com.example.speakerapp.features.devices.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp

@Composable
fun DeviceRegistrationScreen(
    viewModel: DeviceRegistrationViewModel,
    onRegistrationSuccess: (role: String) -> Unit
) {
    var selectedRole by remember { mutableStateOf<String?>(null) }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.device) {
        if (uiState.device != null) {
            onRegistrationSuccess(uiState.device?.role ?: "child_device")
        }
    }

    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading && uiState.device == null) {
            selectedRole = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFF6FAFA),
                        Color(0xFFEAF1F2)
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 100.dp, y = (-80).dp)
                .size(260.dp)
                .background(Color(0xFFCDE7ED).copy(alpha = 0.35f), CircleShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-90).dp, y = 80.dp)
                .size(220.dp)
                .background(Color(0xFF135F6B).copy(alpha = 0.08f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "SafeEar",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF004650),
                letterSpacing = 3.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Select Mode",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 54.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Choose how this phone should run in SafeEar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(30.dp))

            ModeActionButton(
                title = "Parent Mode",
                icon = Icons.Default.SupervisorAccount,
                loading = uiState.isLoading && selectedRole == "parent_device",
                selected = selectedRole == "parent_device",
                enabled = !uiState.isLoading && selectedRole == null,
                onClick = {
                    if (selectedRole != null) return@ModeActionButton
                    selectedRole = "parent_device"
                    viewModel.registerDevice("parent_device")
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            ModeActionButton(
                title = "Child Mode",
                icon = Icons.Default.ChildCare,
                loading = uiState.isLoading && selectedRole == "child_device",
                selected = selectedRole == "child_device",
                enabled = !uiState.isLoading && selectedRole == null,
                onClick = {
                    if (selectedRole != null) return@ModeActionButton
                    selectedRole = "child_device"
                    viewModel.registerDevice("child_device")
                }
            )

            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = uiState.error ?: "Unknown Error",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeActionButton(
    title: String,
    icon: ImageVector,
    loading: Boolean,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLowest
            },
            contentColor = MaterialTheme.colorScheme.primary
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 10.dp,
            focusedElevation = 4.dp,
            hoveredElevation = 4.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp)
            .graphicsLayer {
                scaleX = if (isPressed) 0.985f else 1f
                scaleY = if (isPressed) 0.985f else 1f
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                modifier = Modifier.size(76.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(38.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.2).sp
                ),
                maxLines = 1
            )
        }
    }
}
