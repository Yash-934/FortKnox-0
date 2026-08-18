package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.vaultDataStore: DataStore<Preferences> by preferencesDataStore(name = "aegis_vault_prefs")

class VaultPreferences(private val context: Context) {

    companion object {
        val KEY_INITIALIZED = booleanPreferencesKey("is_initialized")
        val KEY_MASTER_SALT = stringPreferencesKey("master_salt")
        val KEY_WRAPPED_DEK = stringPreferencesKey("wrapped_dek")
        val KEY_WRAPPED_DEK_IV = stringPreferencesKey("wrapped_dek_iv")
        val KEY_BIOMETRIC_WRAPPED_DEK = stringPreferencesKey("biometric_wrapped_dek")
        val KEY_BIOMETRIC_WRAPPED_DEK_IV = stringPreferencesKey("biometric_wrapped_dek_iv")
        val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("is_biometric_enabled")
        val KEY_AUTOLOCK_TIMEOUT = intPreferencesKey("autolock_timeout_sec")
        val KEY_SELF_DESTRUCT = booleanPreferencesKey("self_destruct_enabled")
        val KEY_FAILED_ATTEMPTS = intPreferencesKey("failed_attempts")
        val KEY_LOCKOUT_UNTIL = longPreferencesKey("lockout_until")
        val KEY_DISGUISE_MODE = booleanPreferencesKey("is_disguise_mode")
        val KEY_DECOY_TYPE = stringPreferencesKey("decoy_type")
        val KEY_PRIVACY_PROTECTION = booleanPreferencesKey("is_privacy_protection_enabled")
        val KEY_PHOTO_TRIGGER_THRESHOLD = intPreferencesKey("photo_trigger_threshold")
        val KEY_PARANOID_2FA = booleanPreferencesKey("is_paranoid_2fa_enabled")
        val KEY_DEVICE_BOUND_BACKUP = booleanPreferencesKey("is_device_bound_backup")
        val KEY_PERIODIC_ROOT_CHECK = booleanPreferencesKey("is_periodic_root_check_enabled")
        val KEY_SCREEN_RECORDING_DETECTION = booleanPreferencesKey("is_screen_recording_detection_enabled")
    }

    data class VaultConfig(
        val isInitialized: Boolean,
        val masterSalt: String?,
        val wrappedDek: String?,
        val wrappedDekIv: String?,
        val biometricWrappedDek: String?,
        val biometricWrappedDekIv: String?,
        val isBiometricEnabled: Boolean,
        val autoLockTimeoutSec: Int,
        val selfDestructEnabled: Boolean,
        val failedAttempts: Int,
        val lockoutUntil: Long,
        val isDisguiseMode: Boolean = false,
        val decoyType: String = "NOTES", // "NOTES" or "CALCULATOR"
        val isPrivacyProtectionEnabled: Boolean = true,
        val photoTriggerThreshold: Int = 3,
        val isParanoid2FaEnabled: Boolean = false,
        val isDeviceBoundBackup: Boolean = false,
        val isPeriodicRootCheckEnabled: Boolean = true,
        val isScreenRecordingDetectionEnabled: Boolean = true
    )

    val configFlow: Flow<VaultConfig> = context.vaultDataStore.data.map { prefs ->
        VaultConfig(
            isInitialized = prefs[KEY_INITIALIZED] ?: false,
            masterSalt = prefs[KEY_MASTER_SALT],
            wrappedDek = prefs[KEY_WRAPPED_DEK],
            wrappedDekIv = prefs[KEY_WRAPPED_DEK_IV],
            biometricWrappedDek = prefs[KEY_BIOMETRIC_WRAPPED_DEK],
            biometricWrappedDekIv = prefs[KEY_BIOMETRIC_WRAPPED_DEK_IV],
            isBiometricEnabled = prefs[KEY_BIOMETRIC_ENABLED] ?: false,
            autoLockTimeoutSec = prefs[KEY_AUTOLOCK_TIMEOUT] ?: 30,
            selfDestructEnabled = prefs[KEY_SELF_DESTRUCT] ?: true,
            failedAttempts = prefs[KEY_FAILED_ATTEMPTS] ?: 0,
            lockoutUntil = prefs[KEY_LOCKOUT_UNTIL] ?: 0L,
            isDisguiseMode = prefs[KEY_DISGUISE_MODE] ?: false,
            decoyType = prefs[KEY_DECOY_TYPE] ?: "NOTES",
            isPrivacyProtectionEnabled = prefs[KEY_PRIVACY_PROTECTION] ?: true,
            photoTriggerThreshold = prefs[KEY_PHOTO_TRIGGER_THRESHOLD] ?: 3,
            isParanoid2FaEnabled = prefs[KEY_PARANOID_2FA] ?: false,
            isDeviceBoundBackup = prefs[KEY_DEVICE_BOUND_BACKUP] ?: false,
            isPeriodicRootCheckEnabled = prefs[KEY_PERIODIC_ROOT_CHECK] ?: true,
            isScreenRecordingDetectionEnabled = prefs[KEY_SCREEN_RECORDING_DETECTION] ?: true
        )
    }

