package com.ntoprevd.cogno.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["source_session_id"]),
        Index(value = ["pinned", "updated_at"])
    ]
)
class NoteEntity(
    @JvmField
    @PrimaryKey
    @ColumnInfo(name = "id")
    var id: String,

    @JvmField
    @ColumnInfo(name = "title")
    var title: String,

    @JvmField
    @ColumnInfo(name = "content")
    var content: String,

    @JvmField
    @ColumnInfo(name = "preview")
    var preview: String,

    @JvmField
    @ColumnInfo(name = "source_session_id")
    var sourceSessionId: String?,

    @JvmField
    @ColumnInfo(name = "source_message_count")
    var sourceMessageCount: Int,

    @JvmField
    @ColumnInfo(name = "pinned")
    var pinned: Boolean,

    @JvmField
    @ColumnInfo(name = "created_at")
    var createdAt: Long,

    @JvmField
    @ColumnInfo(name = "updated_at")
    var updatedAt: Long
)
