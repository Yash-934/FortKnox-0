package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AegisDarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = CyberBackground,
    primaryContainer = CyberSurfaceVariant,
    onPrimaryContainer = CyberCyan,
    secondary = CyberEmerald,
    onSecondary = CyberBackground,
    secondaryContainer = CyberSurfaceVariant,
    onSecondaryContainer = CyberEmerald,
    tertiary = CyberPurple,
    onTertiary = CyberTextPrimary,
    background = CyberBackground,
    onBackground = CyberTextPrimary,
    surface = CyberSurface,
    onSurface = CyberTextPrimary,
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = CyberTextSecondary,
    error = CyberLaserRed,
    onError = CyberTextPrimary,
    outline = CyberBorder,
    outlineVariant = CyberSurfaceHigh
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AegisDarkColorScheme,
        typography = Typography,
        content = content
    )
}
