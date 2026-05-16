package com.ntoprevd.cogno.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ntoprevd.cogno.ui.theme.CognoBackground
import com.ntoprevd.cogno.ui.theme.CognoDarkBackground
import com.ntoprevd.cogno.ui.theme.CognoDarkPrimary
import com.ntoprevd.cogno.ui.theme.CognoDarkSurface
import com.ntoprevd.cogno.ui.theme.CognoDarkText
import com.ntoprevd.cogno.ui.theme.CognoMuted
import com.ntoprevd.cogno.ui.theme.CognoPrimary
import com.ntoprevd.cogno.ui.theme.CognoText

@Composable
fun NoteDetailScreen(
    noteId: String,
    onBack: () -> Unit,
    onOpenChat: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val background = if (isDark) CognoDarkBackground else CognoBackground

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        NoteDetailTopBar(isDark = isDark, onBack = onBack, onOpenChat = onOpenChat)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 18.dp)
        ) {
            Text(
                text = titleFor(noteId),
                color = if (isDark) CognoDarkText else CognoText,
                fontSize = 26.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "2024年4月28日 14:22",
                color = CognoMuted,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(28.dp))
            MarkdownLikeContent(isDark = isDark)
        }
    }
}

@Composable
private fun NoteDetailTopBar(
    isDark: Boolean,
    onBack: () -> Unit,
    onOpenChat: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Default.ArrowBackIosNew,
                contentDescription = "返回",
                tint = if (isDark) CognoDarkPrimary else CognoPrimary
            )
        }
        Row {
            IconButton(onClick = { }) {
                Icon(Icons.Default.Edit, contentDescription = "编辑", tint = if (isDark) CognoDarkPrimary else CognoPrimary)
            }
            IconButton(onClick = onOpenChat) {
                Icon(Icons.Default.Link, contentDescription = "跳转对话", tint = if (isDark) CognoDarkPrimary else CognoPrimary)
            }
            IconButton(onClick = { }) {
                Icon(Icons.Default.IosShare, contentDescription = "分享", tint = if (isDark) CognoDarkPrimary else CognoPrimary)
            }
        }
    }
}

@Composable
private fun MarkdownLikeContent(isDark: Boolean) {
    BodyText(
        text = "在多线程环境中，确保线程安全是 Java 开发的核心。synchronized 关键字通过 JVM 内部的 Monitor 锁机制实现原子性。",
        isDark = isDark
    )
    SectionTitle("1. Synchronized 的作用", isDark)
    BodyText(
        text = "它主要用于解决多个线程访问共享资源时产生的竞争问题，确保同一时刻只有一个线程执行特定代码块。",
        isDark = isDark
    )
    CodeBlock("public synchronized void syncMethod() {\n    // 临界区代码\n}", isDark)
    SectionTitle("2. 经典单例模式 (DCL)", isDark)
    BodyText(
        text = "双重检查锁定通过 volatile 关键字禁止指令重排，是并发场景中常见的延迟初始化写法。",
        isDark = isDark
    )
    CodeBlock(
        text = "class Singleton {\n    private static volatile Singleton instance;\n\n    static Singleton getInstance() {\n        if (instance == null) {\n            synchronized (Singleton.class) {\n                if (instance == null) instance = new Singleton();\n            }\n        }\n        return instance;\n    }\n}",
        isDark = isDark
    )
}

@Composable
private fun SectionTitle(text: String, isDark: Boolean) {
    Text(
        text = text,
        color = if (isDark) CognoDarkText else CognoText,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 22.dp, bottom = 8.dp)
    )
}

@Composable
private fun BodyText(text: String, isDark: Boolean) {
    Text(
        text = text,
        color = if (isDark) CognoDarkText.copy(alpha = 0.86f) else CognoText,
        fontSize = 15.sp,
        lineHeight = 25.sp
    )
}

@Composable
private fun CodeBlock(text: String, isDark: Boolean) {
    Text(
        text = text,
        color = if (isDark) CognoDarkText else CognoText,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .background(if (isDark) CognoDarkSurface else Color.White)
            .padding(14.dp)
    )
}

private fun titleFor(noteId: String): String {
    return when (noteId) {
        "web-security" -> "Web 安全基础：从 SQL 注入到 XSS"
        "prompt-guide" -> "DeepSeek-V3 提示词工程优化指南"
        else -> "Java 并发编程与单例模式深度解析"
    }
}
