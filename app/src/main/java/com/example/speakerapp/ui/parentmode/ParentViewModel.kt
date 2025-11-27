package com.example.speakerapp.ui.parentmode

import android.app.Application
import android.media.MediaPlayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.speakerapp.core.AlertBus
import com.example.speakerapp.models.Alert
import com.example.speakerapp.network.RetrofitInstance
import kotlinx.coroutines.launch
import java.io.File

class ParentViewModel(application: Application) : AndroidViewModel(application) {

    val alerts = mutableStateListOf<Alert>()
    val familiarList = mutableStateListOf<String>()
    var connectionStatus by mutableStateOf("Unknown")

    private var mediaPlayer: MediaPlayer? = null

    init {
        refreshFamiliarList()
        listenForAlerts()
    }

    private fun listenForAlerts() {
        viewModelScope.launch {
            AlertBus.alerts.collect { alert ->
                alerts.add(0, alert)
            }
        }
    }

    // --- THIS IS THE NEW FUNCTION TO REMOVE AN ALERT ---
    fun removeAlert(alert: Alert) {
        alerts.remove(alert)
    }

    fun testConnection() {
        viewModelScope.launch {
            connectionStatus = try {
                val response = RetrofitInstance.api.testConnection()
                if (response.isSuccessful) "Connected" else "Failed: ${response.code()}"
            } catch (e: Exception) {
                "Failed: ${e.message}"
            }
        }
    }

    fun refreshFamiliarList() {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.listSpeakers()
                if (response.isSuccessful && response.body() != null) {
                    familiarList.clear()
                    familiarList.addAll(response.body()!!.speakers)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun deleteSpeaker(name: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.deleteSpeaker(name)
                if (response.isSuccessful) {
                    familiarList.remove(name)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun flagAsFamiliar(alert: Alert) {
        viewModelScope.launch {
            try {
                // This logic needs to be revisited, but for now we keep it
                val response = RetrofitInstance.api.flagFamiliar(alert.audio.name)
                if (response.isSuccessful) {
                    alerts.remove(alert)
                    refreshFamiliarList()
                }
            } catch (e: Exception) {
                // Handle error
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
            // Handle error
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
    }
}