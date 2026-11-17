package com.harunmaul.epelatihan.feature.usermanagement

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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.harunmaul.epelatihan.R
import com.harunmaul.epelatihan.core.designsystem.theme.BrandSuccess
import com.harunmaul.epelatihan.domain.model.InvitationStatus
import com.harunmaul.epelatihan.domain.model.UserProfile
import com.harunmaul.epelatihan.domain.model.UserRole

@Composable
fun UserListRoute(
    onNavigateBack: () -> Unit,
    viewModel: UserListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        val error = uiState.errorMessage
        val success = uiState.successMessage
        if (error != null) {
            snackbarHostState.showSnackbar(message = error)
            viewModel.clearMessages()
        } else if (success != null) {
            snackbarHostState.showSnackbar(message = success)
            viewModel.clearMessages()
        }
    }

    UserListScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onFilterSelected = viewModel::onFilterSelected,
        onOpenInviteDialog = viewModel::openInviteDialog,
        onCloseInviteDialog = viewModel::closeInviteDialog,
        onInviteUser = viewModel::inviteUser,
        onRequestRoleChange = viewModel::requestRoleChange,
        onConfirmRoleChange = viewModel::confirmRoleChange,
        onCancelRoleChange = viewModel::cancelRoleChange,
        onRequestStatusChange = viewModel::requestStatusChange,
        onConfirmStatusChange = viewModel::confirmStatusChange,
        onCancelStatusChange = viewModel::cancelStatusChange,
        onRequestResend = viewModel::requestResend,
        onConfirmResend = viewModel::confirmResend,
        onCancelResend = viewModel::cancelResend,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListScreen(
    uiState: UserListUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onFilterSelected: (UserFilter) -> Unit,
    onOpenInviteDialog: () -> Unit,
    onCloseInviteDialog: () -> Unit,
    onInviteUser: (name: String, email: String, role: UserRole) -> Unit,
    onRequestRoleChange: (UserProfile) -> Unit,
    onConfirmRoleChange: () -> Unit,
    onCancelRoleChange: () -> Unit,
    onRequestStatusChange: (UserProfile) -> Unit,
    onConfirmStatusChange: () -> Unit,
    onCancelStatusChange: () -> Unit,
    onRequestResend: (UserProfile) -> Unit,
    onConfirmResend: () -> Unit,
    onCancelResend: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.user_management_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (uiState.isCurrentAdmin) {
                FloatingActionButton(
                    onClick = onOpenInviteDialog,
                    containerColor = MaterialTheme.colorScheme.primary,
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = stringResource(R.string.invite_user_button),
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { contentPadding ->
        // Cek hak akses Admin
        if (!uiState.isCurrentAdmin && uiState.currentUser != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Akses Ditolak",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Hanya Administrator aktif yang memiliki hak untuk mengelola pengguna lembaga.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onNavigateBack) {
                            Text(stringResource(R.string.back))
                        }
                    }
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            // Search field
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.search_users_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )

            // Filter Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = uiState.filter == UserFilter.ALL,
                        onClick = { onFilterSelected(UserFilter.ALL) },
                        label = { Text(stringResource(R.string.filter_all)) },
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.filter == UserFilter.ADMIN,
                        onClick = { onFilterSelected(UserFilter.ADMIN) },
                        label = { Text(stringResource(R.string.filter_admin)) },
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.filter == UserFilter.USER,
                        onClick = { onFilterSelected(UserFilter.USER) },
                        label = { Text(stringResource(R.string.filter_user)) },
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.filter == UserFilter.INACTIVE,
                        onClick = { onFilterSelected(UserFilter.INACTIVE) },
                        label = { Text(stringResource(R.string.filter_inactive)) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Users List
            if (uiState.filteredUsers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.empty_users_message),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(
                        items = uiState.filteredUsers,
                        key = { it.id },
                    ) { user ->
                        UserCard(
                            user = user,
                            isSelf = user.id == uiState.currentUser?.id,
                            onRequestRoleChange = { onRequestRoleChange(user) },
                            onRequestStatusChange = { onRequestStatusChange(user) },
                            onRequestResend = { onRequestResend(user) },
                        )
                    }
                }
            }
        }
    }

    // Invite User Dialog
    if (uiState.isInviteDialogOpen) {
        InviteUserDialog(
            isLoading = uiState.isInviting,
            onDismissRequest = onCloseInviteDialog,
            onInviteConfirm = onInviteUser,
        )
    }

    // Role Change Dialog
    uiState.selectedUserForRoleChange?.let { targetUser ->
        val newRole = if (targetUser.role == UserRole.ADMIN) UserRole.USER else UserRole.ADMIN
        AlertDialog(
            onDismissRequest = onCancelRoleChange,
            title = { Text(stringResource(R.string.change_role_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.change_role_message,
                        targetUser.displayName,
                        newRole.name,
                    ),
                )
            },
            confirmButton = {
                Button(onClick = onConfirmRoleChange) {
                    Text(stringResource(R.string.confirm_change_role))
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelRoleChange) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    // Status Change Dialog
    uiState.selectedUserForStatusChange?.let { targetUser ->
        val willDeactivate = targetUser.active
        AlertDialog(
            onDismissRequest = onCancelStatusChange,
            title = {
                Text(
                    stringResource(
                        if (willDeactivate) R.string.deactivate_user_title else R.string.activate_user_title,
                    ),
                )
            },
            text = {
                Text(
                    if (willDeactivate) {
                        stringResource(R.string.deactivate_user_message, targetUser.displayName)
                    } else {
                        stringResource(R.string.activate_user_message, targetUser.displayName)
                    },
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirmStatusChange,
                    colors = if (willDeactivate) {
                        androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                        )
                    } else {
                        androidx.compose.material3.ButtonDefaults.buttonColors()
                    },
                ) {
                    Text(
                        stringResource(
                            if (willDeactivate) R.string.confirm_deactivate else R.string.confirm_activate,
                        ),
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelStatusChange) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    // Resend Invitation Dialog
    uiState.selectedUserForResend?.let { targetUser ->
        AlertDialog(
            onDismissRequest = onCancelResend,
            title = { Text(stringResource(R.string.resend_invitation_title)) },
            text = {
                Text(
                    stringResource(R.string.resend_invitation_message, targetUser.email),
                )
            },
            confirmButton = {
                Button(onClick = onConfirmResend) {
                    Text(stringResource(R.string.confirm_resend))
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelResend) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun UserCard(
    user: UserProfile,
    isSelf: Boolean,
    onRequestRoleChange: () -> Unit,
    onRequestStatusChange: () -> Unit,
    onRequestResend: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (user.active) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = if (user.role == UserRole.ADMIN) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (user.role == UserRole.ADMIN) {
                            Icons.Default.AdminPanelSettings
                        } else {
                            Icons.Default.Person
                        },
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (user.role == UserRole.ADMIN) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondary
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (isSelf) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(Anda)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Role Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    ) {
                        Text(
                            text = user.role.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }

                    // Status Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (user.active) {
                            BrandSuccess.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                        },
                    ) {
                        Text(
                            text = if (user.active) {
                                stringResource(R.string.status_active)
                            } else {
                                stringResource(R.string.status_inactive)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (user.active) BrandSuccess else MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }

                    // Invitation status badge
                    if (user.invitationStatus == InvitationStatus.INVITED) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                        ) {
                            Text(
                                text = stringResource(R.string.status_invited),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }

            // Actions dropdown menu
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu Aksi",
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    // Role toggle (cannot remove admin from self)
                    if (!isSelf) {
                        val nextRoleText = if (user.role == UserRole.ADMIN) "Jadikan User" else "Jadikan Admin"
                        DropdownMenuItem(
                            text = { Text(nextRoleText) },
                            onClick = {
                                menuExpanded = false
                                onRequestRoleChange()
                            },
                        )
                    }

                    // Active toggle (cannot deactivate self)
                    if (!isSelf) {
                        val nextStatusText = if (user.active) "Nonaktifkan Akun" else "Aktifkan Akun"
                        DropdownMenuItem(
                            text = { Text(nextStatusText) },
                            onClick = {
                                menuExpanded = false
                                onRequestStatusChange()
                            },
                        )
                    }

                    // Resend invitation if invited
                    if (user.invitationStatus == InvitationStatus.INVITED) {
                        DropdownMenuItem(
                            text = { Text("Kirim Ulang Undangan") },
                            onClick = {
                                menuExpanded = false
                                onRequestResend()
                            },
                        )
                    }
                }
            }
        }
    }
}
