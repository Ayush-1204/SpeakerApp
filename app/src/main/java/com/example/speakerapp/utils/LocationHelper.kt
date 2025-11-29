package com.example.speakerapp.utils

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object LocationHelper {

    @SuppressLint("MissingPermission")
    suspend fun getLocation(context: Context): String =
        suspendCancellableCoroutine { cont ->

            val client = LocationServices.getFusedLocationProviderClient(context)

            // Permissions must be checked before calling this function
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        val gmapsUrl = "http://maps.google.com/maps?q=loc:${loc.latitude},${loc.longitude}"
                        Log.d("LocationHelper", "Location found: $gmapsUrl")
                        cont.resume(gmapsUrl)
                    } else {
                        Log.w("LocationHelper", "Failed to get location: location object is null.")
                        cont.resume("Unknown Location (Not Found)")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("LocationHelper", "Failed to get location", e)
                    cont.resume("Unknown Location (Error)")
                }
        }
}
