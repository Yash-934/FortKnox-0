package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.IntrusionLogEntity
import com.example.data.preferences.VaultPreferences
import com.example.security.ScanSummary
import com.example.security.SecurityCheckCategory
import com.example.security.SecurityCheckResult
import com.example.security.SecurityScanner
import com.example.ui.components.CyberButton
import com.example.ui.components.CyberCard
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SecurityScannerScreen(
    scanner: SecurityScanner,
    config: VaultPreferences.VaultConfig,
    intrusionLogs: List<IntrusionLogEntity>,
    onDecryptPhoto: (IntrusionLogEntity) -> ByteArray?,
    onClearLogs: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var scanSummary by remember { mutableStateOf<ScanSummary?>(null) }
    var isScanning by remember { mutableStateOf(false) }
    var expandedCheckId by remember { mutableStateOf<String?>(null) }
    var viewingPhotoLog by remember { mutableStateOf<IntrusionLogEntity?>(null) }

    suspend fun runScan() {
        isScanning = true
        delay(600) // Brief animation
        scanSummary = scanner.performFullScan(config)
        isScanning = false
    }

    LaunchedEffect(Unit) {
        runScan()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
            .padding(16.dp)
    ) {
        if (onBack != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Back",
                        tint = CyberTextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "SECURITY ENCLAVE SCANNER",
                    color = CyberTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Tab Selector: 0: Security Scanner (Score & Checks), 1: Intrusion & Photo Logs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = CyberSurface,
            contentColor = CyberCyan,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = CyberCyan
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(androidx.compose.foundation.shape.CutCornerShape(12.dp))
                .border(1.dp, CyberBorder, androidx.compose.foundation.shape.CutCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Security Score", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Intrusions (${intrusionLogs.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (selectedTab == 0) {
            // TAB 0: SECURITY SCANNER
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    // Header Score Card
                    CyberCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val score = scanSummary?.overallScore ?: 0
                            val animatedScore by animateFloatAsState(
                                targetValue = score.toFloat(),
                                animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                                label = "scoreAnim"
                            )

                            val scoreColor = when {
                                score >= 90 -> CyberEmerald
                                score >= 75 -> CyberCyan
                                score >= 50 -> CyberGold
                                else -> CyberLaserRed
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "SECURITY INTEGRITY SCORE",
                                        color = CyberCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = when {
                                            score >= 95 -> "MILITARY-GRADE AIR-GAPPED"
                                            score >= 85 -> "HARDENED ENCLAVE"
                                            score >= 70 -> "STANDARD PROTECTED"
                                            else -> "AT RISK"
                                        },
                                        color = CyberTextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }

                                CyberButton(
                                    text = if (isScanning) "SCANNING..." else "SCAN NOW",
                                    onClick = {
                                        // Trigger re-scan
                                        scanSummary = null
                                    },
                                    isLoading = isScanning,
                                    modifier = Modifier.height(36.dp)
                                )
                            }

                            LaunchedEffect(scanSummary) {
                                if (scanSummary == null) {
                                    runScan()
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Circular Score Gauge
                            Box(
                                modifier = Modifier
                                    .size(130.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(scoreColor.copy(alpha = 0.2f), Color.Transparent)
                                        )
                                    )
                                    .border(2.dp, scoreColor.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    progress = { animatedScore / 100f },
                                    modifier = Modifier.size(116.dp),
                                    color = scoreColor,
                                    trackColor = CyberSurfaceVariant,
                                    strokeWidth = 8.dp
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${animatedScore.toInt()}",
                                        color = CyberTextPrimary,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = "OUT OF 100",
                                        color = CyberTextMuted,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            val passedCount = scanSummary?.passedCount ?: 0
                            val totalCount = scanSummary?.totalCount ?: 20
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(androidx.compose.foundation.shape.CutCornerShape(8.dp))
                                    .background(CyberSurfaceVariant)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$passedCount / $totalCount Hardening Checks Passed",
                                    color = CyberTextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${((passedCount.toDouble() / totalCount) * 100).toInt()}% Compliant",
                                    color = scoreColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "20-POINT ZERO-KNOWLEDGE & HARDWARE ATTESTATION AUDIT",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                val checks = scanSummary?.results ?: emptyList()
                items(checks, key = { it.id }) { check ->
                    SecurityCheckCard(
                        check = check,
                        isExpanded = expandedCheckId == check.id,
                        onToggleExpand = {
                            expandedCheckId = if (expandedCheckId == check.id) null else check.id
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        } else {
            // TAB 1: INTRUSION & PHOTO LOGS
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "UNAUTHORIZED ACCESS LOGS",
                            color = CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Silent front camera captures & intrusion telemetry",
                            color = CyberTextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    if (intrusionLogs.isNotEmpty()) {
                        IconButton(onClick = onClearLogs) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear All Logs",
                                tint = CyberLaserRed
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (intrusionLogs.isEmpty()) {
                    CyberCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = CyberEmerald,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Zero Security Breaches",
                                color = CyberTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "No wrong password attempts or suspicious activity detected. Front camera will automatically trigger after 3 failed attempts.",
                                color = CyberTextSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(intrusionLogs, key = { it.id }) { log ->
                            IntrusionLogCard(
                                log = log,
                                onViewPhoto = { viewingPhotoLog = log }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }

    // Photo Preview Dialog
    if (viewingPhotoLog != null) {
        val currentLog = viewingPhotoLog!!
        val photoBytes = remember(currentLog) { onDecryptPhoto(currentLog) }
        val bitmap = remember(photoBytes) {
            photoBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
        }

        AlertDialog(
            onDismissRequest = { viewingPhotoLog = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = CyberLaserRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Intruder Photo Capture",
                        color = CyberTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Intruder Photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(androidx.compose.foundation.shape.CutCornerShape(12.dp))
                                .border(1.dp, CyberLaserRed.copy(alpha = 0.6f), androidx.compose.foundation.shape.CutCornerShape(12.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(androidx.compose.foundation.shape.CutCornerShape(12.dp))
                                .background(CyberSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Decrypted Photo Unavailable", color = CyberTextMuted, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    val dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(currentLog.timestamp))
                    Text("Timestamp: $dateFormatted", color = CyberTextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text("Attempt #${currentLog.attemptNumber} • ${currentLog.status}", color = CyberLaserRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Device: ${currentLog.deviceInfo}", color = CyberTextMuted, fontSize = 11.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { viewingPhotoLog = null }) {
                    Text("DISMISS", color = CyberCyan, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = CyberSurfaceHigh
        )
    }
}

@Composable
fun SecurityCheckCard(
    check: SecurityCheckResult,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    CyberCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleExpand)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (check.isPassed) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (check.isPassed) CyberEmerald else CyberLaserRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = check.title,
                            color = CyberTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = check.category.label,
                            color = CyberTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(androidx.compose.foundation.shape.CutCornerShape(4.dp))
                            .background(
                                if (check.isPassed) CyberEmerald.copy(alpha = 0.15f)
                                else CyberLaserRed.copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (check.isPassed) "PASS" else "FAIL",
                            color = if (check.isPassed) CyberEmerald else CyberLaserRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = CyberTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Text(
                        text = check.description,
                        color = CyberTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(androidx.compose.foundation.shape.CutCornerShape(6.dp))
                            .background(CyberSurfaceVariant)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = check.details,
                            color = if (check.isPassed) CyberCyan else CyberLaserRed,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IntrusionLogCard(
    log: IntrusionLogEntity,
    onViewPhoto: () -> Unit
) {
    CyberCard(modifier = Modifier.fillMaxWidth()) {
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
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CyberLaserRed.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (log.photoAvailable) Icons.Default.CameraAlt else Icons.Default.Error,
                        contentDescription = null,
                        tint = CyberLaserRed,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    val dateFormatted = SimpleDateFormat("MMM d, yyyy • HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                    Text(
                        text = "Failed Attempt #${log.attemptNumber}",
                        color = CyberLaserRed,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dateFormatted,
                        color = CyberTextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = log.failureReason,
                        color = CyberTextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            if (log.photoAvailable) {
                Box(
                    modifier = Modifier
                        .clip(androidx.compose.foundation.shape.CutCornerShape(6.dp))
                        .background(CyberLaserRed.copy(alpha = 0.2f))
                        .border(1.dp, CyberLaserRed, androidx.compose.foundation.shape.CutCornerShape(6.dp))
                        .clickable(onClick = onViewPhoto)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = CyberLaserRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "VIEW PHOTO",
                            color = CyberLaserRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
