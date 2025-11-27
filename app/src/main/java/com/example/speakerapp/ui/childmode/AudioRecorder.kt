package com.example.speakerapp.ui.childmode

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class AudioRecorder(
    private val outputDir: File,
    private val onStrangerDetected: () -> Unit
) {
    private val sampleRate = 16000
    private val channel = AudioFormat.CHANNEL_IN_MONO
    private val format = AudioFormat.ENCODING_PCM_16BIT

    private val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channel, format)
    private var recorder: AudioRecord? = null

    private val client = OkHttpClient()
    private val baseUrl = "http://10.0.2.2:8000"

    @SuppressLint("MissingPermission")
    suspend fun start() = withContext(Dispatchers.IO) {
        recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channel,
            format,
            minBufferSize
        )

        try {
            val buffer = ByteArray(minBufferSize)
            recorder?.startRecording()

            while (isActive) {
                val read = recorder?.read(buffer, 0, minBufferSize) ?: 0
                if (read > 0) {
                    val energy = buffer.take(2000).sumOf { it * it }
                    if (energy > 300000) {
                        val wav = saveAsWav(buffer.copyOf())
                        sendToBackend(wav)
                    }
                }
            }
        } finally {
            recorder?.stop()
            recorder?.release()
        }
    }

    private fun saveAsWav(raw: ByteArray): File {
        val wavFile = File(outputDir, "chunk_${System.currentTimeMillis()}.wav")
        wavFile.outputStream().use { out ->
            val totalDataLen = raw.size + 36
            val byteRate = sampleRate * 2

            out.write("RIFF".toByteArray())
            out.write(intToBytes(totalDataLen))
            out.write("WAVEfmt ".toByteArray())
            out.write(intToBytes(16))
            out.write(shortToBytes(1))
            out.write(shortToBytes(1))
            out.write(intToBytes(sampleRate))
            out.write(intToBytes(byteRate))
            out.write(shortToBytes(2))
            out.write(shortToBytes(16))
            out.write("data".toByteArray())
            out.write(intToBytes(raw.size))
            out.write(raw)
        }
        return wavFile
    }

    private fun intToBytes(i: Int) = byteArrayOf(
        (i and 0xff).toByte(),
        ((i shr 8) and 0xff).toByte(),
        ((i shr 16) and 0xff).toByte(),
        ((i shr 24) and 0xff).toByte(),
    )

    private fun shortToBytes(i: Int) = byteArrayOf(
        (i and 0xff).toByte(),
        ((i shr 8) and 0xff).toByte(),
    )

    private fun sendToBackend(file: File) {
        try {
            val reqBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    file.name,
                    file.readBytes().toRequestBody()
                )
                .build()

            val request = Request.Builder()
                .url("$baseUrl/recognize")
                .post(reqBody)
                .build()

            client.newCall(request).execute().use { response ->
                val result = response.body?.string() ?: ""
                if ("stranger" in result.lowercase()) {
                    onStrangerDetected()
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
