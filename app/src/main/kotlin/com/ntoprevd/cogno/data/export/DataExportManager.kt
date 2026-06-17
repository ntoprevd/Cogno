package com.ntoprevd.cogno.data.export

import android.content.Context
import android.net.Uri
import com.ntoprevd.cogno.BuildConfig
import com.ntoprevd.cogno.data.db.AppDatabase
import com.ntoprevd.cogno.data.db.entity.MessageEntity
import com.ntoprevd.cogno.data.db.entity.NoteEntity
import com.ntoprevd.cogno.data.db.entity.NoteTopicSegmentEntity
import com.ntoprevd.cogno.data.db.entity.SessionEntity
import com.ntoprevd.cogno.data.db.entity.TopicEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

enum class DataExportMode {
    ALL_DATA,
    CHATS_ONLY
}

class DataExportManager(context: Context) {
    private val appContext = context.applicationContext
    private val database = AppDatabase.getInstance(appContext)

    suspend fun exportTo(destination: Uri, mode: DataExportMode) = withContext(Dispatchers.IO) {
        val sessions = database.sessionDao().getAllSessionsOrderByUpdatedAtDesc()
        val messages = database.messageDao().getAllMessagesForExport()
        val notes = if (mode == DataExportMode.ALL_DATA) {
            database.noteDao().getAllNotesForExport()
        } else {
            emptyList()
        }
        val topics = if (mode == DataExportMode.ALL_DATA) {
            database.topicDao().getAllTopicsForExport()
        } else {
            emptyList()
        }
        val segments = if (mode == DataExportMode.ALL_DATA) {
            database.topicDao().getAllSegmentsForExport()
        } else {
            emptyList()
        }

        val output = appContext.contentResolver.openOutputStream(destination)
            ?: error("无法打开导出位置")
        output.buffered().use { buffered ->
            ZipOutputStream(buffered).use { zip ->
                val imageEntries = writeImages(zip, messages)
                zip.writeText(
                    "manifest.json",
                    buildManifest(mode, sessions, messages, notes, topics, imageEntries.size)
                )
                zip.writeText(
                    "conversations.json",
                    buildConversationsJson(sessions, messages, imageEntries)
                )
                zip.writeText("conversations.md", buildConversationsMarkdown(sessions, messages))
                if (mode == DataExportMode.ALL_DATA) {
                    zip.writeText("notes.json", buildNotesJson(notes))
                    zip.writeText("notes.md", buildNotesMarkdown(notes))
                    zip.writeText("topics.json", buildTopicsJson(topics, segments))
                }
            }
        }
    }

    private fun writeImages(
        zip: ZipOutputStream,
        messages: List<MessageEntity>
    ): Map<String, String> {
        val entries = linkedMapOf<String, String>()
        messages.forEach { message ->
            val imagePath = message.imagePath ?: return@forEach
            val imageFile = File(imagePath)
            if (!imageFile.isFile) return@forEach
            val extension = imageFile.extension.ifBlank { "jpg" }
            val entryName = "images/${message.id}.$extension"
            zip.putNextEntry(ZipEntry(entryName))
            imageFile.inputStream().buffered().use { input -> input.copyTo(zip) }
            zip.closeEntry()
            entries[message.id] = entryName
        }
        return entries
    }

    private fun buildManifest(
        mode: DataExportMode,
        sessions: List<SessionEntity>,
        messages: List<MessageEntity>,
        notes: List<NoteEntity>,
        topics: List<TopicEntity>,
        imageCount: Int
    ): String = JSONObject()
        .put("format", "cogno-export")
        .put("format_version", 1)
        .put("app_version", BuildConfig.VERSION_NAME)
        .put("exported_at", isoTime(System.currentTimeMillis()))
        .put("scope", if (mode == DataExportMode.ALL_DATA) "all_data" else "chats_only")
        .put("session_count", sessions.size)
        .put("message_count", messages.size)
        .put("note_count", notes.size)
        .put("topic_count", topics.size)
        .put("image_count", imageCount)
        .toString(2)

