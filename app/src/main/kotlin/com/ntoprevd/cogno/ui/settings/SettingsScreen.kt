package com.ntoprevd.cogno.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import com.ntoprevd.cogno.data.network.AiChatClient
import com.ntoprevd.cogno.data.settings.AiSettings
import com.ntoprevd.cogno.data.settings.AiSettingsStore
import com.ntoprevd.cogno.data.settings.AppLanguagePreference
import com.ntoprevd.cogno.data.settings.DarkModePreference
import com.ntoprevd.cogno.data.settings.ResponseStylePreference
import com.ntoprevd.cogno.ui.theme.CognoBackground
import com.ntoprevd.cogno.ui.theme.CognoDarkBackground
import com.ntoprevd.cogno.ui.theme.CognoDarkLine
import com.ntoprevd.cogno.ui.theme.CognoDarkPrimary
import com.ntoprevd.cogno.ui.theme.CognoDarkSurface
import com.ntoprevd.cogno.ui.theme.CognoDarkText
import com.ntoprevd.cogno.ui.theme.CognoLine
import com.ntoprevd.cogno.ui.theme.CognoMuted
import com.ntoprevd.cogno.ui.theme.CognoPrimary
import com.ntoprevd.cogno.ui.theme.CognoText
import com.ntoprevd.cogno.ui.theme.isCognoDarkTheme
import java.io.File
import kotlinx.coroutines.launch

private data class SettingsCopy(
    val savedLocal: String,
    val cacheStatusPrefix: String,
    val cacheStatusSuffix: String,
    val settings: String,
    val back: String,
    val aiConfig: String,
    val currentModel: String,
    val unset: String,
    val apiConfig: String,
    val testing: String,
    val connectionSuccess: String,
    val connectionFailed: String,
    val testConnection: String,
    val save: String,
    val apiUsage: String,
    val displayStorage: String,
    val privacyTerms: String,
    val modelPresetTitle: String,
    val responseStyleTitle: String,
    val appearanceTitle: String,
    val languageTitle: String,
    val cancel: String,
    val saveChangesTitle: String,
    val saveChangesMessage: String,
    val dontSave: String,
    val modelPreset: String,
    val responseStyle: String,
    val appearance: String,
    val language: String,
    val clearCache: String,
    val safeMode: String,
    val cacheSafetyNote: String,
    val systemPrompt: String
)

private fun settingsCopy(languagePreference: String): SettingsCopy {
    return if (languagePreference == AppLanguagePreference.EN) {
        SettingsCopy(
            savedLocal = "Saved locally",
            cacheStatusPrefix = "Temporary cache about ",
            cacheStatusSuffix = "; chats and notes are kept",
            settings = "Settings",
            back = "Back",
            aiConfig = "AI Settings",
            currentModel = "Current Model",
            unset = "Not set",
            apiConfig = "API Settings",
            testing = "Testing...",
            connectionSuccess = "Connection successful",
            connectionFailed = "Connection failed. Please check your configuration.",
            testConnection = "Test Connection",
            save = "Save",
            apiUsage = "API Usage (This Month)",
            displayStorage = "Display & Storage",
            privacyTerms = "Privacy Policy  |  Terms of Service",
            modelPresetTitle = "Choose Model Preset",
            responseStyleTitle = "Choose Response Style",
            appearanceTitle = "Choose Appearance",
            languageTitle = "Choose Language",
            cancel = "Cancel",
            saveChangesTitle = "Save changes?",
            saveChangesMessage = "There are unsaved changes on this settings page. Save before leaving?",
            dontSave = "Don't Save",
            modelPreset = "Model Preset",
            responseStyle = "Response Style",
            appearance = "Appearance",
            language = "Language Settings",
            clearCache = "Clear Cache",
            safeMode = "Safe Mode",
            cacheSafetyNote = "This safe entry only reports temporary cache and does not clear chats, messages, notes, or API settings.",
            systemPrompt = "System Prompt"
        )
    } else {
        SettingsCopy(
            savedLocal = "已保存到本机",
            cacheStatusPrefix = "临时缓存约 ",
            cacheStatusSuffix = "；聊天和笔记数据已保留",
            settings = "设置",
            back = "返回",
            aiConfig = "AI 配置",
            currentModel = "当前模型",
            unset = "未设置",
            apiConfig = "API 配置",
            testing = "正在测试...",
            connectionSuccess = "连接成功",
            connectionFailed = "连接失败，请检查配置",
            testConnection = "测试连接",
            save = "保存",
            apiUsage = "API 消耗统计（本月）",
            displayStorage = "显示与存储",
            privacyTerms = "隐私协议  |  服务条款",
            modelPresetTitle = "选择模型预设",
            responseStyleTitle = "选择输出风格",
            appearanceTitle = "选择外观显示",
            languageTitle = "选择语言",
            cancel = "取消",
            saveChangesTitle = "保存修改？",
            saveChangesMessage = "检测到设置页面有未保存的修改，离开前是否保存？",
            dontSave = "不保存",
            modelPreset = "模型预设",
            responseStyle = "输出风格",
            appearance = "外观显示",
            language = "语言设置",
            clearCache = "清理缓存",
            safeMode = "安全模式",
            cacheSafetyNote = "安全版本只处理临时缓存入口，不清空聊天、消息、笔记和 API 配置。",
            systemPrompt = "系统提示词"
        )
    }
}

