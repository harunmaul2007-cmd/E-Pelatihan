package com.harunmaul.epelatihan.domain.usecase

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.domain.model.TrainingDetailSnapshot
import com.harunmaul.epelatihan.domain.repository.TrainingRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveTrainingDetailUseCase @Inject constructor(
    private val trainingRepository: TrainingRepository,
) {
    operator fun invoke(trainingId: String): Flow<AppResult<TrainingDetailSnapshot>> {
        return trainingRepository.observeTraining(trainingId)
    }
}
