package com.harunmaul.epelatihan.feature.training.form

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
import com.harunmaul.epelatihan.domain.usecase.CreateTrainingUseCase
import com.harunmaul.epelatihan.domain.usecase.ObserveSessionUseCase
import com.harunmaul.epelatihan.domain.usecase.UpdateTrainingUseCase
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TrainingFormViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val sampleTraining = Training(
        id = "tr-200",
        name = "Pelatihan Arsitektur Android",
        description = "Clean Architecture",
        startAt = Instant.parse("2026-11-25T02:00:00Z"),
        endAt = Instant.parse("2026-11-25T05:00:00Z"),
        timeZoneId = "Asia/Jakarta",
        speaker = "Budi Hartono",
        meetingMethod = MeetingMethod.ONLINE,
        zoomUrl = "https://zoom.us/j/123456789",
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
        var createCalled = false
        var updateCalled = false

        override fun observeTrainings(includeArchived: Boolean): Flow<AppResult<TrainingListSnapshot>> =
            flowOf(AppResult.Success(TrainingListSnapshot(emptyList(), false)))

        override fun observeTraining(trainingId: String): Flow<AppResult<TrainingDetailSnapshot>> =
            flowOf(AppResult.Success(TrainingDetailSnapshot(training, false)))

        override suspend fun getTraining(trainingId: String): AppResult<Training?> =
            AppResult.Success(training)

        override suspend fun createTraining(training: Training): AppResult<Training> {
            createCalled = true
            return AppResult.Success(training.copy(id = "new-id"))
        }

        override suspend fun updateTraining(actorId: String, training: Training): AppResult<Training> {
            updateCalled = true
            return AppResult.Success(training)
        }

        override suspend fun archiveTraining(actorId: String, trainingId: String): AppResult<Unit> =
            AppResult.Success(Unit)
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
    fun `create mode initial state is empty and user can save valid training`() = runTest {
        val trainingRepo = FakeTrainingRepo(null)
        val authRepo = FakeAuthRepo(userProfile)
        val createUseCase = CreateTrainingUseCase(trainingRepo, authRepo)
        val updateUseCase = UpdateTrainingUseCase(trainingRepo, authRepo)
        val sessionUseCase = ObserveSessionUseCase(authRepo)
        val savedStateHandle = SavedStateHandle()

        val viewModel = TrainingFormViewModel(
            createUseCase,
            updateUseCase,
            trainingRepo,
            sessionUseCase,
            savedStateHandle,
        )
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isEditMode)
        assertFalse(viewModel.uiState.value.isAccessDenied)

        // Fill form
        viewModel.onNameChanged("Workshop Baru")
        viewModel.onSpeakerChanged("Narasumber Hebat")
        viewModel.onMeetingMethodChanged(MeetingMethod.ONLINE)
        viewModel.onZoomUrlChanged("https://zoom.us/j/123456")
        viewModel.onPricingSchemeChanged(PricingScheme.NORMAL_ONLY)
        viewModel.onNormalPriceChanged("250000")
        viewModel.onStartTimeChanged(LocalTime.of(9, 0))
        viewModel.onEndTimeChanged(LocalTime.of(12, 0))

        viewModel.saveTraining()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.saveSuccess)
        assertTrue(trainingRepo.createCalled)
    }

    @Test
    fun `validation error on blank fields prevents saving`() = runTest {
        val trainingRepo = FakeTrainingRepo(null)
        val authRepo = FakeAuthRepo(userProfile)
        val createUseCase = CreateTrainingUseCase(trainingRepo, authRepo)
        val updateUseCase = UpdateTrainingUseCase(trainingRepo, authRepo)
        val sessionUseCase = ObserveSessionUseCase(authRepo)
        val savedStateHandle = SavedStateHandle()

        val viewModel = TrainingFormViewModel(
            createUseCase,
            updateUseCase,
            trainingRepo,
            sessionUseCase,
            savedStateHandle,
        )
        advanceUntilIdle()

        // Submit without filling required fields
        viewModel.saveTraining()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.saveSuccess)
        assertFalse(trainingRepo.createCalled)
        assertNotNull(viewModel.uiState.value.nameError)
        assertNotNull(viewModel.uiState.value.speakerError)
    }

    @Test
    fun `edit mode loads existing training and admin can update`() = runTest {
        val trainingRepo = FakeTrainingRepo(sampleTraining)
        val authRepo = FakeAuthRepo(adminProfile)
        val createUseCase = CreateTrainingUseCase(trainingRepo, authRepo)
        val updateUseCase = UpdateTrainingUseCase(trainingRepo, authRepo)
        val sessionUseCase = ObserveSessionUseCase(authRepo)
        val savedStateHandle = SavedStateHandle(mapOf("trainingId" to "tr-200"))

        val viewModel = TrainingFormViewModel(
            createUseCase,
            updateUseCase,
            trainingRepo,
            sessionUseCase,
            savedStateHandle,
        )
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isEditMode)
        assertFalse(viewModel.uiState.value.isAccessDenied)
        assertEquals("Pelatihan Arsitektur Android", viewModel.uiState.value.name)
        assertEquals("Budi Hartono", viewModel.uiState.value.speaker)
        assertEquals("300000", viewModel.uiState.value.normalPriceInput)

        // Modify and save
        viewModel.onNameChanged("Pelatihan Arsitektur Android Lanjutan")
        viewModel.saveTraining()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.saveSuccess)
        assertTrue(trainingRepo.updateCalled)
    }

    @Test
    fun `edit mode blocks non-admin user`() = runTest {
        val trainingRepo = FakeTrainingRepo(sampleTraining)
        val authRepo = FakeAuthRepo(userProfile) // regular user
        val createUseCase = CreateTrainingUseCase(trainingRepo, authRepo)
        val updateUseCase = UpdateTrainingUseCase(trainingRepo, authRepo)
        val sessionUseCase = ObserveSessionUseCase(authRepo)
        val savedStateHandle = SavedStateHandle(mapOf("trainingId" to "tr-200"))

        val viewModel = TrainingFormViewModel(
            createUseCase,
            updateUseCase,
            trainingRepo,
            sessionUseCase,
            savedStateHandle,
        )
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isAccessDenied)

        // Attempting to save does nothing
        viewModel.saveTraining()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.saveSuccess)
        assertFalse(trainingRepo.updateCalled)
    }
}