    suspend fun saveMasterKeyConfig(
        saltBase64: String,
        wrappedDekBase64: String,
        wrappedDekIvBase64: String
    ) {
        context.vaultDataStore.edit { prefs ->
            prefs[KEY_INITIALIZED] = true
            prefs[KEY_MASTER_SALT] = saltBase64
            prefs[KEY_WRAPPED_DEK] = wrappedDekBase64
            prefs[KEY_WRAPPED_DEK_IV] = wrappedDekIvBase64
            prefs[KEY_FAILED_ATTEMPTS] = 0
            prefs[KEY_LOCKOUT_UNTIL] = 0L
        }
    }

    suspend fun saveBiometricConfig(
        biometricWrappedDekBase64: String?,
        biometricWrappedDekIvBase64: String?,
        enabled: Boolean
    ) {
        context.vaultDataStore.edit { prefs ->
            if (biometricWrappedDekBase64 != null && biometricWrappedDekIvBase64 != null) {
                prefs[KEY_BIOMETRIC_WRAPPED_DEK] = biometricWrappedDekBase64
                prefs[KEY_BIOMETRIC_WRAPPED_DEK_IV] = biometricWrappedDekIvBase64
                prefs[KEY_BIOMETRIC_ENABLED] = enabled
            } else {
                prefs.remove(KEY_BIOMETRIC_WRAPPED_DEK)
                prefs.remove(KEY_BIOMETRIC_WRAPPED_DEK_IV)
                prefs[KEY_BIOMETRIC_ENABLED] = false
            }
        }
    }

    suspend fun setAutoLockTimeout(seconds: Int) {
        context.vaultDataStore.edit { prefs ->
            prefs[KEY_AUTOLOCK_TIMEOUT] = seconds
        }
    }

    suspend fun setSelfDestructEnabled(enabled: Boolean) {
        context.vaultDataStore.edit { prefs ->
            prefs[KEY_SELF_DESTRUCT] = enabled
        }
    }

    suspend fun setDisguiseMode(enabled: Boolean) {
        context.vaultDataStore.edit { prefs ->
            prefs[KEY_DISGUISE_MODE] = enabled
        }
    }

    suspend fun setDecoyType(type: String) {
        context.vaultDataStore.edit { prefs ->
            prefs[KEY_DECOY_TYPE] = type
        }
    }

    suspend fun setParanoid2FaEnabled(enabled: Boolean) {
        context.vaultDataStore.edit { prefs ->
            prefs[KEY_PARANOID_2FA] = enabled
        }
    }

    suspend fun setDeviceBoundBackup(enabled: Boolean) {
        context.vaultDataStore.edit { prefs ->
            prefs[KEY_DEVICE_BOUND_BACKUP] = enabled
        }
    }

    suspend fun setPeriodicRootCheckEnabled(enabled: Boolean) {
        context.vaultDataStore.edit { prefs ->
            prefs[KEY_PERIODIC_ROOT_CHECK] = enabled
        }
    }

    suspend fun setScreenRecordingDetectionEnabled(enabled: Boolean) {
        context.vaultDataStore.edit { prefs ->
            prefs[KEY_SCREEN_RECORDING_DETECTION] = enabled
        }
    }

    suspend fun setPrivacyProtection(enabled: Boolean) {
        context.vaultDataStore.edit { prefs ->
            prefs[KEY_PRIVACY_PROTECTION] = enabled
        }
    }

    suspend fun setPrivacyProtectionEnabled(enabled: Boolean) {
        setPrivacyProtection(enabled)
    }

    suspend fun setPhotoTriggerThreshold(threshold: Int) {
        context.vaultDataStore.edit { prefs ->
            prefs[KEY_PHOTO_TRIGGER_THRESHOLD] = threshold
        }
    }

    suspend fun incrementFailedAttempts(): Int {
        var attempts = 0
        context.vaultDataStore.edit { prefs ->
            val current = prefs[KEY_FAILED_ATTEMPTS] ?: 0
            attempts = current + 1
            prefs[KEY_FAILED_ATTEMPTS] = attempts
            if (attempts >= 5) {
                // Enforce 30-second delay
                prefs[KEY_LOCKOUT_UNTIL] = System.currentTimeMillis() + 30_000L
            }
        }
        return attempts
    }

    suspend fun setLockoutUntil(timeMillis: Long) {
        context.vaultDataStore.edit { prefs ->
            prefs[KEY_LOCKOUT_UNTIL] = timeMillis
        }
    }

    suspend fun resetFailedAttempts() {
        context.vaultDataStore.edit { prefs ->
            prefs[KEY_FAILED_ATTEMPTS] = 0
            prefs[KEY_LOCKOUT_UNTIL] = 0L
        }
    }

    suspend fun resetAll() {
        context.vaultDataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
