package com.ntoprevd.cogno.data.repository;

import com.ntoprevd.cogno.data.db.entity.MessageEntity;
import com.ntoprevd.cogno.data.db.entity.SessionEntity;
import java.util.List;

public interface ChatRepository {

    void getAllSessions(OnResultCallback<List<SessionEntity>> callback);

    void getSessionById(String id, OnResultCallback<SessionEntity> callback);

    void createSession(SessionEntity session, OnResultCallback<Void> callback);

    void updateSession(SessionEntity session, OnResultCallback<Void> callback);

    void deleteSessionById(String id, OnResultCallback<Void> callback);

    void getMessagesBySessionId(
            String sessionId,
            int limit,
            int offset,
            OnResultCallback<List<MessageEntity>> callback
    );

    void getMessageById(String id, OnResultCallback<MessageEntity> callback);

    void insertMessage(MessageEntity message, OnResultCallback<Void> callback);

    void insertMessages(List<MessageEntity> messages, OnResultCallback<Void> callback);

    void updateMessage(MessageEntity message, OnResultCallback<Void> callback);
}
