package com.harunmaul.epelatihan.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.domain.model.TrainingLifecycle
import com.harunmaul.epelatihan.domain.model.UserProfile
import com.harunmaul.epelatihan.domain.model.UserSession
import com.harunmaul.epelatihan.domain.repository.RegistrationRepository
import com.harunmaul.epelatihan.domain.repository.TrainingRepository
import com.harunmaul.epelatihan.domain.usecase.LogoutUseCase
import com.harunmaul.epelatihan.domain.usecase.ObserveAppSettingsUseCase
import com.harunmaul.epelatihan.domain.usecase.ObserveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Month
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MonthlyParticipantTrend(
    val monthValue: Int,
    val monthName: String,
    val participantCount: Int,
)

data class HomeUiState(
    val appName: String = "E-Pelatihan",
    val communityName: String = "E-Pelatihan",
    val currentUser: UserProfile? = null,
    val activeTrainingsCount: Int = 0,
    val totalParticipantsCount: Int = 0,
    val currentYear: Int = YearMonth.now().year,
    val monthlyTrends: List<MonthlyParticipantTrend> = emptyList(),
    val totalYearParticipants: Int = 0,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeAppSettingsUseCase: ObserveAppSettingsUseCase,
    observeSessionUseCase: ObserveSessionUseCase,
    trainingRepository: TrainingRepository,
    registrationRepository: RegistrationRepository,
    private val logoutUseCase: LogoutUseCase,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        observeAppSettingsUseCase(),
        observeSessionUseCase(),
        trainingRepository.observeTrainings(includeArchived = true),
        registrationRepository.observeAllRegistrations(),
    ) { settings, session, trainingsResult, registrationsResult ->
        val user = (session as? UserSession.Authenticated)?.profile
        val allTrainings = (trainingsResult as? AppResult.Success)?.value?.trainings ?: emptyList()
        val allRegistrations = (registrationsResult as? AppResult.Success)?.value ?: emptyList()

        val activeTrainings = allTrainings.count { it.lifecycle == TrainingLifecycle.ACTIVE }
        // Total peserta yang ikut pelatihan (bukan kontak yang hanya tersimpan di daftar peserta)
        val totalParticipants = allRegistrations.size

        val currentYear = YearMonth.now().year
        val regByTraining = allRegistrations.groupBy { it.trainingId }
        val idLocale = Locale.forLanguageTag("id-ID")

        val monthlyTrends = (1..12).map { monthNum ->
            val month = Month.of(monthNum)
            val monthLabel = month.getDisplayName(TextStyle.SHORT, idLocale)

            val trainingsInMonth = allTrainings.filter { training ->
                val zoneId = try {
                    ZoneId.of(training.timeZoneId)
                } catch (_: Exception) {
                    ZoneId.of("Asia/Jakarta")
                }
                val trainingYearMonth = YearMonth.from(training.startAt.atZone(zoneId))
                trainingYearMonth.year == currentYear && trainingYearMonth.monthValue == monthNum
            }

            val count = trainingsInMonth.sumOf { training ->
                regByTraining[training.id]?.size ?: 0
            }

            MonthlyParticipantTrend(
                monthValue = monthNum,
                monthName = monthLabel,
                participantCount = count,
            )
        }

        val totalYearParticipants = monthlyTrends.sumOf { it.participantCount }

        HomeUiState(
            communityName = settings.communityName,
            currentUser = user,
            activeTrainingsCount = activeTrainings,
            totalParticipantsCount = totalParticipants,
            currentYear = currentYear,
            monthlyTrends = monthlyTrends,
            totalYearParticipants = totalYearParticipants,
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
