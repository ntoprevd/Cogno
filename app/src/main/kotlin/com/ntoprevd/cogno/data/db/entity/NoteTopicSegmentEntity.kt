package com.ntoprevd.cogno.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "note_topic_segments",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["note_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["note_id"]),
        Index(value = ["topic_name", "created_at"])
    ]
)
data class NoteTopicSegmentEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "note_id")
    val noteId: String,
    @ColumnInfo(name = "topic_name")
    val topicName: String,
    @ColumnInfo(name = "heading")
    val heading: String,
    @ColumnInfo(name = "content")
    val content: String,
    @ColumnInfo(name = "position")
    val position: Int,
    @ColumnInfo(name = "source_message_count")
    val sourceMessageCount: Int,
    @ColumnInfo(name = "created_at")
    val createdAt: Long
)
