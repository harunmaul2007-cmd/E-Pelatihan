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

class InviteUserUseCaseTest {

    private class FakeUserRepository : UserRepository {
        var inviteCalled = false
        var actorProfile: UserProfile? = null

        override suspend fun getUserProfile(uid: String): UserProfile? = actorProfile
        override fun observeUserProfile(uid: String): Flow<UserProfile?> = flowOf(actorProfile)
        override fun observeUsers(): Flow<List<UserProfile>> = flowOf(emptyList())
        override suspend fun hasAnyAdmin(): Boolean = true
        override suspend fun bootstrapFirstAdmin(name: String, email: String, password: String): AppResult<UserProfile> = error("Not needed")
        override suspend fun inviteUser(actorId: String, name: String, email: String, role: UserRole): AppResult<UserProfile> {
            inviteCalled = true
            return AppResult.Success(
                UserProfile(
                    id = "new-uid",
                    displayName = name,
                    email = email,
                    role = role,
                    active = true,
                    invitationStatus = InvitationStatus.INVITED,
                    createdBy = actorId,
                    createdAt = Instant.now(),
                    updatedBy = actorId,
                    updatedAt = Instant.now(),
                ),
            )
        }
        override suspend fun resendInvitation(actorId: String, userId: String, email: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateUserRole(actorId: String, userId: String, newRole: UserRole): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateUserStatus(actorId: String, userId: String, active: Boolean): AppResult<Unit> = AppResult.Success(Unit)
    }

    private fun createAdmin(id: String = "admin-1", active: Boolean = true): UserProfile = UserProfile(
        id = id,
        displayName = "Admin Super",
        email = "admin@lembaga.org",
        role = UserRole.ADMIN,
        active = active,
        invitationStatus = InvitationStatus.ACTIVE,
        createdBy = "system",
        createdAt = Instant.now(),
        updatedBy = "system",
        updatedAt = Instant.now(),
    )

    private fun createUser(id: String = "user-1"): UserProfile = UserProfile(
        id = id,
        displayName = "User Biasa",
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
        val useCase = InviteUserUseCase(fakeRepo)

        val result = useCase("user-1", "Budi", "budi@example.com", UserRole.USER)

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).cause is AuthorizationException)
        assertEquals(false, fakeRepo.inviteCalled)
    }

    @Test
    fun `invoke with inactive admin actor fails with AuthorizationException`() = runTest {
        val fakeRepo = FakeUserRepository()
        fakeRepo.actorProfile = createAdmin(active = false)
        val useCase = InviteUserUseCase(fakeRepo)

        val result = useCase("admin-1", "Budi", "budi@example.com", UserRole.USER)

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).cause is AuthorizationException)
        assertEquals(false, fakeRepo.inviteCalled)
    }

    @Test
    fun `invoke with empty name fails with ValidationException`() = runTest {
        val fakeRepo = FakeUserRepository()
        fakeRepo.actorProfile = createAdmin()
        val useCase = InviteUserUseCase(fakeRepo)

        val result = useCase("admin-1", "", "budi@example.com", UserRole.USER)

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).cause is ValidationException)
        assertEquals(false, fakeRepo.inviteCalled)
    }

    @Test
    fun `invoke with invalid email fails with ValidationException`() = runTest {
        val fakeRepo = FakeUserRepository()
        fakeRepo.actorProfile = createAdmin()
        val useCase = InviteUserUseCase(fakeRepo)

        val result = useCase("admin-1", "Budi", "budi-bukan-email", UserRole.USER)

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).cause is ValidationException)
        assertEquals(false, fakeRepo.inviteCalled)
    }

    @Test
    fun `invoke with valid admin and inputs succeeds`() = runTest {
        val fakeRepo = FakeUserRepository()
        fakeRepo.actorProfile = createAdmin()
        val useCase = InviteUserUseCase(fakeRepo)

        val result = useCase("admin-1", "Budi Santoso", "budi@example.com", UserRole.USER)

        assertTrue(result is AppResult.Success)
        assertEquals(true, fakeRepo.inviteCalled)
        val profile = (result as AppResult.Success).value
        assertEquals("Budi Santoso", profile.displayName)
        assertEquals("budi@example.com", profile.email)
        assertEquals(UserRole.USER, profile.role)
    }
}
