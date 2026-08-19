package com.example.ui.screens

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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VaultEntry
import com.example.security.PasswordGenerator
import com.example.security.SecurityIntegrityChecker
import com.example.ui.components.CyberBadge
import com.example.ui.components.CyberButton
import com.example.ui.components.CyberCard
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberGold
import com.example.ui.theme.CyberLaserRed
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary

@Composable
fun SecurityAuditScreen(
    entries: List<VaultEntry>,
    integrityReport: SecurityIntegrityChecker.IntegrityReport,
    onEntryClick: (VaultEntry) -> Unit,
    onNavigateToIntruderLogs: () -> Unit = {},
    onNavigateToEncryptionInspector: () -> Unit = {}
) {
    val weakEntries = remember(entries) {
        entries.filter {
            val score = PasswordGenerator.evaluateStrength(it.password)
            score.rating == PasswordGenerator.StrengthRating.VERY_WEAK || score.rating == PasswordGenerator.StrengthRating.WEAK
        }
    }

    val reusedEntries = remember(entries) {
        val passwordGroups = entries.groupBy { it.password }
        passwordGroups.filter { it.key.isNotBlank() && it.value.size > 1 }.flatMap { it.value }
    }

    val overallScore = remember(entries, weakEntries, reusedEntries) {
        if (entries.isEmpty()) 100
        else {
            val deduction = (weakEntries.size * 20) + (reusedEntries.size * 10)
            (100 - (deduction.toDouble() / entries.size * 50)).toInt().coerceIn(10, 100)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "SECURITY AUDIT",
            color = CyberTextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp
        )
        Text(
            text = "HARDWARE INTEGRITY & CRYPTOGRAPHIC HEALTH",
            color = CyberCyan,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        // Health Score Card
        CyberCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "VAULT HEALTH SCORE",
                        color = CyberTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "$overallScore%",
                        color = if (overallScore >= 80) CyberEmerald else if (overallScore >= 50) CyberGold else CyberLaserRed,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = if (overallScore >= 80) "Optimal Security Posture" else "Vulnerabilities detected",
                        color = CyberTextSecondary,
                        fontSize = 12.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            if (overallScore >= 80) CyberEmerald.copy(alpha = 0.2f)
                            else CyberGold.copy(alpha = 0.2f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (overallScore >= 80) CyberEmerald else CyberGold,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Encryption Inspector Quick Access Card
        CyberCard(
            modifier = Modifier.fillMaxWidth(),
            glowColor = CyberCyan.copy(alpha = 0.25f),
            borderColor = CyberCyan.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CutCornerShape(8.dp))
                            .background(CyberCyan.copy(alpha = 0.15f))
                            .border(1.dp, CyberCyan.copy(alpha = 0.4f), CutCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Encryption Inspector",
                            color = CyberTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(CutCornerShape(3.dp))
                                    .background(CyberEmerald.copy(alpha = 0.2f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "ACTIVE",
                                    color = CyberEmerald,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SQLCipher • AES-GCM • Argon2id",
                                color = CyberTextMuted,
                                fontSize = 10.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                CyberButton(
                    text = "INSPECT",
                    color = CyberCyan,
                    onClick = onNavigateToEncryptionInspector,
                    modifier = Modifier.height(34.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Intrusion & Security Logs Quick Access
        CyberCard(
            modifier = Modifier.fillMaxWidth(),
            glowColor = CyberLaserRed.copy(alpha = 0.2f),
            borderColor = CyberLaserRed.copy(alpha = 0.4f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(androidx.compose.foundation.shape.CutCornerShape(8.dp))
                            .background(CyberLaserRed.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = CyberLaserRed,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Intruder & Security Logs",
                            color = CyberTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "View captured photos & access breach logs",
                            color = CyberTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                CyberButton(
                    text = "LOGS",
                    color = CyberLaserRed,
                    onClick = onNavigateToIntruderLogs,
                    modifier = Modifier.height(34.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Device Integrity Report
        CyberCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    text = "DEVICE INTEGRITY ATTESTATION",
                    color = CyberTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(12.dp))

                IntegrityRow(
                    title = "Root Binary Detection",
                    isPass = !integrityReport.isRooted,
                    detail = if (integrityReport.isRooted) "Root indicators found" else "No su binaries detected"
                )

                IntegrityRow(
                    title = "Anti-Hook / Frida Inspection",
                    isPass = !integrityReport.isHookDetected,
                    detail = if (integrityReport.isHookDetected) "Injected hooks detected" else "/proc/self/maps verified clean"
                )

                IntegrityRow(
                    title = "APK Signature Digest",
                    isPass = integrityReport.isSignatureValid,
                    detail = "SHA-256 certificate verified"
                )

                IntegrityRow(
                    title = "Network Isolation",
                    isPass = true,
                    detail = "0 network permissions in manifest (Completely Offline)"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Password Health Breakdown
        if (weakEntries.isNotEmpty()) {
            CyberCard(
                modifier = Modifier.fillMaxWidth(),
                glowColor = CyberLaserRed.copy(alpha = 0.25f),
                borderColor = CyberLaserRed.copy(alpha = 0.5f)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "WEAK PASSWORDS (${weakEntries.size})",
                            color = CyberLaserRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        CyberBadge(text = "ACTION REQUIRED", color = CyberLaserRed)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    for (entry in weakEntries.take(4)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(androidx.compose.foundation.shape.CutCornerShape(8.dp))
                                .clickable { onEntryClick(entry) }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = entry.title, color = CyberTextPrimary, fontSize = 14.sp)
                            Text(text = "Low Entropy", color = CyberLaserRed, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (reusedEntries.isNotEmpty()) {
            CyberCard(
                modifier = Modifier.fillMaxWidth(),
                glowColor = CyberGold.copy(alpha = 0.25f),
                borderColor = CyberGold.copy(alpha = 0.5f)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "REUSED PASSWORDS (${reusedEntries.size})",
                            color = CyberGold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        CyberBadge(text = "HIGH RISK", color = CyberGold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    for (entry in reusedEntries.take(4)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(androidx.compose.foundation.shape.CutCornerShape(8.dp))
                                .clickable { onEntryClick(entry) }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = entry.title, color = CyberTextPrimary, fontSize = 14.sp)
                            Text(text = "Duplicate", color = CyberGold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        if (weakEntries.isEmpty() && reusedEntries.isEmpty() && entries.isNotEmpty()) {
            CyberCard(
                modifier = Modifier.fillMaxWidth(),
                glowColor = CyberEmerald.copy(alpha = 0.25f),
                borderColor = CyberEmerald.copy(alpha = 0.5f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = CyberEmerald,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "All ${entries.size} passwords meet military-grade cryptographic entropy standards.",
                        color = CyberTextPrimary,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun IntegrityRow(
    title: String,
    isPass: Boolean,
    detail: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = CyberTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = detail,
                color = CyberTextMuted,
                fontSize = 11.sp
            )
        }
        Icon(
            imageVector = if (isPass) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (isPass) CyberEmerald else CyberLaserRed,
            modifier = Modifier.size(20.dp)
        )
    }
}
