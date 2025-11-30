package com.example.speakerapp.ui

import android.content.Context
import java.util.UUID

fun getDeviceID(context: Context): String {
    val sharedPrefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
    var deviceId = sharedPrefs.getString("DEVICE_ID", null)
    if (deviceId == null) {
        deviceId = UUID.randomUUID().toString()
        sharedPrefs.edit().putString("DEVICE_ID", deviceId).apply()
    }
    return deviceId
}
