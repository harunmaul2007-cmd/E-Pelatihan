package com.harunmaul.epelatihan.feature.report

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
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
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = if (isPdf) Color(0xFFFEE2E2) else Color(0xFFDCFCE7),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isPdf) Icons.Default.PictureAsPdf else Icons.Default.TableChart,
                        contentDescription = null,
                        tint = if (isPdf) Color(0xFFDC2626) else Color(0xFF16A34A),
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = stringResource(R.string.export_options_dialog_title, formatName),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                    )
                    if (periodText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Periode: $periodText",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // 1. WhatsApp
                ExportOptionCard(
                    icon = Icons.AutoMirrored.Filled.Send,
                    iconTint = Color(0xFF16A34A),
                    iconBg = Color(0xFFDCFCE7),
                    title = stringResource(R.string.export_options_send_wa_title),
                    description = stringResource(R.string.export_options_send_wa_desc),
                    onClick = {
                        onDismissRequest()
                        onSendViaWhatsApp()
                    },
                )

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

                // 4. Save to device
                ExportOptionCard(
                    icon = Icons.Default.Download,
                    iconTint = Color(0xFF475569),
                    iconBg = Color(0xFFF1F5F9),
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
                Text(
                    text = stringResource(R.string.cancel),
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B),
                )
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
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
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

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B),
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = Color(0xFF94A3B8),
            )
        }
    }
}
