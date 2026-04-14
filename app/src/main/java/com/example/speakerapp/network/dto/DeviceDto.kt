package com.example.speakerapp.network.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

// ===== Device Registration Request/Response =====
@Serializable
data class DeviceRequest(
    val device_name: String,
    val role: String, // "child_device" or "parent_device"
    val device_token: String? = null
)

@Serializable
data class DeviceData(
    val id: String,
    val parent_id: String,
    val device_name: String,
    val role: String,
    val device_token: String? = null,
    val battery_percent: Int? = null,
    val is_online: Boolean? = null,
    val monitoring_enabled: Boolean? = null
)

@Serializable
data class DeviceResponse(
    val status: String,
    val device: DeviceData
)

@Serializable
data class DeviceListResponse(
    val items: List<DeviceData> = emptyList(),
    @SerialName("devices") val devices: List<DeviceData> = emptyList(),
    @SerialName("data") val data: List<DeviceData> = emptyList()
)

@Serializable
data class DeviceMonitoringUpdateRequest(
    val monitoring_enabled: Boolean
)

@Serializable
data class UpdateDeviceTokenRequest(
    val device_token: String
)
