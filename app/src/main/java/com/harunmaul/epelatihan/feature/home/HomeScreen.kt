package com.harunmaul.epelatihan.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.harunmaul.epelatihan.R
import com.harunmaul.epelatihan.domain.model.UserRole
import com.harunmaul.epelatihan.feature.participant.importwa.ImportWhatsAppDialog

data class HomeMenuItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val backgroundColor: Color,
    val iconTint: Color,
    val onClick: () -> Unit,
)

@Composable
fun HomeRoute(
    onNavigateToUserManagement: () -> Unit = {},
    onNavigateToTrainings: () -> Unit = {},
    onNavigateToParticipants: () -> Unit = {},
    onNavigateToReport: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showImportWaDialog by remember { mutableStateOf(false) }

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

    if (showImportWaDialog) {
        ImportWhatsAppDialog(
            onDismissRequest = { showImportWaDialog = false },
            onImportFinished = {
                // Refresh or handle completion
            },
        )
    }

    HomeScreen(
        uiState = uiState,
        onLogoutClick = { showLogoutDialog = true },
        onOpenImportWa = { showImportWaDialog = true },
        onNavigateToUserManagement = onNavigateToUserManagement,
        onNavigateToTrainings = onNavigateToTrainings,
        onNavigateToParticipants = onNavigateToParticipants,
        onNavigateToReport = onNavigateToReport,
        onNavigateToSettings = onNavigateToSettings,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    uiState: HomeUiState,
    onLogoutClick: () -> Unit,
    onOpenImportWa: () -> Unit,
    onNavigateToUserManagement: () -> Unit = {},
    onNavigateToTrainings: () -> Unit = {},
    onNavigateToParticipants: () -> Unit = {},
    onNavigateToReport: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
) {
    val scrollState = rememberScrollState()

    val menuItems = buildList {
        add(
            HomeMenuItem(
                id = "trainings",
                title = "Pelatihan",
                icon = Icons.Default.School,
                backgroundColor = Color(0xFFE0F2FE),
                iconTint = Color(0xFF0284C7),
                onClick = onNavigateToTrainings,
            ),
        )
        add(
            HomeMenuItem(
                id = "participants",
                title = "Peserta",
                icon = Icons.Default.People,
                backgroundColor = Color(0xFFE0F7FA),
                iconTint = Color(0xFF00838F),
                onClick = onNavigateToParticipants,
            ),
        )
        add(
            HomeMenuItem(
                id = "registrations",
                title = "Pendaftaran",
                icon = Icons.Default.EditNote,
                backgroundColor = Color(0xFFE8F5E9),
                iconTint = Color(0xFF2E7D32),
                onClick = onNavigateToTrainings,
            ),
        )
        add(
            HomeMenuItem(
                id = "report",
                title = "Lap. Bulanan",
                icon = Icons.Default.CalendarMonth,
                backgroundColor = Color(0xFFF3E8FF),
                iconTint = Color(0xFF7E22CE),
                onClick = onNavigateToReport,
            ),
        )
        add(
            HomeMenuItem(
                id = "import_wa",
                title = "Import WA",
                icon = Icons.Default.ContactPhone,
                backgroundColor = Color(0xFFDCFCE7),
                iconTint = Color(0xFF16A34A),
                onClick = onOpenImportWa,
            ),
        )
        add(
            HomeMenuItem(
                id = "export_data",
                title = "Ekspor Data",
                icon = Icons.Default.FileDownload,
                backgroundColor = Color(0xFFEFF6FF),
                iconTint = Color(0xFF2563EB),
                onClick = onNavigateToReport,
            ),
        )
        if (uiState.currentUser?.role == UserRole.ADMIN) {
            add(
                HomeMenuItem(
                    id = "settings",
                    title = "Pengaturan",
                    icon = Icons.Default.Settings,
                    backgroundColor = Color(0xFFFEF3C7),
                    iconTint = Color(0xFFD97706),
                    onClick = onNavigateToSettings,
                ),
            )
            add(
                HomeMenuItem(
                    id = "users",
                    title = "Kelola User",
                    icon = Icons.Default.AdminPanelSettings,
                    backgroundColor = Color(0xFFFFE4E6),
                    iconTint = Color(0xFFE11D48),
                    onClick = onNavigateToUserManagement,
                ),
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.communityName.ifBlank { uiState.appName },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 1. Top Banner Card (Style Gambar 2)
            val user = uiState.currentUser
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent,
                ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF0284C7),
                                    Color(0xFF0EA5E9),
                                ),
                            ),
                            shape = RoundedCornerShape(16.dp),
                        )
                        .padding(20.dp),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = uiState.communityName.ifBlank { "E-Pelatihan" },
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Medium,
                            )

                            if (user != null) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color.White.copy(alpha = 0.25f),
                                ) {
                                    Text(
                                        text = user.role.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Hai, ${user?.displayName ?: "Pengguna"}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )

                        if (!user?.email.isNullOrBlank()) {
                            Text(
                                text = user?.email.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Menu Grid (Circular Icons in Card - Gambar 2 Style)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 8.dp),
                ) {
                    // Split menu items into rows of 4 items each
                    val rows = menuItems.chunked(4)
                    rows.forEachIndexed { rowIndex, rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            rowItems.forEach { item ->
                                HomeMenuButton(
                                    item = item,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            // Fill empty slots if last row has less than 4 items
                            val emptySlots = 4 - rowItems.size
                            repeat(emptySlots) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        if (rowIndex < rows.size - 1) {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Statistik Pelatihan Card (Gambar 2 Style)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Ringkasan Pelatihan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )

                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Stat 1: Pelatihan Aktif
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onNavigateToTrainings() },
                            color = Color(0xFFE0F2FE),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = "${uiState.activeTrainingsCount}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0369A1),
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Pelatihan Aktif",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF0284C7),
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }

                        // Stat 2: Total Peserta
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onNavigateToParticipants() },
                            color = Color(0xFFE0F7FA),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = "${uiState.totalParticipantsCount}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF00695C),
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Total Peserta",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF00897B),
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HomeMenuButton(
    item: HomeMenuItem,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { item.onClick() }
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(item.backgroundColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = item.iconTint,
                modifier = Modifier.size(28.dp),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = item.title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
