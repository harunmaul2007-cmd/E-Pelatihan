package com.harunmaul.epelatihan.feature.usermanagement

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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.user_management_title),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        if (uiState.users.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            ) {
                                Text(
                                    text = "${uiState.users.size}",
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
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                ),
            )
        },
        floatingActionButton = {
            if (uiState.isCurrentAdmin) {
                ExtendedFloatingActionButton(
                    onClick = onOpenInviteDialog,
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                    text = {
                        Text(
                            text = stringResource(R.string.invite_user_button),
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                )
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
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(40.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Akses Ditolak",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Hanya Administrator aktif yang memiliki hak untuk mengelola pengguna lembaga.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center,
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
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            // Search Bar & Filter Chips
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
                            text = stringResource(R.string.search_users_placeholder),
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

                Spacer(modifier = Modifier.height(8.dp))

                // Filter Chips
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        FilterChip(
                            selected = uiState.filter == UserFilter.ALL,
                            onClick = { onFilterSelected(UserFilter.ALL) },
                            label = { Text(stringResource(R.string.filter_all)) },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = uiState.filter == UserFilter.ALL,
                                borderColor = Color(0xFFE2E8F0),
                                selectedBorderColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }
                    item {
                        FilterChip(
                            selected = uiState.filter == UserFilter.ADMIN,
                            onClick = { onFilterSelected(UserFilter.ADMIN) },
                            label = { Text(stringResource(R.string.filter_admin)) },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFF3E8FF),
                                selectedLabelColor = Color(0xFF7E22CE),
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = uiState.filter == UserFilter.ADMIN,
                                borderColor = Color(0xFFE2E8F0),
                                selectedBorderColor = Color(0xFF7E22CE),
                            ),
                        )
                    }
                    item {
                        FilterChip(
                            selected = uiState.filter == UserFilter.USER,
                            onClick = { onFilterSelected(UserFilter.USER) },
                            label = { Text(stringResource(R.string.filter_user)) },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFEFF6FF),
                                selectedLabelColor = Color(0xFF1D4ED8),
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = uiState.filter == UserFilter.USER,
                                borderColor = Color(0xFFE2E8F0),
                                selectedBorderColor = Color(0xFF1D4ED8),
                            ),
                        )
                    }
                    item {
                        FilterChip(
                            selected = uiState.filter == UserFilter.INACTIVE,
                            onClick = { onFilterSelected(UserFilter.INACTIVE) },
                            label = { Text(stringResource(R.string.filter_inactive)) },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFEE2E2),
                                selectedLabelColor = Color(0xFFB91C1C),
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = uiState.filter == UserFilter.INACTIVE,
                                borderColor = Color(0xFFE2E8F0),
                                selectedBorderColor = Color(0xFFB91C1C),
                            ),
                        )
                    }
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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(Color(0xFFEFF6FF), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = Color(0xFF2563EB),
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.empty_users_message),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
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
            shape = RoundedCornerShape(20.dp),
            title = { Text(stringResource(R.string.change_role_title), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    stringResource(
                        R.string.change_role_message,
                        targetUser.displayName,
                        newRole.name,
                    ),
                    color = Color(0xFF475569),
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirmRoleChange,
                    shape = RoundedCornerShape(10.dp),
                ) {
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
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    stringResource(
                        if (willDeactivate) R.string.deactivate_user_title else R.string.activate_user_title,
                    ),
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    if (willDeactivate) {
                        stringResource(R.string.deactivate_user_message, targetUser.displayName)
                    } else {
                        stringResource(R.string.activate_user_message, targetUser.displayName)
                    },
                    color = Color(0xFF475569),
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirmStatusChange,
                    shape = RoundedCornerShape(10.dp),
                    colors = if (willDeactivate) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                        )
                    } else {
                        ButtonDefaults.buttonColors()
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
            shape = RoundedCornerShape(20.dp),
            title = { Text(stringResource(R.string.resend_invitation_title), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    stringResource(R.string.resend_invitation_message, targetUser.email),
                    color = Color(0xFF475569),
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirmResend,
                    shape = RoundedCornerShape(10.dp),
                ) {
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
    val isAdmin = user.role == UserRole.ADMIN

    Card(
        modifier = Modifier.fillMaxWidth(),
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
            // Circular Avatar Badge
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        color = if (isAdmin) Color(0xFFF3E8FF) else Color(0xFFEFF6FF),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (isAdmin) Color(0xFF7E22CE) else Color(0xFF2563EB),
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (isSelf) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFEFF6FF),
                        ) {
                            Text(
                                text = "Anda",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF2563EB),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = Color(0xFF94A3B8),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Role Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isAdmin) Color(0xFFF3E8FF) else Color(0xFFEFF6FF),
                    ) {
                        Text(
                            text = user.role.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isAdmin) Color(0xFF7E22CE) else Color(0xFF2563EB),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                    }

                    // Status Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (user.active) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                    ) {
                        Text(
                            text = if (user.active) {
                                stringResource(R.string.status_active)
                            } else {
                                stringResource(R.string.status_inactive)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (user.active) Color(0xFF15803D) else Color(0xFFB91C1C),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                    }

                    // Invitation status badge
                    if (user.invitationStatus == InvitationStatus.INVITED) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFEF3C7),
                        ) {
                            Text(
                                text = stringResource(R.string.status_invited),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
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
                        tint = Color(0xFF64748B),
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
