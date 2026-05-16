package com.ntoprevd.cogno

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.ntoprevd.cogno.ui.CognoApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureEdgeToEdgeWindow(window)

        setContent {
            CognoApp(
                onDarkModeChanged = { isDark ->
                    applyThemeToSystemBars(isDark)
                }
            )
        }
    }

    private fun configureEdgeToEdgeWindow(window: Window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.TRANSPARENT
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    /**
     * Compose owns the screen background; system bars only mirror light/dark contrast.
     */
    private fun applyThemeToSystemBars(isDark: Boolean) {
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = !isDark
        controller.isAppearanceLightNavigationBars = !isDark

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val decorView = window.decorView
            var flags = decorView.systemUiVisibility
            flags = if (isDark) {
                flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            } else {
                flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                flags = if (isDark) {
                    flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
                } else {
                    flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                }
            }
            decorView.systemUiVisibility = flags
        }
    }
}
