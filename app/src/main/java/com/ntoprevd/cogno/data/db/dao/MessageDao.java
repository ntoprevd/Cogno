package com.ntoprevd.cogno.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.ntoprevd.cogno.data.db.entity.MessageEntity;
import java.util.List;

@Dao
public interface MessageDao {

    @Query(
            "SELECT * FROM messages "
                    + "WHERE session_id = :sessionId "
                    + "ORDER BY created_at ASC "
                    + "LIMIT :limit OFFSET :offset"
    )
    List<MessageEntity> getMessagesBySessionId(String sessionId, int limit, int offset);

    @Query("SELECT * FROM messages WHERE id = :id LIMIT 1")
    MessageEntity getMessageById(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMessage(MessageEntity message);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMessages(List<MessageEntity> messages);

    @Update
    void updateMessage(MessageEntity message);

    @Query("DELETE FROM messages WHERE id = :id")
    void deleteMessageById(String id);
}
