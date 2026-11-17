package com.harunmaul.epelatihan.feature.training.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.domain.model.MeetingMethod
import com.harunmaul.epelatihan.domain.model.PricingScheme
import com.harunmaul.epelatihan.domain.model.Training
import com.harunmaul.epelatihan.domain.model.TrainingLifecycle
import com.harunmaul.epelatihan.domain.model.UserRole
import com.harunmaul.epelatihan.domain.model.UserSession
import com.harunmaul.epelatihan.domain.repository.TrainingRepository
import com.harunmaul.epelatihan.domain.usecase.CreateTrainingUseCase
import com.harunmaul.epelatihan.domain.usecase.ObserveSessionUseCase
import com.harunmaul.epelatihan.domain.usecase.UpdateTrainingUseCase
import com.harunmaul.epelatihan.domain.validation.TrainingValidator
import com.harunmaul.epelatihan.domain.validation.ValidationCode
import com.harunmaul.epelatihan.domain.validation.ValidationField
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TrainingFormUiState(
    val isEditMode: Boolean = false,
    val trainingId: String? = null,
    val name: String = "",
    val description: String = "",
    val selectedDate: LocalDate = LocalDate.now().plusDays(1),
    val startTime: LocalTime = LocalTime.of(9, 0),
    val endTime: LocalTime = LocalTime.of(12, 0),
    val timeZoneId: String = "Asia/Jakarta",
    val speaker: String = "",
    val meetingMethod: MeetingMethod = MeetingMethod.ONLINE,
    val zoomUrl: String = "",
    val location: String = "",
    val recordingUrl: String = "",
    val materialUrl: String = "",
    val pricingScheme: PricingScheme = PricingScheme.NORMAL_ONLY,
    val normalPriceInput: String = "",
    val earlyBirdPriceInput: String = "",
    // Field-specific validation errors
    val nameError: String? = null,
    val speakerError: String? = null,
    val timeError: String? = null,
    val zoomUrlError: String? = null,
    val locationError: String? = null,
    val recordingUrlError: String? = null,
    val materialUrlError: String? = null,
    val normalPriceError: String? = null,
    val earlyBirdPriceError: String? = null,
    // Status
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null,
    val currentUserRole: UserRole? = null,
    val isAccessDenied: Boolean = false,
)

