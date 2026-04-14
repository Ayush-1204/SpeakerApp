package com.example.speakerapp.features.settings.ui.setup

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private const val PREFS_NAME = "safeear_background_setup"
private const val KEY_XIAOMI_AUTOSTART_SHOWN = "xiaomi_autostart_shown"
private const val KEY_SAMSUNG_BATTERY_SHOWN = "samsung_battery_shown"
private const val KEY_SETUP_ACKNOWLEDGED = "background_setup_acknowledged"

private fun Context.setupPrefs(): SharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

fun hasAcknowledgedBackgroundSetup(context: Context): Boolean {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_SETUP_ACKNOWLEDGED, false)
}

fun markBackgroundSetupAcknowledged(context: Context) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_SETUP_ACKNOWLEDGED, true)
        .apply()
}

private fun Context.isIgnoringBatteryOptimizations(): Boolean {
    val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        powerManager.isIgnoringBatteryOptimizations(packageName)
    } else {
        true
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun BatterySetupGuideScreen(
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.setupPrefs() }

    var batteryExempt by remember { mutableStateOf(context.isIgnoringBatteryOptimizations()) }
    var showBatteryDialog by remember { mutableStateOf(false) }
    var showStrongBatteryWarning by remember { mutableStateOf(false) }
    var showXiaomiDialog by remember { mutableStateOf(false) }
    var showSamsungDialog by remember { mutableStateOf(false) }
    var launchedBatterySettings by remember { mutableStateOf(false) }
    val manufacturer = Build.MANUFACTURER.lowercase()

    val batteryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        batteryExempt = context.isIgnoringBatteryOptimizations()
        showBatteryDialog = !batteryExempt
        showStrongBatteryWarning = !batteryExempt
        launchedBatterySettings = false
        if (batteryExempt) {
            maybeShowOemDialogs(context, prefs) { xiaomi, samsung ->
                showXiaomiDialog = xiaomi
                showSamsungDialog = samsung
            }
        }
    }

    val oemLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        showXiaomiDialog = false
        showSamsungDialog = false
    }

    LaunchedEffect(Unit) {
        maybeShowOemDialogs(context, prefs) { xiaomi, samsung ->
            showXiaomiDialog = xiaomi
            showSamsungDialog = samsung
        }
    }

    Scaffold(
        containerColor = Color(0xFFF6FAFA),
        topBar = {
            TopAppBar(
                title = { Text("Background Monitoring", color = Color(0xFF004650)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF004650))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF6FAFA))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Keep SafeEar active in the background",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF181C1D)
                    )
                    Text(
                        "Some phones kill background monitoring aggressively. Finish these steps once so child mode keeps working when the screen is off.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF3F4949)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.BatterySaver, contentDescription = null, tint = Color(0xFF004650))
                        Text(
                            if (batteryExempt) "Battery optimization is already disabled" else "Battery optimization still needs attention",
                            color = if (batteryExempt) Color(0xFF006518) else Color(0xFFBA1A1A),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    launchedBatterySettings = true
                    launchBatteryExemptionIntent(context, batteryLauncher)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Request battery exemption")
            }

            OutlinedButton(
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Open app settings")
            }

            OutlinedButton(
                onClick = {
                    batteryExempt = context.isIgnoringBatteryOptimizations()
                    showBatteryDialog = !batteryExempt
                    showStrongBatteryWarning = !batteryExempt
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Re-check status")
            }

            if (manufacturer.contains("xiaomi") || manufacturer.contains("samsung")) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "OEM background settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF181C1D)
                        )
                        Text(
                            text = "Some manufacturers require extra setup in addition to battery exemption.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF3F4949)
                        )
                        if (manufacturer.contains("xiaomi")) {
                            OutlinedButton(onClick = { openXiaomiAutostart(context, oemLauncher) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Open Xiaomi Autostart")
                            }
                        }
                        if (manufacturer.contains("samsung")) {
                            OutlinedButton(onClick = { openSamsungBatterySettings(context) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Open Samsung Battery Settings")
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    markBackgroundSetupAcknowledged(context)
                    onDone()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.height(0.dp))
                Text(if (batteryExempt) "Continue to monitoring" else "Continue anyway")
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                if (batteryExempt) {
                    "You can continue now."
                } else {
                    if (showStrongBatteryWarning || launchedBatterySettings) {
                        "Monitoring will stop when your screen turns off. Your child may not be protected."
                    } else {
                        "You can continue now and finish setup later from Settings."
                    }
                },
                color = if (batteryExempt) Color(0xFF006518) else Color(0xFFBA1A1A),
                fontWeight = FontWeight.Medium
            )
        }
    }

    if (showBatteryDialog) {
        AlertDialog(
            onDismissRequest = { showBatteryDialog = false },
            title = { Text("One important step") },
            text = {
                Text(
                    if (showStrongBatteryWarning) {
                        "Monitoring will stop when your screen turns off. Your child may not be protected."
                    } else {
                        "To keep monitoring active when the screen is off, please tap Allow on the next screen."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showBatteryDialog = false
                    launchedBatterySettings = true
                    launchBatteryExemptionIntent(context, batteryLauncher)
                }) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatteryDialog = false }) {
                    Text("Not now")
                }
            }
        )
    }

    if (showXiaomiDialog) {
        AlertDialog(
            onDismissRequest = { showXiaomiDialog = false },
            title = { Text("Xiaomi extra step needed") },
            text = {
                Text("Open Security app -> Permissions -> Autostart -> enable SafeEar. Without this, monitoring stops after a few minutes.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showXiaomiDialog = false
                    openXiaomiAutostart(context, oemLauncher)
                }) {
                    Text("Open Security App")
                }
            }
        )
    }

    if (showSamsungDialog) {
        AlertDialog(
            onDismissRequest = { showSamsungDialog = false },
            title = { Text("Samsung extra step") },
            text = { Text("Go to Settings -> Apps -> SafeEar -> Battery -> set to Unrestricted.") },
            confirmButton = {
                TextButton(onClick = {
                    showSamsungDialog = false
                    openSamsungBatterySettings(context)
                }) {
                    Text("Open App Settings")
                }
            }
        )
    }
}

