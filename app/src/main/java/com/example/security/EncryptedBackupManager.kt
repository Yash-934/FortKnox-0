package com.example.security

import android.content.Context
import android.util.Base64
import com.example.data.model.VaultEntry
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import javax.crypto.AEADBadTagException

/**
 * Encrypted Backup & Restore Manager (Phase 3 Hardened).
 *
 * Implements:
 * - Separate backup password derivation with Argon2id (16-byte salt, 64MB memory, 3 iterations).
 * - Full vault encryption using AES-256-GCM with 12-byte random nonce.
 * - Hardware Device-Bound backup option using Android Keystore StrongBox/TEE key.
 * - Authenticated ciphertext encapsulation (Base64 JSON envelope).
 * - Pre-restore Anti-Tamper & Root verification.
 * - AEADBadTagException handling on invalid password or corrupted data.
 * - Secure zero-wiping of intermediate buffers.
 */
object EncryptedBackupManager {

    private const val BACKUP_FORMAT_VERSION = 2
    private const val KDF_ARGON2ID = "argon2id"

    data class ExportData(
        val jsonPayload: String
    )

    /**
     * Exports a list of vault entries into an encrypted backup JSON string.
     * Supports optional device binding via Android Keystore.
     */
    fun createEncryptedBackup(
        context: Context,
        entries: List<VaultEntry>,
        backupPassword: CharArray,
        isDeviceBound: Boolean = false
    ): String {
        val rootJson = JSONObject()
        val entriesArray = JSONArray()

        for (entry in entries) {
            val item = JSONObject()
            item.put("id", entry.id)
            item.put("title", entry.title)
            item.put("username", entry.username)
            item.put("password", entry.password)
            item.put("url", entry.url)
            item.put("notes", entry.notes)
            item.put("category", entry.category.name)
            item.put("isFavorite", entry.isFavorite)
            item.put("createdAt", entry.createdAt)
            item.put("updatedAt", entry.updatedAt)
            entriesArray.put(item)
        }

        rootJson.put("entries", entriesArray)
        rootJson.put("exportedAt", System.currentTimeMillis())
        rootJson.put("app", "Fort Knox")

        val plaintextBytes = rootJson.toString().toByteArray(StandardCharsets.UTF_8)
        val salt = CryptoEngine.generateSalt(16)
        var backupKey: ByteArray? = null

        try {
            backupKey = CryptoEngine.deriveArgon2idKey(
                password = backupPassword,
                salt = salt,
                memoryKb = 65536,
                iterations = 3,
                parallelism = 1
            )

            var payload = CryptoEngine.encryptAesGcm(
                plaintext = plaintextBytes,
                key = backupKey,
                associatedData = "AEGIS_VAULT_BACKUP_V2".toByteArray(StandardCharsets.UTF_8)
            )

            var deviceIvBase64 = ""
            if (isDeviceBound) {
                // Wrap inner ciphertext with hardware keystore key
                val hwWrapped = KeystoreManager.encryptWithDeviceKey(context, payload.ciphertext)
                payload = CryptoEngine.EncryptedPayload(iv = payload.iv, ciphertext = hwWrapped.ciphertext)
                deviceIvBase64 = Base64.encodeToString(hwWrapped.iv, Base64.NO_WRAP)
            }

            val backupEnvelope = JSONObject()
            backupEnvelope.put("version", BACKUP_FORMAT_VERSION)
            backupEnvelope.put("kdf", KDF_ARGON2ID)
            backupEnvelope.put("iterations", 3)
            backupEnvelope.put("memoryKb", 65536)
            backupEnvelope.put("isDeviceBound", isDeviceBound)
            if (isDeviceBound) {
                backupEnvelope.put("deviceIv", deviceIvBase64)
            }
            backupEnvelope.put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            backupEnvelope.put("nonce", Base64.encodeToString(payload.iv, Base64.NO_WRAP))
            backupEnvelope.put("ciphertext", Base64.encodeToString(payload.ciphertext, Base64.NO_WRAP))
            backupEnvelope.put("timestamp", System.currentTimeMillis())

            return backupEnvelope.toString(2)
        } finally {
            CryptoEngine.wipe(backupKey)
            CryptoEngine.wipe(plaintextBytes)
            CryptoEngine.wipe(backupPassword)
        }
    }

    /**
     * Exports a list of vault entries directly to an OutputStream (SAF compatible).
     * Zeroizes all memory buffers after write.
     */
    fun exportEncryptedBackupToStream(
        context: Context,
        outputStream: java.io.OutputStream,
        entries: List<VaultEntry>,
        backupPassword: CharArray,
        isDeviceBound: Boolean = false
    ) {
        val encryptedJson = createEncryptedBackup(
            context = context,
            entries = entries,
            backupPassword = backupPassword,
            isDeviceBound = isDeviceBound
        )
        val bytes = encryptedJson.toByteArray(StandardCharsets.UTF_8)
        try {
            outputStream.use { out ->
                out.write(bytes)
                out.flush()
            }
        } finally {
            CryptoEngine.wipe(bytes)
        }
    }

