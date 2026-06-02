package com.ntoprevd.cogno.data.settings

object DarkModePreference {
    const val SYSTEM = "system"
    const val LIGHT = "light"
    const val DARK = "dark"

    val all: List<String> = listOf(SYSTEM, LIGHT, DARK)
}

object AppLanguagePreference {
    const val ZH_CN = "zh_cn"
    const val EN = "en"

    val all: List<String> = listOf(ZH_CN, EN)
}