@Composable
fun SettingsScreen(
    darkModePreference: String,
    onDarkModePreferenceChange: (String) -> Unit,
    languagePreference: String,
    onLanguagePreferenceChange: (String) -> Unit,
    onAiSettingsChanged: () -> Unit,
    onBack: () -> Unit
) {
    val isDark = isCognoDarkTheme()
    val background = if (isDark) CognoDarkBackground else CognoBackground
    val copy = settingsCopy(languagePreference)
    val context = LocalContext.current
    val settingsStore = remember(context) { AiSettingsStore(context) }
    val aiChatClient = remember { AiChatClient() }
    val scope = rememberCoroutineScope()
    val savedSettings = remember { settingsStore.load() }

    var apiBaseUrl by remember { mutableStateOf(savedSettings.apiBaseUrl) }
    var modelId by remember { mutableStateOf(savedSettings.modelId) }
    var apiKey by remember { mutableStateOf(savedSettings.apiKey) }
    var systemPrompt by remember { mutableStateOf(savedSettings.systemPrompt) }
    var responseStyle by remember { mutableStateOf(savedSettings.responseStyle) }
    var temperature by remember { mutableStateOf(savedSettings.temperature) }
    var saveStatus by remember(languagePreference) { mutableStateOf("") }
    var testStatus by remember(languagePreference) { mutableStateOf("") }
    var cacheStatus by remember(languagePreference) {
        mutableStateOf("${copy.cacheStatusPrefix}${formatBytes(cacheSizeBytes(context.cacheDir))}")
    }
    var modelDialogVisible by remember { mutableStateOf(false) }
    var responseStyleDialogVisible by remember { mutableStateOf(false) }
    var darkModeDialogVisible by remember { mutableStateOf(false) }
    var languageDialogVisible by remember { mutableStateOf(false) }
    var saveConfirmVisible by remember { mutableStateOf(false) }

    val hasUnsavedChanges =
        apiBaseUrl != savedSettings.apiBaseUrl ||
            modelId != savedSettings.modelId ||
            apiKey != savedSettings.apiKey ||
            systemPrompt != savedSettings.systemPrompt ||
            responseStyle != savedSettings.responseStyle ||
            temperature != savedSettings.temperature

    fun saveCurrentSettings() {
        settingsStore.save(
            currentAiSettings(
                apiBaseUrl = apiBaseUrl,
                modelId = modelId,
                apiKey = apiKey,
                systemPrompt = systemPrompt,
                responseStyle = responseStyle,
                temperature = temperature
            )
        )
        onAiSettingsChanged()
        saveStatus = copy.savedLocal
    }

    fun requestBack() {
        if (hasUnsavedChanges) {
            saveConfirmVisible = true
        } else {
            onBack()
        }
    }

    BackHandler(onBack = ::requestBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        SettingsTopBar(isDark = isDark, copy = copy, onBack = ::requestBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            ProfileCard(isDark = isDark)
            SettingsSection(title = copy.aiConfig, isDark = isDark) {
                SettingsRow(copy.currentModel, modelId.ifBlank { copy.unset }, isDark)
                DividerLine(isDark)
                ModelPresetSection(
                    modelId = modelId,
                    isDark = isDark,
                    copy = copy,
                    onOpenPicker = { modelDialogVisible = true },
                    onModelSelected = {
                        modelId = it
                        saveStatus = ""
                        testStatus = ""
                    }
                )
                DividerLine(isDark)
                ResponseStyleSelector(
                    value = responseStyle,
                    isDark = isDark,
                    languagePreference = languagePreference,
                    copy = copy,
                    onOpenPicker = { responseStyleDialogVisible = true },
                    onValueChange = {
                        responseStyle = it
                        temperature = ResponseStylePreference.temperatureFor(it)
                        saveStatus = ""
                        testStatus = ""
                    }
                )
                DividerLine(isDark)
                PromptBox(
                    value = systemPrompt,
                    isDark = isDark,
                    copy = copy,
                    onValueChange = {
                        systemPrompt = it
                        saveStatus = ""
                        testStatus = ""
                    }
                )
            }

            SettingsSection(
                title = copy.apiConfig,
                isDark = isDark,
                trailing = {
                    TextButton(
                        onClick = {
                            testStatus = copy.testing
                            val settings = currentAiSettings(
                                apiBaseUrl = apiBaseUrl,
                                modelId = modelId,
                                apiKey = apiKey,
                                systemPrompt = systemPrompt,
                                responseStyle = responseStyle,
                                temperature = temperature
                            )
                            scope.launch {
                                runCatching {
                                    aiChatClient.testConnection(settings)
                                }.onSuccess {
                                    settingsStore.save(settings)
                                    onAiSettingsChanged()
                                    saveStatus = copy.savedLocal
                                    testStatus = copy.connectionSuccess
                                }.onFailure { error ->
                                    testStatus = error.message ?: copy.connectionFailed
                                }
                            }
                        }
                    ) {
                        Text(copy.testConnection, color = if (isDark) CognoDarkPrimary else CognoPrimary)
                    }
                }
            ) {
                SettingsField(
                    label = "Model ID",
                    value = modelId,
                    placeholder = AiSettings.DEFAULT_MODEL_ID,
                    isDark = isDark,
                    onValueChange = {
                        modelId = it
                        saveStatus = ""
                        testStatus = ""
                    }
                )
                DividerLine(isDark)
                SettingsField(
                    label = "API Base URL",
                    value = apiBaseUrl,
                    placeholder = AiSettings.DEFAULT_API_BASE_URL,
                    isDark = isDark,
                    onValueChange = {
                        apiBaseUrl = it
                        saveStatus = ""
                        testStatus = ""
                    }
                )
                DividerLine(isDark)
                ApiKeyField(
                    value = apiKey,
                    isDark = isDark,
                    onValueChange = {
                        apiKey = it
                        saveStatus = ""
                        testStatus = ""
                    }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = saveStatus,
                        color = if (isDark) CognoDarkPrimary else CognoPrimary,
                        fontSize = 12.sp
                    )
                    TextButton(
                        onClick = {
                            saveCurrentSettings()
                        }
                    ) {
                        Text(copy.save, color = if (isDark) CognoDarkPrimary else CognoPrimary)
                    }
                }
                if (testStatus.isNotBlank()) {
                    DividerLine(isDark)
                    Text(
                        text = testStatus,
                        color = if (testStatus.startsWith(copy.connectionSuccess)) {
                            if (isDark) CognoDarkPrimary else CognoPrimary
                        } else {
                            Color(0xFFE05650)
                        },
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }

            SettingsSection(title = copy.apiUsage, isDark = isDark) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text("Estimated Cost", color = CognoMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("¥ 2.45", color = if (isDark) CognoDarkText else CognoText, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Total Tokens", color = CognoMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("842.1k", color = if (isDark) CognoDarkText else CognoText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) CognoDarkBackground else CognoBackground)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.65f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDark) CognoDarkPrimary else CognoPrimary)
                    )
                }
            }

            SettingsSection(title = copy.displayStorage, isDark = isDark) {
                DarkModeSelector(
                    value = darkModePreference,
                    isDark = isDark,
                    languagePreference = languagePreference,
                    copy = copy,
                    onOpenPicker = { darkModeDialogVisible = true },
                    onValueChange = onDarkModePreferenceChange
                )
                DividerLine(isDark)
                LanguageSelector(
                    value = languagePreference,
                    isDark = isDark,
                    copy = copy,
                    onOpenPicker = { languageDialogVisible = true },
                    onValueChange = onLanguagePreferenceChange
                )
                DividerLine(isDark)
                CacheCleanupRow(
                    status = cacheStatus,
                    isDark = isDark,
                    copy = copy,
                    onClick = {
                        cacheStatus = "${copy.cacheStatusPrefix}${formatBytes(cacheSizeBytes(context.cacheDir))}${copy.cacheStatusSuffix}"
                    }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Cogno v1.1.0-stable", color = CognoMuted, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(copy.privacyTerms, color = if (isDark) CognoDarkPrimary else CognoPrimary, fontSize = 12.sp)
            }
        }
    }

    if (modelDialogVisible) {
        SettingsOptionDialog(
            title = copy.modelPresetTitle,
            isDark = isDark,
            cancelText = copy.cancel,
            options = listOf(
                SettingsOption("deepseek-v4-flash", "V4 Flash"),
                SettingsOption("deepseek-v4-pro", "V4 Pro")
            ),
            selectedValue = modelId,
            onSelect = {
                modelId = it
                saveStatus = ""
                testStatus = ""
                modelDialogVisible = false
            },
            onDismiss = { modelDialogVisible = false }
        )
    }

    if (responseStyleDialogVisible) {
        SettingsOptionDialog(
            title = copy.responseStyleTitle,
            isDark = isDark,
            cancelText = copy.cancel,
            options = listOf(
                SettingsOption(ResponseStylePreference.BALANCED, responseStyleLabel(ResponseStylePreference.BALANCED, languagePreference)),
                SettingsOption(ResponseStylePreference.COMPREHENSIVE, responseStyleLabel(ResponseStylePreference.COMPREHENSIVE, languagePreference)),
                SettingsOption(ResponseStylePreference.CONCISE, responseStyleLabel(ResponseStylePreference.CONCISE, languagePreference)),
                SettingsOption(ResponseStylePreference.FRIENDLY, responseStyleLabel(ResponseStylePreference.FRIENDLY, languagePreference)),
                SettingsOption(ResponseStylePreference.WARM, responseStyleLabel(ResponseStylePreference.WARM, languagePreference))
            ),
            selectedValue = responseStyle,
            onSelect = {
                responseStyle = it
                temperature = ResponseStylePreference.temperatureFor(it)
                saveStatus = ""
                testStatus = ""
                responseStyleDialogVisible = false
            },
            onDismiss = { responseStyleDialogVisible = false }
        )
    }

    if (darkModeDialogVisible) {
        SettingsOptionDialog(
            title = copy.appearanceTitle,
            isDark = isDark,
            cancelText = copy.cancel,
            options = listOf(
                SettingsOption(DarkModePreference.SYSTEM, darkModeOptionLabel(DarkModePreference.SYSTEM, languagePreference)),
                SettingsOption(DarkModePreference.LIGHT, darkModeOptionLabel(DarkModePreference.LIGHT, languagePreference)),
                SettingsOption(DarkModePreference.DARK, darkModeOptionLabel(DarkModePreference.DARK, languagePreference))
            ),
            selectedValue = darkModePreference,
            onSelect = {
                onDarkModePreferenceChange(it)
                darkModeDialogVisible = false
            },
            onDismiss = { darkModeDialogVisible = false }
        )
    }

    if (languageDialogVisible) {
        SettingsOptionDialog(
            title = copy.languageTitle,
            isDark = isDark,
            cancelText = copy.cancel,
            options = listOf(
                SettingsOption(AppLanguagePreference.ZH_CN, "简体中文"),
                SettingsOption(AppLanguagePreference.EN, "English")
            ),
            selectedValue = languagePreference,
            onSelect = {
                onLanguagePreferenceChange(it)
                languageDialogVisible = false
            },
            onDismiss = { languageDialogVisible = false }
        )
    }

    if (saveConfirmVisible) {
        SaveChangesDialog(
            isDark = isDark,
            copy = copy,
            onDismiss = { saveConfirmVisible = false },
            onDiscard = {
                saveConfirmVisible = false
                onBack()
            },
            onSave = {
                saveCurrentSettings()
                saveConfirmVisible = false
                onBack()
            }
        )
    }
}

@Composable
private fun SettingsTopBar(isDark: Boolean, copy: SettingsCopy, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Default.ArrowBackIosNew,
                contentDescription = copy.back,
                tint = if (isDark) CognoDarkText else CognoText
            )
        }
        Text(
            text = copy.settings,
            color = if (isDark) CognoDarkText else CognoText,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@Composable
private fun ProfileCard(isDark: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(if (isDark) CognoDarkSurface else Color.White)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (isDark) CognoDarkPrimary else CognoPrimary),
            contentAlignment = Alignment.Center
        ) {
            Text("JD", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Jane Doe", color = if (isDark) CognoDarkText else CognoText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("jane.doe@cogno.ai", color = CognoMuted, fontSize = 12.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = CognoMuted)
    }
}

