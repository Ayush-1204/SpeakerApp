package com.example.speakerapp.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.speakerapp.MainActivity
import com.example.speakerapp.core.AlertBus
import com.example.speakerapp.models.Alert
import com.example.speakerapp.network.Constants.BASE_URL
import com.example.speakerapp.utils.LocationHelper
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.*

class RecorderService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false
    private var alertRaised = false
    private val client = OkHttpClient()

    override fun onBind(intent: Intent?): IBinder? = null

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
        alertRaised = false

        createNotificationChannel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                1,
                createBaseNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(1, createBaseNotification())
        }

        serviceScope.launch { recordingLoop() }
    }

    private fun stopRecording() {
        isRunning = false
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    /************************************************************
     * RECORDING LOOP (Runs until stranger is detected)
     ************************************************************/
    private suspend fun recordingLoop() {
        while (isRunning && !alertRaised) {
            try {
                val wavFile = record10SecWav()
                if (wavFile != null) {
                    val json = sendToBackend(wavFile)
                    handleBackendResult(json, wavFile)
                }
            } catch (e: Exception) {
                Log.e("RecorderService", "Loop error: ${e.message}")
            }
        }
    }

    /************************************************************
     * 10 SECOND CHUNK RECORDING
     ************************************************************/
    @SuppressLint("MissingPermission")
    private suspend fun record10SecWav(): File? = withContext(Dispatchers.IO) {
        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBuffer = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        val recorder = AudioRecord(
            android.media.MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            minBuffer * 4
        )

        val pcmFile = File(cacheDir, "temp_chunk.pcm")
        val fos = FileOutputStream(pcmFile)
        val buffer = ByteArray(minBuffer)

        try {
            recorder.startRecording()
            val endTime = System.currentTimeMillis() + 10_000 // 10 seconds

            while (isRunning && System.currentTimeMillis() < endTime) {
                val read = recorder.read(buffer, 0, buffer.size)
                if (read > 0) fos.write(buffer, 0, read)
            }

        } finally {
            recorder.stop()
            recorder.release()
            fos.close()
        }

        val wavFile = File(cacheDir, "final_chunk.wav")
        rawToWav(pcmFile, wavFile, sampleRate)
        pcmFile.delete()

        wavFile
    }

    /************************************************************
     * WAV HEADER WRITER (CORRECTED)
     ************************************************************/
    private fun rawToWav(pcmFile: File, wavFile: File, sampleRate: Int) {
        val pcm = pcmFile.readBytes()
        DataOutputStream(FileOutputStream(wavFile)).use { out ->

            val channels = 1
            val bitsPerSample = 16
            val byteRate = sampleRate * channels * bitsPerSample / 8

            out.writeBytes("RIFF")
            out.writeInt(Integer.reverseBytes(36 + pcm.size))
            out.writeBytes("WAVE")
            out.writeBytes("fmt ")
            out.writeInt(Integer.reverseBytes(16))
            out.writeShort(java.lang.Short.reverseBytes(1.toShort()).toInt())
            out.writeShort(java.lang.Short.reverseBytes(channels.toShort()).toInt())
            out.writeInt(Integer.reverseBytes(sampleRate))
            out.writeInt(Integer.reverseBytes(byteRate))
            out.writeShort(java.lang.Short.reverseBytes((channels * bitsPerSample / 8).toShort()).toInt())
            out.writeShort(java.lang.Short.reverseBytes(bitsPerSample.toShort()).toInt())
            out.writeBytes("data")
            out.writeInt(Integer.reverseBytes(pcm.size))
            out.write(pcm)
        }
    }

    /************************************************************
     * SEND TO BACKEND
     ************************************************************/
    private fun sendToBackend(file: File): JSONObject? {
        val url = "${BASE_URL}recognize"

        return try {
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "chunk.wav",
                    file.asRequestBody("audio/wav".toMediaTypeOrNull()))
                .build()

            val req = Request.Builder().url(url).post(body).build()

            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return null
                res.body?.string()?.let { JSONObject(it) }
            }
        } catch (e: Exception) {
            Log.e("RecorderService", "HTTP error: ${e.message}")
            null
        }
    }

    /************************************************************
     * HANDLE BACKEND RESULT
     ************************************************************/
    private fun handleBackendResult(json: JSONObject?, chunkWav: File) {
        if (json == null) return

        val result = json.optString("result")

        if (result.equals("stranger", true) && !alertRaised) {
            alertRaised = true
            isRunning = false

            // Stop service
            startService(Intent(this, RecorderService::class.java).apply { action = "STOP" })

            serviceScope.launch {
                val location = try {
                    LocationHelper.getLocation(this@RecorderService)
                } catch (e: Exception) {
                    "Unknown Location"
                }

                // Persistent file
                val finalFile = File(
                    filesDir,
                    "alert_${System.currentTimeMillis()}.wav"
                )
                chunkWav.copyTo(finalFile, overwrite = true)

                AlertBus.sendAlert(
                    Alert(
                        timestamp = System.currentTimeMillis(),
                        audio = finalFile,
                        location = location
                    )
                )
            }

            sendAlertNotification("🚨 Stranger detected!")
        }
    }

    /************************************************************
     * NOTIFICATIONS
     ************************************************************/
    private fun createBaseNotification(): Notification {
        val intent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "recorder_channel")
            .setContentTitle("Child Protection Active")
            .setContentText("Listening…")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setContentIntent(intent)
            .build()
    }

    private fun sendAlertNotification(msg: String) {
        val n = NotificationCompat.Builder(this, "alert_channel")
            .setContentTitle("High Alert")
            .setContentText(msg)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .build()

        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(1001, n)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val recorder = NotificationChannel(
                "recorder_channel", "Recorder Service",
                NotificationManager.IMPORTANCE_LOW
            )

            val alert = NotificationChannel(
                "alert_channel", "Alert Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )

            manager.createNotificationChannel(recorder)
            manager.createNotificationChannel(alert)
        }
    }
}
