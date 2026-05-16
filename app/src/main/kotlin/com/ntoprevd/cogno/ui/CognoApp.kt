package com.ntoprevd.cogno.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
    CognoTheme(onDarkModeChanged = onDarkModeChanged) {
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
                    onOpenNotes = { navController.navigate(CognoRoutes.NOTES) },
                    onOpenSettings = { navController.navigate(CognoRoutes.SETTINGS) }
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
                    onOpenChat = {
                        navController.navigate(CognoRoutes.CHAT) {
                            popUpTo(CognoRoutes.CHAT) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(route = CognoRoutes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
