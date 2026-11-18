package com.harunmaul.epelatihan.domain.usecase

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.core.common.AuthorizationException
import com.harunmaul.epelatihan.core.common.InactiveAccountException
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
import com.harunmaul.epelatihan.domain.repository.AuthRepository
import com.harunmaul.epelatihan.domain.repository.RegistrationRepository
import com.harunmaul.epelatihan.domain.repository.TrainingRepository
import java.time.Instant
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerateMonthlyReportUseCaseTest {

    private val adminProfile = UserProfile(
        id = "admin-1",
        displayName = "Admin E-Pelatihan",
        email = "admin@example.com",
        role = UserRole.ADMIN,
        active = true,
        invitationStatus = InvitationStatus.ACTIVE,
        createdBy = "system",
        createdAt = Instant.now(),
        updatedBy = "system",
        updatedAt = Instant.now(),
    )

    private val inactiveProfile = adminProfile.copy(active = false)

    private class FakeAuthRepo(private val profile: UserProfile?) : AuthRepository {
        override fun observeSession(): Flow<UserSession> = flowOf(
            profile?.let { UserSession.Authenticated(it) } ?: UserSession.Unauthenticated,
        )
        override suspend fun login(email: String, password: String): AppResult<UserProfile> =
            profile?.let { AppResult.Success(it) } ?: AppResult.Failure(AuthorizationException("Unauthenticated"))
        override suspend fun sendPasswordReset(email: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun getCurrentUserProfile(): UserProfile? = profile
        override suspend fun logout() {}
    }

    private class FakeTrainingRepo(
        private val trainings: List<Training>,
        private val isFromCache: Boolean = false,
    ) : TrainingRepository {
        override fun observeTrainings(includeArchived: Boolean): Flow<AppResult<TrainingListSnapshot>> =
            flowOf(AppResult.Success(TrainingListSnapshot(trainings, isFromCache)))

        override fun observeTraining(trainingId: String): Flow<AppResult<TrainingDetailSnapshot>> =
            flowOf(AppResult.Failure(NoSuchElementException()))

        override suspend fun getTraining(trainingId: String): AppResult<Training?> =
            AppResult.Success(trainings.find { it.id == trainingId })

        override suspend fun createTraining(training: Training): AppResult<Training> = AppResult.Success(training)
        override suspend fun updateTraining(actorId: String, training: Training): AppResult<Training> = AppResult.Success(training)
        override suspend fun archiveTraining(actorId: String, trainingId: String): AppResult<Unit> =
            AppResult.Success(Unit)
    }

    private class FakeRegistrationRepo(
        private val registrationsByTraining: Map<String, List<Registration>>,
    ) : RegistrationRepository {
        override fun observeRegistrationsByTraining(trainingId: String): Flow<AppResult<RegistrationListSnapshot>> =
            flowOf(AppResult.Success(RegistrationListSnapshot(registrationsByTraining[trainingId] ?: emptyList(), false)))

        override fun observeRegistration(registrationId: String): Flow<AppResult<RegistrationDetailSnapshot>> =
            flowOf(AppResult.Failure(NoSuchElementException()))

        override suspend fun getRegistration(registrationId: String): AppResult<Registration?> =
            AppResult.Success(null)

        override suspend fun getRegistrationsByTraining(trainingId: String): AppResult<List<Registration>> =
            AppResult.Success(registrationsByTraining[trainingId] ?: emptyList())

        override suspend fun createRegistration(registration: Registration): AppResult<Registration> =
            AppResult.Success(registration)

        override suspend fun updateRegistrationPrice(
            actorId: String,
            registrationId: String,
            newPricingType: PricingType,
            newPriceAmount: Long,
        ): AppResult<Registration> = error("Not needed")

        override suspend fun updatePaymentStatus(
            actorId: String,
            registrationId: String,
            newStatus: PaymentStatus,
        ): AppResult<Registration> = error("Not needed")
    }

    private fun createTraining(
        id: String,
        name: String,
        startAt: Instant,
        timeZoneId: String = "Asia/Jakarta",
    ): Training = Training(
        id = id,
        name = name,
        description = "Deskripsi $name",
        startAt = startAt,
        endAt = startAt.plusSeconds(7200),
        timeZoneId = timeZoneId,
        speaker = "Pemateri",
        meetingMethod = MeetingMethod.ONLINE,
        zoomUrl = "https://meet.google.com/abc",
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

    private fun createRegistration(
        id: String,
        trainingId: String,
        pricingType: PricingType,
        priceAmount: Long,
        paymentStatus: PaymentStatus,
    ): Registration = Registration(
        id = id,
        trainingId = trainingId,
        participantId = "p-$id",
        pricingType = pricingType,
        priceAmount = priceAmount,
        paymentStatus = paymentStatus,
        paidAt = if (paymentStatus == PaymentStatus.LUNAS) Instant.now() else null,
        paidBy = if (paymentStatus == PaymentStatus.LUNAS) "admin-1" else null,
        createdBy = "admin-1",
        createdAt = Instant.now(),
        updatedBy = "admin-1",
        updatedAt = Instant.now(),
    )

    @Test
    fun `generateReport calculates aggregates and per-training metrics correctly`() = runBlocking {
        // Training 1 in May 2026: 2026-05-10 10:00 WIB
        val t1Start = LocalDateTime.of(2026, 5, 10, 10, 0)
            .atZone(ZoneId.of("Asia/Jakarta")).toInstant()
        val t1 = createTraining("t-1", "Kotlin Coroutines", t1Start)

        // Training 2 in May 2026: 2026-05-20 14:00 WIB
        val t2Start = LocalDateTime.of(2026, 5, 20, 14, 0)
            .atZone(ZoneId.of("Asia/Jakarta")).toInstant()
        val t2 = createTraining("t-2", "Jetpack Compose", t2Start)

        // Registrations for T1:
        // - Reg 1: EARLY_BIRD, 300k, LUNAS
        // - Reg 2: NORMAL, 500k, BELUM_BAYAR
        val r1 = createRegistration("r-1", "t-1", PricingType.EARLY_BIRD, 300_000L, PaymentStatus.LUNAS)
        val r2 = createRegistration("r-2", "t-1", PricingType.NORMAL, 500_000L, PaymentStatus.BELUM_BAYAR)

        // Registrations for T2:
        // - Reg 3: NORMAL, 400k, LUNAS
        val r3 = createRegistration("r-3", "t-2", PricingType.NORMAL, 400_000L, PaymentStatus.LUNAS)

        val fakeAuth = FakeAuthRepo(adminProfile)
        val fakeTraining = FakeTrainingRepo(listOf(t1, t2))
        val fakeReg = FakeRegistrationRepo(
            mapOf(
                "t-1" to listOf(r1, r2),
                "t-2" to listOf(r3),
            ),
        )

        val useCase = GenerateMonthlyReportUseCase(fakeTraining, fakeReg, fakeAuth)
        val result = useCase(YearMonth.of(2026, 5))

        assertTrue(result is AppResult.Success)
        val report = (result as AppResult.Success).value

        assertEquals(YearMonth.of(2026, 5), report.month)
        assertEquals(2, report.trainingCount)
        assertEquals(3, report.registrationCount)
        assertEquals(2, report.paidCount)
        assertEquals(1, report.unpaidCount)
        assertEquals(1, report.earlyBirdCount)
        assertEquals(2, report.normalCount)
        assertEquals(1_200_000L, report.totalBilled)
        assertEquals(700_000L, report.totalIncome)
        assertEquals(500_000L, report.outstanding)

        assertEquals(2, report.trainings.size)

        val row1 = report.trainings[0]
        assertEquals("t-1", row1.training.id)
        assertEquals(2, row1.registrationCount)
        assertEquals(1, row1.paidCount)
        assertEquals(1, row1.unpaidCount)
        assertEquals(1, row1.earlyBirdCount)
        assertEquals(1, row1.normalCount)
        assertEquals(800_000L, row1.totalBilled)
        assertEquals(300_000L, row1.totalIncome)
        assertEquals(500_000L, row1.outstanding)

        val row2 = report.trainings[1]
        assertEquals("t-2", row2.training.id)
        assertEquals(1, row2.registrationCount)
        assertEquals(1, row2.paidCount)
        assertEquals(0, row2.unpaidCount)
        assertEquals(0, row2.earlyBirdCount)
        assertEquals(1, row2.normalCount)
        assertEquals(400_000L, row2.totalBilled)
        assertEquals(400_000L, row2.totalIncome)
        assertEquals(0L, row2.outstanding)
    }

    @Test
    fun `generateReport filters out trainings from other months`() = runBlocking {
        // Training in May 2026
        val tMayStart = LocalDateTime.of(2026, 5, 10, 10, 0)
            .atZone(ZoneId.of("Asia/Jakarta")).toInstant()
        val tMay = createTraining("t-may", "Pelatihan Mei", tMayStart)

        // Training in June 2026
        val tJuneStart = LocalDateTime.of(2026, 6, 1, 10, 0)
            .atZone(ZoneId.of("Asia/Jakarta")).toInstant()
        val tJune = createTraining("t-june", "Pelatihan Juni", tJuneStart)

        val fakeAuth = FakeAuthRepo(adminProfile)
        val fakeTraining = FakeTrainingRepo(listOf(tMay, tJune))
        val fakeReg = FakeRegistrationRepo(emptyMap())

        val useCase = GenerateMonthlyReportUseCase(fakeTraining, fakeReg, fakeAuth)
        val result = useCase(YearMonth.of(2026, 5))

        assertTrue(result is AppResult.Success)
        val report = (result as AppResult.Success).value
        assertEquals(1, report.trainingCount)
        assertEquals("t-may", report.trainings.first().training.id)
    }

    @Test
    fun `generateReport respects training timezone when determining month`() = runBlocking {
        // Instant at UTC: 2026-05-31T23:30:00Z -> In Asia/Jakarta (UTC+7) it is 2026-06-01 06:30 (June!)
        val instantNearMidnight = LocalDateTime.of(2026, 5, 31, 23, 30)
            .toInstant(ZoneOffset.UTC)

        val tJakarta = createTraining("t-wib", "Pelatihan Subuh", instantNearMidnight, timeZoneId = "Asia/Jakarta")

        val fakeAuth = FakeAuthRepo(adminProfile)
        val fakeTraining = FakeTrainingRepo(listOf(tJakarta))
        val fakeReg = FakeRegistrationRepo(emptyMap())

        val useCase = GenerateMonthlyReportUseCase(fakeTraining, fakeReg, fakeAuth)

        // Should not be in May report
        val mayReport = (useCase(YearMonth.of(2026, 5)) as AppResult.Success).value
        assertEquals(0, mayReport.trainingCount)

        // Should be in June report
        val juneReport = (useCase(YearMonth.of(2026, 6)) as AppResult.Success).value
        assertEquals(1, juneReport.trainingCount)
        assertEquals("t-wib", juneReport.trainings.first().training.id)
    }

    @Test
    fun `generateReport returns failure when user is unauthenticated`() = runBlocking {
        val fakeAuth = FakeAuthRepo(null)
        val fakeTraining = FakeTrainingRepo(emptyList())
        val fakeReg = FakeRegistrationRepo(emptyMap())

        val useCase = GenerateMonthlyReportUseCase(fakeTraining, fakeReg, fakeAuth)
        val result = useCase(YearMonth.of(2026, 5))

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).cause is AuthorizationException)
    }

    @Test
    fun `generateReport returns failure when user account is inactive`() = runBlocking {
        val fakeAuth = FakeAuthRepo(inactiveProfile)
        val fakeTraining = FakeTrainingRepo(emptyList())
        val fakeReg = FakeRegistrationRepo(emptyMap())

        val useCase = GenerateMonthlyReportUseCase(fakeTraining, fakeReg, fakeAuth)
        val result = useCase(YearMonth.of(2026, 5))

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).cause is InactiveAccountException)
    }
}
