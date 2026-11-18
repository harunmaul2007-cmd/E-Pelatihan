package com.harunmaul.epelatihan.feature.participant.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.domain.model.Participant
import com.harunmaul.epelatihan.domain.model.UserRole
import com.harunmaul.epelatihan.domain.model.UserSession
import com.harunmaul.epelatihan.domain.usecase.ArchiveParticipantUseCase
import com.harunmaul.epelatihan.domain.usecase.ObserveParticipantDetailUseCase
import com.harunmaul.epelatihan.domain.usecase.ObserveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ParticipantDetailUiState(
    val isLoading: Boolean = true,
    val isFromCache: Boolean = false,
    val participant: Participant? = null,
    val isAdmin: Boolean = false,
    val isArchiving: Boolean = false,
    val isArchivedSuccess: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class ParticipantDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeParticipantDetailUseCase: ObserveParticipantDetailUseCase,
    private val archiveParticipantUseCase: ArchiveParticipantUseCase,
    private val observeSessionUseCase: ObserveSessionUseCase,
) : ViewModel() {

    val participantId: String = checkNotNull(savedStateHandle["participantId"])

    private val _uiState = MutableStateFlow(ParticipantDetailUiState())
    val uiState: StateFlow<ParticipantDetailUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    init {
        viewModelScope.launch {
            observeSessionUseCase().collect { session ->
                val isAdmin = (session as? UserSession.Authenticated)?.profile?.role == UserRole.ADMIN
                _uiState.update { it.copy(isAdmin = isAdmin) }
            }
        }
        loadDetail()
    }

    private fun loadDetail() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            observeParticipantDetailUseCase(participantId).collect { result ->
                when (result) {
                    is AppResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isFromCache = result.value.isFromCache,
                                participant = result.value.participant,
                                errorMessage = null,
                            )
                        }
                    }
                    is AppResult.Failure -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.cause.message ?: "Gagal memuat detail peserta",
                            )
                        }
                    }
                }
            }
        }
    }

    fun archiveParticipant() {
        viewModelScope.launch {
            _uiState.update { it.copy(isArchiving = true, errorMessage = null) }
            when (val result = archiveParticipantUseCase(participantId)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isArchiving = false,
                            isArchivedSuccess = true,
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isArchiving = false,
                            errorMessage = result.cause.message ?: "Gagal mengarsipkan peserta",
                        )
                    }
                }
            }
        }
    }
}
