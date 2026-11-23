package com.harunmaul.epelatihan.feature.usermanagement

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.harunmaul.epelatihan.R
import com.harunmaul.epelatihan.domain.model.UserRole
import com.harunmaul.epelatihan.domain.validation.AuthValidator
import com.harunmaul.epelatihan.domain.validation.ValidationResult

@Composable
fun InviteUserDialog(
    isLoading: Boolean,
    onDismissRequest: () -> Unit,
    onInviteConfirm: (name: String, email: String, role: UserRole) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var selectedRole by remember { mutableStateOf(UserRole.USER) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismissRequest() },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color(0xFFEFF6FF), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.invite_user_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Kirim tautan aktivasi akun ke email pengguna",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B),
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.display_name_label)) },
                    placeholder = { Text(stringResource(R.string.display_name_placeholder)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                        )
                    },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    singleLine = true,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC),
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                    ),
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        emailError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.email_label)) },
                    placeholder = { Text(stringResource(R.string.email_placeholder)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                        )
                    },
                    isError = emailError != null,
                    supportingText = emailError?.let { { Text(it) } },
                    singleLine = true,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC),
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done,
                    ),
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.role_picker_label),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A),
                    )

                    // Modern Segmented Pill for Role
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        val isUserSelected = selectedRole == UserRole.USER
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isUserSelected) Color.White else Color.Transparent)
                                .clickable(enabled = !isLoading) { selectedRole = UserRole.USER }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isUserSelected) Color(0xFF2563EB) else Color(0xFF64748B),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.role_user),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isUserSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isUserSelected) Color(0xFF2563EB) else Color(0xFF64748B),
                                )
                            }
                        }

                        val isAdminSelected = selectedRole == UserRole.ADMIN
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isAdminSelected) Color.White else Color.Transparent)
                                .clickable(enabled = !isLoading) { selectedRole = UserRole.ADMIN }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isAdminSelected) Color(0xFF7E22CE) else Color(0xFF64748B),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.role_admin),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isAdminSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isAdminSelected) Color(0xFF7E22CE) else Color(0xFF64748B),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val nameValidation = AuthValidator.validateDisplayName(name)
                    val emailValidation = AuthValidator.validateEmail(email)

                    val nError = (nameValidation as? ValidationResult.Invalid)?.issues?.firstOrNull()?.message
                    val eError = (emailValidation as? ValidationResult.Invalid)?.issues?.firstOrNull()?.message

                    if (nError != null || eError != null) {
                        nameError = nError
                        emailError = eError
                        return@Button
                    }

                    onInviteConfirm(name.trim(), email.trim(), selectedRole)
                },
                enabled = name.isNotBlank() && email.isNotBlank() && !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(R.string.send_invitation), fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismissRequest,
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
