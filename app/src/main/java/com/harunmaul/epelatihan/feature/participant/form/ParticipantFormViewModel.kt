package com.harunmaul.epelatihan.feature.participant.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.core.common.DuplicateParticipantPhoneException
import com.harunmaul.epelatihan.domain.model.Participant
import com.harunmaul.epelatihan.domain.model.UserRole
import com.harunmaul.epelatihan.domain.model.UserSession
import com.harunmaul.epelatihan.domain.usecase.CreateParticipantUseCase
import com.harunmaul.epelatihan.domain.usecase.FindParticipantByPhoneUseCase
import com.harunmaul.epelatihan.domain.usecase.ObserveParticipantDetailUseCase
import com.harunmaul.epelatihan.domain.usecase.ObserveSessionUseCase
import com.harunmaul.epelatihan.domain.usecase.UpdateParticipantUseCase
import com.harunmaul.epelatihan.domain.validation.PhoneNormalizationResult
import com.harunmaul.epelatihan.domain.validation.PhoneNumberNormalizer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ParticipantFormUiState(
    val isEditMode: Boolean = false,
    val participantId: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSaveSuccess: Boolean = false,
    val isAdmin: Boolean = false,
    val isReadOnlyNonAdmin: Boolean = false,
    val name: String = "",
    val phoneRaw: String = "",
    val phoneNormalized: String = "",
    val email: String = "",
    val notes: String = "",
    val nameError: String? = null,
    val phoneError: String? = null,
    val emailError: String? = null,
    val duplicateParticipant: Participant? = null,
    val showDuplicateDialog: Boolean = false,
    val errorMessage: String? = null,
) {
    val isFormValid: Boolean
        get() = name.isNotBlank() &&
            phoneRaw.isNotBlank() &&
            phoneError == null &&
            nameError == null &&
            emailError == null &&
            !isReadOnlyNonAdmin
}

