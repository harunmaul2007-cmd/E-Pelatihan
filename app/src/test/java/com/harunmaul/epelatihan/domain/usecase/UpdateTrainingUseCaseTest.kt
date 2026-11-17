package com.harunmaul.epelatihan.domain.usecase

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.core.common.AuthorizationException
import com.harunmaul.epelatihan.core.common.ValidationException
import com.harunmaul.epelatihan.domain.model.InvitationStatus
import com.harunmaul.epelatihan.domain.model.MeetingMethod
import com.harunmaul.epelatihan.domain.model.PricingScheme
import com.harunmaul.epelatihan.domain.model.Training
import com.harunmaul.epelatihan.domain.model.TrainingDetailSnapshot
import com.harunmaul.epelatihan.domain.model.TrainingLifecycle
import com.harunmaul.epelatihan.domain.model.TrainingListSnapshot
import com.harunmaul.epelatihan.domain.model.UserProfile
import com.harunmaul.epelatihan.domain.model.UserRole
import com.harunmaul.epelatihan.domain.model.UserSession
import com.harunmaul.epelatihan.domain.repository.AuthRepository
import com.harunmaul.epelatihan.domain.repository.TrainingRepository
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateTrainingUseCaseTest {

    private class FakeAuthRepository(var currentProfile: UserProfile?) : AuthRepository {
        override fun observeSession(): Flow<UserSession> = flowOf(
            currentProfile?.let { UserSession.Authenticated(it) } ?: UserSession.Unauthenticated
        )
        override suspend fun login(email: String, password: String): AppResult<UserProfile> = error("Not needed")
        override suspend fun logout() {}
        override suspend fun sendPasswordReset(email: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun getCurrentUserProfile(): UserProfile? = currentProfile
    }

    private class FakeTrainingRepository : TrainingRepository {
        var updatedTraining: Training? = null

        override fun observeTrainings(includeArchived: Boolean): Flow<AppResult<TrainingListSnapshot>> =
            flowOf(AppResult.Success(TrainingListSnapshot(emptyList(), false)))

        override fun observeTraining(trainingId: String): Flow<AppResult<TrainingDetailSnapshot>> =
            flowOf(AppResult.Success(TrainingDetailSnapshot(null, false)))

        override suspend fun getTraining(trainingId: String): AppResult<Training?> =
            AppResult.Success(null)

        override suspend fun createTraining(training: Training): AppResult<Training> =
            AppResult.Success(training)

        override suspend fun updateTraining(actorId: String, training: Training): AppResult<Training> {
            updatedTraining = training
            return AppResult.Success(training)
        }

        override suspend fun archiveTraining(actorId: String, trainingId: String): AppResult<Unit> =
            AppResult.Success(Unit)
    }

    private val adminProfile = UserProfile(
        id = "admin-1",
        displayName = "Admin Lembaga",
        email = "admin@lembaga.org",
        role = UserRole.ADMIN,
        active = true,
        invitationStatus = InvitationStatus.ACTIVE,
        createdBy = "admin-1",
        createdAt = Instant.now(),
        updatedBy = "admin-1",
        updatedAt = Instant.now(),
    )

    private val userProfile = UserProfile(
        id = "user-1",
        displayName = "User Biasa",
        email = "user@lembaga.org",
        role = UserRole.USER,
        active = true,
        invitationStatus = InvitationStatus.ACTIVE,
        createdBy = "admin-1",
        createdAt = Instant.now(),
        updatedBy = "admin-1",
        updatedAt = Instant.now(),
    )

    private val existingTraining = Training(
        id = "tr-100",
        name = "Workshop Android Jetpack",
        description = "Membahas Compose",
        startAt = Instant.now().plus(1, ChronoUnit.DAYS),
        endAt = Instant.now().plus(1, ChronoUnit.DAYS).plus(3, ChronoUnit.HOURS),
        timeZoneId = "Asia/Jakarta",
        speaker = "Ahmad Dani",
        meetingMethod = MeetingMethod.ONLINE,
        zoomUrl = "https://zoom.us/j/987654321",
        location = null,
        recordingUrl = null,
        materialUrl = null,
        pricingScheme = PricingScheme.NORMAL_ONLY,
        normalPrice = 250000,
        earlyBirdPrice = null,
        lifecycle = TrainingLifecycle.ACTIVE,
        createdBy = "user-1",
        createdAt = Instant.now(),
        updatedBy = "user-1",
        updatedAt = Instant.now(),
    )

    @Test
    fun `admin can update training successfully`() = runTest {
        val authRepo = FakeAuthRepository(adminProfile)
        val trainingRepo = FakeTrainingRepository()
        val useCase = UpdateTrainingUseCase(trainingRepo, authRepo)

        val updated = existingTraining.copy(name = "Workshop Android Modern")
        val result = useCase(updated)
        assertTrue(result is AppResult.Success)
        assertEquals("admin-1", trainingRepo.updatedTraining?.updatedBy)
        assertEquals("Workshop Android Modern", trainingRepo.updatedTraining?.name)
    }

    @Test
    fun `user role cannot update training`() = runTest {
        val authRepo = FakeAuthRepository(userProfile)
        val trainingRepo = FakeTrainingRepository()
        val useCase = UpdateTrainingUseCase(trainingRepo, authRepo)

        val updated = existingTraining.copy(name = "Ganti Judul")
        val result = useCase(updated)
        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).cause is AuthorizationException)
    }

    @Test
    fun `invalid early bird price returns validation failure`() = runTest {
        val authRepo = FakeAuthRepository(adminProfile)
        val trainingRepo = FakeTrainingRepository()
        val useCase = UpdateTrainingUseCase(trainingRepo, authRepo)

        val invalidPricing = existingTraining.copy(
            pricingScheme = PricingScheme.EARLY_BIRD_AND_NORMAL,
            normalPrice = 200000,
            earlyBirdPrice = 250000, // early bird larger than normal!
        )
        val result = useCase(invalidPricing)
        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).cause is ValidationException)
    }
}
