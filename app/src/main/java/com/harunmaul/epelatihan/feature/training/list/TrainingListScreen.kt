package com.harunmaul.epelatihan.feature.training.list

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.harunmaul.epelatihan.core.common.formatSchedule
import com.harunmaul.epelatihan.core.common.toRupiahString
import com.harunmaul.epelatihan.domain.model.MeetingMethod
import com.harunmaul.epelatihan.domain.model.PricingScheme
import com.harunmaul.epelatihan.domain.model.Training
import com.harunmaul.epelatihan.domain.model.UserRole
import com.harunmaul.epelatihan.domain.validation.TrainingDisplayStatus
import com.harunmaul.epelatihan.domain.validation.TrainingStatusResolver

@Composable
fun TrainingListRoute(
    onNavigateBack: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {},
    onNavigateToCreate: () -> Unit = {},
    viewModel: TrainingListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TrainingListScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onTrainingClick = onNavigateToDetail,
        onAddTrainingClick = onNavigateToCreate,
        onTabSelected = viewModel::selectTab,
        onFilterSelected = viewModel::selectFilter,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onRetryClick = viewModel::retry,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingListScreen(
    uiState: TrainingListUiState,
    onNavigateBack: () -> Unit = {},
    onTrainingClick: (String) -> Unit = {},
    onAddTrainingClick: () -> Unit = {},
    onTabSelected: (TrainingTab) -> Unit = {},
    onFilterSelected: (TrainingFilterOption) -> Unit = {},
    onSearchQueryChanged: (String) -> Unit = {},
    onRetryClick: () -> Unit = {},
) {
    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White),
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.training_title),
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

                // Segmented Pill Tab (Aktif vs Arsip) untuk Admin
                if (uiState.currentUserRole == UserRole.ADMIN) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                            .padding(4.dp),
                    ) {
                        val activeSelected = uiState.selectedTab == TrainingTab.ACTIVE
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (activeSelected) Color.White else Color.Transparent)
                                .clickable { onTabSelected(TrainingTab.ACTIVE) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.filter_active),
                                fontWeight = if (activeSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (activeSelected) MaterialTheme.colorScheme.primary else Color(0xFF64748B),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }

                        val archivedSelected = uiState.selectedTab == TrainingTab.ARCHIVED
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (archivedSelected) Color.White else Color.Transparent)
                                .clickable { onTabSelected(TrainingTab.ARCHIVED) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.filter_archived),
                                fontWeight = if (archivedSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (archivedSelected) MaterialTheme.colorScheme.primary else Color(0xFF64748B),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTrainingClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = {
                    Text(
                        text = stringResource(R.string.add_training_button),
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            // Offline banner
            AnimatedVisibility(visible = uiState.isFromCache) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
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

            // Search Bar & Filter Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChanged,
                    placeholder = {
                        Text(
                            text = "Cari nama pelatihan atau pemateri...",
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
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC),
                    ),
                    singleLine = true,
                )

                if (uiState.selectedTab == TrainingTab.ACTIVE) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp),
                    ) {
                        item {
                            FilterChip(
                                selected = uiState.selectedFilter == TrainingFilterOption.ALL,
                                onClick = { onFilterSelected(TrainingFilterOption.ALL) },
                                label = { Text(stringResource(R.string.filter_all)) },
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = uiState.selectedFilter == TrainingFilterOption.ALL,
                                    borderColor = Color(0xFFE2E8F0),
                                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                                ),
                            )
                        }
                        item {
                            FilterChip(
                                selected = uiState.selectedFilter == TrainingFilterOption.UPCOMING,
                                onClick = { onFilterSelected(TrainingFilterOption.UPCOMING) },
                                label = { Text(stringResource(R.string.filter_upcoming)) },
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFEFF6FF),
                                    selectedLabelColor = Color(0xFF1D4ED8),
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = uiState.selectedFilter == TrainingFilterOption.UPCOMING,
                                    borderColor = Color(0xFFE2E8F0),
                                    selectedBorderColor = Color(0xFF1D4ED8),
                                ),
                            )
                        }
                        item {
                            FilterChip(
                                selected = uiState.selectedFilter == TrainingFilterOption.ONGOING,
                                onClick = { onFilterSelected(TrainingFilterOption.ONGOING) },
                                label = { Text(stringResource(R.string.filter_ongoing)) },
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFDCFCE7),
                                    selectedLabelColor = Color(0xFF15803D),
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = uiState.selectedFilter == TrainingFilterOption.ONGOING,
                                    borderColor = Color(0xFFE2E8F0),
                                    selectedBorderColor = Color(0xFF15803D),
                                ),
                            )
                        }
                        item {
                            FilterChip(
                                selected = uiState.selectedFilter == TrainingFilterOption.COMPLETED,
                                onClick = { onFilterSelected(TrainingFilterOption.COMPLETED) },
                                label = { Text(stringResource(R.string.filter_completed)) },
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFF1F5F9),
                                    selectedLabelColor = Color(0xFF475569),
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = uiState.selectedFilter == TrainingFilterOption.COMPLETED,
                                    borderColor = Color(0xFFE2E8F0),
                                    selectedBorderColor = Color(0xFF475569),
                                ),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Content: Loading, Error, Empty, or List
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
            ) {
                when {
                    uiState.isLoading && uiState.trainings.isEmpty() -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.loading_trainings),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF64748B),
                            )
                        }
                    }

                    uiState.errorMessage != null && uiState.trainings.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(Color(0xFFFEE2E2), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = Color(0xFFDC2626),
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = uiState.errorMessage,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFF991B1B),
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onRetryClick,
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text(stringResource(R.string.retry))
                            }
                        }
                    }

                    uiState.filteredTrainings.isEmpty() -> {
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
                                    .background(Color(0xFFE0F2FE), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = Color(0xFF0284C7),
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = if (uiState.selectedTab == TrainingTab.ARCHIVED) {
                                    stringResource(R.string.empty_archived_trainings)
                                } else {
                                    stringResource(R.string.empty_trainings_title)
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (uiState.selectedTab == TrainingTab.ACTIVE) {
                                Text(
                                    text = stringResource(R.string.empty_trainings_description),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF64748B),
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = onAddTrainingClick,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                    ),
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.add_training_button),
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(
                                items = uiState.filteredTrainings,
                                key = { it.id },
                            ) { training ->
                                TrainingCard(
                                    training = training,
                                    onClick = { onTrainingClick(training.id) },
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
fun TrainingCard(
    training: Training,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayStatus = TrainingStatusResolver.resolve(training)
    val isOnline = training.meetingMethod == MeetingMethod.ONLINE

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Icon badge + Title + Status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                // Circular icon badge
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = if (isOnline) Color(0xFFE0F2FE) else Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(12.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isOnline) Icons.Default.Videocam else Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = if (isOnline) Color(0xFF0284C7) else Color(0xFFD97706),
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = training.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TrainingStatusBadge(status = displayStatus)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Pemateri
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = Color(0xFF64748B),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = training.speaker,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF475569),
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Jadwal
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF8FAFC),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formatSchedule(training.startAt, training.endAt, training.timeZoneId),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF334155),
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer: Metode Pelaksanaan & Skema Harga
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Metode Tag
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isOnline) Color(0xFFF0FDF4) else Color(0xFFFFFBEB),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (isOnline) {
                                stringResource(R.string.method_online)
                            } else {
                                training.location?.ifBlank { stringResource(R.string.method_offline) }
                                    ?: stringResource(R.string.method_offline)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isOnline) Color(0xFF16A34A) else Color(0xFFD97706),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // Harga Tag
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (training.pricingScheme == PricingScheme.EARLY_BIRD_AND_NORMAL && training.earlyBirdPrice != null) {
                            "${training.earlyBirdPrice.toRupiahString()} / ${training.normalPrice.toRupiahString()}"
                        } else {
                            training.normalPrice.toRupiahString()
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
fun TrainingStatusBadge(status: TrainingDisplayStatus) {
    val (backgroundColor, textColor, labelRes) = when (status) {
        TrainingDisplayStatus.AKAN_DATANG -> Triple(
            Color(0xFFEFF6FF),
            Color(0xFF1D4ED8),
            R.string.status_upcoming,
        )
        TrainingDisplayStatus.BERLANGSUNG -> Triple(
            Color(0xFFDCFCE7),
            Color(0xFF15803D),
            R.string.status_ongoing,
        )
        TrainingDisplayStatus.SELESAI -> Triple(
            Color(0xFFF1F5F9),
            Color(0xFF475569),
            R.string.status_completed,
        )
        TrainingDisplayStatus.DIARSIPKAN -> Triple(
            Color(0xFFFEE2E2),
            Color(0xFFB91C1C),
            R.string.status_archived,
        )
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
    ) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}
