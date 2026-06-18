package com.ntoprevd.cogno.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ntoprevd.cogno.data.db.entity.TopicEntity
import com.ntoprevd.cogno.data.repository.TopicRepository
import com.ntoprevd.cogno.data.settings.AppLanguagePreference
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
fun TopicSettingsScreen(
    languagePreference: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember(context) { TopicRepository(context) }
    val scope = rememberCoroutineScope()
    val topics by repository.observeTopics().collectAsStateWithLifecycle(initialValue = emptyList())
    val isDark = isCognoDarkTheme()
    val isEnglish = languagePreference == AppLanguagePreference.EN
    var editing by remember { mutableStateOf<TopicEntity?>(null) }
    var adding by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<TopicEntity?>(null) }
    var resetConfirmationVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { repository.ensureDefaultTopics() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) CognoDarkBackground else CognoBackground)
    ) {
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
                    Icons.Default.ArrowBackIosNew,
                    contentDescription = null,
                    tint = if (isDark) CognoDarkText else CognoText
                )
            }
            Text(
                text = if (isEnglish) "Topic Rules" else "主题规则",
                color = if (isDark) CognoDarkText else CognoText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { resetConfirmationVisible = true }) {
                Icon(
                    Icons.Default.RestartAlt,
                    contentDescription = if (isEnglish) "Reset" else "恢复默认",
                    tint = if (isDark) CognoDarkPrimary else CognoPrimary
                )
            }
            IconButton(onClick = { adding = true }) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = if (isEnglish) "Add" else "新增",
                    tint = if (isDark) CognoDarkPrimary else CognoPrimary
                )
            }
        }
        Text(
            text = if (isEnglish) {
                "Rule changes apply to newly generated topic units. Existing note content is not reprocessed."
            } else {
                "规则修改只影响以后新生成的主题单元，已有笔记正文不会重新处理。"
            },
            color = CognoMuted,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(topics, key = { it.id }) { topic ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isDark) CognoDarkSurface else Color.White)
                        .clickable { editing = topic }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            topic.name,
                            color = if (isDark) CognoDarkText else CognoText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            topic.keywords.ifBlank {
                                if (isEnglish) "Fallback topic" else "兜底主题"
                            },
                            color = CognoMuted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { pendingDelete = topic }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFFE05650)
                        )
                    }
                }
            }
        }
    }

    if (adding || editing != null) {
        TopicEditDialog(
            topic = editing,
            isDark = isDark,
            isEnglish = isEnglish,
            onDismiss = {
                adding = false
                editing = null
            },
            onSave = { name, keywords ->
                scope.launch {
                    if (editing == null) {
                        repository.addTopic(name, keywords)
                    } else {
                        repository.renameTopic(editing!!, name, keywords)
                    }
                }
                adding = false
                editing = null
            }
        )
    }
    if (resetConfirmationVisible) {
        TopicActionConfirmDialog(
            isDark = isDark,
            title = if (isEnglish) "Reset topic rules?" else "重置主题规则？",
            message = if (isEnglish) {
                "This restores the initial topic categories. Notes that have already been generated will not be affected. Continue?"
            } else {
                "此操作会将主题类别恢复为初始类别，不影响已经生成的笔记。是否继续？"
            },
            cancelText = if (isEnglish) "Cancel" else "取消",
            confirmText = if (isEnglish) "Reset" else "继续重置",
            onDismiss = { resetConfirmationVisible = false },
            onConfirm = {
                resetConfirmationVisible = false
                scope.launch { repository.resetTopics() }
            }
        )
    }
    pendingDelete?.let { topic ->
        TopicActionConfirmDialog(
            isDark = isDark,
            title = if (isEnglish) "Delete topic rule?" else "删除主题规则？",
            message = if (isEnglish) {
                "Delete \"${topic.name}\"? Generated notes will not be affected, but this action cannot be undone."
            } else {
                "确定删除“${topic.name}”吗？已生成的笔记不会受到影响，此操作不可撤销。"
            },
            cancelText = if (isEnglish) "Cancel" else "取消",
            confirmText = if (isEnglish) "Delete" else "删除",
            destructive = true,
            onDismiss = { pendingDelete = null },
            onConfirm = {
                pendingDelete = null
                scope.launch { repository.deleteTopic(topic) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopicActionConfirmDialog(
    isDark: Boolean,
    title: String,
    message: String,
    cancelText: String,
    confirmText: String,
    destructive: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            color = if (isDark) CognoDarkSurface else Color.White,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .border(1.dp, if (isDark) CognoDarkLine else CognoLine, RoundedCornerShape(22.dp))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = title,
                    color = if (isDark) CognoDarkText else CognoText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = message,
                    color = CognoMuted,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = cancelText,
                        color = if (isDark) CognoDarkText else CognoText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onDismiss)
                            .padding(vertical = 12.dp)
                    )
                    Text(
                        text = confirmText,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (destructive) Color(0xFFE05650) else if (isDark) CognoDarkPrimary else CognoPrimary)
                            .clickable(onClick = onConfirm)
                            .padding(vertical = 12.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopicEditDialog(
    topic: TopicEntity?,
    isDark: Boolean,
    isEnglish: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember(topic?.id) { mutableStateOf(topic?.name.orEmpty()) }
    var keywords by remember(topic?.id) { mutableStateOf(topic?.keywords.orEmpty()) }
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            color = if (isDark) CognoDarkSurface else Color.White,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .border(1.dp, if (isDark) CognoDarkLine else CognoLine, RoundedCornerShape(22.dp))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = if (topic == null) {
                        if (isEnglish) "Add Topic" else "新增主题"
                    } else {
                        if (isEnglish) "Edit Topic" else "编辑主题"
                    },
                    color = if (isDark) CognoDarkText else CognoText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))
                TopicInput(name, if (isEnglish) "Topic name" else "主题名称", isDark) { name = it }
                Spacer(modifier = Modifier.height(10.dp))
                TopicInput(
                    keywords,
                    if (isEnglish) "Keywords, separated by commas" else "关键词，用逗号分隔",
                    isDark
                ) { keywords = it }
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isEnglish) "Cancel" else "取消",
                        color = if (isDark) CognoDarkText else CognoText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onDismiss)
                            .padding(vertical = 12.dp)
                    )
                    Text(
                        text = if (isEnglish) "Save" else "保存",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) CognoDarkPrimary else CognoPrimary)
                            .clickable { onSave(name, keywords) }
                            .padding(vertical = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TopicInput(
    value: String,
    placeholder: String,
    isDark: Boolean,
    onValueChange: (String) -> Unit
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(
            color = if (isDark) CognoDarkText else CognoText,
            fontSize = 14.sp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDark) CognoDarkBackground else CognoBackground)
            .padding(13.dp),
        decorationBox = { inner ->
            if (value.isBlank()) Text(placeholder, color = CognoMuted, fontSize = 13.sp)
            inner()
        }
    )
}
