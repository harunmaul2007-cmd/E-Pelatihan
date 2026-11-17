package com.harunmaul.epelatihan.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harunmaul.epelatihan.domain.model.UserProfile
import com.harunmaul.epelatihan.domain.model.UserSession
import com.harunmaul.epelatihan.domain.repository.AppInfoRepository
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
    val environment: String = "development",
    val firebaseConfigured: Boolean = false,
    val currentUser: UserProfile? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    appInfoRepository: AppInfoRepository,
    observeAppSettingsUseCase: ObserveAppSettingsUseCase,
    observeSessionUseCase: ObserveSessionUseCase,
    private val logoutUseCase: LogoutUseCase,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        appInfoRepository.observeAppInfo(),
        observeAppSettingsUseCase(),
        observeSessionUseCase(),
    ) { appInfo, settings, session ->
        val user = (session as? UserSession.Authenticated)?.profile
        HomeUiState(
            appName = appInfo.name,
            communityName = settings.communityName,
            environment = appInfo.environment,
            firebaseConfigured = appInfo.firebaseConfigured,
            currentUser = user,
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
