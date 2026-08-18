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
 * Robust Android Autofill Framework Service for Fort Knox Password Manager.
 *
 * Implements OS-level autofill provider integration, supporting:
 * - Comprehensive AssistStructure parsing for username/email/password fields across native apps and WebViews.
 * - FillResponse dataset generation with high-contrast presentation views.
 * - Authenticated unlock flow when vault is locked.
 * - Always-active SaveInfo registration so system "Save to Fort Knox" prompt appears on form submission.
 * - Internal debug telemetry logged to app storage (without plaintext passwords).
 */
@RequiresApi(Build.VERSION_CODES.O)
class VaultAutofillService : AutofillService() {

    private val serviceScope = CoroutineScope(Dispatchers.Default)

    companion object {
        private const val TAG = "VaultAutofillService"
    }

    data class ParsedFields(
        var usernameId: AutofillId? = null,
        var passwordId: AutofillId? = null,
        var usernameValue: String? = null,
        var passwordValue: String? = null,
        var webDomain: String? = null,
        var packageName: String? = null
    )

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        val fillContexts = request.fillContexts
        val latestContext = fillContexts.lastOrNull()
        val structure = latestContext?.structure

        if (structure == null) {
            AutofillLogger.log(applicationContext, "onFillRequest", "No AssistStructure available in context.")
            callback.onSuccess(null)
            return
        }

        val parsed = parseStructure(structure)
        AutofillLogger.log(
            applicationContext,
            "onFillRequest PARSED",
            "pkg=${parsed.packageName}, domain=${parsed.webDomain}, hasUserField=${parsed.usernameId != null}, hasPassField=${parsed.passwordId != null}"
        )

        // If no relevant fields are found in the view tree, return null
        if (parsed.usernameId == null && parsed.passwordId == null) {
            AutofillLogger.log(applicationContext, "onFillRequest", "No username or password fields detected.")
            callback.onSuccess(null)
            return
        }

