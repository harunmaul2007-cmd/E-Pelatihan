package com.harunmaul.epelatihan.feature.usermanagement

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.domain.model.InvitationStatus
import com.harunmaul.epelatihan.domain.model.UserProfile
import com.harunmaul.epelatihan.domain.model.UserRole
import com.harunmaul.epelatihan.domain.model.UserSession
import com.harunmaul.epelatihan.domain.repository.AuthRepository
import com.harunmaul.epelatihan.domain.repository.UserRepository
import com.harunmaul.epelatihan.domain.usecase.InviteUserUseCase
import com.harunmaul.epelatihan.domain.usecase.ObserveSessionUseCase
import com.harunmaul.epelatihan.domain.usecase.ObserveUsersUseCase
import com.harunmaul.epelatihan.domain.usecase.ResendInvitationUseCase
import com.harunmaul.epelatihan.domain.usecase.UpdateUserRoleUseCase
import com.harunmaul.epelatihan.domain.usecase.UpdateUserStatusUseCase
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeUserRepository : UserRepository {
        val usersFlow = MutableStateFlow<List<UserProfile>>(emptyList())
        var shouldSucceed = true

        override suspend fun getUserProfile(uid: String): UserProfile? =
            usersFlow.value.find { it.id == uid }

        override fun observeUserProfile(uid: String): Flow<UserProfile?> = flowOf(null)
        override fun observeUsers(): Flow<List<UserProfile>> = usersFlow
        override suspend fun hasAnyAdmin(): Boolean = true
        override suspend fun bootstrapFirstAdmin(name: String, email: String, password: String): AppResult<UserProfile> = error("Not needed")

        override suspend fun inviteUser(actorId: String, name: String, email: String, role: UserRole): AppResult<UserProfile> {
            return if (shouldSucceed) {
                val newUser = UserProfile(
                    id = "new-id",
                    displayName = name,
                    email = email,
                    role = role,
                    active = true,
                    invitationStatus = InvitationStatus.INVITED,
                    createdBy = actorId,
                    createdAt = Instant.now(),
                    updatedBy = actorId,
                    updatedAt = Instant.now(),
                )
                usersFlow.value = usersFlow.value + newUser
                AppResult.Success(newUser)
            } else {
                AppResult.Failure(Exception("Email sudah terdaftar."))
            }
        }

        override suspend fun resendInvitation(actorId: String, userId: String, email: String): AppResult<Unit> =
            if (shouldSucceed) AppResult.Success(Unit) else AppResult.Failure(Exception("Gagal mengirim."))

        override suspend fun updateUserRole(actorId: String, userId: String, newRole: UserRole): AppResult<Unit> =
            if (shouldSucceed) AppResult.Success(Unit) else AppResult.Failure(Exception("Gagal mengubah role."))

        override suspend fun updateUserStatus(actorId: String, userId: String, active: Boolean): AppResult<Unit> =
            if (shouldSucceed) AppResult.Success(Unit) else AppResult.Failure(Exception("Gagal mengubah status."))
    }

    private class FakeAuthRepository(val sessionFlow: Flow<UserSession>) : AuthRepository {
        override fun observeSession(): Flow<UserSession> = sessionFlow
        override suspend fun login(email: String, password: String): AppResult<UserProfile> = error("Not needed")
        override suspend fun logout() {}
        override suspend fun sendPasswordReset(email: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun getCurrentUserProfile(): UserProfile? = null
    }

    private lateinit var userRepository: FakeUserRepository
    private lateinit var viewModel: UserListViewModel

    private val adminUser = UserProfile(
        id = "admin-1",
        displayName = "Admin Super",
        email = "admin@lembaga.org",
        role = UserRole.ADMIN,
        active = true,
        invitationStatus = InvitationStatus.ACTIVE,
        createdBy = "system",
        createdAt = Instant.now(),
        updatedBy = "system",
        updatedAt = Instant.now(),
    )

    private val regularUser = UserProfile(
        id = "user-2",
        displayName = "Budi Santoso",
        email = "budi@lembaga.org",
        role = UserRole.USER,
        active = true,
        invitationStatus = InvitationStatus.INVITED,
        createdBy = "admin-1",
        createdAt = Instant.now(),
        updatedBy = "admin-1",
        updatedAt = Instant.now(),
    )

    private val inactiveUser = UserProfile(
        id = "user-3",
        displayName = "Citra Nonaktif",
        email = "citra@lembaga.org",
        role = UserRole.USER,
        active = false,
        invitationStatus = InvitationStatus.ACTIVE,
        createdBy = "admin-1",
        createdAt = Instant.now(),
        updatedBy = "admin-1",
        updatedAt = Instant.now(),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        userRepository = FakeUserRepository()
        userRepository.usersFlow.value = listOf(adminUser, regularUser, inactiveUser)

        val authRepo = FakeAuthRepository(flowOf(UserSession.Authenticated(adminUser)))

        viewModel = UserListViewModel(
            observeUsersUseCase = ObserveUsersUseCase(userRepository),
            observeSessionUseCase = ObserveSessionUseCase(authRepo),
            inviteUserUseCase = InviteUserUseCase(userRepository),
            resendInvitationUseCase = ResendInvitationUseCase(userRepository),
            updateUserRoleUseCase = UpdateUserRoleUseCase(userRepository),
            updateUserStatusUseCase = UpdateUserStatusUseCase(userRepository),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads all users and admin status`() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(3, state.users.size)
        assertEquals(3, state.filteredUsers.size)
        assertTrue(state.isCurrentAdmin)
    }

    @Test
    fun `search filter filters users by name or email`() = runTest {
        advanceUntilIdle()
        viewModel.onSearchQueryChanged("budi")
        val state = viewModel.uiState.value
        assertEquals(1, state.filteredUsers.size)
        assertEquals("Budi Santoso", state.filteredUsers[0].displayName)
    }

    @Test
    fun `category filter filters by role or active state`() = runTest {
        advanceUntilIdle()
        viewModel.onFilterSelected(UserFilter.ADMIN)
        assertEquals(1, viewModel.uiState.value.filteredUsers.size)
        assertEquals(UserRole.ADMIN, viewModel.uiState.value.filteredUsers[0].role)

        viewModel.onFilterSelected(UserFilter.INACTIVE)
        assertEquals(1, viewModel.uiState.value.filteredUsers.size)
        assertFalse(viewModel.uiState.value.filteredUsers[0].active)
    }

    @Test
    fun `inviteUser success closes dialog and sets success message`() = runTest {
        advanceUntilIdle()
        viewModel.openInviteDialog()
        assertTrue(viewModel.uiState.value.isInviteDialogOpen)

        viewModel.inviteUser("Dewi Baru", "dewi@lembaga.org", UserRole.USER)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isInviteDialogOpen)
        assertTrue(viewModel.uiState.value.successMessage != null)
        assertEquals(4, viewModel.uiState.value.users.size)
    }

    @Test
    fun `inviteUser failure sets errorMessage`() = runTest {
        advanceUntilIdle()
        userRepository.shouldSucceed = false

        viewModel.inviteUser("Dewi Baru", "dewi@lembaga.org", UserRole.USER)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.errorMessage != null)
    }
}