@Composable
private fun SettingsSection(
    title: String,
    isDark: Boolean,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = CognoMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            trailing?.invoke()
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(if (isDark) CognoDarkSurface else Color.White)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            content = content
        )
    }
}

@Composable
private fun SettingsRow(
    label: String,
    value: String,
    isDark: Boolean,
    danger: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (danger) Color(0xFFE05650) else if (isDark) CognoDarkText else CognoText,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = if (danger) CognoMuted else if (isDark) CognoDarkPrimary else CognoPrimary,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun SettingsPickerRow(
    label: String,
    value: String,
    isDark: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (isDark) CognoDarkText else CognoText,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = if (isDark) CognoDarkPrimary else CognoPrimary,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = CognoMuted,
            modifier = Modifier.size(14.dp)
        )
    }
}

private data class SettingsOption(
    val value: String,
    val label: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsOptionDialog(
    title: String,
    isDark: Boolean,
    cancelText: String,
    options: List<SettingsOption>,
    selectedValue: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            color = if (isDark) CognoDarkSurface else Color.White,
            shape = RoundedCornerShape(22.dp),
            shadowElevation = 18.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp)
                .border(
                    width = 1.dp,
                    color = if (isDark) CognoDarkLine else CognoLine,
                    shape = RoundedCornerShape(22.dp)
                )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = title,
                    color = if (isDark) CognoDarkText else CognoText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            width = 1.dp,
                            color = if (isDark) CognoDarkLine else CognoLine,
                            shape = RoundedCornerShape(14.dp)
                        )
                ) {
                    options.forEachIndexed { index, option ->
                        SettingsOptionRow(
                            option = option,
                            selected = option.value == selectedValue,
                            isDark = isDark,
                            isFirst = index == 0,
                            isLast = index == options.lastIndex,
                            onClick = { onSelect(option.value) }
                        )
                        if (index != options.lastIndex) DividerLine(isDark)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(cancelText, color = if (isDark) CognoDarkPrimary else CognoPrimary)
                }
            }
        }
    }
}

