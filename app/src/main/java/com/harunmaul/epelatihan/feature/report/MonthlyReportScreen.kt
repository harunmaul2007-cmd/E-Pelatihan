package com.harunmaul.epelatihan.feature.report

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.harunmaul.epelatihan.R
import com.harunmaul.epelatihan.core.common.formatSchedule
import com.harunmaul.epelatihan.core.common.toRupiahString
import com.harunmaul.epelatihan.core.designsystem.theme.BrandSuccess
import com.harunmaul.epelatihan.domain.model.MeetingMethod
import com.harunmaul.epelatihan.domain.model.MonthlyReport
import com.harunmaul.epelatihan.domain.model.TrainingReportRow
import com.harunmaul.epelatihan.feature.report.component.TrainingParticipantBarChart
import java.io.OutputStream
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun MonthlyReportRoute(
    onNavigateBack: () -> Unit,
    onNavigateToTrainingDetail: (String) -> Unit = {},
    viewModel: MonthlyReportViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri: Uri? ->
        if (uri != null) {
            val outputStream: OutputStream? = try {
                context.contentResolver.openOutputStream(uri)
            } catch (e: Exception) {
                null
            }
            if (outputStream != null) {
                viewModel.exportPdf(outputStream)
            } else {
                // Lokasi file tidak dapat dibuka
            }
        }
    }

    val xlsxLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    ) { uri: Uri? ->
        if (uri != null) {
            val outputStream: OutputStream? = try {
                context.contentResolver.openOutputStream(uri)
            } catch (e: Exception) {
                null
            }
            if (outputStream != null) {
                viewModel.exportXlsx(outputStream)
            } else {
                // Lokasi file tidak dapat dibuka
            }
        }
    }

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

    var exportFormatDialog by remember { mutableStateOf<Boolean?>(null) } // true: PDF, false: Excel, null: closed

    if (exportFormatDialog != null) {
        val isPdf = exportFormatDialog == true
        val idLocale = Locale.forLanguageTag("id-ID")
        val periodStr = uiState.report?.let {
            "${it.month.month.getDisplayName(TextStyle.FULL, idLocale)} ${it.month.year}"
        } ?: uiState.selectedMonth.toString()

        ExportOptionsDialog(
            isPdf = isPdf,
            periodText = periodStr,
            onDismissRequest = { exportFormatDialog = null },
            onSendViaWhatsApp = {
                exportAndShare(
                    context = context,
                    uiState = uiState,
                    viewModel = viewModel,
                    isPdf = isPdf,
                    destination = ExportShareDestination.WHATSAPP,
                )
            },
            onSendViaEmail = {
                exportAndShare(
                    context = context,
                    uiState = uiState,
                    viewModel = viewModel,
                    isPdf = isPdf,
                    destination = ExportShareDestination.EMAIL,
                )
            },
            onShareGeneral = {
                exportAndShare(
                    context = context,
                    uiState = uiState,
                    viewModel = viewModel,
                    isPdf = isPdf,
                    destination = ExportShareDestination.GENERAL,
                )
            },
            onSaveToDevice = {
                val defaultName = if (isPdf) {
                    "Laporan-Pelatihan-${uiState.selectedMonth}.pdf"
                } else {
                    "Laporan-Pelatihan-${uiState.selectedMonth}.xlsx"
                }
                if (isPdf) {
                    pdfLauncher.launch(defaultName)
                } else {
                    xlsxLauncher.launch(defaultName)
                }
            },
        )
    }

    MonthlyReportScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onPreviousMonth = viewModel::previousMonth,
        onNextMonth = viewModel::nextMonth,
        onSelectMonth = viewModel::selectMonth,
        onRefresh = viewModel::refresh,
        onExportPdf = {
            exportFormatDialog = true
        },
        onExportXlsx = {
            exportFormatDialog = false
        },
        onNavigateToTrainingDetail = onNavigateToTrainingDetail,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyReportScreen(
    uiState: MonthlyReportUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onNavigateBack: () -> Unit = {},
    onPreviousMonth: () -> Unit = {},
    onNextMonth: () -> Unit = {},
    onSelectMonth: (YearMonth) -> Unit = {},
    onRefresh: () -> Unit = {},
    onExportPdf: () -> Unit = {},
    onExportXlsx: () -> Unit = {},
    onNavigateToTrainingDetail: (String) -> Unit = {},
) {
    var showMonthPicker by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }

    if (showMonthPicker) {
        MonthYearPickerDialog(
            currentMonth = uiState.selectedMonth,
            onDismissRequest = { showMonthPicker = false },
            onConfirm = { selected ->
                showMonthPicker = false
                onSelectMonth(selected)
            },
        )
    }

    if (uiState.isExporting) {
        AlertDialog(
            onDismissRequest = { /* Modal non-dismissible during active export */ },
            confirmButton = {},
            title = { Text(stringResource(R.string.export_report_button)) },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text(stringResource(R.string.exporting_progress))
                }
            },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.report_title),
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
                actions = {
                    Box {
                        IconButton(
                            onClick = { showExportMenu = true },
                            enabled = uiState.report != null && !uiState.isExporting,
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = stringResource(R.string.export_report_button),
                            )
                        }
                        DropdownMenu(
                            expanded = showExportMenu,
                            onDismissRequest = { showExportMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.export_pdf_button)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.PictureAsPdf,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                },
                                onClick = {
                                    showExportMenu = false
                                    onExportPdf()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.export_xlsx_button)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.TableChart,
                                        contentDescription = null,
                                        tint = BrandSuccess,
                                    )
                                },
                                onClick = {
                                    showExportMenu = false
                                    onExportXlsx()
                                },
                            )
                        }
                    }

                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.retry),
                        )
                    }
                },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            // Month selector bar
            MonthSelectorHeader(
                selectedMonth = uiState.selectedMonth,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onMonthTextClick = { showMonthPicker = true },
            )

            // Offline banner
            AnimatedVisibility(visible = uiState.report?.isFromCache == true) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
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

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading && uiState.report == null -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }

                    uiState.report != null -> {
                        ReportContent(
                            report = uiState.report,
                            onExportPdf = onExportPdf,
                            onExportXlsx = onExportXlsx,
                            onTrainingClick = onNavigateToTrainingDetail,
                        )
                    }

                    else -> {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = uiState.errorMessage ?: "Gagal memuat data laporan.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onRefresh) {
                                Text(stringResource(R.string.retry))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthSelectorHeader(
    selectedMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onMonthTextClick: () -> Unit,
) {
    val idLocale = Locale.forLanguageTag("id-ID")
    val monthName = selectedMonth.month.getDisplayName(TextStyle.FULL, idLocale)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Bulan sebelumnya",
                )
            }

            Row(
                modifier = Modifier
                    .clickable(onClick = onMonthTextClick)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$monthName ${selectedMonth.year}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            IconButton(onClick = onNextMonth) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Bulan berikutnya",
                )
            }
        }
    }
}

