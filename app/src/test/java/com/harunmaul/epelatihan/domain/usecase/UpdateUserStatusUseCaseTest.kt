package com.harunmaul.epelatihan.domain.usecase

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.core.common.AuthorizationException
import com.harunmaul.epelatihan.core.common.ValidationException
import com.harunmaul.epelatihan.domain.model.InvitationStatus
import com.harunmaul.epelatihan.domain.model.UserProfile
import com.harunmaul.epelatihan.domain.model.UserRole
import com.harunmaul.epelatihan.domain.repository.UserRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateUserStatusUseCaseTest {

    private class FakeUserRepository : UserRepository {
        var updateCalled = false
        var actorProfile: UserProfile? = null

        override suspend fun getUserProfile(uid: String): UserProfile? = actorProfile
        override fun observeUserProfile(uid: String): Flow<UserProfile?> = flowOf(actorProfile)
        override fun observeUsers(): Flow<List<UserProfile>> = flowOf(emptyList())
        override suspend fun hasAnyAdmin(): Boolean = true
        override suspend fun bootstrapFirstAdmin(name: String, email: String, password: String): AppResult<UserProfile> = error("Not needed")
        override suspend fun inviteUser(actorId: String, name: String, email: String, role: UserRole): AppResult<UserProfile> = error("Not needed")
        override suspend fun resendInvitation(actorId: String, userId: String, email: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateUserRole(actorId: String, userId: String, newRole: UserRole): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateUserStatus(actorId: String, userId: String, active: Boolean): AppResult<Unit> {
            updateCalled = true
            return AppResult.Success(Unit)
        }
    }

    private fun createAdmin(id: String = "admin-1"): UserProfile = UserProfile(
        id = id,
        displayName = "Admin",
        email = "admin@lembaga.org",
        role = UserRole.ADMIN,
        active = true,
        invitationStatus = InvitationStatus.ACTIVE,
        createdBy = "system",
        createdAt = Instant.now(),
        updatedBy = "system",
        updatedAt = Instant.now(),
    )

    private fun createUser(id: String = "user-1"): UserProfile = UserProfile(
        id = id,
        displayName = "User",
        email = "user@lembaga.org",
        role = UserRole.USER,
        active = true,
        invitationStatus = InvitationStatus.ACTIVE,
        createdBy = "system",
        createdAt = Instant.now(),
        updatedBy = "system",
        updatedAt = Instant.now(),
    )

    @Test
    fun `invoke with non-admin actor fails with AuthorizationException`() = runTest {
        val fakeRepo = FakeUserRepository()
        fakeRepo.actorProfile = createUser()
        val useCase = UpdateUserStatusUseCase(fakeRepo)

        val result = useCase("user-1", "target-user", false)

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).cause is AuthorizationException)
        assertEquals(false, fakeRepo.updateCalled)
    }

    @Test
    fun `invoke to deactivate self fails with ValidationException`() = runTest {
        val fakeRepo = FakeUserRepository()
        fakeRepo.actorProfile = createAdmin("admin-1")
        val useCase = UpdateUserStatusUseCase(fakeRepo)

        val result = useCase("admin-1", "admin-1", false)

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).cause is ValidationException)
        assertEquals(false, fakeRepo.updateCalled)
    }

    @Test
    fun `invoke with valid admin deactivating other user succeeds`() = runTest {
        val fakeRepo = FakeUserRepository()
        fakeRepo.actorProfile = createAdmin("admin-1")
        val useCase = UpdateUserStatusUseCase(fakeRepo)

        val result = useCase("admin-1", "user-2", false)

        assertTrue(result is AppResult.Success)
        assertEquals(true, fakeRepo.updateCalled)
    }
}
