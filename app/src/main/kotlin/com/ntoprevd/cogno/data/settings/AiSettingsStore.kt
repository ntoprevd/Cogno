package com.ntoprevd.cogno.data.settings

import android.content.Context

class AiSettingsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun load(): AiSettings {
        return AiSettings(
            apiBaseUrl = preferences.getString(KEY_API_BASE_URL, null)
                ?: AiSettings.DEFAULT_API_BASE_URL,
            modelId = preferences.getString(KEY_MODEL_ID, null)
                ?: AiSettings.DEFAULT_MODEL_ID,
            apiKey = preferences.getString(KEY_API_KEY, null).orEmpty(),
            systemPrompt = preferences.getString(KEY_SYSTEM_PROMPT, null)
                ?: AiSettings.DEFAULT_SYSTEM_PROMPT
        )
    }

    fun save(settings: AiSettings) {
        val apiBaseUrl = settings.apiBaseUrl.trim().trimEnd('/')
            .ifBlank { AiSettings.DEFAULT_API_BASE_URL }
        val modelId = settings.modelId.trim()
            .ifBlank { AiSettings.DEFAULT_MODEL_ID }
        val systemPrompt = settings.systemPrompt.trim()
            .ifBlank { AiSettings.DEFAULT_SYSTEM_PROMPT }

        // API Key 只保存在本机 SharedPreferences，不写入仓库或构建配置。
        preferences.edit()
            .putString(KEY_API_BASE_URL, apiBaseUrl)
            .putString(KEY_MODEL_ID, modelId)
            .putString(KEY_API_KEY, settings.apiKey.trim())
            .putString(KEY_SYSTEM_PROMPT, systemPrompt)
            .apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "cogno_ai_settings"
        private const val KEY_API_BASE_URL = "api_base_url"
        private const val KEY_MODEL_ID = "model_id"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_SYSTEM_PROMPT = "system_prompt"
    }
}
