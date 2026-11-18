package com.harunmaul.epelatihan.feature.training.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.harunmaul.epelatihan.R
import com.harunmaul.epelatihan.core.common.toRupiahString
import com.harunmaul.epelatihan.domain.model.PricingScheme
import com.harunmaul.epelatihan.domain.model.PricingType
import com.harunmaul.epelatihan.domain.model.RegistrationWithParticipant
import com.harunmaul.epelatihan.domain.model.Training

@Composable
fun EditRegistrationPriceDialog(
    training: Training,
    registrationWithParticipant: RegistrationWithParticipant,
    isUpdating: Boolean,
    onDismissRequest: () -> Unit,
    onConfirm: (PricingType) -> Unit,
) {
    val currentPricingType = registrationWithParticipant.registration.pricingType
    var selectedPricingType by remember { mutableStateOf(currentPricingType) }

    val participantName = registrationWithParticipant.participant?.name ?: "Peserta"

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = stringResource(R.string.edit_pricing_type_dialog_title),
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.edit_pricing_type_message, participantName),
                    style = MaterialTheme.typography.bodyMedium,
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (training.pricingScheme == PricingScheme.EARLY_BIRD_AND_NORMAL) {
                    val earlyBirdFormatted = (training.earlyBirdPrice ?: 0).toRupiahString()
                    val normalFormatted = training.normalPrice.toRupiahString()

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
                } else {
                    Text(
                        text = stringResource(
                            R.string.pricing_normal_only_notice,
                            training.normalPrice.toRupiahString(),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedPricingType) },
                enabled = !isUpdating && (selectedPricingType != currentPricingType),
            ) {
                if (isUpdating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(R.string.save))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                enabled = !isUpdating,
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
