package com.ntoprevd.cogno.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ntoprevd.cogno.data.db.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query(
        "SELECT * FROM messages " +
            "WHERE session_id = :sessionId " +
            "ORDER BY created_at ASC"
    )
    fun observeMessagesBySessionId(sessionId: String): Flow<List<MessageEntity>>

    @Query(
        "SELECT * FROM messages " +
            "WHERE session_id = :sessionId " +
            "ORDER BY created_at ASC " +
            "LIMIT :limit OFFSET :offset"
    )
    suspend fun getMessagesBySessionId(sessionId: String, limit: Int, offset: Int): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :id LIMIT 1")
    suspend fun getMessageById(id: String): MessageEntity?

    @Query(
        "SELECT * FROM messages " +
            "WHERE session_id = :sessionId AND created_at < :createdBefore " +
            "ORDER BY created_at ASC"
    )
    suspend fun getMessagesBefore(sessionId: String, createdBefore: Long): List<MessageEntity>

    @Query(
        "SELECT * FROM messages " +
            "WHERE session_id = :sessionId " +
            "ORDER BY created_at DESC " +
            "LIMIT 1"
    )
    suspend fun getLatestMessageBySessionId(sessionId: String): MessageEntity?

    @Query(
        "SELECT * FROM messages " +
            "WHERE session_id = :sessionId AND role = 'assistant' AND created_at > :createdAfter " +
            "ORDER BY created_at ASC " +
            "LIMIT 1"
    )
    suspend fun getNextAssistantMessage(sessionId: String, createdAfter: Long): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessageById(id: String)
}