@Composable
private fun SettingsOptionRow(
    option: SettingsOption,
    selected: Boolean,
    isDark: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit
) {
    val rowShape = RoundedCornerShape(
        topStart = if (isFirst) 14.dp else 0.dp,
        topEnd = if (isFirst) 14.dp else 0.dp,
        bottomStart = if (isLast) 14.dp else 0.dp,
        bottomEnd = if (isLast) 14.dp else 0.dp
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(rowShape)
            .background(
                if (selected) {
                    (if (isDark) CognoDarkPrimary else CognoPrimary).copy(alpha = 0.12f)
                } else {
                    Color.Transparent
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = option.label,
            color = if (selected) {
                if (isDark) CognoDarkPrimary else CognoPrimary
            } else if (isDark) {
                CognoDarkText
            } else {
                CognoText
            },
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Text(
                text = "✓",
                color = if (isDark) CognoDarkPrimary else CognoPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SaveChangesDialog(
    isDark: Boolean,
    copy: SettingsCopy,
    onDismiss: () -> Unit,
    onDiscard: () -> Unit,
    onSave: () -> Unit
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            color = if (isDark) CognoDarkSurface else Color.White,
            shape = RoundedCornerShape(22.dp),
            shadowElevation = 18.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp)
                .border(
                    width = 1.dp,
                    color = if (isDark) CognoDarkLine else CognoLine,
                    shape = RoundedCornerShape(22.dp)
                )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = copy.saveChangesTitle,
                    color = if (isDark) CognoDarkText else CognoText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = copy.saveChangesMessage,
                    color = CognoMuted,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = copy.dontSave,
                        color = if (isDark) CognoDarkText else CognoText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = 1.dp,
                                color = if (isDark) CognoDarkLine else CognoLine,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable(onClick = onDiscard)
                            .padding(vertical = 12.dp)
                    )
                    Text(
                        text = copy.save,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) CognoDarkPrimary else CognoPrimary)
                            .clickable(onClick = onSave)
                            .padding(vertical = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelPresetSection(
    modelId: String,
    isDark: Boolean,
    copy: SettingsCopy,
    onOpenPicker: () -> Unit,
    onModelSelected: (String) -> Unit
) {
    SettingsPickerRow(
        label = copy.modelPreset,
        value = modelPresetLabel(modelId, copy),
        isDark = isDark,
        onClick = onOpenPicker
    )
}

@Composable
private fun ResponseStyleSelector(
    value: String,
    isDark: Boolean,
    languagePreference: String,
    copy: SettingsCopy,
    onOpenPicker: () -> Unit,
    onValueChange: (String) -> Unit
) {
    val normalized = value.takeIf { it in ResponseStylePreference.all }
        ?: ResponseStylePreference.BALANCED
    SettingsPickerRow(
        label = copy.responseStyle,
        value = responseStyleLabel(normalized, languagePreference),
        isDark = isDark,
        onClick = onOpenPicker
    )
}

@Composable
private fun ModelPresetChip(
    text: String,
    selected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = text,
        color = if (selected) Color.White else if (isDark) CognoDarkText else CognoText,
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (selected) {
                    if (isDark) CognoDarkPrimary else CognoPrimary
                } else {
                    if (isDark) CognoDarkBackground else CognoBackground
                }
            )
            .border(
                width = 1.dp,
                color = if (selected) Color.Transparent else if (isDark) CognoDarkLine else CognoLine,
                shape = RoundedCornerShape(999.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    )
}

@Composable
private fun ApiKeyField(
    value: String,
    isDark: Boolean,
    onValueChange: (String) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text("API Key", color = if (isDark) CognoDarkText else CognoText, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(8.dp))
        SettingsInput(
            value = value,
            placeholder = "sk-...",
            isDark = isDark,
            singleLine = true,
            visualTransformation = ApiKeyPartialVisualTransformation,
            onValueChange = onValueChange
        )
    }
}

@Composable
private fun DarkModeSelector(
    value: String,
    isDark: Boolean,
    languagePreference: String,
    copy: SettingsCopy,
    onOpenPicker: () -> Unit,
    onValueChange: (String) -> Unit
) {
    SettingsPickerRow(
        label = copy.appearance,
        value = darkModeLabel(value, isDark, languagePreference),
        isDark = isDark,
        onClick = onOpenPicker
    )
}

@Composable
private fun LanguageSelector(
    value: String,
    isDark: Boolean,
    copy: SettingsCopy,
    onOpenPicker: () -> Unit,
    onValueChange: (String) -> Unit
) {
    SettingsPickerRow(
        label = copy.language,
        value = languageLabel(value),
        isDark = isDark,
        onClick = onOpenPicker
    )
}

@Composable
private fun CacheCleanupRow(
    status: String,
    isDark: Boolean,
    copy: SettingsCopy,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = copy.clearCache,
                color = if (isDark) CognoDarkText else CognoText,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = copy.safeMode,
                color = if (isDark) CognoDarkPrimary else CognoPrimary,
                fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = status,
            color = if (isDark) CognoDarkText.copy(alpha = 0.78f) else CognoText.copy(alpha = 0.78f),
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = copy.cacheSafetyNote,
            color = CognoMuted,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun SettingsField(
    label: String,
    value: String,
    placeholder: String,
    isDark: Boolean,
    onValueChange: (String) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(label, color = if (isDark) CognoDarkText else CognoText, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(8.dp))
        SettingsInput(
            value = value,
            placeholder = placeholder,
            isDark = isDark,
            singleLine = true,
            visualTransformation = VisualTransformation.None,
            onValueChange = onValueChange
        )
    }
}

@Composable
private fun PromptBox(
    value: String,
    isDark: Boolean,
    copy: SettingsCopy,
    onValueChange: (String) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 14.dp)) {
        Text(copy.systemPrompt, color = if (isDark) CognoDarkText else CognoText, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(8.dp))
        SettingsInput(
            value = value,
            placeholder = AiSettings.DEFAULT_SYSTEM_PROMPT,
            isDark = isDark,
            singleLine = false,
            visualTransformation = VisualTransformation.None,
            onValueChange = onValueChange
        )
    }
}

@Composable
private fun SettingsInput(
    value: String,
    placeholder: String,
    isDark: Boolean,
    singleLine: Boolean,
    visualTransformation: VisualTransformation,
    onValueChange: (String) -> Unit
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        maxLines = if (singleLine) 1 else 5,
        visualTransformation = visualTransformation,
        textStyle = TextStyle(
            color = if (isDark) CognoDarkText else CognoText,
            fontSize = 13.sp,
            lineHeight = 20.sp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isDark) CognoDarkBackground else CognoBackground)
            .padding(12.dp),
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = CognoMuted,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
                innerTextField()
            }
        }
    )
}

