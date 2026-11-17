package com.harunmaul.epelatihan.domain.repository

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.domain.model.Training
import com.harunmaul.epelatihan.domain.model.TrainingDetailSnapshot
import com.harunmaul.epelatihan.domain.model.TrainingListSnapshot
import kotlinx.coroutines.flow.Flow

interface TrainingRepository {
    fun observeTrainings(includeArchived: Boolean = false): Flow<AppResult<TrainingListSnapshot>>
    fun observeTraining(trainingId: String): Flow<AppResult<TrainingDetailSnapshot>>
    suspend fun getTraining(trainingId: String): AppResult<Training?>
    suspend fun createTraining(training: Training): AppResult<Training>
    suspend fun updateTraining(actorId: String, training: Training): AppResult<Training>
    suspend fun archiveTraining(actorId: String, trainingId: String): AppResult<Unit>
}
