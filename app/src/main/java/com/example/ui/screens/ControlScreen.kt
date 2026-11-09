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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bluetooth.BeltState
import com.example.bluetooth.ConnectionStatus
import com.example.ui.GoalType
import com.example.ui.WalkingPadViewModel
import com.example.ui.components.MetricsRow
import com.example.ui.components.SpeedControls
import com.example.ui.components.SpeedGauge
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.OrangeWarning
import com.example.ui.theme.VoltGreen
import com.example.ui.theme.VoltGreenBright

@Composable
fun ControlScreen(
    viewModel: WalkingPadViewModel,
    onNavigateToDevice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
    val connectedDeviceName by viewModel.connectedDeviceName.collectAsStateWithLifecycle()
    val beltState by viewModel.beltState.collectAsStateWithLifecycle()
    val liveData by viewModel.liveData.collectAsStateWithLifecycle()
    val targetSpeed by viewModel.targetSpeedKmh.collectAsStateWithLifecycle()

    val isSessionActive by viewModel.isSessionActive.collectAsStateWithLifecycle()
    val sessionDuration by viewModel.sessionDurationSec.collectAsStateWithLifecycle()
    val sessionDistance by viewModel.sessionDistanceMeters.collectAsStateWithLifecycle()
    val sessionSteps by viewModel.sessionSteps.collectAsStateWithLifecycle()
    val sessionCalories by viewModel.sessionCalories.collectAsStateWithLifecycle()

    val goalType by viewModel.goalType.collectAsStateWithLifecycle()
    val goalTargetValue by viewModel.goalTargetValue.collectAsStateWithLifecycle()
    val goalProgress by viewModel.goalProgress.collectAsStateWithLifecycle()

    var showGoalDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Connection Status Header
        ConnectionStatusBanner(
            status = connectionStatus,
            deviceName = connectedDeviceName,
            onConnectClick = onNavigateToDevice
        )

        // Target Goal Banner / Progress (if set)
        if (goalType != GoalType.NONE) {
            GoalProgressBar(
                goalType = goalType,
                targetValue = goalTargetValue,
                progress = goalProgress,
                onConfigureClick = { showGoalDialog = true }
            )
        } else {
            // Quick Button to Set Target Goal
            TargetGoalTrigger(onClick = { showGoalDialog = true })
        }

        // Speed Gauge Cockpit
        SpeedGauge(
            currentSpeedKmh = liveData.instantaneousSpeedKmh,
            targetSpeedKmh = targetSpeed,
            beltState = beltState
        )

        // Metrics Panel (Time, Distance, Steps, Calories, Cadence)
        MetricsRow(
            durationSeconds = if (isSessionActive) sessionDuration else liveData.elapsedTimeSeconds,
            distanceMeters = if (isSessionActive) sessionDistance else liveData.totalDistanceMeters,
            steps = if (isSessionActive) sessionSteps else liveData.totalSteps,
            calories = if (isSessionActive) sessionCalories else liveData.instantaneousKcal,
            cadenceSpm = liveData.stepCadenceSpm
        )

        // Speed Controls (Fine +/- steppers, preset chips, Start/Pause/Stop actions)
        SpeedControls(
            targetSpeedKmh = targetSpeed,
            beltState = beltState,
            onAdjustSpeed = { delta -> viewModel.adjustSpeed(delta) },
            onSetSpeed = { speed -> viewModel.setTargetSpeed(speed) },
            onStartBelt = { viewModel.startBelt() },
            onPauseBelt = { viewModel.pauseBelt() },
            onEmergencyStop = { viewModel.emergencyStop() }
        )

        // Finish & Save Session Button (visible when a workout session is in progress)
        AnimatedVisibility(visible = isSessionActive) {
            OutlinedButton(
                onClick = { viewModel.finishAndSaveWorkout() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_finish_save_workout"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = VoltGreen
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(VoltGreen)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "FINISH & LOG WORKOUT",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showGoalDialog) {
        GoalSettingDialog(
            currentGoalType = goalType,
            onDismiss = { showGoalDialog = false },
            onSetGoal = { type, value ->
                viewModel.startWorkoutSession(mode = "TARGET_GOAL", goal = type, goalVal = value)
                showGoalDialog = false
            }
        )
    }
}

@Composable
fun ConnectionStatusBanner(
    status: ConnectionStatus,
    deviceName: String?,
    onConnectClick: () -> Unit
) {
    val isConnected = status == ConnectionStatus.CONNECTED
    val statusText = when (status) {
        ConnectionStatus.CONNECTED -> deviceName ?: "WalkingPad MC21"
        ConnectionStatus.CONNECTING -> "Connecting to WalkingPad…"
        ConnectionStatus.SCANNING -> "Scanning for MC21…"
        ConnectionStatus.DISCONNECTED -> "Treadmill Disconnected"
        ConnectionStatus.ERROR -> "Connection Error - Tap to retry"
    }

    val iconColor = when (status) {
        ConnectionStatus.CONNECTED -> VoltGreen
        ConnectionStatus.CONNECTING, ConnectionStatus.SCANNING -> OrangeWarning
        ConnectionStatus.DISCONNECTED -> Color(0xFF8B949E)
        ConnectionStatus.ERROR -> Color(0xFFFF4444)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurface)
            .border(1.dp, if (isConnected) VoltGreen.copy(alpha = 0.4f) else DarkSurfaceVariant, RoundedCornerShape(14.dp))
            .clickable(onClick = onConnectClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("connection_banner"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(iconColor, CircleShape)
            )

            Icon(
                imageVector = if (isConnected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )

            Column {
                Text(
                    text = statusText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (isConnected) "FTMS Real-time Sync Active" else "Tap to pair or scan MC21",
                    fontSize = 11.sp,
                    color = Color(0xFF8B949E)
                )
            }
        }

        Box(
            modifier = Modifier
                .background(if (isConnected) VoltGreen.copy(alpha = 0.15f) else DarkSurfaceElevated, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (isConnected) "MANAGE" else "CONNECT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isConnected) VoltGreen else Color.White
            )
        }
    }
}

@Composable
fun GoalProgressBar(
    goalType: GoalType,
    targetValue: Float,
    progress: Float,
    onConfigureClick: () -> Unit
) {
    val goalLabel = when (goalType) {
        GoalType.DISTANCE -> "Target Distance: ${String.format("%.1f", targetValue)} km"
        GoalType.TIME -> "Target Time: ${targetValue.toInt()} min"
        GoalType.CALORIES -> "Target Energy: ${targetValue.toInt()} kcal"
        GoalType.NONE -> ""
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, CyanAccent.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .clickable(onClick = onConfigureClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("goal_progress_bar"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = goalLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Text(
                text = "${(progress * 100).toInt()}%",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (progress >= 1f) VoltGreen else CyanAccent
            )
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = if (progress >= 1f) VoltGreen else CyanAccent,
            trackColor = Color(0xFF1E2633)
        )
    }
}

@Composable
fun TargetGoalTrigger(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceElevated)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("btn_set_goal_trigger"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Flag,
                contentDescription = null,
                tint = CyanAccent,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Set a target workout goal (Distance, Time, Kcal)",
                fontSize = 12.sp,
                color = Color(0xFF8B949E)
            )
        }
        Text(
            text = "SET",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = CyanAccent
        )
    }
}

