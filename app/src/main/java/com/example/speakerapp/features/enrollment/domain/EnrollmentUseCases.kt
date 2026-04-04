package com.example.speakerapp.features.enrollment.domain

import com.example.speakerapp.features.enrollment.data.EnrollmentRepository
import java.io.File
import javax.inject.Inject

class EnrollSpeakerUseCase @Inject constructor(private val repo: EnrollmentRepository) {
    suspend operator fun invoke(displayName: String, audioFile: File, speakerId: String? = null) =
    repo.enrollSpeaker(displayName, audioFile, speakerId)
}

class ListSpeakersUseCase @Inject constructor(private val repo: EnrollmentRepository) {
    suspend operator fun invoke() = repo.listSpeakers()
}

class UpdateSpeakerUseCase @Inject constructor(private val repo: EnrollmentRepository) {
    suspend operator fun invoke(speakerId: String, displayName: String) =
    repo.updateSpeaker(speakerId, displayName)
}

class DeleteSpeakerUseCase @Inject constructor(private val repo: EnrollmentRepository) {
    suspend operator fun invoke(speakerId: String) = repo.deleteSpeaker(speakerId)
}
