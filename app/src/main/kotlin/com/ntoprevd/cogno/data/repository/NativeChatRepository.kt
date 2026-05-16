package com.ntoprevd.cogno.data.repository

import android.content.Context
import com.ntoprevd.cogno.data.db.AppDatabase
import com.ntoprevd.cogno.data.db.entity.MessageEntity
import com.ntoprevd.cogno.data.db.entity.SessionEntity
import java.util.UUID
import kotlinx.coroutines.flow.Flow

data class SendUserMessageResult(
    val session: SessionEntity,
    val message: MessageEntity
)

class NativeChatRepository(context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val sessionDao = database.sessionDao()
    private val messageDao = database.messageDao()

    fun observeSessions(): Flow<List<SessionEntity>> =
        sessionDao.observeAllSessionsOrderByUpdatedAtDesc()

    fun observeMessages(sessionId: String): Flow<List<MessageEntity>> =
        messageDao.observeMessagesBySessionId(sessionId)

    suspend fun getSessions(): List<SessionEntity> =
        sessionDao.getAllSessionsOrderByUpdatedAtDesc()

    suspend fun getMessages(sessionId: String): List<MessageEntity> =
        messageDao.getMessagesBySessionId(sessionId, DEFAULT_MESSAGE_LIMIT, 0)

    suspend fun renameSession(sessionId: String, title: String) {
        val session = sessionDao.getSessionById(sessionId) ?: return
        session.title = title.trim().ifBlank { session.title }
        sessionDao.updateSession(session)
    }

    suspend fun setSessionPinned(sessionId: String, pinned: Boolean) {
        val session = sessionDao.getSessionById(sessionId) ?: return
        session.pinned = pinned
        sessionDao.updateSession(session)
    }

    suspend fun deleteSession(sessionId: String) {
        sessionDao.deleteSessionById(sessionId)
    }

    suspend fun sendUserMessage(sessionId: String?, content: String): SendUserMessageResult {
        val now = System.currentTimeMillis()
        val targetSession = if (sessionId.isNullOrBlank()) {
            SessionEntity(
                UUID.randomUUID().toString(),
                buildTitle(content),
                DEFAULT_MODEL_ID,
                false,
                false,
                now,
                now,
                null
            ).also { sessionDao.insertSession(it) }
        } else {
            sessionDao.getSessionById(sessionId) ?: SessionEntity(
                sessionId,
                buildTitle(content),
                DEFAULT_MODEL_ID,
                false,
                false,
                now,
                now,
                null
            ).also { sessionDao.insertSession(it) }
        }

        val message = MessageEntity(
            UUID.randomUUID().toString(),
            targetSession.id,
            "user",
            content,
            "completed",
            null,
            null,
            now,
            now
        )
        messageDao.insertMessage(message)

        // 会话预览用于侧边栏快速扫描；后续接入 AI 后继续由最后一条消息驱动。
        val updatedSession = SessionEntity(
            targetSession.id,
            targetSession.title,
            targetSession.modelId,
            targetSession.pinned,
            targetSession.archived,
            targetSession.createdAt,
            now,
            preview(content)
        )
        sessionDao.updateSession(updatedSession)
        return SendUserMessageResult(updatedSession, message)
    }

    private fun buildTitle(content: String): String {
        val title = preview(content)
        return title.ifBlank { "新会话" }
    }

    private fun preview(content: String): String {
        val trimmed = content.trim()
        return if (trimmed.length <= PREVIEW_LIMIT) trimmed else trimmed.take(PREVIEW_LIMIT)
    }

    companion object {
        private const val DEFAULT_MODEL_ID = "deepseek-v3"
        private const val DEFAULT_MESSAGE_LIMIT = 200
        private const val PREVIEW_LIMIT = 80
    }
}
