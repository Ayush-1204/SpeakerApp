package com.example.speakerapp.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream

class AudioMonitor(
    private val context: Context,
    private val onWindowReady: (File) -> Unit
) {

    private var recordJob: Job? = null
    private var recorder: AudioRecord? = null

    fun start() {
        if (recordJob != null) return

        recordJob = CoroutineScope(Dispatchers.IO).launch {
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
            val windowSize = sampleRate * 3 * 2  // 3 seconds of 16-bit PCM

            recorder?.startRecording()

            val buffer = ByteArray(minBuffer)
            var pcmCollector = ByteArray(0)

            while (isActive) {
                val read = recorder!!.read(buffer, 0, buffer.size)
                if (read > 0) {
                    pcmCollector += buffer.copyOf(read)
                }

                if (pcmCollector.size >= windowSize) {
                    val windowFile = saveAsWav(pcmCollector.copyOf(windowSize))
                    onWindowReady(windowFile)

                    pcmCollector = pcmCollector.copyOfRange(windowSize, pcmCollector.size)
                }
            }
        }
    }

    fun stop() {
        recordJob?.cancel()
        recordJob = null

        recorder?.stop()
        recorder?.release()
        recorder = null
    }

    private fun saveAsWav(pcmData: ByteArray): File {
        val pcmFile = File(
            context.getExternalFilesDir(null),
            "window_${System.currentTimeMillis()}.wav"
        )
        val output = FileOutputStream(pcmFile)

        writeWavHeader(output, pcmData.size, 16000)
        output.write(pcmData)
        output.close()

        return pcmFile
    }

    private fun writeWavHeader(out: FileOutputStream, pcmLength: Int, sampleRate: Int) {
        val channels = 1
        val byteRate = 16 * sampleRate * channels / 8
        val totalDataLen = pcmLength + 36

        fun intLE(value: Int) = byteArrayOf(
            (value).toByte(), (value shr 8).toByte(),
            (value shr 16).toByte(), (value shr 24).toByte()
        )
        fun shortLE(value: Short) = byteArrayOf(
            (value.toInt() and 0xff).toByte(),
            ((value.toInt() shr 8) and 0xff).toByte()
        )

        out.write("RIFF".toByteArray())
        out.write(intLE(totalDataLen))
        out.write("WAVE".toByteArray())
        out.write("fmt ".toByteArray())
        out.write(intLE(16))
        out.write(shortLE(1))
        out.write(shortLE(channels.toShort()))
        out.write(intLE(sampleRate))
        out.write(intLE(byteRate))
        out.write(shortLE((channels * 2).toShort()))
        out.write(shortLE(16))
        out.write("data".toByteArray())
        out.write(intLE(pcmLength))
    }
}
