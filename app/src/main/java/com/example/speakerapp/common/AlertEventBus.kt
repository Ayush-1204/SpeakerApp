package com.example.speakerapp.common

import com.example.speakerapp.models.Alert
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object AlertEventBus {
    private val _events = MutableSharedFlow<Alert>()
    val events = _events.asSharedFlow()

    suspend fun postEvent(event: Alert) {
        _events.emit(event)
    }
}
