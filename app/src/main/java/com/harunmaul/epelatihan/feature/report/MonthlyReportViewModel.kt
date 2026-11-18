package com.harunmaul.epelatihan.feature.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.domain.model.MonthlyReport
import com.harunmaul.epelatihan.domain.usecase.GenerateMonthlyReportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MonthlyReportUiState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val report: MonthlyReport? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

@HiltViewModel
class MonthlyReportViewModel @Inject constructor(
    private val generateMonthlyReportUseCase: GenerateMonthlyReportUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MonthlyReportUiState())
    val uiState: StateFlow<MonthlyReportUiState> = _uiState.asStateFlow()

    init {
        loadReport(_uiState.value.selectedMonth)
    }

    fun selectMonth(month: YearMonth) {
        if (_uiState.value.selectedMonth != month) {
            _uiState.update { it.copy(selectedMonth = month) }
            loadReport(month)
        }
    }

    fun previousMonth() {
        selectMonth(_uiState.value.selectedMonth.minusMonths(1))
    }

    fun nextMonth() {
        selectMonth(_uiState.value.selectedMonth.plusMonths(1))
    }

    fun refresh() {
        loadReport(_uiState.value.selectedMonth)
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun loadReport(month: YearMonth) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = generateMonthlyReportUseCase(month)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            report = result.value,
                            isLoading = false,
                            errorMessage = null,
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.cause.message ?: "Gagal memuat laporan bulanan.",
                        )
                    }
                }
            }
        }
    }
}
