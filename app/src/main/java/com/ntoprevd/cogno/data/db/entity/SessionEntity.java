package com.ntoprevd.cogno.data.db.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "sessions",
        indices = {
                @Index(value = {"updated_at"}),
                @Index(value = {"pinned", "updated_at"})
        }
)
public class SessionEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id;

    @NonNull
    @ColumnInfo(name = "title")
    public String title;

    @Nullable
    @ColumnInfo(name = "model_id")
    public String modelId;

    @ColumnInfo(name = "pinned")
    public boolean pinned;

    @ColumnInfo(name = "archived")
    public boolean archived;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    @Nullable
    @ColumnInfo(name = "last_message_preview")
    public String lastMessagePreview;

    public SessionEntity(
            @NonNull String id,
            @NonNull String title,
            @Nullable String modelId,
            boolean pinned,
            boolean archived,
            long createdAt,
            long updatedAt,
            @Nullable String lastMessagePreview
    ) {
        this.id = id;
        this.title = title;
        this.modelId = modelId;
        this.pinned = pinned;
        this.archived = archived;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastMessagePreview = lastMessagePreview;
    }
}
