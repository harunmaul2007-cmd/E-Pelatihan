package com.harunmaul.epelatihan.domain.usecase

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.core.common.AuthenticationException
import com.harunmaul.epelatihan.core.common.InactiveAccountException
import com.harunmaul.epelatihan.core.common.ValidationException
import com.harunmaul.epelatihan.domain.model.Training
import com.harunmaul.epelatihan.domain.model.TrainingLifecycle
import com.harunmaul.epelatihan.domain.repository.AuthRepository
import com.harunmaul.epelatihan.domain.repository.TrainingRepository
import com.harunmaul.epelatihan.domain.validation.TrainingValidator
import javax.inject.Inject

class CreateTrainingUseCase @Inject constructor(
    private val trainingRepository: TrainingRepository,
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(training: Training): AppResult<Training> {
        val profile = authRepository.getCurrentUserProfile()
            ?: return AppResult.Failure(AuthenticationException("Pengguna belum login."))
        if (!profile.active) {
            return AppResult.Failure(InactiveAccountException())
        }

        val validation = TrainingValidator.validate(training)
        if (validation is com.harunmaul.epelatihan.domain.validation.ValidationResult.Invalid) {
            val message = validation.issues.first().message
            return AppResult.Failure(ValidationException(message))
        }

        val trainingToCreate = training.copy(
            lifecycle = TrainingLifecycle.ACTIVE,
            createdBy = profile.id,
            updatedBy = profile.id,
        )

        return trainingRepository.createTraining(trainingToCreate)
    }
}
