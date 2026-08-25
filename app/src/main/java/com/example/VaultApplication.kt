package com.example

import android.app.Application
import com.example.security.CrashHandler

/**
 * Custom Application class for Fort Knox.
 *
 * Initializes global uncaught exception crash handler on process startup.
 */
class VaultApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize global uncaught crash handler with automatic clipboard copy & report activity
        CrashHandler.init(this)
    }
}
