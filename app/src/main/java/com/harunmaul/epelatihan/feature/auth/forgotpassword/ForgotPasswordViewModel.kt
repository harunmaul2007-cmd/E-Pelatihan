package com.harunmaul.epelatihan.feature.auth.forgotpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.domain.usecase.SendPasswordResetUseCase
import com.harunmaul.epelatihan.domain.validation.AuthValidator
import com.harunmaul.epelatihan.domain.validation.ValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ForgotPasswordUiState(
    val email: String = "",
    val emailError: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val sentEmail: String = "",
) {
    val canSubmit: Boolean
        get() = email.isNotBlank() && !isLoading
}

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val sendPasswordResetUseCase: SendPasswordResetUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun onEmailChanged(value: String) {
        _uiState.update {
            it.copy(
                email = value,
                emailError = null,
                errorMessage = null,
            )
        }
    }

    fun sendResetLink() {
        val email = _uiState.value.email.trim()
        val validation = AuthValidator.validateEmail(email)
        val emailError = (validation as? ValidationResult.Invalid)?.issues?.firstOrNull()?.message

        if (emailError != null) {
            _uiState.update { it.copy(emailError = emailError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = sendPasswordResetUseCase(email)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = true,
                            sentEmail = email,
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.cause.message ?: "Gagal mengirim tautan reset password.",
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
