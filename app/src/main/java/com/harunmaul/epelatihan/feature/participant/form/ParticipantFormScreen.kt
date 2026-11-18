package com.harunmaul.epelatihan.feature.participant.form

import android.app.Activity
import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.harunmaul.epelatihan.R
import com.harunmaul.epelatihan.feature.participant.common.ContactPickerHelper

@Composable
fun ParticipantFormRoute(
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: ParticipantFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val contactUri = result.data?.data ?: return@rememberLauncherForActivityResult
            val contactInfo = ContactPickerHelper.extractContactInfo(context, contactUri)
            if (contactInfo != null) {
                viewModel.onContactSelected(name = contactInfo.name, phone = contactInfo.phone)
            }
        }
    }

    LaunchedEffect(uiState.isSaveSuccess) {
        if (uiState.isSaveSuccess) {
            onSaveSuccess()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    ParticipantFormScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onNameChanged = viewModel::onNameChanged,
        onPhoneChanged = viewModel::onPhoneChanged,
        onEmailChanged = viewModel::onEmailChanged,
        onNotesChanged = viewModel::onNotesChanged,
        onPickContactClick = {
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            contactPickerLauncher.launch(intent)
        },
        onUseExistingParticipant = viewModel::onUseExistingParticipant,
        onDismissDuplicateDialog = viewModel::onDismissDuplicateDialog,
        onSaveClick = viewModel::saveParticipant,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParticipantFormScreen(
    uiState: ParticipantFormUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onNameChanged: (String) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onPickContactClick: () -> Unit,
    onUseExistingParticipant: () -> Unit,
    onDismissDuplicateDialog: () -> Unit,
    onSaveClick: () -> Unit,
) {
    if (uiState.showDuplicateDialog && uiState.duplicateParticipant != null) {
        AlertDialog(
            onDismissRequest = onDismissDuplicateDialog,
            title = { Text(stringResource(R.string.duplicate_participant_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.duplicate_participant_message,
                        uiState.duplicateParticipant.name,
                    ),
                )
            },
            confirmButton = {
                Button(onClick = onUseExistingParticipant) {
                    Text(stringResource(R.string.use_existing_participant))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDuplicateDialog) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (uiState.isEditMode) {
                                R.string.edit_participant_title
                            } else {
                                R.string.create_participant_title
                            },
                        ),
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cancel),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Non-Admin Read Only Notice
                if (uiState.isReadOnlyNonAdmin) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Hanya Admin yang dapat mengubah data peserta yang tersimpan.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }

                // Pick from Contacts Button
                if (!uiState.isReadOnlyNonAdmin) {
                    OutlinedButton(
                        onClick = onPickContactClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContactPhone,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.pick_from_contacts_button))
                    }
                }

                // Name Field
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = onNameChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.participant_name_label)) },
                    placeholder = { Text(stringResource(R.string.participant_name_placeholder)) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null)
                    },
                    isError = uiState.nameError != null,
                    supportingText = uiState.nameError?.let { { Text(it) } },
                    singleLine = true,
                    enabled = !uiState.isReadOnlyNonAdmin && !uiState.isSaving,
                    shape = RoundedCornerShape(12.dp),
                )

                // WhatsApp Phone Field
                OutlinedTextField(
                    value = uiState.phoneRaw,
                    onValueChange = onPhoneChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.participant_phone_label)) },
                    placeholder = { Text(stringResource(R.string.participant_phone_placeholder)) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Phone, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = uiState.phoneError != null,
                    supportingText = {
                        when {
                            uiState.phoneError != null -> Text(uiState.phoneError)
                            uiState.phoneNormalized.isNotBlank() -> Text("Tersimpan sebagai: +${uiState.phoneNormalized}")
                        }
                    },
                    singleLine = true,
                    enabled = !uiState.isReadOnlyNonAdmin && !uiState.isSaving,
                    shape = RoundedCornerShape(12.dp),
                )

                // Email Field
                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = onEmailChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.participant_email_label)) },
                    placeholder = { Text(stringResource(R.string.participant_email_placeholder)) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Email, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    isError = uiState.emailError != null,
                    supportingText = uiState.emailError?.let { { Text(it) } },
                    singleLine = true,
                    enabled = !uiState.isReadOnlyNonAdmin && !uiState.isSaving,
                    shape = RoundedCornerShape(12.dp),
                )

                // Notes Field
                OutlinedTextField(
                    value = uiState.notes,
                    onValueChange = onNotesChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.participant_notes_label)) },
                    placeholder = { Text(stringResource(R.string.participant_notes_placeholder)) },
                    leadingIcon = {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Note, contentDescription = null)
                    },
                    minLines = 3,
                    maxLines = 5,
                    enabled = !uiState.isReadOnlyNonAdmin && !uiState.isSaving,
                    shape = RoundedCornerShape(12.dp),
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Save Button
                Button(
                    onClick = onSaveClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = uiState.isFormValid && !uiState.isSaving,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.save_participant_button),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}
