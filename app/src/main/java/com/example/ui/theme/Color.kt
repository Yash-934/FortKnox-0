package com.example.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.data.preferences.CyberpunkTemplate

// Base default palette constants
val CyberBackground = Color(0xFF02060B)
val CyberSurface = Color(0xFF04101A)
val CyberSurfaceVariant = Color(0xFF06141D)
val CyberSurfaceHigh = Color(0xFF0E2A3A)
val CyberCyan = Color(0xFF00E5FF)
val CyberCyanMuted = Color(0xFF0A454F)
val CyberEmerald = Color(0xFF00FF87)
val CyberPurple = Color(0xFFA855F7)
val CyberPink = Color(0xFFFF2A6D)
val CyberGold = Color(0xFFFFB300)
val CyberLaserRed = Color(0xFFFF3333)
val CyberLaserRedGlow = Color(0xFFFF1E1E)
val CyberTextPrimary = Color(0xFFE0F7FA)
val CyberTextSecondary = Color(0xFF4A8B99)
val CyberTextMuted = Color(0xFF235460)
val CyberBorder = Color(0xFF0F3B45)
val CyberBorderGlowing = Color(0xFF00E5FF)

/**
 * Dynamic Cyber Theme Colors holder.
 */
data class CyberPalette(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val surfaceHigh: Color,
    val border: Color,
    val borderGlowing: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val error: Color,
    val glowColor: Color,
    val scanlineColor: Color,
    val isCyberpunk: Boolean,
    val template: CyberpunkTemplate
)

// 1. J.A.R.V.I.S (CYAN)
val JarvisCyanPalette = CyberPalette(
    primary = Color(0xFF00FFE1),
    onPrimary = Color(0xFF0A0E17),
    primaryContainer = Color(0xFF131F33),
    onPrimaryContainer = Color(0xFF00FFE1),
    secondary = Color(0xFF00E5FF),
    onSecondary = Color(0xFF0A0E17),
    background = Color(0xFF0A0E17),
    onBackground = Color(0xFFE0F7FA),
    surface = Color(0xFF0F1726),
    onSurface = Color(0xFFE0F7FA),
    surfaceVariant = Color(0xFF131F33),
    onSurfaceVariant = Color(0xFF5A8E9E),
    surfaceHigh = Color(0xFF1B2B47),
    border = Color(0xFF163B59),
    borderGlowing = Color(0xFF00FFE1),
    textPrimary = Color(0xFFE0F7FA),
    textSecondary = Color(0xFF5A8E9E),
    textMuted = Color(0xFF285566),
    error = Color(0xFFFF3333),
    glowColor = Color(0xFF00FFE1),
    scanlineColor = Color(0xFF00FFE1),
    isCyberpunk = true,
    template = CyberpunkTemplate.JARVIS_CYAN
)

// 2. STARK IND (AMBER)
val StarkAmberPalette = CyberPalette(
    primary = Color(0xFFFFB300),
    onPrimary = Color(0xFF111111),
    primaryContainer = Color(0xFF242424),
    onPrimaryContainer = Color(0xFFFFB300),
    secondary = Color(0xFFFF9100),
    onSecondary = Color(0xFF111111),
    background = Color(0xFF111111),
    onBackground = Color(0xFFFFF8E1),
    surface = Color(0xFF1A1A1A),
    onSurface = Color(0xFFFFF8E1),
    surfaceVariant = Color(0xFF242424),
    onSurfaceVariant = Color(0xFFBCAAA4),
    surfaceHigh = Color(0xFF333333),
    border = Color(0xFF4A3508),
    borderGlowing = Color(0xFFFFB300),
    textPrimary = Color(0xFFFFF8E1),
    textSecondary = Color(0xFFBCAAA4),
    textMuted = Color(0xFF6D4C41),
    error = Color(0xFFFF3333),
    glowColor = Color(0xFFFFB300),
    scanlineColor = Color(0xFFFFB300),
    isCyberpunk = true,
    template = CyberpunkTemplate.STARK_IND_AMBER
)

// 3. VERONICA (CRIMSON)
val VeronicaCrimsonPalette = CyberPalette(
    primary = Color(0xFFFF0033),
    onPrimary = Color(0xFF0D0D0D),
    primaryContainer = Color(0xFF281014),
    onPrimaryContainer = Color(0xFFFF0033),
    secondary = Color(0xFFFF3366),
    onSecondary = Color(0xFF0D0D0D),
    background = Color(0xFF0D0D0D),
    onBackground = Color(0xFFFFEBEF),
    surface = Color(0xFF1A0C0E),
    onSurface = Color(0xFFFFEBEF),
    surfaceVariant = Color(0xFF281014),
    onSurfaceVariant = Color(0xFFC27B88),
    surfaceHigh = Color(0xFF3D141C),
    border = Color(0xFF59121E),
    borderGlowing = Color(0xFFFF0033),
    textPrimary = Color(0xFFFFEBEF),
    textSecondary = Color(0xFFC27B88),
    textMuted = Color(0xFF6B303B),
    error = Color(0xFFFF0033),
    glowColor = Color(0xFFFF0033),
    scanlineColor = Color(0xFFFF0033),
    isCyberpunk = true,
    template = CyberpunkTemplate.VERONICA_CRIMSON
)

// 4. CYBER MATRIX (EMERALD)
val CyberMatrixEmeraldPalette = CyberPalette(
    primary = Color(0xFF00FF88),
    onPrimary = Color(0xFF0A0F0A),
    primaryContainer = Color(0xFF142914),
    onPrimaryContainer = Color(0xFF00FF88),
    secondary = Color(0xFF00E676),
    onSecondary = Color(0xFF0A0F0A),
    background = Color(0xFF0A0F0A),
    onBackground = Color(0xFFE8F5E9),
    surface = Color(0xFF0E1A0E),
    onSurface = Color(0xFFE8F5E9),
    surfaceVariant = Color(0xFF142914),
    onSurfaceVariant = Color(0xFF66BB6A),
    surfaceHigh = Color(0xFF1C381C),
    border = Color(0xFF184D23),
    borderGlowing = Color(0xFF00FF88),
    textPrimary = Color(0xFFE8F5E9),
    textSecondary = Color(0xFF66BB6A),
    textMuted = Color(0xFF2E7D32),
    error = Color(0xFFFF3333),
    glowColor = Color(0xFF00FF88),
    scanlineColor = Color(0xFF00FF88),
    isCyberpunk = true,
    template = CyberpunkTemplate.CYBER_MATRIX_EMERALD
)

// Default Standard Dark Mode (When Cyberpunk Mode is OFF)
val DefaultCleanPalette = CyberPalette(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF0B111E),
    primaryContainer = Color(0xFF1E293B),
    onPrimaryContainer = Color(0xFF38BDF8),
    secondary = Color(0xFF10B981),
    onSecondary = Color(0xFF0B111E),
    background = Color(0xFF0B111E),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF131D2E),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceHigh = Color(0xFF334155),
    border = Color(0xFF1E293B),
    borderGlowing = Color(0xFF38BDF8),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFF94A3B8),
    textMuted = Color(0xFF64748B),
    error = Color(0xFFEF4444),
    glowColor = Color(0xFF38BDF8),
    scanlineColor = Color.Transparent,
    isCyberpunk = false,
    template = CyberpunkTemplate.JARVIS_CYAN
)

val LocalCyberColors = staticCompositionLocalOf { DefaultCleanPalette }
