package com.harunmaul.epelatihan.feature.training.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.harunmaul.epelatihan.R
import com.harunmaul.epelatihan.core.common.formatSchedule
import com.harunmaul.epelatihan.core.common.toRupiahString
import com.harunmaul.epelatihan.core.designsystem.theme.BrandSuccess
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.training_title),
                            fontWeight = FontWeight.SemiBold,
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
                )

                // Tab aktif vs arsip hanya ditampilkan jika role adalah Admin
                if (uiState.currentUserRole == UserRole.ADMIN) {
                    TabRow(selectedTabIndex = uiState.selectedTab.ordinal) {
                        Tab(
                            selected = uiState.selectedTab == TrainingTab.ACTIVE,
                            onClick = { onTabSelected(TrainingTab.ACTIVE) },
                            text = { Text(stringResource(R.string.filter_active)) },
                        )
                        Tab(
                            selected = uiState.selectedTab == TrainingTab.ARCHIVED,
                            onClick = { onTabSelected(TrainingTab.ARCHIVED) },
                            text = { Text(stringResource(R.string.filter_archived)) },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            // Admin dan User keduanya dapat menambahkan pelatihan
            ExtendedFloatingActionButton(
                onClick = onAddTrainingClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_training_button)) },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            // Banner status offline
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

            // Search & Filter section
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChanged,
                    placeholder = { Text(stringResource(R.string.search_users_placeholder)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                )

                if (uiState.selectedTab == TrainingTab.ACTIVE) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        FilterChip(
                            selected = uiState.selectedFilter == TrainingFilterOption.ALL,
                            onClick = { onFilterSelected(TrainingFilterOption.ALL) },
                            label = { Text(stringResource(R.string.filter_all)) },
                        )
                        FilterChip(
                            selected = uiState.selectedFilter == TrainingFilterOption.UPCOMING,
                            onClick = { onFilterSelected(TrainingFilterOption.UPCOMING) },
                            label = { Text(stringResource(R.string.filter_upcoming)) },
                        )
                        FilterChip(
                            selected = uiState.selectedFilter == TrainingFilterOption.ONGOING,
                            onClick = { onFilterSelected(TrainingFilterOption.ONGOING) },
                            label = { Text(stringResource(R.string.filter_ongoing)) },
                        )
                        FilterChip(
                            selected = uiState.selectedFilter == TrainingFilterOption.COMPLETED,
                            onClick = { onFilterSelected(TrainingFilterOption.COMPLETED) },
                            label = { Text(stringResource(R.string.filter_completed)) },
                        )
                    }
                }
            }

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
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.loading_trainings),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.error,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = uiState.errorMessage,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onRetryClick) {
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
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.outline,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (uiState.selectedTab == TrainingTab.ARCHIVED) {
                                    stringResource(R.string.empty_archived_trainings)
                                } else {
                                    stringResource(R.string.empty_trainings_title)
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (uiState.selectedTab == TrainingTab.ACTIVE) {
                                Text(
                                    text = stringResource(R.string.empty_trainings_description),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(onClick = onAddTrainingClick) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.add_training_button))
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
                            // Spacer agar item terakhir tidak tertutup FAB
                            item {
                                Spacer(modifier = Modifier.height(80.dp))
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

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Judul & Badge Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = training.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.width(8.dp))
                TrainingStatusBadge(status = displayStatus)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Jadwal
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = formatSchedule(training.startAt, training.endAt, training.timeZoneId),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Pemateri
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.secondary,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = training.speaker,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer: Metode Pelaksanaan & Skema Harga
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Metode
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (training.meetingMethod == MeetingMethod.ONLINE) {
                            Icons.Default.Videocam
                        } else {
                            Icons.Default.LocationOn
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (training.meetingMethod == MeetingMethod.ONLINE) {
                            stringResource(R.string.method_online)
                        } else {
                            training.location?.ifBlank { stringResource(R.string.method_offline) }
                                ?: stringResource(R.string.method_offline)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Harga
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
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            R.string.status_upcoming,
        )
        TrainingDisplayStatus.BERLANGSUNG -> Triple(
            BrandSuccess.copy(alpha = 0.15f),
            BrandSuccess,
            R.string.status_ongoing,
        )
        TrainingDisplayStatus.SELESAI -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            R.string.status_completed,
        )
        TrainingDisplayStatus.DIARSIPKAN -> Triple(
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.onErrorContainer,
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
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}
