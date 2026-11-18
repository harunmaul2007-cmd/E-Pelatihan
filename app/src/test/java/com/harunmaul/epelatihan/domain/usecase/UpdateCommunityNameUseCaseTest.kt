package com.harunmaul.epelatihan.domain.usecase

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.core.common.AuthorizationException
import com.harunmaul.epelatihan.core.common.InactiveAccountException
import com.harunmaul.epelatihan.core.common.ValidationException
import com.harunmaul.epelatihan.domain.model.AppSettings
import com.harunmaul.epelatihan.domain.model.AuditLog
import com.harunmaul.epelatihan.domain.model.InvitationStatus
import com.harunmaul.epelatihan.domain.model.UserProfile
import com.harunmaul.epelatihan.domain.model.UserRole
import com.harunmaul.epelatihan.domain.model.UserSession
import com.harunmaul.epelatihan.domain.repository.AppSettingsRepository
import com.harunmaul.epelatihan.domain.repository.AuditLogRepository
import com.harunmaul.epelatihan.domain.repository.AuthRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCommunityNameUseCaseTest {

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

    private val inactiveAdminProfile = adminProfile.copy(active = false)

    private class FakeAuthRepo(private val profile: UserProfile?) : AuthRepository {
        override fun observeSession(): Flow<UserSession> = flowOf(
            profile?.let { UserSession.Authenticated(it) } ?: UserSession.Unauthenticated,
        )
        override suspend fun login(email: String, password: String): AppResult<UserProfile> = error("Not needed")
        override suspend fun logout() {}
        override suspend fun sendPasswordReset(email: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun getCurrentUserProfile(): UserProfile? = profile
    }

    private class FakeAppSettingsRepo(initialName: String = "E-Pelatihan") : AppSettingsRepository {
        var currentSettings = AppSettings(
            communityName = initialName,
            schemaVersion = 1,
            updatedBy = null,
            updatedAt = null,
        )
        var lastActorId: String? = null

        override fun observeAppSettings(): Flow<AppSettings> = flowOf(currentSettings)
        override suspend fun getAppSettings(): AppSettings = currentSettings

        override suspend fun updateCommunityName(actorId: String, communityName: String): AppResult<AppSettings> {
            lastActorId = actorId
            currentSettings = currentSettings.copy(
                communityName = communityName,
                updatedBy = actorId,
                updatedAt = Instant.now(),
            )
            return AppResult.Success(currentSettings)
        }
    }

    private class FakeAuditLogRepo : AuditLogRepository {
        var lastActorId: String? = null
        var lastAction: String? = null
        var lastEntityType: String? = null
        var lastEntityId: String? = null
        var lastBefore: Map<String, Any?> = emptyMap()
        var lastAfter: Map<String, Any?> = emptyMap()

        override suspend fun recordLog(
            actorId: String,
            action: String,
            entityType: String,
            entityId: String,
            before: Map<String, Any?>,
            after: Map<String, Any?>,
        ): AppResult<Unit> {
            lastActorId = actorId
            lastAction = action
            lastEntityType = entityType
            lastEntityId = entityId
            lastBefore = before
            lastAfter = after
            return AppResult.Success(Unit)
        }

        override fun observeAuditLogs(limit: Int): Flow<List<AuditLog>> = flowOf(emptyList())
    }

    @Test
    fun `invoke updates community name and records audit log when admin is active`() = runBlocking {
        val fakeAuth = FakeAuthRepo(adminProfile)
        val fakeSettings = FakeAppSettingsRepo("Nama Lama")
        val fakeAudit = FakeAuditLogRepo()

        val useCase = UpdateCommunityNameUseCase(fakeSettings, fakeAudit, fakeAuth)
        val result = useCase("   Komunitas Baru Indonesia   ")

        assertTrue(result is AppResult.Success)
        val updated = (result as AppResult.Success).value
        assertEquals("Komunitas Baru Indonesia", updated.communityName)
        assertEquals("admin-1", fakeSettings.lastActorId)

        // Verify audit log
        assertEquals("admin-1", fakeAudit.lastActorId)
        assertEquals("UPDATE_COMMUNITY_NAME", fakeAudit.lastAction)
        assertEquals("APP_SETTINGS", fakeAudit.lastEntityType)
        assertEquals("general", fakeAudit.lastEntityId)
        assertEquals("Nama Lama", fakeAudit.lastBefore["communityName"])
        assertEquals("Komunitas Baru Indonesia", fakeAudit.lastAfter["communityName"])
    }

    @Test
    fun `invoke returns failure when user has USER role`() = runBlocking {
        val fakeAuth = FakeAuthRepo(userProfile)
        val fakeSettings = FakeAppSettingsRepo()
        val fakeAudit = FakeAuditLogRepo()

        val useCase = UpdateCommunityNameUseCase(fakeSettings, fakeAudit, fakeAuth)
        val result = useCase("Komunitas Baru")

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).cause is AuthorizationException)
    }

    @Test
    fun `invoke returns failure when admin is inactive`() = runBlocking {
        val fakeAuth = FakeAuthRepo(inactiveAdminProfile)
        val fakeSettings = FakeAppSettingsRepo()
        val fakeAudit = FakeAuditLogRepo()

        val useCase = UpdateCommunityNameUseCase(fakeSettings, fakeAudit, fakeAuth)
        val result = useCase("Komunitas Baru")

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).cause is InactiveAccountException)
    }

    @Test
    fun `invoke returns failure when unauthenticated`() = runBlocking {
        val fakeAuth = FakeAuthRepo(null)
        val fakeSettings = FakeAppSettingsRepo()
        val fakeAudit = FakeAuditLogRepo()

        val useCase = UpdateCommunityNameUseCase(fakeSettings, fakeAudit, fakeAuth)
        val result = useCase("Komunitas Baru")

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).cause is AuthorizationException)
    }

    @Test
    fun `invoke returns failure when community name is blank`() = runBlocking {
        val fakeAuth = FakeAuthRepo(adminProfile)
        val fakeSettings = FakeAppSettingsRepo()
        val fakeAudit = FakeAuditLogRepo()

        val useCase = UpdateCommunityNameUseCase(fakeSettings, fakeAudit, fakeAuth)
        val result = useCase("    ")

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).cause is ValidationException)
    }

    @Test
    fun `invoke returns failure when community name exceeds 100 chars`() = runBlocking {
        val fakeAuth = FakeAuthRepo(adminProfile)
        val fakeSettings = FakeAppSettingsRepo()
        val fakeAudit = FakeAuditLogRepo()

        val useCase = UpdateCommunityNameUseCase(fakeSettings, fakeAudit, fakeAuth)
        val longName = "A".repeat(101)
        val result = useCase(longName)

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).cause is ValidationException)
    }
}
