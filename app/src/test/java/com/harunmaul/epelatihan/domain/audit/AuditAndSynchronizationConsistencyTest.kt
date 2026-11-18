package com.harunmaul.epelatihan.domain.audit

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.domain.model.AppSettings
import com.harunmaul.epelatihan.domain.model.AuditLog
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
import com.harunmaul.epelatihan.domain.repository.AuditLogRepository
import com.harunmaul.epelatihan.domain.repository.AuthRepository
import com.harunmaul.epelatihan.domain.repository.RegistrationRepository
import com.harunmaul.epelatihan.domain.repository.TrainingRepository
import com.harunmaul.epelatihan.domain.usecase.GenerateMonthlyReportUseCase
import com.harunmaul.epelatihan.domain.usecase.UpdateCommunityNameUseCase
import java.io.OutputStream
import java.time.Instant
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuditAndSynchronizationConsistencyTest {

    private val adminProfile = UserProfile(
        id = "admin-1",
        displayName = "Admin",
        email = "admin@example.com",
        role = UserRole.ADMIN,
        active = true,
        invitationStatus = InvitationStatus.ACTIVE,
        createdBy = "admin-1",
        createdAt = Instant.now(),
        updatedBy = "admin-1",
        updatedAt = Instant.now(),
    )

    private class RecordingAuditLogRepo : AuditLogRepository {
        val recordedLogs = mutableListOf<AuditLogEntry>()

        data class AuditLogEntry(
            val actorId: String,
            val action: String,
            val entityType: String,
            val entityId: String,
            val before: Map<String, Any?>,
            val after: Map<String, Any?>,
        )

        override suspend fun recordLog(
            actorId: String,
            action: String,
            entityType: String,
            entityId: String,
            before: Map<String, Any?>,
            after: Map<String, Any?>,
        ): AppResult<Unit> {
            recordedLogs.add(
                AuditLogEntry(
                    actorId = actorId,
                    action = action,
                    entityType = entityType,
                    entityId = entityId,
                    before = before,
                    after = after,
                ),
            )
            return AppResult.Success(Unit)
        }

        override fun observeAuditLogs(limit: Int): Flow<List<AuditLog>> = flowOf(emptyList())
    }

    private class MemoryAppSettingsRepo(var currentName: String = "E-Pelatihan") : AppSettingsRepository {
        override fun observeAppSettings(): Flow<AppSettings> =
            flowOf(AppSettings(currentName, 1, "admin-1", Instant.now()))

        override suspend fun getAppSettings(): AppSettings =
            AppSettings(currentName, 1, "admin-1", Instant.now())

        override suspend fun updateCommunityName(actorId: String, communityName: String): AppResult<AppSettings> {
            currentName = communityName
            return AppResult.Success(AppSettings(communityName, 1, actorId, Instant.now()))
        }
    }

    private class MemoryAuthRepo(private val profile: UserProfile) : AuthRepository {
        override fun observeSession(): Flow<UserSession> = flowOf(UserSession.Authenticated(profile))
        override suspend fun login(email: String, password: String): AppResult<UserProfile> = AppResult.Success(profile)
        override suspend fun logout() {}
        override suspend fun sendPasswordReset(email: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun getCurrentUserProfile(): UserProfile? = profile
    }

    @Test
    fun `historical registration prices remain consistent when training master prices change`() = runBlocking {
        // Pelatihan awal memiliki harga Normal 500.000 dan Early Bird 300.000
        val trainingId = "t-100"
        val initialTraining = Training(
            id = trainingId,
            name = "Android Masterclass",
            description = "Deskripsi",
            startAt = Instant.parse("2026-05-15T10:00:00Z"),
            endAt = Instant.parse("2026-05-15T12:00:00Z"),
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

        // Pendaftaran dibuat dengan snapshot harga 300.000 (Early Bird)
        val initialRegistration = Registration(
            id = "reg-1",
            trainingId = trainingId,
            participantId = "p-1",
            pricingType = PricingType.EARLY_BIRD,
            priceAmount = 300_000,
            paymentStatus = PaymentStatus.LUNAS,
            paidAt = Instant.now(),
            paidBy = "admin-1",
            createdBy = "admin-1",
            createdAt = Instant.now(),
            updatedBy = "admin-1",
            updatedAt = Instant.now(),
        )

        // Master harga pelatihan kemudian dinaikkan menjadi Normal 1.000.000 dan Early Bird 700.000
        val modifiedTraining = initialTraining.copy(
            normalPrice = 1_000_000,
            earlyBirdPrice = 700_000,
            updatedAt = Instant.now(),
        )

        val fakeAuth = MemoryAuthRepo(adminProfile)
        val fakeTrainingRepo = object : TrainingRepository {
            override fun observeTrainings(includeArchived: Boolean): Flow<AppResult<TrainingListSnapshot>> =
                flowOf(AppResult.Success(TrainingListSnapshot(listOf(modifiedTraining), false)))
            override fun observeTraining(trainingId: String): Flow<AppResult<TrainingDetailSnapshot>> =
                flowOf(AppResult.Success(TrainingDetailSnapshot(modifiedTraining, false)))
            override suspend fun getTraining(trainingId: String): AppResult<Training?> = AppResult.Success(modifiedTraining)
            override suspend fun createTraining(training: Training): AppResult<Training> = AppResult.Success(training)
            override suspend fun updateTraining(actorId: String, training: Training): AppResult<Training> = AppResult.Success(training)
            override suspend fun archiveTraining(actorId: String, trainingId: String): AppResult<Unit> = AppResult.Success(Unit)
        }

        val fakeRegRepo = object : RegistrationRepository {
            override fun observeRegistrationsByTraining(trainingId: String): Flow<AppResult<RegistrationListSnapshot>> =
                flowOf(AppResult.Success(RegistrationListSnapshot(listOf(initialRegistration), false)))
            override fun observeRegistration(registrationId: String): Flow<AppResult<RegistrationDetailSnapshot>> =
                flowOf(AppResult.Success(RegistrationDetailSnapshot(initialRegistration, false)))
            override suspend fun getRegistration(registrationId: String): AppResult<Registration?> = AppResult.Success(initialRegistration)
            override suspend fun getRegistrationsByTraining(trainingId: String): AppResult<List<Registration>> =
                AppResult.Success(listOf(initialRegistration))
            override suspend fun createRegistration(registration: Registration): AppResult<Registration> = AppResult.Success(registration)
            override suspend fun updateRegistrationPrice(actorId: String, registrationId: String, newPricingType: PricingType, newPriceAmount: Long): AppResult<Registration> = error("")
            override suspend fun updatePaymentStatus(actorId: String, registrationId: String, newStatus: PaymentStatus): AppResult<Registration> = error("")
        }

        // Laporan bulanan harus tetap menghitung harga snapshot pendaftaran lama (300.000), BUKAN harga master yang baru (700.000/1.000.000)
        val generateReportUseCase = GenerateMonthlyReportUseCase(fakeTrainingRepo, fakeRegRepo, fakeAuth)
        val result = generateReportUseCase(YearMonth.of(2026, 5))

        assertTrue(result is AppResult.Success)
        val report = (result as AppResult.Success).value
        assertEquals(300_000L, report.totalBilled)
        assertEquals(300_000L, report.totalIncome)
        assertEquals(0L, report.outstanding)
        assertEquals(300_000L, report.trainings.first().totalBilled)
    }

    @Test
    fun `critical operations write audit logs with actor and state diff`() = runBlocking {
        val auditRepo = RecordingAuditLogRepo()
        val authRepo = MemoryAuthRepo(adminProfile)
        val settingsRepo = MemoryAppSettingsRepo("Komunitas Lama")

        val updateCommunityUseCase = UpdateCommunityNameUseCase(settingsRepo, auditRepo, authRepo)
        val result = updateCommunityUseCase("Komunitas Baru")

        assertTrue(result is AppResult.Success)
        assertEquals(1, auditRepo.recordedLogs.size)

        val log = auditRepo.recordedLogs.first()
        assertEquals("admin-1", log.actorId)
        assertEquals("UPDATE_COMMUNITY_NAME", log.action)
        assertEquals("APP_SETTINGS", log.entityType)
        assertEquals("general", log.entityId)
        assertEquals("Komunitas Lama", log.before["communityName"])
        assertEquals("Komunitas Baru", log.after["communityName"])
    }
}
