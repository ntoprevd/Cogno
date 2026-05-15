package com.ntoprevd.cogno.data.repository;

import com.ntoprevd.cogno.data.db.entity.MessageEntity;
import com.ntoprevd.cogno.data.db.entity.SessionEntity;
import java.util.List;

public interface ChatRepository {

    List<SessionEntity> getAllSessions();

    SessionEntity getSessionById(String id);

    void createSession(SessionEntity session);

    void updateSession(SessionEntity session);

    void deleteSessionById(String id);

    List<MessageEntity> getMessagesBySessionId(String sessionId, int limit, int offset);

    MessageEntity getMessageById(String id);

    void insertMessage(MessageEntity message);

    void insertMessages(List<MessageEntity> messages);

    void updateMessage(MessageEntity message);
}
