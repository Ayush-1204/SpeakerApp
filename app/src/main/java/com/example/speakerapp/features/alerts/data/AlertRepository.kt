package com.example.speakerapp.features.alerts.data

import com.example.speakerapp.core.network.Resource
import com.example.speakerapp.core.network.SafeEarApi
import com.example.speakerapp.core.network.dto.AlertDto
import okhttp3.ResponseBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertRepository @Inject constructor(
    private val api: SafeEarApi
) {
    suspend fun getAlerts(limit: Int = 50, offset: Int = 0): Resource<List<AlertDto>> {
        return try {
            val response = api.getAlerts(limit, offset)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!.items)
            } else {
                Resource.Error(response.message() ?: "Failed to fetch alerts")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unknown error")
        }
    }

    suspend fun acknowledgeAlert(alertId: String): Resource<AlertDto> {
        return try {
            val response = api.acknowledgeAlert(alertId)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error(response.message() ?: "Failed to acknowledge alert")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unknown error")
        }
    }

    suspend fun getAlertClip(alertId: String): Resource<ResponseBody> {
        return try {
            val response = api.getAlertClip(alertId)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error(response.message() ?: "Failed to fetch audio clip")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unknown error")
        }
    }
}
