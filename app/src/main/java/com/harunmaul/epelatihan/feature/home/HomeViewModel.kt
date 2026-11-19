package com.harunmaul.epelatihan.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.domain.model.UserProfile
import com.harunmaul.epelatihan.domain.model.UserSession
import com.harunmaul.epelatihan.domain.repository.ParticipantRepository
import com.harunmaul.epelatihan.domain.repository.TrainingRepository
import com.harunmaul.epelatihan.domain.usecase.LogoutUseCase
import com.harunmaul.epelatihan.domain.usecase.ObserveAppSettingsUseCase
import com.harunmaul.epelatihan.domain.usecase.ObserveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val appName: String = "E-Pelatihan",
    val communityName: String = "E-Pelatihan",
    val currentUser: UserProfile? = null,
    val activeTrainingsCount: Int = 0,
    val totalParticipantsCount: Int = 0,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeAppSettingsUseCase: ObserveAppSettingsUseCase,
    observeSessionUseCase: ObserveSessionUseCase,
    trainingRepository: TrainingRepository,
    participantRepository: ParticipantRepository,
    private val logoutUseCase: LogoutUseCase,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        observeAppSettingsUseCase(),
        observeSessionUseCase(),
        trainingRepository.observeTrainings(includeArchived = false),
        participantRepository.observeParticipants(includeInactive = false),
    ) { settings, session, trainingsResult, participantsResult ->
        val user = (session as? UserSession.Authenticated)?.profile
        val activeTrainings = (trainingsResult as? AppResult.Success)?.value?.trainings?.size ?: 0
        val totalParticipants = (participantsResult as? AppResult.Success)?.value?.participants?.size ?: 0

        HomeUiState(
            communityName = settings.communityName,
            currentUser = user,
            activeTrainingsCount = activeTrainings,
            totalParticipantsCount = totalParticipants,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
        }
    }
}
