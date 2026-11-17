package com.harunmaul.epelatihan.domain.usecase

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.core.common.ValidationException
import com.harunmaul.epelatihan.domain.model.UserProfile
import com.harunmaul.epelatihan.domain.repository.UserRepository
import com.harunmaul.epelatihan.domain.validation.AuthValidator
import com.harunmaul.epelatihan.domain.validation.ValidationResult
import javax.inject.Inject

class BootstrapFirstAdminUseCase @Inject constructor(
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(
        name: String,
        email: String,
        password: String,
    ): AppResult<UserProfile> {
        val nameValidation = AuthValidator.validateDisplayName(name)
        if (nameValidation is ValidationResult.Invalid) {
            return AppResult.Failure(ValidationException(nameValidation.issues.first().message))
        }

        val emailValidation = AuthValidator.validateEmail(email)
        if (emailValidation is ValidationResult.Invalid) {
            return AppResult.Failure(ValidationException(emailValidation.issues.first().message))
        }

        val passwordValidation = AuthValidator.validatePassword(password)
        if (passwordValidation is ValidationResult.Invalid) {
            return AppResult.Failure(ValidationException(passwordValidation.issues.first().message))
        }

        if (userRepository.hasAnyAdmin()) {
            return AppResult.Failure(
                IllegalStateException("Admin pertama sudah terdaftar. Mekanisme bootstrap tidak lagi tersedia.")
            )
        }

        return userRepository.bootstrapFirstAdmin(name.trim(), email.trim(), password)
    }
}
