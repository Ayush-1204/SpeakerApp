package com.example.speakerapp.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.BatteryManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.core.content.ContextCompat
import com.example.speakerapp.MainActivity
import com.example.speakerapp.R
import com.example.speakerapp.core.audio.AudioRecorder
import com.example.speakerapp.core.auth.TokenManager
import com.example.speakerapp.features.auth.data.AuthRepository
import com.example.speakerapp.features.auth.data.AuthResult
import com.example.speakerapp.features.detection.data.DetectionRepository
import com.example.speakerapp.features.devices.data.DeviceRepository
import com.example.speakerapp.features.devices.data.DeviceResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import retrofit2.HttpException

/**
 * Background Detection Service for child monitoring
 * Runs as a foreground service to continue recording/streaming even when app is in background
 */
@AndroidEntryPoint
class DetectionService : Service() {

    @Inject lateinit var detectionRepository: DetectionRepository
    @Inject lateinit var deviceRepository: DeviceRepository
    @Inject lateinit var tokenManager: TokenManager
    @Inject lateinit var audioRecorder: AudioRecorder
    @Inject lateinit var authRepository: AuthRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isServiceActive = false
    private var monitoringSyncJob: Job? = null
    private var recordingJob: Job? = null
    private var retryJob: Job? = null
    private var chunkIndex = 0
    private val isRecording = AtomicBoolean(false)
    private val bufferedChunks = ArrayDeque<BufferedChunk>()
    private var offlineSinceMs: Long? = null
    private var connectionLostNotificationShown = false

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

