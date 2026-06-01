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

    companion object {
        private const val PREFERENCES_NAME = "cogno_app_settings"
        private const val KEY_DARK_MODE = "dark_mode"
    }
}
