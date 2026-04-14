package com.example.speakerapp.features.enrollment.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.speakerapp.features.enrollment.data.EnrollmentRepository
import com.example.speakerapp.features.enrollment.data.EnrollmentResult
import com.example.speakerapp.features.enrollment.data.SpeakerListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class SpeakerListUiState(
    val isLoading: Boolean = false,
    val speakers: List<SpeakerListItem> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class SpeakerListViewModel @Inject constructor(
    private val enrollmentRepository: EnrollmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpeakerListUiState())
    val uiState: StateFlow<SpeakerListUiState> = _uiState.asStateFlow()
    private var cacheInitialized = false

    init {
        observeSpeakerCache()
        loadSpeakers()
    }

    private fun observeSpeakerCache() {
        viewModelScope.launch {
            enrollmentRepository.speakerCache.collectLatest { cached ->
                if (!cacheInitialized) {
                    cacheInitialized = true
                    if (cached.isEmpty()) return@collectLatest
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    speakers = cached,
                    error = null
                )
            }
        }
    }

    fun loadSpeakers() {
        viewModelScope.launch {
            enrollmentRepository.listSpeakers().collect { result ->
                when (result) {
                    is EnrollmentResult.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                    }
                    is EnrollmentResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            speakers = result.data,
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

    fun updateSpeaker(speakerId: String, newDisplayName: String) {
        viewModelScope.launch {
            enrollmentRepository.updateSpeaker(speakerId, newDisplayName).collect { result ->
                when (result) {
                    is EnrollmentResult.Success -> loadSpeakers()
                    is EnrollmentResult.Error -> _uiState.value = _uiState.value.copy(error = result.message)
                    else -> {}
                }
            }
        }
    }

    fun deleteSpeaker(speakerId: String) {
        viewModelScope.launch {
            enrollmentRepository.deleteSpeaker(speakerId).collect { result ->
                when (result) {
                    is EnrollmentResult.Success -> loadSpeakers()
                    is EnrollmentResult.Error -> _uiState.value = _uiState.value.copy(error = result.message)
                    else -> {}
                }
            }
        }
    }

    fun updateSpeakerAvatar(speakerId: String, imageFile: File) {
        viewModelScope.launch {
            enrollmentRepository.updateSpeakerAvatar(speakerId, imageFile).collect { result ->
                when (result) {
                    is EnrollmentResult.Success -> loadSpeakers()
                    is EnrollmentResult.Error -> _uiState.value = _uiState.value.copy(error = result.message)
                    else -> {}
                }
            }
        }
    }

    fun setError(message: String) {
        _uiState.value = _uiState.value.copy(error = message)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
