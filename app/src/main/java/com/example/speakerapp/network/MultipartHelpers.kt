package com.example.speakerapp.network

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

fun String.toTextBody(): RequestBody = toRequestBody("text/plain".toMediaType())

fun makeAudioPart(file: File, fieldName: String = "audio"): MultipartBody.Part {
    val body = file.asRequestBody("audio/wav".toMediaType())
    return MultipartBody.Part.createFormData(fieldName, file.name, body)
}

fun makeImagePart(file: File, fieldName: String = "profile_image"): MultipartBody.Part {
    val body = file.asRequestBody("image/*".toMediaType())
    return MultipartBody.Part.createFormData(fieldName, file.name, body)
}
