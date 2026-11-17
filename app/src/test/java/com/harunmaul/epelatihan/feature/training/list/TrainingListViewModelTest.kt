package com.harunmaul.epelatihan.feature.training.list

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
import com.harunmaul.epelatihan.domain.usecase.ObserveSessionUseCase
import com.harunmaul.epelatihan.domain.usecase.ObserveTrainingsUseCase
import java.time.Instant
import java.time.temporal.ChronoUnit
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TrainingListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val sampleActiveTraining = Training(
        id = "t-1",
        name = "Pelatihan Android Jetpack Compose",
        description = "Deskripsi",
        startAt = Instant.now().plus(2, ChronoUnit.DAYS),
        endAt = Instant.now().plus(2, ChronoUnit.DAYS).plus(3, ChronoUnit.HOURS),
        timeZoneId = "Asia/Jakarta",
        speaker = "Budi Hartono",
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

    private val sampleArchivedTraining = Training(
        id = "t-2",
        name = "Pelatihan Kotlin Dasar",
        description = "Arsip pelatihan lama",
        startAt = Instant.now().minus(30, ChronoUnit.DAYS),
        endAt = Instant.now().minus(30, ChronoUnit.DAYS).plus(4, ChronoUnit.HOURS),
        timeZoneId = "Asia/Jakarta",
        speaker = "Siti Rahma",
        meetingMethod = MeetingMethod.OFFLINE,
        zoomUrl = null,
        location = "Gedung Diklat",
        recordingUrl = null,
        materialUrl = null,
        pricingScheme = PricingScheme.NORMAL_ONLY,
        normalPrice = 250000,
        earlyBirdPrice = null,
        lifecycle = TrainingLifecycle.ARCHIVED,
        createdBy = "admin-1",
        createdAt = Instant.now(),
        updatedBy = "admin-1",
        updatedAt = Instant.now(),
    )

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

    private class FakeTrainingRepo : TrainingRepository {
        var trainings = listOf<Training>()
        var isFromCache = false
        var shouldFail = false

        override fun observeTrainings(includeArchived: Boolean): Flow<AppResult<TrainingListSnapshot>> {
            return if (shouldFail) {
                flowOf(AppResult.Failure(RuntimeException("Koneksi gagal")))
            } else {
                val list = if (includeArchived) {
                    trainings
                } else {
                    trainings.filter { it.lifecycle == TrainingLifecycle.ACTIVE }
                }
                flowOf(AppResult.Success(TrainingListSnapshot(list, isFromCache)))
            }
        }

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

    private class FakeAuthRepo(val profile: UserProfile) : AuthRepository {
        override fun observeSession(): Flow<UserSession> = flowOf(UserSession.Authenticated(profile))
        override suspend fun login(email: String, password: String): AppResult<UserProfile> = error("Not needed")
        override suspend fun logout() {}
        override suspend fun sendPasswordReset(email: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun getCurrentUserProfile(): UserProfile = profile
    }

    private lateinit var trainingRepo: FakeTrainingRepo
    private lateinit var observeTrainingsUseCase: ObserveTrainingsUseCase
    private lateinit var observeSessionUseCase: ObserveSessionUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        trainingRepo = FakeTrainingRepo().apply {
            trainings = listOf(sampleActiveTraining, sampleArchivedTraining)
        }
        observeTrainingsUseCase = ObserveTrainingsUseCase(trainingRepo)
        observeSessionUseCase = ObserveSessionUseCase(FakeAuthRepo(adminProfile))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial load displays active trainings and admin role`() = runTest {
        val viewModel = TrainingListViewModel(observeTrainingsUseCase, observeSessionUseCase)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(UserRole.ADMIN, state.currentUserRole)
        assertEquals(1, state.trainings.size)
        assertEquals("Pelatihan Android Jetpack Compose", state.trainings[0].name)
        assertEquals(1, state.filteredTrainings.size)
    }

    @Test
    fun `selectTab ARCHIVED loads archived trainings`() = runTest {
        val viewModel = TrainingListViewModel(observeTrainingsUseCase, observeSessionUseCase)
        advanceUntilIdle()

        viewModel.selectTab(TrainingTab.ARCHIVED)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TrainingTab.ARCHIVED, state.selectedTab)
        assertEquals(1, state.trainings.size)
        assertEquals("Pelatihan Kotlin Dasar", state.trainings[0].name)
    }

    @Test
    fun `search filter works by name and speaker`() = runTest {
        val viewModel = TrainingListViewModel(observeTrainingsUseCase, observeSessionUseCase)
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("Budi")
        assertEquals(1, viewModel.uiState.value.filteredTrainings.size)

        viewModel.onSearchQueryChanged("TidakAda")
        assertEquals(0, viewModel.uiState.value.filteredTrainings.size)
    }

    @Test
    fun `error state shows error message`() = runTest {
        trainingRepo.shouldFail = true
        val viewModel = TrainingListViewModel(observeTrainingsUseCase, observeSessionUseCase)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Koneksi gagal", state.errorMessage)
    }
}
