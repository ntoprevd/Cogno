package com.ntoprevd.cogno.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.ntoprevd.cogno.data.db.AppDatabase;
import com.ntoprevd.cogno.data.db.dao.MessageDao;
import com.ntoprevd.cogno.data.db.dao.SessionDao;
import com.ntoprevd.cogno.data.db.entity.MessageEntity;
import com.ntoprevd.cogno.data.db.entity.SessionEntity;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatRepositoryImpl implements ChatRepository {

    private final SessionDao sessionDao;
    private final MessageDao messageDao;
    private final ExecutorService databaseExecutor;
    private final Handler mainHandler;

    public ChatRepositoryImpl(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        this.sessionDao = database.sessionDao();
        this.messageDao = database.messageDao();
        this.databaseExecutor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void getAllSessions(OnResultCallback<List<SessionEntity>> callback) {
        execute(callback, () -> sessionDao.getAllSessionsOrderByUpdatedAtDesc());
    }

    @Override
    public void getSessionById(String id, OnResultCallback<SessionEntity> callback) {
        execute(callback, () -> sessionDao.getSessionById(id));
    }

    @Override
    public void createSession(SessionEntity session, OnResultCallback<Void> callback) {
        execute(callback, () -> {
            sessionDao.insertSession(session);
            return null;
        });
    }

    @Override
    public void updateSession(SessionEntity session, OnResultCallback<Void> callback) {
        execute(callback, () -> {
            sessionDao.updateSession(session);
            return null;
        });
    }

    @Override
    public void deleteSessionById(String id, OnResultCallback<Void> callback) {
        execute(callback, () -> {
            sessionDao.deleteSessionById(id);
            return null;
        });
    }

    @Override
    public void getMessagesBySessionId(
            String sessionId,
            int limit,
            int offset,
            OnResultCallback<List<MessageEntity>> callback
    ) {
        execute(callback, () -> messageDao.getMessagesBySessionId(sessionId, limit, offset));
    }

    @Override
    public void getMessageById(String id, OnResultCallback<MessageEntity> callback) {
        execute(callback, () -> messageDao.getMessageById(id));
    }

    @Override
    public void insertMessage(MessageEntity message, OnResultCallback<Void> callback) {
        execute(callback, () -> {
            messageDao.insertMessage(message);
            return null;
        });
    }

    @Override
    public void insertMessages(List<MessageEntity> messages, OnResultCallback<Void> callback) {
        execute(callback, () -> {
            messageDao.insertMessages(messages);
            return null;
        });
    }

    @Override
    public void updateMessage(MessageEntity message, OnResultCallback<Void> callback) {
        execute(callback, () -> {
            messageDao.updateMessage(message);
            return null;
        });
    }

    private <T> void execute(OnResultCallback<T> callback, DatabaseTask<T> task) {
        databaseExecutor.execute(() -> {
            try {
                T result = task.run();
                postSuccess(callback, result);
            } catch (Exception e) {
                postError(callback, e);
            }
        });
    }

    private <T> void postSuccess(OnResultCallback<T> callback, T result) {
        if (callback == null) return;
        mainHandler.post(() -> callback.onSuccess(result));
    }

    private void postError(OnResultCallback<?> callback, Exception e) {
        if (callback == null) return;
        mainHandler.post(() -> callback.onError(e));
    }

    private interface DatabaseTask<T> {
        T run() throws Exception;
    }
}
