package com.ntoprevd.cogno.data.settings

import android.content.Context

class AiSettingsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )
    private val apiKeyCipher = ApiKeyCipher()

    fun load(): AiSettings {
        val apiKey = loadApiKey()
        return AiSettings(
            sourceMode = preferences.getString(KEY_SOURCE_MODE, null)
                ?.takeIf { it in AiSourceMode.all }
                ?: AiSourceMode.CUSTOM,
            customProvider = preferences.getString(KEY_CUSTOM_PROVIDER, null)
                ?.takeIf { it in CustomAiProvider.all }
                ?: CustomAiProvider.DEEPSEEK,
            apiBaseUrl = preferences.getString(KEY_API_BASE_URL, null)
                ?: AiSettings.DEFAULT_API_BASE_URL,
            modelId = preferences.getString(KEY_MODEL_ID, null)
                ?: AiSettings.DEFAULT_MODEL_ID,
            apiKey = apiKey,
            systemPrompt = preferences.getString(KEY_SYSTEM_PROMPT, null)
                ?: AiSettings.DEFAULT_SYSTEM_PROMPT,
            responseStyle = preferences.getString(KEY_RESPONSE_STYLE, null)
                ?.takeIf { it in ResponseStylePreference.all }
                ?: ResponseStylePreference.BALANCED,
            temperature = preferences.getFloat(
                KEY_TEMPERATURE,
                ResponseStylePreference.temperatureFor(ResponseStylePreference.BALANCED).toFloat()
            ).toDouble()
        )
    }

    fun save(settings: AiSettings) {
        val apiBaseUrl = settings.apiBaseUrl.trim().trimEnd('/')
            .ifBlank { AiSettings.DEFAULT_API_BASE_URL }
        val modelId = settings.modelId.trim()
            .ifBlank { AiSettings.DEFAULT_MODEL_ID }
        val systemPrompt = settings.systemPrompt.trim()
            .ifBlank { AiSettings.DEFAULT_SYSTEM_PROMPT }
        val responseStyle = settings.responseStyle
            .takeIf { it in ResponseStylePreference.all }
            ?: ResponseStylePreference.BALANCED
        val temperature = settings.temperature.coerceIn(MIN_TEMPERATURE, MAX_TEMPERATURE)
        val sourceMode = settings.sourceMode.takeIf { it in AiSourceMode.all } ?: AiSourceMode.CUSTOM
        val customProvider = settings.customProvider.takeIf { it in CustomAiProvider.all }
            ?: CustomAiProvider.DEEPSEEK
        // Do not silently erase an existing credential if Keystore access fails.
        val encryptedApiKey = apiKeyCipher.encrypt(settings.apiKey.trim())

        preferences.edit()
            .putString(KEY_SOURCE_MODE, sourceMode)
            .putString(KEY_CUSTOM_PROVIDER, customProvider)
            .putString(KEY_API_BASE_URL, apiBaseUrl)
            .putString(KEY_MODEL_ID, modelId)
            .putString(KEY_API_KEY_ENCRYPTED, encryptedApiKey)
            .remove(KEY_API_KEY)
            .putString(KEY_SYSTEM_PROMPT, systemPrompt)
            .putString(KEY_RESPONSE_STYLE, responseStyle)
            .putFloat(KEY_TEMPERATURE, temperature.toFloat())
            .apply()
    }

    private fun loadApiKey(): String {
        val encrypted = preferences.getString(KEY_API_KEY_ENCRYPTED, null).orEmpty()
        if (encrypted.isNotBlank()) {
            return runCatching { apiKeyCipher.decrypt(encrypted) }.getOrDefault("")
        }

        // Migrate API keys saved by older app versions.
        val legacy = preferences.getString(KEY_API_KEY, null).orEmpty()
        if (legacy.isBlank()) return ""
        val migrated = runCatching { apiKeyCipher.encrypt(legacy) }.getOrNull() ?: return legacy
        preferences.edit()
            .putString(KEY_API_KEY_ENCRYPTED, migrated)
            .remove(KEY_API_KEY)
            .apply()
        return legacy
    }

    companion object {
        private const val PREFERENCES_NAME = "cogno_ai_settings"
        private const val KEY_API_BASE_URL = "api_base_url"
        private const val KEY_SOURCE_MODE = "source_mode"
        private const val KEY_CUSTOM_PROVIDER = "custom_provider"
        private const val KEY_MODEL_ID = "model_id"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_API_KEY_ENCRYPTED = "api_key_encrypted"
        private const val KEY_SYSTEM_PROMPT = "system_prompt"
        private const val KEY_RESPONSE_STYLE = "response_style"
        private const val KEY_TEMPERATURE = "temperature"
        private const val MIN_TEMPERATURE = 0.0
        private const val MAX_TEMPERATURE = 1.2
    }
}
