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
    val folder: String = "",
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Resolves the effective folder name for grouping.
     * If user explicitly assigned a folder, uses that; otherwise infers smartly from title/url/category.
     */
    fun getEffectiveFolder(): String {
        if (folder.isNotBlank()) return folder.trim()
        val lowerTitle = title.lowercase()
        val lowerUrl = url.lowercase()
        return when {
            lowerTitle.contains("github") || lowerUrl.contains("github") -> "GitHub"
            lowerTitle.contains("google") || lowerUrl.contains("google") || lowerTitle.contains("gmail") -> "Google"
            lowerTitle.contains("aws") || lowerTitle.contains("amazon") || lowerUrl.contains("aws") -> "AWS Cloud"
            lowerTitle.contains("microsoft") || lowerTitle.contains("azure") || lowerUrl.contains("microsoft") || lowerTitle.contains("outlook") -> "Microsoft"
            lowerTitle.contains("netflix") || lowerTitle.contains("spotify") || lowerTitle.contains("youtube") || lowerUrl.contains("netflix") -> "Entertainment"
            lowerTitle.contains("crypto") || lowerTitle.contains("binance") || lowerTitle.contains("coinbase") || lowerTitle.contains("wallet") -> "Crypto"
            lowerTitle.contains("bank") || lowerTitle.contains("paypal") || lowerTitle.contains("stripe") -> "Finance"
            lowerTitle.contains("work") || lowerTitle.contains("enterprise") || lowerTitle.contains("slack") -> "Work"
            category == VaultCategory.CARDS -> "Payment Cards"
            category == VaultCategory.IDENTITY -> "Identity"
            category == VaultCategory.SECURE_NOTES -> "Secure Notes"
            else -> "Personal"
        }
    }
}
