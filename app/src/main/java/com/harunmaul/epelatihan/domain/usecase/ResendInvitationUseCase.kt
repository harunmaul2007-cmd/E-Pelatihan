package com.harunmaul.epelatihan.domain.usecase

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.core.common.AuthorizationException
import com.harunmaul.epelatihan.core.common.ValidationException
import com.harunmaul.epelatihan.domain.model.UserRole
import com.harunmaul.epelatihan.domain.repository.UserRepository
import com.harunmaul.epelatihan.domain.validation.AuthValidator
import com.harunmaul.epelatihan.domain.validation.ValidationResult
import javax.inject.Inject

class ResendInvitationUseCase @Inject constructor(
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(
        actorId: String,
        userId: String,
        email: String,
    ): AppResult<Unit> {
        val actor = userRepository.getUserProfile(actorId)
        if (actor == null || !actor.active || actor.role != UserRole.ADMIN) {
            return AppResult.Failure(
                AuthorizationException("Hanya Admin aktif yang memiliki hak untuk mengirim ulang undangan.")
            )
        }

        val emailValidation = AuthValidator.validateEmail(email)
        if (emailValidation is ValidationResult.Invalid) {
            return AppResult.Failure(ValidationException(emailValidation.issues.first().message))
        }

        return userRepository.resendInvitation(actorId, userId, email.trim())
    }
}
