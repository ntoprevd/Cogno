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

    companion object {
        private const val PREFERENCES_NAME = "cogno_app_settings"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_LANGUAGE = "language"
    }
}
