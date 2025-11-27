package com.example.speakerapp.audio

import android.app.Activity
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.cancellation.CancellationException

class AudioRecorder(private val activity: Activity) {

    private var recorder: AudioRecord? = null
    private var isRecording = false

    suspend fun startRecording(): File = withContext(Dispatchers.IO) {
        val sampleRate = 16000
        val channel = AudioFormat.CHANNEL_IN_MONO
        val encoding = AudioFormat.ENCODING_PCM_16BIT

        val minBuffer = AudioRecord.getMinBufferSize(sampleRate, channel, encoding)
        recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channel,
            encoding,
            minBuffer
        )

        val rawFile = File(
            activity.getExternalFilesDir(null),
            "recording_${System.currentTimeMillis()}.pcm"
        )
        val output = FileOutputStream(rawFile)

        recorder?.startRecording()
        isRecording = true

        val buffer = ByteArray(minBuffer)

        try {
            while (isActive && isRecording) {
                val read = recorder!!.read(buffer, 0, buffer.size)
                if (read > 0) {
                    output.write(buffer, 0, read)
                }
            }
        } catch (e: CancellationException) {
            // recording stopped
        } finally {
            recorder?.stop()
            recorder?.release()
            recorder = null
            output.close()
        }

        // Convert raw PCM → WAV
        return@withContext convertPcmToWav(rawFile)
    }

    fun stopRecording() {
        isRecording = false
    }

    private fun convertPcmToWav(pcmFile: File): File {
        val wavFile = File(pcmFile.absolutePath.replace(".pcm", ".wav"))

        val pcmData = pcmFile.readBytes()
        val wavOutput = FileOutputStream(wavFile)

        // WAV HEADER
        val totalDataLen = pcmData.size + 36
        val sampleRate = 16000
        val channels = 1
        val byteRate = 16 * sampleRate * channels / 8

        wavOutput.write("RIFF".toByteArray())
        wavOutput.write(intToLittleEndian(totalDataLen))
        wavOutput.write("WAVE".toByteArray())
        wavOutput.write("fmt ".toByteArray())
        wavOutput.write(intToLittleEndian(16)) // Subchunk1 Size
        wavOutput.write(shortToLittleEndian(1)) // Audio Format (PCM)
        wavOutput.write(shortToLittleEndian(channels.toShort()))
        wavOutput.write(intToLittleEndian(sampleRate))
        wavOutput.write(intToLittleEndian(byteRate))
        wavOutput.write(shortToLittleEndian((channels * 2).toShort()))
        wavOutput.write(shortToLittleEndian(16)) // Bits per sample
        wavOutput.write("data".toByteArray())
        wavOutput.write(intToLittleEndian(pcmData.size))
        wavOutput.write(pcmData)
        wavOutput.close()

        pcmFile.delete() // remove raw PCM
        return wavFile
    }

    private fun intToLittleEndian(value: Int): ByteArray =
        byteArrayOf(
            (value and 0xff).toByte(),
            ((value shr 8) and 0xff).toByte(),
            ((value shr 16) and 0xff).toByte(),
            ((value shr 24) and 0xff).toByte()
        )

    private fun shortToLittleEndian(value: Short): ByteArray =
        byteArrayOf(
            (value.toInt() and 0xff).toByte(),
            ((value.toInt() shr 8) and 0xff).toByte()
        )
}
