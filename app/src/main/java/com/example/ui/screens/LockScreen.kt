package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
import com.example.ui.theme.CyberPurple
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
    var masterPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var usePinPad by remember { mutableStateOf(false) }

    // Lockout countdown calculation
    val isLockedOut = config.lockoutUntil > System.currentTimeMillis()
    var remainingLockoutSec by remember {
        mutableIntStateOf(((config.lockoutUntil - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0))
    }

    LaunchedEffect(config.lockoutUntil) {
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
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Futuristic Crest
        Box(
            modifier = Modifier
                .size(90.dp)
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
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "FORT KNOX",
            color = CyberTextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )

        Text(
            text = if (!config.isInitialized) "INITIALIZE ZERO-KNOWLEDGE MASTER KEY" else "MILITARY-GRADE ENCRYPTED ENCLAVE",
            color = CyberCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (!config.isInitialized) {
            // First time setup mode
            CyberCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = "Create Master Password",
                        color = CyberTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Argon2id KDF will derive a 256-bit Key Encryption Key (KEK). Memorize this password; it cannot be recovered.",
                        color = CyberTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = masterPassword,
                        onValueChange = { masterPassword = it },
                        label = { Text("Master Password") },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = CyberTextMuted
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = CyberBorder,
                            focusedTextColor = CyberTextPrimary,
                            unfocusedTextColor = CyberTextPrimary
                        ),
                        singleLine = true
                    )

                    if (masterPassword.isNotEmpty()) {
                        val analysis = PasswordGenerator.evaluateStrength(masterPassword)
                        Spacer(modifier = Modifier.height(10.dp))
                        PasswordStrengthBar(analysis = analysis)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Master Password") },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = CyberBorder,
                            focusedTextColor = CyberTextPrimary,
                            unfocusedTextColor = CyberTextPrimary
                        ),
                        singleLine = true
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            color = CyberLaserRed,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    CyberButton(
                        text = "INITIALIZE VAULT",
                        onClick = {
                            if (masterPassword.isNotEmpty() && masterPassword == confirmPassword) {
                                onSetupMasterPassword(masterPassword.toCharArray())
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = masterPassword.isNotEmpty() && masterPassword == confirmPassword,
                        isLoading = isLoading
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(androidx.compose.foundation.shape.CutCornerShape(10.dp))
                            .background(CyberCyan.copy(alpha = 0.12f))
                            .border(1.dp, CyberCyan.copy(alpha = 0.5f), androidx.compose.foundation.shape.CutCornerShape(10.dp))
                            .clickable {
                                masterPassword = "1234"
                                confirmPassword = "1234"
                                onSetupMasterPassword("1234".toCharArray())
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⚡ 1-Tap Quick Setup: Demo PIN (1234)",
                            color = CyberCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            // Unlock mode
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
                                text = "Enter Master Key / PIN",
                                color = CyberTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier
                                    .clip(androidx.compose.foundation.shape.CutCornerShape(6.dp))
                                    .background(CyberSurfaceVariant)
                                    .clickable { usePinPad = !usePinPad }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Pin,
                                    contentDescription = null,
                                    tint = if (usePinPad) CyberCyan else CyberTextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (usePinPad) "Scrambled PIN" else "Keyboard",
                                    color = if (usePinPad) CyberCyan else CyberTextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (config.isParanoid2FaEnabled) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(androidx.compose.foundation.shape.CutCornerShape(6.dp))
                                    .background(CyberCyan.copy(alpha = 0.15f))
                                    .border(1.dp, CyberCyan.copy(alpha = 0.4f), androidx.compose.foundation.shape.CutCornerShape(6.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "PARANOID 2FA ACTIVE: Password + Biometrics Required",
                                        color = CyberCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (usePinPad) {
                            // PIN display
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(androidx.compose.foundation.shape.CutCornerShape(8.dp))
                                    .background(CyberSurfaceVariant)
                                    .border(1.dp, CyberBorder, androidx.compose.foundation.shape.CutCornerShape(8.dp)),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (i in 0 until masterPassword.length) {
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(CyberCyan)
                                    )
                                }
                                if (masterPassword.isEmpty()) {
                                    Text(
                                        text = "Touch scrambled keypad below",
                                        color = CyberTextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            ScrambledPinPad(
                                onDigitClick = { digit ->
                                    if (masterPassword.length < 16) {
                                        masterPassword += digit
                                    }
                                },
                                onBackspace = {
                                    if (masterPassword.isNotEmpty()) {
                                        masterPassword = masterPassword.dropLast(1)
                                    }
                                },
                                onShuffle = {}
                            )
                        } else {
                            OutlinedTextField(
                                value = masterPassword,
                                onValueChange = { masterPassword = it },
                                label = { Text("Master Password") },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = CyberTextMuted
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = CyberBorder,
                                    focusedTextColor = CyberTextPrimary,
                                    unfocusedTextColor = CyberTextPrimary
                                ),
                                singleLine = true
                            )
                        }

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage,
                                color = CyberLaserRed,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        if (config.failedAttempts > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Failed attempts: ${config.failedAttempts}/10 (Wipe after 10)",
                                color = if (config.failedAttempts >= 5) CyberLaserRed else CyberGold,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        CyberButton(
                            text = "UNLOCK ENCLAVE",
                            onClick = {
                                if (masterPassword.isNotEmpty()) {
                                    onUnlockWithPassword(masterPassword.toCharArray())
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = masterPassword.isNotEmpty(),
                            isLoading = isLoading
                        )

                        if (com.example.BuildConfig.DEBUG) {
                            Spacer(modifier = Modifier.height(10.dp))
    
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .clip(androidx.compose.foundation.shape.CutCornerShape(8.dp))
                                    .background(CyberCyan.copy(alpha = 0.08f))
                                    .border(1.dp, CyberCyan.copy(alpha = 0.4f), androidx.compose.foundation.shape.CutCornerShape(8.dp))
                                    .clickable {
                                        masterPassword = "1234"
                                        onUnlockWithPassword("1234".toCharArray())
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "⚡ Quick Unlock: Demo PIN (1234)",
                                    color = CyberCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (config.isBiometricEnabled) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(androidx.compose.foundation.shape.CutCornerShape(12.dp))
                                    .background(CyberSurfaceVariant.copy(alpha = 0.5f))
                                    .border(1.dp, CyberEmerald.copy(alpha = 0.6f), androidx.compose.foundation.shape.CutCornerShape(12.dp))
                                    .clickable(onClick = onBiometricClick),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Biometric Unlock",
                                        tint = CyberEmerald,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Biometric Hardware Unlock",
                                        color = CyberEmerald,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Security badge footer
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = CyberEmerald,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "AES-256-GCM • Argon2id 64MB • Offline Enclave",
                color = CyberTextMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
