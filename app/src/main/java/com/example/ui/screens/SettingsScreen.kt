package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.VaultPreferences
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

@Composable
fun SettingsScreen(
    config: VaultPreferences.VaultConfig,
    onSetAutoLockTimeout: (Int) -> Unit,
    onToggleBiometrics: (Boolean) -> Unit,
    onToggleParanoid2Fa: (Boolean) -> Unit = {},
    onToggleSelfDestruct: (Boolean) -> Unit,
    onToggleDisguiseMode: (Boolean) -> Unit,
    onSetDecoyType: (String) -> Unit = {},
    onTogglePrivacyProtection: (Boolean) -> Unit,
    onToggleScreenRecordingDetection: (Boolean) -> Unit = {},
    onSetPhotoTriggerThreshold: (Int) -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToIntruderLogs: () -> Unit = {},
    onNavigateToAbout: () -> Unit,
    onChangeMasterPassword: (CharArray) -> Unit,
    onRotateVaultKey: (CharArray) -> Unit = {},
    onExportBackupToFile: (CharArray, Boolean, Uri) -> Unit = { _, _, _ -> },
    onRestoreBackupFromFile: (Uri, CharArray, Boolean) -> Unit = { _, _, _ -> },
    onLockVault: () -> Unit
) {
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showRotateKeyDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showRestorePasswordDialog by remember { mutableStateOf(false) }
    
    val context = androidx.compose.ui.platform.LocalContext.current

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            android.widget.Toast.makeText(context, "Camera permission granted for intruder capture", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    var pendingExportPassword by remember { mutableStateOf("") }
    var pendingExportDeviceBound by remember { mutableStateOf(false) }
    var selectedRestoreUri by remember { mutableStateOf<Uri?>(null) }

    // SAF Launchers
    // We will use MainActivity.launchLegacyFilePicker instead of ActivityResultContracts to avoid 16-bit crash on certain ROMs.

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "SECURITY SETTINGS",
            color = CyberTextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp
        )
        Text(
            text = "HARDWARE-BACKED VAULT PROTOCOLS (PHASE 3)",
            color = CyberCyan,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        // Security Scanner & Audit Quick Card
        CyberCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(androidx.compose.foundation.shape.CutCornerShape(8.dp))
                            .background(CyberCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Zero-Knowledge Security Score",
                            color = CyberTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "20-Point Multi-Layer Audit & Attestation",
                            color = CyberTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                CyberButton(
                    text = "AUDIT",
                    onClick = onNavigateToScanner,
                    modifier = Modifier.height(34.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stealth & Disguise Mode Card
        CyberCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    text = "STEALTH DISGUISE ENGINE",
                    color = CyberTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Disguise Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Stealth Disguise Mode",
                                color = CyberTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(androidx.compose.foundation.shape.CutCornerShape(4.dp))
                                    .background(CyberGold.copy(alpha = 0.2f))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                Text("COVERT", color = CyberGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(
                            text = "Disguises vault icon & screen as an authentic decoy application.",
                            color = CyberTextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Switch(
                        checked = config.isDisguiseMode,
                        onCheckedChange = { onToggleDisguiseMode(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberBackground,
                            checkedTrackColor = CyberGold,
                            uncheckedTrackColor = CyberSurfaceVariant
                        )
                    )
                }

                if (config.isDisguiseMode) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Decoy Persona Selection:",
                        color = CyberTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isNotes = config.decoyType == "NOTES"
                        val isCalc = config.decoyType == "CALCULATOR"

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(androidx.compose.foundation.shape.CutCornerShape(8.dp))
                                .clickable { onSetDecoyType("NOTES") },
                            color = if (isNotes) CyberGold.copy(alpha = 0.2f) else CyberSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isNotes) CyberGold else CyberBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = if (isNotes) CyberGold else CyberTextMuted, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Notepad", color = if (isNotes) CyberGold else CyberTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(androidx.compose.foundation.shape.CutCornerShape(8.dp))
                                .clickable { onSetDecoyType("CALCULATOR") },
                            color = if (isCalc) CyberGold.copy(alpha = 0.2f) else CyberSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isCalc) CyberGold else CyberBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Calculate, contentDescription = null, tint = if (isCalc) CyberGold else CyberTextMuted, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Calculator", color = if (isCalc) CyberGold else CyberTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Privacy / Screen Recording Protection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Privacy Protection (FLAG_SECURE)",
                            color = CyberTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Blocks screenshots, screen recording, and system app switcher preview.",
                            color = CyberTextMuted,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = config.isPrivacyProtectionEnabled,
                        onCheckedChange = { onTogglePrivacyProtection(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberBackground,
                            checkedTrackColor = CyberCyan,
                            uncheckedTrackColor = CyberSurfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Screen Recording Active Detection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Virtual Display & Screen Recording Detection",
                            color = CyberTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Actively monitors and warns against third-party screen capture sessions.",
                            color = CyberTextMuted,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = config.isScreenRecordingDetectionEnabled,
                        onCheckedChange = { onToggleScreenRecordingDetection(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberBackground,
                            checkedTrackColor = CyberLaserRed,
                            uncheckedTrackColor = CyberSurfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Wrong Password Photo Trigger Threshold
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Intruder Photo Trigger Threshold",
                            color = CyberTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Silently captures camera photo after failed attempts",
                            color = CyberTextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val thresholds = listOf(1, 2, 3, 5)
                    for (t in thresholds) {
                        val isSelected = config.photoTriggerThreshold == t
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(androidx.compose.foundation.shape.CutCornerShape(8.dp))
                                .clickable {
                                    if (androidx.core.content.ContextCompat.checkSelfPermission(
                                            context,
                                            android.Manifest.permission.CAMERA
                                        ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                                    ) {
                                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                    }
                                    onSetPhotoTriggerThreshold(t)
                                },
                            color = if (isSelected) CyberLaserRed.copy(alpha = 0.2f) else CyberSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) CyberLaserRed else CyberBorder
                            )
                        ) {
                            Text(
                                text = "$t Attempt${if (t > 1) "s" else ""}",
                                color = if (isSelected) CyberLaserRed else CyberTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    CyberOutlinedButton(
                        text = "VIEW INTRUDER LOGS & PHOTOS",
                        color = CyberLaserRed,
                        onClick = onNavigateToIntruderLogs,
                        modifier = Modifier.height(34.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Autofill Framework Integration Card
        CyberCard(
            modifier = Modifier.fillMaxWidth(),
            glowColor = CyberCyan.copy(alpha = 0.15f),
            borderColor = CyberCyan.copy(alpha = 0.3f)
        ) {
            Column {
                Text(
                    text = "ANDROID AUTOFILL SERVICE INTEGRATION",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Enable Fort Knox as your system-wide autofill provider to fill credentials in apps and web browsers with hardware encryption security.",
                    color = CyberTextMuted,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    CyberButton(
                        text = "MANAGE AUTOFILL SETTINGS",
                        color = CyberCyan,
                        onClick = {
                            try {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                    val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                } else {
                                    android.widget.Toast.makeText(context, "Autofill requires Android 8.0+", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                try {
                                    // Fallback to system autofill / input settings
                                    val fallbackIntent = android.content.Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS)
                                    context.startActivity(fallbackIntent)
                                } catch (_: Exception) {
                                    android.widget.Toast.makeText(context, "Open Settings > System > Autofill Service", android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.height(34.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Security Configuration Card
        CyberCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    text = "HARDWARE AUTHENTICATION & DEFENSE",
                    color = CyberTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Paranoid 2FA Split Unlock
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Paranoid 2FA Mode",
                                color = CyberTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Box(
                                modifier = Modifier
                                    .clip(androidx.compose.foundation.shape.CutCornerShape(4.dp))
                                    .background(CyberCyan.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "DUAL-FACTOR",
                                    color = CyberCyan,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                        Text(
                            text = "Requires BOTH Master PIN/Password AND Hardware Biometrics to unlock vault.",
                            color = CyberTextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Switch(
                        checked = config.isParanoid2FaEnabled,
                        onCheckedChange = { onToggleParanoid2Fa(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberBackground,
                            checkedTrackColor = CyberCyan,
                            uncheckedTrackColor = CyberSurfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Auto lock timeout selector
                Text(
                    text = "Auto-Lock Timeout",
                    color = CyberTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Locks vault when idle or app is sent to background",
                    color = CyberTextMuted,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val timeouts = listOf(
                        0 to "Instant",
                        30 to "30s",
                        60 to "1m",
                        300 to "5m"
                    )
                    for ((sec, label) in timeouts) {
                        val isSelected = config.autoLockTimeoutSec == sec
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(androidx.compose.foundation.shape.CutCornerShape(8.dp))
                                .clickable { onSetAutoLockTimeout(sec) },
                            color = if (isSelected) CyberCyan.copy(alpha = 0.2f) else CyberSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) CyberCyan else CyberBorder
                            )
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) CyberCyan else CyberTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Biometrics toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Biometric Hardware Unlock",
                            color = CyberTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Requires Keystore biometric enrollment auth",
                            color = CyberTextMuted,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = config.isBiometricEnabled,
                        onCheckedChange = { onToggleBiometrics(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberBackground,
                            checkedTrackColor = CyberEmerald,
                            uncheckedTrackColor = CyberSurfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Self Destruct toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Anti-Brute Force Self Destruct",
                            color = CyberTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Purges entire vault after 10 consecutive failed attempts",
                            color = CyberTextMuted,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = config.selfDestructEnabled,
                        onCheckedChange = { onToggleSelfDestruct(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberBackground,
                            checkedTrackColor = CyberLaserRed,
                            uncheckedTrackColor = CyberSurfaceVariant
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Encrypted Backup & Restore Card
        CyberCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    text = "ENCRYPTED BACKUP & RESTORE",
                    color = CyberTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Zero-Knowledge Export using AES-256-GCM & Argon2id. Includes optional hardware Keystore device binding.",
                    color = CyberTextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CyberOutlinedButton(
                        text = "Export Vault File",
                        icon = Icons.Default.Upload,
                        onClick = { showExportDialog = true },
                        modifier = Modifier.weight(1f)
                    )

                    CyberOutlinedButton(
                        text = "Restore Vault File",
                        icon = Icons.Default.Download,
                        onClick = {
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_GET_CONTENT).apply {
                                    type = "*/*"
                                }
                                (context as com.example.MainActivity).launchLegacyFilePicker(intent) { uri ->
                                    if (uri != null) {
                                        selectedRestoreUri = uri
                                        showRestorePasswordDialog = true
                                    }
                                }
                            } catch (e: Exception) {
                                // Fallback for emulators without file picker
                                val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                                val latestFile = dir?.listFiles { file -> file.name.endsWith(".fortknox") }?.maxByOrNull { it.lastModified() }
                                if (latestFile != null) {
                                    android.widget.Toast.makeText(context, "File picker error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                    selectedRestoreUri = android.net.Uri.fromFile(latestFile)
                                    showRestorePasswordDialog = true
                                } else {
                                    android.widget.Toast.makeText(context, "File picker error: ${e.message}. And no backups in Downloads.", android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Master Password & Key Rotation Card
        CyberCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    text = "KEY MANAGEMENT & CREDENTIAL ACTIONS",
                    color = CyberTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(12.dp))

                CyberOutlinedButton(
                    text = "Rotate Data Encryption Key (DEK)",
                    icon = Icons.Default.Refresh,
                    onClick = { showRotateKeyDialog = true },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                CyberOutlinedButton(
                    text = "Change Master Password",
                    icon = Icons.Default.Key,
                    onClick = { showChangePasswordDialog = true },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                CyberButton(
                    text = "LOCK VAULT IMMEDIATELY",
                    icon = Icons.Default.Lock,
                    onClick = onLockVault,
                    color = CyberCyan,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Information / About Card
        CyberCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    text = "SYSTEM INFORMATION",
                    color = CyberTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(12.dp))

                CyberOutlinedButton(
                    text = "About Fort Knox",
                    icon = Icons.Default.Info,
                    onClick = onNavigateToAbout,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    // Rotate Key Dialog
    if (showRotateKeyDialog) {
        var masterPass by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showRotateKeyDialog = false },
            title = { Text("Rotate Vault Encryption Key", color = CyberTextPrimary) },
            text = {
                Column {
                    Text(
                        "Generates a brand new 256-bit AES-GCM Data Encryption Key (DEK), re-encrypts all database records in place, updates master KEK wrapping, and zero-wipes the old key.",
                        color = CyberTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = masterPass,
                        onValueChange = { masterPass = it },
                        label = { Text("Confirm Master Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (masterPass.isNotEmpty()) {
                            showRotateKeyDialog = false
                            onRotateVaultKey(masterPass.toCharArray())
                        }
                    },
                    enabled = masterPass.isNotEmpty()
                ) {
                    Text("ROTATE KEYS", color = CyberEmerald, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRotateKeyDialog = false }) {
                    Text("CANCEL", color = CyberTextSecondary)
                }
            },
            containerColor = CyberSurface
        )
    }

    // Change Password Dialog
    if (showChangePasswordDialog) {
        var newPass by remember { mutableStateOf("") }
        var confirmPass by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = { Text("Change Master Password", color = CyberTextPrimary) },
            text = {
                Column {
                    Text("Enter new master password. The vault DEK will be re-wrapped using Argon2id.", color = CyberTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = { Text("New Master Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPass,
                        onValueChange = { confirmPass = it },
                        label = { Text("Confirm New Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPass.isNotEmpty() && newPass == confirmPass) {
                            showChangePasswordDialog = false
                            onChangeMasterPassword(newPass.toCharArray())
                        }
                    },
                    enabled = newPass.isNotEmpty() && newPass == confirmPass
                ) {
                    Text("UPDATE", color = CyberCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) {
                    Text("CANCEL", color = CyberTextSecondary)
                }
            },
            containerColor = CyberSurface
        )
    }

    // Export Backup Dialog
    if (showExportDialog) {
        var backupPass by remember { mutableStateOf("") }
        var isDeviceBound by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Encrypted Vault File", color = CyberTextPrimary) },
            text = {
                Column {
                    Text("Enter a strong backup password. This password will encrypt the export file using Argon2id (64MB memory, 3 iterations) + AES-256-GCM without copying any data to clipboard.", color = CyberTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = backupPass,
                        onValueChange = { backupPass = it },
                        label = { Text("Backup Encryption Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { isDeviceBound = !isDeviceBound }
                    ) {
                        Checkbox(
                            checked = isDeviceBound,
                            onCheckedChange = { isDeviceBound = it },
                            colors = CheckboxDefaults.colors(checkedColor = CyberCyan)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text("Device-Bound Hardware Key", color = CyberTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("Binds ciphertext to this device's hardware Keystore StrongBox/TEE", color = CyberTextMuted, fontSize = 10.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (backupPass.isNotEmpty()) {
                            showExportDialog = false
                            pendingExportPassword = backupPass
                            pendingExportDeviceBound = isDeviceBound
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_CREATE_DOCUMENT).apply {
                                    type = "application/octet-stream"
                                    putExtra(android.content.Intent.EXTRA_TITLE, "fortknox_backup_${System.currentTimeMillis()}.fortknox")
                                }
                                (context as com.example.MainActivity).launchLegacyFilePicker(intent) { uri ->
                                    if (uri != null && pendingExportPassword.isNotEmpty()) {
                                        onExportBackupToFile(pendingExportPassword.toCharArray(), pendingExportDeviceBound, uri)
                                        pendingExportPassword = ""
                                    }
                                }
                            } catch (e: Exception) {
                                // Fallback for emulators without file picker
                                val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                                val fallbackFile = java.io.File(dir, "fortknox_backup_${System.currentTimeMillis()}.fortknox")
                                val fallbackUri = android.net.Uri.fromFile(fallbackFile)
                                android.widget.Toast.makeText(context, "File picker error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                onExportBackupToFile(pendingExportPassword.toCharArray(), pendingExportDeviceBound, fallbackUri)
                                pendingExportPassword = ""
                            }
                        }
                    },
                    enabled = backupPass.isNotEmpty()
                ) {
                    Text("SELECT LOCATION & SAVE", color = CyberCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("CANCEL", color = CyberTextSecondary)
                }
            },
            containerColor = CyberSurface
        )
    }

    // Restore Backup Password Dialog (after file is selected via SAF)
    if (showRestorePasswordDialog && selectedRestoreUri != null) {
        var restorePass by remember { mutableStateOf("") }
        var isMerge by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = {
                showRestorePasswordDialog = false
                selectedRestoreUri = null
            },
            title = { Text("Decrypt & Restore Vault File", color = CyberTextPrimary) },
            text = {
                Column {
                    Text(
                        "File selected from storage. Enter the backup password used when this export was created to decrypt credentials.",
                        color = CyberTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = restorePass,
                        onValueChange = { restorePass = it },
                        label = { Text("Backup Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Restore Strategy", color = CyberCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { isMerge = true }.padding(vertical = 4.dp).fillMaxWidth()
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = isMerge,
                            onClick = { isMerge = true },
                            colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = CyberCyan)
                        )
                        Text("Merge (Keep current entries)", color = CyberTextPrimary, fontSize = 13.sp)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { isMerge = false }.padding(vertical = 4.dp).fillMaxWidth()
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = !isMerge,
                            onClick = { isMerge = false },
                            colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = CyberCyan)
                        )
                        Text("Replace (Delete current vault)", color = CyberTextPrimary, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uri = selectedRestoreUri
                        if (uri != null && restorePass.isNotEmpty()) {
                            showRestorePasswordDialog = false
                            onRestoreBackupFromFile(uri, restorePass.toCharArray(), isMerge)
                            selectedRestoreUri = null
                        }
                    },
                    enabled = restorePass.isNotEmpty()
                ) {
                    Text("DECRYPT & RESTORE", color = CyberEmerald, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestorePasswordDialog = false
                    selectedRestoreUri = null
                }) {
                    Text("CANCEL", color = CyberTextSecondary)
                }
            },
            containerColor = CyberSurface
        )
    }
}