    /**
     * Restores vault entries from an InputStream (SAF compatible).
     */
    fun restoreEncryptedBackupFromStream(
        context: Context,
        inputStream: java.io.InputStream,
        backupPassword: CharArray
    ): List<VaultEntry> {
        val bytes = inputStream.use { it.readBytes() }
        val jsonString = String(bytes, StandardCharsets.UTF_8)
        try {
            return restoreEncryptedBackup(context, jsonString, backupPassword)
        } finally {
            CryptoEngine.wipe(bytes)
        }
    }

    /**
     * Restores vault entries from an encrypted backup JSON string.
     * Enforces Anti-Tampering verification and device binding checks.
     * Throws AEADBadTagException or SecurityException if verification fails.
     */
    fun restoreEncryptedBackup(
        context: Context,
        backupJsonString: String,
        backupPassword: CharArray
    ): List<VaultEntry> {
        // 1. Anti-Tamper & Root integrity pre-check
        val integrity = SecurityIntegrityChecker.performFullIntegrityAudit(context)
        if (integrity.isRooted || !integrity.isSignatureValid) {
            throw SecurityException("Backup restore aborted: Device security integrity compromised.")
        }

        var backupKey: ByteArray? = null
        var decryptedBytes: ByteArray? = null

        try {
            val envelope = JSONObject(backupJsonString)
            val version = envelope.optInt("version", 1)
            val isDeviceBound = envelope.optBoolean("isDeviceBound", false)
            val saltBase64 = envelope.getString("salt")
            val nonceBase64 = envelope.getString("nonce")
            val ciphertextBase64 = envelope.getString("ciphertext")
            val iterations = envelope.optInt("iterations", 3)
            val memoryKb = envelope.optInt("memoryKb", 65536)

            val salt = Base64.decode(saltBase64, Base64.DEFAULT)
            val nonce = Base64.decode(nonceBase64, Base64.DEFAULT)
            var ciphertext = Base64.decode(ciphertextBase64, Base64.DEFAULT)

            if (isDeviceBound) {
                val deviceIvBase64 = envelope.getString("deviceIv")
                val deviceIv = Base64.decode(deviceIvBase64, Base64.DEFAULT)
                // Unwrap hardware keystore layer
                val hwPayload = CryptoEngine.EncryptedPayload(iv = deviceIv, ciphertext = ciphertext)
                ciphertext = KeystoreManager.decryptWithDeviceKey(context, hwPayload)
            }

            backupKey = CryptoEngine.deriveArgon2idKey(
                password = backupPassword,
                salt = salt,
                memoryKb = memoryKb,
                iterations = iterations,
                parallelism = 1
            )

            val aad = if (version >= 2) "AEGIS_VAULT_BACKUP_V2" else "AEGIS_VAULT_BACKUP_V1"
            val payload = CryptoEngine.EncryptedPayload(iv = nonce, ciphertext = ciphertext)
            decryptedBytes = CryptoEngine.decryptAesGcm(
                payload = payload,
                key = backupKey,
                associatedData = aad.toByteArray(StandardCharsets.UTF_8)
            )

            val decryptedJsonString = String(decryptedBytes, StandardCharsets.UTF_8)
            val root = JSONObject(decryptedJsonString)
            val entriesArray = root.getJSONArray("entries")

            val resultList = mutableListOf<VaultEntry>()
            for (i in 0 until entriesArray.length()) {
                val item = entriesArray.getJSONObject(i)
                val categoryName = item.optString("category", "LOGINS")
                val category = try {
                    com.example.data.model.VaultCategory.valueOf(categoryName)
                } catch (e: Exception) {
                    com.example.data.model.VaultCategory.LOGINS
                }

                val entry = VaultEntry(
                    id = 0, // Fresh ID on restore
                    title = item.getString("title"),
                    username = item.optString("username", ""),
                    password = item.optString("password", ""),
                    url = item.optString("url", ""),
                    notes = item.optString("notes", ""),
                    category = category,
                    isFavorite = item.optBoolean("isFavorite", false),
                    createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = item.optLong("updatedAt", System.currentTimeMillis())
                )
                resultList.add(entry)
            }

            return resultList
        } finally {
            CryptoEngine.wipe(backupKey)
            CryptoEngine.wipe(decryptedBytes)
            CryptoEngine.wipe(backupPassword)
        }
    }
}
