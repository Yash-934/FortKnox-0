package com.example.autofill

import android.app.PendingIntent
import android.app.assist.AssistStructure
import android.content.Intent
import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillContext
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveInfo
import android.service.autofill.SaveRequest
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import com.example.MainActivity
import com.example.R
import com.example.data.model.VaultCategory
import com.example.data.model.VaultEntry
import com.example.data.repository.VaultRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.ArrayDeque

/**
 * Production-Grade Android Autofill Framework Service for Fort Knox Password Manager.
 *
 * Provides rock-solid OS-level autofill provider integration supporting:
 * - Robust recursive AssistStructure & HtmlInfo traversal across DuckDuckGo, Chrome, Edge, Brave, and WebViews.
 * - Multi-signal heuristics: Autofill Hints, InputType variations, HTML attributes, resource IDs, and proximity.
 * - Reliable authentication-gated and unlocked Dataset presentations.
 * - System SaveInfo registration for automatic credential capture.
 * - Comprehensive sanitized diagnostic telemetry logging.
 */
@RequiresApi(Build.VERSION_CODES.O)
class VaultAutofillService : AutofillService() {

    private val serviceScope = CoroutineScope(Dispatchers.Default)

    companion object {
        private const val TAG = "VaultAutofillService"

        fun createDatasetPresentation(context: android.content.Context, title: String, subtitle: String): RemoteViews {
            return try {
                RemoteViews(context.packageName, R.layout.autofill_dataset_item).apply {
                    setTextViewText(R.id.autofill_title, "🛡️ $title")
                    setTextViewText(R.id.autofill_subtitle, subtitle)
                }
            } catch (_: Exception) {
                RemoteViews(context.packageName, android.R.layout.simple_list_item_2).apply {
                    setTextViewText(android.R.id.text1, "🛡️ $title")
                    setTextViewText(android.R.id.text2, subtitle)
                }
            }
        }

        fun createAuthPresentation(context: android.content.Context): RemoteViews {
            return try {
                RemoteViews(context.packageName, R.layout.autofill_auth_item).apply {
                    setTextViewText(R.id.autofill_auth_title, "🛡️ Unlock Fort Knox to Autofill")
                    setTextViewText(R.id.autofill_auth_subtitle, "Tap to enter PIN or biometric unlock")
                }
            } catch (_: Exception) {
                RemoteViews(context.packageName, android.R.layout.simple_list_item_1).apply {
                    setTextViewText(android.R.id.text1, "🛡️ Unlock Fort Knox to Autofill")
                }
            }
        }

        fun sanitizeTitle(domain: String?, pkg: String?): String {
            if (!domain.isNullOrBlank()) {
                val clean = domain.removePrefix("https://")
                    .removePrefix("http://")
                    .removePrefix("www.")
                    .substringBefore("/")
                    .substringBefore(":")
                if (clean.contains("github")) return "GitHub"
                if (clean.contains("google") || clean.contains("accounts.google")) return "Google"
                if (clean.contains("amazon") || clean.contains("aws.")) return "Amazon / AWS"
                if (clean.contains("netflix")) return "Netflix"
                if (clean.contains("twitter") || clean.contains("x.com")) return "X / Twitter"
                if (clean.contains("reddit")) return "Reddit"
                if (clean.contains("facebook")) return "Facebook"
                if (clean.contains("instagram")) return "Instagram"
                if (clean.contains("linkedin")) return "LinkedIn"
                if (clean.contains("microsoft") || clean.contains("live.com")) return "Microsoft"
                return if (clean.isNotEmpty()) clean[0].uppercaseChar() + clean.substring(1) else clean
            }
            if (!pkg.isNullOrBlank()) {
                val simple = pkg.substringAfterLast(".")
                return (if (simple.isNotEmpty()) simple[0].uppercaseChar() + simple.substring(1) else simple) + " App"
            }
            return "Saved Login"
        }

        fun sanitizeFolder(domain: String?, pkg: String?): String {
            val target = (domain ?: pkg ?: "").lowercase()
            return when {
                target.contains("github") -> "GitHub"
                target.contains("google") -> "Google"
                target.contains("amazon") || target.contains("aws") -> "AWS Cloud"
                target.contains("netflix") || target.contains("spotify") || target.contains("youtube") -> "Entertainment"
                target.contains("binance") || target.contains("crypto") || target.contains("coinbase") -> "Crypto"
                target.contains("bank") || target.contains("chase") || target.contains("paypal") -> "Finance"
                else -> ""
            }
        }

        /**
         * Recursively parses the AssistStructure view hierarchy (Native apps, DuckDuckGo, Chrome, Edge, WebViews).
         * Uses multi-signal heuristics to identify username/email/password fields accurately.
         */
        fun parseStructure(structure: AssistStructure, context: android.content.Context? = null): ParsedFields {
            val parsed = ParsedFields()
            try {
                parsed.packageName = structure.activityComponent?.packageName
            } catch (_: Exception) {}

            val windowCount = structure.windowNodeCount
            parsed.totalWindowNodes = windowCount

            val queue = ArrayDeque<AssistStructure.ViewNode>()
            for (i in 0 until windowCount) {
                val windowNode = structure.getWindowNodeAt(i) ?: continue
                val root = windowNode.rootViewNode ?: continue
                queue.add(root)
            }

            var previousTextInputNode: AssistStructure.ViewNode? = null
            var scannedCount = 0

            while (!queue.isEmpty()) {
                val node = queue.poll() ?: continue
                scannedCount++

                // 1. Web Domain extraction
                if (parsed.webDomain.isNullOrBlank() && !node.webDomain.isNullOrBlank()) {
                    parsed.webDomain = node.webDomain
                }

                val autofillId = node.autofillId
                val hints = node.autofillHints?.map { it.lowercase() }
                val hintText = node.hint?.toString()?.lowercase() ?: ""
                val idEntry = node.idEntry?.lowercase() ?: ""
                val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
                val inputType = node.inputType
                val className = node.className?.lowercase() ?: ""
                val htmlInfo = node.htmlInfo
                val htmlTag = htmlInfo?.tag?.lowercase() ?: ""

                var nodeValue = node.autofillValue?.textValue?.toString()
                    ?: node.text?.toString()
                if (nodeValue.isNullOrBlank() && htmlInfo != null) {
                    nodeValue = htmlInfo.attributes?.firstOrNull { it.first.equals("value", ignoreCase = true) }?.second
                }

                var isPassword = false
                var isUsername = false

                // 2. Primary Signal: Autofill Hints (Standard OS API)
                if (hints != null && hints.isNotEmpty()) {
                    if (hints.contains(View.AUTOFILL_HINT_PASSWORD.lowercase()) ||
                        hints.contains("new_password") ||
                        hints.contains("current_password") ||
                        hints.contains("password")
                    ) {
                        isPassword = true
                    }
                    if (hints.contains(View.AUTOFILL_HINT_USERNAME.lowercase()) ||
                        hints.contains(View.AUTOFILL_HINT_EMAIL_ADDRESS.lowercase()) ||
                        hints.contains(View.AUTOFILL_HINT_NAME.lowercase()) ||
                        hints.contains("email") ||
                        hints.contains("username") ||
                        hints.contains("login")
                    ) {
                        isUsername = true
                    }
                }

                // 3. Secondary Signal: HTML Info Attributes (Browsers & WebViews)
                if (htmlInfo != null) {
                    htmlInfo.attributes?.forEach { attrPair ->
                        val lowerName = attrPair.first?.lowercase() ?: ""
                        val lowerVal = attrPair.second?.lowercase() ?: ""

                        if (lowerName == "autocomplete") {
                            if (lowerVal.contains("username") || lowerVal.contains("email")) {
                                isUsername = true
                            }
                            if (lowerVal.contains("password") || lowerVal.contains("current-password") || lowerVal.contains("new-password")) {
                                isPassword = true
                            }
                        }

                        if (lowerName == "type") {
                            if (lowerVal == "password") {
                                isPassword = true
                            }
                            if (lowerVal in listOf("email", "text", "tel")) {
                                // Potential text input
                            }
                        }

                        if (lowerName in listOf("name", "id", "placeholder", "aria-label", "aria-labelledby")) {
                            if (lowerVal.contains("pass") || lowerVal.contains("pwd") || lowerVal.contains("secret") || lowerVal.contains("pin")) {
                                isPassword = true
                            }
                            if (lowerVal.contains("user") || lowerVal.contains("email") || lowerVal.contains("login") || lowerVal.contains("account") || lowerVal.contains("identifier")) {
                                isUsername = true
                            }
                        }
                    }
                }

                // 4. Tertiary Signal: InputType Variations
                val textVariation = inputType and InputType.TYPE_MASK_VARIATION
                val isTextClass = (inputType and InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT
                val isNumberClass = (inputType and InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_NUMBER

                if (isTextClass) {
                    if (textVariation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                        textVariation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                        textVariation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
                    ) {
                        isPassword = true
                    }
                    if (textVariation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
                        textVariation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS ||
                        textVariation == InputType.TYPE_TEXT_VARIATION_PERSON_NAME
                    ) {
                        isUsername = true
                    }
                } else if (isNumberClass && (inputType and InputType.TYPE_NUMBER_VARIATION_PASSWORD != 0)) {
                    isPassword = true
                }

                // 5. Signal: Resource ID, Hint, and Content Description Strings
                val combinedMeta = "$idEntry $hintText $contentDesc"
                if (combinedMeta.contains("password") || combinedMeta.contains("pass") || combinedMeta.contains("pwd") || combinedMeta.contains("secret")) {
                    isPassword = true
                }
                if (combinedMeta.contains("username") || combinedMeta.contains("user") || combinedMeta.contains("email") ||
                    combinedMeta.contains("login") || combinedMeta.contains("account") || combinedMeta.contains("identifier")
                ) {
                    isUsername = true
                }

                // 6. Proximity Heuristic (Associate preceding text input with password field)
                if (isPassword && parsed.usernameId == null && previousTextInputNode != null) {
                    val prevId = previousTextInputNode.autofillId
                    if (prevId != null) {
                        parsed.usernameId = prevId
                        if (!parsed.allCandidateUserIds.contains(prevId)) {
                            parsed.allCandidateUserIds.add(prevId)
                        }
                    }
                    val prevVal = previousTextInputNode.autofillValue?.textValue?.toString()
                        ?: previousTextInputNode.text?.toString()
                    if (!prevVal.isNullOrBlank() && parsed.usernameValue.isNullOrBlank()) {
                        parsed.usernameValue = prevVal
                    }
                }

                // 7. Record matches and candidate IDs
                if (autofillId != null) {
                    if (isPassword) {
                        if (!parsed.allCandidatePassIds.contains(autofillId)) {
                            parsed.allCandidatePassIds.add(autofillId)
                        }
                        if (parsed.passwordId == null) {
                            parsed.passwordId = autofillId
                        }
                        if (!nodeValue.isNullOrBlank() && parsed.passwordValue.isNullOrBlank()) {
                            parsed.passwordValue = nodeValue
                        }
                    } else if (isUsername) {
                        if (!parsed.allCandidateUserIds.contains(autofillId)) {
                            parsed.allCandidateUserIds.add(autofillId)
                        }
                        if (parsed.usernameId == null) {
                            parsed.usernameId = autofillId
                        }
                        if (!nodeValue.isNullOrBlank() && parsed.usernameValue.isNullOrBlank()) {
                            parsed.usernameValue = nodeValue
                        }
                    } else if (isTextClass || className.contains("edittext") || className.contains("input") || htmlTag == "input") {
                        previousTextInputNode = node
                    }
                }

                for (j in 0 until node.childCount) {
                    val child = node.getChildAt(j) ?: continue
                    queue.add(child)
                }
            }

            parsed.totalScannedNodes = scannedCount
            return parsed
        }
    }

    data class ParsedFields(
        var totalWindowNodes: Int = 0,
        var totalScannedNodes: Int = 0,
        var usernameId: AutofillId? = null,
        var passwordId: AutofillId? = null,
        var usernameValue: String? = null,
        var passwordValue: String? = null,
        var webDomain: String? = null,
        var packageName: String? = null,
        val allCandidateUserIds: MutableList<AutofillId> = mutableListOf(),
        val allCandidatePassIds: MutableList<AutofillId> = mutableListOf()
    )

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        AutofillLogger.log(applicationContext, "onFillRequest called", "Request ID=${request.id}, Flags=${request.flags}")

        val fillContexts = request.fillContexts
        if (fillContexts.isEmpty()) {
            AutofillLogger.log(applicationContext, "onFillRequest", "No FillContexts available.")
            callback.onSuccess(null)
            return
        }

        // Aggregate structure parsing from the latest fill context
        val latestContext = fillContexts.last()
        val structure = latestContext.structure

        if (structure == null) {
            AutofillLogger.log(applicationContext, "onFillRequest", "AssistStructure is null.")
            callback.onSuccess(null)
            return
        }

        val parsed = parseStructure(structure, applicationContext)
        AutofillLogger.log(
            applicationContext,
            "onFillRequest PARSED",
            "Windows=${parsed.totalWindowNodes}, ScannedNodes=${parsed.totalScannedNodes}, pkg=${parsed.packageName}, domain=${parsed.webDomain}, detectedUser=${parsed.usernameId != null}, detectedPass=${parsed.passwordId != null}"
        )

        // If no credentials or inputs found at all, return null
        if (parsed.usernameId == null && parsed.passwordId == null &&
            parsed.allCandidateUserIds.isEmpty() && parsed.allCandidatePassIds.isEmpty()
        ) {
            AutofillLogger.log(applicationContext, "onFillRequest", "No username or password fields detected.")
            callback.onSuccess(null)
            return
        }

        serviceScope.launch {
            try {
                val repository = VaultRepository(applicationContext)
                val isUnlocked = repository.isUnlocked.value
                val responseBuilder = FillResponse.Builder()

                val primaryUserId = parsed.usernameId ?: parsed.allCandidateUserIds.firstOrNull()
                val primaryPassId = parsed.passwordId ?: parsed.allCandidatePassIds.firstOrNull()

                if (!isUnlocked) {
                    AutofillLogger.log(applicationContext, "onFillRequest LOCKED", "Vault is locked. Returning authentication prompt.")

                    val authIntent = Intent(applicationContext, AutofillAuthActivity::class.java).apply {
                        putExtra("EXTRA_DOMAIN", parsed.webDomain)
                        putExtra("EXTRA_PACKAGE", parsed.packageName)
                        putExtra("EXTRA_MODE", "FILL")
                        putExtra("EXTRA_REQUEST_ID", request.id)
                        primaryUserId?.let { putExtra("EXTRA_USER_ID", it) }
                        primaryPassId?.let { putExtra("EXTRA_PASS_ID", it) }
                        if (parsed.allCandidateUserIds.isNotEmpty()) {
                            putParcelableArrayListExtra("EXTRA_ALL_USER_IDS", ArrayList(parsed.allCandidateUserIds))
                        }
                        if (parsed.allCandidatePassIds.isNotEmpty()) {
                            putParcelableArrayListExtra("EXTRA_ALL_PASS_IDS", ArrayList(parsed.allCandidatePassIds))
                        }
                    }
                    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
                    } else {
                        PendingIntent.FLAG_CANCEL_CURRENT
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        applicationContext,
                        1001,
                        authIntent,
                        flags
                    )

                    val authPresentation = createAuthPresentation(applicationContext)
                    val authIds = listOfNotNull(primaryUserId, primaryPassId).toTypedArray()
                    if (authIds.isNotEmpty()) {
                        responseBuilder.setAuthentication(
                            authIds,
                            pendingIntent.intentSender,
                            authPresentation
                        )
                    }
                } else {
                    // Vault is unlocked - query stored credentials
                    val entries = repository.getAllDecryptedEntries()
                    val matched = entries.filter { entry ->
                        val domainMatch = !parsed.webDomain.isNullOrBlank() &&
                                (entry.url.contains(parsed.webDomain!!, ignoreCase = true) ||
                                 entry.title.contains(parsed.webDomain!!, ignoreCase = true) ||
                                 entry.folder.contains(parsed.webDomain!!, ignoreCase = true))
                        val pkgMatch = !parsed.packageName.isNullOrBlank() &&
                                (entry.url.contains(parsed.packageName!!, ignoreCase = true) ||
                                 entry.title.contains(parsed.packageName!!, ignoreCase = true))
                        domainMatch || pkgMatch
                    }.ifEmpty {
                        entries.filter { it.category == VaultCategory.LOGINS }
                    }.ifEmpty {
                        entries
                    }.take(8)

                    AutofillLogger.log(applicationContext, "onFillRequest UNLOCKED", "Found ${matched.size} matching credentials.")

                    for (entry in matched) {
                        val datasetBuilder = Dataset.Builder()
                        val presentation = createDatasetPresentation(applicationContext, entry.title, entry.username.ifBlank { "Fort Knox Vault" })

                        var hasValue = false
                        primaryUserId?.let { uId ->
                            if (entry.username.isNotBlank()) {
                                datasetBuilder.setValue(uId, AutofillValue.forText(entry.username), presentation)
                                hasValue = true
                            }
                        }

                        primaryPassId?.let { pId ->
                            if (entry.password.isNotBlank()) {
                                datasetBuilder.setValue(pId, AutofillValue.forText(entry.password), presentation)
                                hasValue = true
                            }
                        }

                        if (hasValue) {
                            responseBuilder.addDataset(datasetBuilder.build())
                        }
                    }
                }

                // ALWAYS configure SaveInfo so the system "Save to Fort Knox" prompt appears
                val requiredIds = mutableListOf<AutofillId>()
                val optionalIds = mutableListOf<AutofillId>()

                if (primaryPassId != null) {
                    requiredIds.add(primaryPassId)
                    if (primaryUserId != null) {
                        optionalIds.add(primaryUserId)
                    }
                    for (extraPass in parsed.allCandidatePassIds) {
                        if (extraPass != primaryPassId && !optionalIds.contains(extraPass)) {
                            optionalIds.add(extraPass)
                        }
                    }
                    for (extraUser in parsed.allCandidateUserIds) {
                        if (extraUser != primaryUserId && !optionalIds.contains(extraUser)) {
                            optionalIds.add(extraUser)
                        }
                    }
                } else if (primaryUserId != null) {
                    requiredIds.add(primaryUserId)
                    for (extraUser in parsed.allCandidateUserIds) {
                        if (extraUser != primaryUserId && !optionalIds.contains(extraUser)) {
                            optionalIds.add(extraUser)
                        }
                    }
                }

                if (requiredIds.isNotEmpty()) {
                    val saveInfoType = when {
                        primaryPassId != null && primaryUserId != null ->
                            SaveInfo.SAVE_DATA_TYPE_USERNAME or SaveInfo.SAVE_DATA_TYPE_PASSWORD
                        primaryPassId != null -> SaveInfo.SAVE_DATA_TYPE_PASSWORD
                        else -> SaveInfo.SAVE_DATA_TYPE_USERNAME
                    }

                    val saveInfoBuilder = SaveInfo.Builder(saveInfoType, requiredIds.toTypedArray())
                    if (optionalIds.isNotEmpty()) {
                        saveInfoBuilder.setOptionalIds(optionalIds.toTypedArray())
                    }
                    saveInfoBuilder.setFlags(SaveInfo.FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE)
                    responseBuilder.setSaveInfo(saveInfoBuilder.build())
                }

                AutofillLogger.log(applicationContext, "onFillRequest SUCCESS", "Delivering FillResponse.")
                callback.onSuccess(responseBuilder.build())
            } catch (e: Exception) {
                AutofillLogger.log(applicationContext, "onFillRequest ERROR", "${e.message}")
                Log.e(TAG, "Autofill fill request failed", e)
                callback.onFailure(e.message)
            }
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        AutofillLogger.log(applicationContext, "onSaveRequest called", "Save request triggered.")

        var username = ""
        var password = ""
        var webDomain: String? = null
        var packageName: String? = null

        // Aggregate structure parsing from all fill contexts (supports multi-screen flows)
        for (fillContext in request.fillContexts) {
            val structure = fillContext.structure ?: continue
            val parsed = parseStructure(structure, applicationContext)
            if (!parsed.usernameValue.isNullOrBlank()) username = parsed.usernameValue!!.trim()
            if (!parsed.passwordValue.isNullOrBlank()) password = parsed.passwordValue!!.trim()
            if (!parsed.webDomain.isNullOrBlank()) webDomain = parsed.webDomain
            if (!parsed.packageName.isNullOrBlank()) packageName = parsed.packageName
        }

        AutofillLogger.log(
            applicationContext,
            "onSaveRequest PARSED",
            "domain=$webDomain, pkg=$packageName, hasUser=${username.isNotBlank()}, hasPass=${password.isNotBlank()}"
        )

        if (password.isBlank() && username.isBlank()) {
            AutofillLogger.log(applicationContext, "onSaveRequest", "Username and password both empty. Skipping.")
            callback.onSuccess()
            return
        }

        serviceScope.launch {
            try {
                val repository = VaultRepository(applicationContext)
                val candidateTitle = sanitizeTitle(webDomain, packageName)
                val candidateFolder = sanitizeFolder(webDomain, packageName)

                if (repository.isUnlocked.value) {
                    val entry = VaultEntry(
                        title = candidateTitle,
                        username = username,
                        password = password,
                        url = webDomain ?: packageName ?: "",
                        folder = candidateFolder,
                        category = VaultCategory.LOGINS,
                        notes = "Captured automatically via Android Autofill"
                    )
                    repository.saveEntry(entry)
                    AutofillLogger.log(applicationContext, "onSaveRequest SAVED", "Encrypted credentials saved for $candidateTitle.")
                    callback.onSuccess()
                } else {
                    AutofillLogger.log(applicationContext, "onSaveRequest LOCKED", "Vault locked. Requesting auth for Save.")
                    val saveIntent = Intent(applicationContext, AutofillAuthActivity::class.java).apply {
                        putExtra("EXTRA_MODE", "SAVE")
                        putExtra("EXTRA_DOMAIN", webDomain)
                        putExtra("EXTRA_PACKAGE", packageName)
                        putExtra("EXTRA_SAVE_USER", username)
                        putExtra("EXTRA_SAVE_PASS", password)
                    }
                    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
                    } else {
                        PendingIntent.FLAG_CANCEL_CURRENT
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        applicationContext,
                        1002,
                        saveIntent,
                        flags
                    )
                    callback.onSuccess(pendingIntent.intentSender)
                }
            } catch (e: Exception) {
                AutofillLogger.log(applicationContext, "onSaveRequest ERROR", "${e.message}")
                Log.e(TAG, "Autofill save request failed", e)
                callback.onFailure(e.message)
            }
        }
    }
}
