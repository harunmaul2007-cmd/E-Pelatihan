package com.harunmaul.epelatihan.feature.training.detail

import android.app.Activity
import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.harunmaul.epelatihan.R
import com.harunmaul.epelatihan.core.common.toRupiahString
import com.harunmaul.epelatihan.domain.model.Participant
import com.harunmaul.epelatihan.domain.model.PricingScheme
import com.harunmaul.epelatihan.domain.model.PricingType
import com.harunmaul.epelatihan.domain.model.Training
import com.harunmaul.epelatihan.domain.validation.PhoneNormalizationResult
import com.harunmaul.epelatihan.domain.validation.PhoneNumberNormalizer
import com.harunmaul.epelatihan.feature.participant.common.ContactPickerHelper

@Composable
fun RegisterParticipantDialog(
    training: Training,
    availableParticipants: List<Participant>,
    isRegistering: Boolean,
    onDismissRequest: () -> Unit,
    onRegisterExisting: (participantId: String, pricingType: PricingType?) -> Unit,
    onRegisterNew: (
        name: String,
        phoneRaw: String,
        email: String?,
        notes: String?,
        pricingType: PricingType?,
    ) -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    // Existing participant tab state
    var selectedParticipantId by remember { mutableStateOf<String?>(null) }
    var participantSearchQuery by remember { mutableStateOf("") }

    // New participant tab state
    var newName by remember { mutableStateOf("") }
    var newPhoneRaw by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf("") }
    var newNotes by remember { mutableStateOf("") }

    // Pricing type state
    var selectedPricingType by remember {
        mutableStateOf<PricingType?>(
            if (training.pricingScheme == PricingScheme.NORMAL_ONLY) {
                PricingType.NORMAL
            } else {
                null
            },
        )
    }

    val context = LocalContext.current
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val contactUri = result.data?.data ?: return@rememberLauncherForActivityResult
            val contactInfo = ContactPickerHelper.extractContactInfo(context, contactUri)
            if (contactInfo != null) {
                if (newName.isBlank()) newName = contactInfo.name
                newPhoneRaw = contactInfo.phone
            }
        }
    }

    val filteredParticipants = remember(availableParticipants, participantSearchQuery) {
        if (participantSearchQuery.isBlank()) {
            availableParticipants
        } else {
            val q = participantSearchQuery.trim().lowercase()
            availableParticipants.filter {
                it.name.lowercase().contains(q) ||
                    it.phoneDisplay.contains(q) ||
                    it.phoneNormalized.contains(q)
            }
        }
    }

    val isNewPhoneValid = remember(newPhoneRaw) {
        if (newPhoneRaw.isBlank()) false
        else PhoneNumberNormalizer.normalize(newPhoneRaw) is PhoneNormalizationResult.Success
    }

    val isFormValid = when (selectedTab) {
        0 -> selectedParticipantId != null && (training.pricingScheme == PricingScheme.NORMAL_ONLY || selectedPricingType != null)
        1 -> newName.isNotBlank() && isNewPhoneValid && (training.pricingScheme == PricingScheme.NORMAL_ONLY || selectedPricingType != null)
        else -> false
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = stringResource(R.string.register_participant_dialog_title),
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                // Tabs
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(stringResource(R.string.choose_existing_participant)) },
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(stringResource(R.string.create_new_participant)) },
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab 0: Choose Existing
                if (selectedTab == 0) {
                    OutlinedTextField(
                        value = participantSearchQuery,
                        onValueChange = { participantSearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.participants_search_placeholder)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.select_participant_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp),
                    ) {
                        if (filteredParticipants.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "Tidak ada peserta.",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        } else {
                            LazyColumn {
                                items(filteredParticipants, key = { it.id }) { participant ->
                                    val isSelected = selectedParticipantId == participant.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedParticipantId = participant.id }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = participant.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            )
                                            Text(
                                                text = participant.phoneDisplay,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    }
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }

                // Tab 1: Create New Participant
                if (selectedTab == 1) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_PICK,
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            )
                            contactPickerLauncher.launch(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContactPhone,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.pick_from_contacts_button))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.participant_name_label)) },
                        placeholder = { Text(stringResource(R.string.participant_name_placeholder)) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newPhoneRaw,
                        onValueChange = { newPhoneRaw = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.participant_phone_label)) },
                        placeholder = { Text(stringResource(R.string.participant_phone_placeholder)) },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.participant_email_label)) },
                        placeholder = { Text(stringResource(R.string.participant_email_placeholder)) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                // Price Scheme Selection
                Text(
                    text = stringResource(R.string.pricing_type_label),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (training.pricingScheme == PricingScheme.NORMAL_ONLY) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(
                                R.string.pricing_normal_only_notice,
                                training.normalPrice.toRupiahString(),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                } else {
                    // Early bird & Normal selection
                    val earlyBirdFormatted = (training.earlyBirdPrice ?: 0).toRupiahString()
                    val normalFormatted = training.normalPrice.toRupiahString()

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPricingType = PricingType.EARLY_BIRD }
                                .padding(vertical = 4.dp),
                        ) {
                            RadioButton(
                                selected = selectedPricingType == PricingType.EARLY_BIRD,
                                onClick = { selectedPricingType = PricingType.EARLY_BIRD },
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.pricing_early_bird_option, earlyBirdFormatted),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPricingType = PricingType.NORMAL }
                                .padding(vertical = 4.dp),
                        ) {
                            RadioButton(
                                selected = selectedPricingType == PricingType.NORMAL,
                                onClick = { selectedPricingType = PricingType.NORMAL },
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.pricing_normal_option, normalFormatted),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedTab == 0) {
                        val pId = selectedParticipantId ?: return@Button
                        onRegisterExisting(pId, selectedPricingType)
                    } else {
                        onRegisterNew(
                            newName,
                            newPhoneRaw,
                            newEmail.ifBlank { null },
                            newNotes.ifBlank { null },
                            selectedPricingType,
                        )
                    }
                },
                enabled = isFormValid && !isRegistering,
            ) {
                if (isRegistering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(R.string.register_submit_button))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                enabled = !isRegistering,
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
