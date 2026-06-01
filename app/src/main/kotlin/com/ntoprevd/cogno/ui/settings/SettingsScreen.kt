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
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ntoprevd.cogno.data.network.AiChatClient
import com.ntoprevd.cogno.data.settings.AiSettings
import com.ntoprevd.cogno.data.settings.AiSettingsStore
import com.ntoprevd.cogno.data.settings.DarkModePreference
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
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    darkModePreference: String,
    onDarkModePreferenceChange: (String) -> Unit,
    onBack: () -> Unit
) {
    val isDark = isCognoDarkTheme()
    val background = if (isDark) CognoDarkBackground else CognoBackground
    val context = LocalContext.current
    val settingsStore = remember(context) { AiSettingsStore(context) }
    val aiChatClient = remember { AiChatClient() }
    val scope = rememberCoroutineScope()
    val savedSettings = remember { settingsStore.load() }
    var apiBaseUrl by remember { mutableStateOf(savedSettings.apiBaseUrl) }
    var modelId by remember { mutableStateOf(savedSettings.modelId) }
    var apiKey by remember { mutableStateOf(savedSettings.apiKey) }
    var apiKeyVisible by remember { mutableStateOf(false) }
    var systemPrompt by remember { mutableStateOf(savedSettings.systemPrompt) }
    var saveStatus by remember { mutableStateOf("") }
    var testStatus by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        SettingsTopBar(isDark = isDark, onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            ProfileCard(isDark = isDark)
            SettingsSection(title = "AI 配置", isDark = isDark) {
                SettingsRow("当前模型", modelId.ifBlank { "未设置" }, isDark)
                DividerLine(isDark)
                ModelPresetSection(
                    modelId = modelId,
                    isDark = isDark,
                    onModelSelected = {
                        modelId = it
                        saveStatus = ""
                        testStatus = ""
                    }
                )
                DividerLine(isDark)
                PromptBox(
                    value = systemPrompt,
                    isDark = isDark,
                    onValueChange = {
                        systemPrompt = it
                        saveStatus = ""
                        testStatus = ""
                    }
                )
            }
            SettingsSection(title = "API 配置", isDark = isDark) {
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
                    visible = apiKeyVisible,
                    isDark = isDark,
                    onValueChange = {
                        apiKey = it
                        saveStatus = ""
                        testStatus = ""
                    },
                    onToggleVisible = { apiKeyVisible = !apiKeyVisible },
                    onClear = {
                        apiKey = ""
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
                            settingsStore.save(currentAiSettings(apiBaseUrl, modelId, apiKey, systemPrompt))
                            saveStatus = "已保存到本机"
                        }
                    ) {
                        Text("保存", color = if (isDark) CognoDarkPrimary else CognoPrimary)
                    }
                }
                DividerLine(isDark)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = testStatus,
                        color = if (testStatus.startsWith("连接成功")) {
                            if (isDark) CognoDarkPrimary else CognoPrimary
                        } else {
                            Color(0xFFE05650)
                        },
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            testStatus = "正在测试..."
                            val settings = currentAiSettings(apiBaseUrl, modelId, apiKey, systemPrompt)
                            scope.launch {
                                runCatching {
                                    aiChatClient.testConnection(settings)
                                }.onSuccess {
                                    settingsStore.save(settings)
                                    saveStatus = "已保存到本机"
                                    testStatus = "连接成功"
                                }.onFailure { error ->
                                    testStatus = error.message ?: "连接失败，请检查配置"
                                }
                            }
                        }
                    ) {
                        Text("测试连接", color = if (isDark) CognoDarkPrimary else CognoPrimary)
                    }
                }
            }
            SettingsSection(title = "API 消耗统计 (本月)", isDark = isDark) {
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
            SettingsSection(title = "显示与存储", isDark = isDark) {
                DarkModeSelector(
                    value = darkModePreference,
                    isDark = isDark,
                    onValueChange = onDarkModePreferenceChange
                )
                DividerLine(isDark)
                SettingsRow("语言", "简体中文", isDark)
                DividerLine(isDark)
                SettingsRow("清理本地缓存", "24.5 MB", isDark, danger = true)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Cogno v1.1.0-stable", color = CognoMuted, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("隐私协议  |  服务条款", color = if (isDark) CognoDarkPrimary else CognoPrimary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SettingsTopBar(isDark: Boolean, onBack: () -> Unit) {
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
                contentDescription = "返回",
                tint = if (isDark) CognoDarkText else CognoText
            )
        }
        Text(
            text = "设置",
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
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            color = CognoMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
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
    trailingToggle: Boolean = false,
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
        if (trailingToggle) {
            Icon(Icons.Default.ToggleOff, contentDescription = null, tint = if (isDark) CognoDarkPrimary else CognoMuted)
        } else {
            Text(value, color = if (danger) CognoMuted else if (isDark) CognoDarkPrimary else CognoPrimary, fontSize = 14.sp)
        }
    }
}

