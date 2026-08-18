package com.example.data.model

/**
 * In-memory decrypted representation of a vault item.
 */
data class VaultEntry(
    val id: Long = 0,
    val title: String,
    val username: String = "",
    val password: String = "",
    val url: String = "",
    val notes: String = "",
    val category: VaultCategory = VaultCategory.LOGINS,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
