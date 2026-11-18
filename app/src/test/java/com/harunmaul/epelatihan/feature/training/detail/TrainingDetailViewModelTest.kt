package com.harunmaul.epelatihan.feature.training.detail

import androidx.lifecycle.SavedStateHandle
import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.domain.model.InvitationStatus
import com.harunmaul.epelatihan.domain.model.MeetingMethod
import com.harunmaul.epelatihan.domain.model.Participant
import com.harunmaul.epelatihan.domain.model.ParticipantDetailSnapshot
import com.harunmaul.epelatihan.domain.model.ParticipantListSnapshot
import com.harunmaul.epelatihan.domain.model.PaymentStatus
import com.harunmaul.epelatihan.domain.model.PricingScheme
import com.harunmaul.epelatihan.domain.model.PricingType
import com.harunmaul.epelatihan.domain.model.Registration
import com.harunmaul.epelatihan.domain.model.RegistrationDetailSnapshot
import com.harunmaul.epelatihan.domain.model.RegistrationListSnapshot
import com.harunmaul.epelatihan.domain.model.RegistrationWithParticipant
import com.harunmaul.epelatihan.domain.model.Training
import com.harunmaul.epelatihan.domain.model.TrainingDetailSnapshot
import com.harunmaul.epelatihan.domain.model.TrainingLifecycle
import com.harunmaul.epelatihan.domain.model.AppSettings
import com.harunmaul.epelatihan.domain.model.TrainingListSnapshot
import com.harunmaul.epelatihan.domain.model.UserProfile
import com.harunmaul.epelatihan.domain.model.UserRole
import com.harunmaul.epelatihan.domain.model.UserSession
import com.harunmaul.epelatihan.domain.model.WhatsAppOpenLog
import com.harunmaul.epelatihan.domain.repository.AppSettingsRepository
import com.harunmaul.epelatihan.domain.repository.AuthRepository
import com.harunmaul.epelatihan.domain.repository.ParticipantRepository
import com.harunmaul.epelatihan.domain.repository.RegistrationRepository
import com.harunmaul.epelatihan.domain.repository.TrainingRepository
import com.harunmaul.epelatihan.domain.repository.WhatsAppLauncher
import com.harunmaul.epelatihan.domain.repository.WhatsAppLogRepository
import com.harunmaul.epelatihan.domain.usecase.ArchiveTrainingUseCase
import com.harunmaul.epelatihan.domain.usecase.BuildWhatsAppMessageUseCase
import com.harunmaul.epelatihan.domain.usecase.CreateParticipantUseCase
import com.harunmaul.epelatihan.domain.usecase.ObserveParticipantsUseCase
import com.harunmaul.epelatihan.domain.usecase.ObserveSessionUseCase
import com.harunmaul.epelatihan.domain.usecase.ObserveTrainingDetailUseCase
import com.harunmaul.epelatihan.domain.usecase.ObserveTrainingRegistrationsUseCase
import com.harunmaul.epelatihan.domain.usecase.OpenWhatsAppUseCase
import com.harunmaul.epelatihan.domain.usecase.RegisterParticipantUseCase
import com.harunmaul.epelatihan.domain.usecase.UpdatePaymentStatusUseCase
import com.harunmaul.epelatihan.domain.usecase.UpdateRegistrationPriceUseCase
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
        pricingScheme = PricingScheme.EARLY_BIRD_AND_NORMAL,
        normalPrice = 500000,
        earlyBirdPrice = 400000,
        lifecycle = TrainingLifecycle.ACTIVE,
        createdBy = "admin-1",
        createdAt = Instant.now(),
        updatedBy = "admin-1",
        updatedAt = Instant.now(),
    )

    private val sampleParticipant = Participant(
        id = "p-1",
        name = "Andi Wijaya",
        nameSort = "andi wijaya",
        phoneDisplay = "0812-3456-7890",
        phoneNormalized = "6281234567890",
        email = "andi@test.com",
        notes = null,
        active = true,
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
            currentProfile?.let { UserSession.Authenticated(it) } ?: UserSession.Unauthenticated,
        )
        override suspend fun login(email: String, password: String): AppResult<UserProfile> = error("Not needed")
        override suspend fun logout() {}
        override suspend fun sendPasswordReset(email: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun getCurrentUserProfile(): UserProfile? = currentProfile
    }

    private class FakeRegistrationRepo(initialRegistrations: List<Registration> = emptyList()) : RegistrationRepository {
        val registrations = initialRegistrations.toMutableList()

        override fun observeRegistrationsByTraining(trainingId: String): Flow<AppResult<RegistrationListSnapshot>> =
            flowOf(AppResult.Success(RegistrationListSnapshot(registrations, false)))

        override fun observeRegistration(registrationId: String): Flow<AppResult<RegistrationDetailSnapshot>> =
            flowOf(AppResult.Success(RegistrationDetailSnapshot(registrations.find { it.id == registrationId }, false)))

        override suspend fun getRegistration(registrationId: String): AppResult<Registration?> =
            AppResult.Success(registrations.find { it.id == registrationId })

        override suspend fun getRegistrationsByTraining(trainingId: String): AppResult<List<Registration>> =
            AppResult.Success(registrations)

        override suspend fun createRegistration(registration: Registration): AppResult<Registration> {
            registrations.add(registration)
            return AppResult.Success(registration)
        }

        override suspend fun updateRegistrationPrice(
            actorId: String,
            registrationId: String,
            newPricingType: PricingType,
            newPriceAmount: Long,
        ): AppResult<Registration> {
            val idx = registrations.indexOfFirst { it.id == registrationId }
            if (idx >= 0) {
                val updated = registrations[idx].copy(pricingType = newPricingType, priceAmount = newPriceAmount)
                registrations[idx] = updated
                return AppResult.Success(updated)
            }
            return AppResult.Failure(NoSuchElementException())
        }

        override suspend fun updatePaymentStatus(
            actorId: String,
            registrationId: String,
            newStatus: PaymentStatus,
        ): AppResult<Registration> {
            val idx = registrations.indexOfFirst { it.id == registrationId }
            if (idx >= 0) {
                val updated = registrations[idx].copy(
                    paymentStatus = newStatus,
                    paidAt = if (newStatus == PaymentStatus.LUNAS) Instant.now() else null,
                    paidBy = if (newStatus == PaymentStatus.LUNAS) actorId else null,
                )
                registrations[idx] = updated
                return AppResult.Success(updated)
            }
            return AppResult.Failure(NoSuchElementException())
        }
    }

    private class FakeParticipantRepo(val participants: List<Participant>) : ParticipantRepository {
        override fun observeParticipants(includeInactive: Boolean): Flow<AppResult<ParticipantListSnapshot>> =
            flowOf(AppResult.Success(ParticipantListSnapshot(participants, false)))

        override fun observeParticipant(participantId: String): Flow<AppResult<ParticipantDetailSnapshot>> =
            flowOf(AppResult.Success(ParticipantDetailSnapshot(participants.find { it.id == participantId }, false)))

        override suspend fun getParticipant(participantId: String): AppResult<Participant?> =
            AppResult.Success(participants.find { it.id == participantId })

        override suspend fun findParticipantByPhone(phoneNormalized: String): AppResult<Participant?> =
            AppResult.Success(participants.find { it.phoneNormalized == phoneNormalized })

        override suspend fun createParticipant(participant: Participant): AppResult<Participant> =
            AppResult.Success(participant)

        override suspend fun updateParticipant(actorId: String, participant: Participant): AppResult<Participant> =
            AppResult.Success(participant)

        override suspend fun archiveParticipant(actorId: String, participantId: String): AppResult<Unit> =
            AppResult.Success(Unit)
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

    private class FakeAppSettingsRepo : AppSettingsRepository {
        override fun observeAppSettings(): Flow<AppSettings> = flowOf(AppSettings("E-Pelatihan", 1, null, null))
        override suspend fun getAppSettings(): AppSettings = AppSettings("E-Pelatihan", 1, null, null)
    }

    private class FakeWhatsAppLauncher : WhatsAppLauncher {
        var opened = false
        override fun openWhatsApp(phoneNormalized: String, message: String): AppResult<Unit> {
            opened = true
            return AppResult.Success(Unit)
        }
    }

    private class FakeWhatsAppLogRepo : WhatsAppLogRepository {
        override suspend fun recordOpenLog(actorId: String, registrationId: String, recipientPhone: String, messageSnapshot: String): AppResult<WhatsAppOpenLog> =
            AppResult.Success(WhatsAppOpenLog("1", registrationId, recipientPhone, messageSnapshot, actorId, Instant.now()))
        override fun observeLogsByRegistration(registrationId: String): Flow<List<WhatsAppOpenLog>> = flowOf(emptyList())
    }

    private fun createViewModel(
        trainingRepo: TrainingRepository,
        authRepo: AuthRepository,
        regRepo: RegistrationRepository,
        partRepo: ParticipantRepository,
    ): TrainingDetailViewModel {
        val observeDetailUseCase = ObserveTrainingDetailUseCase(trainingRepo)
        val archiveUseCase = ArchiveTrainingUseCase(trainingRepo, authRepo)
        val observeRegistrationsUseCase = ObserveTrainingRegistrationsUseCase(regRepo, partRepo)
        val observeParticipantsUseCase = ObserveParticipantsUseCase(partRepo)
        val createPartUseCase = CreateParticipantUseCase(partRepo, authRepo)
        val registerPartUseCase = RegisterParticipantUseCase(regRepo, createPartUseCase, authRepo)
        val updateRegPriceUseCase = UpdateRegistrationPriceUseCase(regRepo, trainingRepo, authRepo)
        val updatePaymentStatusUseCase = UpdatePaymentStatusUseCase(regRepo, authRepo)
        val openWhatsAppUseCase = OpenWhatsAppUseCase(
            registrationRepository = regRepo,
            participantRepository = partRepo,
            trainingRepository = trainingRepo,
            appSettingsRepository = FakeAppSettingsRepo(),
            authRepository = authRepo,
            buildWhatsAppMessageUseCase = BuildWhatsAppMessageUseCase(),
            whatsAppLauncher = FakeWhatsAppLauncher(),
            whatsAppLogRepository = FakeWhatsAppLogRepo(),
        )
        val sessionUseCase = ObserveSessionUseCase(authRepo)
        val savedStateHandle = SavedStateHandle(mapOf("trainingId" to "t-100"))

        return TrainingDetailViewModel(
            savedStateHandle = savedStateHandle,
            observeTrainingDetailUseCase = observeDetailUseCase,
            archiveTrainingUseCase = archiveUseCase,
            observeTrainingRegistrationsUseCase = observeRegistrationsUseCase,
            observeParticipantsUseCase = observeParticipantsUseCase,
            registerParticipantUseCase = registerPartUseCase,
            updateRegistrationPriceUseCase = updateRegPriceUseCase,
            updatePaymentStatusUseCase = updatePaymentStatusUseCase,
            openWhatsAppUseCase = openWhatsAppUseCase,
            observeSessionUseCase = sessionUseCase,
        )
    }

    @Test
    fun `initial load displays training detail`() = runTest {
        val trainingRepo = FakeTrainingRepo(sampleTraining)
        val authRepo = FakeAuthRepo(adminProfile)
        val regRepo = FakeRegistrationRepo()
        val partRepo = FakeParticipantRepo(listOf(sampleParticipant))

        val viewModel = createViewModel(trainingRepo, authRepo, regRepo, partRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(sampleTraining, state.training)
        assertEquals(UserRole.ADMIN, state.currentUserRole)
        assertEquals(1, state.availableParticipants.size)
    }

    @Test
    fun `archive succeeds when user is admin`() = runTest {
        val trainingRepo = FakeTrainingRepo(sampleTraining)
        val authRepo = FakeAuthRepo(adminProfile)
        val regRepo = FakeRegistrationRepo()
        val partRepo = FakeParticipantRepo(listOf(sampleParticipant))

        val viewModel = createViewModel(trainingRepo, authRepo, regRepo, partRepo)
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
        val regRepo = FakeRegistrationRepo()
        val partRepo = FakeParticipantRepo(listOf(sampleParticipant))

        val viewModel = createViewModel(trainingRepo, authRepo, regRepo, partRepo)
        advanceUntilIdle()

        viewModel.archiveTraining()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.archiveSuccess)
        assertFalse(trainingRepo.archiveCalled)
    }

    @Test
    fun `registering participant succeeds and adds to list`() = runTest {
        val trainingRepo = FakeTrainingRepo(sampleTraining)
        val authRepo = FakeAuthRepo(userProfile)
        val regRepo = FakeRegistrationRepo()
        val partRepo = FakeParticipantRepo(listOf(sampleParticipant))

        val viewModel = createViewModel(trainingRepo, authRepo, regRepo, partRepo)
        advanceUntilIdle()

        viewModel.registerExistingParticipant("p-1", PricingType.EARLY_BIRD)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Peserta berhasil didaftarkan.", state.successMessage)
        assertEquals(1, regRepo.registrations.size)
        assertEquals(400000L, regRepo.registrations[0].priceAmount)
    }

    @Test
    fun `admin updating registration price updates registration in list`() = runTest {
        val initialReg = Registration(
            id = "reg-100",
            trainingId = "t-100",
            participantId = "p-1",
            pricingType = PricingType.EARLY_BIRD,
            priceAmount = 400000L,
            paymentStatus = PaymentStatus.BELUM_BAYAR,
            paidAt = null,
            paidBy = null,
            createdBy = "admin-1",
            createdAt = Instant.now(),
            updatedBy = "admin-1",
            updatedAt = Instant.now(),
        )
        val trainingRepo = FakeTrainingRepo(sampleTraining)
        val authRepo = FakeAuthRepo(adminProfile)
        val regRepo = FakeRegistrationRepo(listOf(initialReg))
        val partRepo = FakeParticipantRepo(listOf(sampleParticipant))

        val viewModel = createViewModel(trainingRepo, authRepo, regRepo, partRepo)
        advanceUntilIdle()

        viewModel.openEditPriceDialog(RegistrationWithParticipant(initialReg, sampleParticipant))
        viewModel.updateRegistrationPrice(PricingType.NORMAL)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Jenis harga pendaftaran berhasil diperbarui.", state.successMessage)
        assertEquals(PricingType.NORMAL, regRepo.registrations[0].pricingType)
        assertEquals(500000L, regRepo.registrations[0].priceAmount)
    }

    @Test
    fun `admin updating payment status to LUNAS succeeds and updates list`() = runTest {
        val initialReg = Registration(
            id = "reg-100",
            trainingId = "t-100",
            participantId = "p-1",
            pricingType = PricingType.NORMAL,
            priceAmount = 500000L,
            paymentStatus = PaymentStatus.BELUM_BAYAR,
            paidAt = null,
            paidBy = null,
            createdBy = "admin-1",
            createdAt = Instant.now(),
            updatedBy = "admin-1",
            updatedAt = Instant.now(),
        )
        val trainingRepo = FakeTrainingRepo(sampleTraining)
        val authRepo = FakeAuthRepo(adminProfile)
        val regRepo = FakeRegistrationRepo(listOf(initialReg))
        val partRepo = FakeParticipantRepo(listOf(sampleParticipant))

        val viewModel = createViewModel(trainingRepo, authRepo, regRepo, partRepo)
        advanceUntilIdle()

        viewModel.openPaymentStatusDialog(RegistrationWithParticipant(initialReg, sampleParticipant))
        assertTrue(viewModel.uiState.value.showPaymentStatusDialog)

        viewModel.updatePaymentStatus(PaymentStatus.LUNAS)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Status pembayaran berhasil ditandai Lunas.", state.successMessage)
        assertFalse(state.showPaymentStatusDialog)
        assertEquals(PaymentStatus.LUNAS, regRepo.registrations[0].paymentStatus)
        assertEquals("admin-1", regRepo.registrations[0].paidBy)
    }

    @Test
    fun `admin reverting payment status to BELUM_BAYAR succeeds`() = runTest {
        val initialReg = Registration(
            id = "reg-100",
            trainingId = "t-100",
            participantId = "p-1",
            pricingType = PricingType.NORMAL,
            priceAmount = 500000L,
            paymentStatus = PaymentStatus.LUNAS,
            paidAt = Instant.now(),
            paidBy = "admin-1",
            createdBy = "admin-1",
            createdAt = Instant.now(),
            updatedBy = "admin-1",
            updatedAt = Instant.now(),
        )
        val trainingRepo = FakeTrainingRepo(sampleTraining)
        val authRepo = FakeAuthRepo(adminProfile)
        val regRepo = FakeRegistrationRepo(listOf(initialReg))
        val partRepo = FakeParticipantRepo(listOf(sampleParticipant))

        val viewModel = createViewModel(trainingRepo, authRepo, regRepo, partRepo)
        advanceUntilIdle()

        viewModel.openPaymentStatusDialog(RegistrationWithParticipant(initialReg, sampleParticipant))
        viewModel.updatePaymentStatus(PaymentStatus.BELUM_BAYAR)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Status pembayaran berhasil diubah menjadi Belum Bayar.", state.successMessage)
        assertEquals(PaymentStatus.BELUM_BAYAR, regRepo.registrations[0].paymentStatus)
        assertEquals(null, regRepo.registrations[0].paidBy)
    }

    @Test
    fun `regular user cannot open payment dialog or update payment status`() = runTest {
        val initialReg = Registration(
            id = "reg-100",
            trainingId = "t-100",
            participantId = "p-1",
            pricingType = PricingType.NORMAL,
            priceAmount = 500000L,
            paymentStatus = PaymentStatus.BELUM_BAYAR,
            paidAt = null,
            paidBy = null,
            createdBy = "admin-1",
            createdAt = Instant.now(),
            updatedBy = "admin-1",
            updatedAt = Instant.now(),
        )
        val trainingRepo = FakeTrainingRepo(sampleTraining)
        val authRepo = FakeAuthRepo(userProfile)
        val regRepo = FakeRegistrationRepo(listOf(initialReg))
        val partRepo = FakeParticipantRepo(listOf(sampleParticipant))

        val viewModel = createViewModel(trainingRepo, authRepo, regRepo, partRepo)
        advanceUntilIdle()

        viewModel.openPaymentStatusDialog(RegistrationWithParticipant(initialReg, sampleParticipant))
        assertFalse(viewModel.uiState.value.showPaymentStatusDialog)

        viewModel.updatePaymentStatus(PaymentStatus.LUNAS)
        advanceUntilIdle()

        assertEquals(PaymentStatus.BELUM_BAYAR, regRepo.registrations[0].paymentStatus)
    }

    @Test
    fun `opening and dismissing registration detail dialog updates state properly`() = runTest {
        val initialReg = Registration(
            id = "reg-100",
            trainingId = "t-100",
            participantId = "p-1",
            pricingType = PricingType.NORMAL,
            priceAmount = 500000L,
            paymentStatus = PaymentStatus.BELUM_BAYAR,
            paidAt = null,
            paidBy = null,
            createdBy = "admin-1",
            createdAt = Instant.now(),
            updatedBy = "admin-1",
            updatedAt = Instant.now(),
        )
        val trainingRepo = FakeTrainingRepo(sampleTraining)
        val authRepo = FakeAuthRepo(userProfile)
        val regRepo = FakeRegistrationRepo(listOf(initialReg))
        val partRepo = FakeParticipantRepo(listOf(sampleParticipant))

        val viewModel = createViewModel(trainingRepo, authRepo, regRepo, partRepo)
        advanceUntilIdle()

        val item = RegistrationWithParticipant(initialReg, sampleParticipant)
        viewModel.openRegistrationDetailDialog(item)
        assertTrue(viewModel.uiState.value.showRegistrationDetailDialog)
        assertEquals(item, viewModel.uiState.value.selectedRegistrationForDetail)

        viewModel.dismissRegistrationDetailDialog()
        assertFalse(viewModel.uiState.value.showRegistrationDetailDialog)
        assertEquals(null, viewModel.uiState.value.selectedRegistrationForDetail)
    }

    @Test
    fun `opening whatsapp for paid registration updates success message`() = runTest {
        val paidReg = Registration(
            id = "reg-100",
            trainingId = "t-100",
            participantId = "p-1",
            pricingType = PricingType.NORMAL,
            priceAmount = 500000L,
            paymentStatus = PaymentStatus.LUNAS,
            paidAt = Instant.now(),
            paidBy = "admin-1",
            createdBy = "admin-1",
            createdAt = Instant.now(),
            updatedBy = "admin-1",
            updatedAt = Instant.now(),
        )
        val trainingRepo = FakeTrainingRepo(sampleTraining)
        val authRepo = FakeAuthRepo(userProfile)
        val regRepo = FakeRegistrationRepo(listOf(paidReg))
        val partRepo = FakeParticipantRepo(listOf(sampleParticipant))

        val viewModel = createViewModel(trainingRepo, authRepo, regRepo, partRepo)
        advanceUntilIdle()

        viewModel.openWhatsApp("reg-100")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Membuka WhatsApp… Periksa pesan dan tekan kirim di WhatsApp.", state.successMessage)
    }
}
