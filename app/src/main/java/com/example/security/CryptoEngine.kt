package com.example.security

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Military-grade Cryptographic Engine.
 *
 * Implements:
 * - Argon2id KDF (RFC 9106) for deriving Key Encryption Keys (KEK) from master passwords.
 *   Default parameters: Memory = 64 MiB (65536 KiB), Iterations = 3, Parallelism = 1.
 * - AES-256-GCM (Authenticated Encryption with Associated Data) for encrypting vault payloads and keys.
 * - Secure zero-filling of memory buffers (ByteArray, CharArray).
 * - Zero-knowledge key wrapping for Data Encryption Keys (DEK).
 */
object CryptoEngine {

    private const val GCM_IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val AES_KEY_LENGTH_BYTES = 32 // 256-bit AES
    private const val SALT_LENGTH_BYTES = 16

    private val secureRandom = SecureRandom()

    data class EncryptedPayload(
        val iv: ByteArray,
        val ciphertext: ByteArray,
        val salt: ByteArray? = null
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is EncryptedPayload) return false
            if (!iv.contentEquals(other.iv)) return false
            if (!ciphertext.contentEquals(other.ciphertext)) return false
            if (salt != null) {
                if (other.salt == null || !salt.contentEquals(other.salt)) return false
            } else if (other.salt != null) return false
            return true
        }

        override fun hashCode(): Int {
            var result = iv.contentHashCode()
            result = 31 * result + ciphertext.contentHashCode()
            result = 31 * result + (salt?.contentHashCode() ?: 0)
            return result
        }

        fun wipe() {
            iv.fill(0)
            ciphertext.fill(0)
            salt?.fill(0)
        }
    }

    /**
     * Generates a cryptographically strong random salt.
     */
    fun generateSalt(length: Int = SALT_LENGTH_BYTES): ByteArray {
        val salt = ByteArray(length)
        secureRandom.nextBytes(salt)
        return salt
    }

    /**
     * Generates a 256-bit random AES Data Encryption Key (DEK).
     */
    fun generateDEK(): ByteArray {
        val dek = ByteArray(AES_KEY_LENGTH_BYTES)
        secureRandom.nextBytes(dek)
        return dek
    }

    /**
     * Derives a 256-bit Key Encryption Key (KEK) from a password using Argon2id.
     * Memory: 64 MiB (65536 KiB), Iterations: 3, Parallelism: 1
     */
    fun deriveArgon2idKey(
        password: CharArray,
        salt: ByteArray,
        memoryKb: Int = 65536,
        iterations: Int = 3,
        parallelism: Int = 1,
        keyLengthBytes: Int = AES_KEY_LENGTH_BYTES
    ): ByteArray {
        val passwordBytes = charArrayToByteArray(password)
        val result = ByteArray(keyLengthBytes)
        try {
            val params = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withMemoryAsKB(memoryKb)
                .withIterations(iterations)
                .withParallelism(parallelism)
                .withSalt(salt)
                .build()

            val generator = Argon2BytesGenerator()
            generator.init(params)
            generator.generateBytes(passwordBytes, result, 0, result.size)
            return result
        } finally {
            wipe(passwordBytes)
        }
    }

    /**
     * Encrypts plaintext bytes using AES-256-GCM.
     */
    fun encryptAesGcm(
        plaintext: ByteArray,
        key: ByteArray,
        associatedData: ByteArray? = null
    ): EncryptedPayload {
        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

        if (associatedData != null && associatedData.isNotEmpty()) {
            cipher.updateAAD(associatedData)
        }

        val ciphertext = cipher.doFinal(plaintext)
        return EncryptedPayload(iv = iv, ciphertext = ciphertext)
    }

    /**
     * Decrypts ciphertext bytes using AES-256-GCM.
     */
    fun decryptAesGcm(
        payload: EncryptedPayload,
        key: ByteArray,
        associatedData: ByteArray? = null
    ): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, payload.iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)

        if (associatedData != null && associatedData.isNotEmpty()) {
            cipher.updateAAD(associatedData)
        }

        return cipher.doFinal(payload.ciphertext)
    }

    /**
     * Wraps the Vault DEK using the KEK derived from Argon2id.
     */
    fun wrapDEK(dek: ByteArray, kek: ByteArray): EncryptedPayload {
        return encryptAesGcm(dek, kek, "AEGIS_VAULT_DEK_WRAP".toByteArray(StandardCharsets.UTF_8))
    }

    /**
     * Unwraps the Vault DEK using the KEK derived from Argon2id.
     */
    fun unwrapDEK(wrappedDek: EncryptedPayload, kek: ByteArray): ByteArray {
        return decryptAesGcm(wrappedDek, kek, "AEGIS_VAULT_DEK_WRAP".toByteArray(StandardCharsets.UTF_8))
    }

    /**
     * Converts CharArray to UTF-8 ByteArray securely.
     */
    fun charArrayToByteArray(chars: CharArray): ByteArray {
        val byteBuffer = StandardCharsets.UTF_8.encode(java.nio.CharBuffer.wrap(chars))
        val bytes = ByteArray(byteBuffer.remaining())
        byteBuffer.get(bytes)
        return bytes
    }

    /**
     * Securely zero-fills a byte array to minimize plaintext residue in memory.
     */
    fun wipe(bytes: ByteArray?) {
        bytes?.fill(0)
    }

    /**
     * Securely zero-fills a char array to minimize sensitive residue in memory.
     */
    fun wipe(chars: CharArray?) {
        chars?.fill('\u0000')
    }
}
