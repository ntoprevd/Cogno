package com.ntoprevd.cogno.data.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.ntoprevd.cogno.data.db.entity.SessionEntity;
import java.util.List;

@Dao
public interface SessionDao {

    @Query("SELECT * FROM sessions ORDER BY pinned DESC, updated_at DESC")
    List<SessionEntity> getAllSessionsOrderByUpdatedAtDesc();

    @Query("SELECT * FROM sessions WHERE id = :id LIMIT 1")
    SessionEntity getSessionById(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSession(SessionEntity session);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSessions(List<SessionEntity> sessions);

    @Update
    void updateSession(SessionEntity session);

    @Delete
    void deleteSession(SessionEntity session);

    @Query("DELETE FROM sessions WHERE id = :id")
    void deleteSessionById(String id);
}
