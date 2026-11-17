package com.harunmaul.epelatihan.feature.training.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.domain.model.Training
import com.harunmaul.epelatihan.domain.model.UserRole
import com.harunmaul.epelatihan.domain.model.UserSession
import com.harunmaul.epelatihan.domain.usecase.ArchiveTrainingUseCase
import com.harunmaul.epelatihan.domain.usecase.ObserveSessionUseCase
import com.harunmaul.epelatihan.domain.usecase.ObserveTrainingDetailUseCase
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
    val currentUserRole: UserRole? = null,
)

@HiltViewModel
class TrainingDetailViewModel @Inject constructor(
    private val observeTrainingDetailUseCase: ObserveTrainingDetailUseCase,
    private val archiveTrainingUseCase: ArchiveTrainingUseCase,
    private val observeSessionUseCase: ObserveSessionUseCase,
    savedStateHandle: SavedStateHandle,
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
}
