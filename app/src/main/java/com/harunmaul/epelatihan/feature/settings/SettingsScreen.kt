package com.harunmaul.epelatihan.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.harunmaul.epelatihan.R

@Composable
fun SettingsRoute(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearErrorMessage()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { success ->
            snackbarHostState.showSnackbar(success)
            viewModel.clearSuccessMessage()
        }
    }

    SettingsScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onCommunityNameChanged = viewModel::onCommunityNameChanged,
        onSaveClick = viewModel::saveSettings,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onNavigateBack: () -> Unit = {},
    onCommunityNameChanged: (String) -> Unit = {},
    onSaveClick: () -> Unit = {},
) {
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                ),
            )
        },
    ) { contentPadding ->
        if (!uiState.isLoading && !uiState.isAdmin) {
            // Akses ditolak jika bukan Admin
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(Color(0xFFFEE2E2), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = Color(0xFFDC2626),
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = stringResource(R.string.settings_access_denied),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF0F172A),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Hanya Administrator yang memiliki hak akses untuk mengubah pengaturan lembaga.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF64748B),
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onNavigateBack,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(stringResource(R.string.back))
                    }
                }
            }
        } else if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .imePadding()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Hero Organization / Community Profile Banner Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(2.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.size(90.dp),
                            shadowElevation = 2.dp,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Image(
                                    painter = painterResource(R.drawable.logo_epelatihan),
                                    contentDescription = stringResource(R.string.logo_content_description),
                                    modifier = Modifier.size(72.dp),
                                    contentScale = ContentScale.Fit,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = uiState.communityName.ifBlank { "Lembaga E-Pelatihan" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            textAlign = TextAlign.Center,
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFDCFCE7),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Administrator Terverifikasi",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D),
                                )
                            }
                        }
                    }
                }

                // Community Name Form Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFFEF3C7), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.settings_community_name_label),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                )
                                Text(
                                    text = "Nama ini akan ditampilkan di header dan dokumen laporan",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF64748B),
                                )
                            }
                        }

                        OutlinedTextField(
                            value = uiState.communityName,
                            onValueChange = onCommunityNameChanged,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(stringResource(R.string.settings_community_name_placeholder)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                )
                            },
                            isError = uiState.nameError != null,
                            supportingText = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = uiState.nameError ?: stringResource(R.string.settings_community_name_helper),
                                        modifier = Modifier.weight(1f),
                                        color = if (uiState.nameError != null) Color(0xFFDC2626) else Color(0xFF64748B),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    Text(
                                        text = "${uiState.communityName.length}/100",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (uiState.communityName.length > 100) Color(0xFFDC2626) else Color(0xFF64748B),
                                    )
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                            ),
                            singleLine = true,
                        )

                        Button(
                            onClick = onSaveClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            enabled = uiState.canSave,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.settings_saving_progress))
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.settings_save_button),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                )
                            }
                        }
                    }
                }

                // System & App Information Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFEFF6FF), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Informasi Sistem",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        InfoRowItem(
                            icon = Icons.Default.Info,
                            title = "Versi Aplikasi",
                            value = "v1.0.0 (Build Resmi)",
                        )

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        InfoRowItem(
                            icon = Icons.Default.CloudDone,
                            title = "Koneksi Database",
                            value = "Cloud Firestore (Online)",
                            valueColor = Color(0xFF16A34A),
                        )

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        InfoRowItem(
                            icon = Icons.Default.Security,
                            title = "Sistem Keamanan",
                            value = "RBAC (Admin / User)",
                            valueColor = Color(0xFF2563EB),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRowItem(
    icon: ImageVector,
    title: String,
    value: String,
    valueColor: Color = Color(0xFF0F172A),
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color(0xFF94A3B8),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF64748B),
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
        )
    }
}
