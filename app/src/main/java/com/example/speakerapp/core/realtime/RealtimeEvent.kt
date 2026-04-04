package com.example.speakerapp.core.realtime

import kotlinx.serialization.json.JsonObject

data class RealtimeEvent(
    val type: String,
    val payload: JsonObject
)