        deviceRepository.startDevicePolling()
        startBufferedUploadLoop()
        updateNotification("Monitoring sync active")
        startRemoteMonitoringSync()
    }

    private fun stopAllAndSelf() {
        isServiceActive = false
        monitoringSyncJob?.cancel()
        monitoringSyncJob = null
        retryJob?.cancel()
        retryJob = null
        stopRecordingLoop()
        cancelConnectionLostNotification()
        deviceRepository.stopDevicePolling()

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
            stopAllAndSelf()
            return
        }

        val deviceId = tokenManager.getDeviceId()
        if (deviceId == null) {
            stopRecordingLoop()
            updateNotification("Device registration required")
            return
        }

        val selfDevice = deviceRepository.deviceCache.value.firstOrNull { it.id == deviceId }
        if (selfDevice != null) {
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

                    audioRecorder.recordAudio(1500, chunkFile)

                    if (chunkFile.length() <= MIN_VALID_WAV_BYTES) {
                        chunkFile.delete()
                        delay(300)
                        continue
                    }

                    tryUploadChunk(
                        deviceId = deviceId,
                        audioFile = chunkFile,
                        latitude = null,
                        longitude = null,
                        batteryPercent = readBatteryPercent()
                    )

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

    private fun startBufferedUploadLoop() {
        if (retryJob?.isActive == true) return
        retryJob = serviceScope.launch {
            while (isActive && isServiceActive) {
                tryFlushBufferedChunks()
                delay(5_000L)
            }
        }
    }

    private suspend fun tryUploadChunk(
        deviceId: String,
        audioFile: File,
        latitude: Double?,
        longitude: Double?,
        batteryPercent: Int?
    ) {
        try {
            detectionRepository.uploadDetectionChunkStrict(
                deviceId = deviceId,
                audioFile = audioFile,
                latitude = latitude,
                longitude = longitude,
                batteryPercent = batteryPercent
            )
            setOfflineState(false)
            cancelConnectionLostNotification()
            offlineSinceMs = null
        } catch (e: IOException) {
            Log.w(TAG, "chunk upload failed, buffering locally")
            bufferChunk(audioFile.readBytes(), deviceId, latitude, longitude, batteryPercent)
            setOfflineState(true)
        } catch (e: HttpException) {
            when (e.code()) {
                403 -> {
                    Log.w(TAG, "Stopping service: backend rejected chunk upload for non-child role")
                    stopAllAndSelf()
                }
                401 -> {
                    if (refreshAuthToken()) {
                        detectionRepository.uploadDetectionChunkStrict(
                            deviceId = deviceId,
                            audioFile = audioFile,
                            latitude = latitude,
                            longitude = longitude,
                            batteryPercent = batteryPercent
                        )
                        setOfflineState(false)
                        cancelConnectionLostNotification()
                        offlineSinceMs = null
                    } else {
                        showSessionExpiredNotification()
                        stopAllAndSelf()
                    }
                }
                else -> {
                    bufferChunk(audioFile.readBytes(), deviceId, latitude, longitude, batteryPercent)
                    setOfflineState(true)
                }
            }
        }
    }

    private fun bufferChunk(
        bytes: ByteArray,
        deviceId: String,
        latitude: Double?,
        longitude: Double?,
        batteryPercent: Int?
    ) {
        synchronized(bufferedChunks) {
            if (bufferedChunks.size >= 120) {
                if (bufferedChunks.isNotEmpty()) {
                    bufferedChunks.removeFirst()
                }
            }
            bufferedChunks.addLast(
                BufferedChunk(bytes, deviceId, latitude, longitude, batteryPercent)
            )
        }
        if (offlineSinceMs == null) {
            offlineSinceMs = System.currentTimeMillis()
        }
        maybeShowConnectionLostNotification()
    }

    private suspend fun tryFlushBufferedChunks() {
        val chunk = synchronized(bufferedChunks) { bufferedChunks.firstOrNull() }
            ?: run {
                setOfflineState(false)
                cancelConnectionLostNotification()
                offlineSinceMs = null
                return
            }

        val tempFile = File.createTempFile("retry_chunk_", ".wav", cacheDir)
        tempFile.writeBytes(chunk.bytes)

        try {
            detectionRepository.uploadDetectionChunkStrict(
                deviceId = chunk.deviceId,
                audioFile = tempFile,
                latitude = chunk.latitude,
                longitude = chunk.longitude,
                batteryPercent = chunk.batteryPercent
            )
            synchronized(bufferedChunks) {
                if (bufferedChunks.isNotEmpty()) bufferedChunks.removeFirst()
            }
            if (bufferedChunks.isEmpty()) {
                setOfflineState(false)
                cancelConnectionLostNotification()
                offlineSinceMs = null
            }
        } catch (e: IOException) {
            Log.w(TAG, "chunk upload failed, buffering locally")
            setOfflineState(true)
        } catch (e: HttpException) {
            when (e.code()) {
                403 -> stopAllAndSelf()
                401 -> {
                    if (!refreshAuthToken()) {
                        showSessionExpiredNotification()
                        stopAllAndSelf()
                    }
                }
            }
        } finally {
            tempFile.delete()
            maybeShowConnectionLostNotification()
        }
    }

    private suspend fun refreshAuthToken(): Boolean {
        var refreshed = false
        authRepository.refreshToken().collect { result ->
            when (result) {
                is AuthResult.Success -> refreshed = true
                is AuthResult.Error -> refreshed = false
                else -> Unit
            }
        }
        return refreshed
    }

    private fun setOfflineState(isOffline: Boolean) {
        getSharedPreferences(CONNECTIVITY_PREFS, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_IS_OFFLINE, isOffline) }
    }

    private fun maybeShowConnectionLostNotification() {
        val startedAt = offlineSinceMs ?: return
        if (connectionLostNotificationShown) return
        if (System.currentTimeMillis() - startedAt < 90_000L) return
        connectionLostNotificationShown = true

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SafeEar")
            .setContentText("connection lost — monitoring may be interrupted.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(CONNECTION_LOST_NOTIFICATION_ID, notification)
    }

    private fun cancelConnectionLostNotification() {
        if (!connectionLostNotificationShown) return
        connectionLostNotificationShown = false
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(CONNECTION_LOST_NOTIFICATION_ID)
    }

    private fun showSessionExpiredNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SafeEar session expired")
            .setContentText("tap to re-open the app.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(SESSION_EXPIRED_NOTIFICATION_ID, notification)
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

    private data class BufferedChunk(
        val bytes: ByteArray,
        val deviceId: String,
        val latitude: Double?,
        val longitude: Double?,
        val batteryPercent: Int?
    )

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
        retryJob?.cancel()
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
        private const val CONNECTION_LOST_NOTIFICATION_ID = 1002
        private const val SESSION_EXPIRED_NOTIFICATION_ID = 1003
        private const val CONNECTIVITY_PREFS = "safeear_connectivity"
        private const val KEY_IS_OFFLINE = "is_offline"
    }
}
