package com.harunmaul.epelatihan.feature.home.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harunmaul.epelatihan.feature.home.MonthlyParticipantTrend
import java.time.LocalDate

@Composable
fun AnnualParticipantLineChartCard(
    year: Int,
    trends: List<MonthlyParticipantTrend>,
    totalYearParticipants: Int,
    modifier: Modifier = Modifier,
) {
    val currentMonthValue = remember { LocalDate.now().monthValue }

    // Default selection to current month or highest month
    var selectedMonthIndex by remember(trends) {
        val peak = trends.maxByOrNull { it.participantCount }
        val defaultIdx = if (peak != null && peak.participantCount > 0) {
            peak.monthValue - 1
        } else {
            (currentMonthValue - 1).coerceIn(0, 11)
        }
        mutableIntStateOf(defaultIdx)
    }

    val selectedTrend = trends.getOrNull(selectedMonthIndex)

    val maxParticipants = remember(trends) {
        val highest = trends.maxOfOrNull { it.participantCount } ?: 0
        if (highest <= 5) 5 else highest
    }

    val peakTrend = remember(trends) {
        trends.filter { it.participantCount > 0 }.maxByOrNull { it.participantCount }
    }

    val chartLineColor = Color(0xFF0284C7)
    val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

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
            // 1. Header
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
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Grafik Peserta Tahun $year",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Tren kenaikan & penurunan 1 tahun berjalan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFE0F2FE),
                ) {
                    Text(
                        text = "$totalYearParticipants Peserta",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0369A1),
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Selected Month Info Pill
            AnimatedVisibility(
                visible = selectedTrend != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                selectedTrend?.let { trend ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF0F9FF),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Bulan ${trend.monthName} $year",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0369A1),
                            )
                            Text(
                                text = "${trend.participantCount} Peserta",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Line Chart Canvas
            val chartHeight = 160.dp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight),
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight)
                        .pointerInput(trends) {
                            detectTapGestures { offset ->
                                val availableWidth = size.width - 40f
                                if (availableWidth > 0 && trends.isNotEmpty()) {
                                    val relX = (offset.x - 20f).coerceIn(0f, availableWidth)
                                    val step = availableWidth / (trends.size - 1)
                                    val closestIndex = ((relX + step / 2) / step).toInt().coerceIn(0, trends.size - 1)
                                    selectedMonthIndex = closestIndex
                                }
                            }
                        },
                ) {
                    val width = size.width
                    val height = size.height

                    val paddingLeft = 32f
                    val paddingRight = 16f
                    val paddingTop = 16f
                    val paddingBottom = 20f

                    val drawWidth = width - paddingLeft - paddingRight
                    val drawHeight = height - paddingTop - paddingBottom

                    // Draw Horizontal Grid Lines
                    val gridSteps = 3
                    for (i in 0..gridSteps) {
                        val gridY = paddingTop + (drawHeight / gridSteps) * i
                        drawLine(
                            color = gridLineColor,
                            start = Offset(paddingLeft, gridY),
                            end = Offset(width - paddingRight, gridY),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
                        )
                    }

                    if (trends.isEmpty()) return@Canvas

                    val stepX = drawWidth / (trends.size - 1)

                    // Calculate Coordinates for all 12 points
                    val points = trends.mapIndexed { index, item ->
                        val ratio = if (maxParticipants > 0) {
                            (item.participantCount.toFloat() / maxParticipants.toFloat()).coerceIn(0f, 1f)
                        } else {
                            0f
                        }
                        val x = paddingLeft + index * stepX
                        val y = paddingTop + drawHeight * (1f - ratio)
                        Offset(x, y)
                    }

                    // Build Cubic Bezier Path for Smooth Wave
                    val linePath = Path()
                    val fillPath = Path()

                    if (points.isNotEmpty()) {
                        linePath.moveTo(points[0].x, points[0].y)
                        fillPath.moveTo(points[0].x, points[0].y)

                        for (i in 0 until points.size - 1) {
                            val p0 = points[i]
                            val p1 = points[i + 1]

                            val controlX1 = p0.x + (p1.x - p0.x) / 2
                            val controlY1 = p0.y
                            val controlX2 = p0.x + (p1.x - p0.x) / 2
                            val controlY2 = p1.y

                            linePath.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                            fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                        }

                        // Close fill path down to baseline
                        val baselineY = paddingTop + drawHeight
                        fillPath.lineTo(points.last().x, baselineY)
                        fillPath.lineTo(points.first().x, baselineY)
                        fillPath.close()

                        // Draw Gradient Area Fill under the curve
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    chartLineColor.copy(alpha = 0.32f),
                                    chartLineColor.copy(alpha = 0.02f),
                                ),
                                startY = paddingTop,
                                endY = baselineY,
                            ),
                        )

                        // Draw the Line Curve
                        drawPath(
                            path = linePath,
                            color = chartLineColor,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round,
                            ),
                        )

                        // Draw vertical guide line for selected point
                        if (selectedMonthIndex in points.indices) {
                            val selPoint = points[selectedMonthIndex]
                            drawLine(
                                color = chartLineColor.copy(alpha = 0.4f),
                                start = Offset(selPoint.x, paddingTop),
                                end = Offset(selPoint.x, baselineY),
                                strokeWidth = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
                            )
                        }

                        // Draw Circles on Points
                        points.forEachIndexed { index, pt ->
                            val isSelected = index == selectedMonthIndex
                            val count = trends[index].participantCount

                            if (isSelected) {
                                // Outer Glow Ring
                                drawCircle(
                                    color = chartLineColor.copy(alpha = 0.25f),
                                    radius = 9.dp.toPx(),
                                    center = pt,
                                )
                                // Outer Ring
                                drawCircle(
                                    color = chartLineColor,
                                    radius = 6.dp.toPx(),
                                    center = pt,
                                )
                                // Center White Dot
                                drawCircle(
                                    color = Color.White,
                                    radius = 3.dp.toPx(),
                                    center = pt,
                                )
                            } else if (count > 0) {
                                drawCircle(
                                    color = chartLineColor,
                                    radius = 3.5.dp.toPx(),
                                    center = pt,
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 1.5.dp.toPx(),
                                    center = pt,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 4. X-Axis Month Labels (Jan - Des)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                trends.forEachIndexed { index, trend ->
                    val isSelected = index == selectedMonthIndex
                    Text(
                        text = trend.monthName,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) {
                            Color(0xFF0284C7)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { selectedMonthIndex = index }
                            .padding(horizontal = 2.dp, vertical = 2.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Insights Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (peakTrend != null && peakTrend.participantCount > 0) {
                        "Partisipasi tertinggi ada di bulan ${peakTrend.monthName} (${peakTrend.participantCount} peserta)."
                    } else {
                        "Belum ada kegiatan pelatihan yang diselenggarakan di tahun $year."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
