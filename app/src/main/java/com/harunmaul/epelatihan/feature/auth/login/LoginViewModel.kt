package com.harunmaul.epelatihan.feature.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.domain.usecase.CheckHasAnyAdminUseCase
import com.harunmaul.epelatihan.domain.usecase.LoginUseCase
import com.harunmaul.epelatihan.domain.usecase.ObserveAppSettingsUseCase
import com.harunmaul.epelatihan.domain.validation.AuthValidator
import com.harunmaul.epelatihan.domain.validation.ValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val communityName: String = "E-Pelatihan",
    val email: String = "",
    val emailError: String? = null,
    val password: String = "",
    val passwordError: String? = null,
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val hasAnyAdmin: Boolean = true,
) {
    val canSubmit: Boolean
        get() = email.isNotBlank() && password.isNotBlank() && !isLoading
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val checkHasAnyAdminUseCase: CheckHasAnyAdminUseCase,
    observeAppSettingsUseCase: ObserveAppSettingsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeAppSettingsUseCase().collect { settings ->
                _uiState.update { it.copy(communityName = settings.communityName) }
            }
        }
        refreshAdminStatus()
    }

    fun refreshAdminStatus() {
        viewModelScope.launch {
            val hasAdmin = checkHasAnyAdminUseCase()
            _uiState.update { it.copy(hasAnyAdmin = hasAdmin) }
        }
    }

    fun onEmailChanged(value: String) {
        _uiState.update {
            it.copy(
                email = value,
                emailError = null,
                errorMessage = null,
            )
        }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update {
            it.copy(
                password = value,
                passwordError = null,
                errorMessage = null,
            )
        }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun login() {
        val currentState = _uiState.value
        val emailValidation = AuthValidator.validateEmail(currentState.email)
        val passwordValidation = AuthValidator.validatePassword(currentState.password)

        val emailError = (emailValidation as? ValidationResult.Invalid)?.issues?.firstOrNull()?.message
        val passwordError = (passwordValidation as? ValidationResult.Invalid)?.issues?.firstOrNull()?.message

        if (emailError != null || passwordError != null) {
            _uiState.update {
                it.copy(
                    emailError = emailError,
                    passwordError = passwordError,
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = loginUseCase(currentState.email, currentState.password)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.cause.message ?: "Gagal masuk. Periksa kembali akun Anda.",
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
