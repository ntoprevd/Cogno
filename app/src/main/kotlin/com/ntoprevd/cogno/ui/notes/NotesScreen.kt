package com.ntoprevd.cogno.ui.notes

import android.app.Application
import android.content.Intent
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
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
import com.ntoprevd.cogno.data.db.entity.NoteTopicSegmentEntity
import com.ntoprevd.cogno.data.db.entity.TopicEntity
import com.ntoprevd.cogno.data.repository.NativeNoteRepository
import com.ntoprevd.cogno.data.repository.TopicRepository
import com.ntoprevd.cogno.data.settings.AppLanguagePreference
import com.ntoprevd.cogno.ui.common.BasicMarkdown
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
    val notes: List<NoteEntity> = emptyList(),
    val topics: List<TopicEntity> = emptyList(),
    val topicSegments: List<NoteTopicSegmentEntity> = emptyList()
)

private data class TopicSegment(
    val topic: String,
    val text: String,
    val sourceNoteId: String,
    val sourceTitle: String,
    val updatedAt: Long
)

private data class TopicGroup(
    val topic: String,
    val segments: List<TopicSegment>,
    val updatedAt: Long,
    val key: String = topic
) {
    val sourceCount: Int = segments.map { it.sourceNoteId }.distinct().size
}

class NotesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NativeNoteRepository(application.applicationContext)
    private val topicRepository = TopicRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            topicRepository.ensureDefaultTopics()
            repository.observeNotes().collect { notes ->
                _uiState.update { it.copy(notes = notes) }
                // 老版本笔记首次进入主题页时只做本地拆段，不调用 AI，也不改写原文。
                notes.forEach { note ->
                    topicRepository.syncSegments(note, emptyList(), note.sourceMessageCount)
                }
            }
        }
        viewModelScope.launch {
            topicRepository.observeTopics().collect { topics ->
                _uiState.update { it.copy(topics = topics) }
            }
        }
        viewModelScope.launch {
            topicRepository.observeSegments().collect { segments ->
                _uiState.update { it.copy(topicSegments = segments) }
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

    fun createNote(title: String, content: String, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val note = repository.createNote(title, content)
            onCreated(note.id)
        }
    }

    fun renameTopic(topicName: String, name: String) {
        val topic = _uiState.value.topics.firstOrNull { it.name == topicName } ?: return
        viewModelScope.launch { topicRepository.renameTopic(topic, name, topic.keywords) }
    }

    fun toggleTopicPinned(topicName: String) {
        val topic = _uiState.value.topics.firstOrNull { it.name == topicName } ?: return
        viewModelScope.launch { topicRepository.setPinned(topic, !topic.pinned) }
    }

    fun deleteTopic(topicName: String) {
        val topic = _uiState.value.topics.firstOrNull { it.name == topicName } ?: return
        viewModelScope.launch { topicRepository.deleteTopic(topic) }
    }
}

private data class NotesScreenCopy(
    val back: String,
    val searchNotes: String,
    val switchMode: String,
    val conversationModeTitle: String,
    val topicModeTitle: String,
    val switchToTopicMode: String,
    val switchToConversationMode: String,
    val searchPlaceholder: String,
    val topicSearchPlaceholder: String,
    val emptyNotes: String,
    val noNotesFound: String,
    val emptyTopics: String,
    val noTopicsFound: String,
    val addNote: String,
    val newNoteTitle: String,
    val newNoteContent: String,
    val renameNote: String,
    val pinned: String,
    val pin: String,
    val unpin: String,
    val rename: String,
    val delete: String,
    val view: String,
    val edit: String,
    val share: String,
    val cancel: String,
    val confirm: String,
    val sourceNotes: (Int) -> String,
    val topicSegments: (Int) -> String,
    val fromSource: (String) -> String,
    val sourceNoteTitle: String,
    val generatedFromFragments: String,
    val timePattern: String,
    val timeLocale: Locale
)

