package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.VaultPreferences
import com.example.security.PasswordGenerator
import com.example.ui.components.CyberButton
import com.example.ui.components.CyberCard
import com.example.ui.components.PasswordStrengthBar
import com.example.ui.components.ScrambledPinPad
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberGold
import com.example.ui.theme.CyberLaserRed
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceHigh
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import kotlinx.coroutines.delay

@Composable
fun LockScreen(
    config: VaultPreferences.VaultConfig,
    onSetupMasterPassword: (CharArray) -> Unit,
    onUnlockWithPassword: (CharArray) -> Unit,
    onBiometricClick: () -> Unit,
    errorMessage: String? = null,
    isLoading: Boolean = false
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = androidx.compose.ui.platform.LocalContext.current

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    // Suppress system IME keyboard at all times on lock screen
    LaunchedEffect(Unit) {
        keyboardController?.hide()
    }

    var enteredPin by remember { mutableStateOf("") }

    // First time setup state (Step 1: Enter, Step 2: Confirm)
    var setupStep by remember { mutableIntStateOf(1) }
    var initialSetupPin by remember { mutableStateOf("") }
    var confirmSetupPin by remember { mutableStateOf("") }
    var setupMismatchError by remember { mutableStateOf<String?>(null) }

    // Lockout countdown calculation
    val isLockedOut = config.lockoutUntil > System.currentTimeMillis()
    var remainingLockoutSec by remember {
        mutableIntStateOf(((config.lockoutUntil - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0))
    }

    LaunchedEffect(config.lockoutUntil) {
        keyboardController?.hide()
        while (config.lockoutUntil > System.currentTimeMillis()) {
            remainingLockoutSec = ((config.lockoutUntil - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
            delay(1000L)
        }
        remainingLockoutSec = 0
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "radarPulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Futuristic Crest
        Box(
            modifier = Modifier
                .size(76.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(CyberCyan.copy(alpha = 0.25f), Color.Transparent)
                    )
                )
                .border(2.dp, CyberCyan, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (!config.isInitialized) Icons.Default.Security else Icons.Default.Lock,
                contentDescription = null,
                tint = CyberCyan,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "FORT KNOX",
            color = CyberTextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )

        Text(
            text = if (!config.isInitialized) "INITIALIZE ZERO-KNOWLEDGE ENCLAVE" else "ANTI-KEYLOGGER SCRAMBLED PIN PAD",
            color = CyberCyan,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (!config.isInitialized) {
            // ==========================================
            // FIRST-TIME INITIALIZATION (CUSTOM KEYPAD)
            // ==========================================
            CyberCard(modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (setupStep == 1) "1. Set Master PIN (4-16 Digits)" else "2. Confirm Master PIN",
                        color = CyberTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = if (setupStep == 1) "Use the secure on-screen keypad to enter your master PIN."
                        else "Re-enter the exact master PIN to verify and derive the Argon2id KEK.",
                        color = CyberTextSecondary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                    val activePin = if (setupStep == 1) initialSetupPin else confirmSetupPin

                    // PIN Masked Display Indicator
                    PinMaskDisplay(pinLength = activePin.length)

                    if (setupStep == 1 && initialSetupPin.isNotEmpty()) {
                        val analysis = PasswordGenerator.evaluateStrength(initialSetupPin)
                        Spacer(modifier = Modifier.height(8.dp))
                        PasswordStrengthBar(analysis = analysis)
                    }

                    if (setupMismatchError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = setupMismatchError!!,
                            color = CyberLaserRed,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = errorMessage,
                            color = CyberLaserRed,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Scrambled Keypad for Setup
                    ScrambledPinPad(
                        onDigitClick = { digit ->
                            if (setupStep == 1) {
                                if (initialSetupPin.length < 16) initialSetupPin += digit
                            } else {
                                if (confirmSetupPin.length < 16) confirmSetupPin += digit
                            }
                            setupMismatchError = null
                        },
                        onBackspace = {
                            if (setupStep == 1) {
                                if (initialSetupPin.isNotEmpty()) initialSetupPin = initialSetupPin.dropLast(1)
                            } else {
                                if (confirmSetupPin.isNotEmpty()) confirmSetupPin = confirmSetupPin.dropLast(1)
                            }
                            setupMismatchError = null
                        },
                        onShuffle = {},
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (setupStep == 2) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(CutCornerShape(8.dp))
                                    .background(CyberSurfaceVariant)
                                    .border(1.dp, CyberBorder, CutCornerShape(8.dp))
                                    .clickable {
                                        setupStep = 1
                                        confirmSetupPin = ""
                                        setupMismatchError = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Back", color = CyberTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        CyberButton(
                            text = if (setupStep == 1) "NEXT: CONFIRM PIN" else "INITIALIZE VAULT",
                            onClick = {
                                if (setupStep == 1) {
                                    if (initialSetupPin.length >= 4) {
                                        setupStep = 2
                                        confirmSetupPin = ""
                                        setupMismatchError = null
                                    } else {
                                        setupMismatchError = "PIN must be at least 4 digits long"
                                    }
                                } else {
                                    if (initialSetupPin == confirmSetupPin) {
                                        if (!com.example.security.SilentCameraCapture.hasCameraPermission(context)) {
                                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                        }
                                        onSetupMasterPassword(confirmSetupPin.toCharArray())
                                    } else {
                                        setupMismatchError = "PINs do not match. Please try again."
                                    }
                                }
                            },
                            modifier = Modifier.weight(if (setupStep == 2) 2f else 1f),
                            enabled = if (setupStep == 1) initialSetupPin.length >= 4 else confirmSetupPin.length >= 4,
                            isLoading = isLoading
                        )
                    }
                }
            }
        } else {
            // ==========================================
            // UNLOCK MODE (100% SCRAMBLED IN-APP KEYPAD)
            // ==========================================
            if (isLockedOut && remainingLockoutSec > 0) {
                CyberCard(
                    modifier = Modifier.fillMaxWidth(),
                    glowColor = CyberLaserRed.copy(alpha = 0.4f),
                    borderColor = CyberLaserRed
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "SECURITY LOCKOUT ACTIVE",
                            color = CyberLaserRed,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Too many failed attempts. Cryptographic enclave locked for $remainingLockoutSec seconds.",
                            color = CyberTextPrimary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                CyberCard(modifier = Modifier.fillMaxWidth()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Enter Master PIN",
                                color = CyberTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )

                            if (enteredPin.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .clip(CutCornerShape(6.dp))
                                        .background(CyberSurfaceVariant)
                                        .clickable { enteredPin = "" }
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Clear",
                                        tint = CyberTextMuted,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Clear",
                                        color = CyberTextMuted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        if (config.isParanoid2FaEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(CutCornerShape(6.dp))
                                    .background(CyberCyan.copy(alpha = 0.15f))
                                    .border(1.dp, CyberCyan.copy(alpha = 0.4f), CutCornerShape(6.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "PARANOID 2FA: PIN + Biometrics Required",
                                        color = CyberCyan,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // PIN Mask Display Chips
                        PinMaskDisplay(pinLength = enteredPin.length)

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage,
                                color = CyberLaserRed,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        if (config.failedAttempts > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Failed attempts: ${config.failedAttempts}/10 (Wipe at 10)",
                                color = if (config.failedAttempts >= 5) CyberLaserRed else CyberGold,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Anti-Keylogger Scrambled Keypad
                        ScrambledPinPad(
                            onDigitClick = { digit ->
                                if (enteredPin.length < 16) {
                                    enteredPin += digit
                                }
                            },
                            onBackspace = {
                                if (enteredPin.isNotEmpty()) {
                                    enteredPin = enteredPin.dropLast(1)
                                }
                            },
                            onShuffle = {},
                            onBiometricClick = if (config.isBiometricEnabled && !config.isParanoid2FaEnabled) onBiometricClick else null,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        CyberButton(
                            text = "UNLOCK ENCLAVE",
                            onClick = {
                                if (enteredPin.isNotEmpty()) {
                                    onUnlockWithPassword(enteredPin.toCharArray())
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = enteredPin.isNotEmpty(),
                            isLoading = isLoading
                        )

                        if (config.isBiometricEnabled && config.isParanoid2FaEnabled) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Dual-Factor: Enter PIN above, biometric prompt will follow automatically.",
                                color = CyberTextMuted,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center
                            )
                        } else if (config.isBiometricEnabled) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clip(CutCornerShape(10.dp))
                                    .background(CyberSurfaceVariant.copy(alpha = 0.5f))
                                    .border(1.dp, CyberEmerald.copy(alpha = 0.6f), CutCornerShape(10.dp))
                                    .clickable(onClick = onBiometricClick),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Biometric Unlock",
                                        tint = CyberEmerald,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Biometric Hardware Unlock",
                                        color = CyberEmerald,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Security badge footer
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = CyberEmerald,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Isolated In-App Keypad • Zero IME Keylogging",
                color = CyberTextMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Custom cyber-styled PIN mask indicator that completely isolates touch input
 * from system accessibility scraping and text prediction engines.
 */
@Composable
private fun PinMaskDisplay(
    pinLength: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(CutCornerShape(8.dp))
            .background(CyberSurfaceVariant)
            .border(1.dp, if (pinLength > 0) CyberCyan.copy(alpha = 0.6f) else CyberBorder, CutCornerShape(8.dp)),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (pinLength == 0) {
            Text(
                text = "Touch scrambled keypad below",
                color = CyberTextMuted,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        } else {
            for (i in 0 until pinLength.coerceAtMost(16)) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(CyberCyan)
                        .border(1.dp, CyberCyan.copy(alpha = 0.8f), CircleShape)
                )
            }
            if (pinLength > 16) {
                Text(
                    text = " +${pinLength - 16}",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
