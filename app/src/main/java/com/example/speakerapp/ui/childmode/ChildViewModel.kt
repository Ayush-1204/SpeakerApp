package com.example.speakerapp.ui.childmode

import android.annotation.SuppressLint
import android.app.Application
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.speakerapp.audio.AudioMonitor
import com.example.speakerapp.core.AlertBus
import com.example.speakerapp.models.Alert
import com.example.speakerapp.network.Constants.BASE_URL
import com.google.android.gms.location.* 
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

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    var statusText: String = "Listening..."
        private set

    var onStrangerDetected: (() -> Unit)? = null

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
        lastRequestTime = now

        fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val request = buildRequest(file)
                        val response = client.newCall(request).execute()

                        val body = response.body?.string() ?: ""
                        if (body.contains("stranger", ignoreCase = true)) {
                            statusText = "⚠ Stranger Detected!"
                            val alert = Alert(
                                timestamp = System.currentTimeMillis(),
                                audio = file,
                                location = "${location.latitude}, ${location.longitude}"
                            )
                            AlertBus.sendAlert(alert)
                            onStrangerDetected?.invoke()
                        } else if (body.contains("familiar", ignoreCase = true)) {
                            statusText = "Familiar voice"
                        } else {
                            statusText = "Listening..."
                        }

                    } catch (e: Exception) {
                        statusText = "Error: ${e.message}"
                    }
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
