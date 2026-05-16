package com.ntoprevd.cogno.ui.chat

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AddComment
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ntoprevd.cogno.data.db.entity.MessageEntity
import com.ntoprevd.cogno.data.db.entity.SessionEntity
import com.ntoprevd.cogno.ui.theme.CognoBackground
import com.ntoprevd.cogno.ui.theme.CognoDarkBackground
import com.ntoprevd.cogno.ui.theme.CognoDarkLine
import com.ntoprevd.cogno.ui.theme.CognoDarkPrimary
import com.ntoprevd.cogno.ui.theme.CognoDarkSurface
import com.ntoprevd.cogno.ui.theme.CognoDarkText
import com.ntoprevd.cogno.ui.theme.CognoDarkUserBubble
import com.ntoprevd.cogno.ui.theme.CognoLine
import com.ntoprevd.cogno.ui.theme.CognoMuted
import com.ntoprevd.cogno.ui.theme.CognoPrimary
import com.ntoprevd.cogno.ui.theme.CognoSurface
import com.ntoprevd.cogno.ui.theme.CognoText
import com.ntoprevd.cogno.ui.theme.CognoUserBubble
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    onOpenNotes: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDark = isSystemInDarkTheme()
    val listState = rememberLazyListState()
    val background = if (isDark) CognoDarkBackground else CognoBackground

    LaunchedEffect(uiState.messages.size, uiState.currentSessionId) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ChatTopBar(
                isDark = isDark,
                onOpenDrawer = viewModel::openDrawer,
                onNewSession = viewModel::startNewSession
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Crossfade(
                    targetState = uiState.showWelcome,
                    animationSpec = tween(260),
                    label = "chat-content"
                ) { showWelcome ->
                    if (showWelcome) {
                        WelcomeView(isDark = isDark)
                    } else {
                        MessageList(
                            messages = uiState.messages,
                            isDark = isDark,
                            listState = listState,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            uiState.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = Color(0xFFD94841),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            ChatInputBar(
                text = uiState.inputText,
                isDark = isDark,
                isSending = uiState.isSending,
                onTextChange = viewModel::onInputChange,
                onSend = viewModel::sendMessage
            )
        }

        SidebarDrawer(
            isOpen = uiState.isDrawerOpen,
            sessions = uiState.sessions,
            currentSessionId = uiState.currentSessionId,
            isDark = isDark,
            onOpen = viewModel::openDrawer,
            onClose = viewModel::closeDrawer,
            onSelectSession = viewModel::selectSession,
            onRenameSession = viewModel::renameSession,
            onTogglePinSession = viewModel::toggleSessionPinned,
            onDeleteSession = viewModel::deleteSession,
            onOpenNotes = {
                viewModel.closeDrawer()
                onOpenNotes()
            },
            onOpenSettings = {
                viewModel.closeDrawer()
                onOpenSettings()
            }
        )
    }
}

@Composable
private fun ChatTopBar(
    isDark: Boolean,
    onOpenDrawer: () -> Unit,
    onNewSession: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onOpenDrawer,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "打开侧边栏",
                    tint = if (isDark) CognoDarkPrimary else CognoPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = "Cogno",
                color = if (isDark) CognoDarkText else CognoText,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                letterSpacing = 0.sp
            )
            Text(
                text = "DeepSeek-V3",
                color = if (isDark) CognoDarkPrimary else CognoPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                lineHeight = 9.sp,
                modifier = Modifier
                    .padding(start = 7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background((if (isDark) CognoDarkPrimary else CognoPrimary).copy(alpha = 0.11f))
                    .border(
                        width = 1.dp,
                        color = (if (isDark) CognoDarkPrimary else CognoPrimary).copy(alpha = 0.14f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { }, modifier = Modifier.size(42.dp)) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = "生成笔记",
                    tint = if (isDark) CognoDarkPrimary else CognoPrimary,
                    modifier = Modifier.size(21.dp)
                )
            }
            IconButton(onClick = onNewSession, modifier = Modifier.size(42.dp)) {
                Icon(
                    imageVector = Icons.Outlined.AddComment,
                    contentDescription = "新建会话",
                    tint = if (isDark) CognoDarkPrimary else CognoPrimary,
                    modifier = Modifier.size(21.dp)
                )
            }
        }
    }
}

@Composable
private fun WelcomeView(isDark: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "✦", fontSize = 50.sp, lineHeight = 50.sp)
        Spacer(modifier = Modifier.height(38.dp))
        Text(
            text = "把问题、灵感和碎片想法都放进来。\nCogno 会先帮你沉淀成对话，之后再整理成笔记。",
            color = if (isDark) CognoDarkText.copy(alpha = 0.70f) else CognoMuted.copy(alpha = 0.86f),
            fontSize = 15.sp,
            lineHeight = 27.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            letterSpacing = 0.sp
        )
    }
}

