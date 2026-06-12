package com.ntoprevd.cogno.ui.chat

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ntoprevd.cogno.data.db.entity.MessageEntity
import com.ntoprevd.cogno.data.db.entity.SessionEntity
import com.ntoprevd.cogno.data.repository.GeneratedNoteResult
import com.ntoprevd.cogno.data.repository.NativeChatRepository
import com.ntoprevd.cogno.data.settings.AppLanguagePreference
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class NoteToast(
    val message: String,
    val isError: Boolean = false,
    val autoDismiss: Boolean = false
)

data class ChatUiState(
    val sessions: List<SessionEntity> = emptyList(),
    val currentSessionId: String? = null,
    val messages: List<MessageEntity> = emptyList(),
    val inputText: String = "",
    val isDrawerOpen: Boolean = false,
    val isSending: Boolean = false,
    val isGeneratingNote: Boolean = false,
    val noteToast: NoteToast? = null,
    val errorMessage: String? = null
) {
    val showWelcome: Boolean
        get() = currentSessionId == null || messages.isEmpty()
}

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NativeChatRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var messagesJob: Job? = null

    init {
        observeSessions()
    }

    fun onInputChange(value: String) {
        _uiState.update { it.copy(inputText = value) }
    }

    fun stopGenerating() {
        repository.cancelAssistantReply()
    }

    fun openDrawer() {
        _uiState.update { it.copy(isDrawerOpen = true) }
    }

    fun closeDrawer() {
        _uiState.update { it.copy(isDrawerOpen = false) }
    }

    fun startNewSession() {
        messagesJob?.cancel()
        _uiState.update {
            it.copy(
                currentSessionId = null,
                messages = emptyList(),
                inputText = "",
                isDrawerOpen = false,
                errorMessage = null
            )
        }
    }

    fun selectSession(sessionId: String) {
        observeMessages(sessionId)
        _uiState.update {
            it.copy(
                currentSessionId = sessionId,
                isDrawerOpen = false,
                errorMessage = null
            )
        }
    }

    fun sendMessage(languagePreference: String, imageUri: Uri? = null) {
        val state = uiState.value
        val content = state.inputText.trim()
        if ((content.isEmpty() && imageUri == null) || state.isSending) return
        val copy = chatViewModelCopy(languagePreference)

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, errorMessage = null) }

            runCatching {
                repository.beginUserMessage(uiState.value.currentSessionId, content, imageUri)
            }.onSuccess { result ->
                observeMessages(result.session.id)
                _uiState.update {
                    it.copy(
                        currentSessionId = result.session.id,
                        inputText = "",
                        errorMessage = null
                    )
                }
                runCatching {
                    repository.completeAssistantReply(result.session.id, result.assistantMessage.id)
                }.onSuccess {
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            errorMessage = null
                        )
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            errorMessage = error.message ?: copy.aiRequestFailed
                        )
                    }
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSending = false,
                        errorMessage = error.message ?: copy.sendFailed
                    )
                }
            }
        }
    }

    fun renameSession(sessionId: String, title: String) {
        viewModelScope.launch {
            repository.renameSession(sessionId, title)
        }
    }

    fun toggleSessionPinned(session: SessionEntity) {
        viewModelScope.launch {
            repository.setSessionPinned(session.id, !session.pinned)
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            _uiState.update {
                if (it.currentSessionId == sessionId) {
                    it.copy(currentSessionId = null, messages = emptyList(), errorMessage = null)
                } else {
                    it
                }
            }
        }
    }

    fun updateUserMessage(messageId: String, content: String, languagePreference: String) {
        if (uiState.value.isSending) return
        val copy = chatViewModelCopy(languagePreference)

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, errorMessage = null) }
            runCatching {
                repository.updateUserMessageAndRegenerate(messageId, content)
            }.onSuccess { regenerated ->
                _uiState.update {
                    it.copy(
                        isSending = false,
                        errorMessage = if (regenerated) null else copy.noReplyToRegenerate
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSending = false,
                        errorMessage = error.message ?: copy.editRegenerateFailed
                    )
                }
            }
        }
    }

    fun setAssistantFeedback(messageId: String, feedback: String?) {
        viewModelScope.launch {
            repository.setAssistantFeedback(messageId, feedback)
        }
    }

    fun regenerateAssistantMessage(message: MessageEntity, languagePreference: String) {
        if (uiState.value.isSending) return
        val copy = chatViewModelCopy(languagePreference)

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, errorMessage = null) }
            runCatching {
                repository.regenerateAssistantReply(message.sessionId, message.id)
            }.onSuccess {
                _uiState.update { it.copy(isSending = false, errorMessage = null) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSending = false,
                        errorMessage = error.message ?: copy.regenerateFailed
                    )
                }
            }
        }
    }

    fun generateNote(style: String, languagePreference: String) {
        val sessionId = uiState.value.currentSessionId
        if (sessionId.isNullOrBlank() || uiState.value.isGeneratingNote) return
        val copy = chatViewModelCopy(languagePreference)

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGeneratingNote = true,
                    noteToast = NoteToast(copy.generatingNote),
                    errorMessage = null
                )
            }
            runCatching {
                repository.generateNoteFromSession(sessionId, style)
            }.onSuccess { result ->
                _uiState.update {
                    it.copy(
                        isGeneratingNote = false,
                        noteToast = NoteToast(noteResultMessage(result, copy), autoDismiss = true)
                    )
                }
                dismissNoteToastLater()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isGeneratingNote = false,
                        noteToast = NoteToast(
                            error.message ?: copy.generateNoteFailed,
                            isError = true,
                            autoDismiss = true
                        )
                    )
                }
                dismissNoteToastLater()
            }
        }
    }

    private fun dismissNoteToastLater() {
        viewModelScope.launch {
            delay(2200)
            _uiState.update {
                if (it.noteToast?.autoDismiss == true) {
                    it.copy(noteToast = null)
                } else {
                    it
                }
            }
        }
    }

    private fun noteResultMessage(result: GeneratedNoteResult, copy: ChatViewModelCopy): String {
        return when (result.status) {
            GeneratedNoteResult.UPDATED -> "${copy.noteUpdated}${result.note.title}"
            GeneratedNoteResult.UP_TO_DATE -> "${copy.noteUpToDate}${result.note.title}"
            else -> "${copy.noteSaved}${result.note.title}"
        }
    }

    private fun observeSessions() {
        viewModelScope.launch {
            repository.observeSessions().collect { sessions ->
                _uiState.update { it.copy(sessions = sessions) }
            }
        }
    }

    private fun observeMessages(sessionId: String) {
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            repository.observeMessages(sessionId).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }
}

