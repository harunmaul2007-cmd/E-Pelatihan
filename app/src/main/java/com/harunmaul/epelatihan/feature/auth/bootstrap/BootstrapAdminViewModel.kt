package com.harunmaul.epelatihan.feature.auth.bootstrap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.domain.usecase.BootstrapFirstAdminUseCase
import com.harunmaul.epelatihan.domain.validation.AuthValidator
import com.harunmaul.epelatihan.domain.validation.ValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BootstrapAdminUiState(
    val name: String = "",
    val nameError: String? = null,
    val email: String = "",
    val emailError: String? = null,
    val password: String = "",
    val passwordError: String? = null,
    val confirmPassword: String = "",
    val confirmPasswordError: String? = null,
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
) {
    val canSubmit: Boolean
        get() = name.isNotBlank() && email.isNotBlank() && password.isNotBlank() &&
            confirmPassword.isNotBlank() && !isLoading
}

@HiltViewModel
class BootstrapAdminViewModel @Inject constructor(
    private val bootstrapFirstAdminUseCase: BootstrapFirstAdminUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BootstrapAdminUiState())
    val uiState: StateFlow<BootstrapAdminUiState> = _uiState.asStateFlow()

    fun onNameChanged(value: String) {
        _uiState.update { it.copy(name = value, nameError = null, errorMessage = null) }
    }

    fun onEmailChanged(value: String) {
        _uiState.update { it.copy(email = value, emailError = null, errorMessage = null) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null, errorMessage = null) }
    }

    fun onConfirmPasswordChanged(value: String) {
        _uiState.update { it.copy(confirmPassword = value, confirmPasswordError = null, errorMessage = null) }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun bootstrap() {
        val state = _uiState.value

        val nameValidation = AuthValidator.validateDisplayName(state.name)
        val emailValidation = AuthValidator.validateEmail(state.email)
        val passwordValidation = AuthValidator.validatePassword(state.password)

        val nameError = (nameValidation as? ValidationResult.Invalid)?.issues?.firstOrNull()?.message
        val emailError = (emailValidation as? ValidationResult.Invalid)?.issues?.firstOrNull()?.message
        val passwordError = (passwordValidation as? ValidationResult.Invalid)?.issues?.firstOrNull()?.message
        val confirmError = if (state.password != state.confirmPassword) {
            "Konfirmasi kata sandi tidak cocok"
        } else {
            null
        }

        if (nameError != null || emailError != null || passwordError != null || confirmError != null) {
            _uiState.update {
                it.copy(
                    nameError = nameError,
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmError,
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = bootstrapFirstAdminUseCase(state.name, state.email, state.password)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.cause.message ?: "Gagal mendaftarkan Admin pertama.",
                        )
                    }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
