package com.harunmaul.epelatihan.feature.training.detail

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PriceChange
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.harunmaul.epelatihan.R
import com.harunmaul.epelatihan.core.common.formatDateTime
import com.harunmaul.epelatihan.core.common.formatSchedule
import com.harunmaul.epelatihan.core.common.toRupiahString
import com.harunmaul.epelatihan.core.designsystem.theme.BrandSuccess
import com.harunmaul.epelatihan.domain.model.MeetingMethod
import com.harunmaul.epelatihan.domain.model.PaymentStatus
import com.harunmaul.epelatihan.domain.model.PricingScheme
import com.harunmaul.epelatihan.domain.model.PricingType
import com.harunmaul.epelatihan.domain.model.RegistrationWithParticipant
import com.harunmaul.epelatihan.domain.model.Training
import com.harunmaul.epelatihan.domain.model.TrainingLifecycle
import com.harunmaul.epelatihan.domain.model.UserRole
import com.harunmaul.epelatihan.domain.validation.TrainingStatusResolver
import com.harunmaul.epelatihan.feature.training.list.TrainingStatusBadge

@Composable
fun TrainingDetailRoute(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit = {},
    viewModel: TrainingDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.archiveSuccess) {
        if (uiState.archiveSuccess) {
            Toast.makeText(context, context.getString(R.string.training_archived_success), Toast.LENGTH_SHORT).show()
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearErrorMessage()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSuccessMessage()
        }
    }

    TrainingDetailScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onEditClick = { uiState.training?.id?.let(onNavigateToEdit) },
        onArchiveClick = viewModel::archiveTraining,
        onOpenRegisterDialog = viewModel::openRegisterDialog,
        onDismissRegisterDialog = viewModel::dismissRegisterDialog,
        onRegisterExisting = viewModel::registerExistingParticipant,
        onRegisterNew = viewModel::registerNewParticipant,
        onOpenEditPriceDialog = viewModel::openEditPriceDialog,
        onDismissEditPriceDialog = viewModel::dismissEditPriceDialog,
        onUpdateRegistrationPrice = viewModel::updateRegistrationPrice,
        onOpenPaymentStatusDialog = viewModel::openPaymentStatusDialog,
        onDismissPaymentStatusDialog = viewModel::dismissPaymentStatusDialog,
        onUpdatePaymentStatus = viewModel::updatePaymentStatus,
        onOpenRegistrationDetailDialog = viewModel::openRegistrationDetailDialog,
        onDismissRegistrationDetailDialog = viewModel::dismissRegistrationDetailDialog,
        onOpenWhatsApp = viewModel::openWhatsApp,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingDetailScreen(
    uiState: TrainingDetailUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onNavigateBack: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onArchiveClick: () -> Unit = {},
    onOpenRegisterDialog: () -> Unit = {},
    onDismissRegisterDialog: () -> Unit = {},
    onRegisterExisting: (participantId: String, pricingType: PricingType?) -> Unit = { _, _ -> },
    onRegisterNew: (
        name: String,
        phoneRaw: String,
        email: String?,
        notes: String?,
        pricingType: PricingType?,
    ) -> Unit = { _, _, _, _, _ -> },
    onOpenEditPriceDialog: (RegistrationWithParticipant) -> Unit = {},
    onDismissEditPriceDialog: () -> Unit = {},
    onUpdateRegistrationPrice: (PricingType) -> Unit = {},
    onOpenPaymentStatusDialog: (RegistrationWithParticipant) -> Unit = {},
    onDismissPaymentStatusDialog: () -> Unit = {},
    onUpdatePaymentStatus: (PaymentStatus) -> Unit = {},
    onOpenRegistrationDetailDialog: (RegistrationWithParticipant) -> Unit = {},
    onDismissRegistrationDetailDialog: () -> Unit = {},
    onOpenWhatsApp: (String) -> Unit = {},
) {
    val context = LocalContext.current
    var showArchiveDialog by remember { mutableStateOf(false) }

    if (showArchiveDialog && uiState.training != null) {
        AlertDialog(
            onDismissRequest = { showArchiveDialog = false },
            icon = { Icon(Icons.Default.Archive, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.archive_training)) },
            text = { Text(stringResource(R.string.archive_training_confirmation, uiState.training.name)) },
            confirmButton = {
                Button(
                    onClick = {
                        showArchiveDialog = false
                        onArchiveClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.confirm_archive))
                }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (uiState.showRegisterDialog && uiState.training != null) {
        RegisterParticipantDialog(
            training = uiState.training,
            availableParticipants = uiState.availableParticipants,
            isRegistering = uiState.isRegistering,
            onDismissRequest = onDismissRegisterDialog,
            onRegisterExisting = onRegisterExisting,
            onRegisterNew = onRegisterNew,
        )
    }

    if (uiState.showEditPriceDialog && uiState.training != null && uiState.selectedRegistrationForEdit != null) {
        EditRegistrationPriceDialog(
            training = uiState.training,
            registrationWithParticipant = uiState.selectedRegistrationForEdit,
            isUpdating = uiState.isUpdatingPrice,
            onDismissRequest = onDismissEditPriceDialog,
            onConfirm = onUpdateRegistrationPrice,
        )
    }

    if (uiState.showPaymentStatusDialog && uiState.selectedRegistrationForPayment != null) {
        ConfirmPaymentStatusDialog(
            item = uiState.selectedRegistrationForPayment,
            isUpdating = uiState.isUpdatingPaymentStatus,
            onDismissRequest = onDismissPaymentStatusDialog,
            onConfirm = onUpdatePaymentStatus,
        )
    }

    if (uiState.showRegistrationDetailDialog && uiState.training != null && uiState.selectedRegistrationForDetail != null) {
        RegistrationDetailDialog(
            item = uiState.selectedRegistrationForDetail,
            training = uiState.training,
            isAdmin = uiState.currentUserRole == UserRole.ADMIN,
            onDismissRequest = onDismissRegistrationDetailDialog,
            onOpenWhatsAppClick = {
                onOpenWhatsApp(uiState.selectedRegistrationForDetail.registration.id)
            },
            onChangePaymentStatusClick = {
                onOpenPaymentStatusDialog(uiState.selectedRegistrationForDetail)
            },
            onEditPriceClick = {
                onOpenEditPriceDialog(uiState.selectedRegistrationForDetail)
            },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.training_detail_title),
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
                actions = {
                    if (uiState.currentUserRole == UserRole.ADMIN && uiState.training?.lifecycle == TrainingLifecycle.ACTIVE) {
                        IconButton(onClick = onEditClick) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.action_edit),
                            )
                        }
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
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                uiState.training == null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(R.string.training_not_found),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onNavigateBack) {
                            Text(stringResource(R.string.back))
                        }
                    }
                }

                else -> {
                    val training = uiState.training
                    val status = TrainingStatusResolver.resolve(training)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        // Offline notice
                        AnimatedVisibility(visible = uiState.isFromCache) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.offline_banner_text),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            // Header Nama & Status
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top,
                            ) {
                                Text(
                                    text = training.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                TrainingStatusBadge(status = status)
                            }

                            // Deskripsi
                            val description = training.description
                            if (!description.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Kartu Jadwal
                            DetailSectionCard(
                                icon = Icons.Default.Event,
                                title = stringResource(R.string.schedule),
                            ) {
                                Text(
                                    text = formatSchedule(training.startAt, training.endAt, training.timeZoneId),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.timezone_label, training.timeZoneId),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Kartu Pemateri
                            DetailSectionCard(
                                icon = Icons.Default.Person,
                                title = stringResource(R.string.speaker),
                            ) {
                                Text(
                                    text = training.speaker,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Kartu Metode & Lokasi/Link
                            DetailSectionCard(
                                icon = if (training.meetingMethod == MeetingMethod.ONLINE) {
                                    Icons.Default.Videocam
                                } else {
                                    Icons.Default.LocationOn
                                },
                                title = if (training.meetingMethod == MeetingMethod.ONLINE) {
                                    stringResource(R.string.method_online)
                                } else {
                                    stringResource(R.string.method_offline)
                                },
                            ) {
                                val zoomUrl = training.zoomUrl
                                if (training.meetingMethod == MeetingMethod.ONLINE && !zoomUrl.isNullOrBlank()) {
                                    Text(
                                        text = zoomUrl,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    FilledTonalButton(
                                        onClick = { openUrl(context, zoomUrl) },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.open_zoom))
                                    }
                                }

                                val location = training.location
                                if (training.meetingMethod == MeetingMethod.OFFLINE && !location.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = location,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }

                                // Link Materi / PDF
                                val materialUrl = training.materialUrl
                                if (!materialUrl.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedButton(
                                        onClick = { openUrl(context, materialUrl) },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Icon(Icons.Default.Description, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.open_material))
                                    }
                                }

                                // Link Rekaman
                                val recordingUrl = training.recordingUrl
                                if (!recordingUrl.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = { openUrl(context, recordingUrl) },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Icon(Icons.Default.VideoLibrary, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.open_recording))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Kartu Skema & Harga
                            DetailSectionCard(
                                icon = Icons.Default.Payments,
                                title = stringResource(R.string.pricing_scheme),
                            ) {
                                Text(
                                    text = if (training.pricingScheme == PricingScheme.EARLY_BIRD_AND_NORMAL) {
                                        stringResource(R.string.pricing_early_bird_and_normal)
                                    } else {
                                        stringResource(R.string.pricing_normal_only)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                if (training.pricingScheme == PricingScheme.EARLY_BIRD_AND_NORMAL && training.earlyBirdPrice != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(stringResource(R.string.price_early_bird), style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            text = training.earlyBirdPrice.toRupiahString(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(stringResource(R.string.price_normal), style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = training.normalPrice.toRupiahString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // SECTION PENDAFTARAN PESERTA
                            DetailSectionCard(
                                icon = Icons.Default.Group,
                                title = stringResource(R.string.registrations_section_title, uiState.registrations.size),
                            ) {
                                Button(
                                    onClick = onOpenRegisterDialog,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.register_participant_button))
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                if (uiState.registrations.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = stringResource(R.string.empty_registrations_title),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Medium,
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = stringResource(R.string.empty_registrations_description),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center,
                                            )
                                        }
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        uiState.registrations.forEach { item ->
                                            RegistrationItemCard(
                                                item = item,
                                                isAdmin = uiState.currentUserRole == UserRole.ADMIN,
                                                onClick = { onOpenRegistrationDetailDialog(item) },
                                                onOpenWhatsAppClick = { onOpenWhatsApp(item.registration.id) },
                                                onEditPriceClick = { onOpenEditPriceDialog(item) },
                                                onChangePaymentStatusClick = { onOpenPaymentStatusDialog(item) },
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Metadata Card
                            DetailSectionCard(
                                icon = Icons.Default.Info,
                                title = "Informasi Tambahan",
                            ) {
                                training.createdAt?.let {
                                    Text(
                                        text = stringResource(R.string.created_at_label, it.formatDateTime(training.timeZoneId)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                training.updatedAt?.let {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(R.string.updated_at_label, it.formatDateTime(training.timeZoneId)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            // Aksi Admin di bagian bawah jika belum diarsipkan
                            if (uiState.currentUserRole == UserRole.ADMIN) {
                                Spacer(modifier = Modifier.height(24.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Button(
                                        onClick = onEditClick,
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.action_edit))
                                    }

                                    if (training.lifecycle == TrainingLifecycle.ACTIVE) {
                                        OutlinedButton(
                                            onClick = { showArchiveDialog = true },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = MaterialTheme.colorScheme.error,
                                            ),
                                        ) {
                                            Icon(Icons.Default.Archive, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(stringResource(R.string.action_archive))
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RegistrationItemCard(
    item: RegistrationWithParticipant,
    isAdmin: Boolean,
    onClick: () -> Unit,
    onOpenWhatsAppClick: () -> Unit,
    onEditPriceClick: () -> Unit,
    onChangePaymentStatusClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.participant?.name ?: "Peserta",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.registration.paymentStatus == PaymentStatus.LUNAS) {
                        IconButton(
                            onClick = onOpenWhatsAppClick,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = stringResource(R.string.open_whatsapp_button),
                                tint = BrandSuccess,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    if (isAdmin) {
                        IconButton(
                            onClick = onChangePaymentStatusClick,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = stringResource(R.string.change_payment_status),
                                tint = if (item.registration.paymentStatus == PaymentStatus.LUNAS) {
                                    BrandSuccess
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        IconButton(
                            onClick = onEditPriceClick,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.PriceChange,
                                contentDescription = stringResource(R.string.edit_pricing_type_dialog_title),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }

            if (item.participant != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.participant.phoneDisplay,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Pricing badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (item.registration.pricingType == PricingType.EARLY_BIRD) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                ) {
                    Text(
                        text = "${item.registration.pricingType.name}: ${item.registration.priceAmount.toRupiahString()}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                // Payment Status badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (item.registration.paymentStatus == PaymentStatus.LUNAS) {
                        BrandSuccess.copy(alpha = 0.15f)
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    },
                    modifier = if (isAdmin) {
                        Modifier.clickable(onClick = onChangePaymentStatusClick)
                    } else {
                        Modifier
                    },
                ) {
                    Text(
                        text = if (item.registration.paymentStatus == PaymentStatus.LUNAS) {
                            stringResource(R.string.status_paid)
                        } else {
                            stringResource(R.string.status_unpaid)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (item.registration.paymentStatus == PaymentStatus.LUNAS) {
                            BrandSuccess
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailSectionCard(
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
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        val cleanUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else {
            url
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl))
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Gagal membuka tautan.", Toast.LENGTH_SHORT).show()
    }
}
