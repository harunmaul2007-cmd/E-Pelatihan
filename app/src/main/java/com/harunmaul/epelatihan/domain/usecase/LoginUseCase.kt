package com.harunmaul.epelatihan.domain.usecase

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.core.common.ValidationException
import com.harunmaul.epelatihan.domain.model.UserProfile
import com.harunmaul.epelatihan.domain.repository.AuthRepository
import com.harunmaul.epelatihan.domain.validation.AuthValidator
import com.harunmaul.epelatihan.domain.validation.ValidationResult
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String, password: String): AppResult<UserProfile> {
        val validation = AuthValidator.validateLogin(email, password)
        if (validation is ValidationResult.Invalid) {
            val firstIssue = validation.issues.first()
            return AppResult.Failure(ValidationException(firstIssue.message))
        }
        return authRepository.login(email.trim(), password)
    }
}
