package com.ntoprevd.cogno.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.io.File
import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
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
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.ntoprevd.cogno.R
import com.ntoprevd.cogno.data.db.entity.MessageEntity
import com.ntoprevd.cogno.data.db.entity.SessionEntity
import com.ntoprevd.cogno.data.settings.AppLanguagePreference
import com.ntoprevd.cogno.ui.chat.NoteToast
import com.ntoprevd.cogno.ui.common.BasicMarkdown
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
import com.ntoprevd.cogno.ui.theme.isCognoDarkTheme
import com.ntoprevd.cogno.ui.settings.UserAvatar
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val FEEDBACK_LIKE = "like"
private const val FEEDBACK_DISLIKE = "dislike"

private enum class VoiceInputPhase {
    IDLE,
    LISTENING,
    PROCESSING,
    MESSAGE
}

private data class VoiceInputState(
    val phase: VoiceInputPhase = VoiceInputPhase.IDLE,
    val detail: String = ""
)

private data class PendingAttachment(
    val uri: Uri,
    val displayName: String,
    val isImage: Boolean
)

private data class ChatScreenCopy(
    val openSidebar: String,
    val generateNote: String,
    val newSession: String,
    val noteStyleTitle: String,
    val conciseTitle: String,
    val conciseDescription: String,
    val concisePrompt: String,
    val standardTitle: String,
    val standardDescription: String,
    val standardPrompt: String,
    val detailedTitle: String,
    val detailedDescription: String,
    val detailedPrompt: String,
    val thinking: String,
    val emptyContent: String,
    val copy: String,
    val like: String,
    val dislike: String,
    val regenerate: String,
    val edit: String,
    val editMessage: String,
    val cancel: String,
    val confirm: String,
    val more: String,
    val attachmentTitle: String,
    val camera: String,
    val photos: String,
    val files: String,
    val attachmentPending: String,
    val inputPlaceholder: String,
    val voiceInput: String,
    val listening: String,
    val voiceReleaseToFinish: String,
    val voiceReleaseToCancel: String,
    val voiceProcessing: String,
    val voiceProcessingTimeout: String,
    val voicePermissionDenied: String,
    val voicePermissionGranted: String,
    val voiceCancelled: String,
    val voiceNoMatch: String,
    val voiceUnavailable: String,
    val voiceFailed: String,
    val send: String,
    val stopGenerating: String,
    val today: String,
    val emptySessions: String,
    val noSessionFound: String,
    val renameSession: String,
    val searchSessions: String,
    val openNotes: String,
    val pinned: String,
    val pin: String,
    val unpin: String,
    val rename: String,
    val delete: String,
    val settings: String,
    val messageTimePattern: String,
    val messageTimeLocale: Locale,
    val welcomePhrases: List<String>,
    val legacyWelcome: String
)

