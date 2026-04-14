package com.example.speakerapp.core.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import javax.inject.Inject
import javax.inject.Singleton
import java.io.File

@Singleton
class AudioPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var exoPlayer: ExoPlayer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var progressJob: Job? = null
    private var playRequestJob: Job? = null
    private var currentTempFile: File? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    fun initialize() {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context).build()
            exoPlayer?.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    _isBuffering.value = playbackState == Player.STATE_BUFFERING
                    updatePlaybackSnapshot()
                }

                override fun onPlayerError(error: PlaybackException) {
                    _isBuffering.value = false
                    _isPlaying.value = false
                    _playbackError.value = error.message ?: "Audio clip unavailable"
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                    updatePlaybackSnapshot()
                    if (isPlaying) {
                        startProgressUpdates()
                    } else {
                        progressJob?.cancel()
                        progressJob = null
                    }
                }
            })
        }
    }

    fun playFromFile(file: File) {
        if (exoPlayer == null) {
            initialize()
        }

        _playbackError.value = null

        val mediaItem = MediaItem.fromUri(Uri.fromFile(file))
        exoPlayer?.apply {
            stop()
            clearMediaItems()
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
        updatePlaybackSnapshot()
        startProgressUpdates()
    }

    fun playFromBytes(bytes: ByteArray, fileName: String = "temp.wav") {
        playRequestJob?.cancel()
        playRequestJob = scope.launch {
            runCatching {
                _playbackError.value = null
                val tempFile = withContext(Dispatchers.IO) {
                    val cacheDir = context.cacheDir
                    val filePrefix = fileName.substringBeforeLast('.').ifBlank { "audio" }
                    val temp = File.createTempFile("${filePrefix}_", ".wav", cacheDir)
                    temp.writeBytes(bytes)
                    temp
                }

                currentTempFile?.takeIf { it.exists() }?.delete()
                currentTempFile = tempFile

                playFromFile(tempFile)
            }.onFailure {
                _isPlaying.value = false
            }
        }
    }

    fun pause() {
        exoPlayer?.pause()
        _isPlaying.value = false
        updatePlaybackSnapshot()
    }

    fun resume() {
        exoPlayer?.play()
        _isPlaying.value = true
        updatePlaybackSnapshot()
        startProgressUpdates()
    }

    fun stop() {
        exoPlayer?.stop()
        _isPlaying.value = false
        _isBuffering.value = false
        _currentPosition.value = 0
        _duration.value = 0
        progressJob?.cancel()
        progressJob = null
        _playbackError.value = null
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _currentPosition.value = positionMs.coerceAtLeast(0L)
    }

    fun getCurrentPosition(): Long {
        return exoPlayer?.currentPosition ?: 0
    }

    fun getDuration(): Long {
        return exoPlayer?.duration ?: 0
    }

    fun release() {
        progressJob?.cancel()
        progressJob = null
        playRequestJob?.cancel()
        playRequestJob = null
        currentTempFile?.takeIf { it.exists() }?.delete()
        currentTempFile = null
        exoPlayer?.release()
        exoPlayer = null
        _isPlaying.value = false
        _isBuffering.value = false
        _playbackError.value = null
    }

    private fun startProgressUpdates() {
        if (progressJob?.isActive == true) return

        progressJob = scope.launch {
            while (isActive) {
                updatePlaybackSnapshot()
                delay(200L)
            }
        }
    }

    private fun updatePlaybackSnapshot() {
        val player = exoPlayer ?: return
        val currentDuration = player.duration.takeIf { it > 0 } ?: 0L
        _duration.value = currentDuration
        _currentPosition.value = player.currentPosition.coerceAtLeast(0L)
        _isPlaying.value = player.isPlaying
        _isBuffering.value = player.playbackState == Player.STATE_BUFFERING
    }
}
