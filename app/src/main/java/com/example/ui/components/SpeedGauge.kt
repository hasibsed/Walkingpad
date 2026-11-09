package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bluetooth.BeltState
import com.example.bluetooth.WalkingPadBleConstants
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.OrangeWarning
import com.example.ui.theme.VoltGreen
import com.example.ui.theme.VoltGreenBright
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SpeedGauge(
    currentSpeedKmh: Float,
    targetSpeedKmh: Float,
    beltState: BeltState,
    modifier: Modifier = Modifier
) {
    val maxSpeed = WalkingPadBleConstants.MAX_SPEED_KMH
    val minSpeed = WalkingPadBleConstants.MIN_SPEED_KMH

    // Normalize speeds for arc (0.0 to 1.0)
    val animatedCurrentSpeed by animateFloatAsState(
        targetValue = currentSpeedKmh.coerceIn(0f, maxSpeed),
        animationSpec = tween(durationMillis = 400),
        label = "currentSpeed"
    )

    val currentRatio = ((animatedCurrentSpeed - minSpeed) / (maxSpeed - minSpeed)).coerceIn(0f, 1f)
    val targetRatio = ((targetSpeedKmh - minSpeed) / (maxSpeed - minSpeed)).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .size(260.dp)
            .testTag("speed_gauge"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 18.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val arcSize = Size(diameter, diameter)
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)

            val startAngle = 140f
            val sweepAngleTotal = 260f

            // Background Track
            drawArc(
                color = Color(0xFF1E2633),
                startAngle = startAngle,
                sweepAngle = sweepAngleTotal,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Current Speed Active Gradient Arc
            if (animatedCurrentSpeed > 0.05f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to VoltGreen,
                        0.5f to VoltGreenBright,
                        1.0f to CyanAccent
                    ),
                    startAngle = startAngle,
                    sweepAngle = sweepAngleTotal * currentRatio,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Target Speed Marker tick
            val targetAngleRad = Math.toRadians((startAngle + (sweepAngleTotal * targetRatio)).toDouble())
            val radius = diameter / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            val markerInner = Offset(
                x = (center.x + (radius - strokeWidth * 0.7f) * cos(targetAngleRad)).toFloat(),
                y = (center.y + (radius - strokeWidth * 0.7f) * sin(targetAngleRad)).toFloat()
            )
            val markerOuter = Offset(
                x = (center.x + (radius + strokeWidth * 0.7f) * cos(targetAngleRad)).toFloat(),
                y = (center.y + (radius + strokeWidth * 0.7f) * sin(targetAngleRad)).toFloat()
            )

            drawLine(
                color = Color.White,
                start = markerInner,
                end = markerOuter,
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Central Cockpit Readout
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            // Belt State Badge
            val stateColor = when (beltState) {
                BeltState.RUNNING -> VoltGreen
                BeltState.STARTING -> OrangeWarning
                BeltState.PAUSED -> OrangeWarning
                BeltState.STOPPED -> Color.Gray
            }
            val stateText = when (beltState) {
                BeltState.RUNNING -> "RUNNING"
                BeltState.STARTING -> "STARTING…"
                BeltState.PAUSED -> "PAUSED"
                BeltState.STOPPED -> "STOPPED"
            }

            Box(
                modifier = Modifier
                    .background(stateColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = stateText,
                    color = stateColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main Large Digits
            Text(
                text = String.format("%.1f", animatedCurrentSpeed),
                fontSize = 58.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = Color.White,
                modifier = Modifier.testTag("current_speed_readout")
            )

            Text(
                text = "SPEED (KM/H)",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF8B949E),
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Target Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "TARGET: ",
                    fontSize = 12.sp,
                    color = Color(0xFF6E7681)
                )
                Text(
                    text = "${String.format("%.1f", targetSpeedKmh)} km/h",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = VoltGreenBright
                )
            }
        }
    }
}
