package com.harunmaul.epelatihan.feature.participant.importwa

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.domain.repository.ParticipantRepository
import com.harunmaul.epelatihan.domain.usecase.CreateParticipantUseCase
import com.harunmaul.epelatihan.domain.validation.PhoneNormalizationResult
import com.harunmaul.epelatihan.domain.validation.PhoneNumberNormalizer
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.InputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ImportSummary(
    val totalImported: Int,
    val totalSkipped: Int,
    val totalFailed: Int,
)

data class ImportWhatsAppUiState(
    val rawText: String = "",
    val candidates: List<ParsedContactCandidate> = emptyList(),
    val isAnalyzing: Boolean = false,
    val isImporting: Boolean = false,
    val importProgress: Pair<Int, Int>? = null, // current to total
    val importSummary: ImportSummary? = null,
    val errorMessage: String? = null,
) {
    val selectedCount: Int
        get() = candidates.count { it.isSelected && it.isValidPhone }
    val canImport: Boolean
        get() = selectedCount > 0 && !isImporting && !isAnalyzing
}

@HiltViewModel
class ImportWhatsAppViewModel @Inject constructor(
    private val participantRepository: ParticipantRepository,
    private val createParticipantUseCase: CreateParticipantUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportWhatsAppUiState())
    val uiState: StateFlow<ImportWhatsAppUiState> = _uiState.asStateFlow()

    fun onRawTextChanged(value: String) {
        _uiState.update { it.copy(rawText = value, errorMessage = null) }
    }

    fun parseFromRawText() {
        val text = _uiState.value.rawText
        if (text.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Teks daftar kontak WhatsApp masih kosong.") }
            return
        }
        processCandidates(WhatsAppContactParser.parseText(text))
    }

    fun parseFromFile(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, errorMessage = null) }
            try {
                val candidates = withContext(Dispatchers.IO) {
                    val stream: InputStream? = contentResolver.openInputStream(uri)
                    stream?.use {
                        // Check if it's VCF or plain text
                        val textContent = it.bufferedReader().use { r -> r.readText() }
                        if (textContent.contains("BEGIN:VCARD", ignoreCase = true)) {
                            WhatsAppContactParser.parseVCard(textContent.byteInputStream())
                        } else {
                            WhatsAppContactParser.parseText(textContent)
                        }
                    } ?: emptyList()
                }
                processCandidates(candidates)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        errorMessage = "Gagal membaca file: ${e.localizedMessage ?: "Format tidak didukung"}",
                    )
                }
            }
        }
    }

    private fun processCandidates(initialList: List<ParsedContactCandidate>) {
        if (initialList.isEmpty()) {
            _uiState.update {
                it.copy(
                    isAnalyzing = false,
                    candidates = emptyList(),
                    errorMessage = "Tidak ditemukan nomor kontak yang valid pada teks/file yang dipilih.",
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, errorMessage = null) }
            val checkedList = withContext(Dispatchers.IO) {
                initialList.map { candidate ->
                    if (candidate.isValidPhone) {
                        val existsResult = participantRepository.findParticipantByPhone(candidate.normalizedPhone)
                        val alreadyExists = (existsResult as? AppResult.Success)?.value != null
                        candidate.copy(
                            isAlreadyRegistered = alreadyExists,
                            isSelected = !alreadyExists && candidate.isValidPhone,
                        )
                    } else {
                        candidate.copy(isSelected = false)
                    }
                }
            }
            _uiState.update {
                it.copy(
                    isAnalyzing = false,
                    candidates = checkedList,
                    errorMessage = null,
                )
            }
        }
    }

    fun onToggleCandidateSelection(index: Int) {
        _uiState.update { state ->
            val updated = state.candidates.toMutableList()
            if (index in updated.indices) {
                val current = updated[index]
                updated[index] = current.copy(isSelected = !current.isSelected)
            }
            state.copy(candidates = updated)
        }
    }

    fun onToggleSelectAll(selected: Boolean) {
        _uiState.update { state ->
            val updated = state.candidates.map { candidate ->
                if (candidate.isValidPhone && !candidate.isAlreadyRegistered) {
                    candidate.copy(isSelected = selected)
                } else {
                    candidate.copy(isSelected = false)
                }
            }
            state.copy(candidates = updated)
        }
    }

    fun onUpdateCandidateName(index: Int, newName: String) {
        _uiState.update { state ->
            val updated = state.candidates.toMutableList()
            if (index in updated.indices) {
                updated[index] = updated[index].copy(name = newName)
            }
            state.copy(candidates = updated)
        }
    }

    fun onRemoveCandidate(index: Int) {
        _uiState.update { state ->
            val updated = state.candidates.toMutableList()
            if (index in updated.indices) {
                updated.removeAt(index)
            }
            state.copy(candidates = updated)
        }
    }

    fun executeImport() {
        val selected = _uiState.value.candidates.filter { it.isSelected && it.isValidPhone }
        if (selected.isEmpty()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isImporting = true,
                    importProgress = Pair(0, selected.size),
                    errorMessage = null,
                )
            }

            var successCount = 0
            var skippedCount = 0
            var failedCount = 0

            selected.forEachIndexed { i, candidate ->
                _uiState.update { it.copy(importProgress = Pair(i + 1, selected.size)) }
                val result = createParticipantUseCase(
                    name = candidate.name.ifBlank { "Peserta WA (" + candidate.normalizedPhone.takeLast(4) + ")" },
                    phoneRaw = candidate.rawPhone,
                    notes = "Import dari WhatsApp",
                )
                when (result) {
                    is AppResult.Success -> successCount++
                    is AppResult.Failure -> {
                        if (result.cause.message?.contains("sudah terdaftar", ignoreCase = true) == true) {
                            skippedCount++
                        } else {
                            failedCount++
                        }
                    }
                }
            }

            _uiState.update {
                it.copy(
                    isImporting = false,
                    importProgress = null,
                    importSummary = ImportSummary(
                        totalImported = successCount,
                        totalSkipped = skippedCount,
                        totalFailed = failedCount,
                    ),
                    candidates = emptyList(),
                    rawText = "",
                )
            }
        }
    }

    fun resetSummary() {
        _uiState.update { it.copy(importSummary = null, errorMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