private fun chatScreenCopy(languagePreference: String): ChatScreenCopy {
    return if (languagePreference == AppLanguagePreference.EN) {
        ChatScreenCopy(
            openSidebar = "Open sidebar",
            generateNote = "Generate note",
            newSession = "New chat",
            noteStyleTitle = "Choose note style",
            conciseTitle = "Concise Summary",
            conciseDescription = "Keep only the core conclusions for a quick review.",
            concisePrompt = "Concise: keep only core conclusions and necessary context, without too much detail.",
            standardTitle = "Standard Note",
            standardDescription = "Keep structure, key points, and explanations for review.",
            standardPrompt = "Standard: use a clear structure and keep key explanations, steps, and caveats.",
            detailedTitle = "Detailed Review",
            detailedDescription = "Expand context, examples, and reasoning as much as possible.",
            detailedPrompt = "Detailed: fully expand concepts, steps, examples, caveats, and reviewable details.",
            thinking = "Thinking...",
            emptyContent = "No content",
            copy = "Copy",
            like = "Like",
            dislike = "Dislike",
            regenerate = "Regenerate",
            edit = "Edit",
            editMessage = "Edit Message",
            cancel = "Cancel",
            confirm = "OK",
            more = "More",
            attachmentTitle = "Add attachment",
            camera = "Camera",
            photos = "Photos",
            files = "Files",
            attachmentPending = "Image selected · ready to send",
            inputPlaceholder = "Type a message...",
            voiceInput = "Voice input",
            listening = "Listening...",
            voiceReleaseToFinish = "Release to finish · slide away to cancel",
            voiceReleaseToCancel = "Release to cancel",
            voiceProcessing = "Recognizing speech...",
            voiceProcessingTimeout = "Recognition timed out. Please try again.",
            voicePermissionDenied = "Microphone permission denied",
            voicePermissionGranted = "Permission granted · hold again to speak",
            voiceCancelled = "Voice input cancelled",
            voiceNoMatch = "No speech recognized. Please try again.",
            voiceUnavailable = "No compatible speech recognition engine is available",
            voiceFailed = "Speech recognition failed. Please try again.",
            send = "Send",
            stopGenerating = "Stop generating",
            today = "Today",
            emptySessions = "No chat history",
            noSessionFound = "No matching chats",
            renameSession = "Rename this chat",
            searchSessions = "Search chat history...",
            openNotes = "Open Note Library",
            pinned = "Pinned",
            pin = "Pin",
            unpin = "Unpin",
            rename = "Rename",
            delete = "Delete",
            settings = "Settings",
            messageTimePattern = "MMM d, yyyy HH:mm",
            messageTimeLocale = Locale.US,
            welcomePhrases = listOf(
                "Think it through. Keep what matters.",
                "Questions become conversations. Conversations become memory.",
                "Catch the spark before it disappears.",
                "Bring the loose thoughts. Cogno will shape them."
            ),
            legacyWelcome = "Bring questions, ideas, and loose thoughts here.\nCogno will turn them into conversation first, then organize them into notes."
        )
    } else {
        ChatScreenCopy(
            openSidebar = "打开侧边栏",
            generateNote = "生成笔记",
            newSession = "新建会话",
            noteStyleTitle = "选择总结风格",
            conciseTitle = "简洁摘要",
            conciseDescription = "只保留核心结论，适合快速回顾。",
            concisePrompt = "简洁：只保留核心结论和必要背景，不展开过多细节。",
            standardTitle = "标准笔记",
            standardDescription = "保留结构、重点和解释，适合复习。",
            standardPrompt = "标准：结构清晰，保留关键解释、步骤和注意事项。",
            detailedTitle = "详细复习",
            detailedDescription = "尽量展开上下文、例子和推导。",
            detailedPrompt = "详细：充分展开概念、步骤、例子、注意事项和可复习的细节。",
            thinking = "正在思考...",
            emptyContent = "暂无内容",
            copy = "复制",
            like = "点赞",
            dislike = "点踩",
            regenerate = "重新生成",
            edit = "修改",
            editMessage = "修改消息",
            cancel = "取消",
            confirm = "确定",
            more = "更多",
            attachmentTitle = "添加附件",
            camera = "拍照",
            photos = "相册",
            files = "文件",
            attachmentPending = "图片已选择 · 可以发送",
            inputPlaceholder = "输入消息...",
            voiceInput = "语音输入",
            listening = "正在聆听...",
            voiceReleaseToFinish = "松开结束 · 滑开取消",
            voiceReleaseToCancel = "松开取消",
            voiceProcessing = "正在识别语音...",
            voiceProcessingTimeout = "识别超时，请重试",
            voicePermissionDenied = "麦克风权限被拒绝",
            voicePermissionGranted = "权限已开启，请再次长按说话",
            voiceCancelled = "已取消语音输入",
            voiceNoMatch = "没有识别到语音，请重试",
            voiceUnavailable = "当前系统语音引擎不兼容，请安装标准语音识别服务",
            voiceFailed = "语音识别失败，请重试",
            send = "发送",
            stopGenerating = "停止生成",
            today = "今天",
            emptySessions = "暂无历史会话",
            noSessionFound = "没有找到相关会话",
            renameSession = "重命名此对话",
            searchSessions = "搜索历史对话...",
            openNotes = "进入笔记库",
            pinned = "置顶",
            pin = "置顶",
            unpin = "取消置顶",
            rename = "重命名",
            delete = "删除",
            settings = "设置",
            messageTimePattern = "yyyy年M月d日 HH:mm",
            messageTimeLocale = Locale.CHINA,
            welcomePhrases = listOf(
                "Think it through. Keep what matters.",
                "把零散想法，沉淀成清晰笔记。",
                "Questions become conversations. Conversations become memory.",
                "慢慢说，Cogno 会帮你整理脉络。",
                "Catch the spark before it disappears."
            ),
            legacyWelcome = "把问题、灵感和碎片想法都放进来。\nCogno 会先帮你沉淀成对话，之后再整理成笔记。"
        )
    }
}