private fun launchBatteryExemptionIntent(
    context: Context,
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>
) {
    val requestIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

    try {
        launcher.launch(requestIntent)
    } catch (_: Exception) {
        try {
            launcher.launch(fallbackIntent)
        } catch (_: Exception) {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            )
        }
    }
}

private fun openXiaomiAutostart(
    context: Context,
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>
) {
    val intent = Intent().apply {
        component = ComponentName(
            "com.miui.securitycenter",
            "com.miui.permcenter.autostart.AutoStartManagementActivity"
        )
    }
    try {
        launcher.launch(intent)
    } catch (_: ActivityNotFoundException) {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        )
    }
}

private fun openSamsungBatterySettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    )
}

private fun maybeShowOemDialogs(
    context: Context,
    prefs: SharedPreferences,
    onState: (showXiaomi: Boolean, showSamsung: Boolean) -> Unit
) {
    val xiaomiShown = prefs.getBoolean(KEY_XIAOMI_AUTOSTART_SHOWN, false)
    val samsungShown = prefs.getBoolean(KEY_SAMSUNG_BATTERY_SHOWN, false)

    val showXiaomi = Build.MANUFACTURER.equals("xiaomi", ignoreCase = true) && !xiaomiShown
    val showSamsung = Build.MANUFACTURER.equals("samsung", ignoreCase = true) && !samsungShown

    if (showXiaomi) prefs.edit().putBoolean(KEY_XIAOMI_AUTOSTART_SHOWN, true).apply()
    if (showSamsung) prefs.edit().putBoolean(KEY_SAMSUNG_BATTERY_SHOWN, true).apply()

    onState(showXiaomi, showSamsung)
}
