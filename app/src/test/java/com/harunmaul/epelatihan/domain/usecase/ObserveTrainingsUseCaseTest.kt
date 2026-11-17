package com.harunmaul.epelatihan.domain.usecase

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.domain.model.MeetingMethod
import com.harunmaul.epelatihan.domain.model.PricingScheme
import com.harunmaul.epelatihan.domain.model.Training
import com.harunmaul.epelatihan.domain.model.TrainingDetailSnapshot
import com.harunmaul.epelatihan.domain.model.TrainingLifecycle
import com.harunmaul.epelatihan.domain.model.TrainingListSnapshot
import com.harunmaul.epelatihan.domain.repository.TrainingRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ObserveTrainingsUseCaseTest {

    private class FakeTrainingRepository : TrainingRepository {
        var trainingsResult: AppResult<TrainingListSnapshot> = AppResult.Success(
            TrainingListSnapshot(
                trainings = listOf(
                    Training(
                        id = "training-1",
                        name = "Pelatihan Android Jetpack Compose",
                        description = "Deskripsi pelatihan Compose",
                        startAt = Instant.parse("2026-11-20T09:00:00Z"),
                        endAt = Instant.parse("2026-11-20T12:00:00Z"),
                        timeZoneId = "Asia/Jakarta",
                        speaker = "Budi Santoso",
                        meetingMethod = MeetingMethod.ONLINE,
                        zoomUrl = "https://zoom.us/j/123456",
                        location = null,
                        recordingUrl = null,
                        materialUrl = "https://example.com/material.pdf",
                        pricingScheme = PricingScheme.NORMAL_ONLY,
                        normalPrice = 350000,
                        earlyBirdPrice = null,
                        lifecycle = TrainingLifecycle.ACTIVE,
                        createdBy = "admin-1",
                        createdAt = Instant.now(),
                        updatedBy = "admin-1",
                        updatedAt = Instant.now(),
                    )
                ),
                isFromCache = false,
            )
        )

        override fun observeTrainings(includeArchived: Boolean): Flow<AppResult<TrainingListSnapshot>> =
            flowOf(trainingsResult)

        override fun observeTraining(trainingId: String): Flow<AppResult<TrainingDetailSnapshot>> =
            flowOf(AppResult.Success(TrainingDetailSnapshot(null, false)))

        override suspend fun getTraining(trainingId: String): AppResult<Training?> =
            AppResult.Success(null)

        override suspend fun createTraining(training: Training): AppResult<Training> =
            AppResult.Success(training)

        override suspend fun updateTraining(actorId: String, training: Training): AppResult<Training> =
            AppResult.Success(training)

        override suspend fun archiveTraining(actorId: String, trainingId: String): AppResult<Unit> =
            AppResult.Success(Unit)
    }

    @Test
    fun `invoke emits success trainings list`() = runTest {
        val repo = FakeTrainingRepository()
        val useCase = ObserveTrainingsUseCase(repo)

        val result = useCase().first()
        assertTrue(result is AppResult.Success)
        val snapshot = (result as AppResult.Success).value
        assertEquals(1, snapshot.trainings.size)
        assertEquals("Pelatihan Android Jetpack Compose", snapshot.trainings[0].name)
    }

    @Test
    fun `invoke emits failure when repo fails`() = runTest {
        val repo = FakeTrainingRepository().apply {
            trainingsResult = AppResult.Failure(RuntimeException("Koneksi gagal"))
        }
        val useCase = ObserveTrainingsUseCase(repo)

        val result = useCase().first()
        assertTrue(result is AppResult.Failure)
        assertEquals("Koneksi gagal", (result as AppResult.Failure).cause.message)
    }
}
