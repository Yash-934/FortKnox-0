package com.example.ui.screens

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.data.model.DecoyNoteEntity
import com.example.data.model.VaultEntry
import com.example.data.preferences.VaultPreferences
import com.example.data.repository.VaultRepository
import com.example.lifecycle.ClipboardCleaner
import com.example.security.CryptoEngine
import com.example.security.EncryptedBackupManager
import com.example.security.KeystoreManager
import com.example.security.SecurityIntegrityChecker
import com.example.security.SecurityScanner
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

enum class AppTab(val label: String, val icon: ImageVector) {
    VAULT("Vault", Icons.Default.Lock),
    GENERATOR("Generator", Icons.Default.AutoAwesome),
    AUDIT("Audit", Icons.Default.Shield),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun MainAppScreen(
    repository: VaultRepository,
    integrityReport: SecurityIntegrityChecker.IntegrityReport
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val isUnlocked by repository.isUnlocked.collectAsState()
    val entries by repository.entriesFlow.collectAsState(initial = emptyList())
    val intrusionLogs by repository.intrusionLogsFlow.collectAsState(initial = emptyList())
    val decoyNotes by repository.decoyNotesFlow.collectAsState(initial = emptyList())
    val config by repository.preferences.configFlow.collectAsState(
        initial = VaultPreferences.VaultConfig(
            isInitialized = false,
            masterSalt = null,
            wrappedDek = null,
            wrappedDekIv = null,
            biometricWrappedDek = null,
            biometricWrappedDekIv = null,
            isBiometricEnabled = false,
            autoLockTimeoutSec = 60,
            selfDestructEnabled = true,
            failedAttempts = 0,
            lockoutUntil = 0L,
            isDisguiseMode = false,
            isPrivacyProtectionEnabled = false,
            photoTriggerThreshold = 3
        )
    )

    var currentTab by remember { mutableStateOf(AppTab.VAULT) }
    var editingEntry by remember { mutableStateOf<VaultEntry?>(null) }
    var isAddingEntry by remember { mutableStateOf(false) }
    var unlockErrorMessage by remember { mutableStateOf<String?>(null) }
    var isCryptoBusy by remember { mutableStateOf(false) }
    var rootWarningDismissed by remember { mutableStateOf(false) }
    var showScannerScreen by remember { mutableStateOf(false) }
    var showIntruderLogsScreen by remember { mutableStateOf(false) }
    var showAboutScreen by remember { mutableStateOf(false) }
    var isDecoyModeActive by remember { mutableStateOf(false) }

    val securityScanner = remember { SecurityScanner(context, repository.preferences) }

    // If disguise mode is enabled in preferences, start in decoy mode when vault is locked
    LaunchedEffect(config.isDisguiseMode) {
        if (config.isDisguiseMode && !isUnlocked) {
            isDecoyModeActive = true
        }
    }

    // Seed decoy notes if empty
    LaunchedEffect(Unit) {
        repository.seedInitialDecoyNotesIfEmpty()
    }

    // Dynamic screenshot protection (FLAG_SECURE)
    DisposableEffect(config.isPrivacyProtectionEnabled, isUnlocked) {
        val window = (context as? Activity)?.window
        if (window != null) {
            if (!com.example.BuildConfig.DEBUG && (config.isPrivacyProtectionEnabled || !isUnlocked)) {
                window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
        onDispose {}
    }

    fun showToast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    fun setDisguiseMode(enabled: Boolean, decoyType: String = config.decoyType) {
        scope.launch {
            repository.preferences.setDisguiseMode(enabled)
            try {
                val pm = context.packageManager
                val mainComp = ComponentName(context, "com.example.MainActivity")
                val notesComp = ComponentName(context, "com.example.DecoyNotesAlias")
                val calcComp = ComponentName(context, "com.example.DecoyCalculatorAlias")

                if (enabled) {
                    if (decoyType == "CALCULATOR") {
                        pm.setComponentEnabledSetting(calcComp, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
                        pm.setComponentEnabledSetting(notesComp, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
                    } else {
                        pm.setComponentEnabledSetting(notesComp, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
                        pm.setComponentEnabledSetting(calcComp, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
                    }
                    pm.setComponentEnabledSetting(mainComp, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
                    showToast("Stealth Mode enabled: App disguised as $decoyType")
                } else {
                    pm.setComponentEnabledSetting(mainComp, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
                    pm.setComponentEnabledSetting(notesComp, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
                    pm.setComponentEnabledSetting(calcComp, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
                    showToast("Disguise Mode disabled: Default Fort Knox icon restored")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun handleCopy(label: String, text: String) {
        ClipboardCleaner.copyToClipboardWithAutoClear(
            context = context,
            label = label,
            text = text,
            scope = scope
        )
        scope.launch {
            snackbarHostState.showSnackbar("Copied to clipboard (auto-wiping in 30s)")
        }
    }

    fun prompt2FaBiometricUnlock() {
        val activity = context as? FragmentActivity ?: return
        val executor = Executors.newSingleThreadExecutor()

        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    scope.launch {
                        val res = repository.complete2FaUnlock()
                        if (res.isSuccess) {
                            unlockErrorMessage = null
                        } else {
                            unlockErrorMessage = "2FA Error: ${res.exceptionOrNull()?.message ?: "Dual factor verification failed"}"
                        }
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    scope.launch {
                        repository.cancel2FaUnlock()
                        unlockErrorMessage = "2FA Incomplete: Biometric authentication cancelled ($errString)"
                    }
                }

                override fun onAuthenticationFailed() {
                    // Biometric sensor failed match
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("2FA Dual-Factor Verification (2/2)")
            .setSubtitle("Master PIN verified. Confirm hardware biometric to unlock vault.")
            .setNegativeButtonText("Cancel")
            .setConfirmationRequired(true)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        prompt.authenticate(promptInfo)
    }

    fun promptBiometricUnlock() {
        if (config.isParanoid2FaEnabled) {
            unlockErrorMessage = "Paranoid 2FA Active: Please enter Master PIN/Password first to initiate dual authentication."
            showToast("2FA Active: Enter Master PIN first")
            return
        }

        val activity = context as? FragmentActivity ?: return
        val ivBase64 = config.biometricWrappedDekIv ?: run {
            showToast("Biometric key not configured")
            return
        }
        val ivBytes = android.util.Base64.decode(ivBase64, android.util.Base64.DEFAULT)
        val cipher = KeystoreManager.initBiometricDecryptionCipher(ivBytes) ?: run {
            showToast("Biometric key invalidated or unavailable")
            return
        }

        val executor = Executors.newSingleThreadExecutor()
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    val authCipher = result.cryptoObject?.cipher ?: return
                    scope.launch {
                        val res = repository.unlockWithBiometrics(authCipher)
                        if (res.isFailure) {
                            unlockErrorMessage = "Biometric decryption failed"
                        }
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    unlockErrorMessage = errString.toString()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Fort Knox Biometric Unlock")
            .setSubtitle("Authenticate hardware enclave")
            .setNegativeButtonText("Use Master Password")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        prompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
    }

    fun promptBiometricEnable() {
        val activity = context as? FragmentActivity ?: return
        val cipher = KeystoreManager.initBiometricEncryptionCipher(context) ?: run {
            showToast("Hardware Keystore StrongBox not available")
            return
        }

        val executor = Executors.newSingleThreadExecutor()
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    val authCipher = result.cryptoObject?.cipher ?: return
                    scope.launch {
                        val success = repository.enableBiometricUnlock(authCipher)
                        if (success) {
                            showToast("Biometric hardware unlock configured")
                        }
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    showToast("Biometric auth cancelled: $errString")
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Enable Biometric Unlock")
            .setSubtitle("Authorize hardware Keystore encryption")
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        prompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
    }

    if (integrityReport.isRooted && !rootWarningDismissed) {
        RootWarningScreen(
            reasons = integrityReport.rootReasons,
            onDismiss = { rootWarningDismissed = true }
        )
        return
    }

    // Decoy Mode screen if active (Supports both Notes & Calculator)
    if (isDecoyModeActive) {
        if (config.decoyType == "CALCULATOR") {
            DecoyCalculatorScreen(
                onTriggerSecretVault = {
                    isDecoyModeActive = false
                    showToast("⚡ Master Enclave Disengaged from Stealth Calculator")
                }
            )
        } else {
            DecoyNotesScreen(
                notes = decoyNotes,
                onSaveNote = { note ->
                    scope.launch { repository.saveDecoyNote(note) }
                },
                onDeleteNote = { id ->
                    scope.launch { repository.deleteDecoyNote(id) }
                },
                onTriggerSecretVault = {
                    isDecoyModeActive = false
                    showToast("⚡ Master Enclave Disengaged from Stealth Notes")
                }
            )
        }
        return
    }

    // Back navigation handling
    BackHandler(enabled = isAddingEntry || editingEntry != null) {
        isAddingEntry = false
        editingEntry = null
    }

    BackHandler(enabled = showScannerScreen) {
        showScannerScreen = false
    }

    BackHandler(enabled = showAboutScreen) {
        showAboutScreen = false
    }

    BackHandler(enabled = isUnlocked && currentTab != AppTab.VAULT && !isAddingEntry && editingEntry == null && !showScannerScreen && !showAboutScreen) {
        currentTab = AppTab.VAULT
    }

    if (!isUnlocked) {
        LockScreen(
            config = config,
            onSetupMasterPassword = { pwd ->
                isCryptoBusy = true
                scope.launch {
                    val ok = repository.setupMasterPassword(pwd)
                    isCryptoBusy = false
                    if (!ok) {
                        unlockErrorMessage = "Failed to derive Argon2id master key"
                    }
                }
            },
            onUnlockWithPassword = { pwd ->
                isCryptoBusy = true
                unlockErrorMessage = null
                scope.launch {
                    if (config.isParanoid2FaEnabled) {
                        val factor1Res = repository.validateMasterPasswordFirstFactor(pwd)
                        isCryptoBusy = false
                        if (factor1Res.isSuccess) {
                            prompt2FaBiometricUnlock()
                        } else {
                            unlockErrorMessage = factor1Res.exceptionOrNull()?.message ?: "Invalid master password / PIN (Factor 1 failed)"
                        }
                    } else {
                        val res = repository.unlockWithMasterPassword(pwd)
                        isCryptoBusy = false
                        if (res.isFailure) {
                            unlockErrorMessage = res.exceptionOrNull()?.message ?: "Invalid master password"
                        }
                    }
                }
            },
            onBiometricClick = { promptBiometricUnlock() },
            errorMessage = unlockErrorMessage,
            isLoading = isCryptoBusy
        )
        return
    }

    if (showScannerScreen) {
        SecurityScannerScreen(
            scanner = securityScanner,
            config = config,
            intrusionLogs = intrusionLogs,
            onDecryptPhoto = { repository.decryptPhotoBytes(it) },
            onClearLogs = { scope.launch { repository.clearIntrusionLogs() } },
            onBack = { showScannerScreen = false }
        )
        return
    }

    if (showIntruderLogsScreen) {
        IntruderLogsScreen(
            logs = intrusionLogs,
            config = config,
            onDecryptPhoto = { repository.decryptPhotoBytes(it) },
            onDeleteLog = { id -> scope.launch { repository.deleteIntrusionLog(id) } },
            onClearAllLogs = { scope.launch { repository.clearIntrusionLogs() } },
            onBack = { showIntruderLogsScreen = false }
        )
        return
    }

    if (showAboutScreen) {
        AboutScreen(onBack = { showAboutScreen = false })
        return
    }

    if (isAddingEntry || editingEntry != null) {
        AddEditEntryScreen(
            entryToEdit = editingEntry,
            onSave = { entry ->
                scope.launch {
                    repository.saveEntry(entry)
                    isAddingEntry = false
                    editingEntry = null
                    snackbarHostState.showSnackbar("Entry encrypted and saved")
                }
            },
            onDelete = { id ->
                scope.launch {
                    repository.deleteEntry(id)
                    isAddingEntry = false
                    editingEntry = null
                    snackbarHostState.showSnackbar("Entry permanently removed")
                }
            },
            onBack = {
                isAddingEntry = false
                editingEntry = null
            },
            onCopyUsername = { handleCopy("Username", it) },
            onCopyPassword = { handleCopy("Password", it) }
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = CyberSurface,
                modifier = Modifier
                    .clip(androidx.compose.foundation.shape.CutCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                for (tab in AppTab.values()) {
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyberCyan,
                            selectedTextColor = CyberCyan,
                            unselectedIconColor = CyberTextMuted,
                            unselectedTextColor = CyberTextMuted,
                            indicatorColor = CyberCyan.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        },
        containerColor = CyberBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            com.example.ui.components.CyberGridBackground()
            
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tabTransition"
            ) { tab ->
                when (tab) {
                    AppTab.VAULT -> {
                        VaultListScreen(
                            entries = entries,
                            onEntryClick = { editingEntry = it },
                            onAddClick = { isAddingEntry = true },
                            onLockVault = { repository.lockVault() },
                            onCopyUsername = { handleCopy("Username", it) },
                            onCopyPassword = { handleCopy("Password", it) },
                            onToggleFavorite = { entry ->
                                scope.launch {
                                    repository.saveEntry(entry.copy(isFavorite = !entry.isFavorite))
                                }
                            }
                        )
                    }

                    AppTab.GENERATOR -> {
                        PasswordGeneratorScreen(
                            onCopyPassword = { handleCopy("Password", it) }
                        )
                    }

                    AppTab.AUDIT -> {
                        SecurityAuditScreen(
                            entries = entries,
                            integrityReport = integrityReport,
                            onEntryClick = { editingEntry = it },
                            onNavigateToIntruderLogs = { showIntruderLogsScreen = true }
                        )
                    }

                    AppTab.SETTINGS -> {
                        SettingsScreen(
                            config = config,
                            onSetAutoLockTimeout = { sec ->
                                scope.launch { repository.preferences.setAutoLockTimeout(sec) }
                            },
                            onToggleBiometrics = { enable ->
                                if (enable) {
                                    promptBiometricEnable()
                                } else {
                                    scope.launch { repository.disableBiometricUnlock() }
                                }
                            },
                            onToggleParanoid2Fa = { enable ->
                                scope.launch { repository.preferences.setParanoid2FaEnabled(enable) }
                            },
                            onToggleSelfDestruct = { enable ->
                                scope.launch { repository.preferences.setSelfDestructEnabled(enable) }
                            },
                            onToggleDisguiseMode = { enable ->
                                setDisguiseMode(enable, config.decoyType)
                            },
                            onSetDecoyType = { type ->
                                scope.launch {
                                    repository.preferences.setDecoyType(type)
                                    if (config.isDisguiseMode) {
                                        setDisguiseMode(true, type)
                                    }
                                }
                            },
                            onTogglePrivacyProtection = { enable ->
                                scope.launch { repository.preferences.setPrivacyProtectionEnabled(enable) }
                            },
                            onToggleScreenRecordingDetection = { enable ->
                                scope.launch { repository.preferences.setScreenRecordingDetectionEnabled(enable) }
                            },
                            onSetPhotoTriggerThreshold = { count ->
                                scope.launch { repository.preferences.setPhotoTriggerThreshold(count) }
                            },
                            onNavigateToScanner = {
                                showScannerScreen = true
                            },
                            onNavigateToIntruderLogs = {
                                showIntruderLogsScreen = true
                            },
                            onNavigateToAbout = {
                                showAboutScreen = true
                            },
                            onRotateVaultKey = { masterPwd ->
                                isCryptoBusy = true
                                scope.launch {
                                    val res = repository.rotateVaultKey(masterPwd)
                                    isCryptoBusy = false
                                    if (res.isSuccess) {
                                        snackbarHostState.showSnackbar("DEK rotated successfully: All records re-encrypted")
                                    } else {
                                        snackbarHostState.showSnackbar("Key rotation failed: Incorrect master password")
                                    }
                                }
                            },
                            onChangeMasterPassword = { newPwd ->
                                scope.launch {
                                    val ok = repository.changeMasterPassword(newPwd)
                                    if (ok) {
                                        snackbarHostState.showSnackbar("Master password updated successfully")
                                    } else {
                                        snackbarHostState.showSnackbar("Failed to update master password")
                                    }
                                }
                            },
                            onExportBackupToFile = { backupPwd, isDeviceBound, uri ->
                                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    try {
                                        val outputStream = context.contentResolver.openOutputStream(uri)
                                            ?: throw IllegalStateException("Could not open file for writing")
                                        EncryptedBackupManager.exportEncryptedBackupToStream(
                                            context = context,
                                            entries = entries,
                                            backupPassword = backupPwd,
                                            isDeviceBound = isDeviceBound,
                                            outputStream = outputStream
                                        )
                                        val type = if (isDeviceBound) "Hardware Device-Bound" else "Standard Portable"
                                        snackbarHostState.showSnackbar("$type backup exported securely to file")
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Export error: ${e.message}")
                                    }
                                }
                            },
                            onRestoreBackupFromFile = { uri, backupPwd, isMerge ->
                                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    try {
                                        val inputStream = context.contentResolver.openInputStream(uri)
                                            ?: throw IllegalStateException("Could not open selected file")
                                        val restored = EncryptedBackupManager.restoreEncryptedBackupFromStream(
                                            context = context,
                                            inputStream = inputStream,
                                            backupPassword = backupPwd
                                        )
                                        if (!isMerge) {
                                            repository.clearAllEntries()
                                        }
                                        for (item in restored) {
                                            repository.saveEntry(item)
                                        }
                                        snackbarHostState.showSnackbar("Successfully restored ${restored.size} credentials into vault")
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Restore failed: ${e.message ?: "Incorrect password or corrupt signature"}")
                                    }
                                }
                            },
                            onLockVault = { repository.lockVault() }
                        )
                    }
                }
            }
        }
    }
}
