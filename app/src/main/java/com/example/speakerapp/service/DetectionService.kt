package com.example.speakerapp.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.BatteryManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.speakerapp.MainActivity
import com.example.speakerapp.R
import com.example.speakerapp.core.audio.AudioRecorder
import com.example.speakerapp.core.auth.TokenManager
import com.example.speakerapp.features.detection.data.DetectionRepository
import com.example.speakerapp.features.detection.data.DetectionResult
import com.example.speakerapp.features.devices.data.DeviceRepository
import com.example.speakerapp.features.devices.data.DeviceResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

/**
 * Background Detection Service for child monitoring
 * Runs as a foreground service to continue recording/streaming even when app is in background
 * 
 * Usage:
 * START: context.startService(Intent(context, DetectionService::class.java).apply { action = "START" })
 * STOP: context.startService(Intent(context, DetectionService::class.java).apply { action = "STOP" })
 */
@AndroidEntryPoint
class DetectionService : Service() {

    @Inject lateinit var detectionRepository: DetectionRepository
    @Inject lateinit var deviceRepository: DeviceRepository
    @Inject lateinit var tokenManager: TokenManager
    @Inject lateinit var audioRecorder: AudioRecorder

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isServiceActive = false
    private var monitoringSyncJob: Job? = null
    private var recordingJob: Job? = null
    private var chunkIndex = 0
    private val isRecording = AtomicBoolean(false)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "DetectionService created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: "START"
        
        when (action) {
            "START" -> {
                Log.d(TAG, "START command received")
                startMonitoringService()
            }
            "STOP" -> {
                Log.d(TAG, "STOP command received")
                stopAllAndSelf()
            }
        }
        
        return START_STICKY
    }

    private fun startMonitoringService() {
        if (isServiceActive) return
        isServiceActive = true

        updateNotification("Monitoring sync active")
        startRemoteMonitoringSync()
    }

    private fun stopAllAndSelf() {
        isServiceActive = false
        monitoringSyncJob?.cancel()
        monitoringSyncJob = null
        stopRecordingLoop()

        stopForeground(true)
        stopSelf()
    }

    private fun startRemoteMonitoringSync() {
        monitoringSyncJob?.cancel()
        monitoringSyncJob = serviceScope.launch {
            while (isActive) {
                try {
                    syncRemoteMonitoringState()
                } catch (e: Exception) {
                    Log.e(TAG, "Remote sync error: ${e.message}")
                }
                delay(8_000L)
            }
        }
    }

    private suspend fun syncRemoteMonitoringState() {
        val role = tokenManager.getDeviceRole()
        if (role != "child_device") {
            stopRecordingLoop()
            return
        }

        val deviceId = tokenManager.getDeviceId() ?: return

        deviceRepository.listDevices().collect { result ->
            if (result is DeviceResult.Success) {
                val selfDevice = result.data.firstOrNull { it.id == deviceId }
                    ?: result.data.firstOrNull { it.role == "child_device" }
                    ?: return@collect

                val foregroundOwnerActive = isForegroundOwnerActive()
                if (selfDevice.monitoringEnabled) {
                    if (!foregroundOwnerActive) {
                        startRecordingLoop(deviceId)
                    } else {
                        stopRecordingLoop()
                        updateNotification("Monitoring active in app")
                    }
                } else {
                    stopRecordingLoop()
                    updateNotification("Monitoring idle")
                }
            }
        }
    }

    private fun startRecordingLoop(deviceId: String) {
        if (recordingJob?.isActive == true) return
        if (!hasRecordPermission()) {
            updateNotification("Mic permission required")
            return
        }
        if (!ensureRecorderReady()) {
            updateNotification("Recorder unavailable")
            return
        }

        isRecording.set(true)
        updateNotification("Active - listening for voices")
        recordingJob = serviceScope.launch {
            while (isActive && isRecording.get()) {
                try {
                    val chunkFile = File.createTempFile("bg_chunk_$chunkIndex", ".wav")
                    chunkIndex++

                    val recorded = audioRecorder.recordAudio(1500, chunkFile)
                    val bytes = chunkFile.length()
                    if (recorded == null || bytes <= MIN_VALID_WAV_BYTES) {
                        chunkFile.delete()
                        ensureRecorderReady()
                        delay(350)
                        continue
                    }

                    detectionRepository.uploadDetectionChunk(
                        deviceId = deviceId,
                        audioFile = chunkFile,
                        latitude = null,
                        longitude = null,
                        batteryPercent = readBatteryPercent()
                    ).collect { result ->
                        if (result is DetectionResult.Error) {
                            Log.w(TAG, "Background chunk upload failed: ${result.message}")
                        }
                    }

                    chunkFile.delete()
                    delay(500)
                } catch (e: Exception) {
                    Log.e(TAG, "Background recording loop error: ${e.message}")
                    ensureRecorderReady()
                    delay(400)
                }
            }
        }
    }

    private fun stopRecordingLoop() {
        isRecording.set(false)
        recordingJob?.cancel()
        recordingJob = null
        runCatching { audioRecorder.stopSafely() }
    }

    private fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun ensureRecorderReady(): Boolean {
        runCatching { audioRecorder.stopSafely() }
        return audioRecorder.initialize()
    }

    private fun isForegroundOwnerActive(): Boolean {
        return getSharedPreferences(MONITOR_PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_FOREGROUND_OWNER_ACTIVE, false)
    }

    private fun readBatteryPercent(): Int? {
        val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return null
        val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return null
        return ((level * 100f) / scale).toInt().coerceIn(0, 100)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "SafeEar Detection",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Background child monitoring status"
        }
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(text: String): Notification {
        val intent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SafeEar Monitoring")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(intent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        if (!isServiceActive) return
        
        val notification = createNotification(text)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
        
        // This must be called for startForeground
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update foreground notification: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "DetectionService destroyed")
        monitoringSyncJob?.cancel()
        stopRecordingLoop()
        runCatching { audioRecorder.release() }
        serviceScope.cancel()
        isServiceActive = false
    }

    companion object {
        private const val TAG = "SafeEar-DetectionService"
        private const val CHANNEL_ID = "detection_channel"
        private const val NOTIFICATION_ID = 1001
        private const val MIN_VALID_WAV_BYTES = 44L
        private const val MONITOR_PREFS = "safeear_monitoring"
        private const val KEY_FOREGROUND_OWNER_ACTIVE = "foreground_owner_active"
    }
}
