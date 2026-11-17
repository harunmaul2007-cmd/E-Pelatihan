package com.harunmaul.epelatihan.feature.usermanagement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
        title = {
            Text(
                text = stringResource(R.string.invite_user_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.invite_user_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(16.dp))

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
                        Icon(imageVector = Icons.Default.Person, contentDescription = null)
                    },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    singleLine = true,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                    ),
                )

                Spacer(modifier = Modifier.height(8.dp))

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
                        Icon(imageVector = Icons.Default.Email, contentDescription = null)
                    },
                    isError = emailError != null,
                    supportingText = emailError?.let { { Text(it) } },
                    singleLine = true,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done,
                    ),
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.role_picker_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .selectable(
                                selected = selectedRole == UserRole.USER,
                                onClick = { if (!isLoading) selectedRole = UserRole.USER },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedRole == UserRole.USER,
                            onClick = null,
                            enabled = !isLoading,
                        )
                        Text(
                            text = stringResource(R.string.role_user),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }

                    Row(
                        modifier = Modifier
                            .selectable(
                                selected = selectedRole == UserRole.ADMIN,
                                onClick = { if (!isLoading) selectedRole = UserRole.ADMIN },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedRole == UserRole.ADMIN,
                            onClick = null,
                            enabled = !isLoading,
                        )
                        Text(
                            text = stringResource(R.string.role_admin),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 6.dp),
                        )
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
                shape = RoundedCornerShape(10.dp),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(R.string.send_invitation))
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismissRequest,
                enabled = !isLoading,
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
