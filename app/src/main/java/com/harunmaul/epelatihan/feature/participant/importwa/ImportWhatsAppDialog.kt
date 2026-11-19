package com.harunmaul.epelatihan.feature.participant.importwa

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.harunmaul.epelatihan.core.designsystem.theme.BrandSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportWhatsAppDialog(
    onDismissRequest: () -> Unit,
    onImportFinished: () -> Unit = {},
    viewModel: ImportWhatsAppViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.parseFromFile(uri, context.contentResolver)
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    // Success Summary Dialog
    uiState.importSummary?.let { summary ->
        AlertDialog(
            onDismissRequest = {
                viewModel.resetSummary()
                onImportFinished()
                onDismissRequest()
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = BrandSuccess,
                    modifier = Modifier.size(48.dp),
                )
            },
            title = {
                Text(
                    text = "Import Kontak Selesai",
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column {
                    Text("• ${summary.totalImported} kontak berhasil diimpor sebagai peserta.")
                    if (summary.totalSkipped > 0) {
                        Text("• ${summary.totalSkipped} kontak dilewati (sudah terdaftar sebelumnya).")
                    }
                    if (summary.totalFailed > 0) {
                        Text("• ${summary.totalFailed} kontak gagal karena data tidak lengkap.")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetSummary()
                        onImportFinished()
                        onDismissRequest()
                    },
                ) {
                    Text("Selesai")
                }
            },
        )
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Import dari WhatsApp",
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismissRequest) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Tutup",
                            )
                        }
                    },
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                if (uiState.candidates.isEmpty()) {
                    // Input step
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF25D366), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Salin (copy) teks chat atau daftar nomor dari grup WhatsApp, lalu tempel di bawah. Sistem akan mengekstrak nama dan nomor otomatis.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Teks Kontak / Pesan WhatsApp:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )

                        TextButton(
                            onClick = {
                                val clip = clipboardManager.getText()?.text
                                if (!clip.isNullOrBlank()) {
                                    viewModel.onRawTextChanged(clip)
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tempel")
                        }
                    }

                    OutlinedTextField(
                        value = uiState.rawText,
                        onValueChange = viewModel::onRawTextChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        placeholder = {
                            Text(
                                "Contoh:\n1. Budi Santoso - 08123456789\n2. Siti Rahma (08567891234)\n+628129876543 Ahmad Fauzi\nDewi Lestari, 087811223344",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                filePickerLauncher.launch(
                                    arrayOf(
                                        "text/x-vcard",
                                        "text/vcard",
                                        "text/plain",
                                        "text/comma-separated-values",
                                        "text/csv",
                                        "*/*",
                                    ),
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileOpen,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pilih File")
                        }

                        Button(
                            onClick = viewModel::parseFromRawText,
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(10.dp),
                            enabled = uiState.rawText.isNotBlank() && !uiState.isAnalyzing,
                        ) {
                            if (uiState.isAnalyzing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ekstrak Kontak")
                            }
                        }
                    }
                } else {
                    // Preview step
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "Hasil Ekstrak Kontak",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "${uiState.selectedCount} dari ${uiState.candidates.size} kontak dipilih",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = {
                                    val allSelected = uiState.candidates.all { it.isSelected || it.isAlreadyRegistered }
                                    viewModel.onToggleSelectAll(!allSelected)
                                },
                            ) {
                                Text("Pilih Semua")
                            }
                            IconButton(onClick = { viewModel.onRawTextChanged("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Ulangi")
                            }
                        }
                    }

                    if (uiState.isImporting && uiState.importProgress != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val (current, total) = uiState.importProgress!!
                        Text(
                            text = "Mengimpor kontak $current dari $total...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { current.toFloat() / total.toFloat() },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(uiState.candidates) { index, candidate ->
                            CandidateCard(
                                candidate = candidate,
                                onToggle = { viewModel.onToggleCandidateSelection(index) },
                                onNameChange = { newName -> viewModel.onUpdateCandidateName(index, newName) },
                                onRemove = { viewModel.onRemoveCandidate(index) },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.onRawTextChanged("") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !uiState.isImporting,
                        ) {
                            Text("Batal")
                        }

                        Button(
                            onClick = viewModel::executeImport,
                            modifier = Modifier.weight(1.6f),
                            shape = RoundedCornerShape(10.dp),
                            enabled = uiState.canImport,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF16A34A),
                            ),
                        ) {
                            if (uiState.isImporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White,
                                )
                            } else {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Import ${uiState.selectedCount} Peserta")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CandidateCard(
    candidate: ParsedContactCandidate,
    onToggle: () -> Unit,
    onNameChange: (String) -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (candidate.isAlreadyRegistered) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = candidate.isSelected,
                onCheckedChange = { onToggle() },
                enabled = candidate.isValidPhone && !candidate.isAlreadyRegistered,
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = candidate.name,
                    onValueChange = onNameChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = candidate.normalizedPhone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    if (candidate.isAlreadyRegistered) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                        ) {
                            Text(
                                text = "Sudah Terdaftar",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    } else if (candidate.isValidPhone) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = BrandSuccess.copy(alpha = 0.15f),
                        ) {
                            Text(
                                text = "Nomor Valid",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandSuccess,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                        ) {
                            Text(
                                text = "Nomor Tidak Valid",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }

            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Hapus",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
