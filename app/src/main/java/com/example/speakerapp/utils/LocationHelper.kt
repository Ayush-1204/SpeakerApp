package com.example.speakerapp.utils

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object LocationHelper {

    @SuppressLint("MissingPermission")
    suspend fun getLocation(context: Context): String =
        suspendCancellableCoroutine { cont ->

            val client = LocationServices.getFusedLocationProviderClient(context)

            client.lastLocation
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        cont.resume("Lat: ${loc.latitude}, Lng: ${loc.longitude}")
                    } else {
                        cont.resume("Unknown Location")
                    }
                }
                .addOnFailureListener {
                    cont.resume("Unknown Location")
                }
        }
}
