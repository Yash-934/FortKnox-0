package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Database entity storing individual encrypted vault items.
 *
 * All sensitive credential payloads (username, password, notes, url) are encrypted
 * with AES-256-GCM using the user's random Data Encryption Key (DEK) and a unique per-entry IV.
 */
@Entity(tableName = "vault_entries")
data class EncryptedVaultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recordUid: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val category: String,
    val isFavorite: Boolean,
    val encryptedPayload: ByteArray,
    val iv: ByteArray,
    val salt: ByteArray,
    val createdAt: Long,
    val updatedAt: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncryptedVaultEntity) return false
        if (id != other.id) return false
        if (recordUid != other.recordUid) return false
        if (title != other.title) return false
        if (category != other.category) return false
        if (isFavorite != other.isFavorite) return false
        if (!encryptedPayload.contentEquals(other.encryptedPayload)) return false
        if (!iv.contentEquals(other.iv)) return false
        if (!salt.contentEquals(other.salt)) return false
        if (createdAt != other.createdAt) return false
        if (updatedAt != other.updatedAt) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + recordUid.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + category.hashCode()
        result = 31 * result + isFavorite.hashCode()
        result = 31 * result + encryptedPayload.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + salt.contentHashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + updatedAt.hashCode()
        return result
    }
}