@Composable
private fun ReportContent(
    report: MonthlyReport,
    onExportPdf: () -> Unit,
    onExportXlsx: () -> Unit,
    onTrainingClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            MonthlySummaryCard(report = report)
        }

        if (report.trainings.isNotEmpty()) {
            item {
                TrainingParticipantBarChart(
                    trainings = report.trainings,
                    onTrainingClick = onTrainingClick,
                )
            }
        }

        item {
            // Tombol Cepat Ekspor PDF & Excel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onExportPdf,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.export_pdf_button))
                }

                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onExportXlsx,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.TableChart,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = BrandSuccess,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.export_xlsx_button))
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.report_trainings_detail_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${report.trainings.size} kegiatan",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (report.trainings.isEmpty()) {
            item {
                EmptyReportCard()
            }
        } else {
            items(report.trainings, key = { it.training.id }) { row ->
                TrainingReportCard(
                    row = row,
                    onClick = { onTrainingClick(row.training.id) },
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun MonthlySummaryCard(report: MonthlyReport) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.report_summary_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            // Section Keuangan
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricRow(
                    label = stringResource(R.string.metric_total_billed),
                    value = report.totalBilled.toRupiahString(),
                    isBold = true,
                )
                MetricRow(
                    label = stringResource(R.string.metric_total_income),
                    value = report.totalIncome.toRupiahString(),
                    valueColor = BrandSuccess,
                    isBold = true,
                )
                MetricRow(
                    label = stringResource(R.string.metric_outstanding),
                    value = report.outstanding.toRupiahString(),
                    valueColor = if (report.outstanding > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    isBold = true,
                )
            }

            HorizontalDivider()

            // Section Jumlah Peserta
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    CountPill(
                        label = stringResource(R.string.metric_training_count),
                        count = report.trainingCount.toString(),
                        icon = Icons.Default.Event,
                    )
                    CountPill(
                        label = stringResource(R.string.metric_total_registrations),
                        count = report.registrationCount.toString(),
                        icon = Icons.Default.Group,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    CountPill(
                        label = stringResource(R.string.metric_paid_count),
                        count = report.paidCount.toString(),
                        icon = Icons.Default.Payments,
                    )
                    CountPill(
                        label = stringResource(R.string.metric_unpaid_count),
                        count = report.unpaidCount.toString(),
                        icon = Icons.Default.Payments,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    CountPill(
                        label = stringResource(R.string.metric_early_bird_count),
                        count = report.earlyBirdCount.toString(),
                    )
                    CountPill(
                        label = stringResource(R.string.metric_normal_count),
                        count = report.normalCount.toString(),
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    isBold: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = valueColor,
        )
    }
}

@Composable
private fun CountPill(
    label: String,
    count: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = count,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun TrainingReportCard(
    row: TrainingReportRow,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = row.training.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatSchedule(
                            row.training.startAt,
                            row.training.endAt,
                            row.training.timeZoneId,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                val methodLabel = if (row.training.meetingMethod == MeetingMethod.ONLINE) {
                    stringResource(R.string.method_online)
                } else {
                    stringResource(R.string.method_offline)
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        text = methodLabel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Pendaftar: ${row.registrationCount} (${row.paidCount} Lunas, ${row.unpaidCount} Belum)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Early: ${row.earlyBirdCount} | Normal: ${row.normalCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Total Tagihan",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = row.totalBilled.toRupiahString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Pemasukan",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = row.totalIncome.toRupiahString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandSuccess,
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Sisa Tagihan",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = row.outstanding.toRupiahString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (row.outstanding > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyReportCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Event,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.empty_report_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.empty_report_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun MonthYearPickerDialog(
    currentMonth: YearMonth,
    onDismissRequest: () -> Unit,
    onConfirm: (YearMonth) -> Unit,
) {
    var selectedYear by remember { mutableIntStateOf(currentMonth.year) }
    var selectedMonthVal by remember { mutableIntStateOf(currentMonth.monthValue) }

    val idLocale = Locale.forLanguageTag("id-ID")

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = stringResource(R.string.filter_month_year),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Year selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { selectedYear-- }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Tahun sebelumnya")
                    }
                    Text(
                        text = selectedYear.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(onClick = { selectedYear++ }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Tahun berikutnya")
                    }
                }

                HorizontalDivider()

                // Months grid (3 columns x 4 rows)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (row in 0..3) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            for (col in 1..3) {
                                val monthNum = row * 3 + col
                                val month = Month.of(monthNum)
                                val monthShort = month.getDisplayName(TextStyle.SHORT, idLocale)
                                val isSelected = monthNum == selectedMonthVal

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedMonthVal = monthNum },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    },
                                    contentColor = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                ) {
                                    Text(
                                        text = monthShort,
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(YearMonth.of(selectedYear, selectedMonthVal))
                },
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

private enum class ExportShareDestination {
    WHATSAPP,
    EMAIL,
    GENERAL,
}

private fun exportAndShare(
    context: Context,
    uiState: MonthlyReportUiState,
    viewModel: MonthlyReportViewModel,
    isPdf: Boolean,
    destination: ExportShareDestination,
) {
    val report = uiState.report ?: return
    val idLocale = Locale.forLanguageTag("id-ID")
    val monthName = report.month.month.getDisplayName(TextStyle.FULL, idLocale)
    val year = report.month.year
    val ext = if (isPdf) "pdf" else "xlsx"
    val mimeType = if (isPdf) {
        "application/pdf"
    } else {
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    }

    val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
    val file = File(reportsDir, "Laporan_Pelatihan_${monthName}_$year.$ext")

    val fileOutputStream = try {
        FileOutputStream(file)
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal membuat berkas: ${e.message}", Toast.LENGTH_SHORT).show()
        return
    }

    val onComplete: () -> Unit = {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
            val subject = "Laporan Pelatihan - $monthName $year"
            val textBody = "Berikut terlampir dokumen Laporan Pelatihan periode $monthName $year.\n\nSalam,\nE-Pelatihan"

            when (destination) {
                ExportShareDestination.WHATSAPP -> {
                    val waIntent = Intent(Intent.ACTION_SEND).apply {
                        type = mimeType
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_TEXT, textBody)
                        setPackage("com.whatsapp")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    try {
                        context.startActivity(waIntent)
                    } catch (_: Exception) {
                        try {
                            waIntent.setPackage("com.whatsapp.w4b")
                            context.startActivity(waIntent)
                        } catch (_: Exception) {
                            waIntent.setPackage(null)
                            context.startActivity(Intent.createChooser(waIntent, "Kirim via WhatsApp"))
                        }
                    }
                }

                ExportShareDestination.EMAIL -> {
                    val emailIntent = Intent(Intent.ACTION_SEND).apply {
                        type = mimeType
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, subject)
                        putExtra(Intent.EXTRA_TEXT, textBody)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    try {
                        emailIntent.selector = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
                        context.startActivity(Intent.createChooser(emailIntent, "Kirim via Email"))
                    } catch (_: Exception) {
                        emailIntent.selector = null
                        context.startActivity(Intent.createChooser(emailIntent, "Kirim via Email"))
                    }
                }

                ExportShareDestination.GENERAL -> {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = mimeType
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, subject)
                        putExtra(Intent.EXTRA_TEXT, textBody)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Bagikan Dokumen Laporan"))
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membagikan berkas: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    if (isPdf) {
        viewModel.exportPdf(fileOutputStream, onComplete)
    } else {
        viewModel.exportXlsx(fileOutputStream, onComplete)
    }
}
