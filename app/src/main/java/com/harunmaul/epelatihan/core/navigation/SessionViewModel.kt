package com.harunmaul.epelatihan.core.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harunmaul.epelatihan.domain.model.UserSession
import com.harunmaul.epelatihan.domain.usecase.LogoutUseCase
import com.harunmaul.epelatihan.domain.usecase.ObserveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SessionViewModel @Inject constructor(
    observeSessionUseCase: ObserveSessionUseCase,
    private val logoutUseCase: LogoutUseCase,
) : ViewModel() {

    val sessionState: StateFlow<UserSession> = observeSessionUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = UserSession.Loading,
        )

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
        }
    }
}