private fun notesScreenCopy(languagePreference: String): NotesScreenCopy {
    return if (languagePreference == AppLanguagePreference.EN) {
        NotesScreenCopy(
            back = "Back",
            searchNotes = "Search notes",
            switchMode = "Switch category mode",
            conversationModeTitle = "NoteLibrary",
            topicModeTitle = "Topics",
            switchToTopicMode = "Switch to topic mode",
            switchToConversationMode = "Switch to conversation mode",
            searchPlaceholder = "Search note titles or content...",
            topicSearchPlaceholder = "Search topics or fragments...",
            emptyNotes = "No notes yet. Generate one from a chat using the top feather button.",
            noNotesFound = "No matching notes.",
            emptyTopics = "No topic fragments yet. Generate or update notes first.",
            noTopicsFound = "No matching topics.",
            addNote = "New note",
            newNoteTitle = "New Note",
            newNoteContent = "# New Note\n\n",
            renameNote = "Rename this note",
            pinned = "Pinned",
            pin = "Pin",
            unpin = "Unpin",
            rename = "Rename",
            delete = "Delete",
            view = "View",
            edit = "Edit",
            share = "Share",
            cancel = "Cancel",
            confirm = "OK",
            sourceNotes = { count -> "$count source notes" },
            topicSegments = { count -> "$count fragments" },
            fromSource = { title -> "From $title" },
            sourceNoteTitle = "Source Notes",
            generatedFromFragments = "Aggregated from existing note fragments",
            timePattern = "M/d HH:mm",
            timeLocale = Locale.US
        )
    } else {
        NotesScreenCopy(
            back = "返回",
            searchNotes = "搜索笔记",
            switchMode = "切换分类模式",
            conversationModeTitle = "NoteLibrary",
            topicModeTitle = "主题笔记",
            switchToTopicMode = "切换到主题模式",
            switchToConversationMode = "切换到对话模式",
            searchPlaceholder = "搜索笔记标题或内容...",
            topicSearchPlaceholder = "搜索主题或片段...",
            emptyNotes = "暂无笔记，先在会话中点击顶部羽毛生成。",
            noNotesFound = "没有找到相关笔记。",
            emptyTopics = "暂无可归类的主题片段，先生成或更新一些笔记。",
            noTopicsFound = "没有找到相关主题。",
            addNote = "新增笔记",
            newNoteTitle = "新建笔记",
            newNoteContent = "# 新建笔记\n\n",
            renameNote = "重命名此笔记",
            pinned = "置顶",
            pin = "置顶",
            unpin = "取消置顶",
            rename = "重命名",
            delete = "删除",
            view = "查看",
            edit = "编辑",
            share = "分享",
            cancel = "取消",
            confirm = "确定",
            sourceNotes = { count -> "$count 篇来源笔记" },
            topicSegments = { count -> "$count 个片段" },
            fromSource = { title -> "来自 $title" },
            sourceNoteTitle = "来源笔记",
            generatedFromFragments = "由已有笔记片段聚合而来",
            timePattern = "M/d HH:mm",
            timeLocale = Locale.CHINA
        )
    }
}

