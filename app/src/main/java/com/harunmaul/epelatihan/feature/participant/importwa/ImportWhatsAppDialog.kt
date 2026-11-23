package com.harunmaul.epelatihan.feature.participant.importwa

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Paste Text, 1: File Upload

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
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFFDCFCE7), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(36.dp),
                    )
                }
            },
            title = {
                Text(
                    text = "Import Kontak Selesai",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "• ${summary.totalImported} kontak berhasil diimpor sebagai peserta.",
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF15803D),
                    )
                    if (summary.totalSkipped > 0) {
                        Text(
                            text = "• ${summary.totalSkipped} kontak dilewati (sudah terdaftar sebelumnya).",
                            color = Color(0xFF64748B),
                        )
                    }
                    if (summary.totalFailed > 0) {
                        Text(
                            text = "• ${summary.totalFailed} kontak gagal karena data tidak valid.",
                            color = Color(0xFFDC2626),
                        )
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
                    shape = RoundedCornerShape(12.dp),
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
            containerColor = Color(0xFFF8FAFC),
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFF25D366), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Import dari WhatsApp",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismissRequest) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Tutup",
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                    ),
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                if (uiState.candidates.isEmpty()) {
                    // Input step banner card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                        border = BorderStroke(1.dp, Color(0xFFDCFCE7)),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top,
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
                            Column {
                                Text(
                                    text = "Ekstrak Kontak Instan",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF14532D),
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Salin teks chat grup atau daftar nomor WhatsApp, lalu tempel di sini. Nama dan nomor telepon akan terdeteksi dan dinormalisasi otomatis.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF166534),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Segmented Selector (Tempel Teks vs Unggah File)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                            .padding(4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedTab == 0) Color.White else Color.Transparent)
                                .clickable { selectedTab = 0 }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selectedTab == 0) Color(0xFF16A34A) else Color(0xFF64748B),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Tempel Teks",
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == 0) Color(0xFF16A34A) else Color(0xFF64748B),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedTab == 1) Color.White else Color.Transparent)
                                .clickable { selectedTab = 1 }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FileOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selectedTab == 1) Color(0xFF16A34A) else Color(0xFF64748B),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Unggah File",
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == 1) Color(0xFF16A34A) else Color(0xFF64748B),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (selectedTab == 0) {
                        // TAB 0: Tempel Teks
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Teks Kontak / Pesan WhatsApp:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F172A),
                            )

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFEFF6FF),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        val clip = clipboardManager.getText()?.text
                                        if (!clip.isNullOrBlank()) {
                                            viewModel.onRawTextChanged(clip)
                                        }
                                    },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = Color(0xFF2563EB),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Tempel dari Clipboard",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2563EB),
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = uiState.rawText,
                            onValueChange = viewModel::onRawTextChanged,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            placeholder = {
                                Text(
                                    "Contoh format yang didukung:\n\n1. Budi Santoso - 08123456789\n2. Siti Rahma (08567891234)\n+628129876543 Ahmad Fauzi\nDewi Lestari, 087811223344",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF94A3B8),
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF16A34A),
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                            ),
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = viewModel::parseFromRawText,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            enabled = uiState.rawText.isNotBlank() && !uiState.isAnalyzing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF16A34A),
                            ),
                        ) {
                            if (uiState.isAnalyzing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Menganalisis kontak...")
                            } else {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Ekstrak Kontak",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                )
                            }
                        }
                    } else {
                        // TAB 1: Unggah Berkas
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
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
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.5.dp, Color(0xFFCBD5E1)),
                        ) {
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
                                        .background(Color(0xFFDCFCE7), CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FileOpen,
                                        contentDescription = null,
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(32.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Pilih Berkas Kontak dari Perangkat",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Mendukung file ekspor kontak WhatsApp (.vcf), file teks (.txt), atau daftar spreadsheet (.csv)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF64748B),
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
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
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF16A34A),
                                    ),
                                ) {
                                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Buka File Explorer")
                                }
                            }
                        }
                    }
                } else {
                    // Preview step
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = "Hasil Ekstrak Kontak",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                )
                                Text(
                                    text = "${uiState.selectedCount} dari ${uiState.candidates.size} kontak dipilih untuk diimpor",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF64748B),
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFEFF6FF),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            val allSelected = uiState.candidates.all { it.isSelected || it.isAlreadyRegistered }
                                            viewModel.onToggleSelectAll(!allSelected)
                                        },
                                ) {
                                    Text(
                                        text = "Pilih Semua",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2563EB),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(onClick = { viewModel.onRawTextChanged("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Ulangi", tint = Color(0xFF94A3B8))
                                }
                            }
                        }
                    }

                    if (uiState.isImporting && uiState.importProgress != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val (current, total) = uiState.importProgress!!
                        Text(
                            text = "Mengimpor kontak $current dari $total...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF16A34A),
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { current.toFloat() / total.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF16A34A),
                            trackColor = Color(0xFFDCFCE7),
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

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
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.onRawTextChanged("") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !uiState.isImporting,
                        ) {
                            Text("Batal")
                        }

                        Button(
                            onClick = viewModel::executeImport,
                            modifier = Modifier.weight(1.8f),
                            shape = RoundedCornerShape(12.dp),
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
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Import ${uiState.selectedCount} Peserta",
                                    fontWeight = FontWeight.Bold,
                                )
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (candidate.isAlreadyRegistered) {
                Color(0xFFF8FAFC)
            } else {
                Color.White
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
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
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF16A34A),
                ),
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Avatar initial
            val initial = candidate.name.trim().take(1).uppercase()
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        color = if (candidate.isAlreadyRegistered) Color(0xFFE2E8F0) else Color(0xFFDCFCE7),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initial.ifBlank { "P" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (candidate.isAlreadyRegistered) Color(0xFF64748B) else Color(0xFF15803D),
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = candidate.name,
                    onValueChange = onNameChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF16A34A),
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                    ),
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = Color(0xFF16A34A),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = candidate.normalizedPhone,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569),
                        fontWeight = FontWeight.Medium,
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    if (candidate.isAlreadyRegistered) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFF1F5F9),
                        ) {
                            Text(
                                text = "Sudah Terdaftar",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    } else if (candidate.isValidPhone) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFDCFCE7),
                        ) {
                            Text(
                                text = "Valid",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFEE2E2),
                        ) {
                            Text(
                                text = "Tidak Valid",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626),
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
                    tint = Color(0xFF94A3B8),
                )
            }
        }
    }
}
