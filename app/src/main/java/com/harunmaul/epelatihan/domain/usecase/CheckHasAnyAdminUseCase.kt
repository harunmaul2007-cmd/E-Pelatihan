package com.harunmaul.epelatihan.domain.usecase

import com.harunmaul.epelatihan.domain.repository.UserRepository
import javax.inject.Inject

class CheckHasAnyAdminUseCase @Inject constructor(
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(): Boolean = userRepository.hasAnyAdmin()
}
