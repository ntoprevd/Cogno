package com.ntoprevd.cogno.data.settings

import android.content.Context

class AppSettingsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun loadDarkModePreference(): String {
        val saved = preferences.getString(KEY_DARK_MODE, null)
        return if (saved in DarkModePreference.all) saved!! else DarkModePreference.SYSTEM
    }

    fun saveDarkModePreference(value: String) {
        val normalized = if (value in DarkModePreference.all) value else DarkModePreference.SYSTEM
        preferences.edit()
            .putString(KEY_DARK_MODE, normalized)
            .apply()
    }

    fun loadLanguagePreference(): String {
        val saved = preferences.getString(KEY_LANGUAGE, null)
        return if (saved in AppLanguagePreference.all) saved!! else AppLanguagePreference.ZH_CN
    }

    fun saveLanguagePreference(value: String) {
        val normalized = if (value in AppLanguagePreference.all) value else AppLanguagePreference.ZH_CN
        preferences.edit()
            .putString(KEY_LANGUAGE, normalized)
            .apply()
    }

    fun loadUserName(): String =
        preferences.getString(KEY_USER_NAME, null)?.trim().orEmpty().ifBlank { DEFAULT_USER_NAME }

    fun saveUserName(value: String) {
        preferences.edit()
            .putString(KEY_USER_NAME, value.trim().ifBlank { DEFAULT_USER_NAME })
            .apply()
    }

    fun loadAvatarUri(): String =
        preferences.getString(KEY_AVATAR_URI, null).orEmpty()

    fun saveAvatarUri(value: String) {
        preferences.edit()
            .putString(KEY_AVATAR_URI, value)
            .apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "cogno_app_settings"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_AVATAR_URI = "avatar_uri"
        const val DEFAULT_USER_NAME = "Cogno User"
    }
}
