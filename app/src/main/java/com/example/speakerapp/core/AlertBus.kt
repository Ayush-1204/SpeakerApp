package com.example.speakerapp.core

import com.example.speakerapp.models.Alert
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File

// A simpler event for when a stranger is first detected
data class StrangerDetectedEvent(val audio: File)

object AlertBus {

    // Flow for raw detection events from the service
    private val _strangerEvents = MutableSharedFlow<StrangerDetectedEvent>(replay = 1)
    val strangerEvents = _strangerEvents.asSharedFlow()

    // Flow for completed alerts, enriched with location data
    private val _alerts = MutableSharedFlow<Alert>(replay = 1)
    val alerts = _alerts.asSharedFlow()

    // Called by the service
    fun postStrangerEvent(event: StrangerDetectedEvent) {
        _strangerEvents.tryEmit(event)
    }

    // Called by the ViewModel after location is fetched
    fun sendAlert(alert: Alert) {
        _alerts.tryEmit(alert)
    }
}
