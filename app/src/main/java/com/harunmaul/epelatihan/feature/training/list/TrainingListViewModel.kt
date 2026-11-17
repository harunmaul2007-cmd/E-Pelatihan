package com.harunmaul.epelatihan.feature.training.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.domain.model.Training
import com.harunmaul.epelatihan.domain.model.TrainingLifecycle
import com.harunmaul.epelatihan.domain.model.UserRole
import com.harunmaul.epelatihan.domain.model.UserSession
import com.harunmaul.epelatihan.domain.usecase.ObserveSessionUseCase
import com.harunmaul.epelatihan.domain.usecase.ObserveTrainingsUseCase
import com.harunmaul.epelatihan.domain.validation.TrainingDisplayStatus
import com.harunmaul.epelatihan.domain.validation.TrainingStatusResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TrainingTab {
    ACTIVE,
    ARCHIVED,
}

enum class TrainingFilterOption {
    ALL,
    UPCOMING,
    ONGOING,
    COMPLETED,
}

data class TrainingListUiState(
    val trainings: List<Training> = emptyList(),
    val filteredTrainings: List<Training> = emptyList(),
    val isLoading: Boolean = true,
    val isFromCache: Boolean = false,
    val errorMessage: String? = null,
    val selectedTab: TrainingTab = TrainingTab.ACTIVE,
    val selectedFilter: TrainingFilterOption = TrainingFilterOption.ALL,
    val searchQuery: String = "",
    val currentUserRole: UserRole? = null,
)

@HiltViewModel
class TrainingListViewModel @Inject constructor(
    private val observeTrainingsUseCase: ObserveTrainingsUseCase,
    private val observeSessionUseCase: ObserveSessionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrainingListUiState())
    val uiState: StateFlow<TrainingListUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    init {
        viewModelScope.launch {
            observeSessionUseCase().collect { session ->
                val role = (session as? UserSession.Authenticated)?.profile?.role
                _uiState.update { it.copy(currentUserRole = role) }
            }
        }
        loadTrainings()
    }

    fun selectTab(tab: TrainingTab) {
        if (_uiState.value.selectedTab == tab) return
        _uiState.update {
            it.copy(
                selectedTab = tab,
                selectedFilter = TrainingFilterOption.ALL,
                isLoading = true,
                errorMessage = null,
            )
        }
        loadTrainings()
    }

    fun selectFilter(filter: TrainingFilterOption) {
        _uiState.update { state ->
            val updated = state.copy(selectedFilter = filter)
            updated.copy(filteredTrainings = applyFilters(state.trainings, state.selectedTab, filter, state.searchQuery))
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            val updated = state.copy(searchQuery = query)
            updated.copy(filteredTrainings = applyFilters(state.trainings, state.selectedTab, state.selectedFilter, query))
        }
    }

    fun retry() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        loadTrainings()
    }

    private fun loadTrainings() {
        observeJob?.cancel()
        val includeArchived = _uiState.value.selectedTab == TrainingTab.ARCHIVED
        observeJob = viewModelScope.launch {
            observeTrainingsUseCase(includeArchived = includeArchived).collect { result ->
                when (result) {
                    is AppResult.Success -> {
                        val allTrainings = result.value.trainings
                        val relevantTrainings = if (includeArchived) {
                            allTrainings.filter { it.lifecycle == TrainingLifecycle.ARCHIVED }
                        } else {
                            allTrainings.filter { it.lifecycle == TrainingLifecycle.ACTIVE }
                        }
                        val filtered = applyFilters(
                            trainings = relevantTrainings,
                            tab = _uiState.value.selectedTab,
                            filter = _uiState.value.selectedFilter,
                            query = _uiState.value.searchQuery,
                        )
                        _uiState.update {
                            it.copy(
                                trainings = relevantTrainings,
                                filteredTrainings = filtered,
                                isLoading = false,
                                isFromCache = result.value.isFromCache,
                                errorMessage = null,
                            )
                        }
                    }
                    is AppResult.Failure -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.cause.message ?: "Gagal memuat data pelatihan.",
                            )
                        }
                    }
                }
            }
        }
    }

    private fun applyFilters(
        trainings: List<Training>,
        tab: TrainingTab,
        filter: TrainingFilterOption,
        query: String,
    ): List<Training> {
        return trainings
            .filter { training ->
                if (tab == TrainingTab.ARCHIVED) {
                    true
                } else {
                    when (filter) {
                        TrainingFilterOption.ALL -> true
                        TrainingFilterOption.UPCOMING ->
                            TrainingStatusResolver.resolve(training) == TrainingDisplayStatus.AKAN_DATANG
                        TrainingFilterOption.ONGOING ->
                            TrainingStatusResolver.resolve(training) == TrainingDisplayStatus.BERLANGSUNG
                        TrainingFilterOption.COMPLETED ->
                            TrainingStatusResolver.resolve(training) == TrainingDisplayStatus.SELESAI
                    }
                }
            }
            .filter { training ->
                if (query.isBlank()) {
                    true
                } else {
                    val cleanQuery = query.trim()
                    training.name.contains(cleanQuery, ignoreCase = true) ||
                        training.speaker.contains(cleanQuery, ignoreCase = true)
                }
            }
    }
}
