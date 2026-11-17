package com.harunmaul.epelatihan.domain.usecase

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.core.common.AuthorizationException
import com.harunmaul.epelatihan.core.common.ValidationException
import com.harunmaul.epelatihan.domain.model.UserRole
import com.harunmaul.epelatihan.domain.repository.UserRepository
import javax.inject.Inject

class UpdateUserRoleUseCase @Inject constructor(
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(
        actorId: String,
        userId: String,
        newRole: UserRole,
    ): AppResult<Unit> {
        val actor = userRepository.getUserProfile(actorId)
        if (actor == null || !actor.active || actor.role != UserRole.ADMIN) {
            return AppResult.Failure(
                AuthorizationException("Hanya Admin aktif yang memiliki hak untuk mengubah peran pengguna.")
            )
        }

        if (actorId == userId && newRole != UserRole.ADMIN) {
            return AppResult.Failure(
                ValidationException("Anda tidak dapat mencabut peran Admin dari akun Anda sendiri.")
            )
        }

        return userRepository.updateUserRole(actorId, userId, newRole)
    }
}
