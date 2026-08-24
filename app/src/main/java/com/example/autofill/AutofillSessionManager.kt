package com.example.autofill

import com.example.security.CryptoEngine
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Ephemeral In-Memory Session Token Manager for Autofill Service.
 *
 * Eliminates transmission of plaintext passwords through Android Intent extras,
 * mitigating Intent sniffing and process-memory leakage risks.
 */
object AutofillSessionManager {

    private const val DEFAULT_TTL_MS = 60_000L // 60 seconds

    data class SaveSession(
        val token: String,
        val domain: String,
        val packageName: String,
        val username: String,
        val passwordChars: CharArray,
        val createdAt: Long = System.currentTimeMillis()
    ) {
        fun wipe() {
            CryptoEngine.wipe(passwordChars)
        }
    }

    private val activeSessions = ConcurrentHashMap<String, SaveSession>()

    /**
     * Creates an ephemeral token for a save request.
     */
    fun createSaveSession(
        domain: String,
        packageName: String,
        username: String,
        password: String
    ): String {
        cleanupExpiredSessions()
        val token = UUID.randomUUID().toString()
        val session = SaveSession(
            token = token,
            domain = domain,
            packageName = packageName,
            username = username,
            passwordChars = password.toCharArray()
        )
        activeSessions[token] = session
        return token
    }

    /**
     * Consumes and removes a save session (One-time use).
     */
    fun consumeSaveSession(token: String): SaveSession? {
        cleanupExpiredSessions()
        val session = activeSessions.remove(token) ?: return null
        if (System.currentTimeMillis() - session.createdAt >= DEFAULT_TTL_MS) {
            session.wipe()
            return null
        }
        return session
    }

    /**
     * Prunes expired session tokens and wipes their memory buffers.
     */
    fun cleanupExpiredSessions(maxAgeMs: Long = DEFAULT_TTL_MS) {
        val now = System.currentTimeMillis()
        val iterator = activeSessions.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value.createdAt >= maxAgeMs) {
                entry.value.wipe()
                iterator.remove()
            }
        }
    }
}
