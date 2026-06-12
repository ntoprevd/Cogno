package com.ntoprevd.cogno.data.repository

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import androidx.room.withTransaction
import com.ntoprevd.cogno.data.db.AppDatabase
import com.ntoprevd.cogno.data.db.entity.MessageEntity
import com.ntoprevd.cogno.data.db.entity.NoteEntity
import com.ntoprevd.cogno.data.db.entity.SessionEntity
import com.ntoprevd.cogno.data.media.ChatImageStore
import com.ntoprevd.cogno.data.network.AiChatClient
import com.ntoprevd.cogno.data.network.AiRequestCancelledException
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
    private val aiChatClient = AiChatClient(context)
    private val chatImageStore = ChatImageStore(context)
    private val topicRepository = TopicRepository(context)

    fun observeSessions(): Flow<List<SessionEntity>> =
        sessionDao.observeAllSessionsOrderByUpdatedAtDesc()

    fun observeMessages(sessionId: String): Flow<List<MessageEntity>> =
        messageDao.observeMessagesBySessionId(sessionId)

    suspend fun getSessions(): List<SessionEntity> =
        sessionDao.getAllSessionsOrderByUpdatedAtDesc()

    suspend fun getMessages(sessionId: String): List<MessageEntity> =
        messageDao.getRecentMessagesBySessionId(sessionId, DEFAULT_MESSAGE_LIMIT)

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

    fun cancelAssistantReply() {
        aiChatClient.cancelStreamingRequest()
    }

    suspend fun beginUserMessage(
        sessionId: String?,
        content: String,
        imageUri: Uri? = null
    ): PendingAssistantResult {
        val now = System.currentTimeMillis()
        val settings = settingsStore.load()
        val storedImage = imageUri?.let { chatImageStore.storeCompressed(it) }
        val messagePreview = content.ifBlank { if (storedImage != null) IMAGE_PREVIEW else "" }
        return database.withTransaction {
            val targetSession = if (sessionId.isNullOrBlank()) {
                SessionEntity(
                    UUID.randomUUID().toString(),
                    buildTitle(messagePreview),
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
                    buildTitle(messagePreview),
                    settings.modelId,
                    false,
                    false,
                    now,
                    now,
                    null
                ).also { sessionDao.insertSession(it) }
            }

            val message = MessageEntity(
                id = UUID.randomUUID().toString(),
                sessionId = targetSession.id,
                role = ROLE_USER,
                content = content,
                status = STATUS_COMPLETED,
                errorCode = null,
                tokenCount = null,
                feedback = null,
                imagePath = storedImage?.path,
                imageMimeType = storedImage?.mimeType,
                createdAt = now,
                updatedAt = now
            )
            messageDao.insertMessage(message)

            val assistantMessage = MessageEntity(
                id = UUID.randomUUID().toString(),
                sessionId = targetSession.id,
                role = ROLE_ASSISTANT,
                content = "",
                status = STATUS_PENDING,
                errorCode = null,
                tokenCount = null,
                feedback = null,
                imagePath = null,
                imageMimeType = null,
                createdAt = now + 1,
                updatedAt = now + 1
            )
            messageDao.insertMessage(assistantMessage)

            // Keep the session preview consistent with the new user message.
            val updatedSession = SessionEntity(
                targetSession.id,
                targetSession.title,
                settings.modelId,
                targetSession.pinned,
                targetSession.archived,
                targetSession.createdAt,
                now,
                preview(messagePreview)
            )
            sessionDao.updateSession(updatedSession)
            PendingAssistantResult(updatedSession, message, assistantMessage)
        }
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
        val trimmed = content.trim()
        if (trimmed.isBlank()) return false
        val result = database.withTransaction {
            val message = messageDao.getMessageById(messageId) ?: return@withTransaction null
            if (message.role != ROLE_USER) return@withTransaction null
            val assistantMessage =
                messageDao.getNextAssistantMessage(message.sessionId, message.createdAt)
                    ?: return@withTransaction null

            message.content = trimmed
            message.updatedAt = System.currentTimeMillis()
            messageDao.updateMessage(message)
            // Editing history creates a new branch; stale replies after this pair are removed.
            messageDao.deleteMessagesAfter(message.sessionId, assistantMessage.createdAt)
            message to assistantMessage
        } ?: return false
        val (message, assistantMessage) = result
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

        val messages = messageDao.getRecentMessagesBefore(
            sessionId,
            assistantMessage.createdAt,
            DEFAULT_MESSAGE_LIMIT
        ).filter { it.status == STATUS_COMPLETED }
        messageDao.deleteMessagesAfter(sessionId, assistantMessage.createdAt)
        streamAssistantReply(sessionId, assistantMessageId, messages)
    }

    suspend fun generateNoteFromSession(sessionId: String, style: String): GeneratedNoteResult {
        val session = sessionDao.getSessionById(sessionId)
            ?: throw IllegalStateException("请先选择一个会话")
        val messages = getMessages(sessionId)
            .filter {
                it.status == STATUS_COMPLETED &&
                    (it.content.isNotBlank() || !it.imagePath.isNullOrBlank())
            }
        if (messages.isEmpty()) {
            throw IllegalStateException("当前会话还没有可总结的内容")
        }

        val existingNote = noteDao.getLatestNoteBySourceSessionId(sessionId)
        if (existingNote != null && existingNote.sourceMessageCount >= messages.size) {
            return GeneratedNoteResult(existingNote, GeneratedNoteResult.UP_TO_DATE)
        }

        val settings = settingsStore.load()
        val topicNames = topicRepository.enabledTopics().map { it.name }
        val messagesToSummarize = if (existingNote == null) {
            messages
        } else {
            messages.drop(existingNote.sourceMessageCount.coerceAtMost(messages.size))
        }
        val draft = aiChatClient.requestNoteDraft(
            settings = settings,
            conversationTitle = session.title,
            conversationText = buildConversationText(messagesToSummarize),
            style = style,
            existingContent = existingNote?.content,
            topicNames = topicNames
        )
        val now = System.currentTimeMillis()
        if (existingNote != null) {
            val appendedContent = draft.content.trim()
            existingNote.content = appendNoteContent(existingNote.content, appendedContent)
            existingNote.preview = preview(markdownPlainText(existingNote.content))
            existingNote.sourceMessageCount = messages.size
            existingNote.updatedAt = now
            noteDao.updateNote(existingNote)
            topicRepository.syncSegments(
                note = existingNote,
                aiSegments = draft.segments,
                fallbackContent = appendedContent,
                sourceMessageCount = messages.size
            )
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
        topicRepository.syncSegments(
            note = note,
            aiSegments = draft.segments,
            fallbackContent = draft.content,
            sourceMessageCount = messages.size
        )
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
        var lastPersistedAt = 0L
        var lastPersistedLength = 0

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
                val elapsed = SystemClock.elapsedRealtime()
                val hasEnoughText = streamedContent.length - lastPersistedLength >= STREAM_WRITE_CHARS
                if (elapsed - lastPersistedAt >= STREAM_WRITE_INTERVAL_MS || hasEnoughText) {
                    assistantMessage.content = streamedContent.toString()
                    assistantMessage.updatedAt = System.currentTimeMillis()
                    messageDao.updateMessage(assistantMessage)
                    lastPersistedAt = elapsed
                    lastPersistedLength = streamedContent.length
                }
            }
        }.onSuccess { response ->
            val assistantMessage = messageDao.getMessageById(assistantMessageId) ?: return
            val completedAt = System.currentTimeMillis()
            assistantMessage.content = response.content.ifBlank { streamedContent.toString() }
            assistantMessage.status = STATUS_COMPLETED
            assistantMessage.errorCode = null
            assistantMessage.tokenCount = response.totalTokens
            assistantMessage.updatedAt = completedAt
            messageDao.updateMessage(assistantMessage)

            sessionDao.getSessionById(sessionId)?.let { session ->
                session.modelId = settings.modelId
                session.updatedAt = completedAt
                session.lastMessagePreview = preview(response.content)
                sessionDao.updateSession(session)
            }
        }.onFailure { error ->
            val assistantMessage = messageDao.getMessageById(assistantMessageId) ?: return
            if (error is AiRequestCancelledException && streamedContent.isNotBlank()) {
                assistantMessage.content = streamedContent.toString()
                assistantMessage.status = STATUS_COMPLETED
                assistantMessage.errorCode = null
                assistantMessage.updatedAt = System.currentTimeMillis()
                messageDao.updateMessage(assistantMessage)
                return@onFailure
            }
            assistantMessage.content = error.message ?: "AI 请求失败，请检查 API 配置后重试。"
            assistantMessage.status = STATUS_FAILED
            assistantMessage.errorCode = error::class.simpleName
            assistantMessage.updatedAt = System.currentTimeMillis()
            messageDao.updateMessage(assistantMessage)
        }.exceptionOrNull()?.let { error ->
            if (error !is AiRequestCancelledException) throw error
        }
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
            val content = message.content.ifBlank {
                if (!message.imagePath.isNullOrBlank()) IMAGE_PREVIEW else ""
            }
            "$role：$content"
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
        private const val STREAM_WRITE_INTERVAL_MS = 80L
        private const val STREAM_WRITE_CHARS = 32
        private const val PREVIEW_LIMIT = 80
        private const val STATUS_PENDING = "pending"
        private const val STATUS_STREAMING = "streaming"
        private const val STATUS_COMPLETED = "completed"
        private const val STATUS_FAILED = "failed"
        private const val ROLE_USER = "user"
        private const val ROLE_ASSISTANT = "assistant"
        private const val IMAGE_PREVIEW = "[图片]"
    }
}

internal fun appendNoteContent(existingContent: String, appendedContent: String): String {
    val existing = existingContent.trimEnd()
    val addition = appendedContent.trim()
    if (addition.isBlank()) return existing
    if (existing.isBlank()) return addition
    return "$existing\n\n$addition"
}
