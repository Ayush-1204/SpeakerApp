package com.example.speakerapp

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.speakerapp.core.auth.TokenManager
import com.example.speakerapp.core.notifications.SafeEarFirebaseMessagingService
import com.example.speakerapp.features.alerts.ui.AlertsScreen
import com.example.speakerapp.features.alerts.ui.AlertsViewModel
import com.example.speakerapp.features.alerts.ui.DashboardScreen
import com.example.speakerapp.features.auth.ui.AuthViewModel
import com.example.speakerapp.features.auth.ui.LoginScreen
import com.example.speakerapp.features.detection.ui.ChildMonitoringScreen
import com.example.speakerapp.features.devices.ui.DeviceRegistrationScreen
import com.example.speakerapp.features.devices.ui.DeviceRegistrationViewModel
import com.example.speakerapp.features.enrollment.ui.SpeakerEnrollmentScreen
import com.example.speakerapp.features.enrollment.ui.SpeakerEnrollmentViewModel
import com.example.speakerapp.features.enrollment.ui.SpeakerListScreen
import com.example.speakerapp.features.enrollment.ui.SpeakerListViewModel
import com.example.speakerapp.features.settings.ui.SettingsScreen
import com.example.speakerapp.navigation.Screen
import com.example.speakerapp.service.DetectionService
import com.example.speakerapp.ui.theme.AppTheme
import com.example.speakerapp.ui.theme.SpeakerAppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SpeakerAppTheme {
                MainContent(tokenManager)
            }
        }
    }
}

