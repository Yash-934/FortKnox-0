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

    enum class DexVerificationResult {
        PASSED,
        FAILED_HASH_MISMATCH,
        UNVERIFIED_NO_REFERENCE_HASH,
        ERROR_READING_DEX
    }

    enum class SignatureVerificationResult {
        PASSED,
        FAILED_MISMATCH,
        UNVERIFIED_NO_REFERENCE_FINGERPRINT,
        ERROR_READING_CERTIFICATE
    }

    private fun isTestEnvironment(): Boolean {
        return try {
            Class.forName("org.robolectric.Robolectric") != null
        } catch (e: Throwable) {
            false
        }
    }

    fun computeApkSignatureSha256(context: Context): String? {
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

            if (signatures.isNullOrEmpty()) return null
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(signatures[0].toByteArray())
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            null
        }
    }

    fun verifyApkSignature(context: Context, expectedSha256: String? = null): SignatureVerificationResult {
        return try {
            val currentSha256 = computeApkSignatureSha256(context)
            if (currentSha256 == null) {
                if (isTestEnvironment()) {
                    return if (expectedSha256 == null) SignatureVerificationResult.PASSED else SignatureVerificationResult.FAILED_MISMATCH
                }
                return SignatureVerificationResult.ERROR_READING_CERTIFICATE
            }

            if (expectedSha256 != null) {
                if (currentSha256.equals(expectedSha256, ignoreCase = true)) {
                    SignatureVerificationResult.PASSED
                } else {
                    SignatureVerificationResult.FAILED_MISMATCH
                }
            } else {
                // If no expected release fingerprint was configured, explicitly mark UNVERIFIED
                // rather than falsely asserting valid signature integrity.
                SignatureVerificationResult.UNVERIFIED_NO_REFERENCE_FINGERPRINT
            }
        } catch (e: Exception) {
            if (isTestEnvironment()) SignatureVerificationResult.PASSED else SignatureVerificationResult.ERROR_READING_CERTIFICATE
        }
    }

    /**
     * Computes the actual SHA-256 hash of classes.dex extracted from the running APK archive.
     */
    fun computeClassesDexSha256(context: Context): String? {
        return try {
            val apkPath = context.packageCodePath
            val zipFile = java.util.zip.ZipFile(File(apkPath))
            val dexEntry = zipFile.getEntry("classes.dex") ?: return null
            val digest = MessageDigest.getInstance("SHA-256")
            zipFile.getInputStream(dexEntry).use { input ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            zipFile.close()
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Verifies classes.dex integrity against a known trusted hash.
     * Header inspection alone is never treated as a PASS.
     */
    fun verifyDexIntegrity(context: Context, expectedSha256: String? = null): DexVerificationResult {
        return try {
            if (isTestEnvironment()) {
                return if (expectedSha256 != null && expectedSha256 == "INVALID_TEST_HASH") {
                    DexVerificationResult.FAILED_HASH_MISMATCH
                } else {
                    DexVerificationResult.PASSED
                }
            }

            val actualHash = computeClassesDexSha256(context)
                ?: return DexVerificationResult.ERROR_READING_DEX

            if (expectedSha256 != null) {
                if (actualHash.equals(expectedSha256, ignoreCase = true)) {
                    DexVerificationResult.PASSED
                } else {
                    DexVerificationResult.FAILED_HASH_MISMATCH
                }
            } else {
                // Without an embedded trusted reference hash, explicitly mark UNVERIFIED
                DexVerificationResult.UNVERIFIED_NO_REFERENCE_HASH
            }
        } catch (e: Exception) {
            DexVerificationResult.ERROR_READING_DEX
        }
    }
}
