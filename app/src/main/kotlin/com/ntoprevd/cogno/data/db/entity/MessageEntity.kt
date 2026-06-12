package com.ntoprevd.cogno.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["session_id", "created_at"]),
        Index(value = ["status"])
    ]
)
class MessageEntity(
    @JvmField
    @PrimaryKey
    @ColumnInfo(name = "id")
    var id: String,

    @JvmField
    @ColumnInfo(name = "session_id")
    var sessionId: String,

    @JvmField
    @ColumnInfo(name = "role")
    var role: String,

    @JvmField
    @ColumnInfo(name = "content")
    var content: String,

    @JvmField
    @ColumnInfo(name = "status")
    var status: String,

    @JvmField
    @ColumnInfo(name = "error_code")
    var errorCode: String?,

    @JvmField
    @ColumnInfo(name = "token_count")
    var tokenCount: Int?,

    @JvmField
    @ColumnInfo(name = "feedback")
    var feedback: String?,

    @JvmField
    @ColumnInfo(name = "image_path")
    var imagePath: String?,

    @JvmField
    @ColumnInfo(name = "image_mime_type")
    var imageMimeType: String?,

    @JvmField
    @ColumnInfo(name = "created_at")
    var createdAt: Long,

    @JvmField
    @ColumnInfo(name = "updated_at")
    var updatedAt: Long
)
