package com.ntoprevd.cogno.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ntoprevd.cogno.data.db.entity.MessageEntity
import com.ntoprevd.cogno.data.db.entity.SessionEntity
import com.ntoprevd.cogno.data.repository.NativeChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val sessions: List<SessionEntity> = emptyList(),
    val currentSessionId: String? = null,
    val messages: List<MessageEntity> = emptyList(),
    val inputText: String = "",
    val isDrawerOpen: Boolean = false,
    val isSending: Boolean = false,
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
                repository.sendUserMessage(uiState.value.currentSessionId, content)
            }.onSuccess { result ->
                observeMessages(result.session.id)
                _uiState.update {
                    it.copy(
                        currentSessionId = result.session.id,
                        inputText = "",
                        isSending = false,
                        errorMessage = null
                    )
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
