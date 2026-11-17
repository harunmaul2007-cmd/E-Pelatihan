package com.harunmaul.epelatihan.domain.usecase

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.core.common.ValidationException
import com.harunmaul.epelatihan.domain.model.InvitationStatus
import com.harunmaul.epelatihan.domain.model.UserProfile
import com.harunmaul.epelatihan.domain.model.UserRole
import com.harunmaul.epelatihan.domain.model.UserSession
import com.harunmaul.epelatihan.domain.repository.AuthRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginUseCaseTest {

    private class FakeAuthRepository : AuthRepository {
        var loginCalled = false
        var lastEmail = ""
        var lastPassword = ""

        override fun observeSession(): Flow<UserSession> = flowOf(UserSession.Unauthenticated)

        override suspend fun login(email: String, password: String): AppResult<UserProfile> {
            loginCalled = true
            lastEmail = email
            lastPassword = password
            return AppResult.Success(
                UserProfile(
                    id = "test-uid",
                    displayName = "Test User",
                    email = email,
                    role = UserRole.ADMIN,
                    active = true,
                    invitationStatus = InvitationStatus.ACTIVE,
                    createdBy = "system",
                    createdAt = Instant.now(),
                    updatedBy = "system",
                    updatedAt = Instant.now(),
                ),
            )
        }

        override suspend fun logout() {}
        override suspend fun sendPasswordReset(email: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun getCurrentUserProfile(): UserProfile? = null
    }

    @Test
    fun `invoke with invalid email fails without calling repository`() = runTest {
        val fakeRepo = FakeAuthRepository()
        val useCase = LoginUseCase(fakeRepo)

        val result = useCase("invalid-email", "password123")

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).cause is ValidationException)
        assertEquals(false, fakeRepo.loginCalled)
    }

    @Test
    fun `invoke with short password fails without calling repository`() = runTest {
        val fakeRepo = FakeAuthRepository()
        val useCase = LoginUseCase(fakeRepo)

        val result = useCase("valid@example.com", "123")

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).cause is ValidationException)
        assertEquals(false, fakeRepo.loginCalled)
    }

    @Test
    fun `invoke with valid credentials calls repository with trimmed email`() = runTest {
        val fakeRepo = FakeAuthRepository()
        val useCase = LoginUseCase(fakeRepo)

        val result = useCase("  user@example.com  ", "password123")

        assertTrue(result is AppResult.Success)
        assertEquals(true, fakeRepo.loginCalled)
        assertEquals("user@example.com", fakeRepo.lastEmail)
        assertEquals("password123", fakeRepo.lastPassword)
    }
}
