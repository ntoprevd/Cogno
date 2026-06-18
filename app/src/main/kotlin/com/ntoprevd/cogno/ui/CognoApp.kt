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
import com.ntoprevd.cogno.data.settings.AiSettings
import com.ntoprevd.cogno.data.settings.AiSettingsStore
import com.ntoprevd.cogno.data.settings.AiSourceMode
import com.ntoprevd.cogno.data.settings.CustomAiProvider
import com.ntoprevd.cogno.ui.chat.ChatScreen
import com.ntoprevd.cogno.ui.notes.NoteDetailScreen
import com.ntoprevd.cogno.ui.notes.NotesScreen
import com.ntoprevd.cogno.ui.settings.ProfileSettingsScreen
import com.ntoprevd.cogno.ui.settings.LegalDocumentScreen
import com.ntoprevd.cogno.ui.settings.LegalDocumentType
import com.ntoprevd.cogno.ui.settings.SettingsScreen
import com.ntoprevd.cogno.ui.settings.TopicSettingsScreen
import com.ntoprevd.cogno.ui.settings.VersionRoadmapScreen
import com.ntoprevd.cogno.ui.theme.CognoTheme

object CognoRoutes {
    const val CHAT = "chat"
    const val NOTES = "notes"
    const val SETTINGS = "settings"
    const val PROFILE_SETTINGS = "profileSettings"
    const val TOPIC_SETTINGS = "topicSettings"
    const val PRIVACY_POLICY = "privacyPolicy"
    const val TERMS_OF_SERVICE = "termsOfService"
    const val VERSION_ROADMAP = "versionRoadmap"
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
    var currentAiSettings by remember { mutableStateOf(aiSettingsStore.load()) }
    var userName by remember { mutableStateOf(appSettingsStore.loadUserName()) }
    var avatarUri by remember { mutableStateOf(appSettingsStore.loadAvatarUri()) }
    var pendingChatSessionId by remember { mutableStateOf<String?>(null) }
    var showFirstRunLegalNotice by remember {
        mutableStateOf(appSettingsStore.shouldShowFirstRunLegalNotice())
    }

    fun dismissFirstRunLegalNotice() {
        appSettingsStore.markFirstRunLegalNoticeShown()
        showFirstRunLegalNotice = false
    }

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
                    currentModelId = currentAiSettings.modelId,
                    modelOptions = quickModelOptions(currentAiSettings),
                    onModelSelected = { modelId ->
                        val nextSettings = currentAiSettings.copy(modelId = modelId)
                        aiSettingsStore.save(nextSettings)
                        currentAiSettings = nextSettings
                    },
                    languagePreference = languagePreference,
                    userName = userName,
                    avatarUri = avatarUri,
                    onOpenNotes = { navController.navigate(CognoRoutes.NOTES) },
                    onOpenSettings = { navController.navigate(CognoRoutes.SETTINGS) },
                    showFirstRunLegalNotice = showFirstRunLegalNotice,
                    onDismissFirstRunLegalNotice = ::dismissFirstRunLegalNotice,
                    onOpenPrivacyPolicy = {
                        dismissFirstRunLegalNotice()
                        navController.navigate(CognoRoutes.PRIVACY_POLICY)
                    },
                    onOpenTermsOfService = {
                        dismissFirstRunLegalNotice()
                        navController.navigate(CognoRoutes.TERMS_OF_SERVICE)
                    },
                    initialSessionId = pendingChatSessionId,
                    onInitialSessionConsumed = { pendingChatSessionId = null }
                )
            }
            composable(route = CognoRoutes.NOTES) {
                NotesScreen(
                    languagePreference = languagePreference,
                    onBack = { navController.popBackStack() },
                    onOpenNote = { noteId -> navController.navigate(CognoRoutes.noteDetail(noteId)) },
                    onOpenTopicSettings = { navController.navigate(CognoRoutes.TOPIC_SETTINGS) }
                )
            }
            composable(
                route = CognoRoutes.NOTE_DETAIL,
                arguments = listOf(navArgument("noteId") { type = NavType.StringType })
            ) { entry ->
                NoteDetailScreen(
                    noteId = entry.arguments?.getString("noteId").orEmpty(),
                    languagePreference = languagePreference,
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
                    userName = userName,
                    avatarUri = avatarUri,
                    onOpenProfile = { navController.navigate(CognoRoutes.PROFILE_SETTINGS) },
                    onOpenTopics = { navController.navigate(CognoRoutes.TOPIC_SETTINGS) },
                    onOpenPrivacyPolicy = { navController.navigate(CognoRoutes.PRIVACY_POLICY) },
                    onOpenTermsOfService = { navController.navigate(CognoRoutes.TERMS_OF_SERVICE) },
                    onOpenVersionRoadmap = { navController.navigate(CognoRoutes.VERSION_ROADMAP) },
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
                    onAiSettingsChanged = { currentAiSettings = aiSettingsStore.load() },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(route = CognoRoutes.PROFILE_SETTINGS) {
                ProfileSettingsScreen(
                    languagePreference = languagePreference,
                    initialUserName = userName,
                    initialAvatarUri = avatarUri,
                    onSaved = { nextName, nextAvatarUri ->
                        userName = nextName
                        avatarUri = nextAvatarUri
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(route = CognoRoutes.TOPIC_SETTINGS) {
                TopicSettingsScreen(
                    languagePreference = languagePreference,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(route = CognoRoutes.PRIVACY_POLICY) {
                LegalDocumentScreen(
                    type = LegalDocumentType.PRIVACY_POLICY,
                    languagePreference = languagePreference,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(route = CognoRoutes.TERMS_OF_SERVICE) {
                LegalDocumentScreen(
                    type = LegalDocumentType.TERMS_OF_SERVICE,
                    languagePreference = languagePreference,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(route = CognoRoutes.VERSION_ROADMAP) {
                VersionRoadmapScreen(
                    languagePreference = languagePreference,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

/** 顶栏只展示当前模型平台内的快捷选项，完整配置仍留在设置页。 */
private fun quickModelOptions(settings: AiSettings): List<String> {
    val provider = if (settings.sourceMode == AiSourceMode.EXPERIENCE) {
        CustomAiProvider.GLM
    } else {
        settings.customProvider
    }
    return (CustomAiProvider.modelPresets(provider) + settings.modelId)
        .filter { it.isNotBlank() }
        .distinct()
}
