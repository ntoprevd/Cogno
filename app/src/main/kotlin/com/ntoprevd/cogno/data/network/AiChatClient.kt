package com.ntoprevd.cogno.data.network

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.Settings
import android.util.Base64
import com.ntoprevd.cogno.BuildConfig
import com.ntoprevd.cogno.data.db.entity.MessageEntity
import com.ntoprevd.cogno.data.settings.AiSettings
import com.ntoprevd.cogno.data.settings.AiSourceMode
import com.ntoprevd.cogno.data.settings.CustomAiProvider
import com.ntoprevd.cogno.data.settings.ResponseStylePreference
import java.io.IOException
import java.io.File
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

class AiChatClient(context: Context) {
    private val appContext = context.applicationContext
    private val deviceId = Settings.Secure.getString(
        appContext.contentResolver,
        Settings.Secure.ANDROID_ID
    ).orEmpty()
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private val activeStreamingCall = AtomicReference<Call?>(null)

    fun cancelStreamingRequest() {
        activeStreamingCall.getAndSet(null)?.cancel()
    }

    suspend fun requestChatCompletion(
        settings: AiSettings,
        messages: List<MessageEntity>
    ): AiChatResponse = withContext(Dispatchers.IO) {
        validateSettings(settings)
        val request = authorizedRequest(settings, "chat/completions")
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
        validateSettings(settings)
        val request = authorizedRequest(settings, "chat/completions")
            .post(buildRequestBody(settings, messages, stream = true).toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val call = client.newCall(request)
        activeStreamingCall.set(call)
        try {
            call.execute().use { response ->
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
        } catch (error: IOException) {
            if (call.isCanceled()) throw AiRequestCancelledException()
            throw error
        } finally {
            activeStreamingCall.compareAndSet(call, null)
        }
    }

    suspend fun testConnection(settings: AiSettings): String = withContext(Dispatchers.IO) {
        validateSettings(settings)
        val request = authorizedRequest(settings, "chat/completions")
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
        existingContent: String? = null,
        topicNames: List<String> = emptyList()
    ): AiNoteDraft = withContext(Dispatchers.IO) {
        validateSettings(settings)
        val request = authorizedRequest(settings, "chat/completions")
            .post(
                buildNoteRequestBody(
                    settings = settings,
                    conversationTitle = conversationTitle,
                    conversationText = conversationText,
                    style = style,
                    existingContent = existingContent,
                    topicNames = topicNames
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

    suspend fun fetchExperienceModels(): List<ExperienceModel> = withContext(Dispatchers.IO) {
        if (BuildConfig.EXPERIENCE_API_BASE_URL.isBlank()) return@withContext emptyList()
        val request = Request.Builder()
            .url("${BuildConfig.EXPERIENCE_API_BASE_URL.trimEnd('/')}/models")
            .addHeader("Content-Type", JSON_MEDIA_TYPE.toString())
            .addHeader("X-Cogno-Device-Id", deviceId)
            .apply {
                if (BuildConfig.EXPERIENCE_APP_TOKEN.isNotBlank()) {
                    addHeader("Authorization", "Bearer ${BuildConfig.EXPERIENCE_APP_TOKEN}")
                }
            }
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext emptyList()
            val data = JSONObject(response.body?.string().orEmpty()).optJSONArray("data")
                ?: return@withContext emptyList()
            buildList {
                for (index in 0 until data.length()) {
                    val item = data.optJSONObject(index) ?: continue
                    val id = item.optString("id").trim()
                    if (id.isBlank()) continue
                    add(
                        ExperienceModel(
                            id = id,
                            label = item.optString("label", id),
                            description = item.optString("description")
                        )
                    )
                }
            }
        }
    }

    private fun buildRequestBody(
        settings: AiSettings,
        messages: List<MessageEntity>,
        stream: Boolean = false
    ): JSONObject {
        val requestMessages = JSONArray()
        val systemPrompt = buildSystemPrompt(settings)
        if (systemPrompt.isNotBlank()) {
            requestMessages.put(
                JSONObject()
                    .put("role", "system")
                    .put("content", systemPrompt)
            )
        }

        val completedMessages = messages
            .filter {
                it.status == "completed" &&
                    (it.content.isNotBlank() || !it.imagePath.isNullOrBlank())
            }
        val activeImageMessageId = completedMessages
            .lastOrNull()
            ?.takeIf { it.role == "user" && !it.imagePath.isNullOrBlank() }
            ?.id

        completedMessages.forEach { message ->
                requestMessages.put(
                    JSONObject()
                        .put("role", message.role)
                        .put(
                            "content",
                            buildMessageContent(
                                settings = settings,
                                message = message,
                                includeImage = message.id == activeImageMessageId
                            )
                        )
                )
            }

        return JSONObject()
            .put("model", settings.modelId)
            .put("messages", requestMessages)
            .put("stream", stream)
            .put("temperature", normalizedTemperature(settings.temperature))
            .apply {
                // OpenAI-compatible 流式接口需要显式请求，才会在结束块返回 usage 汇总。
                if (stream) {
                    put("stream_options", JSONObject().put("include_usage", true))
                }
            }
    }

    private fun buildMessageContent(
        settings: AiSettings,
        message: MessageEntity,
        includeImage: Boolean
    ): Any {
        val imagePath = message.imagePath
        if (message.role != "user" || imagePath.isNullOrBlank()) {
            return message.content
        }

        val isGlmModel = settings.modelId.startsWith("glm-", ignoreCase = true)
        val isKnownTextOnlyModel = settings.sourceMode == AiSourceMode.CUSTOM &&
            (
                settings.customProvider == CustomAiProvider.DEEPSEEK ||
                    (isGlmModel && !isGlmVisionModel(settings.modelId))
                )
        if (!includeImage || isKnownTextOnlyModel) {
            // 历史图片和已知文本模型都只保留文字上下文，避免发送必然失败的 image_url。
            return listOf("[图片消息]", message.content)
                .filter { it.isNotBlank() }
                .joinToString(" ")
        }

        val imageFile = File(imagePath)
        if (!imageFile.isFile) {
            return message.content.ifBlank { "[图片已不可用]" }
        }

        val mimeType = message.imageMimeType?.takeIf { it.startsWith("image/") } ?: "image/jpeg"
        val imageBytes = if (isGlmModel) {
            compressedGlmRequestBytes(imageFile)
        } else {
            imageFile.readBytes()
        }
        val base64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
        val imageUrl = if (isGlmModel) {
            // 智谱 GLM 的 Base64 接口要求直接传编码内容，不使用 Data URI 前缀。
            base64
        } else {
            "data:$mimeType;base64,$base64"
        }
        val contentParts = JSONArray()
        contentParts.put(
            JSONObject()
                .put("type", "image_url")
                .put(
                    "image_url",
                    JSONObject().put("url", imageUrl)
                )
        )
        contentParts.put(
            JSONObject()
                .put("type", "text")
                .put("text", message.content.ifBlank { "请描述这张图片。" })
        )
        return contentParts
    }

    private fun compressedGlmRequestBytes(imageFile: File): ByteArray {
        val sourceBitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
            ?: return imageFile.readBytes()
        var workingBitmap = scaleBitmapToLongestSide(sourceBitmap, GLM_REQUEST_IMAGE_SIDE)
        return try {
            var quality = GLM_REQUEST_JPEG_QUALITY
            var encoded = encodeJpeg(workingBitmap, quality)
            while (encoded.size > GLM_REQUEST_MAX_BYTES && quality > GLM_REQUEST_MIN_QUALITY) {
                quality -= 5
                encoded = encodeJpeg(workingBitmap, quality)
            }
            while (encoded.size > GLM_REQUEST_MAX_BYTES && maxOf(workingBitmap.width, workingBitmap.height) > 320) {
                val previous = workingBitmap
                workingBitmap = scaleBitmapToLongestSide(
                    previous,
                    (maxOf(previous.width, previous.height) * 0.8f).toInt()
                )
                if (previous !== sourceBitmap) previous.recycle()
                encoded = encodeJpeg(workingBitmap, GLM_REQUEST_MIN_QUALITY)
            }
            encoded
        } finally {
            if (workingBitmap !== sourceBitmap) workingBitmap.recycle()
            sourceBitmap.recycle()
        }
    }

    private fun scaleBitmapToLongestSide(bitmap: Bitmap, targetSide: Int): Bitmap {
        val longestSide = maxOf(bitmap.width, bitmap.height)
        if (longestSide <= targetSide) return bitmap
        val scale = targetSide.toFloat() / longestSide
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true
        )
    }

    private fun encodeJpeg(bitmap: Bitmap, quality: Int): ByteArray {
        return ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
            output.toByteArray()
        }
    }

    private fun isGlmVisionModel(modelId: String): Boolean {
        val normalized = modelId.lowercase()
        return normalized.contains("glm") &&
            (normalized.contains("4.6v") || normalized.contains("4.1v") || normalized.contains("5v"))
    }

    private fun normalizedTemperature(value: Double): Double {
        return (value * 100.0).roundToInt() / 100.0
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

    private fun validateSettings(settings: AiSettings) {
        if (settings.sourceMode == AiSourceMode.EXPERIENCE) {
            if (BuildConfig.EXPERIENCE_API_BASE_URL.isBlank()) {
                throw AiChatException("体验模型后端地址尚未配置")
            }
        } else if (!settings.hasApiKey) {
            throw AiChatException("请先在设置页填写 API Key")
        }
    }

    private fun authorizedRequest(settings: AiSettings, path: String): Request.Builder {
        val experienceMode = settings.sourceMode == AiSourceMode.EXPERIENCE
        val baseUrl = if (experienceMode) {
            BuildConfig.EXPERIENCE_API_BASE_URL
        } else {
            settings.apiBaseUrl
        }
        val token = if (experienceMode) BuildConfig.EXPERIENCE_APP_TOKEN else settings.apiKey
        return Request.Builder()
            .url("${baseUrl.trimEnd('/')}/$path")
            .addHeader("Content-Type", JSON_MEDIA_TYPE.toString())
            .apply {
                // A stable identifier is only needed by Cogno's own experience service.
                if (experienceMode) addHeader("X-Cogno-Device-Id", deviceId)
                if (token.isNotBlank()) addHeader("Authorization", "Bearer $token")
            }
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
            .put("temperature", normalizedTemperature(settings.temperature))
            .put("max_tokens", 8)
    }

    private fun buildNoteRequestBody(
        settings: AiSettings,
        conversationTitle: String,
        conversationText: String,
        style: String,
        existingContent: String?,
        topicNames: List<String>
    ): JSONObject {
        val systemPrompt = """
            你是 Cogno 的结构化笔记助手。请把本次提供的用户和 AI 对话整理成清晰、可复习的中文 Markdown 笔记内容。
            要求：
            1. 只总结对话中已经出现的信息，不要编造。
            2. 使用二级/三级标题、要点列表和必要的解释。
            3. 去掉寒暄和重复内容，保留关键结论、步骤、概念和注意事项。
            4. 按用户选择的总结风格控制详略：$style。
            5. 如果提供了已有笔记，它只用于参考既有结构和避免重复；绝对不要改写、复述或返回已有笔记。
            6. content 只能包含本次新增对话对应的追加内容；首次总结时才包含完整总结。
            7. 返回严格 JSON，格式为 {"title":"简短标题","content":"本次新增的 Markdown 内容","segments":[]}。
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
            append("\n\n可用主题：")
            append(topicNames.joinToString().ifBlank { "未分类" })
            append(
                "\n请在 title、content 字段之外返回 segments 数组。" +
                    "每项格式为 {\"topic\":\"主题\",\"heading\":\"段落标题\",\"content\":\"最小内容单元 Markdown\"}。" +
                    "更新已有笔记时，content 和 segments 都只返回本次新增内容，不要返回任何已有内容。" +
                    "主题必须优先从可用主题中选择；规则修改不影响历史单元。"
            )
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
            .put("temperature", normalizedTemperature(settings.temperature))
    }

    private fun buildSystemPrompt(settings: AiSettings): String {
        val styleInstruction = ResponseStylePreference.instructionFor(settings.responseStyle)
        return listOf(settings.systemPrompt, styleInstruction)
            .filter { it.isNotBlank() }
            .joinToString(separator = "\n\n")
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
        val segments = parsed?.optJSONArray("segments")?.let { array ->
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val segmentContent = item.optString("content").trim()
                    if (segmentContent.isBlank()) continue
                    add(
                        AiNoteSegment(
                            topic = item.optString("topic").trim(),
                            heading = item.optString("heading").trim(),
                            content = segmentContent
                        )
                    )
                }
            }
        }.orEmpty()
        if (content.isNotBlank()) {
            return AiNoteDraft(
                title = title.ifBlank { fallbackTitle.ifBlank { "会话笔记" } },
                content = content,
                segments = segments
            )
        }
        if (parsed != null) {
            throw AiChatException("AI 返回的笔记正文为空，请重试")
        }

        return AiNoteDraft(
            title = fallbackTitle.ifBlank { "会话笔记" },
            content = rawContent,
            segments = segments
        )
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val SSE_DATA_PREFIX = "data:"
        private const val SSE_DONE_MARKER = "[DONE]"
        private const val GLM_REQUEST_IMAGE_SIDE = 768
        private const val GLM_REQUEST_JPEG_QUALITY = 65
        private const val GLM_REQUEST_MIN_QUALITY = 35
        private const val GLM_REQUEST_MAX_BYTES = 48 * 1024
    }
}

data class AiChatResponse(
    val content: String,
    val totalTokens: Int?
)

data class AiNoteDraft(
    val title: String,
    val content: String,
    val segments: List<AiNoteSegment> = emptyList()
)

data class AiNoteSegment(
    val topic: String,
    val heading: String,
    val content: String
)

data class ExperienceModel(
    val id: String,
    val label: String,
    val description: String
)

class AiChatException(message: String, cause: Throwable? = null) : IOException(message, cause)

class AiRequestCancelledException : IOException("Generation cancelled")
