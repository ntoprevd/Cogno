package com.ntoprevd.cogno.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ntoprevd.cogno.data.db.dao.MessageDao
import com.ntoprevd.cogno.data.db.dao.SessionDao
import com.ntoprevd.cogno.data.db.entity.MessageEntity
import com.ntoprevd.cogno.data.db.entity.SessionEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseInstrumentedTest {
    private lateinit var database: AppDatabase
    private lateinit var sessionDao: SessionDao
    private lateinit var messageDao: MessageDao

    @Before
    fun setUp() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sessionDao = database.sessionDao()
        messageDao = database.messageDao()
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) {
            database.close()
        }
    }

    @Test
    fun insertSession_queryOrdersByPinnedDescAndUpdatedAtDesc() = runBlocking {
        val oldPinned = session("session-old-pinned", "Old pinned", true, 100L)
        val newestUnpinned = session("session-newest-unpinned", "Newest unpinned", false, 300L)
        val newestPinned = session("session-newest-pinned", "Newest pinned", true, 200L)

        sessionDao.insertSessions(listOf(oldPinned, newestUnpinned, newestPinned))

        val sessions = sessionDao.getAllSessionsOrderByUpdatedAtDesc()

        assertEquals(3, sessions.size)
        assertEquals("session-newest-pinned", sessions[0].id)
        assertEquals("session-old-pinned", sessions[1].id)
        assertEquals("session-newest-unpinned", sessions[2].id)
    }

    @Test
    fun insertMessages_queryBySessionIdWithLimitAndOffsetOrdersByCreatedAtAsc() = runBlocking {
        sessionDao.insertSession(session("session-chat", "Chat", false, 100L))
        messageDao.insertMessages(
            listOf(
                message("message-3", "session-chat", "assistant", "Third", 300L),
                message("message-1", "session-chat", "user", "First", 100L),
                message("message-2", "session-chat", "assistant", "Second", 200L)
            )
        )

        val page = messageDao.getMessagesBySessionId("session-chat", 2, 1)

        assertEquals(2, page.size)
        assertEquals("message-2", page[0].id)
        assertEquals("message-3", page[1].id)
    }

    @Test
    fun deleteSession_cascadesDeleteToMessages() = runBlocking {
        sessionDao.insertSession(session("session-delete", "Delete me", false, 100L))
        messageDao.insertMessages(
            listOf(
                message("message-1", "session-delete", "user", "Hello", 100L),
                message("message-2", "session-delete", "assistant", "Hi", 200L)
            )
        )

        sessionDao.deleteSessionById("session-delete")

        assertNull(sessionDao.getSessionById("session-delete"))
        assertEquals(0, messageDao.getMessagesBySessionId("session-delete", 20, 0).size)
    }

    @Test
    fun recentMessages_returnsLatestWindowInChronologicalOrder() = runBlocking {
        sessionDao.insertSession(session("session-recent", "Recent", false, 100L))
        messageDao.insertMessages(
            (1..5).map { index ->
                message(
                    id = "message-$index",
                    sessionId = "session-recent",
                    role = if (index % 2 == 0) "assistant" else "user",
                    content = "Message $index",
                    createdAt = index * 100L
                )
            }
        )

        val recent = messageDao.getRecentMessagesBySessionId("session-recent", 3)

        assertEquals(listOf("message-3", "message-4", "message-5"), recent.map { it.id })
    }

    @Test
    fun deleteMessagesAfter_trimsStaleConversationBranch() = runBlocking {
        sessionDao.insertSession(session("session-branch", "Branch", false, 100L))
        messageDao.insertMessages(
            listOf(
                message("message-1", "session-branch", "user", "First", 100L),
                message("message-2", "session-branch", "assistant", "Second", 200L),
                message("message-3", "session-branch", "user", "Stale", 300L)
            )
        )

        messageDao.deleteMessagesAfter("session-branch", 200L)

        val remaining = messageDao.getMessagesBySessionId("session-branch", 20, 0)
        assertEquals(listOf("message-1", "message-2"), remaining.map { it.id })
    }

    private fun session(id: String, title: String, pinned: Boolean, updatedAt: Long): SessionEntity {
        return SessionEntity(
            id,
            title,
            "deepseek-v3",
            pinned,
            false,
            updatedAt - 10L,
            updatedAt,
            null
        )
    }

    private fun message(
        id: String,
        sessionId: String,
        role: String,
        content: String,
        createdAt: Long
    ): MessageEntity {
        return MessageEntity(
            id,
            sessionId,
            role,
            content,
            "completed",
            null,
            null,
            null,
            null,
            null,
            createdAt,
            createdAt
        )
    }
}
