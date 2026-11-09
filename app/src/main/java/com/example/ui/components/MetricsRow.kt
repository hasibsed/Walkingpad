package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.OrangeWarning
import com.example.ui.theme.VoltGreen

@Composable
fun MetricsRow(
    durationSeconds: Int,
    distanceMeters: Float,
    steps: Int,
    calories: Float,
    cadenceSpm: Int,
    modifier: Modifier = Modifier
) {
    val distanceKm = distanceMeters / 1000f
    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("metrics_panel"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Primary Row: Time & Distance
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                label = "ACTIVE TIME",
                value = timeFormatted,
                unit = "MM:SS",
                accentColor = CyanAccent,
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                label = "DISTANCE",
                value = String.format("%.2f", distanceKm),
                unit = "KM",
                accentColor = VoltGreen,
                modifier = Modifier.weight(1f)
            )
        }

        // Secondary Row: Steps, Calories, Cadence
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(
                label = "STEPS",
                value = "$steps",
                unit = "COUNT",
                accentColor = Color.White,
                modifier = Modifier.weight(1f),
                isCompact = true
            )

            MetricCard(
                label = "CALORIES",
                value = "${calories.toInt()}",
                unit = "KCAL",
                accentColor = OrangeWarning,
                modifier = Modifier.weight(1f),
                isCompact = true
            )

            MetricCard(
                label = "CADENCE",
                value = "$cadenceSpm",
                unit = "SPM",
                accentColor = CyanAccent,
                modifier = Modifier.weight(1f),
                isCompact = true
            )
        }
    }
}

@Composable
fun MetricCard(
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
            .padding(horizontal = if (isCompact) 10.dp else 16.dp, vertical = if (isCompact) 10.dp else 14.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            fontSize = if (isCompact) 10.sp else 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF8B949E),
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 6.dp))

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                fontSize = if (isCompact) 20.sp else 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = accentColor
            )

            Text(
                text = unit,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF6E7681),
                modifier = Modifier.padding(bottom = if (isCompact) 2.dp else 3.dp)
            )
        }
    }
}
