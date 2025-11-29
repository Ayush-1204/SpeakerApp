package com.example.speakerapp.ui

import android.util.Log
import com.example.speakerapp.network.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request

suspend fun releaseModeLock(deviceId: String): Boolean =
    withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("device_id", deviceId)
                .build()

            val request = Request.Builder()
                .url("${Constants.BASE_URL}release_mode")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.e("LockUtils", "Exception releasing mode lock: ${e.message}")
            false
        }
    }
