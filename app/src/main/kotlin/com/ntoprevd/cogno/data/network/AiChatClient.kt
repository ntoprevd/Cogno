package com.ntoprevd.cogno.data.network

import com.ntoprevd.cogno.data.db.entity.MessageEntity
import com.ntoprevd.cogno.data.settings.AiSettings
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class AiChatClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun requestChatCompletion(
        settings: AiSettings,
        messages: List<MessageEntity>
    ): AiChatResponse = withContext(Dispatchers.IO) {
        if (!settings.hasApiKey) {
            throw AiChatException("请先在设置页填写 API Key")
        }

        val request = Request.Builder()
            .url("${settings.apiBaseUrl.trimEnd('/')}/chat/completions")
            .addHeader("Authorization", "Bearer ${settings.apiKey}")
            .addHeader("Content-Type", JSON_MEDIA_TYPE.toString())
            .post(buildRequestBody(settings, messages).toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).execute().use { response ->
            val bodyText = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw AiChatException(parseErrorMessage(bodyText, response.code))
            }

            val json = runCatching { JSONObject(bodyText) }.getOrElse {
                throw AiChatException("AI 返回内容不是有效 JSON")
            }
            val choice = json.optJSONArray("choices")?.optJSONObject(0)
            val content = choice
                ?.optJSONObject("message")
                ?.optString("content")
                ?.trim()
                .orEmpty()
            if (content.isBlank()) {
                throw AiChatException("AI 返回内容为空")
            }

            val usage = json.optJSONObject("usage")
            AiChatResponse(
                content = content,
                totalTokens = if (usage?.has("total_tokens") == true) usage.optInt("total_tokens") else null
            )
        }
    }

    suspend fun streamChatCompletion(
        settings: AiSettings,
        messages: List<MessageEntity>,
        onContentDelta: suspend (String) -> Unit
    ): AiChatResponse = withContext(Dispatchers.IO) {
        if (!settings.hasApiKey) {
            throw AiChatException("请先在设置页填写 API Key")
        }

        val request = Request.Builder()
            .url("${settings.apiBaseUrl.trimEnd('/')}/chat/completions")
            .addHeader("Authorization", "Bearer ${settings.apiKey}")
            .addHeader("Content-Type", JSON_MEDIA_TYPE.toString())
            .post(buildRequestBody(settings, messages, stream = true).toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val bodyText = response.body?.string().orEmpty()
                throw AiChatException(parseErrorMessage(bodyText, response.code))
            }

            val contentBuilder = StringBuilder()
            var totalTokens: Int? = null
            val source = response.body?.source() ?: throw AiChatException("AI 响应为空")

            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: continue
                if (!line.startsWith(SSE_DATA_PREFIX)) continue

                val data = line.removePrefix(SSE_DATA_PREFIX).trim()
                if (data == SSE_DONE_MARKER) break
                if (data.isBlank()) continue

                val chunk = runCatching { JSONObject(data) }.getOrNull() ?: continue
                val usage = chunk.optJSONObject("usage")
                if (usage?.has("total_tokens") == true) {
                    totalTokens = usage.optInt("total_tokens")
                }

                val delta = chunk
                    .optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("delta")
                if (delta == null || !delta.has("content") || delta.isNull("content")) continue

                val contentDelta = delta.getString("content")
                    .takeIf { it.isNotEmpty() }
                    ?: continue

                contentBuilder.append(contentDelta)
                onContentDelta(contentDelta)
            }

            val content = contentBuilder.toString().trim()
            if (content.isBlank()) {
                throw AiChatException("AI 返回内容为空")
            }
            AiChatResponse(content = content, totalTokens = totalTokens)
        }
    }

    suspend fun testConnection(settings: AiSettings): String = withContext(Dispatchers.IO) {
        if (!settings.hasApiKey) {
            throw AiChatException("请先填写 API Key")
        }

        val request = Request.Builder()
            .url("${settings.apiBaseUrl.trimEnd('/')}/chat/completions")
            .addHeader("Authorization", "Bearer ${settings.apiKey}")
            .addHeader("Content-Type", JSON_MEDIA_TYPE.toString())
            .post(buildTestRequestBody(settings).toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).execute().use { response ->
            val bodyText = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw AiChatException(parseErrorMessage(bodyText, response.code))
            }
            "连接成功"
        }
    }

    suspend fun requestNoteDraft(
        settings: AiSettings,
        conversationTitle: String,
        conversationText: String,
        style: String,
        existingContent: String? = null
    ): AiNoteDraft = withContext(Dispatchers.IO) {
        if (!settings.hasApiKey) {
            throw AiChatException("请先在设置页填写 API Key")
        }

        val request = Request.Builder()
            .url("${settings.apiBaseUrl.trimEnd('/')}/chat/completions")
            .addHeader("Authorization", "Bearer ${settings.apiKey}")
            .addHeader("Content-Type", JSON_MEDIA_TYPE.toString())
            .post(
                buildNoteRequestBody(
                    settings = settings,
                    conversationTitle = conversationTitle,
                    conversationText = conversationText,
                    style = style,
                    existingContent = existingContent
                ).toString().toRequestBody(JSON_MEDIA_TYPE)
            )
            .build()

        client.newCall(request).execute().use { response ->
            val bodyText = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw AiChatException(parseErrorMessage(bodyText, response.code))
            }

            val json = runCatching { JSONObject(bodyText) }.getOrElse {
                throw AiChatException("AI 返回内容不是有效 JSON")
            }
            val content = json
                .optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                ?.trim()
                .orEmpty()
            if (content.isBlank()) {
                throw AiChatException("AI 没有生成笔记内容")
            }
            parseNoteDraft(content, conversationTitle)
        }
    }

    private fun buildRequestBody(
        settings: AiSettings,
        messages: List<MessageEntity>,
        stream: Boolean = false
    ): JSONObject {
        val requestMessages = JSONArray()
        if (settings.systemPrompt.isNotBlank()) {
            requestMessages.put(
                JSONObject()
                    .put("role", "system")
                    .put("content", settings.systemPrompt)
            )
        }

        messages
            .filter { it.status == "completed" && it.content.isNotBlank() }
            .forEach { message ->
                requestMessages.put(
                    JSONObject()
                        .put("role", message.role)
                        .put("content", message.content)
                )
            }

        return JSONObject()
            .put("model", settings.modelId)
            .put("messages", requestMessages)
            .put("stream", stream)
    }

    private fun parseErrorMessage(bodyText: String, code: Int): String {
        val apiMessage = runCatching {
            JSONObject(bodyText)
                .optJSONObject("error")
                ?.optString("message")
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()
        return apiMessage ?: "AI 请求失败，HTTP $code"
    }

    private fun buildTestRequestBody(settings: AiSettings): JSONObject {
        return JSONObject()
            .put("model", settings.modelId)
            .put(
                "messages",
                JSONArray()
                    .put(
                        JSONObject()
                            .put("role", "user")
                            .put("content", "请回复 OK。")
                    )
            )
            .put("stream", false)
            .put("max_tokens", 8)
    }

    private fun buildNoteRequestBody(
        settings: AiSettings,
        conversationTitle: String,
        conversationText: String,
        style: String,
        existingContent: String?
    ): JSONObject {
        val systemPrompt = """
            你是 Cogno 的结构化笔记助手。请把用户和 AI 的对话整理成一篇清晰、可复习的中文 Markdown 笔记。
            要求：
            1. 只总结对话中已经出现的信息，不要编造。
            2. 使用二级/三级标题、要点列表和必要的解释。
            3. 去掉寒暄和重复内容，保留关键结论、步骤、概念和注意事项。
            4. 按用户选择的总结风格控制详略：$style。
            5. 如果提供了已有笔记，请在已有笔记基础上合并新信息，避免重复堆叠。
            6. 返回严格 JSON，格式为 {"title":"简短标题","content":"Markdown 内容"}。
        """.trimIndent()
        val userContent = buildString {
            append("会话标题：")
            append(conversationTitle)
            append("\n\n")
            if (!existingContent.isNullOrBlank()) {
                append("已有笔记：\n")
                append(existingContent)
                append("\n\n")
            }
            append("对话内容：\n")
            append(conversationText)
        }

        return JSONObject()
            .put("model", settings.modelId)
            .put(
                "messages",
                JSONArray()
                    .put(JSONObject().put("role", "system").put("content", systemPrompt))
                    .put(
                        JSONObject()
                            .put("role", "user")
                            .put("content", userContent)
                    )
            )
            .put("stream", false)
    }

    private fun parseNoteDraft(rawContent: String, fallbackTitle: String): AiNoteDraft {
        val cleaned = rawContent
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val parsed = runCatching { JSONObject(cleaned) }.getOrNull()
        val title = parsed?.optString("title")?.trim().orEmpty()
        val content = parsed?.optString("content")?.trim().orEmpty()
        if (content.isNotBlank()) {
            return AiNoteDraft(
                title = title.ifBlank { fallbackTitle.ifBlank { "会话笔记" } },
                content = content
            )
        }

        return AiNoteDraft(
            title = fallbackTitle.ifBlank { "会话笔记" },
            content = rawContent
        )
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val SSE_DATA_PREFIX = "data:"
        private const val SSE_DONE_MARKER = "[DONE]"
    }
}

data class AiChatResponse(
    val content: String,
    val totalTokens: Int?
)

data class AiNoteDraft(
    val title: String,
    val content: String
)

class AiChatException(message: String, cause: Throwable? = null) : IOException(message, cause)
