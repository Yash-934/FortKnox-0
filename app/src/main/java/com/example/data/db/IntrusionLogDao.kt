package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.IntrusionLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IntrusionLogDao {

    @Query("SELECT * FROM intrusion_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<IntrusionLogEntity>>

    @Query("SELECT * FROM intrusion_logs ORDER BY timestamp DESC")
    suspend fun getAllLogsSnapshot(): List<IntrusionLogEntity>

    @Query("SELECT COUNT(*) FROM intrusion_logs")
    suspend fun getLogCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: IntrusionLogEntity): Long

    @Query("UPDATE intrusion_logs SET photoEncryptedBase64 = '', photoIv = '', photoAvailable = 0")
    suspend fun shredPhotos()

    @Query("DELETE FROM intrusion_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)

    @Query("DELETE FROM intrusion_logs")
    suspend fun clearAllLogs()
}
