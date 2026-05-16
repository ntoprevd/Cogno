package com.ntoprevd.cogno.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val background = if (isDark) CognoDarkBackground else CognoBackground

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
                SettingsRow("当前模型", "DeepSeek-V3", isDark)
                DividerLine(isDark)
                PromptBox(isDark)
            }
            SettingsSection(title = "API 配置", isDark = isDark) {
                SettingsRow("Model ID", "deepseek-chat", isDark)
                DividerLine(isDark)
                SettingsRow("API Base URL", "https://api.deepseek.com/v1", isDark)
                DividerLine(isDark)
                SettingsRow("API Key", "sk-xxxxxxxxxxxxxxxx", isDark)
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
                SettingsRow("深色模式", if (isDark) "跟随系统：深色" else "跟随系统：浅色", isDark, trailingToggle = true)
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
private fun PromptBox(isDark: Boolean) {
    Column(modifier = Modifier.padding(vertical = 14.dp)) {
        Text("系统提示词", color = if (isDark) CognoDarkText else CognoText, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "定义 AI 的角色，例如：你是一个极简主义助手...",
            color = CognoMuted,
            fontSize = 13.sp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (isDark) CognoDarkBackground else CognoBackground)
                .padding(12.dp)
        )
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
