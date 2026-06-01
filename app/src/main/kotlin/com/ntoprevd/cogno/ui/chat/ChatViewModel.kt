package com.ntoprevd.cogno.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ntoprevd.cogno.data.db.entity.MessageEntity
import com.ntoprevd.cogno.data.db.entity.SessionEntity
import com.ntoprevd.cogno.data.repository.GeneratedNoteResult
import com.ntoprevd.cogno.data.repository.NativeChatRepository
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

    fun sendMessage() {
        val state = uiState.value
        val content = state.inputText.trim()
        if (content.isEmpty() || state.isSending) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, errorMessage = null) }

            runCatching {
                repository.beginUserMessage(uiState.value.currentSessionId, content)
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
                            errorMessage = error.message ?: "AI 请求失败，请检查 API 配置"
                        )
                    }
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSending = false,
                        errorMessage = error.message ?: "发送失败，请稍后重试"
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

    fun updateUserMessage(messageId: String, content: String) {
        if (uiState.value.isSending) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, errorMessage = null) }
            runCatching {
                repository.updateUserMessageAndRegenerate(messageId, content)
            }.onSuccess { regenerated ->
                _uiState.update {
                    it.copy(
                        isSending = false,
                        errorMessage = if (regenerated) null else "已修改消息，但没有找到可重新生成的回复"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSending = false,
                        errorMessage = error.message ?: "修改后重新生成失败，请稍后重试"
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

    fun regenerateAssistantMessage(message: MessageEntity) {
        if (uiState.value.isSending) return

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
                        errorMessage = error.message ?: "重新生成失败，请稍后重试"
                    )
                }
            }
        }
    }

    fun generateNote(style: String) {
        val sessionId = uiState.value.currentSessionId
        if (sessionId.isNullOrBlank() || uiState.value.isGeneratingNote) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGeneratingNote = true,
                    noteToast = NoteToast("正在生成笔记..."),
                    errorMessage = null
                )
            }
            runCatching {
                repository.generateNoteFromSession(sessionId, style)
            }.onSuccess { result ->
                _uiState.update {
                    it.copy(
                        isGeneratingNote = false,
                        noteToast = NoteToast(noteResultMessage(result), autoDismiss = true)
                    )
                }
                dismissNoteToastLater()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isGeneratingNote = false,
                        noteToast = NoteToast(
                            error.message ?: "生成笔记失败，请稍后重试",
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

    private fun noteResultMessage(result: GeneratedNoteResult): String {
        return when (result.status) {
            GeneratedNoteResult.UPDATED -> "已更新笔记：${result.note.title}"
            GeneratedNoteResult.UP_TO_DATE -> "笔记已是最新：${result.note.title}"
            else -> "已保存到笔记库：${result.note.title}"
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
