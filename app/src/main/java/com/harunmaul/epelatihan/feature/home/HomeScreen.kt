package com.harunmaul.epelatihan.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.harunmaul.epelatihan.R
import com.harunmaul.epelatihan.core.designsystem.theme.BrandSuccess
import com.harunmaul.epelatihan.domain.model.UserRole

@Composable
fun HomeRoute(
    onNavigateToUserManagement: () -> Unit = {},
    onNavigateToTrainings: () -> Unit = {},
    onNavigateToParticipants: () -> Unit = {},
    onNavigateToReport: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.logout_confirmation_title)) },
            text = { Text(stringResource(R.string.logout_confirmation_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.confirm_logout),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    HomeScreen(
        uiState = uiState,
        onLogoutClick = { showLogoutDialog = true },
        onNavigateToUserManagement = onNavigateToUserManagement,
        onNavigateToTrainings = onNavigateToTrainings,
        onNavigateToParticipants = onNavigateToParticipants,
        onNavigateToReport = onNavigateToReport,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    uiState: HomeUiState,
    onLogoutClick: () -> Unit,
    onNavigateToUserManagement: () -> Unit = {},
    onNavigateToTrainings: () -> Unit = {},
    onNavigateToParticipants: () -> Unit = {},
    onNavigateToReport: () -> Unit = {},
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.communityName.ifBlank { uiState.appName },
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                actions = {
                    if (uiState.currentUser != null) {
                        IconButton(onClick = onLogoutClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = stringResource(R.string.logout_button),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Image(
                painter = painterResource(R.drawable.logo_epelatihan),
                contentDescription = stringResource(R.string.logo_content_description),
                modifier = Modifier.size(160.dp),
                contentScale = ContentScale.Fit,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // User Profile Card if logged in
            uiState.currentUser?.let { user ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (user.role == UserRole.ADMIN) {
                                Icons.Default.AdminPanelSettings
                            } else {
                                Icons.Default.Person
                            },
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = user.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                text = user.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                        ) {
                            Text(
                                text = user.role.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = stringResource(R.string.home_ready_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.home_ready_description),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                ) {
                    Text(
                        text = stringResource(R.string.home_sections_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        FeatureChip(
                            labelRes = R.string.feature_training,
                            onClick = onNavigateToTrainings,
                        )
                        FeatureChip(
                            labelRes = R.string.feature_participant,
                            onClick = onNavigateToParticipants,
                        )
                        FeatureChip(
                            labelRes = R.string.feature_report,
                            onClick = onNavigateToReport,
                        )
                        FeatureChip(R.string.feature_settings)
                        if (uiState.currentUser?.role == UserRole.ADMIN) {
                            AssistChip(
                                onClick = onNavigateToUserManagement,
                                label = { Text(stringResource(R.string.user_management_title)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.AdminPanelSettings,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                },
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = BrandSuccess.copy(alpha = 0.12f),
                contentColor = BrandSuccess,
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    text = stringResource(
                        R.string.home_environment,
                        uiState.environment,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(
                    if (uiState.firebaseConfigured) {
                        R.string.home_firebase_ready
                    } else {
                        R.string.home_firebase_pending
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FeatureChip(
    labelRes: Int,
    onClick: () -> Unit = {},
) {
    AssistChip(
        onClick = onClick,
        label = { Text(stringResource(labelRes)) },
    )
}
