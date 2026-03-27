package com.example.speakerapp.ui.childmode

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.speakerapp.audio.AudioMonitor
import com.example.speakerapp.network.Constants.BASE_URL
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class ChildViewModel(application: Application) : AndroidViewModel(application) {

    private val client = OkHttpClient()
    private var lastRequestTime = 0L
    private val cooldownMs = 3000L
    private var lastStrangerAlertTime = 0L
    private val strangerAlertCooldownMs = 30000L // 30 seconds

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    var statusText: String = "Listening..."
        private set

    private val audioMonitor = AudioMonitor(getApplication()) { windowFile ->
        processAudio(windowFile)
    }

    fun startMonitoring() {
        audioMonitor.start()
    }

    fun stopMonitoring() {
        audioMonitor.stop()
    }

    @SuppressLint("MissingPermission")
    private fun processAudio(file: File) {
        val now = System.currentTimeMillis()
        if (now - lastRequestTime < cooldownMs) return

        if (lastStrangerAlertTime != 0L && now - lastStrangerAlertTime < strangerAlertCooldownMs) {
            // In cooldown after stranger detection, so don't send a new request.
            return
        }

        lastRequestTime = now

        // The location is updated by the RecorderService in the background.
        // We just need to send the audio for recognition.
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = buildRequest(file)
                val response = client.newCall(request).execute()

                val body = response.body?.string() ?: ""
                if (body.contains("stranger", ignoreCase = true)) {
                    statusText = "⚠ Stranger Detected!"
                    lastStrangerAlertTime = now
                    // The backend handles alert creation.
                    // The child device should not show a pop-up.
                } else if (body.contains("familiar", ignoreCase = true)) {
                    statusText = "Familiar voice"
                    lastStrangerAlertTime = 0L
                } else {
                    statusText = "Listening..."
                    lastStrangerAlertTime = 0L
                }

            } catch (e: Exception) {
                statusText = "Error: ${e.message}"
                lastStrangerAlertTime = 0L
            }
        }
    }

    private fun buildRequest(file: File): Request {
        val mediaType = "audio/wav".toMediaTypeOrNull()
        val fileBody = file.asRequestBody(mediaType)

        val form = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, fileBody)
            .build()

        return Request.Builder()
            .url("$BASE_URL/recognize")
            .post(form)
            .build()
    }

    override fun onCleared() {
        super.onCleared()
        stopMonitoring()
    }
}
