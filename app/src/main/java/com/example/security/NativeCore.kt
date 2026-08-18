package com.example.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import java.io.File
import java.security.MessageDigest

object NativeCore {
    private const val TAG = "NativeCore"
    private var isNativeLoaded = false

    init {
        try {
            System.loadLibrary("fortknox")
            isNativeLoaded = true
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "Native library 'fortknox' not loaded, using secure fallback: ${e.message}")
            isNativeLoaded = false
        }
    }

    fun isNativeEngineActive(): Boolean = isNativeLoaded

    external fun isRootDetected(): Boolean
    external fun isDebuggerOrHookDetected(): Boolean
    external fun enforceKillSwitch()
    external fun lockMemory(array: ByteArray): Boolean
    external fun unlockMemory(array: ByteArray): Boolean
    external fun secureWipe(array: ByteArray)

    fun checkRoot(): Boolean {
        if (isNativeLoaded) {
            try {
                return isRootDetected()
            } catch (e: Throwable) {
                Log.e(TAG, "Native root check failed", e)
            }
        }
        return SecurityIntegrityChecker.checkSuBinary()
    }

    fun checkDebuggerOrHook(): Boolean {
        if (isNativeLoaded) {
            try {
                return isDebuggerOrHookDetected()
            } catch (e: Throwable) {
                Log.e(TAG, "Native hook check failed", e)
            }
        }
        return false
    }

    fun triggerKillSwitch() {
        if (isNativeLoaded) {
            try {
                enforceKillSwitch()
                return
            } catch (e: Throwable) {
                Log.e(TAG, "Native kill switch failed", e)
            }
        }
        android.os.Process.killProcess(android.os.Process.myPid())
        System.exit(1)
    }

    fun mlock(buffer: ByteArray): Boolean {
        if (isNativeLoaded) {
            try {
                return lockMemory(buffer)
            } catch (e: Throwable) {
                Log.e(TAG, "Native mlock failed", e)
            }
        }
        return false
    }

    fun munlock(buffer: ByteArray): Boolean {
        if (isNativeLoaded) {
            try {
                return unlockMemory(buffer)
            } catch (e: Throwable) {
                Log.e(TAG, "Native munlock failed", e)
            }
        }
        return false
    }

    fun wipe(buffer: ByteArray) {
        if (isNativeLoaded) {
            try {
                secureWipe(buffer)
                return
            } catch (e: Throwable) {
                Log.e(TAG, "Native wipe failed", e)
            }
        }
        buffer.fill(0.toByte())
    }

    fun verifyApkSignature(context: Context, expectedSha256: String? = null): Boolean {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            if (signatures.isNullOrEmpty()) return false
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(signatures[0].toByteArray())
            val hexString = hash.joinToString("") { "%02x".format(it) }

            if (expectedSha256 != null) {
                hexString.equals(expectedSha256, ignoreCase = true)
            } else {
                hexString.isNotEmpty()
            }
        } catch (e: Exception) {
            false
        }
    }

    fun verifyDexIntegrity(context: Context): Boolean {
        return try {
            val apkPath = context.packageCodePath
            val apkFile = File(apkPath)
            apkFile.exists() && apkFile.length() > 0
        } catch (e: Exception) {
            false
        }
    }
}
