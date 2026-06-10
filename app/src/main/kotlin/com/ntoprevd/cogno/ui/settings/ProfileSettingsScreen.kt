package com.ntoprevd.cogno.ui.settings

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ntoprevd.cogno.data.settings.AppLanguagePreference
import com.ntoprevd.cogno.data.settings.AppSettingsStore
import com.ntoprevd.cogno.ui.theme.CognoBackground
import com.ntoprevd.cogno.ui.theme.CognoDarkBackground
import com.ntoprevd.cogno.ui.theme.CognoDarkPrimary
import com.ntoprevd.cogno.ui.theme.CognoDarkSurface
import com.ntoprevd.cogno.ui.theme.CognoDarkText
import com.ntoprevd.cogno.ui.theme.CognoMuted
import com.ntoprevd.cogno.ui.theme.CognoPrimary
import com.ntoprevd.cogno.ui.theme.CognoText
import com.ntoprevd.cogno.ui.theme.isCognoDarkTheme

@Composable
fun ProfileSettingsScreen(
    languagePreference: String,
    initialUserName: String,
    initialAvatarUri: String,
    onSaved: (String, String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val store = remember(context) { AppSettingsStore(context) }
    val isDark = isCognoDarkTheme()
    val isEnglish = languagePreference == AppLanguagePreference.EN
    var userName by remember { mutableStateOf(initialUserName) }
    var avatarUri by remember { mutableStateOf(initialAvatarUri) }
    var pendingAvatarUri by remember { mutableStateOf("") }
    var avatarConfirmVisible by remember { mutableStateOf(false) }

    fun saveName() {
        val normalizedName = userName.trim().ifBlank { AppSettingsStore.DEFAULT_USER_NAME }
        store.saveUserName(normalizedName)
        userName = normalizedName
        onSaved(normalizedName, avatarUri)
    }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            pendingAvatarUri = uri.toString()
            avatarConfirmVisible = true
        }
    }

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
                    contentDescription = if (isEnglish) "Back" else "返回",
                    tint = if (isDark) CognoDarkText else CognoText
                )
            }
            Text(
                text = if (isEnglish) "Profile" else "用户信息",
                color = if (isDark) CognoDarkText else CognoText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(if (isDark) CognoDarkSurface else Color.White)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(modifier = Modifier.clickable { avatarPicker.launch(arrayOf("image/*")) }) {
                UserAvatar(userName, avatarUri, 92, isDark)
            }
            Text(
                text = if (isEnglish) "Tap avatar to choose a photo" else "点击头像选择照片",
                color = if (isDark) CognoDarkPrimary else CognoPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (isEnglish) "Display name" else "用户名称",
                    color = CognoMuted,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(
                        value = userName,
                        onValueChange = { userName = it.take(32) },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = if (isDark) CognoDarkText else CognoText,
                            fontSize = 15.sp
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) CognoDarkBackground else CognoBackground)
                            .padding(14.dp)
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(
                        text = if (isEnglish) "Save" else "保存",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) CognoDarkPrimary else CognoPrimary)
                            .clickable(onClick = ::saveName)
                            .padding(horizontal = 16.dp, vertical = 13.dp)
                    )
                }
            }
            Text(
                text = if (isEnglish) "Saved only on this device" else "资料仅保存在本机",
                color = CognoMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }

    if (avatarConfirmVisible) {
        AvatarConfirmDialog(
            userName = userName,
            avatarUri = pendingAvatarUri,
            isDark = isDark,
            isEnglish = isEnglish,
            onChooseAgain = { avatarPicker.launch(arrayOf("image/*")) },
            onCancel = {
                pendingAvatarUri = ""
                avatarConfirmVisible = false
            },
            onSave = {
                avatarUri = pendingAvatarUri
                store.saveAvatarUri(avatarUri)
                onSaved(userName.trim().ifBlank { AppSettingsStore.DEFAULT_USER_NAME }, avatarUri)
                pendingAvatarUri = ""
                avatarConfirmVisible = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AvatarConfirmDialog(
    userName: String,
    avatarUri: String,
    isDark: Boolean,
    isEnglish: Boolean,
    onChooseAgain: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    BasicAlertDialog(onDismissRequest = onCancel) {
        Surface(
            color = if (isDark) CognoDarkSurface else Color.White,
            shape = RoundedCornerShape(22.dp),
            shadowElevation = 18.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isEnglish) "Preview avatar" else "预览头像",
                    color = if (isDark) CognoDarkText else CognoText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(18.dp))
                UserAvatar(userName, avatarUri, 240, isDark)
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = if (isEnglish) "Choose another photo" else "选择图片",
                    color = if (isDark) CognoDarkPrimary else CognoPrimary,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clickable(onClick = onChooseAgain)
                        .padding(8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isEnglish) "Cancel" else "取消",
                        color = if (isDark) CognoDarkText else CognoText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onCancel)
                            .padding(vertical = 12.dp)
                    )
                    Text(
                        text = if (isEnglish) "Save" else "保存",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) CognoDarkPrimary else CognoPrimary)
                            .clickable(onClick = onSave)
                            .padding(vertical = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun UserAvatar(
    userName: String,
    avatarUri: String,
    size: Int,
    isDark: Boolean
) {
    val context = LocalContext.current
    val bitmap = remember(avatarUri) {
        if (avatarUri.isBlank()) {
            null
        } else {
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(avatarUri))?.use(BitmapFactory::decodeStream)
            }.getOrNull()
        }
    }
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(if (isDark) CognoDarkPrimary else CognoPrimary),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = userName.trim().take(2).uppercase().ifBlank { "CO" },
                color = Color.White,
                fontSize = (size * 0.32f).sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
