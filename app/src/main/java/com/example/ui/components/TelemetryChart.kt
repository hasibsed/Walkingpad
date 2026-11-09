package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WorkoutTelemetrySample
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.OrangeWarning
import com.example.ui.theme.VoltGreen
import com.example.ui.theme.VoltGreenBright

enum class ChartMetric {
    SPEED,
    CADENCE,
    CALORIES
}

@Composable
fun TelemetryChart(
    samples: List<WorkoutTelemetrySample>,
    modifier: Modifier = Modifier
) {
    var selectedMetric by remember { mutableStateOf(ChartMetric.SPEED) }
    var scrubbedSampleIndex by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(16.dp))
            .border(1.dp, DarkSurfaceVariant, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("telemetry_chart_card")
    ) {
        // Metric Selectors Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SESSION TELEMETRY",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8B949E),
                letterSpacing = 1.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MetricChip(
                    label = "Speed",
                    isSelected = selectedMetric == ChartMetric.SPEED,
                    onClick = { selectedMetric = ChartMetric.SPEED }
                )
                MetricChip(
                    label = "Cadence",
                    isSelected = selectedMetric == ChartMetric.CADENCE,
                    onClick = { selectedMetric = ChartMetric.CADENCE }
                )
                MetricChip(
                    label = "Kcal",
                    isSelected = selectedMetric == ChartMetric.CALORIES,
                    onClick = { selectedMetric = ChartMetric.CALORIES }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (samples.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No telemetry recorded for this session",
                    fontSize = 13.sp,
                    color = Color(0xFF6E7681)
                )
            }
        } else {
            // Interactive Scrubber Tooltip
            val scrubbed = scrubbedSampleIndex?.let { samples.getOrNull(it) } ?: samples.lastOrNull()
            if (scrubbed != null) {
                val mins = scrubbed.elapsedSeconds / 60
                val secs = scrubbed.elapsedSeconds % 60
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceElevated, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Time: ${String.format("%02d:%02d", mins, secs)}",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                    Text(
                        text = "Speed: ${String.format("%.1f", scrubbed.speedKmh)} km/h",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = VoltGreen
                    )
                    Text(
                        text = "${scrubbed.cadenceSpm} SPM",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyanAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Chart Canvas
            val lineColor = when (selectedMetric) {
                ChartMetric.SPEED -> VoltGreen
                ChartMetric.CADENCE -> CyanAccent
                ChartMetric.CALORIES -> OrangeWarning
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(samples) {
                            detectTapGestures { offset ->
                                val count = samples.size
                                if (count > 1) {
                                    val index = ((offset.x / size.width) * (count - 1)).toInt().coerceIn(0, count - 1)
                                    scrubbedSampleIndex = index
                                }
                            }
                        }
                ) {
                    val width = size.width
                    val height = size.height
                    val paddingBottom = 20.dp.toPx()
                    val chartHeight = height - paddingBottom

                    // Draw Horizontal Grid lines
                    for (i in 0..4) {
                        val y = (chartHeight / 4) * i
                        drawLine(
                            color = Color(0xFF263042),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Extract data points
                    val values = samples.map { sample ->
                        when (selectedMetric) {
                            ChartMetric.SPEED -> sample.speedKmh
                            ChartMetric.CADENCE -> sample.cadenceSpm.toFloat()
                            ChartMetric.CALORIES -> sample.instantaneousCalories
                        }
                    }

                    val maxValue = values.maxOrNull()?.coerceAtLeast(1.0f) ?: 1.0f
                    val minValue = 0.0f
                    val count = values.size

                    if (count >= 2) {
                        val path = Path()
                        val fillPath = Path()

                        for (i in 0 until count) {
                            val x = (i.toFloat() / (count - 1)) * width
                            val normY = (values[i] - minValue) / (maxValue - minValue)
                            val y = chartHeight - (normY * chartHeight)

                            if (i == 0) {
                                path.moveTo(x, y)
                                fillPath.moveTo(x, chartHeight)
                                fillPath.lineTo(x, y)
                            } else {
                                path.lineTo(x, y)
                                fillPath.lineTo(x, y)
                            }
                        }

                        fillPath.lineTo(width, chartHeight)
                        fillPath.close()

                        // Draw Gradient Area Under Curve
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(lineColor.copy(alpha = 0.35f), Color.Transparent),
                                startY = 0f,
                                endY = chartHeight
                            )
                        )

                        // Draw Line Path
                        drawPath(
                            path = path,
                            color = lineColor,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Draw Scrubber Indicator Line if tapped
                        scrubbedSampleIndex?.let { idx ->
                            val scrubX = (idx.toFloat() / (count - 1)) * width
                            drawLine(
                                color = Color.White,
                                start = Offset(scrubX, 0f),
                                end = Offset(scrubX, chartHeight),
                                strokeWidth = 2.dp.toPx()
                            )
                            val normY = (values[idx] - minValue) / (maxValue - minValue)
                            val scrubY = chartHeight - (normY * chartHeight)
                            drawCircle(
                                color = Color.White,
                                radius = 5.dp.toPx(),
                                center = Offset(scrubX, scrubY)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = VoltGreen,
            selectedLabelColor = Color.Black,
            containerColor = DarkSurfaceElevated,
            labelColor = Color(0xFF8B949E)
        ),
        shape = RoundedCornerShape(8.dp),
        border = null
    )
}
