package com.example.speakerapp.features.enrollment.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.speakerapp.core.audio.AudioRecorder
import com.example.speakerapp.features.enrollment.data.EnrollmentRepository
import com.example.speakerapp.features.enrollment.data.EnrollmentResult
import com.example.speakerapp.features.enrollment.data.EnrolledSpeaker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class SpeakerEnrollmentUiState(
    val isLoading: Boolean = false,
    val isRecording: Boolean = false,
    val recordingRemainingMs: Long = 0L,
    val enrolledSpeaker: EnrolledSpeaker? = null,
    val error: String? = null,
    val voicedMs: Double? = null,
    val numSegments: Int? = null,
    val speechQualityPassed: Boolean? = null
)

@HiltViewModel
class SpeakerEnrollmentViewModel @Inject constructor(
    private val enrollmentRepository: EnrollmentRepository,
    private val audioRecorder: AudioRecorder
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpeakerEnrollmentUiState())
    val uiState: StateFlow<SpeakerEnrollmentUiState> = _uiState.asStateFlow()

    fun recordAndEnroll(displayName: String, durationMs: Long = 5000L, speakerId: String? = null) {
        viewModelScope.launch {
            if (!audioRecorder.initialize()) {
                _uiState.value = _uiState.value.copy(
                    isRecording = false,
                    recordingRemainingMs = 0L,
                    error = "Failed to initialize recorder"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                isRecording = true,
                recordingRemainingMs = durationMs,
                error = null
            )

            val countdownJob = launch {
                val startTime = System.currentTimeMillis()
                while (isActive) {
                    val elapsed = System.currentTimeMillis() - startTime
                    val remaining = (durationMs - elapsed).coerceAtLeast(0L)

                    _uiState.value = _uiState.value.copy(
                        isRecording = remaining > 0L,
                        recordingRemainingMs = remaining
                    )

                    if (remaining == 0L) break
                    delay(100L)
                }
            }

            val tempFile = File.createTempFile("enroll_record_", ".wav")
            val recordedFile = audioRecorder.recordAudio(durationMs, tempFile)
            countdownJob.cancel()

            _uiState.value = _uiState.value.copy(
                isRecording = false,
                recordingRemainingMs = 0L
            )

            if (recordedFile == null || !recordedFile.exists()) {
                _uiState.value = _uiState.value.copy(error = "Voice recording failed")
                audioRecorder.release()
                return@launch
            }

            enrollSpeaker(displayName = displayName, audioFile = recordedFile, speakerId = speakerId)
            audioRecorder.release()
        }
    }

    fun enrollSpeaker(displayName: String, audioFile: File, speakerId: String? = null) {
        viewModelScope.launch {
            enrollmentRepository.enrollSpeaker(
                displayName = displayName,
                audioFile = audioFile,
                speakerId = speakerId
            ).collect { result ->
                when (result) {
                    is EnrollmentResult.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                    }
                    is EnrollmentResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            enrolledSpeaker = result.data,
                            voicedMs = result.data.voicedMs,
                            numSegments = result.data.numSegments,
                            speechQualityPassed = result.data.speechQualityPassed,
                            error = null
                        )
                    }
                    is EnrollmentResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
