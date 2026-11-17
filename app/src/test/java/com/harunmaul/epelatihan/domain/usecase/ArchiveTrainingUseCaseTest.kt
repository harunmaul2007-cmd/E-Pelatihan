package com.harunmaul.epelatihan.domain.usecase

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.core.common.AuthorizationException
import com.harunmaul.epelatihan.domain.model.InvitationStatus
import com.harunmaul.epelatihan.domain.model.Training
import com.harunmaul.epelatihan.domain.model.TrainingDetailSnapshot
import com.harunmaul.epelatihan.domain.model.TrainingListSnapshot
import com.harunmaul.epelatihan.domain.model.UserProfile
import com.harunmaul.epelatihan.domain.model.UserRole
import com.harunmaul.epelatihan.domain.model.UserSession
import com.harunmaul.epelatihan.domain.repository.AuthRepository
import com.harunmaul.epelatihan.domain.repository.TrainingRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchiveTrainingUseCaseTest {

    private class FakeAuthRepository(var currentProfile: UserProfile? = null) : AuthRepository {
        override fun observeSession(): Flow<UserSession> = flowOf(
            currentProfile?.let { UserSession.Authenticated(it) } ?: UserSession.Unauthenticated
        )
        override suspend fun login(email: String, password: String): AppResult<UserProfile> =
            currentProfile?.let { AppResult.Success(it) } ?: AppResult.Failure(RuntimeException("Error"))
        override suspend fun logout() {}
        override suspend fun sendPasswordReset(email: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun getCurrentUserProfile(): UserProfile? = currentProfile
    }

    private class FakeTrainingRepository : TrainingRepository {
        var archivedTrainingId: String? = null

        override fun observeTrainings(includeArchived: Boolean): Flow<AppResult<TrainingListSnapshot>> =
            flowOf(AppResult.Success(TrainingListSnapshot(emptyList(), false)))

        override fun observeTraining(trainingId: String): Flow<AppResult<TrainingDetailSnapshot>> =
            flowOf(AppResult.Success(TrainingDetailSnapshot(null, false)))

        override suspend fun getTraining(trainingId: String): AppResult<Training?> =
            AppResult.Success(null)

        override suspend fun createTraining(training: Training): AppResult<Training> =
            AppResult.Success(training)

        override suspend fun updateTraining(actorId: String, training: Training): AppResult<Training> =
            AppResult.Success(training)

        override suspend fun archiveTraining(actorId: String, trainingId: String): AppResult<Unit> {
            archivedTrainingId = trainingId
            return AppResult.Success(Unit)
        }
    }

    @Test
    fun `invoke fails when user is not logged in`() = runTest {
        val authRepo = FakeAuthRepository(currentProfile = null)
        val trainingRepo = FakeTrainingRepository()
        val useCase = ArchiveTrainingUseCase(trainingRepo, authRepo)

        val result = useCase("tr-1")
        assertTrue(result is AppResult.Failure)
    }

    @Test
    fun `invoke fails when user is not admin`() = runTest {
        val authRepo = FakeAuthRepository(
            currentProfile = UserProfile(
                id = "user-1",
                displayName = "User Biasa",
                email = "user@test.org",
                role = UserRole.USER,
                active = true,
                invitationStatus = InvitationStatus.ACTIVE,
                createdBy = "admin-1",
                createdAt = Instant.now(),
                updatedBy = "admin-1",
                updatedAt = Instant.now(),
            )
        )
        val trainingRepo = FakeTrainingRepository()
        val useCase = ArchiveTrainingUseCase(trainingRepo, authRepo)

        val result = useCase("tr-1")
        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).cause is AuthorizationException)
    }

    @Test
    fun `invoke succeeds when user is active admin`() = runTest {
        val authRepo = FakeAuthRepository(
            currentProfile = UserProfile(
                id = "admin-1",
                displayName = "Admin",
                email = "admin@test.org",
                role = UserRole.ADMIN,
                active = true,
                invitationStatus = InvitationStatus.ACTIVE,
                createdBy = "admin-1",
                createdAt = Instant.now(),
                updatedBy = "admin-1",
                updatedAt = Instant.now(),
            )
        )
        val trainingRepo = FakeTrainingRepository()
        val useCase = ArchiveTrainingUseCase(trainingRepo, authRepo)

        val result = useCase("tr-123")
        assertTrue(result is AppResult.Success)
        assertEquals("tr-123", trainingRepo.archivedTrainingId)
    }
}
