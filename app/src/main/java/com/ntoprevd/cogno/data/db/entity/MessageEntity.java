package com.ntoprevd.cogno.data.db.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "messages",
        foreignKeys = {
                @ForeignKey(
                        entity = SessionEntity.class,
                        parentColumns = {"id"},
                        childColumns = {"session_id"},
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {
                @Index(value = {"session_id", "created_at"}),
                @Index(value = {"status"})
        }
)
public class MessageEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id;

    @NonNull
    @ColumnInfo(name = "session_id")
    public String sessionId;

    @NonNull
    @ColumnInfo(name = "role")
    public String role;

    @NonNull
    @ColumnInfo(name = "content")
    public String content;

    @NonNull
    @ColumnInfo(name = "status")
    public String status;

    @Nullable
    @ColumnInfo(name = "error_code")
    public String errorCode;

    @Nullable
    @ColumnInfo(name = "token_count")
    public Integer tokenCount;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    public MessageEntity(
            @NonNull String id,
            @NonNull String sessionId,
            @NonNull String role,
            @NonNull String content,
            @NonNull String status,
            @Nullable String errorCode,
            @Nullable Integer tokenCount,
            long createdAt,
            long updatedAt
    ) {
        this.id = id;
        this.sessionId = sessionId;
        this.role = role;
        this.content = content;
        this.status = status;
        this.errorCode = errorCode;
        this.tokenCount = tokenCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
