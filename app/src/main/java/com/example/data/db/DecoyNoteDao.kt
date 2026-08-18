package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DecoyNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DecoyNoteDao {

    @Query("SELECT * FROM decoy_notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<DecoyNoteEntity>>

    @Query("SELECT * FROM decoy_notes ORDER BY updatedAt DESC")
    suspend fun getAllNotesSnapshot(): List<DecoyNoteEntity>

    @Query("SELECT * FROM decoy_notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: Long): DecoyNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: DecoyNoteEntity): Long

    @Update
    suspend fun updateNote(note: DecoyNoteEntity)

    @Query("DELETE FROM decoy_notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    @Query("DELETE FROM decoy_notes")
    suspend fun deleteAllNotes()
}
