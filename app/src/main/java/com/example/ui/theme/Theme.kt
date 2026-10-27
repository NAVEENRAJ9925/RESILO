package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ResilioCyanPrimary,
    onPrimary = Color.Black,
    primaryContainer = ResilioNavyCard,
    onPrimaryContainer = ResilioCyanPrimary,
    secondary = ResilioMeshBlue,
    onSecondary = Color.Black,
    secondaryContainer = ResilioNavyCard,
    onSecondaryContainer = ResilioMeshBlue,
    tertiary = ResilioWarningAmber,
    onTertiary = Color.Black,
    error = ResilioEmergencyRed,
    onError = Color.White,
    background = ResilioNavyDark,
    onBackground = ResilioTextPrimary,
    surface = ResilioNavySurface,
    onSurface = ResilioTextPrimary,
    surfaceVariant = ResilioNavyCard,
    onSurfaceVariant = ResilioTextSecondary,
    outline = ResilioNavyBorder
)

private val LightColorScheme = darkColorScheme( // Keep tactical dark by default for emergency operations
    primary = ResilioCyanPrimary,
    onPrimary = Color.Black,
    primaryContainer = ResilioNavyCard,
    onPrimaryContainer = ResilioCyanPrimary,
    secondary = ResilioMeshBlue,
    onSecondary = Color.Black,
    secondaryContainer = ResilioNavyCard,
    onSecondaryContainer = ResilioMeshBlue,
    tertiary = ResilioWarningAmber,
    onTertiary = Color.Black,
    error = ResilioEmergencyRed,
    onError = Color.White,
    background = ResilioNavyDark,
    onBackground = ResilioTextPrimary,
    surface = ResilioNavySurface,
    onSurface = ResilioTextPrimary,
    surfaceVariant = ResilioNavyCard,
    onSurfaceVariant = ResilioTextSecondary,
    outline = ResilioNavyBorder
)

@Composable
fun ResilioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