@Composable
fun NotesScreen(
    languagePreference: String,
    onBack: () -> Unit,
    onOpenNote: (String) -> Unit,
    viewModel: NotesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDark = isCognoDarkTheme()
    val background = if (isDark) CognoDarkBackground else CognoBackground
    val copy = notesScreenCopy(languagePreference)
    var renameTarget by remember { mutableStateOf<NoteEntity?>(null) }
    var renameText by remember { mutableStateOf("") }
    var searchExpanded by remember { mutableStateOf(false) }
    var searchKeyword by remember { mutableStateOf("") }
    var topicMode by remember { mutableStateOf(false) }
    var selectedTopic by remember { mutableStateOf<TopicGroup?>(null) }
    var topicRenameTarget by remember { mutableStateOf<TopicGroup?>(null) }
    var topicRenameText by remember { mutableStateOf("") }
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
    val topicGroups = remember(uiState.topicSegments, uiState.notes) {
        buildPersistedTopicGroups(uiState.topicSegments, uiState.notes)
    }
    val displayTopicGroups = remember(topicGroups, uiState.topics) {
        val activeNames = uiState.topics.map { it.name }.toSet()
        val pinnedNames = uiState.topics.filter { it.pinned }.map { it.name }.toSet()
        topicGroups
            .filter { it.topic in activeNames }
            .sortedWith(
                compareByDescending<TopicGroup> { it.topic in pinnedNames }
                    .thenByDescending { it.segments.size }
                    .thenByDescending { it.updatedAt }
            )
    }
    val visibleTopicGroups = remember(displayTopicGroups, searchKeyword) {
        val keyword = searchKeyword.trim()
        if (keyword.isBlank()) {
            displayTopicGroups
        } else {
            displayTopicGroups.filter { group ->
                group.topic.contains(keyword, ignoreCase = true) ||
                    group.segments.any { segment ->
                        segment.text.contains(keyword, ignoreCase = true) ||
                            segment.sourceTitle.contains(keyword, ignoreCase = true)
                    }
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
                copy = copy,
                topicMode = topicMode,
                onBack = onBack,
                onToggleSearch = {
                    searchExpanded = !searchExpanded
                    if (!searchExpanded) searchKeyword = ""
                },
                onToggleMode = {
                    topicMode = !topicMode
                    searchKeyword = ""
                    selectedTopic = null
                }
            )
            NoteSearchBar(
                expanded = searchExpanded,
                value = searchKeyword,
                isDark = isDark,
                copy = copy,
                topicMode = topicMode,
                onValueChange = { searchKeyword = it }
            )
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (topicMode) {
                    if (visibleTopicGroups.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (searchKeyword.isBlank()) copy.emptyTopics else copy.noTopicsFound,
                                    color = CognoMuted,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    items(visibleTopicGroups, key = { it.topic }) { group ->
                        TopicCard(
                            group = group,
                            isDark = isDark,
                            copy = copy,
                            pinned = uiState.topics.firstOrNull { it.name == group.topic }?.pinned == true,
                            onClick = { selectedTopic = group },
                            onRename = {
                                topicRenameTarget = group
                                topicRenameText = group.topic
                            },
                            onTogglePin = {
                                viewModel.toggleTopicPinned(group.topic)
                            },
                            onDelete = {
                                viewModel.deleteTopic(group.topic)
                                if (selectedTopic?.key == group.key) selectedTopic = null
                            }
                        )
                    }
                } else if (visibleNotes.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (searchKeyword.isBlank()) copy.emptyNotes else copy.noNotesFound,
                                color = CognoMuted,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                if (!topicMode) {
                    items(visibleNotes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            isDark = isDark,
                            copy = copy,
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
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 34.dp, bottom = 62.dp)
                .size(56.dp)
                .clip(CircleShape)
                .clickable {
                    viewModel.createNote(copy.newNoteTitle, copy.newNoteContent, onOpenNote)
                }
                .background(if (isDark) CognoDarkPrimary else CognoPrimary),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = copy.addNote, tint = Color.White)
        }
        renameTarget?.let { note ->
            RenameDialog(
                title = copy.renameNote,
                value = renameText,
                isDark = isDark,
                copy = copy,
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
        topicRenameTarget?.let { group ->
            RenameDialog(
                title = copy.renameNote,
                value = topicRenameText,
                isDark = isDark,
                copy = copy,
                onValueChange = { topicRenameText = it },
                onDismiss = { topicRenameTarget = null },
                onConfirm = {
                    val nextTitle = topicRenameText.trim()
                    if (nextTitle.isNotEmpty()) {
                        viewModel.renameTopic(group.topic, nextTitle)
                    }
                    topicRenameTarget = null
                }
            )
        }
        selectedTopic?.let { group ->
            TopicDetailView(
                group = group,
                isDark = isDark,
                copy = copy,
                onBack = { selectedTopic = null }
            )
        }
    }
}

@Composable
private fun NotesTopBar(
    isDark: Boolean,
    copy: NotesScreenCopy,
    topicMode: Boolean,
    onBack: () -> Unit,
    onToggleSearch: () -> Unit,
    onToggleMode: () -> Unit
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
                    contentDescription = copy.back,
                    tint = if (isDark) CognoDarkPrimary else CognoPrimary
                )
            }
            Text(
                text = if (topicMode) copy.topicModeTitle else copy.conversationModeTitle,
                color = if (isDark) CognoDarkText else CognoText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Row {
            IconButton(onClick = onToggleSearch) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = copy.searchNotes,
                    tint = if (isDark) CognoDarkPrimary else CognoPrimary
                )
            }
            IconButton(onClick = onToggleMode) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = if (topicMode) copy.switchToConversationMode else copy.switchToTopicMode,
                    tint = if (topicMode) {
                        if (isDark) CognoDarkText else CognoText
                    } else {
                        if (isDark) CognoDarkPrimary else CognoPrimary
                    }
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
    copy: NotesScreenCopy,
    topicMode: Boolean,
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
                            Text(
                                if (topicMode) copy.topicSearchPlaceholder else copy.searchPlaceholder,
                                color = CognoMuted,
                                fontSize = 14.sp
                            )
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
    copy: NotesScreenCopy,
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
                        contentDescription = copy.pinned,
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
                text = formatNoteTime(note.updatedAt, copy),
                color = CognoMuted,
                fontSize = 9.sp,
                modifier = Modifier.align(Alignment.End)
            )
        }

        if (menuExpanded) {
            WebStyleContextMenu(
                isDark = isDark,
                pinText = if (note.pinned) copy.unpin else copy.pin,
                copy = copy,
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TopicCard(
    group: TopicGroup,
    isDark: Boolean,
    copy: NotesScreenCopy,
    pinned: Boolean,
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
                    text = group.topic,
                    color = if (isDark) CognoDarkText else CognoText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (pinned) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = copy.pinned,
                        tint = if (isDark) CognoDarkPrimary else CognoPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = group.preview,
                color = CognoMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatNoteTime(group.updatedAt, copy),
                color = CognoMuted,
                fontSize = 9.sp,
                modifier = Modifier.align(Alignment.End)
            )
        }

        if (menuExpanded) {
            WebStyleContextMenu(
                isDark = isDark,
                pinText = if (pinned) copy.unpin else copy.pin,
                copy = copy,
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
private fun TopicDetailView(
    group: TopicGroup,
    isDark: Boolean,
    copy: NotesScreenCopy,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isEditing by remember(group.key) { mutableStateOf(false) }
    var editContent by remember(group.key) { mutableStateOf(group.toMarkdown(copy)) }
    var exportDialogVisible by remember(group.key) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) CognoDarkBackground else CognoBackground)
    ) {
        TopicDetailTopBar(
            isDark = isDark,
            copy = copy,
            isEditing = isEditing,
            onBack = onBack,
            onToggleEdit = { isEditing = !isEditing },
            onExport = { exportDialogVisible = true }
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 18.dp)
        ) {
            Text(
                text = group.topic,
                color = if (isDark) CognoDarkText else CognoText,
                fontSize = 26.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = formatNoteTime(group.updatedAt, copy),
                color = CognoMuted,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${copy.sourceNotes(group.sourceCount)} · ${copy.topicSegments(group.segments.size)} · ${copy.generatedFromFragments}",
                color = if (isDark) CognoDarkPrimary else CognoPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = copy.sourceNoteTitle,
                color = CognoMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            group.sourceTitles.forEach { sourceTitle ->
                Text(
                    text = sourceTitle,
                    color = if (isDark) CognoDarkText.copy(alpha = 0.82f) else CognoText.copy(alpha = 0.82f),
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
            Spacer(modifier = Modifier.height(28.dp))
            if (isEditing) {
                TopicContentEditor(
                    value = editContent,
                    isDark = isDark,
                    onValueChange = { editContent = it }
                )
            } else {
                TopicMarkdownSegments(group = group, isDark = isDark, copy = copy)
            }
        }
    }
    if (exportDialogVisible) {
        TopicMarkdownExportDialog(
            isDark = isDark,
            onDismiss = { exportDialogVisible = false },
            onExport = {
                exportDialogVisible = false
                val markdown = "# ${group.topic}\n\n${group.toMarkdown(copy)}"
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "${group.topic}.md")
                    putExtra(Intent.EXTRA_TEXT, markdown)
                }
                context.startActivity(Intent.createChooser(intent, group.topic))
            }
        )
    }
}

@Composable
private fun TopicDetailTopBar(
    isDark: Boolean,
    copy: NotesScreenCopy,
    isEditing: Boolean,
    onBack: () -> Unit,
    onToggleEdit: () -> Unit,
    onExport: () -> Unit
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
            IconButton(onClick = onExport) {
                Icon(
                    imageVector = Icons.Default.IosShare,
                    contentDescription = copy.share,
                    tint = if (isDark) CognoDarkPrimary else CognoPrimary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopicMarkdownExportDialog(
    isDark: Boolean,
    onDismiss: () -> Unit,
    onExport: () -> Unit
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            color = if (isDark) CognoDarkSurface else Color.White,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp)
                .border(1.dp, if (isDark) CognoDarkLine else CognoLine, RoundedCornerShape(22.dp))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    "导出主题笔记",
                    color = if (isDark) CognoDarkText else CognoText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Markdown (.md)  ✓",
                    color = if (isDark) CognoDarkPrimary else CognoPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background((if (isDark) CognoDarkPrimary else CognoPrimary).copy(alpha = 0.12f))
                        .padding(14.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "取消",
                        color = if (isDark) CognoDarkText else CognoText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onDismiss)
                            .padding(vertical = 12.dp)
                    )
                    Text(
                        "导出",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) CognoDarkPrimary else CognoPrimary)
                            .clickable(onClick = onExport)
                            .padding(vertical = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TopicContentEditor(
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
private fun TopicMarkdownSegments(
    group: TopicGroup,
    isDark: Boolean,
    copy: NotesScreenCopy
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        group.segments.forEachIndexed { index, segment ->
            BasicMarkdown(
                content = segment.text,
                isDark = isDark,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = copy.fromSource(segment.sourceTitle),
                color = CognoMuted,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
            if (index != group.segments.lastIndex) {
                Spacer(modifier = Modifier.height(22.dp))
            }
        }
    }
}

private val TopicGroup.preview: String
    get() = segments.firstOrNull()?.text.orEmpty()

private val TopicGroup.sourceTitles: List<String>
    get() = segments
        .map { it.sourceTitle }
        .distinct()

private fun TopicGroup.toMarkdown(copy: NotesScreenCopy): String {
    return buildString {
        segments.forEach { segment ->
            append(segment.text)
            append("\n\n")
            append("> ")
            append(copy.fromSource(segment.sourceTitle))
            append("\n\n")
        }
    }.trim()
}

private fun formatNoteTime(timestamp: Long, copy: NotesScreenCopy): String {
    return SimpleDateFormat(copy.timePattern, copy.timeLocale).format(Date(timestamp))
}

private fun buildPersistedTopicGroups(
    segments: List<NoteTopicSegmentEntity>,
    notes: List<NoteEntity>
): List<TopicGroup> {
    val notesById = notes.associateBy { it.id }
    return segments
        .mapNotNull { segment ->
            val note = notesById[segment.noteId] ?: return@mapNotNull null
            TopicSegment(
                topic = segment.topicName,
                text = buildString {
                    if (segment.heading.isNotBlank()) {
                        append("## ")
                        append(segment.heading)
                        append("\n\n")
                    }
                    append(segment.content)
                },
                sourceNoteId = note.id,
                sourceTitle = note.title,
                updatedAt = segment.createdAt
            )
        }
        .groupBy { it.topic }
        .map { (topic, topicSegments) ->
            TopicGroup(
                topic = topic,
                segments = topicSegments.sortedByDescending { it.updatedAt },
                updatedAt = topicSegments.maxOfOrNull { it.updatedAt } ?: 0L
            )
        }
}

private fun buildTopicGroups(notes: List<NoteEntity>, languagePreference: String): List<TopicGroup> {
    return notes
        .flatMap { note -> note.toTopicSegments(languagePreference) }
        .groupBy { it.topic }
        .map { (topic, segments) ->
            val sortedSegments = segments.sortedByDescending { it.updatedAt }
            TopicGroup(
                topic = topic,
                segments = sortedSegments,
                updatedAt = sortedSegments.maxOfOrNull { it.updatedAt } ?: 0L
            )
        }
        .sortedWith(
            compareByDescending<TopicGroup> { it.segments.size }
                .thenByDescending { it.updatedAt }
        )
}

private fun NoteEntity.toTopicSegments(languagePreference: String): List<TopicSegment> {
    val blocks = contentBlocks(content)
    val source = title.ifBlank { preview.ifBlank { "Untitled" } }
    return blocks
        .mapNotNull { block ->
            val clean = block.cleanTopicText()
            if (clean.length < MIN_TOPIC_SEGMENT_LENGTH) return@mapNotNull null
            TopicSegment(
                topic = inferTopic(clean, languagePreference),
                text = clean.take(TOPIC_SEGMENT_PREVIEW_LIMIT),
                sourceNoteId = id,
                sourceTitle = source,
                updatedAt = updatedAt
            )
        }
        .ifEmpty {
            val clean = preview.cleanTopicText()
            if (clean.isBlank()) {
                emptyList()
            } else {
                listOf(
                    TopicSegment(
                        topic = inferTopic("$title $clean", languagePreference),
                        text = clean.take(TOPIC_SEGMENT_PREVIEW_LIMIT),
                        sourceNoteId = id,
                        sourceTitle = source,
                        updatedAt = updatedAt
                    )
                )
            }
        }
}

private fun contentBlocks(content: String): List<String> {
    val blocks = mutableListOf<String>()
    val current = StringBuilder()

    content.lines().forEach { rawLine ->
        val line = rawLine.trim()
        val startsNewSection = line.startsWith("#") && current.isNotBlank()
        if (line.isBlank() || startsNewSection) {
            if (current.isNotBlank()) {
                blocks += current.toString()
                current.clear()
            }
        }
        if (line.isNotBlank()) {
            current.appendLine(line)
        }
    }

    if (current.isNotBlank()) blocks += current.toString()
    return blocks
}

private fun String.cleanTopicText(): String {
    return replace(Regex("```[\\s\\S]*?```"), " ")
        .replace(Regex("[#>*_`]+"), " ")
        .replace(Regex("^[-+\\d.\\s]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun inferTopic(text: String, languagePreference: String): String {
    val normalized = text.lowercase(Locale.ROOT)
    return TOPIC_RULES.firstOrNull { rule ->
        rule.keywords.any { keyword -> normalized.contains(keyword) }
    }?.label(languagePreference) ?: if (languagePreference == AppLanguagePreference.EN) "General Knowledge" else "综合知识"
}

private data class TopicRule(
    val zh: String,
    val en: String,
    val keywords: List<String>
) {
    fun label(languagePreference: String): String =
        if (languagePreference == AppLanguagePreference.EN) en else zh
}

private val TOPIC_RULES = listOf(
    TopicRule("技术学习", "Technology Learning", listOf("java", "kotlin", "sql", "python", "android", "compose", "database", "room", "api", "编程", "代码", "数据库", "技术", "开发", "计算机")),
    TopicRule("金融知识", "Finance Knowledge", listOf("金融", "投资", "股票", "基金", "利率", "银行", "债券", "市场", "资产", "通胀", "finance", "stock", "fund", "bank")),
    TopicRule("生活常识", "Everyday Knowledge", listOf("生活", "常识", "饮食", "睡眠", "运动", "健康", "习惯", "日常", "衣食住行")),
    TopicRule("生理知识", "Physiology", listOf("生理", "身体", "医学", "疾病", "症状", "激素", "大脑", "神经", "血糖", "血压")),
    TopicRule("哲学探讨", "Philosophy", listOf("哲学", "存在主义", "意义", "自由", "虚无", "伦理", "意识", "人生", "existential", "philosophy")),
    TopicRule("情绪管理", "Emotional Health", listOf("焦虑", "内耗", "压力", "情绪", "抑郁", "恐惧", "自责", "失眠", "心理", "anxiety", "stress")),
    TopicRule("家庭关系", "Family Relationships", listOf("父母", "家庭", "亲子", "关系", "沟通", "边界", "妈妈", "爸爸", "伴侣", "family", "parents")),
    TopicRule("学习方法", "Study Methods", listOf("学习", "复习", "记忆", "考试", "课程", "方法", "计划", "效率", "study", "review"))
)

private const val MIN_TOPIC_SEGMENT_LENGTH = 12
private const val TOPIC_SEGMENT_PREVIEW_LIMIT = 180

@Composable
private fun WebStyleContextMenu(
    isDark: Boolean,
    pinText: String,
    copy: NotesScreenCopy,
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
            shadowElevation = 8.dp,
            modifier = modifier
                .padding(top = 8.dp, end = 20.dp)
                .fillMaxWidth(0.54f)
                .border(
                    width = 1.dp,
                    color = if (isDark) CognoDarkLine else CognoLine,
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Column {
                ContextMenuAction(copy.rename, Icons.Default.Edit, isDark, onRename, isFirst = true)
                ContextMenuAction(pinText, Icons.Default.PushPin, isDark, onTogglePin)
                ContextMenuAction(copy.delete, Icons.Default.Delete, isDark, onDelete, destructive = true, isLast = true)
            }
        }
    }
}

@Composable
private fun ContextMenuAction(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isDark: Boolean,
    onClick: () -> Unit,
    destructive: Boolean = false,
    isFirst: Boolean = false,
    isLast: Boolean = false
) {
    val rowShape = RoundedCornerShape(
        topStart = if (isFirst) 16.dp else 0.dp,
        topEnd = if (isFirst) 16.dp else 0.dp,
        bottomStart = if (isLast) 16.dp else 0.dp,
        bottomEnd = if (isLast) 16.dp else 0.dp
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(rowShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (destructive) Color(0xFFE24A4A) else CognoMuted,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            color = if (destructive) Color(0xFFE24A4A) else if (isDark) CognoDarkText else CognoText,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RenameDialog(
    title: String,
    value: String,
    isDark: Boolean,
    copy: NotesScreenCopy,
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
                        text = copy.cancel,
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
                        text = copy.confirm,
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
