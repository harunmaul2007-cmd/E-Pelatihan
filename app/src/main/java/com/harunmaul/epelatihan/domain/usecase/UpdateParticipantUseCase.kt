package com.harunmaul.epelatihan.domain.usecase

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.core.common.AuthorizationException
import com.harunmaul.epelatihan.core.common.InactiveAccountException
import com.harunmaul.epelatihan.core.common.ValidationException
import com.harunmaul.epelatihan.domain.model.Participant
import com.harunmaul.epelatihan.domain.model.UserRole
import com.harunmaul.epelatihan.domain.model.UserSession
import com.harunmaul.epelatihan.domain.repository.AuthRepository
import com.harunmaul.epelatihan.domain.repository.ParticipantRepository
import com.harunmaul.epelatihan.domain.validation.ParticipantValidator
import com.harunmaul.epelatihan.domain.validation.PhoneNormalizationResult
import com.harunmaul.epelatihan.domain.validation.PhoneNumberNormalizer
import com.harunmaul.epelatihan.domain.validation.ValidationResult
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class UpdateParticipantUseCase @Inject constructor(
    private val participantRepository: ParticipantRepository,
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(
        participantId: String,
        name: String,
        phoneRaw: String,
        email: String? = null,
        notes: String? = null,
    ): AppResult<Participant> {
        val session = authRepository.observeSession().first()
        val authenticated = session as? UserSession.Authenticated
            ?: return AppResult.Failure(AuthorizationException("Pengguna belum login."))

        if (!authenticated.profile.active) {
            return AppResult.Failure(InactiveAccountException())
        }

        if (authenticated.profile.role != UserRole.ADMIN) {
            return AppResult.Failure(AuthorizationException("Hanya Admin yang dapat mengubah data peserta."))
        }

        val normResult = PhoneNumberNormalizer.normalize(phoneRaw)
        if (normResult is PhoneNormalizationResult.Failure) {
            return AppResult.Failure(ValidationException(normResult.message))
        }
        val phoneNormalized = (normResult as PhoneNormalizationResult.Success).normalized

        val participant = Participant(
            id = participantId,
            name = name.trim(),
            nameSort = name.trim().lowercase(),
            phoneDisplay = phoneRaw.trim(),
            phoneNormalized = phoneNormalized,
            email = email?.trim()?.ifBlank { null },
            notes = notes?.trim()?.ifBlank { null },
            active = true,
            createdBy = "",
            createdAt = null,
            updatedBy = authenticated.profile.id,
            updatedAt = null,
        )

        val validation = ParticipantValidator.validate(participant)
        if (validation is ValidationResult.Invalid) {
            return AppResult.Failure(ValidationException(validation.issues.first().message))
        }

        return participantRepository.updateParticipant(authenticated.profile.id, participant)
    }
}
