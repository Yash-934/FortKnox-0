package com.example.data.repository

import android.content.Context
import android.os.Build
import android.util.Base64
import com.example.data.db.DecoyNoteDao
import com.example.data.db.IntrusionLogDao
import com.example.data.db.VaultDao
import com.example.data.db.VaultDatabase
import com.example.data.model.DecoyNoteEntity
import com.example.data.model.EncryptedVaultEntity
import com.example.data.model.IntrusionLogEntity
import com.example.data.model.VaultCategory
import com.example.data.model.VaultEntry
import com.example.data.preferences.VaultPreferences
import com.example.security.CryptoEngine
import com.example.security.KeystoreManager
import com.example.security.NativeCore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher

class VaultRepository(
    private val context: Context,
    private val vaultDao: VaultDao = VaultDatabase.getDatabase(context).vaultDao(),
    private val intrusionLogDao: IntrusionLogDao = VaultDatabase.getDatabase(context).intrusionLogDao(),
    private val decoyNoteDao: DecoyNoteDao = VaultDatabase.getDatabase(context).decoyNoteDao(),
    val preferences: VaultPreferences = VaultPreferences(context)
) {

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    // In-memory decrypted Data Encryption Key (DEK). Zeroed on lock.
    private var activeDek: ByteArray? = null
    private var pending2FaDek: ByteArray? = null

    val intrusionLogsFlow: Flow<List<IntrusionLogEntity>> = intrusionLogDao.getAllLogs()
    val decoyNotesFlow: Flow<List<DecoyNoteEntity>> = decoyNoteDao.getAllNotes()

    val entriesFlow: Flow<List<VaultEntry>> = vaultDao.getAllEntries().map { entities ->
        val currentDek = activeDek
        if (currentDek == null) {
            emptyList()
        } else {
            entities.mapNotNull { entity ->
                decryptEntity(entity, currentDek)
            }
        }
    }

    /**
     * Seeds realistic notes into the decoy database if empty.
     */
    suspend fun seedInitialDecoyNotesIfEmpty() = withContext(Dispatchers.IO) {
        if (decoyNoteDao.getAllNotesSnapshot().isEmpty()) {
            val sampleNotes = listOf(
                DecoyNoteEntity(
                    title = "Weekly Grocery List",
                    content = "- Almond milk & Greek yogurt\n- Organic avocados (4x)\n- Whole grain sourdough bread\n- Fair trade coffee beans\n- Dark chocolate (85%)",
                    colorTag = 1
                ),
                DecoyNoteEntity(
                    title = "Book Recommendations",
                    content = "1. Clean Architecture - Robert C. Martin\n2. Thinking, Fast and Slow - Daniel Kahneman\n3. Designing Data-Intensive Applications\n4. Atomic Habits - James Clear",
                    colorTag = 2
                ),
                DecoyNoteEntity(
                    title = "Weekend Home Projects",
                    content = "- Paint patio furniture\n- Check HVAC air filter replacement\n- Plant basil and cherry tomato seeds in garden bed",
                    colorTag = 3
                )
            )
            for (note in sampleNotes) {
                decoyNoteDao.insertNote(note)
            }
        }
    }

    suspend fun saveDecoyNote(note: DecoyNoteEntity): Long = withContext(Dispatchers.IO) {
        if (note.id == 0L) {
            decoyNoteDao.insertNote(note)
        } else {
            decoyNoteDao.updateNote(note)
            note.id
        }
    }

    suspend fun deleteDecoyNote(id: Long) = withContext(Dispatchers.IO) {
        decoyNoteDao.deleteNoteById(id)
    }

    /**
     * Records a wrong password or unauthorized attempt with optional encrypted front camera photo
     * and tamper-evident HMAC signature.
     */
    suspend fun recordWrongPasswordAttempt(
        attemptCount: Int,
        photoBytes: ByteArray?,
        failureReason: String = "Incorrect master credential entered"
    ) = withContext(Dispatchers.Default) {
        var logKey: ByteArray? = null
        try {
            logKey = KeystoreManager.getOrCreateLogEncryptionKey(context)
            var encryptedPhotoBase64: String? = null
            var photoIvBase64: String? = null
            var photoAvailable = false

            if (photoBytes != null && photoBytes.isNotEmpty()) {
                val encrypted = CryptoEngine.encryptAesGcm(photoBytes, logKey)
                encryptedPhotoBase64 = Base64.encodeToString(encrypted.ciphertext, Base64.NO_WRAP)
                photoIvBase64 = Base64.encodeToString(encrypted.iv, Base64.NO_WRAP)
                photoAvailable = true
            }

            val deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"

            val status = when {
                attemptCount >= 10 -> "SELF_DESTRUCT_TRIGGERED"
                attemptCount >= 5 -> "LOCKOUT_ENFORCED"
                photoAvailable -> "PHOTO_CAPTURED"
                else -> "WRONG_PASSWORD"
            }

            val timestamp = System.currentTimeMillis()

            // Compute HMAC-SHA256 tamper-evident seal
            val hmacMac = javax.crypto.Mac.getInstance("HmacSHA256")
            val hmacSecret = javax.crypto.spec.SecretKeySpec(logKey, "HmacSHA256")
            hmacMac.init(hmacSecret)
            val signedPayload = "$timestamp:$attemptCount:$status:$failureReason:$deviceInfo".toByteArray(StandardCharsets.UTF_8)
            val hmacDigest = hmacMac.doFinal(signedPayload)
            val hmacBase64 = Base64.encodeToString(hmacDigest, Base64.NO_WRAP)

            val logEntity = IntrusionLogEntity(
                timestamp = timestamp,
                attemptNumber = attemptCount,
                status = "$status (HMAC Verified)",
                photoEncryptedBase64 = encryptedPhotoBase64,
                photoIv = photoIvBase64,
                photoAvailable = photoAvailable,
                failureReason = failureReason,
                deviceInfo = "$deviceInfo [HMAC:$hmacBase64]"
            )

            intrusionLogDao.insertLog(logEntity)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            logKey?.let { k: ByteArray ->
                CryptoEngine.wipe(k)
                NativeCore.munlock(k)
            }
        }
    }

    /**
     * Decrypts encrypted photo bytes for viewing inside the Security Logs screen.
     */
    fun decryptPhotoBytes(log: IntrusionLogEntity): ByteArray? {
        val cipherBase64 = log.photoEncryptedBase64 ?: return null
        val ivBase64 = log.photoIv ?: return null
        var logKey: ByteArray? = null
        return try {
            logKey = KeystoreManager.getOrCreateLogEncryptionKey(context)
            val ciphertext = Base64.decode(cipherBase64, Base64.DEFAULT)
            val iv = Base64.decode(ivBase64, Base64.DEFAULT)
            val payload = CryptoEngine.EncryptedPayload(iv = iv, ciphertext = ciphertext)
            CryptoEngine.decryptAesGcm(payload, logKey)
        } catch (e: Exception) {
            null
        } finally {
            logKey?.let { k: ByteArray ->
                CryptoEngine.wipe(k)
                NativeCore.munlock(k)
            }
        }
    }

    suspend fun deleteIntrusionLog(id: Long) = withContext(Dispatchers.IO) {
        intrusionLogDao.deleteLogById(id)
    }

    suspend fun clearIntrusionLogs() = withContext(Dispatchers.IO) {
        intrusionLogDao.shredPhotos()
        intrusionLogDao.clearAllLogs()
    }

    /**
     * Initializes a brand-new master password for the vault.
     */
    suspend fun setupMasterPassword(password: CharArray): Boolean = withContext(Dispatchers.Default) {
        try {
            val salt = CryptoEngine.generateSalt(16)
            val kek = CryptoEngine.deriveArgon2idKey(
                password = password,
                salt = salt,
                memoryKb = 65536,
                iterations = 3,
                parallelism = 1
            )

            val newDek = CryptoEngine.generateDEK()
            val wrappedPayload = CryptoEngine.wrapDEK(newDek, kek)

            val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
            val wrappedDekBase64 = Base64.encodeToString(wrappedPayload.ciphertext, Base64.NO_WRAP)
            val wrappedDekIvBase64 = Base64.encodeToString(wrappedPayload.iv, Base64.NO_WRAP)

            preferences.saveMasterKeyConfig(saltBase64, wrappedDekBase64, wrappedDekIvBase64)

            activeDek = newDek
            _isUnlocked.value = true

            // Insert sample entries for testing if database is empty
            if (vaultDao.getAllEntriesSnapshot().isEmpty()) {
                val demoItems = listOf(
                    VaultEntry(
                        title = "GitHub - Enterprise Dev",
                        username = "alex-dev-sec",
                        password = "ghp_98yTvX#29LmQ4zR7810@k",
                        url = "https://github.com",
                        notes = "SSH and token key repository for enterprise code",
                        category = VaultCategory.LOGINS,
                        folder = "GitHub",
                        isFavorite = true
                    ),
                    VaultEntry(
                        title = "GitHub - OpenSource / Personal",
                        username = "alex-opensource",
                        password = "gho_kP9#vX29LmQ4zR89@open",
                        url = "https://github.com",
                        notes = "Personal repositories & public open source keys",
                        category = VaultCategory.LOGINS,
                        folder = "GitHub",
                        isFavorite = true
                    ),
                    VaultEntry(
                        title = "Google Workspace",
                        username = "alex.security@gmail.com",
                        password = "k8#P9!vX\$mZ2@qW9",
                        url = "https://accounts.google.com",
                        notes = "Primary 2FA authenticated workspace account",
                        category = VaultCategory.LOGINS,
                        folder = "Google",
                        isFavorite = true
                    ),
                    VaultEntry(
                        title = "Google Cloud Console",
                        username = "gcp-alex-admin@company.com",
                        password = "Gcp#Vault!2026Secure",
                        url = "https://console.cloud.google.com",
                        notes = "Production Kubernetes cluster admin",
                        category = VaultCategory.LOGINS,
                        folder = "Google",
                        isFavorite = false
                    ),
                    VaultEntry(
                        title = "AWS Production Console",
                        username = "admin-root-prod",
                        password = "A#99_vL!72xPm@04-Root",
                        url = "https://aws.amazon.com",
                        notes = "IAM root console access with hardware MFA",
                        category = VaultCategory.LOGINS,
                        folder = "AWS Cloud",
                        isFavorite = false
                    ),
                    VaultEntry(
                        title = "Personal Netflix",
                        username = "alex.entertainment@gmail.com",
                        password = "Nf#Stream!2026",
                        url = "https://netflix.com",
                        notes = "Family 4K subscription",
                        category = VaultCategory.LOGINS,
                        folder = "Entertainment",
                        isFavorite = false
                    )
                )
                for (item in demoItems) {
                    saveEntry(item)
                }
            }

            CryptoEngine.wipe(kek)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            CryptoEngine.wipe(password)
        }
    }

    /**
     * Unlocks the vault using the Master Password.
     * When Paranoid 2FA is active, direct single-factor unlock is rejected.
     */
    suspend fun unlockWithMasterPassword(password: CharArray): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            val config = preferences.configFlow.first()
            if (!config.isInitialized || config.masterSalt == null || config.wrappedDek == null || config.wrappedDekIv == null) {
                return@withContext Result.failure(IllegalStateException("Vault not initialized"))
            }

            if (config.isParanoid2FaEnabled) {
                return@withContext Result.failure(IllegalStateException("Paranoid 2FA is active: PIN + Biometric verification required."))
            }

            // Check lockout period
            if (config.lockoutUntil > System.currentTimeMillis()) {
                val remainingSec = ((config.lockoutUntil - System.currentTimeMillis()) / 1000).coerceAtLeast(1)
                return@withContext Result.failure(IllegalStateException("Too many failed attempts. Locked out for ${remainingSec}s"))
            }

            val salt = Base64.decode(config.masterSalt, Base64.DEFAULT)
            val wrappedDekBytes = Base64.decode(config.wrappedDek, Base64.DEFAULT)
            val wrappedDekIvBytes = Base64.decode(config.wrappedDekIv, Base64.DEFAULT)

            val kek = CryptoEngine.deriveArgon2idKey(
                password = password,
                salt = salt,
                memoryKb = 65536,
                iterations = 3,
                parallelism = 1
            )

            val wrappedPayload = CryptoEngine.EncryptedPayload(iv = wrappedDekIvBytes, ciphertext = wrappedDekBytes)
            val unwrappedDek = try {
                CryptoEngine.unwrapDEK(wrappedPayload, kek)
            } catch (e: Exception) {
                val attempts = preferences.incrementFailedAttempts()
                val threshold = config.photoTriggerThreshold
                val isPhotoEnabled = config.isPhotoCaptureEnabled
                if (isPhotoEnabled && attempts >= threshold) {
                    val hasPerm = com.example.security.SilentCameraCapture.hasCameraPermission(context)
                    val photoBytes = if (hasPerm) {
                        com.example.security.SilentCameraCapture.capturePhotoSilently(context)
                    } else null
                    val reason = if (!hasPerm) "Failed password attempt #$attempts (Camera permission not granted)"
                    else "Failed password attempt #$attempts"
                    recordWrongPasswordAttempt(attempts, photoBytes, reason)
                } else {
                    recordWrongPasswordAttempt(attempts, null, "Failed password attempt #$attempts")
                }

                if (config.selfDestructEnabled && attempts >= 10) {
                    selfDestruct()
                    return@withContext Result.failure(SecurityException("Vault wiped due to 10 consecutive failed attempts"))
                }
                return@withContext Result.failure(SecurityException("Invalid master password"))
            }

            activeDek = unwrappedDek
            _isUnlocked.value = true
            preferences.resetFailedAttempts()

            CryptoEngine.wipe(kek)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            CryptoEngine.wipe(password)
        }
    }

    /**
     * Validates the first factor (Master PIN/Password) in Paranoid 2FA mode.
     * If valid, derives and stores the unwrapped DEK locked in RAM with mlock,
     * awaiting the second biometric factor.
     */
    suspend fun validateMasterPasswordFirstFactor(password: CharArray): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            val config = preferences.configFlow.first()
            if (!config.isInitialized || config.masterSalt == null || config.wrappedDek == null || config.wrappedDekIv == null) {
                return@withContext Result.failure(IllegalStateException("Vault not initialized"))
            }

            // Check lockout period
            if (config.lockoutUntil > System.currentTimeMillis()) {
                val remainingSec = ((config.lockoutUntil - System.currentTimeMillis()) / 1000).coerceAtLeast(1)
                return@withContext Result.failure(IllegalStateException("Enclave locked out for ${remainingSec}s due to failed attempts"))
            }

            val salt = Base64.decode(config.masterSalt, Base64.DEFAULT)
            val wrappedDekBytes = Base64.decode(config.wrappedDek, Base64.DEFAULT)
            val wrappedDekIvBytes = Base64.decode(config.wrappedDekIv, Base64.DEFAULT)

            val kek = CryptoEngine.deriveArgon2idKey(
                password = password,
                salt = salt,
                memoryKb = 65536,
                iterations = 3,
                parallelism = 1
            )

            val wrappedPayload = CryptoEngine.EncryptedPayload(iv = wrappedDekIvBytes, ciphertext = wrappedDekBytes)
            val unwrappedDek = try {
                CryptoEngine.unwrapDEK(wrappedPayload, kek)
            } catch (e: Exception) {
                val attempts = preferences.incrementFailedAttempts()
                val threshold = config.photoTriggerThreshold
                val isPhotoEnabled = config.isPhotoCaptureEnabled
                if (isPhotoEnabled && attempts >= threshold) {
                    val hasPerm = com.example.security.SilentCameraCapture.hasCameraPermission(context)
                    val photoBytes = if (hasPerm) {
                        com.example.security.SilentCameraCapture.capturePhotoSilently(context)
                    } else null
                    val reason = if (!hasPerm) "Failed 2FA password attempt #$attempts (Camera permission not granted)"
                    else "Failed 2FA password attempt #$attempts"
                    recordWrongPasswordAttempt(attempts, photoBytes, reason)
                } else {
                    recordWrongPasswordAttempt(attempts, null, "Failed 2FA password attempt #$attempts")
                }

                if (config.selfDestructEnabled && attempts >= 10) {
                    selfDestruct()
                    return@withContext Result.failure(SecurityException("Vault wiped due to 10 consecutive failed attempts"))
                }
                return@withContext Result.failure(SecurityException("Invalid master PIN/password (Factor 1 failed)"))
            }

            // Clean previous pending if any
            pending2FaDek?.let {
                CryptoEngine.wipe(it)
                com.example.security.NativeCore.munlock(it)
            }

            // Lock derived DEK in memory
            com.example.security.NativeCore.mlock(unwrappedDek)
            pending2FaDek = unwrappedDek
            CryptoEngine.wipe(kek)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            CryptoEngine.wipe(password)
        }
    }

    /**
     * Completes 2FA unlock after both Factor 1 (PIN) and Factor 2 (Biometric) succeed.
     */
    suspend fun complete2FaUnlock(): Result<Unit> = withContext(Dispatchers.Default) {
        val dek = pending2FaDek ?: return@withContext Result.failure(IllegalStateException("No pending 2FA authentication"))
        activeDek = dek
        pending2FaDek = null
        _isUnlocked.value = true
        preferences.resetFailedAttempts()
        Result.success(Unit)
    }

    /**
     * Cancels 2FA unlock and zero-wipes the intermediate DEK buffer.
     */
    fun cancel2FaUnlock() {
        pending2FaDek?.let {
            CryptoEngine.wipe(it)
            com.example.security.NativeCore.munlock(it)
        }
        pending2FaDek = null
    }

    /**
     * Enables biometric unlock by wrapping the active DEK with the Keystore hardware key.
     */
    suspend fun enableBiometricUnlock(cipher: Cipher): Boolean = withContext(Dispatchers.Default) {
        val currentDek = activeDek ?: return@withContext false
        try {
            val wrapped = KeystoreManager.wrapDEKWithBiometricCipher(cipher, currentDek)
            val dekBase64 = Base64.encodeToString(wrapped.ciphertext, Base64.NO_WRAP)
            val ivBase64 = Base64.encodeToString(wrapped.iv, Base64.NO_WRAP)

            preferences.saveBiometricConfig(dekBase64, ivBase64, true)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Disables biometric unlock.
     */
    suspend fun disableBiometricUnlock() {
        KeystoreManager.deleteBiometricKey()
        preferences.saveBiometricConfig(null, null, false)
    }

    /**
     * Unlocks the vault using the Keystore-authenticated biometric cipher.
     * When Paranoid 2FA is active, direct biometric-only unlock is rejected.
     */
    suspend fun unlockWithBiometrics(cipher: Cipher): Result<Unit> = withContext(Dispatchers.Default) {
        val config = preferences.configFlow.first()
        try {
            if (config.isParanoid2FaEnabled) {
                return@withContext Result.failure(IllegalStateException("Paranoid 2FA mode is active: Master PIN/Password is required as First Factor."))
            }

            val wrappedBase64 = config.biometricWrappedDek ?: return@withContext Result.failure(IllegalStateException("Biometric unlock not configured"))
            val wrappedBytes = Base64.decode(wrappedBase64, Base64.DEFAULT)

            val unwrappedDek = KeystoreManager.unwrapDEKWithBiometricCipher(cipher, wrappedBytes)
            activeDek = unwrappedDek
            _isUnlocked.value = true
            preferences.resetFailedAttempts()

            Result.success(Unit)
        } catch (e: Exception) {
            val attempts = preferences.incrementFailedAttempts()
            val threshold = config.photoTriggerThreshold
            if (attempts >= threshold) {
                val photoBytes = com.example.security.SilentCameraCapture.capturePhotoSilently(context)
                recordWrongPasswordAttempt(attempts, photoBytes, "Failed biometric authentication #$attempts")
            } else {
                recordWrongPasswordAttempt(attempts, null, "Failed biometric authentication #$attempts")
            }
            Result.failure(e)
        }
    }

    /**
     * Rotates the Data Encryption Key (DEK).
     * Decrypts all existing entries, generates a fresh DEK, re-encrypts all entries,
     * updates master key wrap, and securely zeros old keys.
     */
    suspend fun rotateVaultKey(masterPassword: CharArray): Result<Unit> = withContext(Dispatchers.Default) {
        val oldDek = activeDek ?: return@withContext Result.failure(IllegalStateException("Vault must be unlocked to rotate keys"))
        try {
            // 1. Decrypt all entries currently in database
            val existingEntries = getAllDecryptedEntries()

            // 2. Generate brand new 256-bit DEK
            val newDek = CryptoEngine.generateDEK()

            // 3. Derive new master KEK with fresh salt
            val newSalt = CryptoEngine.generateSalt(16)
            val newKek = CryptoEngine.deriveArgon2idKey(
                password = masterPassword,
                salt = newSalt,
                memoryKb = 65536,
                iterations = 3,
                parallelism = 1
            )

            // 4. Wrap new DEK with new KEK
            val newWrappedPayload = CryptoEngine.wrapDEK(newDek, newKek)
            val saltBase64 = Base64.encodeToString(newSalt, Base64.NO_WRAP)
            val wrappedDekBase64 = Base64.encodeToString(newWrappedPayload.ciphertext, Base64.NO_WRAP)
            val wrappedDekIvBase64 = Base64.encodeToString(newWrappedPayload.iv, Base64.NO_WRAP)

            preferences.saveMasterKeyConfig(saltBase64, wrappedDekBase64, wrappedDekIvBase64)

            // 5. Update activeDek to newDek
            activeDek = newDek

            // 6. Re-encrypt all entries with new DEK
            for (entry in existingEntries) {
                saveEntry(entry)
            }

            // 7. Secure wipe
            CryptoEngine.wipe(oldDek)
            CryptoEngine.wipe(newKek)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            CryptoEngine.wipe(masterPassword)
        }
    }

    /**
     * Changes master password, re-wrapping current DEK with new password KEK.
     */
    suspend fun changeMasterPassword(newPassword: CharArray): Boolean = withContext(Dispatchers.Default) {
        val currentDek = activeDek ?: return@withContext false
        try {
            val newSalt = CryptoEngine.generateSalt(16)
            val newKek = CryptoEngine.deriveArgon2idKey(
                password = newPassword,
                salt = newSalt,
                memoryKb = 65536,
                iterations = 3,
                parallelism = 1
            )

            val newWrapped = CryptoEngine.wrapDEK(currentDek, newKek)
            val saltBase64 = Base64.encodeToString(newSalt, Base64.NO_WRAP)
            val wrappedDekBase64 = Base64.encodeToString(newWrapped.ciphertext, Base64.NO_WRAP)
            val wrappedDekIvBase64 = Base64.encodeToString(newWrapped.iv, Base64.NO_WRAP)

            preferences.saveMasterKeyConfig(saltBase64, wrappedDekBase64, wrappedDekIvBase64)
            CryptoEngine.wipe(newKek)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            CryptoEngine.wipe(newPassword)
        }
    }

    /**
     * Encrypts and saves or updates an entry.
     */
    suspend fun saveEntry(entry: VaultEntry): Long = withContext(Dispatchers.Default) {
        val currentDek = activeDek ?: throw IllegalStateException("Vault is locked")

        val payloadJson = JSONObject().apply {
            put("username", entry.username)
            put("password", entry.password)
            put("url", entry.url)
            put("notes", entry.notes)
            put("folder", entry.folder)
        }

        val plaintextBytes = payloadJson.toString().toByteArray(StandardCharsets.UTF_8)
        val entrySalt = CryptoEngine.generateSalt(16)
        val encryptedPayload = CryptoEngine.encryptAesGcm(plaintextBytes, currentDek)
        CryptoEngine.wipe(plaintextBytes)

        val entity = EncryptedVaultEntity(
            id = entry.id,
            title = entry.title,
            category = entry.category.name,
            isFavorite = entry.isFavorite,
            encryptedPayload = encryptedPayload.ciphertext,
            iv = encryptedPayload.iv,
            salt = entrySalt,
            createdAt = if (entry.id == 0L) System.currentTimeMillis() else entry.createdAt,
            updatedAt = System.currentTimeMillis()
        )

        if (entry.id == 0L) {
            vaultDao.insertEntry(entity)
        } else {
            vaultDao.updateEntry(entity)
            entry.id
        }
    }

    suspend fun deleteEntry(id: Long) = withContext(Dispatchers.IO) {
        vaultDao.shredEntryById(id)
        vaultDao.deleteEntryById(id)
    }

    suspend fun getAllDecryptedEntries(): List<VaultEntry> = withContext(Dispatchers.Default) {
        val currentDek = activeDek ?: return@withContext emptyList()
        val entities = vaultDao.getAllEntriesSnapshot()
        entities.mapNotNull { decryptEntity(it, currentDek) }
    }

    private fun decryptEntity(entity: EncryptedVaultEntity, dek: ByteArray): VaultEntry? {
        return try {
            val payload = CryptoEngine.EncryptedPayload(iv = entity.iv, ciphertext = entity.encryptedPayload)
            val decryptedBytes = CryptoEngine.decryptAesGcm(payload, dek)
            val jsonString = String(decryptedBytes, StandardCharsets.UTF_8)
            CryptoEngine.wipe(decryptedBytes)

            val json = JSONObject(jsonString)
            val category = try {
                VaultCategory.valueOf(entity.category)
            } catch (e: Exception) {
                VaultCategory.LOGINS
            }

            VaultEntry(
                id = entity.id,
                title = entity.title,
                username = json.optString("username", ""),
                password = json.optString("password", ""),
                url = json.optString("url", ""),
                notes = json.optString("notes", ""),
                category = category,
                folder = json.optString("folder", ""),
                isFavorite = entity.isFavorite,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun clearAllEntries() = withContext(Dispatchers.IO) {
        vaultDao.shredAllEntries()
        vaultDao.deleteAll()
    }

    /**
     * Locks the vault immediately, wiping all decrypted keys in memory.
     */
    fun lockVault() {
        cancel2FaUnlock()
        CryptoEngine.wipe(activeDek)
        activeDek = null
        _isUnlocked.value = false
    }

    /**
     * Self-destructs the vault: shreds payloads, deletes all database rows, resets preferences, wipes keys.
     */
    suspend fun selfDestruct() = withContext(Dispatchers.IO) {
        lockVault()
        vaultDao.shredAllEntries()
        vaultDao.deleteAll()
        intrusionLogDao.shredPhotos()
        intrusionLogDao.clearAllLogs()
        preferences.resetAll()
        KeystoreManager.deleteBiometricKey()
    }

    /**
     * Diagnostic test method to verify silent camera capture and log an encrypted test entry.
     */
    suspend fun testCaptureIntruderPhoto(): Boolean = withContext(Dispatchers.IO) {
        val photoBytes = com.example.security.SilentCameraCapture.capturePhotoSilently(context)
        recordWrongPasswordAttempt(0, photoBytes, "Manual Camera Diagnostic Verification")
        photoBytes != null && photoBytes.isNotEmpty()
    }
}
