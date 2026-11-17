package com.harunmaul.epelatihan.feature.training.form

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.harunmaul.epelatihan.R
import com.harunmaul.epelatihan.domain.model.MeetingMethod
import com.harunmaul.epelatihan.domain.model.PricingScheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TrainingFormRoute(
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: TrainingFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            val msg = if (uiState.isEditMode) {
                context.getString(R.string.training_updated_success)
            } else {
                context.getString(R.string.training_saved_success)
            }
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            onSaveSuccess()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    TrainingFormScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onNameChanged = viewModel::onNameChanged,
        onDescriptionChanged = viewModel::onDescriptionChanged,
        onDateChanged = viewModel::onDateChanged,
        onStartTimeChanged = viewModel::onStartTimeChanged,
        onEndTimeChanged = viewModel::onEndTimeChanged,
        onTimeZoneChanged = viewModel::onTimeZoneChanged,
        onSpeakerChanged = viewModel::onSpeakerChanged,
        onMeetingMethodChanged = viewModel::onMeetingMethodChanged,
        onZoomUrlChanged = viewModel::onZoomUrlChanged,
        onLocationChanged = viewModel::onLocationChanged,
        onRecordingUrlChanged = viewModel::onRecordingUrlChanged,
        onMaterialUrlChanged = viewModel::onMaterialUrlChanged,
        onPricingSchemeChanged = viewModel::onPricingSchemeChanged,
        onNormalPriceChanged = viewModel::onNormalPriceChanged,
        onEarlyBirdPriceChanged = viewModel::onEarlyBirdPriceChanged,
        onSaveClick = viewModel::saveTraining,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TrainingFormScreen(
    uiState: TrainingFormUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onNavigateBack: () -> Unit = {},
    onNameChanged: (String) -> Unit = {},
    onDescriptionChanged: (String) -> Unit = {},
    onDateChanged: (LocalDate) -> Unit = {},
    onStartTimeChanged: (LocalTime) -> Unit = {},
    onEndTimeChanged: (LocalTime) -> Unit = {},
    onTimeZoneChanged: (String) -> Unit = {},
    onSpeakerChanged: (String) -> Unit = {},
    onMeetingMethodChanged: (MeetingMethod) -> Unit = {},
    onZoomUrlChanged: (String) -> Unit = {},
    onLocationChanged: (String) -> Unit = {},
    onRecordingUrlChanged: (String) -> Unit = {},
    onMaterialUrlChanged: (String) -> Unit = {},
    onPricingSchemeChanged: (PricingScheme) -> Unit = {},
    onNormalPriceChanged: (String) -> Unit = {},
    onEarlyBirdPriceChanged: (String) -> Unit = {},
    onSaveClick: () -> Unit = {},
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.forLanguageTag("id-ID")) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    // DatePickerDialog Material 3
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.selectedDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selected = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        onDateChanged(selected)
                    }
                    showDatePicker = false
                }) {
                    Text("Pilih")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // TimePicker Jam Mulai
    if (showStartTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = uiState.startTime.hour,
            initialMinute = uiState.startTime.minute,
            is24Hour = true,
        )
        TimePickerDialog(
            title = "Pilih Jam Mulai",
            onDismissRequest = { showStartTimePicker = false },
            onConfirm = {
                onStartTimeChanged(LocalTime.of(timePickerState.hour, timePickerState.minute))
                showStartTimePicker = false
            },
        ) {
            TimePicker(state = timePickerState)
        }
    }

    // TimePicker Jam Selesai
    if (showEndTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = uiState.endTime.hour,
            initialMinute = uiState.endTime.minute,
            is24Hour = true,
        )
        TimePickerDialog(
            title = "Pilih Jam Selesai",
            onDismissRequest = { showEndTimePicker = false },
            onConfirm = {
                onEndTimeChanged(LocalTime.of(timePickerState.hour, timePickerState.minute))
                showEndTimePicker = false
            },
        ) {
            TimePicker(state = timePickerState)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (uiState.isEditMode) {
                                R.string.training_form_title_edit
                            } else {
                                R.string.training_form_title_create
                            }
                        ),
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            when {
                uiState.isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.loading_trainings))
                    }
                }

                uiState.isAccessDenied -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Akses Ditolak",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Hanya Administrator yang memiliki wewenang untuk mengubah data pelatihan tersimpan.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(onClick = onNavigateBack) {
                            Text(stringResource(R.string.back))
                        }
                    }
                }

                else -> {
                    val scrollState = rememberScrollState()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        // 1. Bagian Informasi Dasar
                        FormSectionCard(
                            icon = Icons.Default.Description,
                            title = "Informasi Kegiatan",
                        ) {
                            OutlinedTextField(
                                value = uiState.name,
                                onValueChange = onNameChanged,
                                label = { Text(stringResource(R.string.training_name_label)) },
                                placeholder = { Text(stringResource(R.string.training_name_placeholder)) },
                                isError = uiState.nameError != null,
                                supportingText = uiState.nameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = uiState.speaker,
                                onValueChange = onSpeakerChanged,
                                label = { Text(stringResource(R.string.training_speaker_label)) },
                                placeholder = { Text(stringResource(R.string.training_speaker_placeholder)) },
                                isError = uiState.speakerError != null,
                                supportingText = uiState.speakerError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = uiState.description,
                                onValueChange = onDescriptionChanged,
                                label = { Text(stringResource(R.string.training_description_label)) },
                                placeholder = { Text(stringResource(R.string.training_description_placeholder)) },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 4,
                            )
                        }

                        // 2. Bagian Jadwal & Waktu
                        FormSectionCard(
                            icon = Icons.Default.CalendarMonth,
                            title = stringResource(R.string.training_schedule),
                        ) {
                            // Tanggal
                            Text(
                                text = stringResource(R.string.training_date_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedButton(
                                onClick = { showDatePicker = true },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(uiState.selectedDate.format(dateFormatter))
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Jam Mulai & Jam Selesai
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.training_start_time_label),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedButton(
                                        onClick = { showStartTimePicker = true },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Icon(Icons.Default.AccessTime, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(uiState.startTime.format(timeFormatter))
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.training_end_time_label),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedButton(
                                        onClick = { showEndTimePicker = true },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Icon(Icons.Default.AccessTime, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(uiState.endTime.format(timeFormatter))
                                    }
                                }
                            }

                            if (uiState.timeError != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = uiState.timeError,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Zona Waktu
                            Text(
                                text = stringResource(R.string.training_timezone_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = uiState.timeZoneId == "Asia/Jakarta",
                                    onClick = { onTimeZoneChanged("Asia/Jakarta") },
                                    label = { Text("WIB (Asia/Jakarta)") },
                                )
                                FilterChip(
                                    selected = uiState.timeZoneId == "Asia/Makassar",
                                    onClick = { onTimeZoneChanged("Asia/Makassar") },
                                    label = { Text("WITA (Asia/Makassar)") },
                                )
                                FilterChip(
                                    selected = uiState.timeZoneId == "Asia/Jayapura",
                                    onClick = { onTimeZoneChanged("Asia/Jayapura") },
                                    label = { Text("WIT (Asia/Jayapura)") },
                                )
                            }
                        }

                        // 3. Bagian Metode Pelaksanaan
                        FormSectionCard(
                            icon = if (uiState.meetingMethod == MeetingMethod.ONLINE) {
                                Icons.Default.Videocam
                            } else {
                                Icons.Default.LocationOn
                            },
                            title = stringResource(R.string.training_method_label),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                FilterChip(
                                    selected = uiState.meetingMethod == MeetingMethod.ONLINE,
                                    onClick = { onMeetingMethodChanged(MeetingMethod.ONLINE) },
                                    label = { Text(stringResource(R.string.method_online)) },
                                    leadingIcon = {
                                        if (uiState.meetingMethod == MeetingMethod.ONLINE) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                                FilterChip(
                                    selected = uiState.meetingMethod == MeetingMethod.OFFLINE,
                                    onClick = { onMeetingMethodChanged(MeetingMethod.OFFLINE) },
                                    label = { Text(stringResource(R.string.method_offline)) },
                                    leadingIcon = {
                                        if (uiState.meetingMethod == MeetingMethod.OFFLINE) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (uiState.meetingMethod == MeetingMethod.ONLINE) {
                                OutlinedTextField(
                                    value = uiState.zoomUrl,
                                    onValueChange = onZoomUrlChanged,
                                    label = { Text(stringResource(R.string.training_zoom_label)) },
                                    placeholder = { Text(stringResource(R.string.training_zoom_placeholder)) },
                                    isError = uiState.zoomUrlError != null,
                                    supportingText = uiState.zoomUrlError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                )
                            } else {
                                OutlinedTextField(
                                    value = uiState.location,
                                    onValueChange = onLocationChanged,
                                    label = { Text(stringResource(R.string.training_location_label)) },
                                    placeholder = { Text(stringResource(R.string.training_location_placeholder)) },
                                    isError = uiState.locationError != null,
                                    supportingText = uiState.locationError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                )
                            }
                        }

                        // 4. Bagian Tautan Eksternal Opsional
                        FormSectionCard(
                            icon = Icons.Default.Info,
                            title = "Tautan Materi & Rekaman",
                        ) {
                            Text(
                                text = stringResource(R.string.drive_share_reminder),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = uiState.materialUrl,
                                onValueChange = onMaterialUrlChanged,
                                label = { Text(stringResource(R.string.training_material_label)) },
                                placeholder = { Text(stringResource(R.string.training_material_placeholder)) },
                                isError = uiState.materialUrlError != null,
                                supportingText = uiState.materialUrlError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = uiState.recordingUrl,
                                onValueChange = onRecordingUrlChanged,
                                label = { Text(stringResource(R.string.training_recording_label)) },
                                placeholder = { Text(stringResource(R.string.training_recording_placeholder)) },
                                isError = uiState.recordingUrlError != null,
                                supportingText = uiState.recordingUrlError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )
                        }

                        // 5. Bagian Skema Harga
                        FormSectionCard(
                            icon = Icons.Default.Payments,
                            title = stringResource(R.string.training_pricing_scheme_label),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                FilterChip(
                                    selected = uiState.pricingScheme == PricingScheme.NORMAL_ONLY,
                                    onClick = { onPricingSchemeChanged(PricingScheme.NORMAL_ONLY) },
                                    label = { Text(stringResource(R.string.pricing_normal_only)) },
                                    modifier = Modifier.weight(1f),
                                )
                                FilterChip(
                                    selected = uiState.pricingScheme == PricingScheme.EARLY_BIRD_AND_NORMAL,
                                    onClick = { onPricingSchemeChanged(PricingScheme.EARLY_BIRD_AND_NORMAL) },
                                    label = { Text(stringResource(R.string.pricing_early_bird_and_normal)) },
                                    modifier = Modifier.weight(1f),
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = uiState.normalPriceInput,
                                onValueChange = onNormalPriceChanged,
                                label = { Text(stringResource(R.string.training_normal_price_label)) },
                                placeholder = { Text("Contoh: 500000") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                isError = uiState.normalPriceError != null,
                                supportingText = uiState.normalPriceError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )

                            if (uiState.pricingScheme == PricingScheme.EARLY_BIRD_AND_NORMAL) {
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = uiState.earlyBirdPriceInput,
                                    onValueChange = onEarlyBirdPriceChanged,
                                    label = { Text(stringResource(R.string.training_early_bird_price_label)) },
                                    placeholder = { Text("Contoh: 400000") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    isError = uiState.earlyBirdPriceError != null,
                                    supportingText = uiState.earlyBirdPriceError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                )
                            }
                        }

                        // 6. Tombol Simpan
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onSaveClick,
                            enabled = !uiState.isSaving,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.saving_training))
                            } else {
                                Text(
                                    text = stringResource(R.string.save_training_button),
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FormSectionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun TimePickerDialog(
    title: String,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(16.dp))
                content()
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(onClick = onConfirm) {
                        Text("Pilih")
                    }
                }
            }
        }
    }
}
