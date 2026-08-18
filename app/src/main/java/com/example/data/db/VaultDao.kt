package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.EncryptedVaultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {

    @Query("SELECT * FROM vault_entries ORDER BY isFavorite DESC, updatedAt DESC")
    fun getAllEntries(): Flow<List<EncryptedVaultEntity>>

    @Query("SELECT * FROM vault_entries WHERE id = :id LIMIT 1")
    suspend fun getEntryById(id: Long): EncryptedVaultEntity?

    @Query("SELECT * FROM vault_entries")
    suspend fun getAllEntriesSnapshot(): List<EncryptedVaultEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entity: EncryptedVaultEntity): Long

    @Update
    suspend fun updateEntry(entity: EncryptedVaultEntity)

    @Query("UPDATE vault_entries SET encryptedPayload = zeroblob(length(encryptedPayload)), iv = zeroblob(12), salt = zeroblob(16) WHERE id = :id")
    suspend fun shredEntryById(id: Long)

    @Query("UPDATE vault_entries SET encryptedPayload = zeroblob(length(encryptedPayload)), iv = zeroblob(12), salt = zeroblob(16), title = '', category = ''")
    suspend fun shredAllEntries()

    @Query("DELETE FROM vault_entries WHERE id = :id")
    suspend fun deleteEntryById(id: Long)

    @Query("DELETE FROM vault_entries")
    suspend fun deleteAll()
}
