package com.harunmaul.epelatihan.feature.training.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.core.common.DuplicateRegistrationException
import com.harunmaul.epelatihan.domain.model.Participant
import com.harunmaul.epelatihan.domain.model.PaymentStatus
import com.harunmaul.epelatihan.domain.model.PricingType
import com.harunmaul.epelatihan.domain.model.RegistrationWithParticipant
import com.harunmaul.epelatihan.domain.model.Training
import com.harunmaul.epelatihan.domain.model.UserRole
import com.harunmaul.epelatihan.domain.model.UserSession
import com.harunmaul.epelatihan.domain.usecase.ArchiveTrainingUseCase
import com.harunmaul.epelatihan.domain.usecase.NewParticipantInput
import com.harunmaul.epelatihan.domain.usecase.ObserveParticipantsUseCase
import com.harunmaul.epelatihan.domain.usecase.ObserveSessionUseCase
import com.harunmaul.epelatihan.domain.usecase.ObserveTrainingDetailUseCase
import com.harunmaul.epelatihan.domain.usecase.ObserveTrainingRegistrationsUseCase
import com.harunmaul.epelatihan.domain.usecase.RegisterParticipantUseCase
import com.harunmaul.epelatihan.domain.usecase.UpdatePaymentStatusUseCase
import com.harunmaul.epelatihan.domain.usecase.UpdateRegistrationPriceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TrainingDetailUiState(
    val training: Training? = null,
    val isLoading: Boolean = true,
    val isFromCache: Boolean = false,
    val isArchiving: Boolean = false,
    val archiveSuccess: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val currentUserRole: UserRole? = null,
    // Registrations
    val registrations: List<RegistrationWithParticipant> = emptyList(),
    val availableParticipants: List<Participant> = emptyList(),
    val isRegistering: Boolean = false,
    val isUpdatingPrice: Boolean = false,
    val isUpdatingPaymentStatus: Boolean = false,
    val showRegisterDialog: Boolean = false,
    val showEditPriceDialog: Boolean = false,
    val selectedRegistrationForEdit: RegistrationWithParticipant? = null,
    val showPaymentStatusDialog: Boolean = false,
    val selectedRegistrationForPayment: RegistrationWithParticipant? = null,
    val showRegistrationDetailDialog: Boolean = false,
    val selectedRegistrationForDetail: RegistrationWithParticipant? = null,
)

