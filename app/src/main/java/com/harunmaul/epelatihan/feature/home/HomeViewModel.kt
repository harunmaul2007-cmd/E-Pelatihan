package com.harunmaul.epelatihan.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harunmaul.epelatihan.domain.repository.AppInfoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val appName: String = "E-Pelatihan",
    val environment: String = "development",
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    appInfoRepository: AppInfoRepository,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = appInfoRepository.observeAppInfo()
        .map { appInfo ->
            HomeUiState(
                appName = appInfo.name,
                environment = appInfo.environment,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(),
        )
}
