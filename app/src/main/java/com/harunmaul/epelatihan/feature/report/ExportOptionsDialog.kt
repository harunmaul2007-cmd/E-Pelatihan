package com.harunmaul.epelatihan.feature.report

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.harunmaul.epelatihan.R
import com.harunmaul.epelatihan.core.designsystem.theme.BrandWhatsApp

@Composable
fun ExportOptionsDialog(
    isPdf: Boolean,
    periodText: String,
    onDismissRequest: () -> Unit,
    onSendViaWhatsApp: () -> Unit,
    onSendViaEmail: () -> Unit,
    onShareGeneral: () -> Unit,
    onSaveToDevice: () -> Unit,
) {
    val formatName = if (isPdf) "PDF" else "Excel"

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Column {
                Text(
                    text = stringResource(R.string.export_options_dialog_title, formatName),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                if (periodText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Periode: $periodText",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                // 1. WhatsApp
                ExportOptionCard(
                    icon = Icons.AutoMirrored.Filled.Send,
                    iconTint = BrandWhatsApp,
                    iconBg = Color(0xFFDCFCE7),
                    title = stringResource(R.string.export_options_send_wa_title),
                    description = stringResource(R.string.export_options_send_wa_desc),
                    onClick = {
                        onDismissRequest()
                        onSendViaWhatsApp()
                    },
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 2. Email
                ExportOptionCard(
                    icon = Icons.Default.Email,
                    iconTint = Color(0xFF2563EB),
                    iconBg = Color(0xFFEFF6FF),
                    title = stringResource(R.string.export_options_send_email_title),
                    description = stringResource(R.string.export_options_send_email_desc),
                    onClick = {
                        onDismissRequest()
                        onSendViaEmail()
                    },
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 3. Share Chooser
                ExportOptionCard(
                    icon = Icons.Default.Share,
                    iconTint = Color(0xFF7C3AED),
                    iconBg = Color(0xFFF3E8FF),
                    title = stringResource(R.string.export_options_share_title),
                    description = stringResource(R.string.export_options_share_desc),
                    onClick = {
                        onDismissRequest()
                        onShareGeneral()
                    },
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 4. Save to device
                ExportOptionCard(
                    icon = Icons.Default.Download,
                    iconTint = Color(0xFF475467),
                    iconBg = Color(0xFFF2F4F7),
                    title = stringResource(R.string.export_options_save_title),
                    description = stringResource(R.string.export_options_save_desc),
                    onClick = {
                        onDismissRequest()
                        onSaveToDevice()
                    },
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun ExportOptionCard(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconBg, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        }
    }
}
