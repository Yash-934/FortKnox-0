package com.example.autofill

import android.app.Activity
import android.app.assist.AssistStructure
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import android.service.autofill.SaveInfo
import android.view.autofill.AutofillId
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillValue
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.example.data.model.VaultCategory
import com.example.data.model.VaultEntry
import com.example.data.repository.VaultRepository
import com.example.security.KeystoreManager
import com.example.ui.components.CyberButton
import com.example.ui.components.ScrambledPinPad
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberLaserRed
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceHigh
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/**
 * Dedicated, lightweight, transparent authentication dialog activity for Android Autofill.
 *
 * Runs as a transient overlay on top of the calling browser or app:
 * - Unlocks the vault via Master PIN or Biometrics without opening the full MainActivity.
 * - Handles both Autofill Fill and Autofill Save requests.
 * - Returns the decrypted FillResponse via AutofillManager.EXTRA_AUTHENTICATION_RESULT
 *   and finishes cleanly.
 */
class AutofillAuthActivity : FragmentActivity() {

    private lateinit var repository: VaultRepository

    private var mode: String = "FILL"
    private var webDomain: String? = null
    private var packageNameArg: String? = null
    private var saveUsername: String? = null
    private var savePassword: String? = null
    private var assistStructure: AssistStructure? = null
    private var extraUserId: AutofillId? = null
    private var extraPassId: AutofillId? = null
    private var extraUserIds: ArrayList<AutofillId>? = null
    private var extraPassIds: ArrayList<AutofillId>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repository = VaultRepository(applicationContext)

        // Extract extras
        mode = intent.getStringExtra("EXTRA_MODE") ?: "FILL"
        webDomain = intent.getStringExtra("EXTRA_DOMAIN")
        packageNameArg = intent.getStringExtra("EXTRA_PACKAGE")
        saveUsername = intent.getStringExtra("EXTRA_SAVE_USER")
        savePassword = intent.getStringExtra("EXTRA_SAVE_PASS")

        extraUserId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("EXTRA_USER_ID", AutofillId::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<AutofillId>("EXTRA_USER_ID")
        }

        extraPassId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("EXTRA_PASS_ID", AutofillId::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<AutofillId>("EXTRA_PASS_ID")
        }

        extraUserIds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra("EXTRA_ALL_USER_IDS", AutofillId::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<AutofillId>("EXTRA_ALL_USER_IDS")
        }

        extraPassIds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra("EXTRA_ALL_PASS_IDS", AutofillId::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<AutofillId>("EXTRA_ALL_PASS_IDS")
        }

        assistStructure = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(AutofillManager.EXTRA_ASSIST_STRUCTURE, AssistStructure::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<AssistStructure>(AutofillManager.EXTRA_ASSIST_STRUCTURE)
        }

        AutofillLogger.log(
            this,
            "AutofillAuthActivity onCreate",
            "Mode: $mode, Domain: $webDomain, Pkg: $packageNameArg, HasStructure: ${assistStructure != null}, HasUserExtra: ${extraUserId != null}, HasPassExtra: ${extraPassId != null}"
        )

