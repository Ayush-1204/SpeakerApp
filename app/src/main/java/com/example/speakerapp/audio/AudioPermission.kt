package com.example.speakerapp.audio

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class AudioPermission(private val activity: ComponentActivity) {

    private var onGranted: (() -> Unit)? = null

    private val launcher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onGranted?.invoke()
    }

    fun request(onGranted: () -> Unit) {
        this.onGranted = onGranted
        if (ContextCompat.checkSelfPermission(
                activity, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            onGranted()
        } else {
            launcher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}
