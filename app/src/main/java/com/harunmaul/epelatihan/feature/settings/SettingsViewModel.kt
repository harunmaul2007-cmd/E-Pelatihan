package com.harunmaul.epelatihan.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.domain.model.UserRole
import com.harunmaul.epelatihan.domain.model.UserSession
import com.harunmaul.epelatihan.domain.usecase.ObserveAppSettingsUseCase
import com.harunmaul.epelatihan.domain.usecase.ObserveSessionUseCase
import com.harunmaul.epelatihan.domain.usecase.UpdateCommunityNameUseCase
import com.harunmaul.epelatihan.domain.validation.AppSettingsValidator
import com.harunmaul.epelatihan.domain.validation.ValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val communityName: String = "",
    val initialCommunityName: String = "",
    val nameError: String? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaveSuccess: Boolean = false,
    val isAdmin: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
) {
    val isDirty: Boolean
        get() = communityName.trim() != initialCommunityName.trim()

    val canSave: Boolean
        get() = communityName.isNotBlank() && nameError == null && !isSaving && isDirty && isAdmin
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val observeAppSettingsUseCase: ObserveAppSettingsUseCase,
    private val updateCommunityNameUseCase: UpdateCommunityNameUseCase,
    private val observeSessionUseCase: ObserveSessionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        checkSession()
        loadSettings()
    }

    private fun checkSession() {
        viewModelScope.launch {
            observeSessionUseCase().collect { session ->
                val authenticated = session as? UserSession.Authenticated
                val isAdmin = authenticated?.profile?.active == true && authenticated.profile.role == UserRole.ADMIN
                _uiState.update { it.copy(isAdmin = isAdmin) }
            }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            observeAppSettingsUseCase().collect { settings ->
                _uiState.update { state ->
                    // Jika belum pernah diubah user saat layar aktif, isi dengan nilai dari database
                    val currentName = if (state.communityName.isEmpty() && !state.isDirty) {
                        settings.communityName
                    } else {
                        state.communityName
                    }
                    state.copy(
                        communityName = currentName,
                        initialCommunityName = settings.communityName,
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun onCommunityNameChanged(newName: String) {
        val error = when (val validation = AppSettingsValidator.validateCommunityName(newName)) {
            is ValidationResult.Invalid -> if (newName.isNotBlank()) validation.issues.first().message else null
            ValidationResult.Valid -> null
        }

        _uiState.update {
            it.copy(
                communityName = newName,
                nameError = error,
            )
        }
    }

    fun saveSettings() {
        val trimmed = _uiState.value.communityName.trim()
        val validation = AppSettingsValidator.validateCommunityName(trimmed)
        if (validation is ValidationResult.Invalid) {
            _uiState.update {
                it.copy(nameError = validation.issues.first().message)
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            when (val result = updateCommunityNameUseCase(trimmed)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isSaveSuccess = true,
                            initialCommunityName = result.value.communityName,
                            communityName = result.value.communityName,
                            successMessage = "Nama komunitas berhasil diperbarui.",
                            errorMessage = null,
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = result.cause.message ?: "Gagal memperbarui nama komunitas.",
                        )
                    }
                }
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
}
