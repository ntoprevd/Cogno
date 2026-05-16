package com.ntoprevd.cogno.ui.notes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
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

private data class NotePreview(
    val id: String,
    val title: String,
    val preview: String,
    val time: String,
    val pinned: Boolean
)

private val sampleNotes = listOf(
    NotePreview(
        id = "java-concurrency",
        title = "Java 并发编程与单例模式深度解析",
        preview = "Synchronized 关键字用于解决资源竞争，底层与 Monitor 机制相关...",
        time = "4/28 14:22",
        pinned = true
    ),
    NotePreview(
        id = "web-security",
        title = "Web 安全基础：从 SQL 注入到 XSS",
        preview = "汇总安全审计与日常学习对话中的原子知识点...",
        time = "昨天 18:30",
        pinned = false
    ),
    NotePreview(
        id = "prompt-guide",
        title = "DeepSeek-V3 提示词工程优化指南",
        preview = "通过系统提示词提升模型在复杂逻辑推理中的表现...",
        time = "前天 09:15",
        pinned = false
    )
)

@Composable
fun NotesScreen(
    onBack: () -> Unit,
    onOpenNote: (String) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val background = if (isDark) CognoDarkBackground else CognoBackground
    var notes by remember { mutableStateOf(sampleNotes) }
    var renameTarget by remember { mutableStateOf<NotePreview?>(null) }
    var renameText by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            NotesTopBar(isDark = isDark, onBack = onBack)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        isDark = isDark,
                        onClick = { onOpenNote(note.id) },
                        onRename = {
                            renameTarget = note
                            renameText = note.title
                        },
                        onTogglePin = {
                            notes = notes.map {
                                if (it.id == note.id) it.copy(pinned = !it.pinned) else it
                            }.sortedByDescending { it.pinned }
                        },
                        onDelete = {
                            notes = notes.filterNot { it.id == note.id }
                        }
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 34.dp, bottom = 62.dp)
                .size(56.dp)
                .clip(CircleShape)
                .clickable(onClick = { })
                .background(if (isDark) CognoDarkPrimary else CognoPrimary),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = "新增笔记", tint = Color.White)
        }
        renameTarget?.let { note ->
            RenameDialog(
                title = "重命名此笔记",
                value = renameText,
                isDark = isDark,
                onValueChange = { renameText = it },
                onDismiss = { renameTarget = null },
                onConfirm = {
                    val nextTitle = renameText.trim()
                    if (nextTitle.isNotEmpty()) {
                        notes = notes.map {
                            if (it.id == note.id) it.copy(title = nextTitle) else it
                        }
                    }
                    renameTarget = null
                }
            )
        }
    }
}

@Composable
private fun NotesTopBar(isDark: Boolean, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = "返回",
                    tint = if (isDark) CognoDarkPrimary else CognoPrimary
                )
            }
            Text(
                text = "NoteLibrary",
                color = if (isDark) CognoDarkText else CognoText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Row {
            IconButton(onClick = { }) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "搜索笔记",
                    tint = if (isDark) CognoDarkPrimary else CognoPrimary
                )
            }
            IconButton(onClick = { }) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "切换分类模式",
                    tint = if (isDark) CognoDarkPrimary else CognoPrimary
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteCard(
    note: NotePreview,
    isDark: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(if (isDark) CognoDarkSurface else Color.White)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { menuExpanded = true }
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = note.title,
                    color = if (isDark) CognoDarkText else CognoText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (note.pinned) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "置顶",
                        tint = if (isDark) CognoDarkPrimary else CognoPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = note.preview,
                color = CognoMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = note.time,
                color = CognoMuted,
                fontSize = 9.sp,
                modifier = Modifier.align(Alignment.End)
            )
        }

        if (menuExpanded) {
            WebStyleContextMenu(
                isDark = isDark,
                pinText = if (note.pinned) "取消置顶" else "置顶",
                onRename = {
                    menuExpanded = false
                    onRename()
                },
                onTogglePin = {
                    menuExpanded = false
                    onTogglePin()
                },
                onDelete = {
                    menuExpanded = false
                    onDelete()
                },
                onDismiss = { menuExpanded = false },
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}

@Composable
private fun WebStyleContextMenu(
    isDark: Boolean,
    pinText: String,
    onRename: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Popup(
        alignment = Alignment.TopEnd,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Surface(
            color = if (isDark) CognoDarkSurface else Color.White,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 16.dp,
            modifier = modifier
                .padding(top = 8.dp, end = 20.dp)
                .fillMaxWidth(0.72f)
                .border(
                    width = 1.dp,
                    color = if (isDark) CognoDarkLine else CognoLine,
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                ContextMenuAction("重命名", isDark, onRename)
                ContextMenuAction(pinText, isDark, onTogglePin)
                ContextMenuAction("删除", isDark, onDelete, destructive = true)
            }
        }
    }
}

@Composable
private fun ContextMenuAction(
    text: String,
    isDark: Boolean,
    onClick: () -> Unit,
    destructive: Boolean = false
) {
    Text(
        text = text,
        color = if (destructive) Color(0xFFE24A4A) else if (isDark) CognoDarkText else CognoText,
        fontSize = 15.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RenameDialog(
    title: String,
    value: String,
    isDark: Boolean,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            color = if (isDark) CognoDarkSurface else Color.White,
            shape = RoundedCornerShape(22.dp),
            shadowElevation = 18.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .border(
                    width = 1.dp,
                    color = if (isDark) CognoDarkLine else CognoLine,
                    shape = RoundedCornerShape(22.dp)
                )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = title,
                    color = if (isDark) CognoDarkText else CognoText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = if (isDark) CognoDarkText else CognoText,
                        fontSize = 15.sp
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(
                        if (isDark) CognoDarkPrimary else CognoPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) CognoDarkBackground else CognoBackground)
                        .border(
                            width = 1.dp,
                            color = if (isDark) CognoDarkLine else CognoLine,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "取消",
                        color = if (isDark) CognoDarkText else CognoText,
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = 1.dp,
                                color = if (isDark) CognoDarkLine else CognoLine,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable(onClick = onDismiss)
                            .padding(vertical = 12.dp)
                    )
                    Text(
                        text = "确定",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) CognoDarkPrimary else CognoPrimary)
                            .clickable(onClick = onConfirm)
                            .padding(vertical = 12.dp)
                    )
                }
            }
        }
    }
}
