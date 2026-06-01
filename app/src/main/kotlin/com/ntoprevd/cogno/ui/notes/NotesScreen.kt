package com.ntoprevd.cogno.ui.notes

import android.app.Application
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ntoprevd.cogno.data.db.entity.NoteEntity
import com.ntoprevd.cogno.data.repository.NativeNoteRepository
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect

data class NotesUiState(
    val notes: List<NoteEntity> = emptyList()
)

class NotesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NativeNoteRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeNotes().collect { notes ->
                _uiState.update { it.copy(notes = notes) }
            }
        }
    }

    fun renameNote(noteId: String, title: String) {
        viewModelScope.launch {
            repository.renameNote(noteId, title)
        }
    }

    fun togglePinned(note: NoteEntity) {
        viewModelScope.launch {
            repository.setNotePinned(note.id, !note.pinned)
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            repository.deleteNote(noteId)
        }
    }
}

@Composable
fun NotesScreen(
    onBack: () -> Unit,
    onOpenNote: (String) -> Unit,
    viewModel: NotesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDark = isCognoDarkTheme()
    val background = if (isDark) CognoDarkBackground else CognoBackground
    var renameTarget by remember { mutableStateOf<NoteEntity?>(null) }
    var renameText by remember { mutableStateOf("") }
    var searchExpanded by remember { mutableStateOf(false) }
    var searchKeyword by remember { mutableStateOf("") }
    val visibleNotes = remember(uiState.notes, searchKeyword) {
        val keyword = searchKeyword.trim()
        if (keyword.isBlank()) {
            uiState.notes
        } else {
            uiState.notes.filter { note ->
                note.title.contains(keyword, ignoreCase = true) ||
                    note.preview.contains(keyword, ignoreCase = true) ||
                    note.content.contains(keyword, ignoreCase = true)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            NotesTopBar(
                isDark = isDark,
                onBack = onBack,
                onToggleSearch = {
                    searchExpanded = !searchExpanded
                    if (!searchExpanded) searchKeyword = ""
                }
            )
            NoteSearchBar(
                expanded = searchExpanded,
                value = searchKeyword,
                isDark = isDark,
                onValueChange = { searchKeyword = it }
            )
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (visibleNotes.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (searchKeyword.isBlank()) "暂无笔记，先在会话中点击顶部羽毛生成。" else "没有找到相关笔记。",
                                color = CognoMuted,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                items(visibleNotes, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        isDark = isDark,
                        onClick = { onOpenNote(note.id) },
                        onRename = {
                            renameTarget = note
                            renameText = note.title
                        },
                        onTogglePin = { viewModel.togglePinned(note) },
                        onDelete = { viewModel.deleteNote(note.id) }
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
                        viewModel.renameNote(note.id, nextTitle)
                    }
                    renameTarget = null
                }
            )
        }
    }
}

@Composable
private fun NotesTopBar(
    isDark: Boolean,
    onBack: () -> Unit,
    onToggleSearch: () -> Unit
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
            IconButton(onClick = onToggleSearch) {
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

@Composable
private fun NoteSearchBar(
    expanded: Boolean,
    value: String,
    isDark: Boolean,
    onValueChange: (String) -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(expanded) {
        if (expanded) focusRequester.requestFocus()
    }

    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(animationSpec = tween(300)),
        exit = shrinkVertically(animationSpec = tween(300))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isDark) CognoDarkSurface else Color.White)
                .border(
                    width = 1.dp,
                    color = if (isDark) CognoDarkLine else CognoLine
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) CognoDarkBackground else CognoBackground)
                    .border(
                        width = 1.dp,
                        color = if (isDark) CognoDarkLine.copy(alpha = 0.8f) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = CognoMuted,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = if (isDark) CognoDarkText else CognoText,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    decorationBox = { innerTextField ->
                        if (value.isEmpty()) {
                            Text("搜索笔记标题或内容...", color = CognoMuted, fontSize = 14.sp)
                        }
                        innerTextField()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteCard(
    note: NoteEntity,
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
                text = formatNoteTime(note.updatedAt),
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

private fun formatNoteTime(timestamp: Long): String {
    return SimpleDateFormat("M/d HH:mm", Locale.CHINA).format(Date(timestamp))
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
