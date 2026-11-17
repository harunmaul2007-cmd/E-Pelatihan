package com.harunmaul.epelatihan.domain.usecase

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.domain.model.TrainingListSnapshot
import com.harunmaul.epelatihan.domain.repository.TrainingRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveTrainingsUseCase @Inject constructor(
    private val trainingRepository: TrainingRepository,
) {
    operator fun invoke(includeArchived: Boolean = false): Flow<AppResult<TrainingListSnapshot>> {
        return trainingRepository.observeTrainings(includeArchived)
    }
}