@Composable
fun GoalSettingDialog(
    currentGoalType: GoalType,
    onDismiss: () -> Unit,
    onSetGoal: (GoalType, Float) -> Unit
) {
    var selectedType by remember { mutableStateOf(GoalType.DISTANCE) }
    var selectedValue by remember { mutableStateOf(3.0f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Set Workout Goal",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Select your target metric for this walking session:",
                    fontSize = 13.sp,
                    color = Color(0xFF8B949E)
                )

                // Goal Type Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GoalTypeChip(
                        label = "Distance",
                        isSelected = selectedType == GoalType.DISTANCE,
                        onClick = {
                            selectedType = GoalType.DISTANCE
                            selectedValue = 3.0f
                        },
                        modifier = Modifier.weight(1f)
                    )
                    GoalTypeChip(
                        label = "Duration",
                        isSelected = selectedType == GoalType.TIME,
                        onClick = {
                            selectedType = GoalType.TIME
                            selectedValue = 30f
                        },
                        modifier = Modifier.weight(1f)
                    )
                    GoalTypeChip(
                        label = "Calories",
                        isSelected = selectedType == GoalType.CALORIES,
                        onClick = {
                            selectedType = GoalType.CALORIES
                            selectedValue = 200f
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Preset options based on type
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Quick Targets:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )

                    val presets = when (selectedType) {
                        GoalType.DISTANCE -> listOf(1.5f to "1.5 km", 3.0f to "3.0 km", 5.0f to "5.0 km", 10.0f to "10.0 km")
                        GoalType.TIME -> listOf(15f to "15 min", 30f to "30 min", 45f to "45 min", 60f to "60 min")
                        GoalType.CALORIES -> listOf(100f to "100 kcal", 200f to "200 kcal", 350f to "350 kcal", 500f to "500 kcal")
                        GoalType.NONE -> emptyList()
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for ((valOption, label) in presets) {
                            val isChosen = selectedValue == valOption
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isChosen) VoltGreen else DarkSurfaceElevated)
                                    .clickable { selectedValue = valOption }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isChosen) Color.Black else Color.White
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSetGoal(selectedType, selectedValue) },
                colors = ButtonDefaults.buttonColors(containerColor = VoltGreen, contentColor = Color.Black)
            ) {
                Text("Start Goal Session", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF8B949E))
            }
        },
        containerColor = DarkSurface
    )
}

@Composable
fun GoalTypeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) CyanAccent else DarkSurfaceElevated)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.Black else Color.White
        )
    }
}
