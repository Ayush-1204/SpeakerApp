package com.example.speakerapp.ui.parentmode

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.speakerapp.models.Alert
import com.example.speakerapp.network.Constants
import com.example.speakerapp.network.RetrofitInstance
import com.example.speakerapp.network.ServerAlert
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

class ParentViewModel(application: Application) : AndroidViewModel(application) {

    val alerts = mutableStateListOf<Alert>()
    val familiarList = mutableStateListOf<String>()
    var connectionStatus by mutableStateOf("Unknown")

    private var mediaPlayer: MediaPlayer? = null
    private val api = RetrofitInstance.api

    init {
        // Start polling for alerts and refreshing the familiar list.
        viewModelScope.launch {
            while (isActive) {
                fetchAlertsFromServer()
                refreshFamiliarList()
                delay(5000) // Poll every 5 seconds
            }
        }
    }

    fun fetchAlertsFromServer() {
        viewModelScope.launch {
            try {
                val response = api.getAlerts()
                if (response.isSuccessful) {
                    response.body()?.let { serverAlerts ->
                        addServerAlerts(serverAlerts)
                    }
                } else {
                    Log.e("ParentViewModel", "Failed to fetch alerts. Code: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("ParentViewModel", "Exception when fetching alerts: ${e.message}", e)
            }
        }
    }

    private suspend fun addServerAlerts(serverAlerts: List<ServerAlert>) {
        val context = getApplication<Application>().applicationContext
        withContext(Dispatchers.IO) {
            serverAlerts.forEach { serverAlert ->
                val timestampAsLong = serverAlert.timestamp.toLongOrNull()

                if (timestampAsLong == null) {
                    Log.e("ParentViewModel", "Received invalid timestamp from server: ${serverAlert.timestamp}")
                    return@forEach
                }

                if (alerts.none { it.timestamp == timestampAsLong }) {
                    val audioFile = downloadAudio(context, serverAlert.audio_url)
                    if (audioFile != null) {
                        val newAlert = Alert(
                            timestamp = timestampAsLong,
                            location = serverAlert.location,
                            audio = audioFile
                        )
                        withContext(Dispatchers.Main) {
                            alerts.add(0, newAlert)
                        }
                    }
                }
            }
        }
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
        try {
            if (alert.audio.exists()) {
                alert.audio.delete()
            }
        } catch(e: Exception) {
            Log.e("ParentViewModel", "Error deleting audio file for alert.", e)
        }
        alerts.remove(alert)
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
                    familiarList.clear()
                    familiarList.addAll(response.body()!!.speakers)
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
                    familiarList.remove(name)
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
        mediaPlayer?.release()
    }
}
