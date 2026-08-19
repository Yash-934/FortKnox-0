package com.example.ui.screens

import android.app.Activity
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.ComponentInspection
import com.example.security.EncryptionInspectorEngine
import com.example.security.ParsedBackupHeader
import com.example.security.SelfTestReport
import com.example.ui.components.CyberBadge
import com.example.ui.components.CyberButton
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberGridBackground
import com.example.ui.components.CyberOutlinedButton
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EncryptionInspectorScreen(
    inspectorEngine: EncryptionInspectorEngine,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Mandated Security Constraint: Always enforce FLAG_SECURE on Encryption Inspector
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            // Keep window flags managed by app lifecycle
        }
    }

    var inspections by remember { mutableStateOf<List<ComponentInspection>>(emptyList()) }
    var selfTestReport by remember { mutableStateOf<SelfTestReport?>(null) }
    var isRunningSelfTest by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var lastVerifiedTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }

    var expandedCardId by remember { mutableStateOf<String?>(null) }
    var parsedBackupHeader by remember { mutableStateOf<ParsedBackupHeader?>(null) }
    var isInspectingBackupFile by remember { mutableStateOf(false) }

    fun refreshInspections() {
        scope.launch {
            isRefreshing = true
            inspections = inspectorEngine.inspectAllComponents()
            lastVerifiedTimestamp = System.currentTimeMillis()
            isRefreshing = false
        }
    }

    fun runSelfTest() {
        scope.launch {
            isRunningSelfTest = true
            delay(400) // Visual feedback
            val report = inspectorEngine.performDynamicSelfTest()
            selfTestReport = report
            lastVerifiedTimestamp = System.currentTimeMillis()
            isRunningSelfTest = false
        }
    }

    LaunchedEffect(Unit) {
        refreshInspections()
        runSelfTest()
    }

    // SAF Picker to inspect external or exported encrypted backup headers
    val backupPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                isInspectingBackupFile = true
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val content = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                    val header = inspectorEngine.parseBackupHeader(content)
                    withContext(Dispatchers.Main) {
                        parsedBackupHeader = header
                        expandedCardId = "BACKUPS"
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        parsedBackupHeader = ParsedBackupHeader(
                            formatVersion = 0,
                            kdfAlgorithm = "Error",
                            iterations = 0,
                            memoryKb = 0,
                            saltLengthBytes = 0,
                            nonceLengthBytes = 0,
                            ciphertextLengthBytes = 0,
                            isDeviceBound = false,
                            deviceIvPresent = false,
                            backupTimestamp = 0L,
                            appIdentifier = "Unknown",
                            isValidFormat = false,
                            errorMessage = "Failed to read backup file: ${e.message}"
                        )
                    }
                } finally {
                    withContext(Dispatchers.Main) {
                        isInspectingBackupFile = false
                    }
                }
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alphaPulse"
    )

    val dateFormat = remember { SimpleDateFormat("HH:mm:ss • MMM dd, yyyy", Locale.US) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
    ) {
        CyberGridBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CutCornerShape(8.dp))
                        .background(CyberSurfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "ENCRYPTION INSPECTOR",
                        color = CyberTextPrimary,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                        maxLines = 1
                    )
                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CutCornerShape(4.dp))
                                .background(CyberEmerald.copy(alpha = 0.2f))
                                .border(1.dp, CyberEmerald.copy(alpha = 0.4f), CutCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "AIR-GAP",
                                color = CyberEmerald,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ZERO-KNOWLEDGE SUBSYSTEM",
                            color = CyberCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        refreshInspections()
                        runSelfTest()
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CutCornerShape(8.dp))
                        .background(CyberSurfaceVariant)
                ) {
                    if (isRefreshing || isRunningSelfTest) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = CyberCyan,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Scan & Verify",
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Timestamp Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                color = CyberSurface.copy(alpha = 0.7f),
                shape = CutCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(CyberEmerald)
                                .alpha(pulseAlpha)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Verified: ${dateFormat.format(Date(lastVerifiedTimestamp))}",
                            color = CyberTextSecondary,
                            fontSize = 10.5.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Box(
                        modifier = Modifier
                            .clip(CutCornerShape(4.dp))
                            .background(CyberEmerald.copy(alpha = 0.15f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "FLAG_SECURE",
                            color = CyberEmerald,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Hero Self-Test Dashboard Card
            CyberCard(
                modifier = Modifier.fillMaxWidth(),
                glowColor = CyberCyan.copy(alpha = 0.25f),
                borderColor = CyberCyan.copy(alpha = 0.5f)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CRYPTOGRAPHIC ENGINE STATUS",
                                color = CyberTextSecondary,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1
                            )
                            Text(
                                text = if (selfTestReport?.passedTests == selfTestReport?.totalTests) "100% OPERATIONAL" else "SELF-TEST ACTIVE",
                                color = CyberEmerald,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp,
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(CyberEmerald.copy(alpha = 0.15f))
                                .border(1.dp, CyberEmerald.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Shield Active",
                                tint = CyberEmerald,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Real-time non-destructive self-test: Generates random CSPRNG 256-byte payload, verifies AES-256-GCM authenticated nonces, asserts RFC 9106 Argon2id memory hardness, and tests HMAC-SHA256 tamper seals with zero plaintext or key exposure.",
                        color = CyberTextMuted,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Self Test Button
                    CyberButton(
                        text = if (isRunningSelfTest) "EXECUTING SELF-TEST..." else "RUN ENCRYPTION SELF-TEST",
                        color = CyberCyan,
                        onClick = { runSelfTest() },
                        enabled = !isRunningSelfTest,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Self Test Results List
                    if (selfTestReport != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CutCornerShape(8.dp))
                                .background(CyberSurfaceHigh)
                                .border(1.dp, CyberBorder, CutCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "BENCHMARK RESULTS (${selfTestReport!!.passedTests}/${selfTestReport!!.totalTests} PASSED)",
                                    color = CyberCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "${selfTestReport!!.executionTimeMs} ms total",
                                    color = CyberTextMuted,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            selfTestReport!!.items.forEach { test ->
                                SelfTestItemRow(test = test)
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "SUBSYSTEM ENCRYPTION ARCHITECTURE",
                color = CyberTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp
            )
            Text(
                text = "DETAILED HARDWARE & CIPHER SPECIFICATIONS",
                color = CyberCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Render all 6 Component Cards
            inspections.forEach { component ->
                val icon = when (component.id) {
                    "DATABASE" -> Icons.Default.Storage
                    "VAULT_ENTRIES" -> Icons.Default.Key
                    "INTRUSION_LOGS" -> Icons.Default.PhotoCamera
                    "AUTOFILL" -> Icons.Default.AutoAwesome
                    "BACKUPS" -> Icons.Default.Download
                    "CLIPBOARD" -> Icons.Default.ContentPaste
                    else -> Icons.Default.Shield
                }

                ComponentInspectionCard(
                    component = component,
                    icon = icon,
                    isExpanded = expandedCardId == component.id,
                    onToggleExpand = {
                        expandedCardId = if (expandedCardId == component.id) null else component.id
                    },
                    onInspectBackupFile = if (component.id == "BACKUPS") {
                        { backupPickerLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*")) }
                    } else null,
                    parsedBackupHeader = if (component.id == "BACKUPS") parsedBackupHeader else null,
                    isInspectingFile = if (component.id == "BACKUPS") isInspectingBackupFile else false
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SelfTestItemRow(test: com.example.security.SelfTestItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = if (test.isPassed) Icons.Default.CheckCircle else Icons.Default.Close,
                contentDescription = null,
                tint = if (test.isPassed) CyberEmerald else CyberLaserRed,
                modifier = Modifier
                    .size(16.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = test.testName,
                        color = CyberTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${test.durationMs}ms",
                        color = CyberTextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = test.technicalMetric,
                    color = CyberTextSecondary,
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        CyberBadge(
            text = if (test.isPassed) "PASS" else "FAIL",
            color = if (test.isPassed) CyberEmerald else CyberLaserRed
        )
    }
}

@Composable
private fun ComponentInspectionCard(
    component: ComponentInspection,
    icon: ImageVector,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onInspectBackupFile: (() -> Unit)? = null,
    parsedBackupHeader: ParsedBackupHeader? = null,
    isInspectingFile: Boolean = false
) {
    CyberCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        glowColor = CyberEmerald.copy(alpha = 0.15f),
        borderColor = if (isExpanded) CyberCyan.copy(alpha = 0.6f) else CyberBorder
    ) {
        Column {
            // Main Summary Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
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
                            .background(CyberEmerald.copy(alpha = 0.15f))
                            .border(1.dp, CyberEmerald.copy(alpha = 0.4f), CutCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = CyberEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = component.name,
                                color = CyberTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Active",
                                tint = CyberEmerald,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Text(
                            text = component.algorithm,
                            color = CyberCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    CyberBadge(
                        text = component.status,
                        color = CyberEmerald
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = CyberTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Verification Summary
            Text(
                text = component.verificationSummary,
                color = CyberTextSecondary,
                fontSize = 11.5.sp,
                lineHeight = 16.sp
            )

            // Collapsible Details
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(CyberBorder)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Spec Grid
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CutCornerShape(6.dp))
                            .background(CyberSurfaceHigh)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "SECURITY METADATA & CIPHER PARAMETERS",
                            color = CyberCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (component.keySizeBits > 0) {
                            DetailSpecRow(label = "Key Size / Strength", value = "${component.keySizeBits}-bit AES Key")
                        }
                        if (component.ivSizeBytes > 0) {
                            DetailSpecRow(label = "Initialization Vector (IV)", value = "${component.ivSizeBytes} Bytes (${component.ivSizeBytes * 8}-bit unique nonce)")
                        }
                        if (component.authTagSizeBytes > 0) {
                            DetailSpecRow(label = "Authentication Tag", value = "${component.authTagSizeBytes} Bytes (${component.authTagSizeBytes * 8}-bit integrity tag)")
                        }
                        DetailSpecRow(label = "Key Derivation Function", value = component.kdfDetails)
                        DetailSpecRow(label = "Key Protection Enclave", value = component.keyProtection)

                        component.metadata.forEach { (k, v) ->
                            DetailSpecRow(label = k, value = v)
                        }
                    }

                    // Optional Interactive Backup File Inspector inside Backups Card
                    if (onInspectBackupFile != null) {
                        Spacer(modifier = Modifier.height(12.dp))

                        CyberCard(
                            modifier = Modifier.fillMaxWidth(),
                            glowColor = CyberPurple.copy(alpha = 0.2f),
                            borderColor = CyberPurple.copy(alpha = 0.4f)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "BACKUP HEADER INSPECTOR",
                                            color = CyberTextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Select a backup file to inspect envelope parameters (0 password required)",
                                            color = CyberTextMuted,
                                            fontSize = 10.5.sp
                                        )
                                    }

                                    CyberOutlinedButton(
                                        text = if (isInspectingFile) "PARSING..." else "SELECT FILE",
                                        color = CyberPurple,
                                        onClick = onInspectBackupFile,
                                        modifier = Modifier.height(32.dp)
                                    )
                                }

                                if (parsedBackupHeader != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(CutCornerShape(6.dp))
                                            .background(CyberSurface)
                                            .border(1.dp, CyberBorder, CutCornerShape(6.dp))
                                            .padding(10.dp)
                                    ) {
                                        if (parsedBackupHeader.isValidFormat) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "PARSED BACKUP ENVELOPE (V${parsedBackupHeader.formatVersion})",
                                                    color = CyberEmerald,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                CyberBadge(text = "VALID ENVELOPE", color = CyberEmerald)
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))
                                            DetailSpecRow(label = "KDF Algorithm", value = parsedBackupHeader.kdfAlgorithm.uppercase(Locale.US))
                                            DetailSpecRow(label = "Argon2id Memory Cost", value = "${parsedBackupHeader.memoryKb} KiB (${parsedBackupHeader.memoryKb / 1024} MiB)")
                                            DetailSpecRow(label = "Argon2id Iterations", value = "${parsedBackupHeader.iterations} passes")
                                            DetailSpecRow(label = "Salt Byte Length", value = "${parsedBackupHeader.saltLengthBytes} bytes (${parsedBackupHeader.saltLengthBytes * 8}-bit)")
                                            DetailSpecRow(label = "Nonce (IV) Length", value = "${parsedBackupHeader.nonceLengthBytes} bytes (${parsedBackupHeader.nonceLengthBytes * 8}-bit)")
                                            DetailSpecRow(label = "Ciphertext Payload Size", value = "${parsedBackupHeader.ciphertextLengthBytes} bytes")
                                            DetailSpecRow(label = "Hardware Device Bound", value = if (parsedBackupHeader.isDeviceBound) "YES (Encapsulated in Keystore Key)" else "NO (Portable Backup)")
                                            DetailSpecRow(label = "Timestamp", value = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(parsedBackupHeader.backupTimestamp)))
                                        } else {
                                            Text(
                                                text = "PARSING FAILED: ${parsedBackupHeader.errorMessage}",
                                                color = CyberLaserRed,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailSpecRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = CyberTextSecondary,
            fontSize = 11.sp
        )
        Text(
            text = value,
            color = CyberTextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
    }
}
