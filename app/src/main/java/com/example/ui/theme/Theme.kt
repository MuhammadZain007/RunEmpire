package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ThemeColorScheme = darkColorScheme(
    primary = RunPrimary,
    secondary = RunSecondary,
    background = RunBackground,
    surface = RunSurface,
    surfaceVariant = RunSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    error = ErrorColor,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ThemeColorScheme,
        typography = Typography,
        content = content
    )
}
