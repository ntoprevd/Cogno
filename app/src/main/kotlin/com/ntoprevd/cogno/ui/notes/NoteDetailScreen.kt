package com.ntoprevd.cogno.ui.notes

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ntoprevd.cogno.data.repository.NativeNoteRepository
import com.ntoprevd.cogno.data.settings.AppLanguagePreference
import com.ntoprevd.cogno.ui.common.BasicMarkdown
import com.ntoprevd.cogno.ui.theme.CognoBackground
import com.ntoprevd.cogno.ui.theme.CognoDarkBackground
import com.ntoprevd.cogno.ui.theme.CognoDarkPrimary
import com.ntoprevd.cogno.ui.theme.CognoDarkLine
import com.ntoprevd.cogno.ui.theme.CognoDarkSurface
import com.ntoprevd.cogno.ui.theme.CognoDarkText
import com.ntoprevd.cogno.ui.theme.CognoMuted
import com.ntoprevd.cogno.ui.theme.CognoLine
import com.ntoprevd.cogno.ui.theme.CognoPrimary
import com.ntoprevd.cogno.ui.theme.CognoText
import com.ntoprevd.cogno.ui.theme.isCognoDarkTheme
import kotlinx.coroutines.launch

private data class NoteDetailCopy(
    val missingNoteTitle: String,
    val missingNoteContent: String,
    val back: String,
    val view: String,
    val edit: String,
    val openChat: String,
    val share: String,
    val timePattern: String,
    val timeLocale: Locale,
    val sourceSession: (Int) -> String
)

private fun noteDetailCopy(languagePreference: String): NoteDetailCopy {
    return if (languagePreference == AppLanguagePreference.EN) {
        NoteDetailCopy(
            missingNoteTitle = "Note not found",
            missingNoteContent = "This note may have been deleted.",
            back = "Back",
            view = "View",
            edit = "Edit",
            openChat = "Open source chat",
            share = "Share",
            timePattern = "MMM d, yyyy HH:mm",
            timeLocale = Locale.US,
            sourceSession = { count -> "Source chat · $count messages" }
        )
    } else {
        NoteDetailCopy(
            missingNoteTitle = "笔记不存在",
            missingNoteContent = "这条笔记可能已被删除。",
            back = "返回",
            view = "查看",
            edit = "编辑",
            openChat = "跳转对话",
            share = "分享",
            timePattern = "yyyy年M月d日 HH:mm",
            timeLocale = Locale.CHINA,
            sourceSession = { count -> "来源会话 · $count 条消息" }
        )
    }
}

@Composable
fun NoteDetailScreen(
    noteId: String,
    languagePreference: String,
    onBack: () -> Unit,
    onOpenChat: (String?) -> Unit
) {
    val isDark = isCognoDarkTheme()
    val background = if (isDark) CognoDarkBackground else CognoBackground
    val copy = noteDetailCopy(languagePreference)
    val context = LocalContext.current
    val repository = remember(context) { NativeNoteRepository(context) }
    val scope = rememberCoroutineScope()
    val note = remember(noteId) { repository.observeNote(noteId) }
        .collectAsStateWithLifecycle(initialValue = null)
        .value
    var isEditing by remember { mutableStateOf(false) }
    var editContent by remember { mutableStateOf("") }

    LaunchedEffect(note?.id) {
        editContent = note?.content.orEmpty()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        NoteDetailTopBar(
            isDark = isDark,
            isEditing = isEditing,
            copy = copy,
            onBack = onBack,
            onToggleEdit = {
                if (isEditing) {
                    scope.launch {
                        repository.updateNoteContent(noteId, editContent)
                        isEditing = false
                    }
                } else {
                    editContent = note?.content.orEmpty()
                    isEditing = true
                }
            },
            onOpenChat = { onOpenChat(note?.sourceSessionId) }
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 18.dp)
        ) {
            Text(
                text = note?.title ?: copy.missingNoteTitle,
                color = if (isDark) CognoDarkText else CognoText,
                fontSize = 26.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = note?.let { formatNoteDetailTime(it.updatedAt, copy) } ?: "",
                color = CognoMuted,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
            if (note != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = copy.sourceSession(note.sourceMessageCount),
                    color = if (isDark) CognoDarkPrimary else CognoPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(28.dp))
            if (isEditing) {
                NoteContentEditor(
                    value = editContent,
                    isDark = isDark,
                    onValueChange = { editContent = it }
                )
            } else {
                BasicMarkdown(
                    content = note?.content ?: copy.missingNoteContent,
                    isDark = isDark,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun NoteDetailTopBar(
    isDark: Boolean,
    isEditing: Boolean,
    copy: NoteDetailCopy,
    onBack: () -> Unit,
    onToggleEdit: () -> Unit,
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
                contentDescription = copy.back,
                tint = if (isDark) CognoDarkPrimary else CognoPrimary
            )
        }
        Row {
            IconButton(onClick = onToggleEdit) {
                Icon(
                    imageVector = if (isEditing) Icons.Default.Visibility else Icons.Default.Edit,
                    contentDescription = if (isEditing) copy.view else copy.edit,
                    tint = if (isDark) CognoDarkPrimary else CognoPrimary
                )
            }
            IconButton(onClick = onOpenChat) {
                Icon(Icons.Default.Link, contentDescription = copy.openChat, tint = if (isDark) CognoDarkPrimary else CognoPrimary)
            }
            IconButton(onClick = { }) {
                Icon(Icons.Default.IosShare, contentDescription = copy.share, tint = if (isDark) CognoDarkPrimary else CognoPrimary)
            }
        }
    }
}

@Composable
private fun NoteContentEditor(
    value: String,
    isDark: Boolean,
    onValueChange: (String) -> Unit
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(
            color = if (isDark) CognoDarkText else CognoText,
            fontSize = 15.sp,
            lineHeight = 25.sp,
            fontFamily = FontFamily.Monospace
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isDark) CognoDarkSurface else Color.White)
            .border(
                width = 1.dp,
                color = if (isDark) CognoDarkLine else CognoLine,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(14.dp)
    )
}

@Composable
private fun MarkdownLikeContent(content: String, isDark: Boolean) {
    var inCodeBlock = false
    val codeBuffer = StringBuilder()

    content.lines().forEach { rawLine ->
        val line = rawLine.trimEnd()
        if (line.trim().startsWith("```")) {
            if (inCodeBlock) {
                CodeBlock(codeBuffer.toString().trimEnd(), isDark)
                codeBuffer.clear()
            }
            inCodeBlock = !inCodeBlock
            return@forEach
        }

        if (inCodeBlock) {
            codeBuffer.appendLine(rawLine)
            return@forEach
        }

        when {
            line.isBlank() -> Spacer(modifier = Modifier.height(10.dp))
            line.startsWith("#") -> SectionTitle(line.trimStart('#').trim(), isDark)
            else -> BodyText(line, isDark)
        }
    }

    if (codeBuffer.isNotBlank()) {
        CodeBlock(codeBuffer.toString().trimEnd(), isDark)
    }
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

private fun formatNoteDetailTime(timestamp: Long, copy: NoteDetailCopy): String {
    return SimpleDateFormat(copy.timePattern, copy.timeLocale).format(Date(timestamp))
}
