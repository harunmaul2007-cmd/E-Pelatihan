package com.harunmaul.epelatihan.domain.usecase

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.core.common.ValidationException
import com.harunmaul.epelatihan.domain.model.UserProfile
import com.harunmaul.epelatihan.domain.model.UserSession
import com.harunmaul.epelatihan.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SendPasswordResetUseCaseTest {

    private class FakeAuthRepository : AuthRepository {
        var resetCalled = false
        var lastEmail = ""

        override fun observeSession(): Flow<UserSession> = flowOf(UserSession.Unauthenticated)
        override suspend fun login(email: String, password: String): AppResult<UserProfile> =
            error("Not implemented")
        override suspend fun logout() {}
        override suspend fun sendPasswordReset(email: String): AppResult<Unit> {
            resetCalled = true
            lastEmail = email
            return AppResult.Success(Unit)
        }
        override suspend fun getCurrentUserProfile(): UserProfile? = null
    }

    @Test
    fun `invoke with invalid email fails without calling repository`() = runTest {
        val fakeRepo = FakeAuthRepository()
        val useCase = SendPasswordResetUseCase(fakeRepo)

        val result = useCase("invalid-email")

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).cause is ValidationException)
        assertEquals(false, fakeRepo.resetCalled)
    }

    @Test
    fun `invoke with valid email calls repository with trimmed email`() = runTest {
        val fakeRepo = FakeAuthRepository()
        val useCase = SendPasswordResetUseCase(fakeRepo)

        val result = useCase("  user@example.com  ")

        assertTrue(result is AppResult.Success)
        assertEquals(true, fakeRepo.resetCalled)
        assertEquals("user@example.com", fakeRepo.lastEmail)
    }
}
