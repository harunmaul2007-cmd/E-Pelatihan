package com.harunmaul.epelatihan.feature.home

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.domain.model.AppSettings
import com.harunmaul.epelatihan.domain.model.InvitationStatus
import com.harunmaul.epelatihan.domain.model.MeetingMethod
import com.harunmaul.epelatihan.domain.model.PaymentStatus
import com.harunmaul.epelatihan.domain.model.PricingScheme
import com.harunmaul.epelatihan.domain.model.PricingType
import com.harunmaul.epelatihan.domain.model.Registration
import com.harunmaul.epelatihan.domain.model.RegistrationDetailSnapshot
import com.harunmaul.epelatihan.domain.model.RegistrationListSnapshot
import com.harunmaul.epelatihan.domain.model.Training
import com.harunmaul.epelatihan.domain.model.TrainingDetailSnapshot
import com.harunmaul.epelatihan.domain.model.TrainingLifecycle
import com.harunmaul.epelatihan.domain.model.TrainingListSnapshot
import com.harunmaul.epelatihan.domain.model.UserProfile
import com.harunmaul.epelatihan.domain.model.UserRole
import com.harunmaul.epelatihan.domain.model.UserSession
import com.harunmaul.epelatihan.domain.repository.AppSettingsRepository
import com.harunmaul.epelatihan.domain.repository.AuthRepository
import com.harunmaul.epelatihan.domain.repository.RegistrationRepository
import com.harunmaul.epelatihan.domain.repository.TrainingRepository
import com.harunmaul.epelatihan.domain.usecase.LogoutUseCase
import com.harunmaul.epelatihan.domain.usecase.ObserveAppSettingsUseCase
import com.harunmaul.epelatihan.domain.usecase.ObserveSessionUseCase
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val sampleUser = UserProfile(
        id = "user-1",
        displayName = "Admin E-Pelatihan",
        email = "admin@example.com",
        role = UserRole.ADMIN,
        active = true,
        invitationStatus = InvitationStatus.ACTIVE,
        createdBy = "sys",
        createdAt = Instant.now(),
        updatedBy = "sys",
        updatedAt = Instant.now(),
    )

    private val currentYear = YearMonth.now().year

    private val trainingMay = Training(
        id = "t-may",
        name = "Pelatihan Mei",
        description = "Desc",
        startAt = ZonedDateTime.of(currentYear, 5, 10, 9, 0, 0, 0, ZoneId.of("Asia/Jakarta")).toInstant(),
        endAt = ZonedDateTime.of(currentYear, 5, 10, 12, 0, 0, 0, ZoneId.of("Asia/Jakarta")).toInstant(),
        timeZoneId = "Asia/Jakarta",
        speaker = "Pemateri",
        meetingMethod = MeetingMethod.ONLINE,
        zoomUrl = null,
        location = null,
        recordingUrl = null,
        materialUrl = null,
        pricingScheme = PricingScheme.NORMAL_ONLY,
        normalPrice = 100000,
        earlyBirdPrice = null,
        lifecycle = TrainingLifecycle.ACTIVE,
        createdBy = "admin-1",
        createdAt = Instant.now(),
        updatedBy = "admin-1",
        updatedAt = Instant.now(),
    )

    private val trainingJune = trainingMay.copy(
        id = "t-june",
        name = "Pelatihan Juni",
        startAt = ZonedDateTime.of(currentYear, 6, 15, 9, 0, 0, 0, ZoneId.of("Asia/Jakarta")).toInstant(),
        endAt = ZonedDateTime.of(currentYear, 6, 15, 12, 0, 0, 0, ZoneId.of("Asia/Jakarta")).toInstant(),
    )

    private val registrations = listOf(
        Registration("r-1", "t-may", "p-1", PricingType.NORMAL, 100000, PaymentStatus.LUNAS, Instant.now(), "admin", "admin", Instant.now(), "admin", Instant.now()),
        Registration("r-2", "t-may", "p-2", PricingType.NORMAL, 100000, PaymentStatus.LUNAS, Instant.now(), "admin", "admin", Instant.now(), "admin", Instant.now()),
        Registration("r-3", "t-june", "p-1", PricingType.NORMAL, 100000, PaymentStatus.LUNAS, Instant.now(), "admin", "admin", Instant.now(), "admin", Instant.now()),
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
    fun `uiState calculates active trainings, total participants and monthly trends correctly`() = runTest(testDispatcher) {
        val appSettingsRepo = object : AppSettingsRepository {
            override fun observeAppSettings(): Flow<AppSettings> = flowOf(AppSettings("Komunitas Belajar", 1, null, null))
            override suspend fun getAppSettings(): AppSettings = AppSettings("Komunitas Belajar", 1, null, null)
            override suspend fun updateCommunityName(actorId: String, communityName: String): AppResult<AppSettings> = error("")
        }

        val authRepo = object : AuthRepository {
            override fun observeSession(): Flow<UserSession> = flowOf(UserSession.Authenticated(sampleUser))
            override suspend fun login(email: String, password: String): AppResult<UserProfile> = error("")
            override suspend fun logout() {}
            override suspend fun sendPasswordReset(email: String): AppResult<Unit> = error("")
            override suspend fun getCurrentUserProfile(): UserProfile = sampleUser
        }

        val trainingRepo = object : TrainingRepository {
            override fun observeTrainings(includeArchived: Boolean): Flow<AppResult<TrainingListSnapshot>> =
                flowOf(AppResult.Success(TrainingListSnapshot(listOf(trainingMay, trainingJune), false)))
            override fun observeTraining(trainingId: String): Flow<AppResult<TrainingDetailSnapshot>> = error("")
            override suspend fun getTraining(trainingId: String): AppResult<Training?> = error("")
            override suspend fun createTraining(training: Training): AppResult<Training> = error("")
            override suspend fun updateTraining(actorId: String, training: Training): AppResult<Training> = error("")
            override suspend fun archiveTraining(actorId: String, trainingId: String): AppResult<Unit> = error("")
        }

        val regRepo = object : RegistrationRepository {
            override fun observeAllRegistrations(): Flow<AppResult<List<Registration>>> =
                flowOf(AppResult.Success(registrations))
            override fun observeRegistrationsByTraining(trainingId: String): Flow<AppResult<RegistrationListSnapshot>> = error("")
            override fun observeRegistration(registrationId: String): Flow<AppResult<RegistrationDetailSnapshot>> = error("")
            override suspend fun getRegistration(registrationId: String): AppResult<Registration?> = error("")
            override suspend fun getRegistrationsByTraining(trainingId: String): AppResult<List<Registration>> = error("")
            override suspend fun createRegistration(registration: Registration): AppResult<Registration> = error("")
            override suspend fun updateRegistrationPrice(actorId: String, registrationId: String, newPricingType: PricingType, newPriceAmount: Long): AppResult<Registration> = error("")
            override suspend fun updatePaymentStatus(actorId: String, registrationId: String, newStatus: PaymentStatus): AppResult<Registration> = error("")
        }

        val viewModel = HomeViewModel(
            observeAppSettingsUseCase = ObserveAppSettingsUseCase(appSettingsRepo),
            observeSessionUseCase = ObserveSessionUseCase(authRepo),
            trainingRepository = trainingRepo,
            registrationRepository = regRepo,
            logoutUseCase = LogoutUseCase(authRepo),
        )

        val job = backgroundScope.launch {
            viewModel.uiState.collect()
        }

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Komunitas Belajar", state.communityName)
        assertEquals(2, state.activeTrainingsCount)
        assertEquals(3, state.totalParticipantsCount) // Total registrations across trainings
        assertEquals(12, state.monthlyTrends.size)

        // May (month 5) has 2 participants
        val mayTrend = state.monthlyTrends[4]
        assertEquals(5, mayTrend.monthValue)
        assertEquals(2, mayTrend.participantCount)

        // June (month 6) has 1 participant
        val juneTrend = state.monthlyTrends[5]
        assertEquals(6, juneTrend.monthValue)
        assertEquals(1, juneTrend.participantCount)

        // Other months have 0
        assertEquals(0, state.monthlyTrends[0].participantCount) // Jan

        // Total year participants
        assertEquals(3, state.totalYearParticipants)
    }
}
