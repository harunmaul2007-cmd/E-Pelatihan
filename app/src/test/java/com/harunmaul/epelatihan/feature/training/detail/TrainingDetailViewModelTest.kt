package com.harunmaul.epelatihan.feature.training.detail

import androidx.lifecycle.SavedStateHandle
import com.harunmaul.epelatihan.core.common.AppResult
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
import com.harunmaul.epelatihan.domain.usecase.ArchiveTrainingUseCase
import com.harunmaul.epelatihan.domain.usecase.ObserveSessionUseCase
import com.harunmaul.epelatihan.domain.usecase.ObserveTrainingDetailUseCase
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TrainingDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val sampleTraining = Training(
        id = "t-100",
        name = "Pelatihan Android Jetpack Compose",
        description = "Deskripsi",
        startAt = Instant.now(),
        endAt = Instant.now().plusSeconds(3600),
        timeZoneId = "Asia/Jakarta",
        speaker = "Budi Santoso",
        meetingMethod = MeetingMethod.ONLINE,
        zoomUrl = "https://zoom.us/j/123",
        location = null,
        recordingUrl = null,
        materialUrl = null,
        pricingScheme = PricingScheme.NORMAL_ONLY,
        normalPrice = 300000,
        earlyBirdPrice = null,
        lifecycle = TrainingLifecycle.ACTIVE,
        createdBy = "admin-1",
        createdAt = Instant.now(),
        updatedBy = "admin-1",
        updatedAt = Instant.now(),
    )

    private class FakeTrainingRepo(var training: Training?) : TrainingRepository {
        var archiveCalled = false

        override fun observeTrainings(includeArchived: Boolean): Flow<AppResult<TrainingListSnapshot>> =
            flowOf(AppResult.Success(TrainingListSnapshot(emptyList(), false)))

        override fun observeTraining(trainingId: String): Flow<AppResult<TrainingDetailSnapshot>> =
            flowOf(AppResult.Success(TrainingDetailSnapshot(training, false)))

        override suspend fun getTraining(trainingId: String): AppResult<Training?> =
            AppResult.Success(training)

        override suspend fun createTraining(training: Training): AppResult<Training> =
            AppResult.Success(training)

        override suspend fun updateTraining(actorId: String, training: Training): AppResult<Training> =
            AppResult.Success(training)

        override suspend fun archiveTraining(actorId: String, trainingId: String): AppResult<Unit> {
            archiveCalled = true
            return AppResult.Success(Unit)
        }
    }

    private class FakeAuthRepo(var currentProfile: UserProfile?) : AuthRepository {
        override fun observeSession(): Flow<UserSession> = flowOf(
            currentProfile?.let { UserSession.Authenticated(it) } ?: UserSession.Unauthenticated
        )
        override suspend fun login(email: String, password: String): AppResult<UserProfile> = error("Not needed")
        override suspend fun logout() {}
        override suspend fun sendPasswordReset(email: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun getCurrentUserProfile(): UserProfile? = currentProfile
    }

    private val adminProfile = UserProfile(
        id = "admin-1",
        displayName = "Admin",
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
        displayName = "User",
        email = "user@lembaga.org",
        role = UserRole.USER,
        active = true,
        invitationStatus = InvitationStatus.ACTIVE,
        createdBy = "admin-1",
        createdAt = Instant.now(),
        updatedBy = "admin-1",
        updatedAt = Instant.now(),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial load displays training detail`() = runTest {
        val trainingRepo = FakeTrainingRepo(sampleTraining)
        val authRepo = FakeAuthRepo(adminProfile)
        val observeDetailUseCase = ObserveTrainingDetailUseCase(trainingRepo)
        val archiveUseCase = ArchiveTrainingUseCase(trainingRepo, authRepo)
        val sessionUseCase = ObserveSessionUseCase(authRepo)
        val savedStateHandle = SavedStateHandle(mapOf("trainingId" to "t-100"))

        val viewModel = TrainingDetailViewModel(
            observeDetailUseCase,
            archiveUseCase,
            sessionUseCase,
            savedStateHandle,
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(sampleTraining, state.training)
        assertEquals(UserRole.ADMIN, state.currentUserRole)
    }

    @Test
    fun `archive succeeds when user is admin`() = runTest {
        val trainingRepo = FakeTrainingRepo(sampleTraining)
        val authRepo = FakeAuthRepo(adminProfile)
        val observeDetailUseCase = ObserveTrainingDetailUseCase(trainingRepo)
        val archiveUseCase = ArchiveTrainingUseCase(trainingRepo, authRepo)
        val sessionUseCase = ObserveSessionUseCase(authRepo)
        val savedStateHandle = SavedStateHandle(mapOf("trainingId" to "t-100"))

        val viewModel = TrainingDetailViewModel(
            observeDetailUseCase,
            archiveUseCase,
            sessionUseCase,
            savedStateHandle,
        )
        advanceUntilIdle()

        viewModel.archiveTraining()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.archiveSuccess)
        assertTrue(trainingRepo.archiveCalled)
    }

    @Test
    fun `archive blocked when user is USER role`() = runTest {
        val trainingRepo = FakeTrainingRepo(sampleTraining)
        val authRepo = FakeAuthRepo(userProfile)
        val observeDetailUseCase = ObserveTrainingDetailUseCase(trainingRepo)
        val archiveUseCase = ArchiveTrainingUseCase(trainingRepo, authRepo)
        val sessionUseCase = ObserveSessionUseCase(authRepo)
        val savedStateHandle = SavedStateHandle(mapOf("trainingId" to "t-100"))

        val viewModel = TrainingDetailViewModel(
            observeDetailUseCase,
            archiveUseCase,
            sessionUseCase,
            savedStateHandle,
        )
        advanceUntilIdle()

        viewModel.archiveTraining()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.archiveSuccess)
        assertFalse(trainingRepo.archiveCalled)
        assertEquals("Hanya Admin yang dapat mengarsipkan pelatihan.", state.errorMessage)
    }
}
