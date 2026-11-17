package com.harunmaul.epelatihan.feature.auth.login

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.domain.model.AppSettings
import com.harunmaul.epelatihan.domain.model.InvitationStatus
import com.harunmaul.epelatihan.domain.model.UserProfile
import com.harunmaul.epelatihan.domain.model.UserRole
import com.harunmaul.epelatihan.domain.model.UserSession
import com.harunmaul.epelatihan.domain.repository.AppSettingsRepository
import com.harunmaul.epelatihan.domain.repository.AuthRepository
import com.harunmaul.epelatihan.domain.repository.UserRepository
import com.harunmaul.epelatihan.domain.usecase.CheckHasAnyAdminUseCase
import com.harunmaul.epelatihan.domain.usecase.LoginUseCase
import com.harunmaul.epelatihan.domain.usecase.ObserveAppSettingsUseCase
import java.time.Instant
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
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeAuthRepository : AuthRepository {
        var shouldSucceed = true

        override fun observeSession(): Flow<UserSession> = flowOf(UserSession.Unauthenticated)

        override suspend fun login(email: String, password: String): AppResult<UserProfile> {
            return if (shouldSucceed) {
                AppResult.Success(
                    UserProfile(
                        id = "uid-123",
                        displayName = "Admin User",
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
            } else {
                AppResult.Failure(Exception("Email atau kata sandi salah."))
            }
        }

        override suspend fun logout() {}
        override suspend fun sendPasswordReset(email: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun getCurrentUserProfile(): UserProfile? = null
    }

    private class FakeAppSettingsRepository : AppSettingsRepository {
        override fun observeAppSettings(): Flow<AppSettings> = flowOf(
            AppSettings(
                communityName = "Komunitas Android",
                schemaVersion = 1,
                updatedBy = null,
                updatedAt = null,
            ),
        )

        override suspend fun getAppSettings(): AppSettings = AppSettings(
            communityName = "Komunitas Android",
            schemaVersion = 1,
            updatedBy = null,
            updatedAt = null,
        )
    }

    private class FakeUserRepository : UserRepository {
        var hasAdmin = true
        override suspend fun getUserProfile(uid: String): UserProfile? = null
        override fun observeUserProfile(uid: String): Flow<UserProfile?> = flowOf(null)
        override fun observeUsers(): Flow<List<UserProfile>> = flowOf(emptyList())
        override suspend fun hasAnyAdmin(): Boolean = hasAdmin
        override suspend fun bootstrapFirstAdmin(name: String, email: String, password: String): AppResult<UserProfile> = error("Not needed")
        override suspend fun inviteUser(actorId: String, name: String, email: String, role: UserRole): AppResult<UserProfile> = error("Not needed")
        override suspend fun resendInvitation(actorId: String, userId: String, email: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateUserRole(actorId: String, userId: String, newRole: UserRole): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateUserStatus(actorId: String, userId: String, active: Boolean): AppResult<Unit> = AppResult.Success(Unit)
    }

    private lateinit var authRepository: FakeAuthRepository
    private lateinit var appSettingsRepository: FakeAppSettingsRepository
    private lateinit var userRepository: FakeUserRepository
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = FakeAuthRepository()
        appSettingsRepository = FakeAppSettingsRepository()
        userRepository = FakeUserRepository()
        viewModel = LoginViewModel(
            loginUseCase = LoginUseCase(authRepository),
            checkHasAnyAdminUseCase = CheckHasAnyAdminUseCase(userRepository),
            observeAppSettingsUseCase = ObserveAppSettingsUseCase(appSettingsRepository),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads community name from settings`() = runTest {
        advanceUntilIdle()
        assertEquals("Komunitas Android", viewModel.uiState.value.communityName)
    }

    @Test
    fun `login with invalid email sets emailError`() = runTest {
        viewModel.onEmailChanged("not-an-email")
        viewModel.onPasswordChanged("123456")
        viewModel.login()
        advanceUntilIdle()

        assertEquals("Format email tidak valid", viewModel.uiState.value.emailError)
        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isSuccess)
    }

    @Test
    fun `login with valid credentials succeeds`() = runTest {
        viewModel.onEmailChanged("admin@lembaga.org")
        viewModel.onPasswordChanged("secret123")
        viewModel.login()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSuccess)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `login failure sets errorMessage`() = runTest {
        authRepository.shouldSucceed = false
        viewModel.onEmailChanged("admin@lembaga.org")
        viewModel.onPasswordChanged("secret123")
        viewModel.login()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSuccess)
        assertEquals("Email atau kata sandi salah.", viewModel.uiState.value.errorMessage)
    }
}
