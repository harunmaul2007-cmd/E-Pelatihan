package com.harunmaul.epelatihan.domain.usecase

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.core.common.AuthenticationException
import com.harunmaul.epelatihan.core.common.AuthorizationException
import com.harunmaul.epelatihan.core.common.InactiveAccountException
import com.harunmaul.epelatihan.domain.model.UserRole
import com.harunmaul.epelatihan.domain.repository.AuthRepository
import com.harunmaul.epelatihan.domain.repository.TrainingRepository
import javax.inject.Inject

class ArchiveTrainingUseCase @Inject constructor(
    private val trainingRepository: TrainingRepository,
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(trainingId: String): AppResult<Unit> {
        val profile = authRepository.getCurrentUserProfile()
            ?: return AppResult.Failure(AuthenticationException("Pengguna belum login."))
        if (!profile.active) {
            return AppResult.Failure(InactiveAccountException())
        }
        if (profile.role != UserRole.ADMIN) {
            return AppResult.Failure(AuthorizationException("Hanya Admin yang dapat mengarsipkan pelatihan."))
        }
        return trainingRepository.archiveTraining(actorId = profile.id, trainingId = trainingId)
    }
}