@HiltViewModel
class TrainingFormViewModel @Inject constructor(
    private val createTrainingUseCase: CreateTrainingUseCase,
    private val updateTrainingUseCase: UpdateTrainingUseCase,
    private val trainingRepository: TrainingRepository,
    private val observeSessionUseCase: ObserveSessionUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val trainingId: String? = savedStateHandle["trainingId"]
    val isEditMode: Boolean = !trainingId.isNullOrBlank()

    private val _uiState = MutableStateFlow(
        TrainingFormUiState(
            isEditMode = isEditMode,
            trainingId = trainingId,
            timeZoneId = try {
                ZoneId.systemDefault().id
            } catch (_: Exception) {
                "Asia/Jakarta"
            },
        )
    )
    val uiState: StateFlow<TrainingFormUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSessionUseCase().collect { session ->
                val role = (session as? UserSession.Authenticated)?.profile?.role
                _uiState.update { state ->
                    val denied = isEditMode && role != null && role != UserRole.ADMIN
                    state.copy(currentUserRole = role, isAccessDenied = denied)
                }
            }
        }

        if (isEditMode && !trainingId.isNullOrBlank()) {
            loadExistingTraining(trainingId)
        }
    }

    private fun loadExistingTraining(id: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = trainingRepository.getTraining(id)) {
                is AppResult.Success -> {
                    val training = result.value
                    if (training != null) {
                        val zone = try {
                            ZoneId.of(training.timeZoneId)
                        } catch (_: Exception) {
                            ZoneId.of("Asia/Jakarta")
                        }
                        val startZoned = training.startAt.atZone(zone)
                        val endZoned = training.endAt.atZone(zone)

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                name = training.name,
                                description = training.description ?: "",
                                selectedDate = startZoned.toLocalDate(),
                                startTime = startZoned.toLocalTime(),
                                endTime = endZoned.toLocalTime(),
                                timeZoneId = training.timeZoneId,
                                speaker = training.speaker,
                                meetingMethod = training.meetingMethod,
                                zoomUrl = training.zoomUrl ?: "",
                                location = training.location ?: "",
                                recordingUrl = training.recordingUrl ?: "",
                                materialUrl = training.materialUrl ?: "",
                                pricingScheme = training.pricingScheme,
                                normalPriceInput = training.normalPrice.toString(),
                                earlyBirdPriceInput = training.earlyBirdPrice?.toString() ?: "",
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Pelatihan tidak ditemukan.",
                            )
                        }
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

    fun onNameChanged(value: String) {
        _uiState.update { it.copy(name = value, nameError = null, errorMessage = null) }
    }

    fun onDescriptionChanged(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    fun onDateChanged(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date, timeError = null) }
    }

    fun onStartTimeChanged(time: LocalTime) {
        _uiState.update { it.copy(startTime = time, timeError = null) }
    }

    fun onEndTimeChanged(time: LocalTime) {
        _uiState.update { it.copy(endTime = time, timeError = null) }
    }

    fun onTimeZoneChanged(zoneId: String) {
        _uiState.update { it.copy(timeZoneId = zoneId) }
    }

    fun onSpeakerChanged(value: String) {
        _uiState.update { it.copy(speaker = value, speakerError = null, errorMessage = null) }
    }

    fun onMeetingMethodChanged(method: MeetingMethod) {
        _uiState.update {
            it.copy(
                meetingMethod = method,
                zoomUrlError = null,
                locationError = null,
            )
        }
    }

    fun onZoomUrlChanged(value: String) {
        _uiState.update { it.copy(zoomUrl = value, zoomUrlError = null) }
    }

    fun onLocationChanged(value: String) {
        _uiState.update { it.copy(location = value, locationError = null) }
    }

    fun onRecordingUrlChanged(value: String) {
        _uiState.update { it.copy(recordingUrl = value, recordingUrlError = null) }
    }

    fun onMaterialUrlChanged(value: String) {
        _uiState.update { it.copy(materialUrl = value, materialUrlError = null) }
    }

    fun onPricingSchemeChanged(scheme: PricingScheme) {
        _uiState.update {
            it.copy(
                pricingScheme = scheme,
                earlyBirdPriceError = null,
            )
        }
    }

    fun onNormalPriceChanged(value: String) {
        val filtered = value.filter { it.isDigit() }
        _uiState.update { it.copy(normalPriceInput = filtered, normalPriceError = null) }
    }

    fun onEarlyBirdPriceChanged(value: String) {
        val filtered = value.filter { it.isDigit() }
        _uiState.update { it.copy(earlyBirdPriceInput = filtered, earlyBirdPriceError = null) }
    }

    fun saveTraining() {
        val state = _uiState.value
        if (state.isAccessDenied) return

        val zone = try {
            ZoneId.of(state.timeZoneId)
        } catch (_: Exception) {
            ZoneId.of("Asia/Jakarta")
        }

        val startAt = state.selectedDate.atTime(state.startTime).atZone(zone).toInstant()
        val endAt = state.selectedDate.atTime(state.endTime).atZone(zone).toInstant()

        val normalPrice = state.normalPriceInput.toLongOrNull() ?: -1L
        val earlyBirdPrice = if (state.pricingScheme == PricingScheme.EARLY_BIRD_AND_NORMAL) {
            state.earlyBirdPriceInput.toLongOrNull()
        } else {
            null
        }

        val candidate = Training(
            id = state.trainingId ?: "",
            name = state.name.trim(),
            description = state.description.trim().ifEmpty { null },
            startAt = startAt,
            endAt = endAt,
            timeZoneId = state.timeZoneId,
            speaker = state.speaker.trim(),
            meetingMethod = state.meetingMethod,
            zoomUrl = if (state.meetingMethod == MeetingMethod.ONLINE) state.zoomUrl.trim().ifEmpty { null } else null,
            location = if (state.meetingMethod == MeetingMethod.OFFLINE) state.location.trim().ifEmpty { null } else null,
            recordingUrl = state.recordingUrl.trim().ifEmpty { null },
            materialUrl = state.materialUrl.trim().ifEmpty { null },
            pricingScheme = state.pricingScheme,
            normalPrice = normalPrice,
            earlyBirdPrice = earlyBirdPrice,
            lifecycle = TrainingLifecycle.ACTIVE,
            createdBy = "",
            createdAt = null,
            updatedBy = "",
            updatedAt = null,
        )

        // Validasi menggunakan TrainingValidator
        val validation = TrainingValidator.validate(candidate)
        if (validation is com.harunmaul.epelatihan.domain.validation.ValidationResult.Invalid) {
            var nameErr: String? = null
            var speakerErr: String? = null
            var timeErr: String? = null
            var zoomErr: String? = null
            var locErr: String? = null
            var recErr: String? = null
            var matErr: String? = null
            var normErr: String? = null
            var earlyErr: String? = null

            for (issue in validation.issues) {
                when (issue.field) {
                    ValidationField.TRAINING_NAME -> nameErr = issue.message
                    ValidationField.SPEAKER -> speakerErr = issue.message
                    ValidationField.END_AT -> timeErr = issue.message
                    ValidationField.ZOOM_URL -> zoomErr = issue.message
                    ValidationField.LOCATION -> locErr = issue.message
                    ValidationField.RECORDING_URL -> recErr = issue.message
                    ValidationField.MATERIAL_URL -> matErr = issue.message
                    ValidationField.NORMAL_PRICE -> normErr = issue.message
                    ValidationField.EARLY_BIRD_PRICE -> earlyErr = issue.message
                    else -> {}
                }
            }

            _uiState.update {
                it.copy(
                    nameError = nameErr,
                    speakerError = speakerErr,
                    timeError = timeErr,
                    zoomUrlError = zoomErr,
                    locationError = locErr,
                    recordingUrlError = recErr,
                    materialUrlError = matErr,
                    normalPriceError = normErr,
                    earlyBirdPriceError = earlyErr,
                )
            }
            return
        }

        // Jalankan use case
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            val result = if (isEditMode) {
                updateTrainingUseCase(candidate)
            } else {
                createTrainingUseCase(candidate)
            }

            when (result) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = result.cause.message ?: "Gagal menyimpan pelatihan.",
                        )
                    }
                }
            }
        }
    }
}
