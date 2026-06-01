package com.ntoprevd.cogno.data.repository

import android.content.Context
import com.ntoprevd.cogno.data.db.AppDatabase
import com.ntoprevd.cogno.data.db.entity.MessageEntity
import com.ntoprevd.cogno.data.db.entity.NoteEntity
import com.ntoprevd.cogno.data.db.entity.SessionEntity
import com.ntoprevd.cogno.data.network.AiChatClient
import com.ntoprevd.cogno.data.settings.AiSettingsStore
import java.util.UUID
import kotlinx.coroutines.flow.Flow

data class PendingAssistantResult(
    val session: SessionEntity,
    val userMessage: MessageEntity,
    val assistantMessage: MessageEntity
)

data class GeneratedNoteResult(
    val note: NoteEntity,
    val status: String
) {
    companion object {
        const val CREATED = "created"
        const val UPDATED = "updated"
        const val UP_TO_DATE = "up_to_date"
    }
}

class NativeChatRepository(context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val sessionDao = database.sessionDao()
    private val messageDao = database.messageDao()
    private val noteDao = database.noteDao()
    private val settingsStore = AiSettingsStore(context)
    private val aiChatClient = AiChatClient()

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

    suspend fun beginUserMessage(sessionId: String?, content: String): PendingAssistantResult {
        val now = System.currentTimeMillis()
        val settings = settingsStore.load()
        val targetSession = if (sessionId.isNullOrBlank()) {
            SessionEntity(
                UUID.randomUUID().toString(),
                buildTitle(content),
                settings.modelId,
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
                settings.modelId,
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
            null,
            now,
            now
        )
        messageDao.insertMessage(message)

        val assistantMessage = MessageEntity(
            UUID.randomUUID().toString(),
            targetSession.id,
            "assistant",
            "",
            STATUS_PENDING,
            null,
            null,
            null,
            now + 1,
            now + 1
        )
        messageDao.insertMessage(assistantMessage)

        // 会话预览用于侧边栏快速扫描；后续接入 AI 后继续由最后一条消息驱动。
        val updatedSession = SessionEntity(
            targetSession.id,
            targetSession.title,
            settings.modelId,
            targetSession.pinned,
            targetSession.archived,
            targetSession.createdAt,
            now,
            preview(content)
        )
        sessionDao.updateSession(updatedSession)
        return PendingAssistantResult(updatedSession, message, assistantMessage)
    }

    suspend fun completeAssistantReply(sessionId: String, assistantMessageId: String) {
        val settings = settingsStore.load()
        val messages = getMessages(sessionId)
            .filterNot { it.id == assistantMessageId }
            .filter { it.status == STATUS_COMPLETED }
        streamAssistantReply(sessionId, assistantMessageId, messages)
    }

    suspend fun updateUserMessage(messageId: String, content: String) {
        val trimmed = content.trim()
        if (trimmed.isBlank()) return

        val message = messageDao.getMessageById(messageId) ?: return
        if (message.role != ROLE_USER) return

        val now = System.currentTimeMillis()
        message.content = trimmed
        message.updatedAt = now
        messageDao.updateMessage(message)

        // 如果修改的是当前会话最后一条消息，同步更新侧边栏预览。
        val latestMessage = messageDao.getLatestMessageBySessionId(message.sessionId)
        if (latestMessage?.id == message.id) {
            sessionDao.getSessionById(message.sessionId)?.let { session ->
                session.updatedAt = now
                session.lastMessagePreview = preview(trimmed)
                sessionDao.updateSession(session)
            }
        }
    }

    suspend fun updateUserMessageAndRegenerate(messageId: String, content: String): Boolean {
        updateUserMessage(messageId, content)

        val message = messageDao.getMessageById(messageId) ?: return false
        if (message.role != ROLE_USER) return false

        val assistantMessage = messageDao.getNextAssistantMessage(message.sessionId, message.createdAt)
            ?: return false
        regenerateAssistantReply(message.sessionId, assistantMessage.id)
        return true
    }

    suspend fun setAssistantFeedback(messageId: String, feedback: String?) {
        val message = messageDao.getMessageById(messageId) ?: return
        if (message.role != ROLE_ASSISTANT) return

        message.feedback = feedback
        message.updatedAt = System.currentTimeMillis()
        messageDao.updateMessage(message)
    }

    suspend fun regenerateAssistantReply(sessionId: String, assistantMessageId: String) {
        val assistantMessage = messageDao.getMessageById(assistantMessageId) ?: return
        if (assistantMessage.role != ROLE_ASSISTANT) return

        val messages = messageDao.getMessagesBefore(sessionId, assistantMessage.createdAt)
            .filter { it.status == STATUS_COMPLETED }
        streamAssistantReply(sessionId, assistantMessageId, messages)
    }

    suspend fun generateNoteFromSession(sessionId: String, style: String): GeneratedNoteResult {
        val session = sessionDao.getSessionById(sessionId)
            ?: throw IllegalStateException("请先选择一个会话")
        val messages = getMessages(sessionId)
            .filter { it.status == STATUS_COMPLETED && it.content.isNotBlank() }
        if (messages.isEmpty()) {
            throw IllegalStateException("当前会话还没有可总结的内容")
        }

        val existingNote = noteDao.getLatestNoteBySourceSessionId(sessionId)
        if (existingNote != null && existingNote.sourceMessageCount >= messages.size) {
            return GeneratedNoteResult(existingNote, GeneratedNoteResult.UP_TO_DATE)
        }

        val settings = settingsStore.load()
        val draft = aiChatClient.requestNoteDraft(
            settings = settings,
            conversationTitle = session.title,
            conversationText = buildConversationText(messages),
            style = style,
            existingContent = existingNote?.content
        )
        val now = System.currentTimeMillis()
        if (existingNote != null) {
            existingNote.title = draft.title.ifBlank { existingNote.title }
            existingNote.content = draft.content
            existingNote.preview = preview(markdownPlainText(draft.content))
            existingNote.sourceMessageCount = messages.size
            existingNote.updatedAt = now
            noteDao.updateNote(existingNote)
            return GeneratedNoteResult(existingNote, GeneratedNoteResult.UPDATED)
        }

        val note = NoteEntity(
            id = UUID.randomUUID().toString(),
            title = draft.title.ifBlank { session.title.ifBlank { "会话笔记" } },
            content = draft.content,
            preview = preview(markdownPlainText(draft.content)),
            sourceSessionId = sessionId,
            sourceMessageCount = messages.size,
            pinned = false,
            createdAt = now,
            updatedAt = now
        )
        noteDao.insertNote(note)
        return GeneratedNoteResult(note, GeneratedNoteResult.CREATED)
    }

    private suspend fun streamAssistantReply(
        sessionId: String,
        assistantMessageId: String,
        messages: List<MessageEntity>
    ) {
        val settings = settingsStore.load()
        val now = System.currentTimeMillis()
        val streamedContent = StringBuilder()

        runCatching {
            val assistantMessage = messageDao.getMessageById(assistantMessageId) ?: return
            assistantMessage.content = ""
            assistantMessage.status = STATUS_STREAMING
            assistantMessage.errorCode = null
            assistantMessage.feedback = null
            assistantMessage.updatedAt = now
            messageDao.updateMessage(assistantMessage)

            aiChatClient.streamChatCompletion(settings, messages) { delta ->
                streamedContent.append(delta)
                val streamingMessage = messageDao.getMessageById(assistantMessageId) ?: return@streamChatCompletion
                streamingMessage.content = streamedContent.toString()
                streamingMessage.status = STATUS_STREAMING
                streamingMessage.errorCode = null
                streamingMessage.updatedAt = System.currentTimeMillis()
                messageDao.updateMessage(streamingMessage)
            }
        }.onSuccess { response ->
            val assistantMessage = messageDao.getMessageById(assistantMessageId) ?: return
            assistantMessage.content = response.content.ifBlank { streamedContent.toString() }
            assistantMessage.status = STATUS_COMPLETED
            assistantMessage.errorCode = null
            assistantMessage.tokenCount = response.totalTokens
            assistantMessage.updatedAt = now
            messageDao.updateMessage(assistantMessage)

            sessionDao.getSessionById(sessionId)?.let { session ->
                session.modelId = settings.modelId
                session.updatedAt = now
                session.lastMessagePreview = preview(response.content)
                sessionDao.updateSession(session)
            }
        }.onFailure { error ->
            val assistantMessage = messageDao.getMessageById(assistantMessageId) ?: return
            assistantMessage.content = error.message ?: "AI 请求失败，请检查 API 配置后重试。"
            assistantMessage.status = STATUS_FAILED
            assistantMessage.errorCode = error::class.simpleName
            assistantMessage.updatedAt = now
            messageDao.updateMessage(assistantMessage)
        }.getOrThrow()
    }

    private fun buildTitle(content: String): String {
        val title = preview(content)
        return title.ifBlank { "新会话" }
    }

    private fun preview(content: String): String {
        val trimmed = content.trim()
        return if (trimmed.length <= PREVIEW_LIMIT) trimmed else trimmed.take(PREVIEW_LIMIT)
    }

    private fun buildConversationText(messages: List<MessageEntity>): String {
        return messages.joinToString(separator = "\n\n") { message ->
            val role = if (message.role == ROLE_USER) "用户" else "AI"
            "$role：${message.content}"
        }
    }

    private fun markdownPlainText(content: String): String {
        return content
            .replace(Regex("[#>*_`\\-]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    companion object {
        private const val DEFAULT_MESSAGE_LIMIT = 200
        private const val PREVIEW_LIMIT = 80
        private const val STATUS_PENDING = "pending"
        private const val STATUS_STREAMING = "streaming"
        private const val STATUS_COMPLETED = "completed"
        private const val STATUS_FAILED = "failed"
        private const val ROLE_USER = "user"
        private const val ROLE_ASSISTANT = "assistant"
    }
}
