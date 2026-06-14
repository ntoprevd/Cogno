package com.ntoprevd.cogno.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ntoprevd.cogno.data.db.dao.MessageDao
import com.ntoprevd.cogno.data.db.dao.SessionDao
import com.ntoprevd.cogno.data.db.entity.MessageEntity
import com.ntoprevd.cogno.data.db.entity.SessionEntity
import com.ntoprevd.cogno.data.media.ChatImageStore
import java.io.File
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

    @Test
    fun interruptedAssistantMessages_areMarkedFailed() = runBlocking {
        sessionDao.insertSession(session("session-interrupted", "Interrupted", false, 100L))
        messageDao.insertMessages(
            listOf(
                message(
                    id = "message-pending",
                    sessionId = "session-interrupted",
                    role = "assistant",
                    content = "",
                    createdAt = 100L,
                    status = "pending"
                ),
                message(
                    id = "message-streaming",
                    sessionId = "session-interrupted",
                    role = "assistant",
                    content = "Partial",
                    createdAt = 200L,
                    status = "streaming"
                )
            )
        )

        val updated = messageDao.markInterruptedAssistantMessagesFailed("Interrupted", 300L)

        assertEquals(2, updated)
        assertEquals("Interrupted", messageDao.getMessageById("message-pending")?.content)
        assertEquals("failed", messageDao.getMessageById("message-pending")?.status)
        assertEquals("Partial", messageDao.getMessageById("message-streaming")?.content)
        assertEquals("failed", messageDao.getMessageById("message-streaming")?.status)
    }

    @Test
    fun chatImageStore_onlyDeletesFilesInsideManagedDirectory() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val imageDirectory = File(context.filesDir, "chat_images").apply { mkdirs() }
        val managedFile = File(imageDirectory, "managed-test.jpg").apply { writeText("image") }
        val outsideFile = File(context.filesDir, "outside-test.jpg").apply { writeText("keep") }
        val store = ChatImageStore(context)

        try {
            store.deleteStored(outsideFile.absolutePath)
            store.deleteStored(managedFile.absolutePath)

            assertEquals(true, outsideFile.exists())
            assertEquals(false, managedFile.exists())
        } finally {
            if (managedFile.exists()) managedFile.delete()
            if (outsideFile.exists()) outsideFile.delete()
        }
    }

    @Test
    fun migrateVersion1To6_preservesDataAndCreatesCurrentSchema() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "migration-1-to-6.db"
        context.deleteDatabase(databaseName)

        context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null).use { legacyDb ->
            legacyDb.execSQL(
                "CREATE TABLE sessions (" +
                    "id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, model_id TEXT, " +
                    "pinned INTEGER NOT NULL, archived INTEGER NOT NULL, created_at INTEGER NOT NULL, " +
                    "updated_at INTEGER NOT NULL, last_message_preview TEXT)"
            )
            legacyDb.execSQL("CREATE INDEX index_sessions_updated_at ON sessions(updated_at)")
            legacyDb.execSQL("CREATE INDEX index_sessions_pinned_updated_at ON sessions(pinned, updated_at)")
            legacyDb.execSQL(
                "CREATE TABLE messages (" +
                    "id TEXT NOT NULL PRIMARY KEY, session_id TEXT NOT NULL, role TEXT NOT NULL, " +
                    "content TEXT NOT NULL, status TEXT NOT NULL, error_code TEXT, token_count INTEGER, " +
                    "created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, " +
                    "FOREIGN KEY(session_id) REFERENCES sessions(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
            )
            legacyDb.execSQL(
                "CREATE INDEX index_messages_session_id_created_at " +
                    "ON messages(session_id, created_at)"
            )
            legacyDb.execSQL("CREATE INDEX index_messages_status ON messages(status)")
            legacyDb.execSQL(
                "INSERT INTO sessions VALUES " +
                    "('legacy-session', 'Legacy', 'deepseek-v3', 0, 0, 100, 100, 'Hello')"
            )
            legacyDb.execSQL(
                "INSERT INTO messages VALUES " +
                    "('legacy-message', 'legacy-session', 'user', 'Hello', 'completed', NULL, NULL, 100, 100)"
            )
            legacyDb.version = 1
        }

        val migratedDb = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6
            )
            .allowMainThreadQueries()
            .build()
        try {
            assertEquals("Legacy", migratedDb.sessionDao().getSessionById("legacy-session")?.title)
            val message = migratedDb.messageDao().getMessageById("legacy-message")
            assertEquals("Hello", message?.content)
            assertNull(message?.feedback)
            assertNull(message?.imagePath)
            assertNull(message?.imageMimeType)
            val migratedNoteColumns = migratedDb.openHelper.readableDatabase
                .query("PRAGMA table_info(notes)")
                .use { cursor ->
                    buildSet {
                        val nameIndex = cursor.getColumnIndexOrThrow("name")
                        while (cursor.moveToNext()) add(cursor.getString(nameIndex))
                    }
                }
            assertEquals(true, "source_last_message_created_at" in migratedNoteColumns)
            assertEquals(true, "source_message_revision" in migratedNoteColumns)
            assertEquals(6, migratedDb.openHelper.readableDatabase.version)
        } finally {
            migratedDb.close()
            context.deleteDatabase(databaseName)
        }
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
        createdAt: Long,
        status: String = "completed"
    ): MessageEntity {
        return MessageEntity(
            id,
            sessionId,
            role,
            content,
            status,
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
