package com.example.speakerapp.models

import java.io.File

data class Alert(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long,
    val audio: File,
    val location: String
)

