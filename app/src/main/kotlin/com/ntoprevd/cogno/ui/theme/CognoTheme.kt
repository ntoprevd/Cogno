package com.ntoprevd.cogno.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.ntoprevd.cogno.data.settings.DarkModePreference

val CognoPrimary = Color(0xFFE66A3C)
val CognoDarkPrimary = Color(0xFFF28B62)
val CognoBackground = Color(0xFFFEFAF5)
val CognoSurface = Color(0xFFFFFFFF)
val CognoUserBubble = Color(0xFFF3E8E1)
val CognoText = Color(0xFF2C2A27)
val CognoMuted = Color(0xFF9B9188)
val CognoLine = Color(0xFFEFE9E4)
val CognoDarkBackground = Color(0xFF141210)
val CognoDarkSurface = Color(0xFF1E1B18)
val CognoDarkText = Color(0xFFF0EBE6)
val CognoDarkLine = Color(0xFF2D2824)
val CognoDarkUserBubble = Color(0xFF2F2A26)

private val LightColors = lightColorScheme(
    primary = CognoPrimary,
    background = CognoBackground,
    surface = CognoSurface,
    onPrimary = Color.White,
    onBackground = CognoText,
    onSurface = CognoText,
    outline = CognoLine
)

private val DarkColors = darkColorScheme(
    primary = CognoDarkPrimary,
    background = CognoDarkBackground,
    surface = CognoDarkSurface,
    onPrimary = Color.White,
    onBackground = CognoDarkText,
    onSurface = CognoDarkText,
    outline = CognoDarkLine
)

private val LocalCognoDarkTheme = staticCompositionLocalOf { false }

@Composable
fun isCognoDarkTheme(): Boolean = LocalCognoDarkTheme.current

@Composable
fun CognoTheme(
    darkModePreference: String = DarkModePreference.SYSTEM,
    onDarkModeChanged: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    val followsSystemDark = isSystemInDarkTheme()
    val isDark = when (darkModePreference) {
        DarkModePreference.LIGHT -> false
        DarkModePreference.DARK -> true
        else -> followsSystemDark
    }
    LaunchedEffect(isDark) {
        onDarkModeChanged(isDark)
    }

    CompositionLocalProvider(LocalCognoDarkTheme provides isDark) {
        MaterialTheme(
            colorScheme = if (isDark) DarkColors else LightColors,
            content = content
        )
    }
}