        setContent {
            MyApplicationTheme {
                AutofillAuthDialogContent(
                    mode = mode,
                    domain = webDomain,
                    pkg = packageNameArg,
                    saveUsername = saveUsername,
                    onDismiss = { cancelAndFinish() },
                    onUnlockSuccess = { onVaultUnlocked() },
                    onBiometricRequest = { triggerBiometricUnlock() }
                )
            }
        }
    }

    private fun cancelAndFinish() {
        AutofillLogger.log(this, "AutofillAuthActivity", "Cancelled by user")
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    private fun onVaultUnlocked() {
        lifecycleScope.launch {
            if (mode == "SAVE") {
                handleSaveAction()
            } else {
                handleFillAction()
            }
        }
    }

    private suspend fun handleSaveAction() = withContext(Dispatchers.Default) {
        val user = saveUsername ?: ""
        val pass = savePassword ?: ""
        val title = VaultAutofillService.sanitizeTitle(webDomain, packageNameArg)
        val folder = VaultAutofillService.sanitizeFolder(webDomain, packageNameArg)

        val entry = VaultEntry(
            title = title,
            username = user,
            password = pass,
            url = webDomain ?: packageNameArg ?: "",
            folder = folder,
            category = VaultCategory.LOGINS,
            notes = "Saved via Android Autofill prompt"
        )
        repository.saveEntry(entry)
        AutofillLogger.log(applicationContext, "AutofillAuthActivity SAVE", "Saved $title ($user)")

        withContext(Dispatchers.Main) {
            Toast.makeText(applicationContext, "🛡️ Saved to Fort Knox: $title", Toast.LENGTH_SHORT).show()
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    private suspend fun handleFillAction() = withContext(Dispatchers.Default) {
        val parsed = assistStructure?.let { VaultAutofillService.parseStructure(it) }

        val primaryUserId = extraUserId ?: parsed?.usernameId
        val primaryPassId = extraPassId ?: parsed?.passwordId

        val targetUserIds = (extraUserIds ?: parsed?.allCandidateUserIds ?: emptyList()).toMutableList()
        val targetPassIds = (extraPassIds ?: parsed?.allCandidatePassIds ?: emptyList()).toMutableList()

        if (primaryUserId != null && !targetUserIds.contains(primaryUserId)) {
            targetUserIds.add(0, primaryUserId)
        }
        if (primaryPassId != null && !targetPassIds.contains(primaryPassId)) {
            targetPassIds.add(0, primaryPassId)
        }

        val effectiveDomain = webDomain ?: parsed?.webDomain
        val effectivePkg = packageNameArg ?: parsed?.packageName

        AutofillLogger.log(
            applicationContext,
            "AutofillAuthActivity handleFillAction",
            "effectiveDomain=$effectiveDomain, effectivePkg=$effectivePkg, primaryUser=$primaryUserId, primaryPass=$primaryPassId"
        )

        if (primaryUserId == null && primaryPassId == null) {
            AutofillLogger.log(applicationContext, "AutofillAuthActivity FILL", "No target AutofillIds identified. Returning OK.")
            withContext(Dispatchers.Main) {
                setResult(Activity.RESULT_OK)
                finish()
            }
            return@withContext
        }

        val allEntries = repository.getAllDecryptedEntries()
        val targetQuery = (effectiveDomain ?: effectivePkg ?: "").lowercase()

        // Match credentials by domain, package, url, title or folder
        val matchingEntries = allEntries.filter { entry ->
            val eTitle = entry.title.lowercase()
            val eUrl = entry.url.lowercase()
            val eFolder = entry.folder.lowercase()

            if (targetQuery.isBlank()) true
            else eUrl.contains(targetQuery) || eTitle.contains(targetQuery) || eFolder.contains(targetQuery) ||
                 (effectiveDomain != null && eUrl.contains(effectiveDomain.lowercase())) ||
                 (effectivePkg != null && (eUrl.contains(effectivePkg.lowercase()) || eFolder.contains(effectivePkg.lowercase())))
        }.ifEmpty {
            allEntries.filter { it.category == VaultCategory.LOGINS }
        }.ifEmpty {
            allEntries
        }

        if (matchingEntries.isEmpty()) {
            AutofillLogger.log(applicationContext, "AutofillAuthActivity FILL", "No matching credentials in vault.")
            withContext(Dispatchers.Main) {
                Toast.makeText(applicationContext, "No matching credentials in Fort Knox", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
            return@withContext
        }

        val responseBuilder = FillResponse.Builder()
        var datasetCount = 0

        for (entry in matchingEntries.take(8)) {
            val title = entry.title.ifBlank { VaultAutofillService.sanitizeTitle(effectiveDomain, effectivePkg) }
            val subtitle = entry.username.ifBlank { "Fort Knox Login" }
            val presentation = VaultAutofillService.createDatasetPresentation(applicationContext, title, subtitle)

            val datasetBuilder = Dataset.Builder()
            var hasField = false

            if (primaryUserId != null && entry.username.isNotBlank()) {
                datasetBuilder.setValue(primaryUserId, AutofillValue.forText(entry.username), presentation)
                hasField = true
            }
            for (extraU in targetUserIds) {
                if (extraU != primaryUserId && entry.username.isNotBlank()) {
                    datasetBuilder.setValue(extraU, AutofillValue.forText(entry.username), presentation)
                    hasField = true
                }
            }

            if (primaryPassId != null && entry.password.isNotBlank()) {
                datasetBuilder.setValue(primaryPassId, AutofillValue.forText(entry.password), presentation)
                hasField = true
            }
            for (extraP in targetPassIds) {
                if (extraP != primaryPassId && entry.password.isNotBlank()) {
                    datasetBuilder.setValue(extraP, AutofillValue.forText(entry.password), presentation)
                    hasField = true
                }
            }

            if (!hasField) {
                if (primaryPassId != null && entry.password.isNotBlank()) {
                    datasetBuilder.setValue(primaryPassId, AutofillValue.forText(entry.password), presentation)
                    hasField = true
                }
                if (primaryUserId != null && entry.username.isNotBlank()) {
                    datasetBuilder.setValue(primaryUserId, AutofillValue.forText(entry.username), presentation)
                    hasField = true
                }
            }

            if (hasField) {
                responseBuilder.addDataset(datasetBuilder.build())
                datasetCount++
            }
        }

        // Also configure SaveInfo on the fill response
        val requiredIds = mutableListOf<AutofillId>()
        val optionalIds = mutableListOf<AutofillId>()

        if (primaryPassId != null) {
            requiredIds.add(primaryPassId)
            if (primaryUserId != null) {
                optionalIds.add(primaryUserId)
            }
            for (extraP in targetPassIds) {
                if (extraP != primaryPassId && !optionalIds.contains(extraP)) {
                    optionalIds.add(extraP)
                }
            }
            for (extraU in targetUserIds) {
                if (extraU != primaryUserId && !optionalIds.contains(extraU)) {
                    optionalIds.add(extraU)
                }
            }
        } else if (primaryUserId != null) {
            requiredIds.add(primaryUserId)
            for (extraU in targetUserIds) {
                if (extraU != primaryUserId && !optionalIds.contains(extraU)) {
                    optionalIds.add(extraU)
                }
            }
        }

        if (requiredIds.isNotEmpty()) {
            val saveType = when {
                primaryPassId != null && primaryUserId != null ->
                    SaveInfo.SAVE_DATA_TYPE_USERNAME or SaveInfo.SAVE_DATA_TYPE_PASSWORD
                primaryPassId != null -> SaveInfo.SAVE_DATA_TYPE_PASSWORD
                else -> SaveInfo.SAVE_DATA_TYPE_USERNAME
            }
            val reqArray: Array<AutofillId> = requiredIds.toTypedArray()
            val saveInfoBuilder = SaveInfo.Builder(saveType, reqArray)
            if (optionalIds.isNotEmpty()) {
                val optArray: Array<AutofillId> = optionalIds.toTypedArray()
                saveInfoBuilder.setOptionalIds(optArray)
            }
            saveInfoBuilder.setFlags(SaveInfo.FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE)
            responseBuilder.setSaveInfo(saveInfoBuilder.build())
        }

        if (datasetCount > 0) {
            val fillResponse = responseBuilder.build()
            val replyIntent = Intent().apply {
                putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, fillResponse)
            }
            AutofillLogger.log(applicationContext, "AutofillAuthActivity FILL", "Delivering $datasetCount datasets to caller app via EXTRA_AUTHENTICATION_RESULT.")
            withContext(Dispatchers.Main) {
                setResult(Activity.RESULT_OK, replyIntent)
                finish()
            }
        } else {
            withContext(Dispatchers.Main) {
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }

    private fun triggerBiometricUnlock() {
        lifecycleScope.launch {
            val config = repository.preferences.configFlow.first()
            val ivBase64 = config.biometricWrappedDekIv ?: run {
                Toast.makeText(applicationContext, "Biometrics not configured in Fort Knox", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val ivBytes = android.util.Base64.decode(ivBase64, android.util.Base64.DEFAULT)
            val cipher = KeystoreManager.initBiometricDecryptionCipher(ivBytes) ?: run {
                Toast.makeText(applicationContext, "Biometric key invalidated or unavailable", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val executor = Executors.newSingleThreadExecutor()
            val prompt = BiometricPrompt(
                this@AutofillAuthActivity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        val authCipher = result.cryptoObject?.cipher ?: return
                        lifecycleScope.launch {
                            val res = repository.unlockWithBiometrics(authCipher)
                            if (res.isSuccess) {
                                onVaultUnlocked()
                            } else {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(applicationContext, "Biometric unlock failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        lifecycleScope.launch(Dispatchers.Main) {
                            Toast.makeText(applicationContext, "$errString", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Fort Knox Autofill")
                .setSubtitle("Biometric unlock for autofill")
                .setNegativeButtonText("Use Master PIN")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build()

            prompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        }
    }

    @Composable
    private fun AutofillAuthDialogContent(
        mode: String,
        domain: String?,
        pkg: String?,
        saveUsername: String?,
        onDismiss: () -> Unit,
        onUnlockSuccess: () -> Unit,
        onBiometricRequest: () -> Unit
    ) {
        val scope = rememberCoroutineScope()
        val config by repository.preferences.configFlow.collectAsState(initial = null)

        var pinDigits by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var isSubmitting by remember { mutableStateOf(false) }

        val titleText = if (mode == "SAVE") "Save to Fort Knox" else "Fort Knox Autofill"
        val subtitleText = VaultAutofillService.sanitizeTitle(domain, pkg)
        val isBiometricEnabled = config?.isBiometricEnabled == true

        var autoBiometricPrompted by remember { mutableStateOf(false) }
        androidx.compose.runtime.LaunchedEffect(isBiometricEnabled) {
            if (isBiometricEnabled && !autoBiometricPrompted) {
                autoBiometricPrompted = true
                onBiometricRequest()
            }
        }

        // Outer scrim dismiss
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(CutCornerShape(16.dp))
                    .border(1.5.dp, CyberCyan.copy(alpha = 0.6f), CutCornerShape(16.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* prevent dismiss when clicking dialog body */ },
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                shape = CutCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(CyberCyan.copy(alpha = 0.15f))
                                    .border(1.dp, CyberCyan, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Shield",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = titleText,
                                    color = CyberTextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = subtitleText,
                                    color = CyberCyan,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = CyberTextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (mode == "SAVE" && !saveUsername.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CutCornerShape(8.dp))
                                .background(CyberSurfaceHigh)
                                .border(1.dp, CyberBorder, CutCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = CyberEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Account to Save:",
                                        color = CyberTextMuted,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = saveUsername,
                                        color = CyberTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // PIN Dots
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        for (i in 0 until 6) {
                            val filled = i < pinDigits.length
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 6.dp)
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(if (filled) CyberCyan else CyberSurfaceHigh)
                                    .border(
                                        1.dp,
                                        if (filled) CyberCyan else CyberBorder,
                                        CircleShape
                                    )
                            )
                        }
                    }

                    // Error Message
                    errorMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = msg,
                            color = CyberLaserRed,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Scrambled Keypad
                    ScrambledPinPad(
                        onDigitClick = { digit ->
                            if (pinDigits.length < 6) {
                                val newPin = pinDigits + digit
                                pinDigits = newPin
                                errorMessage = null

                                if (newPin.length >= 4) {
                                    scope.launch {
                                        isSubmitting = true
                                        val result = repository.unlockWithMasterPassword(newPin.toCharArray())
                                        isSubmitting = false
                                        if (result.isSuccess) {
                                            onUnlockSuccess()
                                        } else {
                                            if (newPin.length == 6) {
                                                errorMessage = "Incorrect PIN"
                                                pinDigits = ""
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        onBackspace = {
                            if (pinDigits.isNotEmpty()) {
                                pinDigits = pinDigits.dropLast(1)
                                errorMessage = null
                            }
                        },
                        onShuffle = {},
                        onBiometricClick = if (isBiometricEnabled) onBiometricRequest else null
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        CyberButton(
                            text = "Cancel",
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            color = CyberSurfaceHigh,
                            textColor = CyberTextMuted
                        )
                        if (isBiometricEnabled) {
                            Spacer(modifier = Modifier.width(12.dp))
                            CyberButton(
                                text = "Biometrics",
                                onClick = onBiometricRequest,
                                modifier = Modifier.weight(1f),
                                color = CyberEmerald.copy(alpha = 0.2f),
                                textColor = CyberEmerald
                            )
                        }
                    }
                }
            }
        }
    }
}
