package com.ntoprevd.cogno.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ntoprevd.cogno.data.db.entity.NoteTopicSegmentEntity
import com.ntoprevd.cogno.data.db.entity.TopicEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicDao {
    @Query("SELECT * FROM topics ORDER BY pinned DESC, is_builtin DESC, created_at ASC")
    suspend fun getAllTopicsForExport(): List<TopicEntity>

    @Query("SELECT * FROM note_topic_segments ORDER BY created_at ASC, position ASC")
    suspend fun getAllSegmentsForExport(): List<NoteTopicSegmentEntity>

    @Query("SELECT * FROM topics ORDER BY pinned DESC, is_builtin DESC, created_at ASC")
    fun observeTopics(): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics WHERE enabled = 1 ORDER BY is_builtin DESC, created_at ASC")
    suspend fun getEnabledTopics(): List<TopicEntity>

    @Query("SELECT COUNT(*) FROM topics")
    suspend fun countTopics(): Int

    @Query("SELECT * FROM topics WHERE id = :id LIMIT 1")
    suspend fun getTopic(id: String): TopicEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopics(topics: List<TopicEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: TopicEntity)

    @Update
    suspend fun updateTopic(topic: TopicEntity)

    @Query("DELETE FROM topics WHERE id = :id")
    suspend fun deleteTopic(id: String)

    @Query("DELETE FROM topics WHERE is_builtin = 1")
    suspend fun deleteBuiltInTopics()

    @Query("DELETE FROM topics")
    suspend fun deleteAllTopics()

    @Query("SELECT * FROM note_topic_segments ORDER BY created_at DESC, position ASC")
    fun observeSegments(): Flow<List<NoteTopicSegmentEntity>>

    @Query("SELECT id FROM note_topic_segments WHERE note_id = :noteId")
    suspend fun getSegmentIdsForNote(noteId: String): List<String>

    @Query("SELECT * FROM note_topic_segments WHERE topic_name = :topicName ORDER BY created_at DESC, position ASC")
    suspend fun getSegmentsForTopic(topicName: String): List<NoteTopicSegmentEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSegments(segments: List<NoteTopicSegmentEntity>)

    @Query("DELETE FROM note_topic_segments WHERE note_id = :noteId")
    suspend fun deleteSegmentsForNote(noteId: String)

    @Query("DELETE FROM note_topic_segments WHERE topic_name = :topicName")
    suspend fun deleteSegmentsForTopic(topicName: String)
}
