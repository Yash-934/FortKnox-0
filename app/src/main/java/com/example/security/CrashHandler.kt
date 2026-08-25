package com.example.security

import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.util.Log
import com.example.CrashReportActivity
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Global UncaughtExceptionHandler for Fort Knox.
 *
 * Catches all unexpected crashes/exceptions in any thread:
 * 1. Generates a comprehensive, formatted crash and diagnostic report.
 * 2. Automatically copies the entire report to the system clipboard immediately.
 * 3. Persists the crash log to internal storage.
 * 4. Launches a dedicated CrashReportActivity for inspection, sharing, and safe app restart.
 */
class CrashHandler private constructor(private val context: Context) : Thread.UncaughtExceptionHandler {

    private val defaultHandler: Thread.UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler()

    companion object {
        private const val TAG = "CrashHandler"
        private const val PREFS_NAME = "fortknox_crash_prefs"
        private const val KEY_LAST_CRASH_REPORT = "last_crash_report"
        private const val KEY_LAST_CRASH_TIMESTAMP = "last_crash_timestamp"
        private const val CRASH_FILE_NAME = "fortknox_last_crash.txt"

        @Volatile
        private var instance: CrashHandler? = null

        fun init(context: Context) {
            if (instance == null) {
                synchronized(CrashHandler::class.java) {
                    if (instance == null) {
                        val handler = CrashHandler(context.applicationContext)
                        Thread.setDefaultUncaughtExceptionHandler(handler)
                        instance = handler
                        Log.i(TAG, "CrashHandler initialized successfully.")
                    }
                }
            }
        }

        /**
         * Copies the given text to the system clipboard and returns true if successful.
         */
        fun copyToClipboard(context: Context, text: String, label: String = "Fort Knox Crash Report"): Boolean {
            return try {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                if (clipboard != null) {
                    val clip = ClipData.newPlainText(label, text)
                    clipboard.setPrimaryClip(clip)
                    true
                } else {
                    false
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to copy crash report to clipboard", t)
                false
            }
        }

        /**
         * Retrieves the last saved crash report from storage, or null if none exists.
         */
        fun getLastCrashReport(context: Context): String? {
            return try {
                val file = File(context.filesDir, CRASH_FILE_NAME)
                if (file.exists() && file.length() > 0) {
                    file.readText()
                } else {
                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    prefs.getString(KEY_LAST_CRASH_REPORT, null)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to read last crash report", t)
                null
            }
        }

        /**
         * Clears the saved crash report.
         */
        fun clearLastCrashReport(context: Context) {
            try {
                val file = File(context.filesDir, CRASH_FILE_NAME)
                if (file.exists()) {
                    file.delete()
                }
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().clear().apply()
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to clear last crash report", t)
            }
        }
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            Log.e(TAG, "Uncaught exception in thread '${thread.name}'", throwable)

            // 1. Generate full diagnostic report
            val report = generateDiagnosticReport(thread, throwable)

            // 2. Automatically copy to clipboard immediately
            copyToClipboard(context, report)

            // 3. Save report to local storage
            saveCrashReport(report)

            // 4. Launch dedicated CrashReportActivity
            launchCrashReportActivity(report, throwable.localizedMessage ?: throwable.javaClass.simpleName)

        } catch (t: Throwable) {
            Log.e(TAG, "Error in CrashHandler uncaughtException processing", t)
            // Fallback to default handler if our handling fails
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun saveCrashReport(report: String) {
        try {
            val file = File(context.filesDir, CRASH_FILE_NAME)
            file.writeText(report)

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_LAST_CRASH_REPORT, report)
                .putLong(KEY_LAST_CRASH_TIMESTAMP, System.currentTimeMillis())
                .apply()
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to write crash report to file", t)
        }
    }

    private fun launchCrashReportActivity(report: String, errorMessage: String) {
        try {
            val intent = Intent(context, CrashReportActivity::class.java).apply {
                putExtra(CrashReportActivity.EXTRA_CRASH_REPORT, report)
                putExtra(CrashReportActivity.EXTRA_ERROR_MESSAGE, errorMessage)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)

            // Kill the current failing process smoothly
            Process.killProcess(Process.myPid())
            System.exit(10)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to launch CrashReportActivity", t)
        }
    }

    private fun generateDiagnosticReport(thread: Thread, throwable: Throwable): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS z", Locale.US)
        val timestamp = dateFormat.format(Date())

        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        val stackTrace = sw.toString()

        val packageInfo = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
        } catch (t: Throwable) {
            null
        }

        val versionName = packageInfo?.versionName ?: "1.0.0"
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo?.longVersionCode?.toString() ?: "1"
        } else {
            @Suppress("DEPRECATION")
            packageInfo?.versionCode?.toString() ?: "1"
        }

