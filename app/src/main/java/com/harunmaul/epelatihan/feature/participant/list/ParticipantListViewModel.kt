package com.harunmaul.epelatihan.feature.participant.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.domain.model.Participant
import com.harunmaul.epelatihan.domain.model.UserRole
import com.harunmaul.epelatihan.domain.model.UserSession
import com.harunmaul.epelatihan.domain.usecase.ObserveParticipantsUseCase
import com.harunmaul.epelatihan.domain.usecase.ObserveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ParticipantListUiState(
    val isLoading: Boolean = true,
    val isFromCache: Boolean = false,
    val participants: List<Participant> = emptyList(),
    val searchQuery: String = "",
    val includeInactive: Boolean = false,
    val isAdmin: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class ParticipantListViewModel @Inject constructor(
    private val observeParticipantsUseCase: ObserveParticipantsUseCase,
    private val observeSessionUseCase: ObserveSessionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ParticipantListUiState())
    val uiState: StateFlow<ParticipantListUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    init {
        viewModelScope.launch {
            observeSessionUseCase().collect { session ->
                val isAdmin = (session as? UserSession.Authenticated)?.profile?.role == UserRole.ADMIN
                _uiState.update { it.copy(isAdmin = isAdmin) }
            }
        }
        loadParticipants()
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadParticipants()
    }

    fun onIncludeInactiveToggled(include: Boolean) {
        _uiState.update { it.copy(includeInactive = include) }
        loadParticipants()
    }

    fun retry() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        loadParticipants()
    }

    private fun loadParticipants() {
        observeJob?.cancel()
        val query = _uiState.value.searchQuery
        val includeInactive = _uiState.value.includeInactive
        observeJob = viewModelScope.launch {
            observeParticipantsUseCase(query = query, includeInactive = includeInactive).collect { result ->
                when (result) {
                    is AppResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isFromCache = result.value.isFromCache,
                                participants = result.value.participants,
                                errorMessage = null,
                            )
                        }
                    }
                    is AppResult.Failure -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.cause.message ?: "Gagal memuat data peserta",
                            )
                        }
                    }
                }
            }
        }
    }
}
