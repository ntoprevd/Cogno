package com.ntoprevd.cogno.data.settings

data class AiSettings(
    val apiBaseUrl: String = DEFAULT_API_BASE_URL,
    val modelId: String = DEFAULT_MODEL_ID,
    val apiKey: String = "",
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT
) {
    val hasApiKey: Boolean
        get() = apiKey.isNotBlank()

    companion object {
        const val DEFAULT_API_BASE_URL = "https://api.deepseek.com/v1"
        const val DEFAULT_MODEL_ID = "deepseek-v4-flash"
        const val DEFAULT_SYSTEM_PROMPT = "你是 Cogno，一个简洁、可靠、善于整理思路的 AI 助手。"
    }
}
