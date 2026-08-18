package com.example.autofill

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Diagnostic logger for Android Autofill Service interactions.
 *
 * Persists diagnostic telemetry to internal application storage (no plaintext passwords)
 * to facilitate troubleshooting autofill popup and service triggers on physical devices.
 */
object AutofillLogger {
    private const val TAG = "VaultAutofill"
    private const val LOG_FILE_NAME = "autofill_debug.log"
    private const val MAX_LOG_ENTRIES = 100

    private val _logsFlow = MutableStateFlow<List<String>>(emptyList())
    val logsFlow: StateFlow<List<String>> = _logsFlow.asStateFlow()

    @Synchronized
    fun log(context: Context?, event: String, details: String = "") {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val entry = "[$timestamp] $event ${if (details.isNotBlank()) "• $details" else ""}"
        
        Log.d(TAG, entry)

        // Update in-memory state
        val updated = (_logsFlow.value + entry).takeLast(MAX_LOG_ENTRIES)
        _logsFlow.value = updated

        // Persist to internal storage file
        if (context != null) {
            try {
                val file = File(context.filesDir, LOG_FILE_NAME)
                file.appendText("$entry\n")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write autofill debug log", e)
            }
        }
    }

    @Synchronized
    fun getLogFileContent(context: Context): String {
        return try {
            val file = File(context.filesDir, LOG_FILE_NAME)
            if (file.exists()) file.readText() else "No autofill logs recorded yet."
        } catch (e: Exception) {
            "Error reading log file: ${e.message}"
        }
    }

    @Synchronized
    fun clearLogs(context: Context) {
        _logsFlow.value = emptyList()
        try {
            val file = File(context.filesDir, LOG_FILE_NAME)
            if (file.exists()) file.delete()
        } catch (_: Exception) {}
    }
}
