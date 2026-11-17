package com.harunmaul.epelatihan.domain.usecase

import com.harunmaul.epelatihan.core.common.AppResult
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

class BootstrapFirstAdminUseCaseTest {

    private class FakeUserRepository : UserRepository {
        var bootstrapCalled = false
        var hasAdminResponse = false

        override suspend fun getUserProfile(uid: String): UserProfile? = null
        override fun observeUserProfile(uid: String): Flow<UserProfile?> = flowOf(null)
        override fun observeUsers(): Flow<List<UserProfile>> = flowOf(emptyList())
        override suspend fun hasAnyAdmin(): Boolean = hasAdminResponse
        override suspend fun bootstrapFirstAdmin(name: String, email: String, password: String): AppResult<UserProfile> {
            bootstrapCalled = true
            return AppResult.Success(
                UserProfile(
                    id = "admin-1",
                    displayName = name,
                    email = email,
                    role = UserRole.ADMIN,
                    active = true,
                    invitationStatus = InvitationStatus.ACTIVE,
                    createdBy = "admin-1",
                    createdAt = Instant.now(),
                    updatedBy = "admin-1",
                    updatedAt = Instant.now(),
                ),
            )
        }
        override suspend fun inviteUser(actorId: String, name: String, email: String, role: UserRole): AppResult<UserProfile> = error("Not needed")
        override suspend fun resendInvitation(actorId: String, userId: String, email: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateUserRole(actorId: String, userId: String, newRole: UserRole): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateUserStatus(actorId: String, userId: String, active: Boolean): AppResult<Unit> = AppResult.Success(Unit)
    }

    @Test
    fun `invoke with empty name fails with ValidationException`() = runTest {
        val fakeRepo = FakeUserRepository()
        val useCase = BootstrapFirstAdminUseCase(fakeRepo)

        val result = useCase("", "admin@lembaga.org", "rahasia123")

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).cause is ValidationException)
        assertEquals(false, fakeRepo.bootstrapCalled)
    }

    @Test
    fun `invoke with invalid email fails with ValidationException`() = runTest {
        val fakeRepo = FakeUserRepository()
        val useCase = BootstrapFirstAdminUseCase(fakeRepo)

        val result = useCase("Admin", "admin-bukan-email", "rahasia123")

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).cause is ValidationException)
        assertEquals(false, fakeRepo.bootstrapCalled)
    }

    @Test
    fun `invoke with short password fails with ValidationException`() = runTest {
        val fakeRepo = FakeUserRepository()
        val useCase = BootstrapFirstAdminUseCase(fakeRepo)

        val result = useCase("Admin", "admin@lembaga.org", "123")

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).cause is ValidationException)
        assertEquals(false, fakeRepo.bootstrapCalled)
    }

    @Test
    fun `invoke when admin already exists fails with IllegalStateException`() = runTest {
        val fakeRepo = FakeUserRepository()
        fakeRepo.hasAdminResponse = true
        val useCase = BootstrapFirstAdminUseCase(fakeRepo)

        val result = useCase("Admin Baru", "admin2@lembaga.org", "rahasia123")

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).cause is IllegalStateException)
        assertEquals(false, fakeRepo.bootstrapCalled)
    }

    @Test
    fun `invoke when no admin exists succeeds`() = runTest {
        val fakeRepo = FakeUserRepository()
        fakeRepo.hasAdminResponse = false
        val useCase = BootstrapFirstAdminUseCase(fakeRepo)

        val result = useCase("Admin Pertama", "admin@lembaga.org", "rahasia123")

        assertTrue(result is AppResult.Success)
        assertEquals(true, fakeRepo.bootstrapCalled)
        val profile = (result as AppResult.Success).value
        assertEquals("Admin Pertama", profile.displayName)
        assertEquals("admin@lembaga.org", profile.email)
        assertEquals(UserRole.ADMIN, profile.role)
    }
}
