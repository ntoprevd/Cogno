package com.ntoprevd.cogno.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ntoprevd.cogno.data.settings.AppSettingsStore
import com.ntoprevd.cogno.data.settings.AiSettingsStore
import com.ntoprevd.cogno.ui.chat.ChatScreen
import com.ntoprevd.cogno.ui.notes.NoteDetailScreen
import com.ntoprevd.cogno.ui.notes.NotesScreen
import com.ntoprevd.cogno.ui.settings.SettingsScreen
import com.ntoprevd.cogno.ui.theme.CognoTheme

object CognoRoutes {
    const val CHAT = "chat"
    const val NOTES = "notes"
    const val SETTINGS = "settings"
    const val NOTE_DETAIL = "noteDetail/{noteId}"

    fun noteDetail(noteId: String): String = "noteDetail/$noteId"
}

@Composable
fun CognoApp(onDarkModeChanged: (Boolean) -> Unit) {
    val context = LocalContext.current
    val appSettingsStore = remember(context) { AppSettingsStore(context) }
    val aiSettingsStore = remember(context) { AiSettingsStore(context) }
    var darkModePreference by remember { mutableStateOf(appSettingsStore.loadDarkModePreference()) }
    var languagePreference by remember { mutableStateOf(appSettingsStore.loadLanguagePreference()) }
    var currentModelId by remember { mutableStateOf(aiSettingsStore.load().modelId) }
    var pendingChatSessionId by remember { mutableStateOf<String?>(null) }

    CognoTheme(
        darkModePreference = darkModePreference,
        onDarkModeChanged = onDarkModeChanged
    ) {
        val navController = rememberNavController()
        val enter = fadeIn(animationSpec = tween(180))
        val exit = fadeOut(animationSpec = tween(140))

        NavHost(
            navController = navController,
            startDestination = CognoRoutes.CHAT,
            enterTransition = { enter },
            exitTransition = { exit },
            popEnterTransition = { enter },
            popExitTransition = { ExitTransition.None }
        ) {
            composable(route = CognoRoutes.CHAT) {
                ChatScreen(
                    currentModelId = currentModelId,
                    onOpenNotes = { navController.navigate(CognoRoutes.NOTES) },
                    onOpenSettings = { navController.navigate(CognoRoutes.SETTINGS) },
                    initialSessionId = pendingChatSessionId,
                    onInitialSessionConsumed = { pendingChatSessionId = null }
                )
            }
            composable(route = CognoRoutes.NOTES) {
                NotesScreen(
                    onBack = { navController.popBackStack() },
                    onOpenNote = { noteId -> navController.navigate(CognoRoutes.noteDetail(noteId)) }
                )
            }
            composable(
                route = CognoRoutes.NOTE_DETAIL,
                arguments = listOf(navArgument("noteId") { type = NavType.StringType })
            ) { entry ->
                NoteDetailScreen(
                    noteId = entry.arguments?.getString("noteId").orEmpty(),
                    onBack = { navController.popBackStack() },
                    onOpenChat = { sessionId ->
                        pendingChatSessionId = sessionId
                        navController.navigate(CognoRoutes.CHAT) {
                            popUpTo(CognoRoutes.CHAT) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(route = CognoRoutes.SETTINGS) {
                SettingsScreen(
                    darkModePreference = darkModePreference,
                    onDarkModePreferenceChange = { value ->
                        appSettingsStore.saveDarkModePreference(value)
                        darkModePreference = value
                    },
                    languagePreference = languagePreference,
                    onLanguagePreferenceChange = { value ->
                        appSettingsStore.saveLanguagePreference(value)
                        languagePreference = value
                    },
                    onAiSettingsChanged = { currentModelId = aiSettingsStore.load().modelId },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
