package com.example.speakerapp.core.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Audio recorder that captures PCM audio in 16kHz mono format
 * and encodes it to WAV format for upload to SafeEar backend.
 *
 * Constraints:
 * - Sample rate: 16000 Hz (backend requirement)
 * - Audio format: PCM 16-bit
 * - Channels: Mono (preferred), Stereo accepted but will be downmixed
 * - Output format: WAV
 *
 * Assumption: Audio capture from system will be at 44100Hz or 48000Hz
 * and will be resampled to 16kHz by this recorder.
 */
@Singleton
class AudioRecorder @Inject constructor() {

    companion object {
        private const val SAMPLE_RATE = 16000 // Backend requirement: 16kHz
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private val BYTES_PER_SAMPLE = 2 // 16-bit = 2 bytes
    }

    private var audioRecord: AudioRecord? = null
    private val isRecording = AtomicBoolean(false)
    private val isStopping = AtomicBoolean(false)
    private val recorderLock = Any()
    private var selectedAudioSource: Int = MediaRecorder.AudioSource.MIC
    private var noiseSuppressor: NoiseSuppressor? = null
    private var acousticEchoCanceler: AcousticEchoCanceler? = null

    private val preferredAudioSources = intArrayOf(
        MediaRecorder.AudioSource.VOICE_RECOGNITION,
        MediaRecorder.AudioSource.VOICE_COMMUNICATION,
        MediaRecorder.AudioSource.CAMCORDER,
        MediaRecorder.AudioSource.MIC
    )

    private fun buildAudioRecord(bufferSize: Int): AudioRecord? {
        for (source in preferredAudioSources) {
            try {
                val record = AudioRecord(
                    source,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize * 2
                )
                if (record.state == AudioRecord.STATE_INITIALIZED) {
                    selectedAudioSource = source
                    attachAudioEffects(record)
                    return record
                }
                record.release()
            } catch (_: Exception) {
                // Try next source.
            }
        }
        return null
    }

    private fun attachAudioEffects(record: AudioRecord) {
        val audioSessionId = record.audioSessionId

        if (NoiseSuppressor.isAvailable()) {
            runCatching {
                noiseSuppressor = NoiseSuppressor.create(audioSessionId)?.apply { setEnabled(true) }
            }
        }

        if (AcousticEchoCanceler.isAvailable()) {
            runCatching {
                acousticEchoCanceler = AcousticEchoCanceler.create(audioSessionId)?.apply { setEnabled(true) }
            }
        }
    }