@Composable
fun ChatScreen(
    currentModelId: String,
    languagePreference: String,
    userName: String,
    avatarUri: String,
    onOpenNotes: () -> Unit,
    onOpenSettings: () -> Unit,
    initialSessionId: String? = null,
    onInitialSessionConsumed: () -> Unit = {},
    viewModel: ChatViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isDark = isCognoDarkTheme()
    val listState = rememberLazyListState()
    val background = if (isDark) CognoDarkBackground else CognoBackground
    val copy = chatScreenCopy(languagePreference)
    var attachmentMenuVisible by remember { mutableStateOf(false) }
    var pendingAttachment by remember { mutableStateOf<PendingAttachment?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    fun selectDocument(uri: Uri, isImage: Boolean) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        pendingAttachment = PendingAttachment(
            uri = uri,
            displayName = attachmentDisplayName(context, uri),
            isImage = isImage
        )
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { selectDocument(it, isImage = true) }
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { selectDocument(it, isImage = false) }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val cameraUri = pendingCameraUri
        if (saved && cameraUri != null) {
            pendingAttachment = PendingAttachment(
                uri = cameraUri,
                displayName = "Cogno_${System.currentTimeMillis()}.jpg",
                isImage = true
            )
        }
        pendingCameraUri = null
    }

    LaunchedEffect(uiState.messages.size, uiState.currentSessionId) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    LaunchedEffect(initialSessionId) {
        if (!initialSessionId.isNullOrBlank()) {
            viewModel.selectSession(initialSessionId)
            onInitialSessionConsumed()
        }
    }

    var noteStyleDialogVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ChatTopBar(
                isDark = isDark,
                modelId = currentModelId,
                copy = copy,
                onOpenDrawer = viewModel::openDrawer,
                noteGenerationEnabled = uiState.currentSessionId != null &&
                    uiState.messages.any { it.status == "completed" } &&
                    !uiState.isGeneratingNote,
                onGenerateNote = { noteStyleDialogVisible = true },
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
                        BrandWelcomeView(isDark = isDark, copy = copy)
                    } else {
                        MessageList(
                            messages = uiState.messages,
                            isDark = isDark,
                            copy = copy,
                            listState = listState,
                            onUpdateUserMessage = { messageId, content ->
                                viewModel.updateUserMessage(messageId, content, languagePreference)
                            },
                            onSetAssistantFeedback = viewModel::setAssistantFeedback,
                            onRegenerateAssistant = { message ->
                                viewModel.regenerateAssistantMessage(message, languagePreference)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                NoteGenerationToast(
                    toast = uiState.noteToast,
                    isDark = isDark,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
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
                attachment = pendingAttachment,
                languagePreference = languagePreference,
                onTextChange = viewModel::onInputChange,
                onSend = {
                    viewModel.sendMessage(
                        languagePreference,
                        pendingAttachment?.takeIf { it.isImage }?.uri
                    )
                    pendingAttachment = null
                },
                onStopGenerating = viewModel::stopGenerating,
                onAddAttachment = { attachmentMenuVisible = true },
                onRemoveAttachment = { pendingAttachment = null },
                copy = copy
            )
        }

        SidebarDrawer(
            isOpen = uiState.isDrawerOpen,
            sessions = uiState.sessions,
            currentSessionId = uiState.currentSessionId,
            isDark = isDark,
            copy = copy,
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
            },
            userName = userName,
            avatarUri = avatarUri
        )

        if (noteStyleDialogVisible) {
            NoteStyleDialog(
                isDark = isDark,
                copy = copy,
                onDismiss = { noteStyleDialogVisible = false },
                onSelect = { style ->
                    noteStyleDialogVisible = false
                    viewModel.generateNote(style, languagePreference)
                }
            )
        }

        if (attachmentMenuVisible) {
            AttachmentPickerDialog(
                isDark = isDark,
                copy = copy,
                onDismiss = { attachmentMenuVisible = false },
                onCamera = {
                    attachmentMenuVisible = false
                    val cameraDirectory = File(context.cacheDir, "camera").apply { mkdirs() }
                    val outputFile = File.createTempFile("cogno_", ".jpg", cameraDirectory)
                    val outputUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        outputFile
                    )
                    pendingCameraUri = outputUri
                    cameraLauncher.launch(outputUri)
                },
                onPhotos = {
                    attachmentMenuVisible = false
                    photoPicker.launch(arrayOf("image/*"))
                },
                onFiles = {
                    attachmentMenuVisible = false
                    filePicker.launch(arrayOf("*/*"))
                }
            )
        }
    }
}

private fun attachmentDisplayName(context: android.content.Context, uri: Uri): String {
    val resolver = context.contentResolver
    return resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
    } ?: uri.lastPathSegment?.substringAfterLast('/') ?: "附件"
}

@Composable
private fun ChatTopBar(
    isDark: Boolean,
    modelId: String,
    copy: ChatScreenCopy,
    onOpenDrawer: () -> Unit,
    noteGenerationEnabled: Boolean,
    onGenerateNote: () -> Unit,
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
                    painter = painterResource(R.drawable.dehaze_24px),
                    contentDescription = copy.openSidebar,
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
                text = modelId.ifBlank { "Model" },
                color = if (isDark) CognoDarkPrimary else CognoPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                lineHeight = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(start = 7.dp)
                    .widthIn(max = 130.dp)
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
            IconButton(
                onClick = onGenerateNote,
                enabled = noteGenerationEnabled,
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.wand_shine_24px),
                    contentDescription = copy.generateNote,
                    tint = if (noteGenerationEnabled) {
                        if (isDark) CognoDarkPrimary else CognoPrimary
                    } else {
                        CognoMuted.copy(alpha = 0.55f)
                    },
                    modifier = Modifier.size(21.dp)
                )
            }
            IconButton(onClick = onNewSession, modifier = Modifier.size(42.dp)) {
                Icon(
                    painter = painterResource(R.drawable.maps_ugc_24px),
                    contentDescription = copy.newSession,
                    tint = if (isDark) CognoDarkPrimary else CognoPrimary,
                    modifier = Modifier.size(21.dp)
                )
            }
        }
    }
}

