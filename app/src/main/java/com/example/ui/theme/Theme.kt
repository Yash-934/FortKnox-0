package com.example.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.preferences.CyberpunkTemplate
import com.example.data.preferences.ThemeConfig

fun getPaletteForConfig(config: ThemeConfig): CyberPalette {
    if (!config.isCyberpunkModeEnabled) {
        return DefaultCleanPalette
    }
    return when (config.selectedTemplate) {
        CyberpunkTemplate.JARVIS_CYAN -> JarvisCyanPalette
        CyberpunkTemplate.STARK_IND_AMBER -> StarkAmberPalette
        CyberpunkTemplate.VERONICA_CRIMSON -> VeronicaCrimsonPalette
        CyberpunkTemplate.CYBER_MATRIX_EMERALD -> CyberMatrixEmeraldPalette
    }
}

fun createMaterial3ColorScheme(palette: CyberPalette) = darkColorScheme(
    primary = palette.primary,
    onPrimary = palette.onPrimary,
    primaryContainer = palette.primaryContainer,
    onPrimaryContainer = palette.onPrimaryContainer,
    secondary = palette.secondary,
    onSecondary = palette.onSecondary,
    secondaryContainer = palette.surfaceVariant,
    onSecondaryContainer = palette.secondary,
    tertiary = palette.borderGlowing,
    onTertiary = palette.onPrimary,
    background = palette.background,
    onBackground = palette.onBackground,
    surface = palette.surface,
    onSurface = palette.onSurface,
    surfaceVariant = palette.surfaceVariant,
    onSurfaceVariant = palette.onSurfaceVariant,
    error = palette.error,
    onError = Color.White,
    outline = palette.border,
    outlineVariant = palette.surfaceHigh
)

@Composable
fun ScanlineOverlay(
    modifier: Modifier = Modifier,
    lineColor: Color = LocalCyberColors.current.scanlineColor,
    alpha: Float = 0.035f
) {
    if (LocalCyberColors.current.isCyberpunk) {
        Canvas(
            modifier = modifier.fillMaxSize()
        ) {
            val step = 4.dp.toPx()
            val effectiveColor = lineColor.copy(alpha = alpha)
            var y = 0f
            while (y < size.height) {
                drawLine(
                    color = effectiveColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
                y += step
            }
        }
    }
}

@Composable
fun MyApplicationTheme(
    themeConfig: ThemeConfig = ThemeConfig(),
    content: @Composable () -> Unit
) {
    val palette = remember(themeConfig.isCyberpunkModeEnabled, themeConfig.selectedTemplate) {
        getPaletteForConfig(themeConfig)
    }

    val colorScheme = remember(palette) {
        createMaterial3ColorScheme(palette)
    }

    CompositionLocalProvider(LocalCyberColors provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                content()
                if (palette.isCyberpunk) {
                    ScanlineOverlay()
                }
            }
        }
    }
}
