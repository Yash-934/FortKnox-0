package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "intrusion_logs")
data class IntrusionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val attemptNumber: Int,
    val status: String, // "WRONG_PASSWORD", "PHOTO_CAPTURED", "LOCKOUT_ENFORCED", "SELF_DESTRUCT_TRIGGERED"
    val photoEncryptedBase64: String? = null,
    val photoIv: String? = null,
    val photoAvailable: Boolean = false,
    val failureReason: String = "Incorrect master credential entered",
    val deviceInfo: String = ""
)
