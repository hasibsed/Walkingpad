package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bluetooth.BeltState
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.OrangeWarning
import com.example.ui.theme.VoltGreen

data class SpeedPreset(val speed: Float, val label: String)

val DEFAULT_PRESETS = listOf(
    SpeedPreset(2.0f, "Warmup"),
    SpeedPreset(3.5f, "Walk"),
    SpeedPreset(4.8f, "Brisk"),
    SpeedPreset(6.0f, "Power"),
    SpeedPreset(7.5f, "Jog"),
    SpeedPreset(9.0f, "Run")
)

@Composable
fun SpeedControls(
    targetSpeedKmh: Float,
    beltState: BeltState,
    onAdjustSpeed: (Float) -> Unit,
    onSetSpeed: (Float) -> Unit,
    onStartBelt: () -> Unit,
    onPauseBelt: () -> Unit,
    onEmergencyStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("speed_controls"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Step Stepper Row (-0.5, -0.1, Target Speed, +0.1, +0.5)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface, RoundedCornerShape(16.dp))
                .border(1.dp, DarkSurfaceVariant, RoundedCornerShape(16.dp))
                .padding(vertical = 8.dp, horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Coarse decrement
            StepButton(text = "-0.5", testTag = "btn_sub_05") { onAdjustSpeed(-0.5f) }
            // Fine decrement
            StepButton(text = "-0.1", testTag = "btn_sub_01") { onAdjustSpeed(-0.1f) }

            // Target readout
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${String.format("%.1f", targetSpeedKmh)}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = VoltGreen,
                    modifier = Modifier.testTag("target_speed_display")
                )
                Text(
                    text = "TARGET KM/H",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF8B949E)
                )
            }

            // Fine increment
            StepButton(text = "+0.1", testTag = "btn_add_01") { onAdjustSpeed(0.1f) }
            // Coarse increment
            StepButton(text = "+0.5", testTag = "btn_add_05") { onAdjustSpeed(0.5f) }
        }

        // Quick Preset Chips (Horizontally Scrollable)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (preset in DEFAULT_PRESETS) {
                val isSelected = Math.abs(preset.speed - targetSpeedKmh) < 0.05f
                PresetChip(
                    preset = preset,
                    isSelected = isSelected,
                    onClick = { onSetSpeed(preset.speed) }
                )
            }
        }

        // Action Buttons Row: [START/PAUSE] and [EMERGENCY STOP]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (beltState) {
                BeltState.RUNNING -> {
                    // Pause Button
                    Button(
                        onClick = onPauseBelt,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("btn_pause"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrangeWarning,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pause Belt",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PAUSE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
                BeltState.STARTING -> {
                    Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("btn_starting"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.DarkGray,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "STARTING…",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
                BeltState.PAUSED -> {
                    Button(
                        onClick = onStartBelt,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("btn_resume"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VoltGreen,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Resume Belt",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "RESUME",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
                BeltState.STOPPED -> {
                    Button(
                        onClick = onStartBelt,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("btn_start_belt"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VoltGreen,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Start Belt",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "START BELT",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            // Emergency Stop Button
            Button(
                onClick = onEmergencyStop,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .testTag("btn_emergency_stop"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmergencyRed,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Emergency Stop",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "STOP",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun StepButton(
    text: String,
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceVariant)
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun PresetChip(
    preset: SpeedPreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) VoltGreen else DarkSurface
    val textColor = if (isSelected) Color.Black else Color.White
    val borderColor = if (isSelected) VoltGreen else DarkSurfaceVariant

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("preset_${preset.speed}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "${preset.speed}",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = textColor
        )
        Text(
            text = preset.label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) Color.Black.copy(alpha = 0.8f) else Color(0xFF8B949E)
        )
    }
}
