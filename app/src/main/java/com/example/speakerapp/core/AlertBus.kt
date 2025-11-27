package com.example.speakerapp.core

import com.example.speakerapp.models.Alert
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object AlertBus {

    // replay = 1 is okay — but we will consume only once in MainActivity
    private val _alerts = MutableSharedFlow<Alert>(replay = 1)
    val alerts = _alerts.asSharedFlow()

    fun sendAlert(alert: Alert) {
        _alerts.tryEmit(alert)
    }
}
