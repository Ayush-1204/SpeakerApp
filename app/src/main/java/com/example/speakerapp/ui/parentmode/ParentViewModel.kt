package com.example.speakerapp.ui.parentmode

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.speakerapp.models.Alert
import com.example.speakerapp.network.Constants
import com.example.speakerapp.network.RetrofitInstance
import com.example.speakerapp.network.ServerAlert
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

class ParentViewModel(application: Application) : AndroidViewModel(application) {

    var alerts by mutableStateOf<List<Alert>>(emptyList())
    var familiarList by mutableStateOf<List<String>>(emptyList())
    var connectionStatus by mutableStateOf("Not Tested")

    private var lastAlertTimestamp: Long = 0
    private var pollingJob: Job? = null

    private var mediaPlayer: MediaPlayer? = null
    private val api = RetrofitInstance.api

    init {
        startPollingForAlerts()
        refreshFamiliarList()
    }

    private fun startPollingForAlerts() {
        pollingJob?.cancel() // Cancel any existing job
        pollingJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val newAlerts = api.getAlerts(since = lastAlertTimestamp)
                    if (newAlerts.isSuccessful) {
                        newAlerts.body()?.let { serverAlerts ->
                            if (serverAlerts.isNotEmpty()) {
                                // Update the timestamp to the latest one received
                                lastAlertTimestamp = serverAlerts.maxOfOrNull { it.timestamp } ?: lastAlertTimestamp

                                val downloadedAlerts = downloadAlertAudios(serverAlerts)

                                // Add new alerts to the existing list, avoiding duplicates
                                val currentAlerts = alerts.toMutableList()
                                downloadedAlerts.forEach { newAlert ->
                                    if (currentAlerts.none { it.timestamp == newAlert.timestamp }) {
                                        currentAlerts.add(newAlert)
                                    }
                                }

                                // Sort by timestamp descending to show newest first
                                withContext(Dispatchers.Main) {
                                    alerts = currentAlerts.sortedByDescending { it.timestamp }
                                }
                            }
                        }
                    }

                } catch (e: Exception) {
                    Log.e("ParentViewModel", "Failed to fetch alerts: ${e.message}")
                    // Handle error, maybe update connection status
                }
                delay(5000) // Poll every 5 seconds
            }
        }
    }

    private suspend fun downloadAlertAudios(serverAlerts: List<ServerAlert>): List<Alert> {
        val context = getApplication<Application>().applicationContext
        val downloadedAlerts = mutableListOf<Alert>()

        withContext(Dispatchers.IO) {
            serverAlerts.forEach { serverAlert ->
                val audioFile = downloadAudio(context, serverAlert.audio_url)
                if (audioFile != null) {
                    downloadedAlerts.add(
                        Alert(
                            timestamp = serverAlert.timestamp,
                            location = serverAlert.location,
                            audio = audioFile
                        )
                    )
                }
            }
        }
        return downloadedAlerts
    }


    private fun downloadAudio(context: Context, urlPath: String): File? {
        return try {
            val client = OkHttpClient()
            val fullUrl = Constants.BASE_URL.removeSuffix("/") + urlPath
            val request = Request.Builder().url(fullUrl).build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e("ParentViewModel", "Failed to download audio file from $fullUrl. Code: ${response.code}")
                return null
            }

            val fileName = urlPath.substringAfterLast('/')
            val file = File(context.cacheDir, fileName)
            response.body?.byteStream()?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Log.d("ParentViewModel", "Audio downloaded to ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e("ParentViewModel", "Exception downloading audio: ${e.message}", e)
            null
        }
    }

    fun removeAlert(alert: Alert) {
        alerts = alerts.filterNot { it.timestamp == alert.timestamp }
        try {
            if (alert.audio.exists()) {
                alert.audio.delete()
            }
        } catch(e: Exception) {
            Log.e("ParentViewModel", "Error deleting audio file for alert.", e)
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            connectionStatus = try {
                val response = api.testConnection()
                if (response.isSuccessful) "Connected" else "Failed: ${response.code()}"
            } catch (e: Exception) {
                "Failed: ${e.message}"
            }
        }
    }

    fun refreshFamiliarList() {
        viewModelScope.launch {
            try {
                val response = api.listSpeakers()
                if (response.isSuccessful && response.body() != null) {
                     withContext(Dispatchers.Main) {
                        familiarList = response.body()!!.speakers
                    }
                }
            } catch (e: Exception) {
                 Log.e("ParentViewModel", "Failed to refresh familiar list.", e)
            }
        }
    }

    fun deleteSpeaker(name: String) {
        viewModelScope.launch {
            try {
                val response = api.deleteSpeaker(name)
                if (response.isSuccessful) {
                    refreshFamiliarList()
                }
            } catch (e: Exception) {
                Log.e("ParentViewModel", "Failed to delete speaker.", e)
            }
        }
    }

    fun playAudio(audio: File) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audio.absolutePath)
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("ParentViewModel", "Failed to play audio.", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel() // Ensure the coroutine is cancelled when ViewModel is destroyed
        mediaPlayer?.release()
        Log.d("ParentViewModel", "Polling stopped.")
    }
}
