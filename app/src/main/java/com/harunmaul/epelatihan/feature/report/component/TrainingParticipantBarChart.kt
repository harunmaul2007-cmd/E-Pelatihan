package com.harunmaul.epelatihan.feature.report.component

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harunmaul.epelatihan.core.common.toRupiahString
import com.harunmaul.epelatihan.domain.model.TrainingReportRow
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TrainingParticipantBarChart(
    trainings: List<TrainingReportRow>,
    modifier: Modifier = Modifier,
    onTrainingClick: (String) -> Unit = {},
) {
    if (trainings.isEmpty()) return

    var selectedRow by remember(trainings) {
        mutableStateOf<TrainingReportRow?>(null)
    }

    val maxParticipants = remember(trainings) {
        val highest = trainings.maxOfOrNull { it.registrationCount } ?: 0
        if (highest <= 5) 5 else highest
    }

    val totalAllRegistrations = remember(trainings) {
        trainings.sumOf { it.registrationCount }
    }

    val averageParticipants = remember(trainings) {
        if (trainings.isNotEmpty()) {
            "%.1f".format(Locale.US, totalAllRegistrations.toFloat() / trainings.size)
        } else {
            "0"
        }
    }

    val topTraining = remember(trainings) {
        trainings.maxByOrNull { it.registrationCount }
    }

    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("id-ID"))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFE0F2FE), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "Grafik Tren Peserta",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Jumlah peserta per kegiatan pelatihan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Legend Lunas
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color(0xFF0284C7), RoundedCornerShape(2.dp)),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Lunas",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Legend Belum Bayar
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color(0xFFF59E0B), RoundedCornerShape(2.dp)),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Belum Lunas",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // The Chart Canvas / Bars Area
            val scrollState = rememberScrollState()
            val chartHeight = 170.dp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight + 50.dp),
            ) {
                // Background grid lines (25%, 50%, 75%, 100%)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    repeat(5) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            thickness = 1.dp,
                        )
                    }
                }

                // Scrollable bars
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(scrollState)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = if (trainings.size <= 4) {
                        Arrangement.SpaceEvenly
                    } else {
                        Arrangement.spacedBy(16.dp)
                    },
                    verticalAlignment = Alignment.Bottom,
                ) {
                    trainings.forEach { row ->
                        val isSelected = selectedRow?.training?.id == row.training.id
                        val total = row.registrationCount
                        val paid = row.paidCount
                        val unpaid = row.unpaidCount

                        val barFillRatio = (total.toFloat() / maxParticipants.toFloat()).coerceIn(0f, 1f)
                        val paidRatio = if (total > 0) paid.toFloat() / total.toFloat() else 0f
                        val unpaidRatio = if (total > 0) unpaid.toFloat() / total.toFloat() else 0f

                        val zone = try {
                            ZoneId.of(row.training.timeZoneId)
                        } catch (_: Exception) {
                            ZoneId.systemDefault()
                        }
                        val dateText = row.training.startAt.atZone(zone).format(dateFormatter)

                        Column(
                            modifier = Modifier
                                .width(if (trainings.size <= 3) 80.dp else 64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedRow = if (isSelected) null else row
                                }
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                        ) {
                            // Total count label above the bar
                            Text(
                                text = "$total",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFF0284C7) else MaterialTheme.colorScheme.onSurface,
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Bar track
                            Box(
                                modifier = Modifier
                                    .width(if (trainings.size <= 3) 36.dp else 28.dp)
                                    .height(chartHeight * barFillRatio.coerceAtLeast(0.08f))
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(
                                        if (isSelected) {
                                            Color(0xFFBAE6FD)
                                        } else {
                                            Color(0xFFF1F5F9)
                                        },
                                    ),
                                contentAlignment = Alignment.BottomCenter,
                            ) {
                                // Stacked colors: Lunas (bottom) and Unpaid (top)
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Bottom,
                                ) {
                                    // Unpaid segment (Amber)
                                    if (unpaid > 0) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(unpaidRatio.coerceAtLeast(0.01f))
                                                .background(
                                                    Brush.verticalGradient(
                                                        colors = listOf(
                                                            Color(0xFFFBBF24),
                                                            Color(0xFFF59E0B),
                                                        ),
                                                    ),
                                                ),
                                        )
                                    }

                                    // Paid segment (Blue)
                                    if (paid > 0 || total == 0) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(paidRatio.coerceAtLeast(0.01f))
                                                .background(
                                                    Brush.verticalGradient(
                                                        colors = listOf(
                                                            Color(0xFF38BDF8),
                                                            Color(0xFF0284C7),
                                                        ),
                                                    ),
                                                ),
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Date
                            Text(
                                text = dateText,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            // Training Name Truncated
                            Text(
                                text = row.training.name,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isSelected) Color(0xFF0284C7) else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            // Interactive Tooltip / Selected Details
            selectedRow?.let { sel ->
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onTrainingClick(sel.training.id) },
                    color = Color(0xFFF0F9FF),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = sel.training.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0369A1),
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "Buka Detail ➔",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF0284C7),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Text(
                                text = "Total: ${sel.registrationCount} peserta",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Lunas: ${sel.paidCount}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF0284C7),
                            )
                            Text(
                                text = "Belum: ${sel.unpaidCount}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFD97706),
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Pemasukan: ${sel.totalIncome.toRupiahString()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Summary Footer: Average & Top Performer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Rata-rata: $averageParticipants peserta/kegiatan",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (topTraining != null && topTraining.registrationCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFFF59E0B),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Top: ${topTraining.training.name.take(10)} (${topTraining.registrationCount})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