@HiltViewModel
class TrainingDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeTrainingDetailUseCase: ObserveTrainingDetailUseCase,
    private val archiveTrainingUseCase: ArchiveTrainingUseCase,
    private val observeTrainingRegistrationsUseCase: ObserveTrainingRegistrationsUseCase,
    private val observeParticipantsUseCase: ObserveParticipantsUseCase,
    private val registerParticipantUseCase: RegisterParticipantUseCase,
    private val updateRegistrationPriceUseCase: UpdateRegistrationPriceUseCase,
    private val updatePaymentStatusUseCase: UpdatePaymentStatusUseCase,
    private val observeSessionUseCase: ObserveSessionUseCase,
) : ViewModel() {

    val trainingId: String = checkNotNull(savedStateHandle["trainingId"])

    private val _uiState = MutableStateFlow(TrainingDetailUiState())
    val uiState: StateFlow<TrainingDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSessionUseCase().collect { session ->
                val role = (session as? UserSession.Authenticated)?.profile?.role
                _uiState.update { it.copy(currentUserRole = role) }
            }
        }
        loadDetail()
        loadRegistrations()
        loadAvailableParticipants()
    }

    fun loadDetail() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            observeTrainingDetailUseCase(trainingId).collect { result ->
                when (result) {
                    is AppResult.Success -> {
                        _uiState.update {
                            it.copy(
                                training = result.value.training,
                                isFromCache = result.value.isFromCache,
                                isLoading = false,
                                errorMessage = null,
                            )
                        }
                    }
                    is AppResult.Failure -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.cause.message ?: "Gagal memuat detail pelatihan.",
                            )
                        }
                    }
                }
            }
        }
    }

    private fun loadRegistrations() {
        viewModelScope.launch {
            observeTrainingRegistrationsUseCase(trainingId).collect { result ->
                when (result) {
                    is AppResult.Success -> {
                        _uiState.update {
                            it.copy(registrations = result.value.items)
                        }
                    }
                    is AppResult.Failure -> {
                        _uiState.update {
                            it.copy(errorMessage = result.cause.message ?: "Gagal memuat pendaftaran.")
                        }
                    }
                }
            }
        }
    }

    private fun loadAvailableParticipants() {
        viewModelScope.launch {
            observeParticipantsUseCase(includeInactive = false).collect { result ->
                if (result is AppResult.Success) {
                    _uiState.update { it.copy(availableParticipants = result.value.participants) }
                }
            }
        }
    }

    fun openRegisterDialog() {
        _uiState.update { it.copy(showRegisterDialog = true) }
    }

    fun dismissRegisterDialog() {
        _uiState.update { it.copy(showRegisterDialog = false) }
    }

    fun registerExistingParticipant(participantId: String, pricingType: PricingType?) {
        val training = _uiState.value.training ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isRegistering = true, errorMessage = null) }
            val result = registerParticipantUseCase(
                training = training,
                participantId = participantId,
                selectedPricingType = pricingType,
            )
            when (result) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isRegistering = false,
                            showRegisterDialog = false,
                            successMessage = "Peserta berhasil didaftarkan.",
                        )
                    }
                }
                is AppResult.Failure -> {
                    val msg = when (result.cause) {
                        is DuplicateRegistrationException -> result.cause.message ?: "Peserta sudah terdaftar pada pelatihan ini."
                        else -> result.cause.message ?: "Gagal mendaftarkan peserta."
                    }
                    _uiState.update {
                        it.copy(
                            isRegistering = false,
                            errorMessage = msg,
                        )
                    }
                }
            }
        }
    }

    fun registerNewParticipant(
        name: String,
        phoneRaw: String,
        email: String?,
        notes: String?,
        pricingType: PricingType?,
    ) {
        val training = _uiState.value.training ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isRegistering = true, errorMessage = null) }
            val result = registerParticipantUseCase(
                training = training,
                newParticipantInput = NewParticipantInput(
                    name = name,
                    phoneRaw = phoneRaw,
                    email = email,
                    notes = notes,
                ),
                selectedPricingType = pricingType,
            )
            when (result) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isRegistering = false,
                            showRegisterDialog = false,
                            successMessage = "Peserta berhasil didaftarkan.",
                        )
                    }
                }
                is AppResult.Failure -> {
                    val msg = when (result.cause) {
                        is DuplicateRegistrationException -> result.cause.message ?: "Peserta sudah terdaftar pada pelatihan ini."
                        else -> result.cause.message ?: "Gagal mendaftarkan peserta."
                    }
                    _uiState.update {
                        it.copy(
                            isRegistering = false,
                            errorMessage = msg,
                        )
                    }
                }
            }
        }
    }

    fun openEditPriceDialog(item: RegistrationWithParticipant) {
        if (_uiState.value.currentUserRole == UserRole.ADMIN) {
            _uiState.update {
                it.copy(
                    showEditPriceDialog = true,
                    selectedRegistrationForEdit = item,
                )
            }
        }
    }

    fun dismissEditPriceDialog() {
        _uiState.update {
            it.copy(
                showEditPriceDialog = false,
                selectedRegistrationForEdit = null,
            )
        }
    }

    fun updateRegistrationPrice(newPricingType: PricingType) {
        val selected = _uiState.value.selectedRegistrationForEdit ?: return
        if (_uiState.value.currentUserRole != UserRole.ADMIN) {
            _uiState.update { it.copy(errorMessage = "Hanya Admin yang dapat mengubah jenis harga.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingPrice = true, errorMessage = null) }
            val result = updateRegistrationPriceUseCase(
                registrationId = selected.registration.id,
                newPricingType = newPricingType,
            )
            when (result) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isUpdatingPrice = false,
                            showEditPriceDialog = false,
                            selectedRegistrationForEdit = null,
                            successMessage = "Jenis harga pendaftaran berhasil diperbarui.",
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isUpdatingPrice = false,
                            errorMessage = result.cause.message ?: "Gagal mengubah jenis harga.",
                        )
                    }
                }
            }
        }
    }

    fun openRegistrationDetailDialog(item: RegistrationWithParticipant) {
        _uiState.update {
            it.copy(
                showRegistrationDetailDialog = true,
                selectedRegistrationForDetail = item,
            )
        }
    }

    fun dismissRegistrationDetailDialog() {
        _uiState.update {
            it.copy(
                showRegistrationDetailDialog = false,
                selectedRegistrationForDetail = null,
            )
        }
    }

    fun openPaymentStatusDialog(item: RegistrationWithParticipant) {
        if (_uiState.value.currentUserRole == UserRole.ADMIN) {
            _uiState.update {
                it.copy(
                    showPaymentStatusDialog = true,
                    selectedRegistrationForPayment = item,
                )
            }
        }
    }

    fun dismissPaymentStatusDialog() {
        _uiState.update {
            it.copy(
                showPaymentStatusDialog = false,
                selectedRegistrationForPayment = null,
            )
        }
    }

    fun updatePaymentStatus(newStatus: PaymentStatus) {
        val selected = _uiState.value.selectedRegistrationForPayment ?: return
        if (_uiState.value.currentUserRole != UserRole.ADMIN) {
            _uiState.update { it.copy(errorMessage = "Hanya Admin yang dapat mengubah status pembayaran.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingPaymentStatus = true, errorMessage = null) }
            val result = updatePaymentStatusUseCase(
                registrationId = selected.registration.id,
                newStatus = newStatus,
            )
            when (result) {
                is AppResult.Success -> {
                    val msg = if (newStatus == PaymentStatus.LUNAS) {
                        "Status pembayaran berhasil ditandai Lunas."
                    } else {
                        "Status pembayaran berhasil diubah menjadi Belum Bayar."
                    }
                    _uiState.update {
                        it.copy(
                            isUpdatingPaymentStatus = false,
                            showPaymentStatusDialog = false,
                            selectedRegistrationForPayment = null,
                            showRegistrationDetailDialog = false,
                            selectedRegistrationForDetail = null,
                            successMessage = msg,
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isUpdatingPaymentStatus = false,
                            errorMessage = result.cause.message ?: "Gagal mengubah status pembayaran.",
                        )
                    }
                }
            }
        }
    }

    fun archiveTraining() {
        val training = _uiState.value.training ?: return
        if (_uiState.value.currentUserRole != UserRole.ADMIN) {
            _uiState.update { it.copy(errorMessage = "Hanya Admin yang dapat mengarsipkan pelatihan.") }
            return
        }

        _uiState.update { it.copy(isArchiving = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = archiveTrainingUseCase(training.id)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isArchiving = false, archiveSuccess = true) }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isArchiving = false,
                            errorMessage = result.cause.message ?: "Gagal mengarsipkan pelatihan.",
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
