package com.example.speakerapp.core.notifications

import com.example.speakerapp.core.auth.TokenManager
import com.example.speakerapp.features.devices.data.DeviceRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface FcmServiceEntryPoint {
    fun deviceRepository(): DeviceRepository
    fun tokenManager(): TokenManager
}