@Composable
private fun MessageList(
    messages: List<MessageEntity>,
    isDark: Boolean,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(top = 24.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        items(messages, key = { it.id }) { message ->
            MessageBubble(message = message, isDark = isDark)
        }
    }
}

@Composable
private fun MessageBubble(message: MessageEntity, isDark: Boolean) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isUser) {
                if (isDark) CognoDarkUserBubble else CognoUserBubble
            } else {
                if (isDark) CognoDarkSurface else CognoSurface
            },
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .widthIn(max = 310.dp)
                .then(
                    if (!isDark) {
                        Modifier.border(
                            width = 1.dp,
                            color = Color.Black.copy(alpha = 0.04f),
                            shape = RoundedCornerShape(20.dp)
                        )
                    } else {
                        Modifier
                    }
                )
        ) {
            Text(
                text = message.content,
                color = if (isDark) CognoDarkText else CognoText,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun ChatInputBar(
    text: String,
    isDark: Boolean,
    isSending: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (isDark) 10.dp else 8.dp,
                    shape = RoundedCornerShape(30.dp),
                    ambientColor = Color.Black.copy(alpha = if (isDark) 0.42f else 0.03f),
                    spotColor = Color.Black.copy(alpha = if (isDark) 0.42f else 0.03f)
                )
                .clip(RoundedCornerShape(30.dp))
                .background(if (isDark) CognoDarkSurface else CognoSurface)
                .border(
                    width = 1.dp,
                    color = if (isDark) CognoDarkLine else CognoLine,
                    shape = RoundedCornerShape(30.dp)
                )
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { }, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "更多",
                    tint = CognoMuted,
                    modifier = Modifier.size(21.dp)
                )
            }
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                textStyle = TextStyle(
                    color = if (isDark) CognoDarkText else CognoText,
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                ),
                minLines = 1,
                maxLines = 4,
                cursorBrush = androidx.compose.ui.graphics.SolidColor(
                    if (isDark) CognoDarkPrimary else CognoPrimary
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp, vertical = 9.dp),
                decorationBox = { innerTextField ->
                    if (text.isEmpty()) {
                        Text(
                            text = "输入消息...",
                            color = CognoMuted,
                            fontSize = 16.sp,
                            lineHeight = 22.sp
                        )
                    }
                    innerTextField()
                }
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = { }, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "语音输入",
                        tint = if (isDark) CognoDarkText.copy(alpha = 0.8f) else CognoText.copy(alpha = 0.8f),
                        modifier = Modifier.size(21.dp)
                    )
                }
                FilledIconButton(
                    onClick = onSend,
                    enabled = text.isNotBlank() && !isSending,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isDark) CognoDarkPrimary else CognoPrimary,
                        disabledContainerColor = CognoMuted.copy(alpha = 0.35f),
                        contentColor = Color.White,
                        disabledContentColor = Color.White.copy(alpha = 0.92f)
                    ),
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "发送",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SidebarDrawer(
    isOpen: Boolean,
    sessions: List<SessionEntity>,
    currentSessionId: String?,
    isDark: Boolean,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    onSelectSession: (String) -> Unit,
    onRenameSession: (String, String) -> Unit,
    onTogglePinSession: (SessionEntity) -> Unit,
    onDeleteSession: (String) -> Unit,
    onOpenNotes: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val drawerProgress = remember { Animatable(if (isOpen) 1f else 0f) }
    var renameTarget by remember { mutableStateOf<SessionEntity?>(null) }
    var renameText by remember { mutableStateOf("") }

    LaunchedEffect(isOpen) {
        drawerProgress.animateTo(
            targetValue = if (isOpen) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val drawerWidthPx = with(LocalDensity.current) { (maxWidth * 0.85f).toPx() }
        val progress = drawerProgress.value
        val isVisible = progress > 0.01f || isOpen
        val dragModifier = Modifier.pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragStart = {
                    scope.launch { drawerProgress.stop() }
                },
                onHorizontalDrag = { _, dragAmount ->
                    scope.launch {
                        drawerProgress.snapTo(
                            (drawerProgress.value + dragAmount / drawerWidthPx)
                                .coerceIn(0f, 1f)
                        )
                    }
                },
                onDragEnd = {
                    val shouldOpen = drawerProgress.value > 0.38f
                    if (shouldOpen) onOpen() else onClose()
                    scope.launch {
                        drawerProgress.animateTo(
                            targetValue = if (shouldOpen) 1f else 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }
                },
                onDragCancel = {
                    val shouldOpen = drawerProgress.value > 0.38f
                    if (shouldOpen) onOpen() else onClose()
                    scope.launch {
                        drawerProgress.animateTo(
                            targetValue = if (shouldOpen) 1f else 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }
                }
            )
        }

        if (!isVisible) {
            // 关闭状态只保留左边缘热区，避免遮挡聊天输入框、顶部按钮和其他页面点击。
            Box(
                modifier = dragModifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(12.dp)
            )
        }

        if (isVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = (if (isDark) 0.6f else 0.4f) * progress))
                    .then(
                        if (isOpen) {
                            dragModifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onClose
                            )
                        } else {
                            Modifier
                        }
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.85f)
                    .offset {
                        IntOffset(
                            x = (-drawerWidthPx * (1f - progress)).roundToInt(),
                            y = 0
                        )
                    }
                    .then(dragModifier)
                    .shadow(
                        elevation = if (isDark) 20.dp else 12.dp,
                        ambientColor = Color.Black.copy(alpha = if (isDark) 0.50f else 0.08f),
                        spotColor = Color.Black.copy(alpha = if (isDark) 0.50f else 0.08f)
                    )
                    .background(if (isDark) CognoDarkSurface else Color.White)
                    .border(
                        width = 1.dp,
                        color = if (isDark) CognoDarkLine else Color.Transparent
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    NoteEntry(isDark = isDark, onClick = onOpenNotes)
                    SidebarSearchField(isDark = isDark)
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Text(
                        text = "今天",
                        color = CognoMuted,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                    )
                    if (sessions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("暂无历史会话", color = CognoMuted, fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(sessions, key = { it.id }) { session ->
                                SessionRow(
                                    session = session,
                                    selected = session.id == currentSessionId,
                                    isDark = isDark,
                                    onClick = { onSelectSession(session.id) },
                                    onRename = {
                                        renameTarget = session
                                        renameText = session.title
                                    },
                                    onTogglePin = { onTogglePinSession(session) },
                                    onDelete = { onDeleteSession(session.id) }
                                )
                            }
                        }
                    }
                }
                SidebarFooter(isDark = isDark, onOpenSettings = onOpenSettings)
            }
        }

        renameTarget?.let { session ->
            RenameDialog(
                title = "重命名此对话",
                value = renameText,
                isDark = isDark,
                onValueChange = { renameText = it },
                onDismiss = { renameTarget = null },
                onConfirm = {
                    onRenameSession(session.id, renameText)
                    renameTarget = null
                }
            )
        }
    }
}

