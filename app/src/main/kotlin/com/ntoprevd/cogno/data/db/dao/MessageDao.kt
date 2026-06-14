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

    @Query(
        "SELECT * FROM (" +
            "SELECT * FROM messages WHERE session_id = :sessionId " +
            "ORDER BY created_at DESC LIMIT :limit" +
            ") ORDER BY created_at ASC"
    )
    suspend fun getRecentMessagesBySessionId(sessionId: String, limit: Int): List<MessageEntity>

    @Query(
        "SELECT * FROM messages " +
            "WHERE session_id = :sessionId AND status = 'completed' " +
            "AND (content != '' OR image_path IS NOT NULL) " +
            "ORDER BY created_at ASC"
    )
    suspend fun getCompletedMessagesForNote(sessionId: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :id LIMIT 1")
    suspend fun getMessageById(id: String): MessageEntity?

    @Query(
        "SELECT * FROM messages " +
            "WHERE session_id = :sessionId AND created_at < :createdBefore " +
            "ORDER BY created_at ASC"
    )
    suspend fun getMessagesBefore(sessionId: String, createdBefore: Long): List<MessageEntity>

    @Query(
        "SELECT * FROM (" +
            "SELECT * FROM messages " +
            "WHERE session_id = :sessionId AND created_at < :createdBefore " +
            "ORDER BY created_at DESC LIMIT :limit" +
            ") ORDER BY created_at ASC"
    )
    suspend fun getRecentMessagesBefore(
        sessionId: String,
        createdBefore: Long,
        limit: Int
    ): List<MessageEntity>

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

    @Query(
        "SELECT image_path FROM messages " +
            "WHERE session_id = :sessionId AND image_path IS NOT NULL"
    )
    suspend fun getImagePathsBySessionId(sessionId: String): List<String>

    @Query(
        "SELECT image_path FROM messages " +
            "WHERE session_id = :sessionId AND created_at > :createdAfter " +
            "AND image_path IS NOT NULL"
    )
    suspend fun getImagePathsAfter(sessionId: String, createdAfter: Long): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessageById(id: String)

    @Query(
        "DELETE FROM messages " +
            "WHERE session_id = :sessionId AND created_at > :createdAfter"
    )
    suspend fun deleteMessagesAfter(sessionId: String, createdAfter: Long)

    @Query(
        "UPDATE messages SET " +
            "status = 'failed', " +
            "error_code = 'Interrupted', " +
            "content = CASE WHEN content = '' THEN :emptyMessage ELSE content END, " +
            "updated_at = :updatedAt " +
            "WHERE role = 'assistant' AND status IN ('pending', 'streaming')"
    )
    suspend fun markInterruptedAssistantMessagesFailed(
        emptyMessage: String,
        updatedAt: Long
    ): Int

    @Query(
        "SELECT COALESCE(SUM(token_count), 0) FROM messages " +
            "WHERE role = 'assistant' AND status = 'completed' " +
            "AND created_at >= :monthStart AND token_count IS NOT NULL"
    )
    fun observeRecordedTokensSince(monthStart: Long): Flow<Long>

    @Query(
        "SELECT COUNT(*) FROM messages " +
            "WHERE role = 'assistant' AND status = 'completed' " +
            "AND created_at >= :monthStart AND token_count IS NOT NULL"
    )
    fun observeRecordedRequestsSince(monthStart: Long): Flow<Int>
}
