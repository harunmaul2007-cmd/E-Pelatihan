package com.harunmaul.epelatihan.domain.usecase

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.core.common.AuthorizationException
import com.harunmaul.epelatihan.core.common.InactiveAccountException
import com.harunmaul.epelatihan.domain.exporter.ReportExporter
import com.harunmaul.epelatihan.domain.model.AppSettings
import com.harunmaul.epelatihan.domain.model.InvitationStatus
import com.harunmaul.epelatihan.domain.model.MonthlyReport
import com.harunmaul.epelatihan.domain.model.UserProfile
import com.harunmaul.epelatihan.domain.model.UserRole
import com.harunmaul.epelatihan.domain.model.UserSession
import com.harunmaul.epelatihan.domain.repository.AppSettingsRepository
import com.harunmaul.epelatihan.domain.repository.AuthRepository
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.time.Instant
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportReportPdfUseCaseTest {

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

    private val inactiveProfile = adminProfile.copy(active = false)

    private val sampleReport = MonthlyReport(
        month = YearMonth.of(2026, 5),
        trainingCount = 0,
        registrationCount = 0,
        paidCount = 0,
        unpaidCount = 0,
        earlyBirdCount = 0,
        normalCount = 0,
        totalBilled = 0,
        totalIncome = 0,
        outstanding = 0,
        trainings = emptyList(),
        isFromCache = false,
    )

    private class FakeAuthRepo(private val profile: UserProfile?) : AuthRepository {
        override fun observeSession(): Flow<UserSession> = flowOf(
            profile?.let { UserSession.Authenticated(it) } ?: UserSession.Unauthenticated,
        )
        override suspend fun login(email: String, password: String): AppResult<UserProfile> = error("Not needed")
        override suspend fun logout() {}
        override suspend fun sendPasswordReset(email: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun getCurrentUserProfile(): UserProfile? = profile
    }

    private class FakeAppSettingsRepo(private val communityName: String) : AppSettingsRepository {
        override fun observeAppSettings(): Flow<AppSettings> = flowOf(
            AppSettings(communityName, 1, "admin-1", Instant.now()),
        )
        override suspend fun getAppSettings(): AppSettings =
            AppSettings(communityName, 1, "admin-1", Instant.now())
    }

    private class FakeReportExporter : ReportExporter {
        var lastExportedPdfReport: MonthlyReport? = null
        var lastCommunityName: String? = null
        var shouldThrow: Boolean = false

        override suspend fun exportPdf(report: MonthlyReport, communityName: String, outputStream: OutputStream) {
            if (shouldThrow) throw IllegalStateException("PDF failure")
            lastExportedPdfReport = report
            lastCommunityName = communityName
            outputStream.write("dummy-pdf".toByteArray())
        }

        override suspend fun exportXlsx(report: MonthlyReport, communityName: String, outputStream: OutputStream) {
            error("Not needed")
        }
    }

    @Test
    fun `invoke calls exporter with correct community name and report`() = runBlocking {
        val fakeAuth = FakeAuthRepo(adminProfile)
        val fakeSettings = FakeAppSettingsRepo("Komunitas Android Jogja")
        val fakeExporter = FakeReportExporter()

        val useCase = ExportReportPdfUseCase(fakeExporter, fakeSettings, fakeAuth)
        val out = ByteArrayOutputStream()
        val result = useCase(sampleReport, out)

        assertTrue(result is AppResult.Success)
        assertEquals(sampleReport, fakeExporter.lastExportedPdfReport)
        assertEquals("Komunitas Android Jogja", fakeExporter.lastCommunityName)
        assertEquals("dummy-pdf", out.toString())
    }

    @Test
    fun `invoke returns failure when user is unauthenticated`() = runBlocking {
        val fakeAuth = FakeAuthRepo(null)
        val fakeSettings = FakeAppSettingsRepo("Komunitas")
        val fakeExporter = FakeReportExporter()

        val useCase = ExportReportPdfUseCase(fakeExporter, fakeSettings, fakeAuth)
        val result = useCase(sampleReport, ByteArrayOutputStream())

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).cause is AuthorizationException)
    }

    @Test
    fun `invoke returns failure when user is inactive`() = runBlocking {
        val fakeAuth = FakeAuthRepo(inactiveProfile)
        val fakeSettings = FakeAppSettingsRepo("Komunitas")
        val fakeExporter = FakeReportExporter()

        val useCase = ExportReportPdfUseCase(fakeExporter, fakeSettings, fakeAuth)
        val result = useCase(sampleReport, ByteArrayOutputStream())

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).cause is InactiveAccountException)
    }

    @Test
    fun `invoke returns failure when exporter throws exception`() = runBlocking {
        val fakeAuth = FakeAuthRepo(adminProfile)
        val fakeSettings = FakeAppSettingsRepo("Komunitas")
        val fakeExporter = FakeReportExporter().apply { shouldThrow = true }

        val useCase = ExportReportPdfUseCase(fakeExporter, fakeSettings, fakeAuth)
        val result = useCase(sampleReport, ByteArrayOutputStream())

        assertTrue(result is AppResult.Failure)
        assertEquals("PDF failure", (result as AppResult.Failure).cause.message)
    }
}