@Composable
fun MainContent(tokenManager: TokenManager) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsState()
    
    var deviceRole by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(authState.isLoggedIn) {
        deviceRole = if (authState.isLoggedIn) {
            tokenManager.getDeviceRole()
        } else {
            null
        }
    }

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }
    val appMaxWidth = 720.dp

    LaunchedEffect(Unit) {
        SafeEarFirebaseMessagingService.ensureAlertChannel(context)

        try {
            Log.d("SafeEar", "Launching FCM diagnostics...")
            com.example.speakerapp.core.fcm.FCMDiagnostics.diagnoseAll(context)
        } catch (e: Exception) {
            Log.e("SafeEar", "FCM diagnostics failed: ${e.message}", e)
        }

        try {
            com.example.speakerapp.features.alerts.workers.AlertWorkerManager.startBackgroundAlertRefresh(context)
        } catch (e: Exception) {
            Log.e("SafeEar", "Failed to schedule alert refresh: ${e.message}", e)
        }

        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    LaunchedEffect(authState.isLoggedIn, deviceRole) {
        val serviceIntent = Intent(context, DetectionService::class.java).apply {
            action = if (authState.isLoggedIn && deviceRole == "child_device") "START" else "STOP"
        }
        runCatching {
            context.startService(serviceIntent)
        }
    }

    if (!authState.isLoggedIn) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = appMaxWidth)
            ) {
                LoginScreen(viewModel = authViewModel) {
                    navController.navigate(Screen.DeviceRegistration.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            }
        }
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = appMaxWidth)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = when (deviceRole) {
                        null -> Screen.DeviceRegistration.route
                        "parent_device" -> Screen.ParentDashboard.route
                        else -> Screen.ChildMonitoring.route
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(Screen.DeviceRegistration.route) {
                        val viewModel: DeviceRegistrationViewModel = hiltViewModel()
                        DeviceRegistrationScreen(viewModel) { role ->
                            deviceRole = role
                            val startRoute = if (role == "parent_device") Screen.ParentDashboard.route else Screen.ChildMonitoring.route
                            navController.navigate(startRoute) {
                                popUpTo(Screen.DeviceRegistration.route) { inclusive = true }
                            }
                        }
                    }

                    composable(Screen.ParentDashboard.route) {
                        val viewModel: AlertsViewModel = hiltViewModel()
                        DashboardScreen(
                            viewModel = viewModel,
                            onViewAllAlerts = { navController.navigate(Screen.AlertsFeed.route) },
                            onNotificationClick = { navController.navigate(Screen.AlertsFeed.route) },
                            onOpenMonitor = { navController.navigate(Screen.ChildMonitoring.route) }
                        )
                    }

                    composable(Screen.AlertsFeed.route) {
                        val viewModel: AlertsViewModel = hiltViewModel()
                        AlertsScreen(
                            viewModel = viewModel,
                            onPlayClip = { alertId -> viewModel.playAlertClip(alertId) },
                            onFlagAsFamiliar = { alertId, displayName ->
                                viewModel.flagAsFamiliar(alertId, displayName)
                            },
                            onShowMap = { lat, lng ->
                                val geoIntent = Intent(Intent.ACTION_VIEW, "geo:$lat,$lng?q=$lat,$lng".toUri())
                                geoIntent.setPackage("com.google.android.apps.maps")
                                val webIntent = Intent(Intent.ACTION_VIEW, "https://www.google.com/maps/search/?api=1&query=$lat,$lng".toUri())
                                try {
                                    context.startActivity(geoIntent)
                                } catch (_: ActivityNotFoundException) {
                                    context.startActivity(webIntent)
                                }
                            }
                        )
                    }

                    composable(Screen.ChildMonitoring.route) {
                        ChildMonitoringScreen(
                            onOpenSettings = {
                                navController.navigate(Screen.Settings.route) {
                                    launchSingleTop = true
                                }
                            },
                            onOpenNotifications = {
                                if (deviceRole == "parent_device") {
                                    navController.navigate(Screen.AlertsFeed.route)
                                }
                            }
                        )
                    }

                    composable(Screen.SpeakerList.route) {
                        val viewModel: SpeakerListViewModel = hiltViewModel()
                        SpeakerListScreen(viewModel) {
                            navController.navigate(Screen.SpeakerEnrollment.route)
                        }
                    }

                    composable(Screen.SpeakerEnrollment.route) {
                        val viewModel: SpeakerEnrollmentViewModel = hiltViewModel()
                        SpeakerEnrollmentScreen(viewModel) {
                            navController.popBackStack()
                        }
                    }

                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            tokenManager = tokenManager,
                            onOpenAlerts = {
                                if (deviceRole == "parent_device") {
                                    navController.navigate(Screen.AlertsFeed.route)
                                }
                            },
                            onOpenMonitor = {
                                navController.navigate(Screen.ChildMonitoring.route) {
                                    launchSingleTop = true
                                }
                            },
                            onLogout = {
                                authViewModel.logout()
                            }
                        )
                    }
                }

                // Truly floating Navigation Bar overlay
                deviceRole?.let { role ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 12.dp)
                    ) {
                        BottomNavigationBar(navController, role)
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: androidx.navigation.NavHostController, role: String) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val adaptive = AppTheme.adaptive
    val isCompact = adaptive.isCompact
    val isParent = role == "parent_device"
    val sidePadding = adaptive.horizontalScreenPadding
    val maxPillWidth = if (isParent) 420.dp else 300.dp
    val pillHeight = adaptive.navPillHeight

    Surface(
        modifier = Modifier
            .padding(horizontal = sidePadding)
            .fillMaxWidth()
            .widthIn(max = maxPillWidth)
            .wrapContentWidth()
            .height(pillHeight),
        shape = RoundedCornerShape(100), // Perfect Pill shape
        color = Color.White, // Solid color, removed transparency
        shadowElevation = 12.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.5f)) // Simple subtle border
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .padding(horizontal = if (isCompact) 10.dp else 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isParent) {
                BottomPillItem(
                    icon = Icons.Default.Dashboard,
                    label = "Dashboard",
                    selected = currentDestination?.hierarchy?.any { it.route == Screen.ParentDashboard.route } == true,
                    onClick = {
                        navController.navigate(Screen.ParentDashboard.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                BottomPillItem(
                    icon = Icons.Default.NotificationsActive,
                    label = "Alerts",
                    selected = currentDestination?.hierarchy?.any { it.route == Screen.AlertsFeed.route } == true,
                    onClick = {
                        navController.navigate(Screen.AlertsFeed.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                BottomPillItem(
                    icon = Icons.Default.Group,
                    label = "Familiar",
                    selected = currentDestination?.hierarchy?.any { it.route == Screen.SpeakerList.route || it.route == Screen.SpeakerEnrollment.route } == true,
                    onClick = {
                        navController.navigate(Screen.SpeakerList.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                BottomPillItem(
                    icon = Icons.Default.GraphicEq,
                    label = "Monitor",
                    selected = currentDestination?.hierarchy?.any { it.route == Screen.ChildMonitoring.route } == true,
                    onClick = {
                        navController.navigate(Screen.ChildMonitoring.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            BottomPillItem(
                icon = Icons.Default.Settings,
                label = "Settings",
                selected = currentDestination?.hierarchy?.any { it.route == Screen.Settings.route } == true,
                onClick = {
                    navController.navigate(Screen.Settings.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BottomPillItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val adaptive = AppTheme.adaptive
    val isCompact = adaptive.isCompact
    val bubbleWidth = adaptive.navIconBubbleWidth
    val bubbleHeight = adaptive.navIconBubbleHeight
    val iconSize = if (isCompact) 22.dp else 24.dp
    val labelSize = if (isCompact) 9.sp else 10.sp
    
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(32.dp))
            .clickable(
                onClick = onClick,
                indication = null, // Removes standard rectangular ripple
                interactionSource = interactionSource
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(width = bubbleWidth, height = bubbleHeight)
                    .background(
                        color = if (selected) Color(0xFFBFE1E8) else Color.Transparent, // Solid background when selected
                        shape = RoundedCornerShape(100)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (selected) Color(0xFF004650) else Color(0xFF4A6267),
                    modifier = Modifier.size(iconSize)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
                    fontSize = labelSize,
                    letterSpacing = 0.05.sp
                ),
                color = if (selected) Color(0xFF181C1D) else Color(0xFF4A6267)
            )
        }
    }
}