@Composable
private fun ModelPresetSection(
    modelId: String,
    isDark: Boolean,
    onModelSelected: (String) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text("模型预设", color = if (isDark) CognoDarkText else CognoText, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModelPresetChip(
                text = "V4 Flash",
                selected = modelId == "deepseek-v4-flash",
                isDark = isDark,
                onClick = { onModelSelected("deepseek-v4-flash") }
            )
            ModelPresetChip(
                text = "V4 Pro",
                selected = modelId == "deepseek-v4-pro",
                isDark = isDark,
                onClick = { onModelSelected("deepseek-v4-pro") }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "可在 API 配置中继续手动输入兼容服务的 Model ID。",
            color = CognoMuted,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
    }
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
    visible: Boolean,
    isDark: Boolean,
    onValueChange: (String) -> Unit,
    onToggleVisible: () -> Unit,
    onClear: () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text("API Key", color = if (isDark) CognoDarkText else CognoText, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(8.dp))
        SettingsInput(
            value = value,
            placeholder = "sk-...",
            isDark = isDark,
            singleLine = true,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            onValueChange = onValueChange
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onToggleVisible) {
                Text(if (visible) "隐藏" else "显示", color = if (isDark) CognoDarkPrimary else CognoPrimary)
            }
            TextButton(onClick = onClear, enabled = value.isNotBlank()) {
                Text("清除", color = CognoMuted)
            }
        }
    }
}

@Composable
private fun DarkModeSelector(
    value: String,
    isDark: Boolean,
    onValueChange: (String) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        SettingsRow("深色模式", darkModeLabel(value, isDark), isDark)
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModelPresetChip(
                text = "跟随系统",
                selected = value == DarkModePreference.SYSTEM,
                isDark = isDark,
                onClick = { onValueChange(DarkModePreference.SYSTEM) }
            )
            ModelPresetChip(
                text = "浅色",
                selected = value == DarkModePreference.LIGHT,
                isDark = isDark,
                onClick = { onValueChange(DarkModePreference.LIGHT) }
            )
            ModelPresetChip(
                text = "深色",
                selected = value == DarkModePreference.DARK,
                isDark = isDark,
                onClick = { onValueChange(DarkModePreference.DARK) }
            )
        }
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
    onValueChange: (String) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 14.dp)) {
        Text("系统提示词", color = if (isDark) CognoDarkText else CognoText, fontSize = 15.sp)
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
    systemPrompt: String
): AiSettings {
    return AiSettings(
        apiBaseUrl = apiBaseUrl,
        modelId = modelId,
        apiKey = apiKey,
        systemPrompt = systemPrompt
    )
}

private fun darkModeLabel(value: String, isDark: Boolean): String {
    return when (value) {
        DarkModePreference.LIGHT -> "浅色"
        DarkModePreference.DARK -> "深色"
        else -> if (isDark) "跟随系统：深色" else "跟随系统：浅色"
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
