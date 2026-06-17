package com.ntoprevd.cogno.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sessions",
    indices = [
        Index(value = ["updated_at"]),
        Index(value = ["pinned", "updated_at"])
    ]
)
class SessionEntity(
    @JvmField
    @PrimaryKey
    @ColumnInfo(name = "id")
    var id: String,

    @JvmField
    @ColumnInfo(name = "title")
    var title: String,

    @JvmField
    @ColumnInfo(name = "model_id")
    var modelId: String?,

    @JvmField
    @ColumnInfo(name = "pinned")
    var pinned: Boolean,

    @JvmField
    @ColumnInfo(name = "archived")
    var archived: Boolean,

    @JvmField
    @ColumnInfo(name = "created_at")
    var createdAt: Long,

    @JvmField
    @ColumnInfo(name = "updated_at")
    var updatedAt: Long,

    @JvmField
    @ColumnInfo(name = "last_message_preview")
    var lastMessagePreview: String?
)
