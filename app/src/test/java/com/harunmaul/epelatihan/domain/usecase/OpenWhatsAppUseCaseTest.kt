package com.harunmaul.epelatihan.domain.usecase

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.core.common.AuthorizationException
import com.harunmaul.epelatihan.core.common.InactiveAccountException
import com.harunmaul.epelatihan.domain.model.AppSettings
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
import com.harunmaul.epelatihan.domain.model.Training
import com.harunmaul.epelatihan.domain.model.TrainingDetailSnapshot
import com.harunmaul.epelatihan.domain.model.TrainingLifecycle
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
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenWhatsAppUseCaseTest {

    private val sampleTraining = Training(
        id = "t-1",
        name = "Pelatihan Android",
        description = "Deskripsi",
        startAt = Instant.now(),
        endAt = Instant.now().plusSeconds(3600),
        timeZoneId = "Asia/Jakarta",
        speaker = "Pemateri",
        meetingMethod = MeetingMethod.ONLINE,
        zoomUrl = "https://zoom.us/j/123",
        location = null,
        recordingUrl = null,
        materialUrl = null,
        pricingScheme = PricingScheme.NORMAL_ONLY,
        normalPrice = 150000,
        earlyBirdPrice = null,
        lifecycle = TrainingLifecycle.ACTIVE,
        createdBy = "admin-1",
        createdAt = Instant.now(),
        updatedBy = "admin-1",
        updatedAt = Instant.now(),
    )

    private val sampleParticipant = Participant(
        id = "p-1",
        name = "Budi Hartono",
        nameSort = "budi hartono",
        phoneDisplay = "0812-3456-7890",
        phoneNormalized = "6281234567890",
        email = "budi@example.com",
        notes = null,
        active = true,
        createdBy = "admin-1",
        createdAt = Instant.now(),
        updatedBy = "admin-1",
        updatedAt = Instant.now(),
    )

    private val paidRegistration = Registration(
        id = "reg-paid",
        trainingId = "t-1",
        participantId = "p-1",
        pricingType = PricingType.NORMAL,
        priceAmount = 150000,
        paymentStatus = PaymentStatus.LUNAS,
        paidAt = Instant.now(),
        paidBy = "admin-1",
        createdBy = "admin-1",
        createdAt = Instant.now(),
        updatedBy = "admin-1",
        updatedAt = Instant.now(),
    )

    private val unpaidRegistration = paidRegistration.copy(
        id = "reg-unpaid",
        paymentStatus = PaymentStatus.BELUM_BAYAR,
        paidAt = null,
        paidBy = null,
    )

    private val userProfile = UserProfile(
        id = "user-1",
        displayName = "Petugas",
        email = "user@lembaga.org",
        role = UserRole.USER,
        active = true,
        invitationStatus = InvitationStatus.ACTIVE,
        createdBy = "admin-1",
        createdAt = Instant.now(),
        updatedBy = "admin-1",
        updatedAt = Instant.now(),
    )

    private class FakeAuthRepo(private val profile: UserProfile?) : AuthRepository {
        override fun observeSession(): Flow<UserSession> = flowOf(
            profile?.let { UserSession.Authenticated(it) } ?: UserSession.Unauthenticated,
        )
        override suspend fun login(email: String, password: String): AppResult<UserProfile> = AppResult.Success(profile!!)
        override suspend fun logout() {}
        override suspend fun sendPasswordReset(email: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun getCurrentUserProfile(): UserProfile? = profile
    }

    private class FakeRegRepo(private val registrations: Map<String, Registration>) : RegistrationRepository {
        override fun observeRegistrationsByTraining(trainingId: String): Flow<AppResult<RegistrationListSnapshot>> =
            flowOf(AppResult.Success(RegistrationListSnapshot(registrations.values.toList(), false)))

        override fun observeRegistration(registrationId: String): Flow<AppResult<RegistrationDetailSnapshot>> =
            flowOf(AppResult.Success(RegistrationDetailSnapshot(registrations[registrationId], false)))

        override suspend fun getRegistration(registrationId: String): AppResult<Registration?> =
            AppResult.Success(registrations[registrationId])

        override suspend fun getRegistrationsByTraining(trainingId: String): AppResult<List<Registration>> =
            AppResult.Success(registrations.values.toList())

        override suspend fun createRegistration(registration: Registration): AppResult<Registration> =
            AppResult.Success(registration)

        override suspend fun updateRegistrationPrice(actorId: String, registrationId: String, newPricingType: PricingType, newPriceAmount: Long): AppResult<Registration> = error("")
        override suspend fun updatePaymentStatus(actorId: String, registrationId: String, newStatus: PaymentStatus): AppResult<Registration> = error("")
    }

    private class FakePartRepo(private val participant: Participant?) : ParticipantRepository {
        override fun observeParticipants(includeInactive: Boolean): Flow<AppResult<ParticipantListSnapshot>> =
            flowOf(AppResult.Success(ParticipantListSnapshot(listOfNotNull(participant), false)))

        override fun observeParticipant(participantId: String): Flow<AppResult<ParticipantDetailSnapshot>> =
            flowOf(AppResult.Success(ParticipantDetailSnapshot(participant, false)))

        override suspend fun getParticipant(participantId: String): AppResult<Participant?> =
            AppResult.Success(participant)

        override suspend fun findParticipantByPhone(phoneNormalized: String): AppResult<Participant?> =
            AppResult.Success(participant)

        override suspend fun createParticipant(participant: Participant): AppResult<Participant> = error("")
        override suspend fun updateParticipant(actorId: String, participant: Participant): AppResult<Participant> = error("")
        override suspend fun archiveParticipant(actorId: String, participantId: String): AppResult<Unit> = error("")
    }

    private class FakeTrainingRepo(private val training: Training?) : TrainingRepository {
        override fun observeTrainings(includeArchived: Boolean): Flow<AppResult<TrainingListSnapshot>> =
            flowOf(AppResult.Success(TrainingListSnapshot(emptyList(), false)))

        override fun observeTraining(trainingId: String): Flow<AppResult<TrainingDetailSnapshot>> =
            flowOf(AppResult.Success(TrainingDetailSnapshot(training, false)))

        override suspend fun getTraining(trainingId: String): AppResult<Training?> =
            AppResult.Success(training)

        override suspend fun createTraining(training: Training): AppResult<Training> = error("")
        override suspend fun updateTraining(actorId: String, training: Training): AppResult<Training> = error("")
        override suspend fun archiveTraining(actorId: String, trainingId: String): AppResult<Unit> = error("")
    }

    private class FakeAppSettingsRepo : AppSettingsRepository {
        override fun observeAppSettings(): Flow<AppSettings> =
            flowOf(AppSettings("Komunitas E-Pelatihan", 1, null, null))
        override suspend fun getAppSettings(): AppSettings =
            AppSettings("Komunitas E-Pelatihan", 1, null, null)
        override suspend fun updateCommunityName(actorId: String, communityName: String): AppResult<AppSettings> =
            error("Not needed")
    }

    private class FakeWhatsAppLauncher(var shouldSucceed: Boolean = true) : WhatsAppLauncher {
        var launchedPhone: String? = null
        var launchedMessage: String? = null

        override fun openWhatsApp(phoneNormalized: String, message: String): AppResult<Unit> {
            return if (shouldSucceed) {
                launchedPhone = phoneNormalized
                launchedMessage = message
                AppResult.Success(Unit)
            } else {
                AppResult.Failure(IllegalStateException("WhatsApp tidak dapat dibuka pada perangkat ini."))
            }
        }
    }

    private class FakeWhatsAppLogRepo : WhatsAppLogRepository {
        val recordedLogs = mutableListOf<WhatsAppOpenLog>()

        override suspend fun recordOpenLog(
            actorId: String,
            registrationId: String,
            recipientPhone: String,
            messageSnapshot: String,
        ): AppResult<WhatsAppOpenLog> {
            val log = WhatsAppOpenLog(
                id = "log-${recordedLogs.size}",
                registrationId = registrationId,
                recipientPhone = recipientPhone,
                messageSnapshot = messageSnapshot,
                openedBy = actorId,
                openedAt = Instant.now(),
            )
            recordedLogs.add(log)
            return AppResult.Success(log)
        }

        override fun observeLogsByRegistration(registrationId: String): Flow<List<WhatsAppOpenLog>> =
            flowOf(recordedLogs.filter { it.registrationId == registrationId })
    }

    @Test
    fun `opening whatsapp for paid participant succeeds, launches intent and records log`() = runBlocking {
        val regRepo = FakeRegRepo(mapOf("reg-paid" to paidRegistration))
        val partRepo = FakePartRepo(sampleParticipant)
        val trainingRepo = FakeTrainingRepo(sampleTraining)
        val authRepo = FakeAuthRepo(userProfile)
        val settingsRepo = FakeAppSettingsRepo()
        val launcher = FakeWhatsAppLauncher(shouldSucceed = true)
        val logRepo = FakeWhatsAppLogRepo()

        val useCase = OpenWhatsAppUseCase(
            registrationRepository = regRepo,
            participantRepository = partRepo,
            trainingRepository = trainingRepo,
            appSettingsRepository = settingsRepo,
            authRepository = authRepo,
            buildWhatsAppMessageUseCase = BuildWhatsAppMessageUseCase(),
            whatsAppLauncher = launcher,
            whatsAppLogRepository = logRepo,
        )

        val result = useCase("reg-paid")

        assertTrue(result is AppResult.Success)
        assertEquals("6281234567890", launcher.launchedPhone)
        assertTrue(launcher.launchedMessage?.contains("Halo Budi Hartono") == true)
        assertEquals(1, logRepo.recordedLogs.size)
        assertEquals("reg-paid", logRepo.recordedLogs[0].registrationId)
        assertEquals("user-1", logRepo.recordedLogs[0].openedBy)
    }

    @Test
    fun `opening whatsapp for unpaid participant fails and does not launch intent`() = runBlocking {
        val regRepo = FakeRegRepo(mapOf("reg-unpaid" to unpaidRegistration))
        val partRepo = FakePartRepo(sampleParticipant)
        val trainingRepo = FakeTrainingRepo(sampleTraining)
        val authRepo = FakeAuthRepo(userProfile)
        val settingsRepo = FakeAppSettingsRepo()
        val launcher = FakeWhatsAppLauncher(shouldSucceed = true)
        val logRepo = FakeWhatsAppLogRepo()

        val useCase = OpenWhatsAppUseCase(
            registrationRepository = regRepo,
            participantRepository = partRepo,
            trainingRepository = trainingRepo,
            appSettingsRepository = settingsRepo,
            authRepository = authRepo,
            buildWhatsAppMessageUseCase = BuildWhatsAppMessageUseCase(),
            whatsAppLauncher = launcher,
            whatsAppLogRepository = logRepo,
        )

        val result = useCase("reg-unpaid")

        assertTrue(result is AppResult.Failure)
        assertEquals(null, launcher.launchedPhone)
        assertEquals(0, logRepo.recordedLogs.size)
    }

    @Test
    fun `opening whatsapp fails when whatsapp is not available on device`() = runBlocking {
        val regRepo = FakeRegRepo(mapOf("reg-paid" to paidRegistration))
        val partRepo = FakePartRepo(sampleParticipant)
        val trainingRepo = FakeTrainingRepo(sampleTraining)
        val authRepo = FakeAuthRepo(userProfile)
        val settingsRepo = FakeAppSettingsRepo()
        val launcher = FakeWhatsAppLauncher(shouldSucceed = false)
        val logRepo = FakeWhatsAppLogRepo()

        val useCase = OpenWhatsAppUseCase(
            registrationRepository = regRepo,
            participantRepository = partRepo,
            trainingRepository = trainingRepo,
            appSettingsRepository = settingsRepo,
            authRepository = authRepo,
            buildWhatsAppMessageUseCase = BuildWhatsAppMessageUseCase(),
            whatsAppLauncher = launcher,
            whatsAppLogRepository = logRepo,
        )

        val result = useCase("reg-paid")

        assertTrue(result is AppResult.Failure)
        assertEquals(0, logRepo.recordedLogs.size)
    }

    @Test
    fun `unauthenticated user fails with AuthorizationException`() = runBlocking {
        val regRepo = FakeRegRepo(mapOf("reg-paid" to paidRegistration))
        val partRepo = FakePartRepo(sampleParticipant)
        val trainingRepo = FakeTrainingRepo(sampleTraining)
        val authRepo = FakeAuthRepo(null)
        val settingsRepo = FakeAppSettingsRepo()
        val launcher = FakeWhatsAppLauncher()
        val logRepo = FakeWhatsAppLogRepo()

        val useCase = OpenWhatsAppUseCase(
            registrationRepository = regRepo,
            participantRepository = partRepo,
            trainingRepository = trainingRepo,
            appSettingsRepository = settingsRepo,
            authRepository = authRepo,
            buildWhatsAppMessageUseCase = BuildWhatsAppMessageUseCase(),
            whatsAppLauncher = launcher,
            whatsAppLogRepository = logRepo,
        )

        val result = useCase("reg-paid")

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).cause is AuthorizationException)
    }
}
