package com.ntoprevd.cogno.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ntoprevd.cogno.data.db.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY pinned DESC, updated_at DESC")
    fun observeAllSessionsOrderByUpdatedAtDesc(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions ORDER BY pinned DESC, updated_at DESC")
    suspend fun getAllSessionsOrderByUpdatedAtDesc(): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: String): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<SessionEntity>)

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Delete
    suspend fun deleteSession(session: SessionEntity)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSessionById(id: String)
}
