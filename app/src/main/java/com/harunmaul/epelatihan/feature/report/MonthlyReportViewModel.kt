package com.harunmaul.epelatihan.feature.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.domain.model.MonthlyReport
import com.harunmaul.epelatihan.domain.usecase.ExportReportPdfUseCase
import com.harunmaul.epelatihan.domain.usecase.ExportReportXlsxUseCase
import com.harunmaul.epelatihan.domain.usecase.GenerateMonthlyReportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.OutputStream
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
    val isExporting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

@HiltViewModel
class MonthlyReportViewModel @Inject constructor(
    private val generateMonthlyReportUseCase: GenerateMonthlyReportUseCase,
    private val exportReportPdfUseCase: ExportReportPdfUseCase,
    private val exportReportXlsxUseCase: ExportReportXlsxUseCase,
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

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun exportPdf(outputStream: OutputStream, onSuccess: () -> Unit = {}) {
        val currentReport = _uiState.value.report ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, errorMessage = null) }
            try {
                outputStream.use { stream ->
                    when (val result = exportReportPdfUseCase(currentReport, stream)) {
                        is AppResult.Success -> {
                            _uiState.update {
                                it.copy(
                                    isExporting = false,
                                    successMessage = "Laporan PDF berhasil disimpan.",
                                )
                            }
                            onSuccess()
                        }
                        is AppResult.Failure -> {
                            _uiState.update {
                                it.copy(
                                    isExporting = false,
                                    errorMessage = result.cause.message ?: "Gagal mengekspor laporan PDF.",
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        errorMessage = e.message ?: "Gagal menulis file PDF.",
                    )
                }
            }
        }
    }

    fun exportXlsx(outputStream: OutputStream, onSuccess: () -> Unit = {}) {
        val currentReport = _uiState.value.report ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, errorMessage = null) }
            try {
                outputStream.use { stream ->
                    when (val result = exportReportXlsxUseCase(currentReport, stream)) {
                        is AppResult.Success -> {
                            _uiState.update {
                                it.copy(
                                    isExporting = false,
                                    successMessage = "Laporan Excel berhasil disimpan.",
                                )
                            }
                            onSuccess()
                        }
                        is AppResult.Failure -> {
                            _uiState.update {
                                it.copy(
                                    isExporting = false,
                                    errorMessage = result.cause.message ?: "Gagal mengekspor laporan Excel.",
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        errorMessage = e.message ?: "Gagal menulis file Excel.",
                    )
                }
            }
        }
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
