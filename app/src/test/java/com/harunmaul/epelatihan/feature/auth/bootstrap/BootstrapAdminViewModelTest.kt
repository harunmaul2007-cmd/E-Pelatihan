package com.harunmaul.epelatihan.feature.auth.bootstrap

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.domain.model.InvitationStatus
import com.harunmaul.epelatihan.domain.model.UserProfile
import com.harunmaul.epelatihan.domain.model.UserRole
import com.harunmaul.epelatihan.domain.repository.UserRepository
import com.harunmaul.epelatihan.domain.usecase.BootstrapFirstAdminUseCase
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
class BootstrapAdminViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeUserRepository : UserRepository {
        var shouldSucceed = true
        var hasAdmin = false

        override suspend fun getUserProfile(uid: String): UserProfile? = null
        override fun observeUserProfile(uid: String): Flow<UserProfile?> = flowOf(null)
        override fun observeUsers(): Flow<List<UserProfile>> = flowOf(emptyList())
        override suspend fun hasAnyAdmin(): Boolean = hasAdmin
        override suspend fun bootstrapFirstAdmin(name: String, email: String, password: String): AppResult<UserProfile> {
            return if (shouldSucceed) {
                AppResult.Success(
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
            } else {
                AppResult.Failure(Exception("Gagal membuat admin."))
            }
        }
        override suspend fun inviteUser(actorId: String, name: String, email: String, role: UserRole): AppResult<UserProfile> = error("Not needed")
        override suspend fun resendInvitation(actorId: String, userId: String, email: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateUserRole(actorId: String, userId: String, newRole: UserRole): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateUserStatus(actorId: String, userId: String, active: Boolean): AppResult<Unit> = AppResult.Success(Unit)
    }

    private lateinit var userRepository: FakeUserRepository
    private lateinit var viewModel: BootstrapAdminViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        userRepository = FakeUserRepository()
        viewModel = BootstrapAdminViewModel(
            bootstrapFirstAdminUseCase = BootstrapFirstAdminUseCase(userRepository),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `bootstrap with mismatch password sets error`() = runTest {
        viewModel.onNameChanged("Admin")
        viewModel.onEmailChanged("admin@lembaga.org")
        viewModel.onPasswordChanged("password123")
        viewModel.onConfirmPasswordChanged("different123")

        viewModel.bootstrap()
        advanceUntilIdle()

        assertEquals("Konfirmasi kata sandi tidak cocok", viewModel.uiState.value.confirmPasswordError)
        assertFalse(viewModel.uiState.value.isSuccess)
    }

    @Test
    fun `bootstrap with valid inputs succeeds`() = runTest {
        viewModel.onNameChanged("Admin")
        viewModel.onEmailChanged("admin@lembaga.org")
        viewModel.onPasswordChanged("password123")
        viewModel.onConfirmPasswordChanged("password123")

        viewModel.bootstrap()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSuccess)
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