@HiltViewModel
class ParticipantFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val createParticipantUseCase: CreateParticipantUseCase,
    private val updateParticipantUseCase: UpdateParticipantUseCase,
    private val observeParticipantDetailUseCase: ObserveParticipantDetailUseCase,
    private val findParticipantByPhoneUseCase: FindParticipantByPhoneUseCase,
    private val observeSessionUseCase: ObserveSessionUseCase,
) : ViewModel() {

    private val participantId: String? = savedStateHandle["participantId"]
    private val initialPhone: String? = savedStateHandle["initialPhone"]

    private val _uiState = MutableStateFlow(
        ParticipantFormUiState(
            isEditMode = !participantId.isNullOrBlank(),
            participantId = participantId,
            phoneRaw = initialPhone.orEmpty(),
        ),
    )
    val uiState: StateFlow<ParticipantFormUiState> = _uiState.asStateFlow()

    init {
        checkSessionAndLoad()
    }

    private fun checkSessionAndLoad() {
        viewModelScope.launch {
            val session = observeSessionUseCase().first()
            val isAdmin = (session as? UserSession.Authenticated)?.profile?.role == UserRole.ADMIN
            val isEdit = !participantId.isNullOrBlank()

            _uiState.update {
                it.copy(
                    isAdmin = isAdmin,
                    isReadOnlyNonAdmin = isEdit && !isAdmin,
                )
            }

            if (participantId != null) {
                _uiState.update { it.copy(isLoading = true) }
                when (val result = observeParticipantDetailUseCase(participantId).first()) {
                    is AppResult.Success -> {
                        val p = result.value.participant
                        if (p != null) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    name = p.name,
                                    phoneRaw = p.phoneDisplay,
                                    phoneNormalized = p.phoneNormalized,
                                    email = p.email.orEmpty(),
                                    notes = p.notes.orEmpty(),
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = "Data peserta tidak ditemukan",
                                )
                            }
                        }
                    }
                    is AppResult.Failure -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.cause.message ?: "Gagal memuat peserta",
                            )
                        }
                    }
                }
            } else if (!initialPhone.isNullOrBlank()) {
                onPhoneChanged(initialPhone)
            }
        }
    }

    fun onNameChanged(newName: String) {
        _uiState.update {
            it.copy(
                name = newName,
                nameError = if (newName.isBlank()) "Nama peserta wajib diisi" else null,
            )
        }
    }

    fun onPhoneChanged(newPhone: String) {
        val normResult = PhoneNumberNormalizer.normalize(newPhone)
        val (normPhone, error) = when (normResult) {
            is PhoneNormalizationResult.Success -> normResult.normalized to null
            is PhoneNormalizationResult.Failure -> "" to normResult.message
        }

        _uiState.update {
            it.copy(
                phoneRaw = newPhone,
                phoneNormalized = normPhone,
                phoneError = if (newPhone.isNotBlank()) error else null,
            )
        }

        if (normPhone.isNotBlank()) {
            checkForDuplicate(normPhone)
        }
    }

    fun onContactSelected(name: String, phone: String) {
        val currentName = _uiState.value.name
        val updatedName = if (currentName.isBlank()) name else currentName

        val normResult = PhoneNumberNormalizer.normalize(phone)
        val (normPhone, error) = when (normResult) {
            is PhoneNormalizationResult.Success -> normResult.normalized to null
            is PhoneNormalizationResult.Failure -> "" to normResult.message
        }

        _uiState.update {
            it.copy(
                name = updatedName,
                nameError = if (updatedName.isBlank()) "Nama peserta wajib diisi" else null,
                phoneRaw = phone,
                phoneNormalized = normPhone,
                phoneError = if (phone.isNotBlank()) error else null,
            )
        }

        if (normPhone.isNotBlank()) {
            checkForDuplicate(normPhone)
        }
    }

    fun onEmailChanged(newEmail: String) {
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        val error = if (newEmail.isNotBlank() && !emailRegex.matches(newEmail.trim())) {
            "Format email tidak valid"
        } else {
            null
        }

        _uiState.update {
            it.copy(
                email = newEmail,
                emailError = error,
            )
        }
    }

    fun onNotesChanged(newNotes: String) {
        _uiState.update { it.copy(notes = newNotes) }
    }

    private fun checkForDuplicate(phoneNormalized: String) {
        viewModelScope.launch {
            when (val result = findParticipantByPhoneUseCase(phoneNormalized)) {
                is AppResult.Success -> {
                    val existing = result.value
                    if (existing != null && existing.id != _uiState.value.participantId) {
                        _uiState.update {
                            it.copy(
                                duplicateParticipant = existing,
                                showDuplicateDialog = true,
                            )
                        }
                    }
                }
                is AppResult.Failure -> Unit
            }
        }
    }

    fun onUseExistingParticipant() {
        val existing = _uiState.value.duplicateParticipant ?: return
        _uiState.update {
            it.copy(
                name = existing.name,
                nameError = null,
                phoneRaw = existing.phoneDisplay,
                phoneNormalized = existing.phoneNormalized,
                phoneError = null,
                email = existing.email.orEmpty(),
                emailError = null,
                notes = existing.notes.orEmpty(),
                showDuplicateDialog = false,
            )
        }
    }

    fun onDismissDuplicateDialog() {
        _uiState.update {
            it.copy(showDuplicateDialog = false)
        }
    }

    fun saveParticipant() {
        val currentState = _uiState.value
        if (!currentState.isFormValid) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            val result = if (currentState.isEditMode && currentState.participantId != null) {
                updateParticipantUseCase(
                    participantId = currentState.participantId,
                    name = currentState.name,
                    phoneRaw = currentState.phoneRaw,
                    email = currentState.email,
                    notes = currentState.notes,
                )
            } else {
                createParticipantUseCase(
                    name = currentState.name,
                    phoneRaw = currentState.phoneRaw,
                    email = currentState.email,
                    notes = currentState.notes,
                )
            }

            when (result) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isSaveSuccess = true,
                        )
                    }
                }
                is AppResult.Failure -> {
                    val message = when (result.cause) {
                        is DuplicateParticipantPhoneException -> result.cause.message ?: "Nomor WhatsApp sudah terdaftar."
                        else -> result.cause.message ?: "Gagal menyimpan data peserta"
                    }
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = message,
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