@Composable
private fun NoteGenerationToast(
    toast: NoteToast?,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = toast != null,
        enter = fadeIn(animationSpec = tween(140)),
        exit = fadeOut(animationSpec = tween(180)),
        modifier = modifier
            .padding(top = 8.dp)
    ) {
        Surface(
            color = if (isDark) CognoDarkSurface else Color.White,
            shape = RoundedCornerShape(14.dp),
            shadowElevation = 14.dp,
            modifier = Modifier
                .widthIn(min = 150.dp, max = 260.dp)
                .border(
                    width = 1.dp,
                    color = if (isDark) CognoDarkLine else CognoLine,
                    shape = RoundedCornerShape(14.dp)
                )
        ) {
            Text(
                text = toast?.message.orEmpty(),
                color = if (toast?.isError == true) Color(0xFFE05650) else if (isDark) CognoDarkText else CognoText,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteStyleDialog(
    isDark: Boolean,
    copy: ChatScreenCopy,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            color = if (isDark) CognoDarkSurface else Color.White,
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 18.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .border(
                    width = 1.dp,
                    color = if (isDark) CognoDarkLine else CognoLine,
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = copy.noteStyleTitle,
                    color = if (isDark) CognoDarkText else CognoText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                NoteStyleAction(copy.conciseTitle, copy.conciseDescription, isDark) {
                    onSelect(copy.concisePrompt)
                }
                NoteStyleAction(copy.standardTitle, copy.standardDescription, isDark) {
                    onSelect(copy.standardPrompt)
                }
                NoteStyleAction(copy.detailedTitle, copy.detailedDescription, isDark) {
                    onSelect(copy.detailedPrompt)
                }
            }
        }
    }
}

@Composable
private fun NoteStyleAction(
    title: String,
    description: String,
    isDark: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 11.dp)
    ) {
        Text(
            text = title,
            color = if (isDark) CognoDarkText else CognoText,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = description,
            color = CognoMuted,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttachmentPickerDialog(
    isDark: Boolean,
    copy: ChatScreenCopy,
    onDismiss: () -> Unit,
    onCamera: () -> Unit,
    onPhotos: () -> Unit,
    onFiles: () -> Unit
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
                    text = copy.attachmentTitle,
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
                        .clip(RoundedCornerShape(15.dp))
                        .border(
                            width = 1.dp,
                            color = if (isDark) CognoDarkLine else CognoLine,
                            shape = RoundedCornerShape(15.dp)
                        )
                ) {
                    AttachmentPickerAction(Icons.Default.CameraAlt, copy.camera, isDark, onCamera)
                    AttachmentDivider(isDark)
                    AttachmentPickerAction(Icons.Default.PhotoLibrary, copy.photos, isDark, onPhotos)
                    AttachmentDivider(isDark)
                    AttachmentPickerAction(Icons.AutoMirrored.Filled.InsertDriveFile, copy.files, isDark, onFiles)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = copy.cancel,
                    color = if (isDark) CognoDarkPrimary else CognoPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 11.dp)
                )
            }
        }
    }
}

@Composable
private fun AttachmentPickerAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isDark: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(
                    (if (isDark) CognoDarkPrimary else CognoPrimary).copy(alpha = 0.12f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDark) CognoDarkPrimary else CognoPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = if (isDark) CognoDarkText else CognoText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun AttachmentDivider(isDark: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(if (isDark) CognoDarkLine else CognoLine)
    )
}

@Composable
private fun BrandWelcomeView(isDark: Boolean, copy: ChatScreenCopy) {
    val phrases = copy.welcomePhrases
    val phrase = remember(phrases) { phrases.random() }
    var typedText by remember { mutableStateOf("") }

    LaunchedEffect(phrase) {
        typedText = ""
        phrase.forEachIndexed { index, _ ->
            delay(42)
            typedText = phrase.take(index + 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CognoMark(isDark = isDark, modifier = Modifier.size(112.dp))
        Spacer(modifier = Modifier.height(30.dp))
        Text(
            text = typedText,
            color = if (isDark) CognoDarkText.copy(alpha = 0.88f) else CognoText.copy(alpha = 0.86f),
            fontSize = 25.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            letterSpacing = 0.sp,
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CognoMark(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val primary = if (isDark) CognoDarkPrimary else CognoPrimary
    val ink = if (isDark) CognoDarkText else CognoText
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = androidx.compose.ui.geometry.Offset(w / 2f, h / 2f)
        drawCircle(
            color = primary.copy(alpha = if (isDark) 0.16f else 0.12f),
            radius = w * 0.46f,
            center = center
        )
        drawCircle(
            color = primary.copy(alpha = 0.30f),
            radius = w * 0.34f,
            center = center,
            style = Stroke(width = w * 0.035f)
        )
        drawCircle(
            color = ink.copy(alpha = if (isDark) 0.28f else 0.18f),
            radius = w * 0.22f,
            center = center,
            style = Stroke(width = w * 0.018f)
        )
        val gem = Path().apply {
            moveTo(w * 0.50f, h * 0.23f)
            cubicTo(w * 0.70f, h * 0.34f, w * 0.78f, h * 0.50f, w * 0.50f, h * 0.77f)
            cubicTo(w * 0.22f, h * 0.50f, w * 0.30f, h * 0.34f, w * 0.50f, h * 0.23f)
            close()
        }
        drawPath(gem, color = primary)
        drawCircle(
            color = Color.White.copy(alpha = if (isDark) 0.86f else 0.96f),
            radius = w * 0.055f,
            center = androidx.compose.ui.geometry.Offset(w * 0.44f, h * 0.47f)
        )
        drawCircle(
            color = Color.White.copy(alpha = if (isDark) 0.72f else 0.86f),
            radius = w * 0.04f,
            center = androidx.compose.ui.geometry.Offset(w * 0.60f, h * 0.54f)
        )
    }
}

@Composable
private fun WelcomeView(isDark: Boolean, copy: ChatScreenCopy) {
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
            text = copy.legacyWelcome,
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
    copy: ChatScreenCopy,
    listState: LazyListState,
    onUpdateUserMessage: (String, String) -> Unit,
    onSetAssistantFeedback: (String, String?) -> Unit,
    onRegenerateAssistant: (MessageEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var editTarget by remember { mutableStateOf<MessageEntity?>(null) }
    var editText by remember { mutableStateOf("") }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(top = 24.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        items(messages, key = { it.id }) { message ->
            MessageBubble(
                message = message,
                isDark = isDark,
                copy = copy,
                onEdit = {
                    editTarget = message
                    editText = message.content
                },
                onSetFeedback = { feedback -> onSetAssistantFeedback(message.id, feedback) },
                onRegenerate = { onRegenerateAssistant(message) }
            )
        }
    }

    editTarget?.let { message ->
        MessageEditDialog(
            value = editText,
            isDark = isDark,
            copy = copy,
            onValueChange = { editText = it },
            onDismiss = { editTarget = null },
            onConfirm = {
                onUpdateUserMessage(message.id, editText)
                editTarget = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: MessageEntity,
    isDark: Boolean,
    copy: ChatScreenCopy,
    onEdit: () -> Unit,
    onSetFeedback: (String?) -> Unit,
    onRegenerate: () -> Unit
) {
    val isUser = message.role == "user"
    val clipboardManager = LocalClipboardManager.current
    var menuExpanded by remember { mutableStateOf(false) }
    val displayText = when {
        message.status == "pending" || (message.status == "streaming" && message.content.isBlank()) -> copy.thinking
        message.content.isBlank() -> copy.emptyContent
        else -> message.content
    }
    val textColor = when {
        message.status == "failed" -> Color(0xFFD94841)
        isDark -> CognoDarkText
        else -> CognoText
    }
    Box(modifier = Modifier.fillMaxWidth()) {
        if (isUser) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    color = if (isDark) CognoDarkUserBubble else CognoUserBubble,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .widthIn(max = 310.dp)
                        .combinedClickable(
                            onClick = { },
                            onLongClick = { menuExpanded = true }
                        )
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
                    Column(modifier = Modifier.padding(6.dp)) {
                        message.imagePath?.let { imagePath ->
                            ChatMessageImage(
                                imagePath = imagePath,
                                modifier = Modifier
                                    .widthIn(max = 298.dp)
                                    .height(210.dp)
                            )
                        }
                        if (message.content.isNotBlank()) {
                            Text(
                                text = message.content,
                                color = textColor,
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                modifier = Modifier.padding(
                                    start = 10.dp,
                                    end = 10.dp,
                                    top = if (message.imagePath == null) 6.dp else 8.dp,
                                    bottom = 6.dp
                                )
                            )
                        }
                    }
                }
            }
        } else {
            // AI replies use a full-width reading block so long text, lists and code stay comfortable.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                BasicMarkdown(
                    content = displayText,
                    isDark = isDark,
                    textColor = textColor,
                    modifier = Modifier.fillMaxWidth()
                )
                if (message.status == "completed" || message.status == "failed") {
                    AssistantInlineActions(
                        feedback = message.feedback,
                        isDark = isDark,
                        copy = copy,
                        onCopy = { clipboardManager.setText(AnnotatedString(message.content)) },
                        onLike = { onSetFeedback(if (message.feedback == FEEDBACK_LIKE) null else FEEDBACK_LIKE) },
                        onDislike = { onSetFeedback(if (message.feedback == FEEDBACK_DISLIKE) null else FEEDBACK_DISLIKE) },
                        onRegenerate = onRegenerate
                    )
                }
            }
        }

        if (menuExpanded) {
            if (isUser) {
                UserMessageMenu(
                    message = message,
                    isDark = isDark,
                    copy = copy,
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(message.content))
                        menuExpanded = false
                    },
                    onEdit = {
                        menuExpanded = false
                        onEdit()
                    },
                    onDismiss = { menuExpanded = false }
                )
            }
        }
    }
}

@Composable
private fun ChatMessageImage(
    imagePath: String,
    modifier: Modifier = Modifier
) {
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, imagePath) {
        value = withContext(Dispatchers.IO) {
            BitmapFactory.decodeFile(imagePath)
        }
    }

    bitmap?.let { loadedBitmap ->
        Image(
            bitmap = loadedBitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(RoundedCornerShape(15.dp))
        )
    }
}

