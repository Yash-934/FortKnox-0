package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.IntrusionLogEntity
import com.example.data.preferences.VaultPreferences
import com.example.security.CryptoEngine
import com.example.ui.components.CyberButton
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberOutlinedButton
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogFilter(val label: String) {
    ALL("All Events"),
    PHOTOS("With Photos"),
    LOCKOUTS("Lockouts")
}

@Composable
fun IntruderLogsScreen(
    logs: List<IntrusionLogEntity>,
    config: VaultPreferences.VaultConfig,
    onDecryptPhoto: (IntrusionLogEntity) -> ByteArray?,
    onDeleteLog: (Long) -> Unit,
    onClearAllLogs: () -> Unit,
    onBack: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf(LogFilter.ALL) }
    var viewingPhotoLog by remember { mutableStateOf<IntrusionLogEntity?>(null) }
    var logToDelete by remember { mutableStateOf<IntrusionLogEntity?>(null) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    val filteredLogs = remember(logs, selectedFilter) {
        when (selectedFilter) {
            LogFilter.ALL -> logs
            LogFilter.PHOTOS -> logs.filter { it.photoAvailable }
            LogFilter.LOCKOUTS -> logs.filter { it.attemptNumber >= 5 || it.status.contains("LOCKOUT") || it.status.contains("DESTRUCT") }
        }
    }

    val photoCount = remember(logs) { logs.count { it.photoAvailable } }
    val lockoutCount = remember(logs) { logs.count { it.attemptNumber >= 5 } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CutCornerShape(8.dp))
                    .background(CyberSurfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = CyberTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SECURITY LOGS & INTRUDER SURVEILLANCE",
                    color = CyberTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "ENCRYPTED FORENSIC LOGS • HMAC-SHA256 SEALED",
                    color = CyberLaserRed,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            if (logs.isNotEmpty()) {
                IconButton(
                    onClick = { showClearConfirmDialog = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CutCornerShape(8.dp))
                        .background(CyberLaserRed.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear All Logs",
                        tint = CyberLaserRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Summary Metric Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Incidents Metric
            CyberCard(
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(4.dp)) {
                    Text(
                        text = "INCIDENTS",
                        color = CyberTextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${logs.size}",
                        color = if (logs.isEmpty()) CyberEmerald else CyberLaserRed,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                    Text(
                        text = if (logs.isEmpty()) "No Breaches" else "Recorded Events",
                        color = CyberTextSecondary,
                        fontSize = 10.sp
                    )
                }
            }

            // Photos Captured Metric
            CyberCard(
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(4.dp)) {
                    Text(
                        text = "PHOTOS",
                        color = CyberTextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "$photoCount",
                        color = if (photoCount > 0) CyberLaserRed else CyberCyan,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                    Text(
                        text = "Trigger at ≥${config.photoTriggerThreshold} fails",
                        color = CyberTextSecondary,
                        fontSize = 10.sp
                    )
                }
            }

            // Lockout Incidents Metric
            CyberCard(
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(4.dp)) {
                    Text(
                        text = "LOCKOUTS",
                        color = CyberTextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "$lockoutCount",
                        color = if (lockoutCount > 0) CyberGold else CyberEmerald,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                    Text(
                        text = "Rate-Limit Events",
                        color = CyberTextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter Chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (filter in LogFilter.values()) {
                val isSelected = selectedFilter == filter
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CutCornerShape(6.dp))
                        .clickable { selectedFilter = filter },
                    color = if (isSelected) CyberCyan.copy(alpha = 0.2f) else CyberSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) CyberCyan else CyberBorder
                    )
                ) {
                    Text(
                        text = filter.label,
                        color = if (isSelected) CyberCyan else CyberTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(vertical = 6.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Logs List
        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(CyberEmerald.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = CyberEmerald,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "ENCLAVE INTEGRITY SECURE",
                        color = CyberEmerald,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (logs.isEmpty())
                            "Zero unauthorized access attempts detected. Intruder selfie capture is active (threshold: ${config.photoTriggerThreshold} attempts)."
                        else
                            "No intrusion events matching the selected filter criteria.",
                        color = CyberTextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredLogs, key = { it.id }) { log ->
                    IntruderLogItemCard(
                        log = log,
                        onViewPhoto = { viewingPhotoLog = log },
                        onDelete = { logToDelete = log }
                    )
                }
            }
        }
    }

    // Photo Viewing / Forensic Inspection Dialog
    if (viewingPhotoLog != null) {
        val currentLog = viewingPhotoLog!!
        IntruderPhotoInspectDialog(
            log = currentLog,
            onDecryptPhoto = { onDecryptPhoto(currentLog) },
            onDeleteLog = {
                onDeleteLog(currentLog.id)
                viewingPhotoLog = null
            },
            onDismiss = { viewingPhotoLog = null }
        )
    }

    // Delete Single Log Confirmation Dialog
    if (logToDelete != null) {
        val target = logToDelete!!
        AlertDialog(
            onDismissRequest = { logToDelete = null },
            containerColor = CyberSurface,
            title = {
                Text(
                    text = "PURGE INCIDENT RECORD",
                    color = CyberLaserRed,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Permanently zero-wipe Incident #${target.id} (Attempt #${target.attemptNumber}) and its encrypted photo payload from storage?",
                    color = CyberTextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                CyberButton(
                    text = "PURGE RECORD",
                    color = CyberLaserRed,
                    onClick = {
                        onDeleteLog(target.id)
                        logToDelete = null
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { logToDelete = null }) {
                    Text("CANCEL", color = CyberTextMuted)
                }
            }
        )
    }

    // Clear All Logs Confirmation Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            containerColor = CyberSurface,
            title = {
                Text(
                    text = "PURGE ALL SECURITY LOGS",
                    color = CyberLaserRed,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently erase all ${logs.size} intrusion incident records and cryptographic photo payloads? This action is irreversible.",
                    color = CyberTextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                CyberButton(
                    text = "PURGE ALL",
                    color = CyberLaserRed,
                    onClick = {
                        onClearAllLogs()
                        showClearConfirmDialog = false
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("CANCEL", color = CyberTextMuted)
                }
            }
        )
    }
}

@Composable
fun IntruderLogItemCard(
    log: IntrusionLogEntity,
    onViewPhoto: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormatted = remember(log.timestamp) {
        SimpleDateFormat("MMM d, yyyy • HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
    }

    CyberCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
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
                            .size(38.dp)
                            .clip(CutCornerShape(8.dp))
                            .background(
                                if (log.photoAvailable) CyberLaserRed.copy(alpha = 0.2f)
                                else if (log.attemptNumber >= 5) CyberGold.copy(alpha = 0.2f)
                                else CyberSurfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                log.photoAvailable -> Icons.Default.CameraAlt
                                log.attemptNumber >= 5 -> Icons.Default.LockClock
                                else -> Icons.Default.Warning
                            },
                            contentDescription = null,
                            tint = when {
                                log.photoAvailable -> CyberLaserRed
                                log.attemptNumber >= 5 -> CyberGold
                                else -> CyberTextSecondary
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Attempt #${log.attemptNumber}",
                                color = if (log.attemptNumber >= 3) CyberLaserRed else CyberGold,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CutCornerShape(4.dp))
                                    .background(
                                        if (log.photoAvailable) CyberLaserRed.copy(alpha = 0.15f)
                                        else CyberSurfaceVariant
                                    )
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (log.photoAvailable) "PHOTO CAPTURED" else "LOGGED",
                                    color = if (log.photoAvailable) CyberLaserRed else CyberTextMuted,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = dateFormatted,
                            color = CyberTextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Log",
                        tint = CyberTextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = log.failureReason,
                color = CyberTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            if (log.deviceInfo.isNotBlank()) {
                Text(
                    text = log.deviceInfo,
                    color = CyberTextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            if (log.photoAvailable) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    CyberOutlinedButton(
                        text = "VIEW INTRUDER SELFIE",
                        color = CyberLaserRed,
                        onClick = onViewPhoto,
                        modifier = Modifier.height(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun IntruderPhotoInspectDialog(
    log: IntrusionLogEntity,
    onDecryptPhoto: () -> ByteArray?,
    onDeleteLog: () -> Unit,
    onDismiss: () -> Unit
) {
    var decryptedBytes by remember { mutableStateOf<ByteArray?>(null) }
    var isDecrypting by remember { mutableStateOf(true) }
    var decryptFailed by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(log) {
        isDecrypting = true
        val bytes = onDecryptPhoto()
        if (bytes != null && bytes.isNotEmpty()) {
            decryptedBytes = bytes
            decryptFailed = false
        } else {
            decryptFailed = true
        }
        isDecrypting = false
    }

    val bitmap = remember(decryptedBytes) {
        decryptedBytes?.let { bytes ->
            try {
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (e: Exception) {
                null
            }
        }
    }

    val dateFormatted = remember(log.timestamp) {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
    }

    Dialog(
        onDismissRequest = {
            decryptedBytes?.let { CryptoEngine.wipe(it) }
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            CyberCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = CyberLaserRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "INTRUDER FORENSIC SELFIE",
                                    color = CyberLaserRed,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "Attempt #${log.attemptNumber} • $dateFormatted",
                                    color = CyberTextMuted,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                decryptedBytes?.let { CryptoEngine.wipe(it) }
                                onDismiss()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = CyberTextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Photo Viewer Frame
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f)
                            .clip(CutCornerShape(10.dp))
                            .background(CyberBackground)
                            .border(1.5.dp, CyberLaserRed, CutCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDecrypting) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = CyberLaserRed, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Hardware Keystore Decryption...",
                                    color = CyberTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        } else if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Captured Intruder Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            // High-tech forensic scan line overlay badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(8.dp)
                                    .clip(CutCornerShape(4.dp))
                                    .background(Color.Black.copy(alpha = 0.7f))
                                    .border(1.dp, CyberLaserRed.copy(alpha = 0.5f), CutCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "LIVE FORENSIC FRAME [AES-GCM]",
                                    color = CyberLaserRed,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = CyberLaserRed, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Failed to decrypt photo payload",
                                    color = CyberLaserRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Keystore key unavailable or corrupted buffer",
                                    color = CyberTextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Forensic Audit Metadata Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CutCornerShape(8.dp))
                            .background(CyberSurfaceVariant)
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "INCIDENT AUDIT TELEMETRY",
                                color = CyberCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Reason: ${log.failureReason}",
                                color = CyberTextSecondary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Device: ${log.deviceInfo}",
                                color = CyberTextMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Integrity Status: ${log.status}",
                                color = CyberEmerald,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CyberOutlinedButton(
                            text = "PURGE INCIDENT",
                            color = CyberLaserRed,
                            onClick = onDeleteLog,
                            modifier = Modifier.weight(1f)
                        )
                        CyberButton(
                            text = "DONE",
                            onClick = {
                                decryptedBytes?.let { CryptoEngine.wipe(it) }
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
