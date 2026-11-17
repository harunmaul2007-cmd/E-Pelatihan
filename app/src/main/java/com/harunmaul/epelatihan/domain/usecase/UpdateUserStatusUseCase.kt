package com.harunmaul.epelatihan.domain.usecase

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.core.common.AuthorizationException
import com.harunmaul.epelatihan.core.common.ValidationException
import com.harunmaul.epelatihan.domain.model.UserRole
import com.harunmaul.epelatihan.domain.repository.UserRepository
import javax.inject.Inject

class UpdateUserStatusUseCase @Inject constructor(
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(
        actorId: String,
        userId: String,
        active: Boolean,
    ): AppResult<Unit> {
        val actor = userRepository.getUserProfile(actorId)
        if (actor == null || !actor.active || actor.role != UserRole.ADMIN) {
            return AppResult.Failure(
                AuthorizationException("Hanya Admin aktif yang memiliki hak untuk mengubah status pengguna.")
            )
        }

        if (actorId == userId && !active) {
            return AppResult.Failure(
                ValidationException("Anda tidak dapat menonaktifkan akun Anda sendiri.")
            )
        }

        return userRepository.updateUserStatus(actorId, userId, active)
    }
}
