package com.harunmaul.epelatihan.domain.usecase

import com.harunmaul.epelatihan.domain.repository.AuthRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke() {
        authRepository.logout()
    }
}