        serviceScope.launch {
            try {
                val repository = VaultRepository(applicationContext)
                val isUnlocked = repository.isUnlocked.value
                val responseBuilder = FillResponse.Builder()

                val targetAuthId = parsed.passwordId ?: parsed.usernameId

                if (!isUnlocked) {
                    AutofillLogger.log(applicationContext, "onFillRequest LOCKED", "Vault is locked. Building authentication prompt.")
                    // Provide unlock prompt
                    val intent = Intent(applicationContext, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("AUTOFILL_AUTH_REQUEST", true)
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        applicationContext,
                        1001,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
                    )

                    val authPresentation = createAuthPresentation()

                    if (targetAuthId != null) {
                        responseBuilder.setAuthentication(
                            arrayOf(targetAuthId),
                            pendingIntent.intentSender,
                            authPresentation
                        )
                    }
                } else {
                    // Vault is unlocked - query matched entries
                    val entries = repository.getAllDecryptedEntries()
                    val matched = entries.filter { entry ->
                        val domainMatch = parsed.webDomain != null &&
                                entry.url.contains(parsed.webDomain!!, ignoreCase = true)
                        val pkgMatch = parsed.packageName != null &&
                                entry.url.contains(parsed.packageName!!, ignoreCase = true)
                        val titleMatch = (parsed.packageName != null && entry.title.contains(parsed.packageName!!, ignoreCase = true)) ||
                                (parsed.webDomain != null && entry.title.contains(parsed.webDomain!!, ignoreCase = true))
                        domainMatch || pkgMatch || titleMatch
                    }.ifEmpty {
                        entries.filter { it.category == VaultCategory.LOGINS }
                    }.ifEmpty {
                        entries
                    }.take(6)

                    AutofillLogger.log(applicationContext, "onFillRequest UNLOCKED", "Matched ${matched.size} vault entries.")

                    for (entry in matched) {
                        val datasetBuilder = Dataset.Builder()
                        val presentation = createDatasetPresentation(entry.title, entry.username.ifBlank { "Fort Knox Vault" })

                        parsed.usernameId?.let { uId ->
                            datasetBuilder.setValue(uId, AutofillValue.forText(entry.username), presentation)
                        }

                        parsed.passwordId?.let { pId ->
                            datasetBuilder.setValue(pId, AutofillValue.forText(entry.password), presentation)
                        }

                        responseBuilder.addDataset(datasetBuilder.build())
                    }
                }

                // ALWAYS configure SaveInfo so the system "Save to Fort Knox" prompt appears when user inputs credentials
                val requiredIds = mutableListOf<AutofillId>()
                val optionalIds = mutableListOf<AutofillId>()

                val passId = parsed.passwordId
                val userId = parsed.usernameId

                if (passId != null) {
                    requiredIds.add(passId)
                    if (userId != null) {
                        optionalIds.add(userId)
                    }
                } else if (userId != null) {
                    requiredIds.add(userId)
                }

                if (requiredIds.isNotEmpty()) {
                    val saveInfoType = when {
                        passId != null && userId != null ->
                            SaveInfo.SAVE_DATA_TYPE_PASSWORD or SaveInfo.SAVE_DATA_TYPE_USERNAME
                        passId != null -> SaveInfo.SAVE_DATA_TYPE_PASSWORD
                        else -> SaveInfo.SAVE_DATA_TYPE_USERNAME
                    }

                    val saveInfoBuilder = SaveInfo.Builder(saveInfoType, requiredIds.toTypedArray())
                    if (optionalIds.isNotEmpty()) {
                        saveInfoBuilder.setOptionalIds(optionalIds.toTypedArray())
                    }
                    saveInfoBuilder.setFlags(SaveInfo.FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE)
                    responseBuilder.setSaveInfo(saveInfoBuilder.build())
                }

                AutofillLogger.log(applicationContext, "onFillRequest SUCCESS", "Returning FillResponse with SaveInfo.")
                callback.onSuccess(responseBuilder.build())
            } catch (e: Exception) {
                AutofillLogger.log(applicationContext, "onFillRequest ERROR", "${e.message}")
                Log.e(TAG, "Autofill fill request failed", e)
                callback.onFailure(e.message)
            }
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        AutofillLogger.log(applicationContext, "onSaveRequest RECEIVED", "User accepted system save prompt.")

        val fillContexts = request.fillContexts
        val latestContext = fillContexts.lastOrNull()
        val structure = latestContext?.structure

        if (structure == null) {
            AutofillLogger.log(applicationContext, "onSaveRequest", "No AssistStructure in save context.")
            callback.onSuccess()
            return
        }

        val parsed = parseStructure(structure)
        val username = parsed.usernameValue ?: ""
        val password = parsed.passwordValue ?: ""

        if (password.isBlank() && username.isBlank()) {
            AutofillLogger.log(applicationContext, "onSaveRequest", "Username and password both empty. Skipping.")
            callback.onSuccess()
            return
        }

        serviceScope.launch {
            try {
                val repository = VaultRepository(applicationContext)
                if (repository.isUnlocked.value) {
                    val candidateTitle = parsed.webDomain
                        ?: parsed.packageName
                        ?: "Saved Login"

                    val entry = VaultEntry(
                        title = candidateTitle,
                        username = username,
                        password = password,
                        url = parsed.webDomain ?: parsed.packageName ?: "",
                        category = VaultCategory.LOGINS,
                        notes = "Saved via Android Autofill Service"
                    )
                    repository.saveEntry(entry)
                    AutofillLogger.log(applicationContext, "onSaveRequest SAVED", "Encrypted & persisted credentials for $candidateTitle.")
                } else {
                    AutofillLogger.log(applicationContext, "onSaveRequest LOCKED", "Vault is locked. Credentials require unlocked vault to persist.")
                }
                callback.onSuccess()
            } catch (e: Exception) {
                AutofillLogger.log(applicationContext, "onSaveRequest ERROR", "${e.message}")
                Log.e(TAG, "Autofill save request failed", e)
                callback.onFailure(e.message)
            }
        }
    }