@Composable
private fun AssistantInlineActions(
    feedback: String?,
    isDark: Boolean,
    copy: ChatScreenCopy,
    onCopy: () -> Unit,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onRegenerate: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        InlineActionIcon(Icons.Default.ContentCopy, copy.copy, isDark, selected = false, onClick = onCopy)
        InlineActionIcon(Icons.Default.ThumbUp, copy.like, isDark, selected = feedback == FEEDBACK_LIKE, onClick = onLike)
        InlineActionIcon(Icons.Default.ThumbDown, copy.dislike, isDark, selected = feedback == FEEDBACK_DISLIKE, onClick = onDislike)
        InlineActionIcon(Icons.Default.Refresh, copy.regenerate, isDark, selected = false, onClick = onRegenerate)
    }
}

@Composable
private fun InlineActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    isDark: Boolean,
    selected: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(34.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (selected) {
                if (isDark) CognoDarkPrimary else CognoPrimary
            } else {
                CognoMuted
            },
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun UserMessageMenu(
    message: MessageEntity,
    isDark: Boolean,
    copy: ChatScreenCopy,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit
) {
    Popup(
        alignment = Alignment.Center,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Surface(
            color = if (isDark) CognoDarkSurface else Color.White,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 8.dp,
            modifier = Modifier
                .width(150.dp)
                .border(
                    width = 1.dp,
                    color = if (isDark) CognoDarkLine else CognoLine,
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Column {
                Text(
                    text = formatMessageTime(message.createdAt, copy),
                    color = CognoMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
                ContextMenuAction(copy.copy, Icons.Default.ContentCopy, isDark, onCopy)
                ContextMenuAction(copy.edit, Icons.Default.Edit, isDark, onEdit, isLast = true)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageEditDialog(
    value: String,
    isDark: Boolean,
    copy: ChatScreenCopy,
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
                    text = copy.editMessage,
                    color = if (isDark) CognoDarkText else CognoText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    minLines = 3,
                    maxLines = 6,
                    textStyle = TextStyle(
                        color = if (isDark) CognoDarkText else CognoText,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
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
                            .clickable(enabled = value.isNotBlank(), onClick = onConfirm)
                            .padding(vertical = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    text: String,
    isDark: Boolean,
    isSending: Boolean,
    attachment: PendingAttachment?,
    languagePreference: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onStopGenerating: () -> Unit,
    onAddAttachment: () -> Unit,
    onRemoveAttachment: () -> Unit,
    copy: ChatScreenCopy
) {
    val context = LocalContext.current
    val voiceCancelDistancePx = with(LocalDensity.current) { 72.dp.toPx() }
    val latestText by rememberUpdatedState(text)
    val latestOnTextChange by rememberUpdatedState(onTextChange)
    var inputValue by remember {
        mutableStateOf(TextFieldValue(text = text, selection = TextRange(text.length)))
    }
    var voiceState by remember { mutableStateOf(VoiceInputState()) }
    var cancellationRequested by remember { mutableStateOf(false) }
    var latestPartialResult by remember { mutableStateOf("") }
    val speechRecognizer = remember(context, copy) {
        if (
            !SpeechRecognizer.isRecognitionAvailable(context) ||
            !hasUsableRecognitionService(context)
        ) {
            null
        } else {
            runCatching {
                AndroidSpeechRecognizer(
                    context = context,
                    listener = object : AndroidSpeechRecognizer.Listener {
                    override fun onReady() {
                        voiceState = VoiceInputState(
                            phase = VoiceInputPhase.LISTENING,
                            detail = copy.voiceReleaseToFinish
                        )
                    }

                    override fun onSpeechStarted() {
                        voiceState = VoiceInputState(
                            phase = VoiceInputPhase.LISTENING,
                            detail = copy.voiceReleaseToFinish
                        )
                    }

                    override fun onProcessing() {
                        voiceState = VoiceInputState(
                            phase = VoiceInputPhase.PROCESSING,
                            detail = copy.voiceProcessing
                        )
                    }

                    override fun onPartialResult(text: String) {
                        if (text.isNotBlank()) {
                            latestPartialResult = text
                            if (voiceState.phase != VoiceInputPhase.PROCESSING) {
                                voiceState = VoiceInputState(
                                    phase = VoiceInputPhase.LISTENING,
                                    detail = text
                                )
                            }
                        }
                    }

                    override fun onResult(text: String) {
                        cancellationRequested = false
                        latestPartialResult = ""
                        if (text.isBlank()) {
                            voiceState = VoiceInputState(
                                phase = VoiceInputPhase.MESSAGE,
                                detail = copy.voiceNoMatch
                            )
                        } else {
                            latestOnTextChange(appendRecognizedText(latestText, text))
                            voiceState = VoiceInputState()
                        }
                    }

                    override fun onError(error: Int) {
                        if (cancellationRequested) {
                            cancellationRequested = false
                            return
                        }
                        latestPartialResult = ""
                        voiceState = VoiceInputState(
                            phase = VoiceInputPhase.MESSAGE,
                            detail = voiceErrorMessage(error, copy)
                        )
                    }
                    }
                )
            }.getOrNull()
        }
    }
    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        voiceState = VoiceInputState(
            phase = VoiceInputPhase.MESSAGE,
            detail = if (granted) copy.voicePermissionGranted else copy.voicePermissionDenied
        )
    }
    DisposableEffect(speechRecognizer) {
        onDispose {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        }
    }

    LaunchedEffect(text) {
        if (text != inputValue.text) {
            inputValue = TextFieldValue(
                text = text,
                selection = TextRange(text.length)
            )
        }
    }

    LaunchedEffect(voiceState) {
        if (voiceState.phase == VoiceInputPhase.MESSAGE) {
            delay(2200)
            if (voiceState.phase == VoiceInputPhase.MESSAGE) {
                voiceState = VoiceInputState()
            }
        }
    }

    LaunchedEffect(voiceState.phase) {
        if (voiceState.phase == VoiceInputPhase.PROCESSING) {
            delay(8000)
            if (voiceState.phase == VoiceInputPhase.PROCESSING) {
                cancellationRequested = true
                speechRecognizer?.cancel()
                val partialResult = latestPartialResult.trim()
                latestPartialResult = ""
                if (partialResult.isNotBlank()) {
                    latestOnTextChange(appendRecognizedText(latestText, partialResult))
                    voiceState = VoiceInputState()
                } else {
                    voiceState = VoiceInputState(
                        phase = VoiceInputPhase.MESSAGE,
                        detail = copy.voiceProcessingTimeout
                    )
                }
            }
        }
    }

    fun startVoiceInput(): Boolean {
        if (
            voiceState.phase == VoiceInputPhase.LISTENING ||
            voiceState.phase == VoiceInputPhase.PROCESSING
        ) {
            return false
        }
        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return false
        }
        if (speechRecognizer == null) {
            voiceState = VoiceInputState(
                phase = VoiceInputPhase.MESSAGE,
                detail = copy.voiceUnavailable
            )
            return false
        }

        cancellationRequested = false
        latestPartialResult = ""
        voiceState = VoiceInputState(
            phase = VoiceInputPhase.LISTENING,
            detail = copy.voiceReleaseToFinish
        )
        return runCatching {
            speechRecognizer.start(speechRecognitionLanguageTag(languagePreference))
        }.onFailure {
            voiceState = VoiceInputState(
                phase = VoiceInputPhase.MESSAGE,
                detail = copy.voiceFailed
            )
        }.isSuccess
    }

    fun finishVoiceInput() {
        if (voiceState.phase != VoiceInputPhase.LISTENING) return
        voiceState = VoiceInputState(
            phase = VoiceInputPhase.PROCESSING,
            detail = copy.voiceProcessing
        )
        speechRecognizer?.stop()
    }

    fun cancelVoiceInput() {
        cancellationRequested = true
        latestPartialResult = ""
        speechRecognizer?.cancel()
        voiceState = VoiceInputState(
            phase = VoiceInputPhase.MESSAGE,
            detail = copy.voiceCancelled
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 8.dp)
    ) {
        AnimatedVisibility(
            visible = voiceState.phase != VoiceInputPhase.IDLE,
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(140)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-112).dp)
        ) {
            VoiceListeningBubble(
                isDark = isDark,
                state = voiceState,
                copy = copy,
                onCancel = ::cancelVoiceInput
            )
        }

        Column(
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
        ) {
            if (attachment != null) {
                PendingAttachmentRow(
                    attachment = attachment,
                    message = copy.attachmentPending,
                    removeDescription = copy.cancel,
                    isDark = isDark,
                    onRemove = onRemoveAttachment
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onAddAttachment, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = copy.more,
                        tint = CognoMuted,
                        modifier = Modifier.size(21.dp)
                    )
                }
                BasicTextField(
                    value = inputValue,
                    onValueChange = { newValue ->
                        inputValue = newValue
                        onTextChange(newValue.text)
                    },
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
                                text = copy.inputPlaceholder,
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
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .semantics {
                                role = Role.Button
                                contentDescription = copy.voiceInput
                                onClick(label = copy.voiceInput) {
                                    if (voiceState.phase == VoiceInputPhase.LISTENING) {
                                        finishVoiceInput()
                                    } else {
                                        startVoiceInput()
                                    }
                                    true
                                }
                            }
                            .pointerInput(speechRecognizer, languagePreference) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val longPress = awaitLongPressOrCancellation(down.id)
                                    if (longPress != null) {
                                        if (startVoiceInput()) {
                                                    var shouldCancel = false
                                                    var gestureEnded = false
                                                    while (!gestureEnded) {
                                                        val change = awaitPointerEvent()
                                                            .changes
                                                            .firstOrNull { it.id == down.id }
                                                        if (change == null) {
                                                            shouldCancel = true
                                                            gestureEnded = true
                                                        } else if (!change.pressed) {
                                                            gestureEnded = true
                                                        } else {
                                                            val movedAway = (
                                                                change.position - down.position
                                                                ).getDistance() >=
                                                                voiceCancelDistancePx
                                                            if (movedAway != shouldCancel) {
                                                                shouldCancel = movedAway
                                                                voiceState = VoiceInputState(
                                                                    phase = VoiceInputPhase.LISTENING,
                                                                    detail = if (shouldCancel) {
                                                                        copy.voiceReleaseToCancel
                                                                    } else {
                                                                        copy.voiceReleaseToFinish
                                                                    }
                                                                )
                                                            }
                                                        }
                                                    }
                                                    if (!shouldCancel) {
                                                        finishVoiceInput()
                                                    } else {
                                                        cancelVoiceInput()
                                                    }
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = if (
                                voiceState.phase == VoiceInputPhase.LISTENING ||
                                voiceState.phase == VoiceInputPhase.PROCESSING
                            ) {
                                if (isDark) CognoDarkPrimary else CognoPrimary
                            } else if (isDark) {
                                CognoDarkText.copy(alpha = 0.8f)
                            } else {
                                CognoText.copy(alpha = 0.8f)
                            },
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    FilledIconButton(
                        onClick = if (isSending) onStopGenerating else onSend,
                        enabled = isSending ||
                            (text.isNotBlank() && attachment == null) ||
                            attachment?.isImage == true,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isDark) CognoDarkPrimary else CognoPrimary,
                            disabledContainerColor = CognoMuted.copy(alpha = 0.35f),
                            contentColor = Color.White,
                            disabledContentColor = Color.White.copy(alpha = 0.92f)
                        ),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector =
                                if (isSending) Icons.Default.Close else Icons.Default.ArrowUpward,
                            contentDescription =
                                if (isSending) copy.stopGenerating else copy.send,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingAttachmentRow(
    attachment: PendingAttachment,
    message: String,
    removeDescription: String,
    isDark: Boolean,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 2.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                (if (isDark) CognoDarkPrimary else CognoPrimary).copy(alpha = 0.09f)
            )
            .padding(start = 11.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (attachment.isImage) {
                Icons.Default.PhotoLibrary
            } else {
                Icons.AutoMirrored.Filled.InsertDriveFile
            },
            contentDescription = null,
            tint = if (isDark) CognoDarkPrimary else CognoPrimary,
            modifier = Modifier.size(19.dp)
        )
        Spacer(modifier = Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = attachment.displayName,
                color = if (isDark) CognoDarkText else CognoText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = message,
                color = CognoMuted,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = removeDescription,
                tint = CognoMuted,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

@Composable
private fun VoiceListeningBubble(
    isDark: Boolean,
    state: VoiceInputState,
    copy: ChatScreenCopy,
    onCancel: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "voice-listening")
    val pulse by transition.animateFloat(
        initialValue = 0.86f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(720),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voice-pulse"
    )

    Surface(
        color = if (isDark) CognoDarkSurface else Color.White,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 16.dp,
        modifier = Modifier.border(
            width = 1.dp,
            color = if (isDark) CognoDarkLine else CognoLine,
            shape = RoundedCornerShape(20.dp)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .scale(pulse)
                    .clip(CircleShape)
                    .background(
                        (if (isDark) CognoDarkPrimary else CognoPrimary).copy(alpha = 0.16f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = if (isDark) CognoDarkPrimary else CognoPrimary,
                    modifier = Modifier.size(23.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = when (state.phase) {
                        VoiceInputPhase.LISTENING -> copy.listening
                        VoiceInputPhase.PROCESSING -> copy.voiceProcessing
                        VoiceInputPhase.MESSAGE -> copy.voiceInput
                        VoiceInputPhase.IDLE -> copy.voiceInput
                    },
                    color = if (isDark) CognoDarkText else CognoText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = state.detail,
                    color = CognoMuted,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onCancel,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = copy.cancel,
                    tint = CognoMuted,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

private fun appendRecognizedText(currentText: String, recognizedText: String): String {
    val recognized = recognizedText.trim()
    if (currentText.isBlank()) return recognized
    if (recognized.isBlank()) return currentText
    return "${currentText.trimEnd()} $recognized"
}

private fun voiceErrorMessage(error: Int, copy: ChatScreenCopy): String {
    return when (error) {
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> copy.voicePermissionDenied
        SpeechRecognizer.ERROR_NO_MATCH,
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> copy.voiceNoMatch
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> copy.voiceUnavailable
        else -> copy.voiceFailed
    }
}

@Composable
private fun SidebarDrawer(
    isOpen: Boolean,
    sessions: List<SessionEntity>,
    currentSessionId: String?,
    isDark: Boolean,
    copy: ChatScreenCopy,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    onSelectSession: (String) -> Unit,
    onRenameSession: (String, String) -> Unit,
    onTogglePinSession: (SessionEntity) -> Unit,
    onDeleteSession: (String) -> Unit,
    onOpenNotes: () -> Unit,
    onOpenSettings: () -> Unit,
    userName: String,
    avatarUri: String
) {
    val scope = rememberCoroutineScope()
    val drawerProgress = remember { Animatable(if (isOpen) 1f else 0f) }
    var renameTarget by remember { mutableStateOf<SessionEntity?>(null) }
    var renameText by remember { mutableStateOf("") }
    var sessionKeyword by remember { mutableStateOf("") }
    val visibleSessions = remember(sessions, sessionKeyword) {
        val keyword = sessionKeyword.trim()
        if (keyword.isBlank()) {
            sessions
        } else {
            sessions.filter { session ->
                session.title.contains(keyword, ignoreCase = true) ||
                    session.lastMessagePreview.orEmpty().contains(keyword, ignoreCase = true)
            }
        }
    }

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
                    NoteEntry(isDark = isDark, copy = copy, onClick = onOpenNotes)
                    SidebarSearchField(
                        keyword = sessionKeyword,
                        isDark = isDark,
                        copy = copy,
                        onKeywordChange = { sessionKeyword = it }
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Text(
                        text = copy.today,
                        color = CognoMuted,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                    )
                    if (visibleSessions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (sessionKeyword.isBlank()) copy.emptySessions else copy.noSessionFound,
                                color = CognoMuted,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(visibleSessions, key = { it.id }) { session ->
                                SessionRow(
                                    session = session,
                                    selected = session.id == currentSessionId,
                                    isDark = isDark,
                                    copy = copy,
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
                SidebarFooter(
                    userName = userName,
                    avatarUri = avatarUri,
                    isDark = isDark,
                    copy = copy,
                    onOpenSettings = onOpenSettings
                )
            }
        }

        renameTarget?.let { session ->
            RenameDialog(
                title = copy.renameSession,
                value = renameText,
                isDark = isDark,
                copy = copy,
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
private fun SidebarSearchField(
    keyword: String,
    isDark: Boolean,
    copy: ChatScreenCopy,
    onKeywordChange: (String) -> Unit
) {
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
            onValueChange = onKeywordChange,
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
                    Text(copy.searchSessions, color = CognoMuted, fontSize = 14.sp)
                }
                innerTextField()
            }
        )
    }
}

@Composable
private fun NoteEntry(isDark: Boolean, copy: ChatScreenCopy, onClick: () -> Unit) {
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
            text = copy.openNotes,
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
    copy: ChatScreenCopy,
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
                    contentDescription = copy.pinned,
                    tint = if (isDark) CognoDarkPrimary else CognoPrimary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        if (menuExpanded) {
            WebStyleContextMenu(
                isDark = isDark,
                pinText = if (session.pinned) copy.unpin else copy.pin,
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
private fun WebStyleContextMenu(
    isDark: Boolean,
    pinText: String,
    copy: ChatScreenCopy,
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
                .padding(top = 8.dp, end = 14.dp)
                .width(174.dp)
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

private fun formatMessageTime(timestamp: Long, copy: ChatScreenCopy): String {
    return SimpleDateFormat(copy.messageTimePattern, copy.messageTimeLocale).format(Date(timestamp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RenameDialog(
    title: String,
    value: String,
    isDark: Boolean,
    copy: ChatScreenCopy,
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

@Composable
private fun SidebarFooter(
    userName: String,
    avatarUri: String,
    isDark: Boolean,
    copy: ChatScreenCopy,
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onOpenSettings)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(userName = userName, avatarUri = avatarUri, size = 38, isDark = isDark)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = userName,
                color = if (isDark) CognoDarkText else CognoText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.MoreHoriz,
                contentDescription = copy.settings,
                tint = CognoMuted,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
