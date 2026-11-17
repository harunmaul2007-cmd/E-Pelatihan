package com.harunmaul.epelatihan.feature.usermanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.domain.model.UserProfile
import com.harunmaul.epelatihan.domain.model.UserRole
import com.harunmaul.epelatihan.domain.model.UserSession
import com.harunmaul.epelatihan.domain.usecase.InviteUserUseCase
import com.harunmaul.epelatihan.domain.usecase.ObserveSessionUseCase
import com.harunmaul.epelatihan.domain.usecase.ObserveUsersUseCase
import com.harunmaul.epelatihan.domain.usecase.ResendInvitationUseCase
import com.harunmaul.epelatihan.domain.usecase.UpdateUserRoleUseCase
import com.harunmaul.epelatihan.domain.usecase.UpdateUserStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class UserFilter { ALL, ADMIN, USER, INACTIVE }

data class UserListUiState(
    val users: List<UserProfile> = emptyList(),
    val currentUser: UserProfile? = null,
    val filter: UserFilter = UserFilter.ALL,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isInviting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isInviteDialogOpen: Boolean = false,
    val selectedUserForRoleChange: UserProfile? = null,
    val selectedUserForStatusChange: UserProfile? = null,
    val selectedUserForResend: UserProfile? = null,
) {
    val isCurrentAdmin: Boolean
        get() = currentUser?.role == UserRole.ADMIN

    val filteredUsers: List<UserProfile>
        get() {
            var list = users
            list = when (filter) {
                UserFilter.ALL -> list
                UserFilter.ADMIN -> list.filter { it.role == UserRole.ADMIN }
                UserFilter.USER -> list.filter { it.role == UserRole.USER }
                UserFilter.INACTIVE -> list.filter { !it.active }
            }
            if (searchQuery.isNotBlank()) {
                val query = searchQuery.trim().lowercase()
                list = list.filter {
                    it.displayName.lowercase().contains(query) || it.email.lowercase().contains(query)
                }
            }
            return list
        }
}

@HiltViewModel
class UserListViewModel @Inject constructor(
    observeUsersUseCase: ObserveUsersUseCase,
    observeSessionUseCase: ObserveSessionUseCase,
    private val inviteUserUseCase: InviteUserUseCase,
    private val resendInvitationUseCase: ResendInvitationUseCase,
    private val updateUserRoleUseCase: UpdateUserRoleUseCase,
    private val updateUserStatusUseCase: UpdateUserStatusUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserListUiState(isLoading = true))
    val uiState: StateFlow<UserListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                observeUsersUseCase(),
                observeSessionUseCase(),
            ) { users, session ->
                val currentUser = (session as? UserSession.Authenticated)?.profile
                Pair(users, currentUser)
            }.collect { (users, currentUser) ->
                _uiState.update {
                    it.copy(
                        users = users,
                        currentUser = currentUser,
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onFilterSelected(filter: UserFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun openInviteDialog() {
        _uiState.update { it.copy(isInviteDialogOpen = true, errorMessage = null) }
    }

    fun closeInviteDialog() {
        _uiState.update { it.copy(isInviteDialogOpen = false) }
    }

    fun inviteUser(name: String, email: String, role: UserRole) {
        val actorId = _uiState.value.currentUser?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isInviting = true, errorMessage = null) }
            when (val result = inviteUserUseCase(actorId, name, email, role)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isInviting = false,
                            isInviteDialogOpen = false,
                            successMessage = "Undangan berhasil dikirim ke $email.",
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isInviting = false,
                            errorMessage = result.cause.message ?: "Gagal mengundang pengguna.",
                        )
                    }
                }
            }
        }
    }

    fun requestRoleChange(user: UserProfile) {
        _uiState.update { it.copy(selectedUserForRoleChange = user, errorMessage = null) }
    }

    fun cancelRoleChange() {
        _uiState.update { it.copy(selectedUserForRoleChange = null) }
    }

    fun confirmRoleChange() {
        val user = _uiState.value.selectedUserForRoleChange ?: return
        val actorId = _uiState.value.currentUser?.id ?: return
        val newRole = if (user.role == UserRole.ADMIN) UserRole.USER else UserRole.ADMIN

        viewModelScope.launch {
            _uiState.update { it.copy(selectedUserForRoleChange = null, isLoading = true, errorMessage = null) }
            when (val result = updateUserRoleUseCase(actorId, user.id, newRole)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Peran ${user.displayName} diubah menjadi ${newRole.name}.",
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.cause.message ?: "Gagal mengubah peran pengguna.",
                        )
                    }
                }
            }
        }
    }

    fun requestStatusChange(user: UserProfile) {
        _uiState.update { it.copy(selectedUserForStatusChange = user, errorMessage = null) }
    }

    fun cancelStatusChange() {
        _uiState.update { it.copy(selectedUserForStatusChange = null) }
    }

    fun confirmStatusChange() {
        val user = _uiState.value.selectedUserForStatusChange ?: return
        val actorId = _uiState.value.currentUser?.id ?: return
        val newStatus = !user.active

        viewModelScope.launch {
            _uiState.update { it.copy(selectedUserForStatusChange = null, isLoading = true, errorMessage = null) }
            when (val result = updateUserStatusUseCase(actorId, user.id, newStatus)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Status ${user.displayName} berhasil diubah menjadi ${if (newStatus) "Aktif" else "Nonaktif"}.",
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.cause.message ?: "Gagal mengubah status pengguna.",
                        )
                    }
                }
            }
        }
    }

    fun requestResend(user: UserProfile) {
        _uiState.update { it.copy(selectedUserForResend = user, errorMessage = null) }
    }

    fun cancelResend() {
        _uiState.update { it.copy(selectedUserForResend = null) }
    }

    fun confirmResend() {
        val user = _uiState.value.selectedUserForResend ?: return
        val actorId = _uiState.value.currentUser?.id ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(selectedUserForResend = null, isLoading = true, errorMessage = null) }
            when (val result = resendInvitationUseCase(actorId, user.id, user.email)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Tautan undangan berhasil dikirim ulang ke ${user.email}.",
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.cause.message ?: "Gagal mengirim ulang tautan undangan.",
                        )
                    }
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
