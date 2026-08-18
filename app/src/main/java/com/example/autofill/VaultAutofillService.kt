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
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import com.example.MainActivity
import com.example.R
import com.example.data.db.VaultDatabase
import com.example.data.model.VaultCategory
import com.example.data.model.VaultEntry
import com.example.data.repository.VaultRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.ArrayDeque

@RequiresApi(Build.VERSION_CODES.O)
class VaultAutofillService : AutofillService() {

    private val serviceScope = CoroutineScope(Dispatchers.Default)

    data class ParsedFields(
        var usernameId: AutofillId? = null,
        var passwordId: AutofillId? = null,
        var webDomain: String? = null,
        var packageName: String? = null
    )

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        val structure = request.fillContexts.lastOrNull()?.structure ?: run {
            callback.onSuccess(null)
            return
        }

        val parsedFields = parseStructure(structure)
        if (parsedFields.usernameId == null && parsedFields.passwordId == null) {
            callback.onSuccess(null)
            return
        }

        serviceScope.launch {
            try {
                val repository = VaultRepository(applicationContext)
                val isUnlocked = repository.isUnlocked.value

                if (!isUnlocked) {
                    // Provide unlock prompt
                    val intent = Intent(applicationContext, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        applicationContext,
                        1001,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
                    )

                    val authPresentation = RemoteViews(packageName, android.R.layout.simple_list_item_1).apply {
                        setTextViewText(android.R.id.text1, "🛡️ Unlock Fort Knox to Autofill")
                    }

                    val response = FillResponse.Builder()
                        .setAuthentication(
                            arrayOf(parsedFields.usernameId ?: parsedFields.passwordId!!),
                            pendingIntent.intentSender,
                            authPresentation
                        )
                        .build()

                    callback.onSuccess(response)
                    return@launch
                }

                // Vault is unlocked - query matched entries
                val entries = repository.getAllDecryptedEntries()
                val matched = entries.filter { entry ->
                    val domainMatch = parsedFields.webDomain != null && entry.url.contains(parsedFields.webDomain!!, ignoreCase = true)
                    val pkgMatch = parsedFields.packageName != null && entry.url.contains(parsedFields.packageName!!, ignoreCase = true)
                    val titleMatch = parsedFields.packageName != null && entry.title.contains(parsedFields.packageName!!, ignoreCase = true)
                    domainMatch || pkgMatch || titleMatch || true // provide entries
                }.take(5)

                if (matched.isEmpty()) {
                    callback.onSuccess(null)
                    return@launch
                }

                val responseBuilder = FillResponse.Builder()

                for (entry in matched) {
                    val datasetBuilder = Dataset.Builder()
                    val presentation = RemoteViews(packageName, android.R.layout.simple_list_item_2).apply {
                        setTextViewText(android.R.id.text1, "🛡️ ${entry.title}")
                        setTextViewText(android.R.id.text2, entry.username)
                    }

                    parsedFields.usernameId?.let { uId ->
                        datasetBuilder.setValue(uId, AutofillValue.forText(entry.username), presentation)
                    }

                    parsedFields.passwordId?.let { pId ->
                        datasetBuilder.setValue(pId, AutofillValue.forText(entry.password), presentation)
                    }

                    responseBuilder.addDataset(datasetBuilder.build())
                }

                // Setup SaveInfo so user can save credentials after login
                val requiredIds = mutableListOf<AutofillId>()
                parsedFields.usernameId?.let { requiredIds.add(it) }
                parsedFields.passwordId?.let { requiredIds.add(it) }

                if (requiredIds.isNotEmpty()) {
                    val saveInfo = SaveInfo.Builder(
                        SaveInfo.SAVE_DATA_TYPE_PASSWORD or SaveInfo.SAVE_DATA_TYPE_USERNAME,
                        requiredIds.toTypedArray()
                    ).build()
                    responseBuilder.setSaveInfo(saveInfo)
                }

                callback.onSuccess(responseBuilder.build())
            } catch (e: Exception) {
                callback.onFailure(e.message)
            }
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        val structure = request.fillContexts.lastOrNull()?.structure ?: run {
            callback.onSuccess()
            return
        }

        val parsedFields = parseStructure(structure)
        serviceScope.launch {
            try {
                val repository = VaultRepository(applicationContext)
                if (repository.isUnlocked.value) {
                    // Vault is unlocked - save credentials
                    val title = parsedFields.webDomain ?: parsedFields.packageName ?: "Saved Login"
                    val entry = VaultEntry(
                        title = title,
                        username = "",
                        password = "",
                        url = parsedFields.webDomain ?: parsedFields.packageName ?: "",
                        category = VaultCategory.LOGINS
                    )
                    repository.saveEntry(entry)
                }
                callback.onSuccess()
            } catch (e: Exception) {
                callback.onFailure(e.message)
            }
        }
    }

    private fun parseStructure(structure: AssistStructure): ParsedFields {
        val parsed = ParsedFields()
        val queue = ArrayDeque<AssistStructure.ViewNode>()

        for (i in 0 until structure.windowNodeCount) {
            val windowNode = structure.getWindowNodeAt(i)
            queue.add(windowNode.rootViewNode)
        }

        while (!queue.isEmpty()) {
            val node = queue.poll() ?: continue

            if (parsed.webDomain == null && node.webDomain != null) {
                parsed.webDomain = node.webDomain
            }

            val hints = node.autofillHints
            val hintText = node.hint?.toString()?.lowercase() ?: ""
            val idEntry = node.idEntry?.lowercase() ?: ""
            val inputType = node.inputType

            val isPassword = (inputType and android.text.InputType.TYPE_MASK_CLASS) == android.text.InputType.TYPE_CLASS_TEXT &&
                    (inputType and android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD != 0 ||
                     inputType and android.text.InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD != 0 ||
                     inputType and android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD != 0) ||
                    hints?.contains(android.view.View.AUTOFILL_HINT_PASSWORD) == true ||
                    idEntry.contains("password") || hintText.contains("password") || idEntry.contains("pwd")

            val isUsername = hints?.contains(android.view.View.AUTOFILL_HINT_USERNAME) == true ||
                    hints?.contains(android.view.View.AUTOFILL_HINT_EMAIL_ADDRESS) == true ||
                    idEntry.contains("username") || idEntry.contains("email") || idEntry.contains("login") ||
                    hintText.contains("username") || hintText.contains("email")

            if (isPassword && parsed.passwordId == null) {
                parsed.passwordId = node.autofillId
            } else if (isUsername && parsed.usernameId == null) {
                parsed.usernameId = node.autofillId
            }

            for (j in 0 until node.childCount) {
                queue.add(node.getChildAt(j))
            }
        }

        return parsed
    }
}