private data class ChatViewModelCopy(
    val noReplyToRegenerate: String,
    val editRegenerateFailed: String,
    val regenerateFailed: String,
    val generatingNote: String,
    val generateNoteFailed: String,
    val noteUpdated: String,
    val noteUpToDate: String,
    val noteSaved: String,
    val aiRequestFailed: String,
    val sendFailed: String
)

private fun chatViewModelCopy(languagePreference: String): ChatViewModelCopy {
    return if (languagePreference == AppLanguagePreference.EN) {
        ChatViewModelCopy(
            noReplyToRegenerate = "Message updated, but no reply was found to regenerate",
            editRegenerateFailed = "Regeneration after editing failed. Please try again later.",
            regenerateFailed = "Regeneration failed. Please try again later.",
            generatingNote = "Generating note...",
            generateNoteFailed = "Failed to generate note. Please try again later.",
            noteUpdated = "Updated note: ",
            noteUpToDate = "Note is already up to date: ",
            noteSaved = "Saved to note library: ",
            aiRequestFailed = "AI request failed. Please check API settings.",
            sendFailed = "Failed to send. Please try again later."
        )
    } else {
        ChatViewModelCopy(
            noReplyToRegenerate = "已修改消息，但没有找到可重新生成的回复",
            editRegenerateFailed = "修改后重新生成失败，请稍后重试",
            regenerateFailed = "重新生成失败，请稍后重试",
            generatingNote = "正在生成笔记...",
            generateNoteFailed = "生成笔记失败，请稍后重试",
            noteUpdated = "已更新笔记：",
            noteUpToDate = "笔记已是最新：",
            noteSaved = "已保存到笔记库：",
            aiRequestFailed = "AI 请求失败，请检查 API 配置",
            sendFailed = "发送失败，请稍后重试"
        )
    }
}
