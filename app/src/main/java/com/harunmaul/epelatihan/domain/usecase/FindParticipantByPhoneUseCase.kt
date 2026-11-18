package com.harunmaul.epelatihan.domain.usecase

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.core.common.ValidationException
import com.harunmaul.epelatihan.domain.model.Participant
import com.harunmaul.epelatihan.domain.repository.ParticipantRepository
import com.harunmaul.epelatihan.domain.validation.PhoneNormalizationResult
import com.harunmaul.epelatihan.domain.validation.PhoneNumberNormalizer
import javax.inject.Inject

class FindParticipantByPhoneUseCase @Inject constructor(
    private val participantRepository: ParticipantRepository,
) {
    suspend operator fun invoke(rawPhone: String): AppResult<Participant?> {
        val normResult = PhoneNumberNormalizer.normalize(rawPhone)
        if (normResult is PhoneNormalizationResult.Failure) {
            return AppResult.Failure(ValidationException(normResult.message))
        }
        val phoneNormalized = (normResult as PhoneNormalizationResult.Success).normalized
        return participantRepository.findParticipantByPhone(phoneNormalized)
    }
}
