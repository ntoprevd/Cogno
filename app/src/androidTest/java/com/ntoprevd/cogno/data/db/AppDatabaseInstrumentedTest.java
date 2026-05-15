package com.ntoprevd.cogno.data.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;
import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.ntoprevd.cogno.data.db.dao.MessageDao;
import com.ntoprevd.cogno.data.db.dao.SessionDao;
import com.ntoprevd.cogno.data.db.entity.MessageEntity;
import com.ntoprevd.cogno.data.db.entity.SessionEntity;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AppDatabaseInstrumentedTest {

    private AppDatabase database;
    private SessionDao sessionDao;
    private MessageDao messageDao;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        sessionDao = database.sessionDao();
        messageDao = database.messageDao();
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void insertSession_queryOrdersByPinnedDescAndUpdatedAtDesc() {
        SessionEntity oldPinned = session("session-old-pinned", "Old pinned", true, 100L);
        SessionEntity newestUnpinned = session("session-newest-unpinned", "Newest unpinned", false, 300L);
        SessionEntity newestPinned = session("session-newest-pinned", "Newest pinned", true, 200L);

        sessionDao.insertSessions(Arrays.asList(oldPinned, newestUnpinned, newestPinned));

        List<SessionEntity> sessions = sessionDao.getAllSessionsOrderByUpdatedAtDesc();

        assertEquals(3, sessions.size());
        assertEquals("session-newest-pinned", sessions.get(0).id);
        assertEquals("session-old-pinned", sessions.get(1).id);
        assertEquals("session-newest-unpinned", sessions.get(2).id);
    }

    @Test
    public void insertMessages_queryBySessionIdWithLimitAndOffsetOrdersByCreatedAtAsc() {
        sessionDao.insertSession(session("session-chat", "Chat", false, 100L));
        messageDao.insertMessages(Arrays.asList(
                message("message-3", "session-chat", "assistant", "Third", 300L),
                message("message-1", "session-chat", "user", "First", 100L),
                message("message-2", "session-chat", "assistant", "Second", 200L)
        ));

        List<MessageEntity> page = messageDao.getMessagesBySessionId("session-chat", 2, 1);

        assertEquals(2, page.size());
        assertEquals("message-2", page.get(0).id);
        assertEquals("message-3", page.get(1).id);
    }

    @Test
    public void deleteSession_cascadesDeleteToMessages() {
        sessionDao.insertSession(session("session-delete", "Delete me", false, 100L));
        messageDao.insertMessages(Arrays.asList(
                message("message-1", "session-delete", "user", "Hello", 100L),
                message("message-2", "session-delete", "assistant", "Hi", 200L)
        ));

        sessionDao.deleteSessionById("session-delete");

        assertNull(sessionDao.getSessionById("session-delete"));
        assertEquals(0, messageDao.getMessagesBySessionId("session-delete", 20, 0).size());
    }

    private static SessionEntity session(String id, String title, boolean pinned, long updatedAt) {
        return new SessionEntity(
                id,
                title,
                "deepseek-v3",
                pinned,
                false,
                updatedAt - 10L,
                updatedAt,
                null
        );
    }

    private static MessageEntity message(
            String id,
            String sessionId,
            String role,
            String content,
            long createdAt
    ) {
        return new MessageEntity(
                id,
                sessionId,
                role,
                content,
                "completed",
                null,
                null,
                createdAt,
                createdAt
        );
    }
}