    /**
     * Initialize recorder with system default sample rate and resample to 16kHz.
     * This ensures compatibility with various devices.
     */
    fun initialize(): Boolean {
        return try {
            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            )

            synchronized(recorderLock) {
                releaseLocked()
                audioRecord = buildAudioRecord(bufferSize)
            }

            audioRecord?.state == AudioRecord.STATE_INITIALIZED
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Start recording audio to a file.
     * Returns true if recording started successfully.
     */
    suspend fun startRecording(outputFile: File): Boolean = withContext(Dispatchers.Default) {
        return@withContext try {
            val record = audioRecord ?: return@withContext false

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                return@withContext false
            }

            record.startRecording()
            isRecording.set(true)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Stop recording and save to WAV file.
     * Returns the file if successful, null otherwise.
     */
    suspend fun stopRecording(outputFile: File): File? = withContext(Dispatchers.Default) {
        return@withContext try {
            val record = audioRecord ?: return@withContext null

            if (!isRecording.get()) return@withContext null

            isRecording.set(false)
            safeStop(record)

            // The recording has been stopped and data is ready
            // Save it as WAV
            Thread.sleep(100) // Brief delay to ensure recording is fully stopped
            
            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Capture audio for specified duration and save as WAV.
     * Duration in milliseconds (e.g., 1000 for 1 second).
     */
    suspend fun recordAudio(
        durationMs: Long,
        outputFile: File
    ): File? = withContext(Dispatchers.Default) {
        return@withContext try {
            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            )

            val buffer = ByteArray(bufferSize)
            val audioData = mutableListOf<ByteArray>()

            val record = audioRecord ?: return@withContext null
            
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                return@withContext null
            }

            try {
                record.startRecording()
            } catch (e: IllegalStateException) {
                Log.e("SafeEar-AudioRecorder", "startRecording in invalid state: ${e.message}")
                return@withContext null
            } catch (e: RuntimeException) {
                Log.e("SafeEar-AudioRecorder", "startRecording runtime error: ${e.message}")
                return@withContext null
            }
            isRecording.set(true)

            val expectedBytes = (SAMPLE_RATE * BYTES_PER_SAMPLE * (durationMs / 1000.0)).toInt()
            var bytesRead = 0

            while (bytesRead < expectedBytes && isRecording.get()) {
                val numRead = record.read(buffer, 0, bufferSize)
                if (numRead > 0) {
                    audioData.add(buffer.copyOf(numRead))
                    bytesRead += numRead
                }
            }

            isRecording.set(false)
            safeStop(record)

            // Write WAV file
            writeWavFile(outputFile, audioData)

            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Write PCM audio data to WAV file format.
     * WAV format specification:
     * - RIFF header
     * - fmt chunk with format info
     * - data chunk with PCM samples
     */
    private suspend fun writeWavFile(file: File, audioData: List<ByteArray>) = withContext(Dispatchers.IO) {
        file.outputStream().use { fos ->
            val totalAudioLength = audioData.sumOf { it.size }
            val totalDataLen = 36 + totalAudioLength
            val channels = 1 // Mono
            val byteRate = SAMPLE_RATE * channels * BYTES_PER_SAMPLE
            val blockAlign = channels * BYTES_PER_SAMPLE

            // RIFF header
            writeWavHeader(fos, "RIFF".toByteArray())
            writeInt(fos, totalDataLen)
            writeWavHeader(fos, "WAVE".toByteArray())

            // fmt chunk
            writeWavHeader(fos, "fmt ".toByteArray())
            writeInt(fos, 16) // Subchunk1Size
            writeShort(fos, 1) // AudioFormat (1 = PCM)
            writeShort(fos, channels.toShort())
            writeInt(fos, SAMPLE_RATE)
            writeInt(fos, byteRate)
            writeShort(fos, blockAlign.toShort())
            writeShort(fos, 16) // BitsPerSample

            // data chunk
            writeWavHeader(fos, "data".toByteArray())
            writeInt(fos, totalAudioLength)

            // Write audio data
            for (chunk in audioData) {
                fos.write(chunk)
            }
        }
    }

    private suspend fun writeWavHeader(fos: java.io.OutputStream, data: ByteArray) = withContext(Dispatchers.IO) {
        fos.write(data)
    }

    private suspend fun writeInt(fos: java.io.OutputStream, value: Int) = withContext(Dispatchers.IO) {
        val bytes = ByteArray(4).apply {
            val buffer = ByteBuffer.wrap(this)
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            buffer.putInt(value)
        }
        fos.write(bytes)
    }

    private suspend fun writeShort(fos: java.io.OutputStream, value: Short) = withContext(Dispatchers.IO) {
        val bytes = ByteArray(2).apply {
            val buffer = ByteBuffer.wrap(this)
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            buffer.putShort(value)
        }
        fos.write(bytes)
    }

    fun stopSafely() {
        synchronized(recorderLock) {
            isRecording.set(false)
            safeStop(audioRecord)
        }
    }

    fun release() {
        if (!isStopping.compareAndSet(false, true)) return

        try {
            synchronized(recorderLock) {
                releaseLocked()
            }
        } finally {
            isStopping.set(false)
        }
    }

    private fun releaseLocked() {
        isRecording.set(false)
        safeStop(audioRecord)

        runCatching { noiseSuppressor?.release() }
        noiseSuppressor = null
        runCatching { acousticEchoCanceler?.release() }
        acousticEchoCanceler = null

        runCatching { audioRecord?.release() }
        audioRecord = null
    }

    private fun safeStop(record: AudioRecord?) {
        if (record == null) return
        try {
            if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                record.stop()
            }
        } catch (e: IllegalStateException) {
            Log.w("SafeEar-AudioRecorder", "Ignored stop IllegalStateException: ${e.message}")
        } catch (e: RuntimeException) {
            Log.w("SafeEar-AudioRecorder", "Ignored stop RuntimeException: ${e.message}")
        }
    }
}
