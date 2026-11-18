package com.harunmaul.epelatihan.domain.usecase

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.core.common.AuthorizationException
import com.harunmaul.epelatihan.core.common.InactiveAccountException
import com.harunmaul.epelatihan.core.common.ValidationException
import com.harunmaul.epelatihan.domain.model.PricingType
import com.harunmaul.epelatihan.domain.model.Registration
import com.harunmaul.epelatihan.domain.model.Training
import com.harunmaul.epelatihan.domain.model.UserSession
import com.harunmaul.epelatihan.domain.repository.AuthRepository
import com.harunmaul.epelatihan.domain.repository.RegistrationRepository
import com.harunmaul.epelatihan.domain.validation.RegistrationCreationResult
import com.harunmaul.epelatihan.domain.validation.RegistrationFactory
import javax.inject.Inject
import kotlinx.coroutines.flow.first

data class NewParticipantInput(
    val name: String,
    val phoneRaw: String,
    val email: String? = null,
    val notes: String? = null,
)

class RegisterParticipantUseCase @Inject constructor(
    private val registrationRepository: RegistrationRepository,
    private val createParticipantUseCase: CreateParticipantUseCase,
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(
        training: Training,
        participantId: String? = null,
        newParticipantInput: NewParticipantInput? = null,
        selectedPricingType: PricingType? = null,
    ): AppResult<Registration> {
        val session = authRepository.observeSession().first()
        val authenticated = session as? UserSession.Authenticated
            ?: return AppResult.Failure(AuthorizationException("Pengguna belum login."))

        if (!authenticated.profile.active) {
            return AppResult.Failure(InactiveAccountException())
        }

        val resolvedParticipantId = if (!participantId.isNullOrBlank()) {
            participantId
        } else if (newParticipantInput != null) {
            when (val createResult = createParticipantUseCase(
                name = newParticipantInput.name,
                phoneRaw = newParticipantInput.phoneRaw,
                email = newParticipantInput.email,
                notes = newParticipantInput.notes,
            )) {
                is AppResult.Success -> createResult.value.id
                is AppResult.Failure -> return AppResult.Failure(createResult.cause)
            }
        } else {
            return AppResult.Failure(ValidationException("Peserta wajib dipilih atau diisi."))
        }

        val creationResult = RegistrationFactory.create(
            training = training,
            participantId = resolvedParticipantId,
            selectedPricingType = selectedPricingType,
            actorId = authenticated.profile.id,
        )

        val registration = when (creationResult) {
            is RegistrationCreationResult.Rejected -> {
                return AppResult.Failure(ValidationException(creationResult.issues.first().message))
            }
            is RegistrationCreationResult.Created -> creationResult.registration
        }

        return registrationRepository.createRegistration(registration)
    }
}
