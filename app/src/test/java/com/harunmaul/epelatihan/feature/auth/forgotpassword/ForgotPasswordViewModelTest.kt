package com.harunmaul.epelatihan.feature.auth.forgotpassword

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.domain.model.UserProfile
import com.harunmaul.epelatihan.domain.model.UserSession
import com.harunmaul.epelatihan.domain.repository.AuthRepository
import com.harunmaul.epelatihan.domain.usecase.SendPasswordResetUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ForgotPasswordViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeAuthRepository : AuthRepository {
        var shouldSucceed = true

        override fun observeSession(): Flow<UserSession> = flowOf(UserSession.Unauthenticated)
        override suspend fun login(email: String, password: String): AppResult<UserProfile> = error("Not needed")
        override suspend fun logout() {}
        override suspend fun sendPasswordReset(email: String): AppResult<Unit> {
            return if (shouldSucceed) {
                AppResult.Success(Unit)
            } else {
                AppResult.Failure(Exception("Email tidak terdaftar."))
            }
        }
        override suspend fun getCurrentUserProfile(): UserProfile? = null
    }

    private lateinit var authRepository: FakeAuthRepository
    private lateinit var viewModel: ForgotPasswordViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = FakeAuthRepository()
        viewModel = ForgotPasswordViewModel(
            sendPasswordResetUseCase = SendPasswordResetUseCase(authRepository),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `sendResetLink with invalid email sets emailError`() = runTest {
        viewModel.onEmailChanged("invalid-email")
        viewModel.sendResetLink()
        advanceUntilIdle()

        assertEquals("Format email tidak valid", viewModel.uiState.value.emailError)
        assertFalse(viewModel.uiState.value.isSuccess)
    }

    @Test
    fun `sendResetLink with valid email succeeds`() = runTest {
        viewModel.onEmailChanged("user@example.com")
        viewModel.sendResetLink()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSuccess)
        assertEquals("user@example.com", viewModel.uiState.value.sentEmail)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `sendResetLink failure sets errorMessage`() = runTest {
        authRepository.shouldSucceed = false
        viewModel.onEmailChanged("user@example.com")
        viewModel.sendResetLink()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSuccess)
        assertEquals("Email tidak terdaftar.", viewModel.uiState.value.errorMessage)
    }
}
