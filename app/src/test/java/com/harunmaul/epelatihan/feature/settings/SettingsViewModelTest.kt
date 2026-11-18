package com.harunmaul.epelatihan.feature.settings

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.core.common.NetworkException
import com.harunmaul.epelatihan.domain.model.AppSettings
import com.harunmaul.epelatihan.domain.model.AuditLog
import com.harunmaul.epelatihan.domain.model.InvitationStatus
import com.harunmaul.epelatihan.domain.model.UserProfile
import com.harunmaul.epelatihan.domain.model.UserRole
import com.harunmaul.epelatihan.domain.model.UserSession
import com.harunmaul.epelatihan.domain.repository.AppSettingsRepository
import com.harunmaul.epelatihan.domain.repository.AuditLogRepository
import com.harunmaul.epelatihan.domain.repository.AuthRepository
import com.harunmaul.epelatihan.domain.usecase.ObserveAppSettingsUseCase
import com.harunmaul.epelatihan.domain.usecase.ObserveSessionUseCase
import com.harunmaul.epelatihan.domain.usecase.UpdateCommunityNameUseCase
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

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

    private val userProfile = UserProfile(
        id = "user-1",
        displayName = "User",
        email = "user@example.com",
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

    private class FakeAppSettingsRepo(initialName: String = "E-Pelatihan") : AppSettingsRepository {
        val flow = MutableStateFlow(
            AppSettings(
                communityName = initialName,
                schemaVersion = 1,
                updatedBy = null,
                updatedAt = null,
            ),
        )

        override fun observeAppSettings(): Flow<AppSettings> = flow
        override suspend fun getAppSettings(): AppSettings = flow.value

        override suspend fun updateCommunityName(actorId: String, communityName: String): AppResult<AppSettings> {
            val updated = flow.value.copy(communityName = communityName, updatedBy = actorId)
            flow.value = updated
            return AppResult.Success(updated)
        }
    }

    private class FakeAuthRepo(private val profile: UserProfile?) : AuthRepository {
        override fun observeSession(): Flow<UserSession> = flowOf(
            profile?.let { UserSession.Authenticated(it) } ?: UserSession.Unauthenticated,
        )
        override suspend fun login(email: String, password: String): AppResult<UserProfile> = error("Not needed")
        override suspend fun logout() {}
        override suspend fun sendPasswordReset(email: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun getCurrentUserProfile(): UserProfile? = profile
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

    private class FailingUpdateCommunityNameUseCase(
        settingsRepo: AppSettingsRepository,
        auditRepo: AuditLogRepository,
        authRepo: AuthRepository,
    ) : UpdateCommunityNameUseCase(settingsRepo, auditRepo, authRepo) {
        override suspend fun invoke(communityName: String): AppResult<AppSettings> {
            return AppResult.Failure(NetworkException("Gagal menyimpan ke server"))
        }
    }

    @Test
    fun `init loads current community name and detects admin role`() = runTest(testDispatcher) {
        val fakeSettings = FakeAppSettingsRepo("Komunitas Android")
        val fakeAuth = FakeAuthRepo(adminProfile)
        val fakeAudit = FakeAuditLogRepo()

        val observeSettings = ObserveAppSettingsUseCase(fakeSettings)
        val observeSession = ObserveSessionUseCase(fakeAuth)
        val updateName = UpdateCommunityNameUseCase(fakeSettings, fakeAudit, fakeAuth)

        val viewModel = SettingsViewModel(observeSettings, updateName, observeSession)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.isAdmin)
        assertEquals("Komunitas Android", viewModel.uiState.value.communityName)
        assertEquals("Komunitas Android", viewModel.uiState.value.initialCommunityName)
        assertFalse(viewModel.uiState.value.isDirty)
        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `init detects non-admin role`() = runTest(testDispatcher) {
        val fakeSettings = FakeAppSettingsRepo("Komunitas Android")
        val fakeAuth = FakeAuthRepo(userProfile)
        val fakeAudit = FakeAuditLogRepo()

        val observeSettings = ObserveAppSettingsUseCase(fakeSettings)
        val observeSession = ObserveSessionUseCase(fakeAuth)
        val updateName = UpdateCommunityNameUseCase(fakeSettings, fakeAudit, fakeAuth)

        val viewModel = SettingsViewModel(observeSettings, updateName, observeSession)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isAdmin)
        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `onCommunityNameChanged updates dirty state and validates length`() = runTest(testDispatcher) {
        val fakeSettings = FakeAppSettingsRepo("Komunitas A")
        val fakeAuth = FakeAuthRepo(adminProfile)
        val fakeAudit = FakeAuditLogRepo()

        val observeSettings = ObserveAppSettingsUseCase(fakeSettings)
        val observeSession = ObserveSessionUseCase(fakeAuth)
        val updateName = UpdateCommunityNameUseCase(fakeSettings, fakeAudit, fakeAuth)

        val viewModel = SettingsViewModel(observeSettings, updateName, observeSession)
        advanceUntilIdle()

        viewModel.onCommunityNameChanged("Komunitas B")
        assertTrue(viewModel.uiState.value.isDirty)
        assertTrue(viewModel.uiState.value.canSave)
        assertNull(viewModel.uiState.value.nameError)

        val tooLong = "A".repeat(101)
        viewModel.onCommunityNameChanged(tooLong)
        assertNotNull(viewModel.uiState.value.nameError)
        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `saveSettings success updates name and sets successMessage`() = runTest(testDispatcher) {
        val fakeSettings = FakeAppSettingsRepo("Komunitas A")
        val fakeAuth = FakeAuthRepo(adminProfile)
        val fakeAudit = FakeAuditLogRepo()

        val observeSettings = ObserveAppSettingsUseCase(fakeSettings)
        val observeSession = ObserveSessionUseCase(fakeAuth)
        val updateName = UpdateCommunityNameUseCase(fakeSettings, fakeAudit, fakeAuth)

        val viewModel = SettingsViewModel(observeSettings, updateName, observeSession)
        advanceUntilIdle()

        viewModel.onCommunityNameChanged("Komunitas Baru")
        viewModel.saveSettings()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSaving)
        assertTrue(viewModel.uiState.value.isSaveSuccess)
        assertEquals("Nama komunitas berhasil diperbarui.", viewModel.uiState.value.successMessage)
        assertEquals("Komunitas Baru", fakeSettings.flow.value.communityName)

        viewModel.clearSuccessMessage()
        assertNull(viewModel.uiState.value.successMessage)
    }

    @Test
    fun `saveSettings failure sets errorMessage`() = runTest(testDispatcher) {
        val fakeSettings = FakeAppSettingsRepo("Komunitas A")
        val fakeAuth = FakeAuthRepo(adminProfile)
        val fakeAudit = FakeAuditLogRepo()

        val observeSettings = ObserveAppSettingsUseCase(fakeSettings)
        val observeSession = ObserveSessionUseCase(fakeAuth)
        val failingUpdate = FailingUpdateCommunityNameUseCase(fakeSettings, fakeAudit, fakeAuth)

        val viewModel = SettingsViewModel(observeSettings, failingUpdate, observeSession)
        advanceUntilIdle()

        viewModel.onCommunityNameChanged("Komunitas Baru")
        viewModel.saveSettings()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSaving)
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertEquals("Gagal menyimpan ke server", viewModel.uiState.value.errorMessage)

        viewModel.clearErrorMessage()
        assertNull(viewModel.uiState.value.errorMessage)
    }
}
