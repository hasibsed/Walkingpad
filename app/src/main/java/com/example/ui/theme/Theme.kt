package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = VoltGreen,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF00391E),
    onPrimaryContainer = VoltGreenBright,
    secondary = CyanAccent,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF004D66),
    onSecondaryContainer = Color(0xFFBCE9FF),
    tertiary = OrangeWarning,
    error = EmergencyRed,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    outlineVariant = DarkSurfaceElevated
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF008945),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCEFBD8),
    onPrimaryContainer = Color(0xFF00210B),
    secondary = Color(0xFF007A99),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC4EFFF),
    tertiary = OrangeWarning,
    error = EmergencyRed,
    background = Color(0xFFF7F9FB),
    onBackground = Color(0xFF141923),
    surface = Color.White,
    onSurface = Color(0xFF141923),
    surfaceVariant = Color(0xFFE8EEF5),
    onSurfaceVariant = Color(0xFF484F58),
    outline = Color(0xFFD0D7DE)
)

@Composable
fun WalkingPadTheme(
    darkTheme: Boolean = true, // Default to focused dark athletic cockpit
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
