package com.ntoprevd.cogno.data.settings

data class AiSettings(
    val sourceMode: String = AiSourceMode.EXPERIENCE,
    val customProvider: String = CustomAiProvider.DEEPSEEK,
    val apiBaseUrl: String = DEFAULT_API_BASE_URL,
    val modelId: String = DEFAULT_EXPERIENCE_MODEL_ID,
    val apiKey: String = "",
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    val responseStyle: String = ResponseStylePreference.BALANCED,
    val temperature: Double = ResponseStylePreference.temperatureFor(ResponseStylePreference.BALANCED)
) {
    val hasApiKey: Boolean
        get() = sourceMode == AiSourceMode.EXPERIENCE || apiKey.isNotBlank()

    companion object {
        const val DEFAULT_API_BASE_URL = "https://api.deepseek.com/v1"
        const val DEFAULT_MODEL_ID = "deepseek-v4-flash"
        const val DEFAULT_EXPERIENCE_MODEL_ID = "glm-4.5-air"
        const val DEFAULT_SYSTEM_PROMPT = "你是 Cogno，一个简洁、可靠、善于整理思路的 AI 助手。"
    }
}

object AiSourceMode {
    const val EXPERIENCE = "experience"
    const val CUSTOM = "custom"
    val all = listOf(EXPERIENCE, CUSTOM)
}

object CustomAiProvider {
    const val DEEPSEEK = "deepseek"
    const val OPENAI = "openai"
    const val GLM = "glm"
    const val OTHER = "other"
    val all = listOf(DEEPSEEK, OPENAI, GLM, OTHER)

    fun defaultBaseUrl(value: String): String = when (value) {
        OPENAI -> "https://api.openai.com/v1"
        GLM -> "https://open.bigmodel.cn/api/paas/v4"
        DEEPSEEK -> AiSettings.DEFAULT_API_BASE_URL
        else -> ""
    }

    fun modelPresets(value: String): List<String> = when (value) {
        OPENAI -> listOf("gpt-5.5", "gpt-5.4", "gpt-5.4-mini")
        GLM -> listOf("glm-4.5-air", "glm-4.6v", "glm-4-flash")
        DEEPSEEK -> listOf("deepseek-v4-flash", "deepseek-v4-pro")
        else -> emptyList()
    }
}

object ResponseStylePreference {
    const val BALANCED = "balanced"
    const val COMPREHENSIVE = "comprehensive"
    const val FRIENDLY = "friendly"
    const val WARM = "warm"
    const val CONCISE = "concise"

    val all: List<String> = listOf(BALANCED, COMPREHENSIVE, FRIENDLY, WARM, CONCISE)

    fun temperatureFor(value: String): Double {
        return when (value) {
            COMPREHENSIVE -> 0.55
            FRIENDLY -> 0.75
            WARM -> 0.85
            CONCISE -> 0.35
            else -> 0.60
        }
    }

    fun instructionFor(value: String): String {
        return when (value) {
            COMPREHENSIVE -> "回答要全面、结构清楚，适合较长解释；不要为了完整而编造信息。"
            FRIENDLY -> "回答要友好自然，必要时给出简短解释和下一步建议。"
            WARM -> "回答要更有温度和鼓励感，但保持准确，不要夸张。"
            CONCISE -> "回答要简短直接，优先给出结论和可执行步骤。"
            else -> "回答要理智、清晰、不过度发散，兼顾准确性和可读性。"
        }
    }
}
