package com.ntoprevd.cogno.data.network

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private val streamingCalls = StreamingCallController()

    fun cancelStreamingRequest() {
        streamingCalls.cancel()
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
        streamingCalls.register(call)
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
                val event = parseSseData(data) ?: continue
                if (event.done) break
                event.totalTokens?.let { totalTokens = it }
                val contentDelta = event.content ?: continue

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
            streamingCalls.clear(call)
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
        val primaryContent = executeTextCompletion(
            settings,
            buildNoteRequestBody(
                settings = settings,
                conversationTitle = conversationTitle,
                conversationText = conversationText,
                style = style,
                existingContent = existingContent,
                topicNames = topicNames,
                includeSegments = true
            )
        )
        if (primaryContent.isNotBlank()) {
            runCatching { parseNoteDraft(primaryContent, conversationTitle) }
                .getOrNull()
                ?.let { return@withContext it }
        }

        // 复杂 JSON 为空或被输出上限截断时，退化为正文请求，主题片段可在本地重建。
        val fallbackContent = executeTextCompletion(
            settings,
            buildNoteRequestBody(
                settings = settings,
                conversationTitle = conversationTitle,
                conversationText = conversationText,
                style = style,
                existingContent = existingContent,
                topicNames = topicNames,
                includeSegments = false
            )
        )
        if (fallbackContent.isBlank()) throw AiChatException("AI 没有生成笔记内容，请重试")
        parseNoteDraft(fallbackContent, conversationTitle)
    }

    suspend fun requestConversationTitle(
        settings: AiSettings,
        conversationText: String
    ): String = withContext(Dispatchers.IO) {
        validateSettings(settings)
        val body = JSONObject()
            .put("model", settings.modelId)
            .put(
                "messages",
                JSONArray()
                    .put(
                        JSONObject()
                            .put("role", "system")
                            .put(
                                "content",
                                "请根据首轮用户与 AI 对话生成一个准确、自然的中文会话标题。" +
                                    "只返回标题，不加引号、解释或 Markdown；控制在 6 至 18 个汉字。"
                            )
                    )
                    .put(JSONObject().put("role", "user").put("content", conversationText))
            )
            .put("stream", false)
            .put("temperature", NOTE_TEMPERATURE)
            .put("max_tokens", 48)
        sanitizeConversationTitle(executeTextCompletion(settings, body))
    }

    suspend fun fetchExperienceModels(): List<ExperienceModel> = withContext(Dispatchers.IO) {
        if (BuildConfig.EXPERIENCE_API_BASE_URL.isBlank()) return@withContext emptyList()
        val request = Request.Builder()
            .url("${BuildConfig.EXPERIENCE_API_BASE_URL.trimEnd('/')}/models")
            .addHeader("Content-Type", JSON_MEDIA_TYPE.toString())
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
        topicNames: List<String>,
        includeSegments: Boolean
    ): JSONObject {
        val responseFormat = if (includeSegments) {
            """{"title":"概括整段对话核心的简短标题","content":"本次新增的 Markdown 内容","segments":[]}"""
        } else {
            """{"title":"概括整段对话核心的简短标题","content":"本次新增的 Markdown 内容"}"""
        }
        val systemPrompt = """
            你是 Cogno 的结构化笔记助手。请把本次提供的用户和 AI 对话整理成清晰、可复习的中文 Markdown 笔记内容。
            要求：
            1. 只总结对话中已经出现的信息，不要编造。
            2. 先识别对话中真正独立的话题。只有当用户的核心问题、意图或讨论对象明显改变时，才新建一个二级标题。
            3. 每个独立话题使用一个二级标题；同一话题中的观点、情绪、哲学思考、案例或方法等不同侧面使用三级标题，不要拆成并列大话题。
            4. 三级标题下优先使用无序列表组织细节；只有步骤、时间顺序或明确排名时才使用有序列表。
            5. 去掉寒暄和重复内容，保留关键结论、步骤、概念、分歧和注意事项。标题应概括讨论对象，而不是照搬分类主题名称。
            6. 按用户选择的总结风格控制详略：$style。
            7. 如果提供了已有笔记，它只用于判断话题延续、参考既有结构和避免重复；绝对不要改写、复述或返回已有笔记。
            8. content 只能包含本次新增对话对应的追加内容；首次总结时才包含完整总结。若新增内容延续已有二级话题，直接使用合适的三级标题或无序列表，不要重复该二级标题；只有出现真正的新话题才增加二级标题。
            9. 主题分类与笔记标题层级是两件事：主题仅用于跨笔记归档，不能为了分类而把一个完整话题拆散。
            10. 返回严格 JSON，格式为 $responseFormat。
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
            if (includeSegments) {
                append("\n\n可用主题：")
                append(topicNames.joinToString().ifBlank { "未分类" })
                append(
                    "\n分类边界：" +
                        "身体健康侧重日常保健、习惯与体能，医疗健康侧重症状、疾病、诊断和治疗；" +
                        "心理状态侧重个人内在感受，亲密关系、家庭关系、人际社交按关系对象区分；" +
                        "财务规划侧重预算和资产安排，购物消费侧重具体商品与购买决策；" +
                        "兴趣创作侧重主动创作与技能兴趣，娱乐休闲侧重内容消费和放松；" +
                        "体育竞技侧重赛事和竞技活动，日常锻炼仍归身体健康。" +
                        "\n请在 title、content 字段之外返回 segments 数组。" +
                        "每项格式为 {\"topic\":\"主题\",\"heading\":\"所属话题或侧面标题\",\"content\":\"可独立理解的最小内容单元 Markdown\"}。" +
                        "每个 segment 只选择一个最贴切的主题，topic 必须严格使用可用主题中的原文，不要自造近义主题。" +
                        "heading 描述内容在原对话中的话题或侧面，不要直接把分类主题名称当作 heading。" +
                        "同一大话题可以包含多个不同主题的 segment，但 content 中仍应保持为一个二级标题下的多个三级侧面。" +
                        "segment.content 不要复制整段笔记正文，只保留 1 至 3 条可独立理解的原子要点，单项尽量不超过 180 个汉字。" +
                        "更新已有笔记时，content 和 segments 都只返回本次新增内容，不要返回任何已有内容。" +
                        "规则修改不影响历史单元。"
                )
            }
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
            // 笔记整理需要稳定结构，低温度也能减少无意义扩写带来的等待。
            .put("temperature", NOTE_TEMPERATURE)
            .put(
                "max_tokens",
                if (includeSegments) noteMaxTokens(style) else NOTE_MAX_TOKENS_FALLBACK
            )
    }

    private fun executeTextCompletion(settings: AiSettings, body: JSONObject): String {
        val request = authorizedRequest(settings, "chat/completions")
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
        return client.newCall(request).execute().use { response ->
            val bodyText = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw AiChatException(parseErrorMessage(bodyText, response.code))
            }
            val json = runCatching { JSONObject(bodyText) }.getOrElse {
                throw AiChatException("AI 返回内容不是有效 JSON")
            }
            extractAssistantContent(json)
        }
    }

    private fun noteMaxTokens(style: String): Int {
        val normalized = style.lowercase()
        return when {
            "简洁" in style || "concise" in normalized -> NOTE_MAX_TOKENS_CONCISE
            "详细" in style || "detailed" in normalized -> NOTE_MAX_TOKENS_DETAILED
            else -> NOTE_MAX_TOKENS_STANDARD
        }
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
        if (cleaned.startsWith("{") || cleaned.startsWith("[")) {
            throw AiChatException("AI 返回的笔记 JSON 不完整，请重试")
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
        private const val NOTE_TEMPERATURE = 0.2
        private const val NOTE_MAX_TOKENS_CONCISE = 1_000
        private const val NOTE_MAX_TOKENS_STANDARD = 1_800
        private const val NOTE_MAX_TOKENS_DETAILED = 2_800
        private const val NOTE_MAX_TOKENS_FALLBACK = 3_200
    }
}

internal fun extractAssistantContent(response: JSONObject): String {
    val content = response.optJSONArray("choices")
        ?.optJSONObject(0)
        ?.optJSONObject("message")
        ?.opt("content")
    return when (content) {
        is String -> content.trim()
        is JSONArray -> buildList {
            for (index in 0 until content.length()) {
                when (val part = content.opt(index)) {
                    is String -> add(part)
                    is JSONObject -> part.optString("text").takeIf(String::isNotBlank)?.let(::add)
                }
            }
        }.joinToString("").trim()
        else -> ""
    }
}

internal fun sanitizeConversationTitle(raw: String): String {
    return raw.lineSequence()
        .firstOrNull(String::isNotBlank)
        .orEmpty()
        .trim()
        .removePrefix("#")
        .trim()
        .trim('"', '\'', '“', '”')
        .take(24)
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

internal class StreamingCallController {
    private val lock = Any()
    private var activeCall: Call? = null
    private var cancelNextCall = false

    fun register(call: Call) {
        synchronized(lock) {
            activeCall = call
            if (cancelNextCall) {
                cancelNextCall = false
                call.cancel()
            }
        }
    }

    fun cancel() {
        synchronized(lock) {
            val call = activeCall
            if (call == null) {
                // Covers a stop tap after message persistence but before OkHttp creates the Call.
                cancelNextCall = true
            } else {
                call.cancel()
            }
        }
    }

    fun clear(call: Call) {
        synchronized(lock) {
            if (activeCall === call) activeCall = null
        }
    }
}

internal data class SseEvent(
    val content: String? = null,
    val totalTokens: Int? = null,
    val done: Boolean = false
)

internal fun parseSseData(data: String): SseEvent? {
    if (data.isBlank()) return null
    if (data == "[DONE]") return SseEvent(done = true)
    val chunk = runCatching { JSONObject(data) }.getOrNull() ?: return null
    val usage = chunk.optJSONObject("usage")
    val totalTokens = if (usage?.has("total_tokens") == true) {
        usage.optInt("total_tokens")
    } else {
        null
    }
    val delta = chunk
        .optJSONArray("choices")
        ?.optJSONObject(0)
        ?.optJSONObject("delta")
    val content = if (
        delta != null &&
        delta.has("content") &&
        !delta.isNull("content")
    ) {
        delta.optString("content").takeIf(String::isNotEmpty)
    } else {
        null
    }
    return SseEvent(content = content, totalTokens = totalTokens)
}
