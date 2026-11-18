package com.harunmaul.epelatihan.acceptance

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.core.common.AuthorizationException
import com.harunmaul.epelatihan.core.common.ValidationException
import com.harunmaul.epelatihan.data.exporter.XlsxReportGenerator
import com.harunmaul.epelatihan.domain.model.AppSettings
import com.harunmaul.epelatihan.domain.model.AuditLog
import com.harunmaul.epelatihan.domain.model.InvitationStatus
import com.harunmaul.epelatihan.domain.model.MeetingMethod
import com.harunmaul.epelatihan.domain.model.MonthlyReport
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
import com.harunmaul.epelatihan.domain.repository.AuditLogRepository
import com.harunmaul.epelatihan.domain.repository.AuthRepository
import com.harunmaul.epelatihan.domain.repository.ParticipantRepository
import com.harunmaul.epelatihan.domain.repository.RegistrationRepository
import com.harunmaul.epelatihan.domain.repository.TrainingRepository
import com.harunmaul.epelatihan.domain.repository.WhatsAppLauncher
import com.harunmaul.epelatihan.domain.repository.WhatsAppLogRepository
import com.harunmaul.epelatihan.domain.usecase.ArchiveTrainingUseCase
import com.harunmaul.epelatihan.domain.usecase.BuildWhatsAppMessageUseCase
import com.harunmaul.epelatihan.domain.usecase.GenerateMonthlyReportUseCase
import com.harunmaul.epelatihan.domain.usecase.ObserveAppSettingsUseCase
import com.harunmaul.epelatihan.domain.usecase.OpenWhatsAppUseCase
import com.harunmaul.epelatihan.domain.usecase.UpdateCommunityNameUseCase
import com.harunmaul.epelatihan.domain.usecase.UpdatePaymentStatusUseCase
import com.harunmaul.epelatihan.domain.usecase.UpdateRegistrationPriceUseCase
import com.harunmaul.epelatihan.domain.usecase.UpdateTrainingUseCase
import com.harunmaul.epelatihan.domain.usecase.UpdateUserRoleUseCase
import com.harunmaul.epelatihan.domain.usecase.UpdateUserStatusUseCase
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.YearMonth
import java.util.zip.ZipInputStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AcceptanceVerificationTest {

    private val adminProfile = UserProfile(
        id = "admin-1",
        displayName = "Administrator",
        email = "admin@example.com",
        role = UserRole.ADMIN,
        active = true,
        invitationStatus = InvitationStatus.ACTIVE,
        createdBy = "system",
        createdAt = Instant.now(),
        updatedBy = "system",
        updatedAt = Instant.now(),
    )

    private val regularUserProfile = UserProfile(
        id = "user-1",
        displayName = "Petugas Biasa",
        email = "user@example.com",
        role = UserRole.USER,
        active = true,
        invitationStatus = InvitationStatus.ACTIVE,
        createdBy = "admin-1",
        createdAt = Instant.now(),
        updatedBy = "admin-1",
        updatedAt = Instant.now(),
    )

    private class FakeAuthRepository(private var currentProfile: UserProfile?) : AuthRepository {
        fun setProfile(profile: UserProfile?) {
            currentProfile = profile
        }
        override fun observeSession(): Flow<UserSession> = flowOf(
            currentProfile?.let { UserSession.Authenticated(it) } ?: UserSession.Unauthenticated,
        )
        override suspend fun login(email: String, password: String): AppResult<UserProfile> =
            currentProfile?.let { AppResult.Success(it) } ?: AppResult.Failure(AuthorizationException("Unauthenticated"))
        override suspend fun logout() { currentProfile = null }
        override suspend fun sendPasswordReset(email: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun getCurrentUserProfile(): UserProfile? = currentProfile
    }

    private class FakeAuditLogRepo : AuditLogRepository {
        override suspend fun recordLog(
            actorId: String,
            action: String,
            entityType: String,
            entityId: String,
            before: Map<String, Any?>,
            after: Map<String, Any?>,
        ): AppResult<Unit> = AppResult.Success(Unit)

        override fun observeAuditLogs(limit: Int): Flow<List<AuditLog>> = flowOf(emptyList())
    }

    private class FakeAppSettingsRepo(var name: String = "Komunitas E-Pelatihan") : AppSettingsRepository {
        override fun observeAppSettings(): Flow<AppSettings> = flowOf(AppSettings(name, 1, null, null))
        override suspend fun getAppSettings(): AppSettings = AppSettings(name, 1, null, null)
        override suspend fun updateCommunityName(actorId: String, communityName: String): AppResult<AppSettings> {
            name = communityName
            return AppResult.Success(AppSettings(communityName, 1, actorId, Instant.now()))
        }
    }

    @Test
    fun `acceptance criteria 1 - User role is denied from performing admin operations`() = runBlocking {
        val userAuth = FakeAuthRepository(regularUserProfile)
        val dummyAudit = FakeAuditLogRepo()
        val dummySettings = FakeAppSettingsRepo()

        // 1. User cannot update community name
        val updateCommunityName = UpdateCommunityNameUseCase(dummySettings, dummyAudit, userAuth)
        val communityResult = updateCommunityName("Nama Terlarang")
        assertTrue("User must be denied updating community name", communityResult is AppResult.Failure)
        assertTrue((communityResult as AppResult.Failure).cause is AuthorizationException)

        // 2. User cannot update payment status
        val dummyRegRepo = object : RegistrationRepository {
            override fun observeRegistrationsByTraining(trainingId: String): Flow<AppResult<RegistrationListSnapshot>> = flowOf(AppResult.Failure(Exception()))
            override fun observeRegistration(registrationId: String): Flow<AppResult<RegistrationDetailSnapshot>> = flowOf(AppResult.Failure(Exception()))
            override suspend fun getRegistration(registrationId: String): AppResult<Registration?> = AppResult.Success(null)
            override suspend fun getRegistrationsByTraining(trainingId: String): AppResult<List<Registration>> = AppResult.Success(emptyList())
            override suspend fun createRegistration(registration: Registration): AppResult<Registration> = AppResult.Success(registration)
            override suspend fun updateRegistrationPrice(actorId: String, registrationId: String, newPricingType: PricingType, newPriceAmount: Long): AppResult<Registration> = error("")
            override suspend fun updatePaymentStatus(actorId: String, registrationId: String, newStatus: PaymentStatus): AppResult<Registration> = error("")
        }
        val updatePaymentStatus = UpdatePaymentStatusUseCase(dummyRegRepo, userAuth)
        val paymentResult = updatePaymentStatus("reg-1", PaymentStatus.LUNAS)
        assertTrue("User must be denied updating payment status", paymentResult is AppResult.Failure)
        assertTrue((paymentResult as AppResult.Failure).cause is AuthorizationException)

        // 3. User cannot update registration pricing type
        val dummyTrainingRepo = object : TrainingRepository {
            override fun observeTrainings(includeArchived: Boolean): Flow<AppResult<TrainingListSnapshot>> = flowOf(AppResult.Failure(Exception()))
            override fun observeTraining(trainingId: String): Flow<AppResult<TrainingDetailSnapshot>> = flowOf(AppResult.Failure(Exception()))
            override suspend fun getTraining(trainingId: String): AppResult<Training?> = AppResult.Success(null)
            override suspend fun createTraining(training: Training): AppResult<Training> = AppResult.Success(training)
            override suspend fun updateTraining(actorId: String, training: Training): AppResult<Training> = AppResult.Success(training)
            override suspend fun archiveTraining(actorId: String, trainingId: String): AppResult<Unit> = AppResult.Success(Unit)
        }
        val updateRegistrationPrice = UpdateRegistrationPriceUseCase(dummyRegRepo, dummyTrainingRepo, userAuth)
        val priceResult = updateRegistrationPrice("reg-1", PricingType.NORMAL)
        assertTrue("User must be denied updating registration price", priceResult is AppResult.Failure)
        assertTrue((priceResult as AppResult.Failure).cause is AuthorizationException)

        // 4. User cannot archive training
        val archiveTraining = ArchiveTrainingUseCase(dummyTrainingRepo, userAuth)
        val archiveResult = archiveTraining("training-1")
        assertTrue("User must be denied archiving training", archiveResult is AppResult.Failure)
        assertTrue((archiveResult as AppResult.Failure).cause is AuthorizationException)
    }

    @Test
    fun `acceptance criteria 2 - Community name is readable prior to authentication`() = runBlocking {
        val settingsRepo = FakeAppSettingsRepo("Informatika Edukasi")
        val observeSettings = ObserveAppSettingsUseCase(settingsRepo)

        val settings = observeSettings().first()
        assertEquals("Informatika Edukasi", settings.communityName)
    }

    @Test
    fun `acceptance criteria 3 - WhatsApp message action only permits LUNAS payment status`() = runBlocking {
        val dummyAuth = FakeAuthRepository(adminProfile)
        val dummyLauncher = object : WhatsAppLauncher {
            var invoked = false
            override fun openWhatsApp(phoneNormalized: String, message: String): AppResult<Unit> {
                invoked = true
                return AppResult.Success(Unit)
            }
        }
        val dummyWhatsAppLogs = object : WhatsAppLogRepository {
            override suspend fun recordOpenLog(
                actorId: String,
                registrationId: String,
                recipientPhone: String,
                messageSnapshot: String,
            ): AppResult<WhatsAppOpenLog> = AppResult.Success(
                WhatsAppOpenLog(
                    id = "log-1",
                    registrationId = registrationId,
                    recipientPhone = recipientPhone,
                    messageSnapshot = messageSnapshot,
                    openedBy = actorId,
                    openedAt = Instant.now(),
                ),
            )

            override fun observeLogsByRegistration(registrationId: String): Flow<List<WhatsAppOpenLog>> =
                flowOf(emptyList())
        }

        val dummyParticipantRepo = object : ParticipantRepository {
            override fun observeParticipants(includeInactive: Boolean): Flow<AppResult<com.harunmaul.epelatihan.domain.model.ParticipantListSnapshot>> = flowOf(AppResult.Failure(Exception()))
            override fun observeParticipant(participantId: String): Flow<AppResult<com.harunmaul.epelatihan.domain.model.ParticipantDetailSnapshot>> = flowOf(AppResult.Failure(Exception()))
            override suspend fun getParticipant(participantId: String): AppResult<com.harunmaul.epelatihan.domain.model.Participant?> =
                AppResult.Success(
                    com.harunmaul.epelatihan.domain.model.Participant(
                        id = "p-1",
                        name = "Budi",
                        nameSort = "budi",
                        phoneDisplay = "08123456789",
                        phoneNormalized = "628123456789",
                        email = null,
                        notes = null,
                        active = true,
                        createdBy = "admin-1",
                        createdAt = null,
                        updatedBy = "admin-1",
                        updatedAt = null,
                    ),
                )
            override suspend fun createParticipant(participant: com.harunmaul.epelatihan.domain.model.Participant): AppResult<com.harunmaul.epelatihan.domain.model.Participant> = AppResult.Success(participant)
            override suspend fun updateParticipant(actorId: String, participant: com.harunmaul.epelatihan.domain.model.Participant): AppResult<com.harunmaul.epelatihan.domain.model.Participant> = AppResult.Success(participant)
            override suspend fun archiveParticipant(actorId: String, participantId: String): AppResult<Unit> = AppResult.Success(Unit)
            override suspend fun findParticipantByPhone(phoneNormalized: String): AppResult<com.harunmaul.epelatihan.domain.model.Participant?> = AppResult.Success(null)
        }

        val sampleTraining = Training(
            id = "t-1",
            name = "Android",
            description = null,
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
            normalPrice = 100_000,
            earlyBirdPrice = null,
            lifecycle = TrainingLifecycle.ACTIVE,
            createdBy = "admin-1",
            createdAt = null,
            updatedBy = "admin-1",
            updatedAt = null,
        )

        var registrationStatus = PaymentStatus.BELUM_BAYAR
        val dummyRegRepo = object : RegistrationRepository {
            override fun observeRegistrationsByTraining(trainingId: String): Flow<AppResult<RegistrationListSnapshot>> = flowOf(AppResult.Failure(Exception()))
            override fun observeRegistration(registrationId: String): Flow<AppResult<RegistrationDetailSnapshot>> = flowOf(AppResult.Failure(Exception()))
            override suspend fun getRegistration(registrationId: String): AppResult<Registration?> =
                AppResult.Success(
                    Registration(
                        id = "r-1",
                        trainingId = "t-1",
                        participantId = "p-1",
                        pricingType = PricingType.NORMAL,
                        priceAmount = 100_000,
                        paymentStatus = registrationStatus,
                        paidAt = null,
                        paidBy = null,
                        createdBy = "admin-1",
                        createdAt = null,
                        updatedBy = "admin-1",
                        updatedAt = null,
                    ),
                )
            override suspend fun getRegistrationsByTraining(trainingId: String): AppResult<List<Registration>> = AppResult.Success(emptyList())
            override suspend fun createRegistration(registration: Registration): AppResult<Registration> = AppResult.Success(registration)
            override suspend fun updateRegistrationPrice(actorId: String, registrationId: String, newPricingType: PricingType, newPriceAmount: Long): AppResult<Registration> = error("")
            override suspend fun updatePaymentStatus(actorId: String, registrationId: String, newStatus: PaymentStatus): AppResult<Registration> = error("")
        }

        val dummyTrainingRepo = object : TrainingRepository {
            override fun observeTrainings(includeArchived: Boolean): Flow<AppResult<TrainingListSnapshot>> = flowOf(AppResult.Failure(Exception()))
            override fun observeTraining(trainingId: String): Flow<AppResult<TrainingDetailSnapshot>> = flowOf(AppResult.Failure(Exception()))
            override suspend fun getTraining(trainingId: String): AppResult<Training?> = AppResult.Success(sampleTraining)
            override suspend fun createTraining(training: Training): AppResult<Training> = AppResult.Success(training)
            override suspend fun updateTraining(actorId: String, training: Training): AppResult<Training> = AppResult.Success(training)
            override suspend fun archiveTraining(actorId: String, trainingId: String): AppResult<Unit> = AppResult.Success(Unit)
        }

        val dummySettingsRepo = FakeAppSettingsRepo()
        val buildMessage = BuildWhatsAppMessageUseCase()
        val openWhatsAppUseCase = OpenWhatsAppUseCase(
            registrationRepository = dummyRegRepo,
            participantRepository = dummyParticipantRepo,
            trainingRepository = dummyTrainingRepo,
            appSettingsRepository = dummySettingsRepo,
            authRepository = dummyAuth,
            buildWhatsAppMessageUseCase = buildMessage,
            whatsAppLauncher = dummyLauncher,
            whatsAppLogRepository = dummyWhatsAppLogs,
        )

        // When payment status is BELUM_BAYAR, open WhatsApp MUST fail
        registrationStatus = PaymentStatus.BELUM_BAYAR
        val unpaidResult = openWhatsAppUseCase("r-1")
        assertTrue("Opening WhatsApp for unpaid participant must fail", unpaidResult is AppResult.Failure)
        assertFalse(dummyLauncher.invoked)

        // When payment status is LUNAS, open WhatsApp MUST succeed
        registrationStatus = PaymentStatus.LUNAS
        val paidResult = openWhatsAppUseCase("r-1")
        assertTrue("Opening WhatsApp for paid participant must succeed", paidResult is AppResult.Success)
        assertTrue(dummyLauncher.invoked)
    }

    @Test
    fun `acceptance criteria 4 - Screen calculations and XLSX export calculations match identically`() {
        val sampleTraining = Training(
            id = "t-1",
            name = "Pelatihan Kotlin Multiplatform",
            description = null,
            startAt = Instant.parse("2026-05-10T10:00:00Z"),
            endAt = Instant.parse("2026-05-10T12:00:00Z"),
            timeZoneId = "Asia/Jakarta",
            speaker = "Pemateri",
            meetingMethod = MeetingMethod.ONLINE,
            zoomUrl = "https://zoom.us/j/123",
            location = null,
            recordingUrl = null,
            materialUrl = null,
            pricingScheme = PricingScheme.EARLY_BIRD_AND_NORMAL,
            normalPrice = 500_000,
            earlyBirdPrice = 300_000,
            lifecycle = TrainingLifecycle.ACTIVE,
            createdBy = "admin-1",
            createdAt = Instant.now(),
            updatedBy = "admin-1",
            updatedAt = Instant.now(),
        )

        val report = MonthlyReport(
            month = YearMonth.of(2026, 5),
            trainingCount = 1,
            registrationCount = 2,
            paidCount = 1,
            unpaidCount = 1,
            earlyBirdCount = 1,
            normalCount = 1,
            totalBilled = 800_000,
            totalIncome = 300_000,
            outstanding = 500_000,
            trainings = listOf(
                com.harunmaul.epelatihan.domain.model.TrainingReportRow(
                    training = sampleTraining,
                    registrationCount = 2,
                    paidCount = 1,
                    unpaidCount = 1,
                    earlyBirdCount = 1,
                    normalCount = 1,
                    totalBilled = 800_000,
                    totalIncome = 300_000,
                    outstanding = 500_000,
                ),
            ),
            isFromCache = false,
        )

        // Generate XLSX
        val generator = XlsxReportGenerator()
        val out = ByteArrayOutputStream()
        generator.generate(report, "Komunitas E-Pelatihan", out)

        val zip = ZipInputStream(out.toByteArray().inputStream())
        val entries = mutableMapOf<String, String>()
        var entry = zip.nextEntry
        while (entry != null) {
            entries[entry.name] = String(zip.readBytes(), Charsets.UTF_8)
            entry = zip.nextEntry
        }

        val sheet1 = entries["xl/worksheets/sheet1.xml"]
        assertNotNull(sheet1)
        // Verify numbers in sheet1 match the report domain model exactly
        assertTrue("Total Billed in XLSX must match screen calculation", sheet1!!.contains("<v>800000</v>"))
        assertTrue("Total Income in XLSX must match screen calculation", sheet1.contains("<v>300000</v>"))
        assertTrue("Outstanding in XLSX must match screen calculation", sheet1.contains("<v>500000</v>"))

        val sheet2 = entries["xl/worksheets/sheet2.xml"]
        assertNotNull(sheet2)
        assertTrue("Training detail Total Billed must match", sheet2!!.contains("<v>800000</v>"))
        assertTrue("Training detail Total Income must match", sheet2.contains("<v>300000</v>"))
        assertTrue("Training detail Outstanding must match", sheet2.contains("<v>500000</v>"))
    }
}
