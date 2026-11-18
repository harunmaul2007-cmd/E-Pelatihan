package com.harunmaul.epelatihan.feature.training.detail

import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.harunmaul.epelatihan.R
import com.harunmaul.epelatihan.core.common.toRupiahString
import com.harunmaul.epelatihan.domain.model.PaymentStatus
import com.harunmaul.epelatihan.domain.model.RegistrationWithParticipant

@Composable
fun ConfirmPaymentStatusDialog(
    item: RegistrationWithParticipant,
    isUpdating: Boolean,
    onDismissRequest: () -> Unit,
    onConfirm: (PaymentStatus) -> Unit,
) {
    val currentStatus = item.registration.paymentStatus
    val targetStatus = if (currentStatus == PaymentStatus.LUNAS) {
        PaymentStatus.BELUM_BAYAR
    } else {
        PaymentStatus.LUNAS
    }

    val participantName = item.participant?.name ?: "Peserta"
    val priceFormatted = item.registration.priceAmount.toRupiahString()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = if (targetStatus == PaymentStatus.LUNAS) {
                    stringResource(R.string.confirm_payment_paid_title)
                } else {
                    stringResource(R.string.confirm_payment_unpaid_title)
                },
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Text(
                text = if (targetStatus == PaymentStatus.LUNAS) {
                    stringResource(R.string.confirm_payment_paid_message, participantName, priceFormatted)
                } else {
                    stringResource(R.string.confirm_payment_unpaid_message, participantName)
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(targetStatus) },
                enabled = !isUpdating,
                colors = if (targetStatus == PaymentStatus.BELUM_BAYAR) {
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.buttonColors()
                },
            ) {
                if (isUpdating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = if (targetStatus == PaymentStatus.LUNAS) {
                            stringResource(R.string.btn_mark_paid)
                        } else {
                            stringResource(R.string.btn_mark_unpaid)
                        },
                    )
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
