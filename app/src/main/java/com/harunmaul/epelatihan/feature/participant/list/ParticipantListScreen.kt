package com.harunmaul.epelatihan.feature.participant.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.harunmaul.epelatihan.R
import com.harunmaul.epelatihan.domain.model.Participant
import com.harunmaul.epelatihan.feature.participant.importwa.ImportWhatsAppDialog

@Composable
fun ParticipantListRoute(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToCreate: () -> Unit,
    viewModel: ParticipantListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ParticipantListScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onNavigateToDetail = onNavigateToDetail,
        onNavigateToCreate = onNavigateToCreate,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onIncludeInactiveToggled = viewModel::onIncludeInactiveToggled,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParticipantListScreen(
    uiState: ParticipantListUiState,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToCreate: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onIncludeInactiveToggled: (Boolean) -> Unit,
) {
    var showImportWaDialog by remember { mutableStateOf(false) }

    if (showImportWaDialog) {
        ImportWhatsAppDialog(
            onDismissRequest = { showImportWaDialog = false },
        )
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.participants_title),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        if (!uiState.isLoading && uiState.participants.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            ) {
                                Text(
                                    text = "${uiState.participants.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cancel),
                        )
                    }
                },
                actions = {
                    // Import WA Action Button
                    Surface(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { showImportWaDialog = true },
                        color = Color(0xFFDCFCE7),
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContactPhone,
                                contentDescription = "Import Kontak WhatsApp",
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Import WA",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToCreate,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = {
                    Text(
                        text = stringResource(R.string.add_participant_button),
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // Offline banner
            AnimatedVisibility(visible = uiState.isFromCache) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.offline_banner_text),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
            }

            // Search Bar & Filter
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChanged,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.participants_search_placeholder),
                            color = Color(0xFF94A3B8),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChanged("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Hapus pencarian",
                                    tint = Color(0xFF94A3B8),
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC),
                    ),
                )

                // Admin toggle for inactive participants
                if (uiState.isAdmin) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        FilterChip(
                            selected = uiState.includeInactive,
                            onClick = { onIncludeInactiveToggled(!uiState.includeInactive) },
                            label = {
                                Text(
                                    text = stringResource(R.string.show_inactive_participants),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFEE2E2),
                                selectedLabelColor = Color(0xFFB91C1C),
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = uiState.includeInactive,
                                borderColor = Color(0xFFE2E8F0),
                                selectedBorderColor = Color(0xFFB91C1C),
                            ),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Content Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
            ) {
                when {
                    uiState.isLoading -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Memuat data peserta...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF64748B),
                            )
                        }
                    }

                    uiState.errorMessage != null -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = uiState.errorMessage,
                                color = Color(0xFFDC2626),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    uiState.participants.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(Color(0xFFE0F7FA), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = Color(0xFF00838F),
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = if (uiState.searchQuery.isNotBlank()) {
                                    stringResource(R.string.empty_search_participants)
                                } else {
                                    stringResource(R.string.empty_participants_title)
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                textAlign = TextAlign.Center,
                            )
                            if (uiState.searchQuery.isBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.empty_participants_description),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF64748B),
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = onNavigateToCreate,
                                        shape = RoundedCornerShape(12.dp),
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(stringResource(R.string.add_participant_button))
                                    }
                                    OutlinedButton(
                                        onClick = { showImportWaDialog = true },
                                        shape = RoundedCornerShape(12.dp),
                                    ) {
                                        Icon(Icons.Default.ContactPhone, contentDescription = null, tint = Color(0xFF16A34A))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Import WA", color = Color(0xFF16A34A))
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(
                                items = uiState.participants,
                                key = { it.id },
                            ) { participant ->
                                ParticipantItemCard(
                                    participant = participant,
                                    onClick = { onNavigateToDetail(participant.id) },
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(88.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParticipantItemCard(
    participant: Participant,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Circular Avatar with initial
            val initial = participant.name.trim().take(1).uppercase()
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        color = if (participant.active) Color(0xFFE0F2FE) else Color(0xFFF1F5F9),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initial.ifBlank { "P" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (participant.active) Color(0xFF0284C7) else Color(0xFF64748B),
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = participant.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!participant.active) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFEE2E2),
                        ) {
                            Text(
                                text = stringResource(R.string.participant_inactive_label),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB91C1C),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Telepon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = Color(0xFF16A34A),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = participant.phoneDisplay,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF334155),
                        fontWeight = FontWeight.Medium,
                    )
                }

                // Email
                if (!participant.email.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFF94A3B8),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = participant.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // Catatan
                if (!participant.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Note,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFF94A3B8),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = participant.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