        // Memory stats
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)

        val totalMemMb = memoryInfo.totalMem / (1024 * 1024)
        val availMemMb = memoryInfo.availMem / (1024 * 1024)
        val isLowMem = memoryInfo.lowMemory
        val runtimeMaxHeapMb = Runtime.getRuntime().maxMemory() / (1024 * 1024)
        val runtimeTotalHeapMb = Runtime.getRuntime().totalMemory() / (1024 * 1024)
        val runtimeFreeHeapMb = Runtime.getRuntime().freeMemory() / (1024 * 1024)

        // Hardware feature detection
        val hasStrongBox = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)
        } else false
        val hasFingerprint = context.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)

        val sb = StringBuilder()
        sb.appendLine("================================================================================")
        sb.appendLine("                 FORT KNOX ENCRYPTED VAULT - CRASH REPORT                      ")
        sb.appendLine("================================================================================")
        sb.appendLine("Generated At: $timestamp")
        sb.appendLine("App Package : ${context.packageName}")
        sb.appendLine("App Version : $versionName (Build $versionCode)")
        sb.appendLine("Process PID : ${Process.myPid()}")
        sb.appendLine("Thread Info : ${thread.name} (id: ${thread.id}, priority: ${thread.priority})")
        sb.appendLine()
        sb.appendLine("------------------------------ DEVICE & SYSTEM INFO -----------------------------")
        sb.appendLine("Device Model: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.PRODUCT})")
        sb.appendLine("Brand/Board : ${Build.BRAND} / ${Build.BOARD}")
        sb.appendLine("Hardware    : ${Build.HARDWARE}")
        sb.appendLine("Android OS  : Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            sb.appendLine("Sec. Patch  : ${Build.VERSION.SECURITY_PATCH}")
        }
        sb.appendLine("Architecture: ${Build.SUPPORTED_ABIS.joinToString(", ")}")
        sb.appendLine("Fingerprint : ${Build.FINGERPRINT}")
        sb.appendLine()
        sb.appendLine("------------------------------ RUNTIME & MEMORY --------------------------------")
        sb.appendLine("System RAM  : Available: ${availMemMb}MB / Total: ${totalMemMb}MB (LowMemory: $isLowMem)")
        sb.appendLine("Heap Usage  : Max: ${runtimeMaxHeapMb}MB, Allocated: ${runtimeTotalHeapMb}MB, Free: ${runtimeFreeHeapMb}MB")
        sb.appendLine("StrongBox   : ${if (hasStrongBox) "YES (Hardware Present)" else "NO (TEE Fallback)"}")
        sb.appendLine("Biometrics  : ${if (hasFingerprint) "YES (Fingerprint Sensor Available)" else "NO"}")
        sb.appendLine()
        sb.appendLine("------------------------------ EXCEPTION DETAILS -------------------------------")
        sb.appendLine("Type        : ${throwable.javaClass.name}")
        sb.appendLine("Message     : ${throwable.message ?: "(no message)"}")
        sb.appendLine()
        sb.appendLine("------------------------------ FULL STACK TRACE --------------------------------")
        sb.appendLine(stackTrace.trimEnd())
        sb.appendLine("================================================================================")
        sb.appendLine("                   END OF FORT KNOX CRASH REPORT                               ")
        sb.appendLine("================================================================================")

        return sb.toString()
    }
}
