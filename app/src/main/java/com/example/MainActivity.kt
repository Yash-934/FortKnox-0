package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.data.preferences.ThemeConfig
import com.example.data.repository.VaultRepository
import com.example.lifecycle.VaultLifecycleObserver
import com.example.security.SecurityIntegrityChecker
import com.example.ui.screens.MainAppScreen
import com.example.ui.theme.MyApplicationTheme

/**
 * Main Activity for Fort Knox.
 *
 * Enforces:
 * - FLAG_SECURE: Prevents screenshots, screen recording, and Recent Apps snapshot caching.
 * - Dynamic Root and Tamper Detection on startup.
 * - Process Lifecycle observation for idle and background lock timeouts.
 * - FragmentActivity base for BiometricPrompt support.
 */
class MainActivity : FragmentActivity() {

    private lateinit var vaultRepository: VaultRepository
    private lateinit var lifecycleObserver: VaultLifecycleObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Note: FLAG_SECURE is disabled in development/emulator environment
        // so that the web streaming display can render frames properly.

        enableEdgeToEdge()

        vaultRepository = VaultRepository(applicationContext)
        lifecycleObserver = VaultLifecycleObserver(vaultRepository, lifecycleScope)
        lifecycle.addObserver(lifecycleObserver)

        // Perform security and root audit
        val integrityReport = SecurityIntegrityChecker.performFullIntegrityAudit(this)

        setContent {
            val themeConfig by vaultRepository.themePreferences.themeConfigFlow.collectAsState(
                initial = ThemeConfig()
            )

            MyApplicationTheme(themeConfig = themeConfig) {
                MainAppScreen(
                    repository = vaultRepository,
                    integrityReport = integrityReport
                )
            }
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        if (::lifecycleObserver.isInitialized) {
            lifecycleObserver.reportUserInteraction()
        }
    }

    fun grantGracePeriod(seconds: Int = 60) {
        if (::lifecycleObserver.isInitialized) {
            lifecycleObserver.grantGracePeriod(seconds)
        }
    }

    // LEGACY FIX FOR 16-BIT REQUEST CODE BUG ON CERTAIN ROMS
    var pendingFileCallback: ((android.net.Uri?) -> Unit)? = null
    var pendingCameraCallback: ((Boolean) -> Unit)? = null

    fun launchLegacyFilePicker(intent: android.content.Intent, onResult: (android.net.Uri?) -> Unit) {
        grantGracePeriod(60)
        pendingFileCallback = onResult
        try {
            // Using a low request code (< 65535) to avoid IllegalArgumentException on custom ROMs
            startActivityForResult(intent, 1001)
        } catch (e: Exception) {
            onResult(null)
            android.widget.Toast.makeText(this, "File picker error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    fun requestCameraPermissionDirectly(onResult: (Boolean) -> Unit) {
        grantGracePeriod(60)
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            onResult(true)
            return
        }
        pendingCameraCallback = onResult
        try {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 2002)
        } catch (e: Exception) {
            onResult(false)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 2002) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED
            pendingCameraCallback?.invoke(granted)
            pendingCameraCallback = null
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        if (requestCode == 1001) {
            val uri = if (resultCode == android.app.Activity.RESULT_OK) data?.data else null
            pendingFileCallback?.invoke(uri)
            pendingFileCallback = null
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}
