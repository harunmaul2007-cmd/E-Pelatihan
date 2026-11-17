package com.harunmaul.epelatihan.domain.usecase

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.core.common.AuthorizationException
import com.harunmaul.epelatihan.core.common.ValidationException
import com.harunmaul.epelatihan.domain.model.UserProfile
import com.harunmaul.epelatihan.domain.model.UserRole
import com.harunmaul.epelatihan.domain.repository.UserRepository
import com.harunmaul.epelatihan.domain.validation.AuthValidator
import com.harunmaul.epelatihan.domain.validation.ValidationResult
import javax.inject.Inject

class InviteUserUseCase @Inject constructor(
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(
        actorId: String,
        name: String,
        email: String,
        role: UserRole,
    ): AppResult<UserProfile> {
        val actor = userRepository.getUserProfile(actorId)
        if (actor == null || !actor.active || actor.role != UserRole.ADMIN) {
            return AppResult.Failure(
                AuthorizationException("Hanya Admin aktif yang memiliki hak untuk mengundang pengguna.")
            )
        }

        val nameValidation = AuthValidator.validateDisplayName(name)
        if (nameValidation is ValidationResult.Invalid) {
            return AppResult.Failure(ValidationException(nameValidation.issues.first().message))
        }

        val emailValidation = AuthValidator.validateEmail(email)
        if (emailValidation is ValidationResult.Invalid) {
            return AppResult.Failure(ValidationException(emailValidation.issues.first().message))
        }

        return userRepository.inviteUser(actorId, name.trim(), email.trim(), role)
    }
}