private fun currentAiSettings(
    apiBaseUrl: String,
    modelId: String,
    apiKey: String,
    systemPrompt: String,
    responseStyle: String,
    temperature: Double
): AiSettings {
    return AiSettings(
        apiBaseUrl = apiBaseUrl,
        modelId = modelId,
        apiKey = apiKey,
        systemPrompt = systemPrompt,
        responseStyle = responseStyle,
        temperature = temperature
    )
}

private object ApiKeyPartialVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val masked = when {
            raw.isBlank() -> ""
            raw.length <= 6 -> "•".repeat(raw.length)
            raw.length <= 12 -> raw.take(3) + "•".repeat(raw.length - 5) + raw.takeLast(2)
            else -> raw.take(6) + "•".repeat(raw.length - 10) + raw.takeLast(4)
        }
        return TransformedText(AnnotatedString(masked), OffsetMapping.Identity)
    }
}

private fun responseStyleLabel(value: String, languagePreference: String): String {
    val english = languagePreference == AppLanguagePreference.EN
    return when (value) {
        ResponseStylePreference.COMPREHENSIVE -> if (english) "Comprehensive" else "全面"
        ResponseStylePreference.FRIENDLY -> if (english) "Friendly" else "友好"
        ResponseStylePreference.WARM -> if (english) "Warm" else "热情"
        ResponseStylePreference.CONCISE -> if (english) "Concise" else "简短"
        else -> if (english) "Balanced" else "理智"
    }
}

