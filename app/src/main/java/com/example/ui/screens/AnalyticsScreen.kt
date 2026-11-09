package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.dao.AggregateStats
import com.example.data.model.WorkoutSession
import com.example.data.model.WorkoutTelemetrySample
import com.example.ui.WalkingPadViewModel
import com.example.ui.components.TelemetryChart
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.OrangeWarning
import com.example.ui.theme.VoltGreen
import com.example.ui.theme.VoltGreenBright
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AnalyticsScreen(
    viewModel: WalkingPadViewModel,
    modifier: Modifier = Modifier
) {
    val sessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val aggregateStats by viewModel.aggregateStats.collectAsStateWithLifecycle()

    var selectedSessionForDetail by remember { mutableStateOf<WorkoutSession?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("analytics_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "LIFETIME ANALYTICS",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Track your WalkingPad MC21 mileage, progress, and performance",
                    fontSize = 12.sp,
                    color = Color(0xFF8B949E)
                )
            }
        }

        // Aggregate Lifetime Performance Cockpit
        item {
            LifetimeStatsGrid(stats = aggregateStats)
        }

        // Section Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WORKOUT HISTORY (${sessions.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8B949E),
                    letterSpacing = 1.sp
                )
            }
        }

        if (sessions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(DarkSurface, RoundedCornerShape(14.dp))
                        .border(1.dp, DarkSurfaceVariant, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.DirectionsWalk,
                            contentDescription = null,
                            tint = Color(0xFF6E7681),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No recorded sessions yet",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF8B949E)
                        )
                        Text(
                            text = "Start walking to log your workouts and telemetry charts",
                            fontSize = 12.sp,
                            color = Color(0xFF6E7681)
                        )
                    }
                }
            }
        } else {
            items(sessions, key = { it.id }) { session ->
                WorkoutHistoryCard(
                    session = session,
                    onClick = { selectedSessionForDetail = session },
                    onExportCsv = { viewModel.exportSessionCsv(session.id) },
                    onDelete = { viewModel.deleteSession(session.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Session Detail Dialog with Interactive Telemetry Chart & Export
    selectedSessionForDetail?.let { session ->
        val telemetryFlow = remember(session.id) { viewModel.getTelemetryForSession(session.id) }
        val samples by telemetryFlow.collectAsStateWithLifecycle(emptyList())

        SessionDetailDialog(
            session = session,
            samples = samples,
            onDismiss = { selectedSessionForDetail = null },
            onExportCsv = { viewModel.exportSessionCsv(session.id) },
            onExportJson = { viewModel.exportSessionJson(session.id) },
            onDelete = {
                viewModel.deleteSession(session.id)
                selectedSessionForDetail = null
            }
        )
    }
}

@Composable
fun LifetimeStatsGrid(stats: AggregateStats?) {
    val totalDistKm = (stats?.totalDistanceMeters ?: 0f) / 1000f
    val totalHours = (stats?.totalDurationSeconds ?: 0L) / 3600f
    val totalSteps = stats?.totalSteps ?: 0L
    val totalCalories = stats?.totalCalories ?: 0L
    val maxSpeed = stats?.maxSpeedKmh ?: 0.0f

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LifetimeMetricCard(
                label = "TOTAL DISTANCE",
                value = String.format("%.1f", totalDistKm),
                unit = "KM",
                accentColor = VoltGreen,
                modifier = Modifier.weight(1f)
            )

            LifetimeMetricCard(
                label = "TOTAL TIME",
                value = String.format("%.1f", totalHours),
                unit = "HRS",
                accentColor = CyanAccent,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LifetimeMetricCard(
                label = "TOTAL STEPS",
                value = if (totalSteps > 9999) "${totalSteps / 1000}k" else "$totalSteps",
                unit = "STEPS",
                accentColor = Color.White,
                modifier = Modifier.weight(1f),
                isCompact = true
            )

            LifetimeMetricCard(
                label = "CALORIES",
                value = "$totalCalories",
                unit = "KCAL",
                accentColor = OrangeWarning,
                modifier = Modifier.weight(1f),
                isCompact = true
            )

            LifetimeMetricCard(
                label = "MAX SPEED",
                value = String.format("%.1f", maxSpeed),
                unit = "KM/H",
                accentColor = VoltGreenBright,
                modifier = Modifier.weight(1f),
                isCompact = true
            )
        }
    }
}

@Composable
fun LifetimeMetricCard(
    label: String,
    value: String,
    unit: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    Column(
        modifier = modifier
            .background(DarkSurface, RoundedCornerShape(14.dp))
            .border(1.dp, DarkSurfaceVariant, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = if (isCompact) 10.dp else 14.dp)
    ) {
        Text(
            text = label,
            fontSize = if (isCompact) 10.sp else 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF8B949E),
            letterSpacing = 0.8.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                fontSize = if (isCompact) 20.sp else 26.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = accentColor
            )
            Text(
                text = unit,
                fontSize = 10.sp,
                color = Color(0xFF6E7681),
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
    }
}

@Composable
fun WorkoutHistoryCard(
    session: WorkoutSession,
    onClick: () -> Unit,
    onExportCsv: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("EEE, MMM dd • HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(Date(session.startTime))
    val distKm = session.totalDistanceMeters / 1000f
    val mins = session.durationSeconds / 60
    val secs = session.durationSeconds % 60

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurface)
            .border(1.dp, DarkSurfaceVariant, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
            .testTag("session_card_${session.id}"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            when (session.mode) {
                                "INTERVAL" -> CyanAccent.copy(alpha = 0.15f)
                                "TARGET_GOAL" -> OrangeWarning.copy(alpha = 0.15f)
                                else -> VoltGreen.copy(alpha = 0.15f)
                            },
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = session.mode,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (session.mode) {
                            "INTERVAL" -> CyanAccent
                            "TARGET_GOAL" -> OrangeWarning
                            else -> VoltGreen
                        }
                    )
                }

                Text(
                    text = dateStr,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            IconButton(onClick = onExportCsv, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Export CSV",
                    tint = Color(0xFF8B949E),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Metrics Grid Preview
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = "DISTANCE", fontSize = 10.sp, color = Color(0xFF8B949E))
                Text(
                    text = "${String.format("%.2f", distKm)} km",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
            }

            Column {
                Text(text = "DURATION", fontSize = 10.sp, color = Color(0xFF8B949E))
                Text(
                    text = String.format("%02d:%02d", mins, secs),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
            }

            Column {
                Text(text = "AVG SPEED", fontSize = 10.sp, color = Color(0xFF8B949E))
                Text(
                    text = "${String.format("%.1f", session.avgSpeedKmh)} km/h",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = VoltGreen
                )
            }

            Column {
                Text(text = "STEPS", fontSize = 10.sp, color = Color(0xFF8B949E))
                Text(
                    text = "${session.totalSteps}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = CyanAccent
                )
            }
        }
    }
}

@Composable
fun SessionDetailDialog(
    session: WorkoutSession,
    samples: List<WorkoutTelemetrySample>,
    onDismiss: () -> Unit,
    onExportCsv: () -> Unit,
    onExportJson: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy • HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(Date(session.startTime))
    val distKm = session.totalDistanceMeters / 1000f
    val mins = session.durationSeconds / 60
    val secs = session.durationSeconds % 60

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0E14))
                .padding(16.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with close button
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "WORKOUT DETAILS",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = dateStr,
                                fontSize = 12.sp,
                                color = Color(0xFF8B949E)
                            )
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Summary Stats Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DetailStatBox(label = "Distance", value = "${String.format("%.2f", distKm)} km", modifier = Modifier.weight(1f))
                        DetailStatBox(label = "Time", value = String.format("%02d:%02d", mins, secs), modifier = Modifier.weight(1f))
                        DetailStatBox(label = "Avg Speed", value = "${String.format("%.1f", session.avgSpeedKmh)} km/h", modifier = Modifier.weight(1f))
                        DetailStatBox(label = "Max Speed", value = "${String.format("%.1f", session.maxSpeedKmh)} km/h", modifier = Modifier.weight(1f))
                    }
                }

                // Interactive Telemetry Chart
                item {
                    TelemetryChart(samples = samples)
                }

                // Additional details
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DetailStatBox(label = "Total Steps", value = "${session.totalSteps}", modifier = Modifier.weight(1f))
                        DetailStatBox(label = "Calories", value = "${session.caloriesBurned} kcal", modifier = Modifier.weight(1f))
                        DetailStatBox(label = "Mode", value = session.mode, modifier = Modifier.weight(1f))
                    }
                }

                // Export & Share Actions
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "DATA EXPORT & SHARING",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8B949E),
                            letterSpacing = 1.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onExportCsv,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("btn_export_csv"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VoltGreen,
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export CSV", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = onExportJson,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("btn_export_json"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyanAccent,
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export JSON", fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_delete_workout"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmergencyRed)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete Workout Session", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun DetailStatBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(DarkSurface, RoundedCornerShape(10.dp))
            .border(1.dp, DarkSurfaceVariant, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, fontSize = 10.sp, color = Color(0xFF8B949E))
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color.White
        )
    }
}