    private fun buildConversationsJson(
        sessions: List<SessionEntity>,
        messages: List<MessageEntity>,
        imageEntries: Map<String, String>
    ): String {
        val messagesBySession = messages.groupBy { it.sessionId }
        return JSONObject()
            .put(
                "sessions",
                JSONArray().apply {
                    sessions.forEach { session ->
                        put(
                            JSONObject()
                                .put("id", session.id)
                                .put("title", session.title)
                                .put("model_id", session.modelId ?: JSONObject.NULL)
                                .put("pinned", session.pinned)
                                .put("archived", session.archived)
                                .put("created_at", isoTime(session.createdAt))
                                .put("updated_at", isoTime(session.updatedAt))
                                .put(
                                    "messages",
                                    JSONArray().apply {
                                        messagesBySession[session.id].orEmpty().forEach { message ->
                                            put(messageJson(message, imageEntries[message.id]))
                                        }
                                    }
                                )
                        )
                    }
                }
            )
            .toString(2)
    }

    private fun messageJson(message: MessageEntity, imageEntry: String?): JSONObject =
        JSONObject()
            .put("id", message.id)
            .put("role", message.role)
            .put("content", message.content)
            .put("status", message.status)
            .put("error_code", message.errorCode ?: JSONObject.NULL)
            .put("token_count", message.tokenCount ?: JSONObject.NULL)
            .put("feedback", message.feedback ?: JSONObject.NULL)
            .put("image_file", imageEntry ?: JSONObject.NULL)
            .put("image_mime_type", message.imageMimeType ?: JSONObject.NULL)
            .put("created_at", isoTime(message.createdAt))
            .put("updated_at", isoTime(message.updatedAt))

    private fun buildConversationsMarkdown(
        sessions: List<SessionEntity>,
        messages: List<MessageEntity>
    ): String {
        val messagesBySession = messages.groupBy { it.sessionId }
        return buildString {
            appendLine("# Cogno 聊天记录")
            appendLine()
            sessions.forEach { session ->
                appendLine("## ${session.title}")
                appendLine()
                appendLine("- 创建时间：${displayTime(session.createdAt)}")
                appendLine("- 模型：${session.modelId.orEmpty().ifBlank { "未记录" }}")
                appendLine()
                messagesBySession[session.id].orEmpty().forEach { message ->
                    appendLine("### ${if (message.role == "user") "用户" else "AI"}")
                    appendLine()
                    if (!message.imagePath.isNullOrBlank()) appendLine("[图片：见压缩包 images 目录]")
                    appendLine(message.content.ifBlank { "[无文字内容]" })
                    appendLine()
                }
            }
        }
    }

    private fun buildNotesJson(notes: List<NoteEntity>): String =
        JSONObject()
            .put(
                "notes",
                JSONArray().apply {
                    notes.forEach { note ->
                        put(
                            JSONObject()
                                .put("id", note.id)
                                .put("title", note.title)
                                .put("content", note.content)
                                .put("source_session_id", note.sourceSessionId ?: JSONObject.NULL)
                                .put("pinned", note.pinned)
                                .put("created_at", isoTime(note.createdAt))
                                .put("updated_at", isoTime(note.updatedAt))
                        )
                    }
                }
            )
            .toString(2)

    private fun buildNotesMarkdown(notes: List<NoteEntity>): String = buildString {
        appendLine("# Cogno 笔记")
        appendLine()
        notes.forEach { note ->
            appendLine("## ${note.title}")
            appendLine()
            appendLine(note.content)
            appendLine()
        }
    }

    private fun buildTopicsJson(
        topics: List<TopicEntity>,
        segments: List<NoteTopicSegmentEntity>
    ): String {
        val segmentsByTopic = segments.groupBy { it.topicName }
        return JSONObject()
            .put(
                "topics",
                JSONArray().apply {
                    topics.forEach { topic ->
                        put(
                            JSONObject()
                                .put("id", topic.id)
                                .put("name", topic.name)
                                .put("keywords", topic.keywords)
                                .put("built_in", topic.isBuiltIn)
                                .put("enabled", topic.enabled)
                                .put("pinned", topic.pinned)
                                .put(
                                    "segments",
                                    JSONArray().apply {
                                        segmentsByTopic[topic.name].orEmpty().forEach { segment ->
                                            put(
                                                JSONObject()
                                                    .put("id", segment.id)
                                                    .put("note_id", segment.noteId)
                                                    .put("heading", segment.heading)
                                                    .put("content", segment.content)
                                                    .put("position", segment.position)
                                                    .put("created_at", isoTime(segment.createdAt))
                                            )
                                        }
                                    }
                                )
                        )
                    }
                }
            )
            .toString(2)
    }

    private fun ZipOutputStream.writeText(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun isoTime(time: Long): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(time))

    private fun displayTime(time: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(time))
}
