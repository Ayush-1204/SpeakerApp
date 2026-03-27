package com.example.speakerapp.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceDto(
    @SerialName("id") val id: String,
    @SerialName("parent_id") val parentId: String,
    @SerialName("device_name") val deviceName: String,
    @SerialName("role") val role: String,
    @SerialName("device_token") val deviceToken: String?
)

@Serializable
data class DeviceResponse(
    @SerialName("status") val status: String,
    @SerialName("device") val device: DeviceDto
)
