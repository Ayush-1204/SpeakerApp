package com.example.speakerapp.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.media.AudioFormat
import android.media.AudioRecord
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.speakerapp.MainActivity
import com.example.speakerapp.network.Constants.BASE_URL
import com.example.speakerapp.ui.NotificationHelper
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.*

class RecorderService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false
    private val client = OkHttpClient()
    private lateinit var fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient
    private var locationJob: Job? = null
    private var recordingJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        NotificationHelper.createNotificationChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START" -> start()
            "STOP" -> stopRecording()
        }
        return START_STICKY
    }

    private fun start() {
        if (isRunning) return
        isRunning = true

        startForeground(1, createBaseNotification("Listening for sounds..."))

        locationJob = serviceScope.launch { locationUpdateLoop() }
        recordingJob = serviceScope.launch { recordingLoop() }
    }

    private fun stopRecording() {
        isRunning = false
        locationJob?.cancel()
        recordingJob?.cancel()
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private suspend fun locationUpdateLoop() {
        while (isRunning) {
            getCurrentLocation()?.let { sendLocationToBackend(it) }
            delay(10000) // 10 seconds
        }
    }

    private suspend fun sendLocationToBackend(location: Location) {
        try {
            val locationData = mapOf("latitude" to location.latitude, "longitude" to location.longitude)
            val request = Request.Builder()
                .url("${BASE_URL}update_location")
                .post(Gson().toJson(locationData).toRequestBody("application/json".toMediaTypeOrNull()))
                .build()

            client.newCall(request).execute().use {
                if (it.isSuccessful) Log.d("RecorderService", "Location updated.")
                else Log.e("RecorderService", "Failed to update location: ${it.code}")
            }
        } catch (e: Exception) {
            Log.e("RecorderService", "Location update exception: ${e.message}")
        }
    }

    private suspend fun recordingLoop() {
        while (isRunning) {
            val wavFile = record10SecWav()
            if (wavFile != null) {
                val json = sendToBackend(wavFile)
                val result = json?.optString("result")

                if (result.equals("stranger", true)) {
                    handleStrangerDetected()
                    // Cooldown period
                    updateNotification("Stranger detected. Cooling down...")
                    delay(3 * 60 * 1000) // 3 minutes
                    updateNotification("Resuming listening...")
                }
                wavFile.delete()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun record10SecWav(): File? = withContext(Dispatchers.IO) {
        val minBuffer = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val recorder = AudioRecord(android.media.MediaRecorder.AudioSource.MIC, 16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBuffer * 4)
        val pcmFile = File(cacheDir, "temp.pcm")

        try {
            recorder.startRecording()
            FileOutputStream(pcmFile).use { fos ->
                val buffer = ByteArray(minBuffer)
                val endTime = System.currentTimeMillis() + 10000
                while (System.currentTimeMillis() < endTime && isRunning) {
                    val read = recorder.read(buffer, 0, buffer.size)
                    if (read > 0) fos.write(buffer, 0, read)
                }
            }
        } finally {
            recorder.stop()
            recorder.release()
        }

        val wavFile = File(cacheDir, "chunk.wav")
        rawToWav(pcmFile, wavFile)
        pcmFile.delete()
        wavFile
    }

    private fun rawToWav(pcmFile: File, wavFile: File) {
        val pcm = pcmFile.readBytes()
        DataOutputStream(FileOutputStream(wavFile)).use { out ->
            out.writeBytes("RIFF")
            out.writeInt(Integer.reverseBytes(36 + pcm.size))
            out.writeBytes("WAVEfmt ")
            out.writeInt(Integer.reverseBytes(16))
            out.writeShort(java.lang.Short.reverseBytes(1.toShort()).toInt())
            out.writeShort(java.lang.Short.reverseBytes(1.toShort()).toInt())
            out.writeInt(Integer.reverseBytes(16000))
            out.writeInt(Integer.reverseBytes(16000 * 2))
            out.writeShort(java.lang.Short.reverseBytes(2.toShort()).toInt())
            out.writeShort(java.lang.Short.reverseBytes(16.toShort()).toInt())
            out.writeBytes("data")
            out.writeInt(Integer.reverseBytes(pcm.size))
            out.write(pcm)
        }
    }

    private fun sendToBackend(file: File): JSONObject? {
        return try {
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "chunk.wav", file.asRequestBody("audio/wav".toMediaTypeOrNull()))
                .build()
            val request = Request.Builder().url("${BASE_URL}recognize").post(body).build()

            client.newCall(request).execute().use { res ->
                if (res.isSuccessful) res.body?.string()?.let { JSONObject(it) } else null
            }
        } catch (e: Exception) {
            Log.e("RecorderService", "Backend error: ${e.message}")
            null
        }
    }

    private fun handleStrangerDetected() {
        // The backend now creates the alert, so we only need to notify the parent.
        // The ParentViewModel will handle the high-priority notification.
        Log.d("RecorderService", "Stranger detected. The ParentViewModel will notify the parent device.")
    }

    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocation(): Location? {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return null
        return fusedLocationClient.lastLocation.await()
    }

    private fun createBaseNotification(text: String): Notification {
        val intent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, "recorder_channel")
            .setContentTitle("Child Protection Active")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setContentIntent(intent)
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = createBaseNotification(text)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val recorder = NotificationChannel("recorder_channel", "Recorder Service", NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(recorder)
        }
    }
}
