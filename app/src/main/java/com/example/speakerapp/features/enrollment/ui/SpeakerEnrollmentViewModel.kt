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
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class SpeakerEnrollmentUiState(
    val isLoading: Boolean = false,
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

    fun recordAndEnroll(displayName: String, durationMs: Long = 5000L) {
        viewModelScope.launch {
            if (!audioRecorder.initialize()) {
                _uiState.value = _uiState.value.copy(error = "Failed to initialize recorder")
                return@launch
            }

            val tempFile = File.createTempFile("enroll_record_", ".wav")
            val recordedFile = audioRecorder.recordAudio(durationMs, tempFile)

            if (recordedFile == null || !recordedFile.exists()) {
                _uiState.value = _uiState.value.copy(error = "Voice recording failed")
                audioRecorder.release()
                return@launch
            }

            enrollSpeaker(displayName = displayName, audioFile = recordedFile)
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