    private fun createDatasetPresentation(title: String, subtitle: String): RemoteViews {
        return try {
            RemoteViews(packageName, R.layout.autofill_dataset_item).apply {
                setTextViewText(R.id.autofill_title, "🛡️ $title")
                setTextViewText(R.id.autofill_subtitle, subtitle)
            }
        } catch (_: Exception) {
            RemoteViews(packageName, android.R.layout.simple_list_item_2).apply {
                setTextViewText(android.R.id.text1, "🛡️ $title")
                setTextViewText(android.R.id.text2, subtitle)
            }
        }
    }

    private fun createAuthPresentation(): RemoteViews {
        return try {
            RemoteViews(packageName, R.layout.autofill_auth_item).apply {
                setTextViewText(R.id.autofill_auth_title, "🛡️ Unlock Fort Knox to Autofill")
                setTextViewText(R.id.autofill_auth_subtitle, "Tap to enter PIN or biometric unlock")
            }
        } catch (_: Exception) {
            RemoteViews(packageName, android.R.layout.simple_list_item_1).apply {
                setTextViewText(android.R.id.text1, "🛡️ Unlock Fort Knox to Autofill")
            }
        }
    }

    private fun parseStructure(structure: AssistStructure): ParsedFields {
        val parsed = ParsedFields()
        try {
            parsed.packageName = structure.activityComponent?.packageName
        } catch (_: Exception) {}

        val queue = ArrayDeque<AssistStructure.ViewNode>()

        for (i in 0 until structure.windowNodeCount) {
            val windowNode = structure.getWindowNodeAt(i)
            queue.add(windowNode.rootViewNode)
        }

        while (!queue.isEmpty()) {
            val node = queue.poll() ?: continue

            if (parsed.webDomain == null && !node.webDomain.isNullOrBlank()) {
                parsed.webDomain = node.webDomain
            }

            val hints = node.autofillHints
            val hintText = node.hint?.toString()?.lowercase() ?: ""
            val idEntry = node.idEntry?.lowercase() ?: ""
            val inputType = node.inputType
            val nodeValue = node.autofillValue?.textValue?.toString() ?: node.text?.toString()

            val isPassword = (inputType and InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT &&
                    (inputType and InputType.TYPE_TEXT_VARIATION_PASSWORD != 0 ||
                     inputType and InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD != 0 ||
                     inputType and InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD != 0) ||
                    hints?.contains(View.AUTOFILL_HINT_PASSWORD) == true ||
                    hints?.contains("new_password") == true ||
                    hints?.contains("password") == true ||
                    idEntry.contains("password") || hintText.contains("password") ||
                    idEntry.contains("pwd") || idEntry.contains("pass") || idEntry.contains("secret")

            val isUsername = hints?.contains(View.AUTOFILL_HINT_USERNAME) == true ||
                    hints?.contains(View.AUTOFILL_HINT_EMAIL_ADDRESS) == true ||
                    hints?.contains(View.AUTOFILL_HINT_NAME) == true ||
                    hints?.contains("new_username") == true ||
                    (inputType and InputType.TYPE_MASK_CLASS == InputType.TYPE_CLASS_TEXT &&
                     (inputType and InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS != 0 ||
                      inputType and InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS != 0)) ||
                    idEntry.contains("username") || idEntry.contains("email") || idEntry.contains("login") ||
                    idEntry.contains("user") || idEntry.contains("account") ||
                    hintText.contains("username") || hintText.contains("email") || hintText.contains("login")

            if (isPassword && parsed.passwordId == null) {
                parsed.passwordId = node.autofillId
                if (!nodeValue.isNullOrBlank()) {
                    parsed.passwordValue = nodeValue
                }
            } else if (isUsername && parsed.usernameId == null) {
                parsed.usernameId = node.autofillId
                if (!nodeValue.isNullOrBlank()) {
                    parsed.usernameValue = nodeValue
                }
            }

            for (j in 0 until node.childCount) {
                queue.add(node.getChildAt(j))
            }
        }

        return parsed
    }
}