private fun modelPresetLabel(value: String, copy: SettingsCopy): String {
    return when (value) {
        "deepseek-v4-flash" -> "V4 Flash"
        "deepseek-v4-pro" -> "V4 Pro"
        else -> value.ifBlank { copy.unset }
    }
}

private fun languageLabel(value: String): String {
    return when (value) {
        AppLanguagePreference.EN -> "English"
        else -> "简体中文"
    }
}

private fun darkModeOptionLabel(value: String, languagePreference: String): String {
    val english = languagePreference == AppLanguagePreference.EN
    return when (value) {
        DarkModePreference.LIGHT -> if (english) "Light" else "浅色"
        DarkModePreference.DARK -> if (english) "Dark" else "深色"
        else -> if (english) "Follow System" else "跟随系统"
    }
}

private fun darkModeLabel(value: String, isDark: Boolean, languagePreference: String): String {
    val english = languagePreference == AppLanguagePreference.EN
    return when (value) {
        DarkModePreference.LIGHT -> if (english) "Light" else "浅色"
        DarkModePreference.DARK -> if (english) "Dark" else "深色"
        else -> if (english) {
            if (isDark) "Follow System: Dark" else "Follow System: Light"
        } else {
            if (isDark) "跟随系统：深色" else "跟随系统：浅色"
        }
    }
}

private fun cacheSizeBytes(file: File?): Long {
    if (file == null || !file.exists()) return 0L
    if (file.isFile) return file.length()
    return file.listFiles()
        ?.sumOf { child -> cacheSizeBytes(child) }
        ?: 0L
}

private fun formatBytes(bytes: Long): String {
    val kb = 1024.0
    val mb = kb * 1024.0
    return when {
        bytes >= mb -> String.format("%.1f MB", bytes / mb)
        bytes >= kb -> String.format("%.1f KB", bytes / kb)
        else -> "$bytes B"
    }
}

@Composable
private fun DividerLine(isDark: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(if (isDark) CognoDarkLine else CognoLine)
    )
}
