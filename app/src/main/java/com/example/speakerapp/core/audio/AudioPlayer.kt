package com.example.speakerapp.core.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton
import java.io.File

@Singleton
class AudioPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var exoPlayer: ExoPlayer? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    fun initialize() {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context).build()
        }
    }

    fun playFromFile(file: File) {
        if (exoPlayer == null) {
            initialize()
        }

        val mediaItem = MediaItem.fromUri(Uri.fromFile(file))
        exoPlayer?.apply {
            setMediaItem(mediaItem)
            prepare()
            play()
            _isPlaying.value = true
            _duration.value = duration
        }
    }

    fun playFromBytes(bytes: ByteArray, fileName: String = "temp.wav") {
        try {
            // Create temporary file from bytes
            val cacheDir = context.cacheDir
            val tempFile = File(cacheDir, fileName)
            tempFile.writeBytes(bytes)

            playFromFile(tempFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pause() {
        exoPlayer?.pause()
        _isPlaying.value = false
    }

    fun resume() {
        exoPlayer?.play()
        _isPlaying.value = true
    }

    fun stop() {
        exoPlayer?.stop()
        _isPlaying.value = false
        _currentPosition.value = 0
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }

    fun getCurrentPosition(): Long {
        return exoPlayer?.currentPosition ?: 0
    }

    fun getDuration(): Long {
        return exoPlayer?.duration ?: 0
    }

    fun release() {
        exoPlayer?.release()
        exoPlayer = null
        _isPlaying.value = false
    }
}