@Composable
private fun SidebarSearchField(isDark: Boolean) {
    var keyword by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDark) CognoDarkBackground else CognoBackground)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = CognoMuted,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        BasicTextField(
            value = keyword,
            onValueChange = { keyword = it },
            singleLine = true,
            textStyle = TextStyle(
                color = if (isDark) CognoDarkText else CognoText,
                fontSize = 14.sp,
                lineHeight = 20.sp
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(
                if (isDark) CognoDarkPrimary else CognoPrimary
            ),
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                if (keyword.isEmpty()) {
                    Text("搜索历史对话...", color = CognoMuted, fontSize = 14.sp)
                }
                innerTextField()
            }
        )
    }
}

@Composable
private fun NoteEntry(isDark: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background((if (isDark) CognoDarkPrimary else CognoPrimary).copy(alpha = if (isDark) 0.15f else 0.10f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Bookmark,
            contentDescription = null,
            tint = if (isDark) CognoDarkPrimary else CognoPrimary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "进入笔记库",
            color = if (isDark) CognoDarkPrimary else CognoPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SessionRow(
    session: SessionEntity,
    selected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    val selectedBackground = if (isDark) Color(0xFF3F3A36).copy(alpha = 0.45f) else CognoBackground
    var menuExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) selectedBackground else Color.Transparent)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { menuExpanded = true }
                )
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.ChatBubbleOutline,
                contentDescription = null,
                tint = if (selected || session.pinned) {
                    if (isDark) CognoDarkPrimary else CognoPrimary
                } else {
                    CognoMuted
                },
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = session.title,
                color = if (isDark) CognoDarkText else CognoText,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (session.pinned) {
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = "置顶",
                    tint = if (isDark) CognoDarkPrimary else CognoPrimary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        if (menuExpanded) {
            WebStyleContextMenu(
                isDark = isDark,
                pinText = if (session.pinned) "取消置顶" else "置顶",
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
                .padding(top = 8.dp, end = 14.dp)
                .width(232.dp)
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

@Composable
private fun SidebarFooter(isDark: Boolean, onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isDark) CognoDarkLine else CognoLine
            )
            .clickable(onClick = onOpenSettings)
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isDark) CognoDarkPrimary else CognoPrimary),
            contentAlignment = Alignment.Center
        ) {
            Text("JD", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Jane Doe",
            color = if (isDark) CognoDarkText else CognoText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.MoreHoriz,
            contentDescription = "设置",
            tint = CognoMuted,
            modifier = Modifier.size(28.dp)
        )
    }
}
