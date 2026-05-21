package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ChatDatabase
import com.example.data.local.ChatMessage
import com.example.data.local.ChatSession
import com.example.data.models.SundayModelType
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ChatDatabase.getDatabase(application)
    private val repository = ChatRepository(db.chatDao())

    val sessions: StateFlow<List<ChatSession>> = repository.sessions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()

    private val _currentModelType = MutableStateFlow(SundayModelType.COZY)
    val currentModelType: StateFlow<SundayModelType> = _currentModelType.asStateFlow()

    val activeMessages: StateFlow<List<ChatMessage>> = _activeSessionId
        .flatMapLatest { sessionId ->
            if (sessionId != null) {
                repository.getMessagesFlow(sessionId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    init {
        // Automatically check if we have any sessions, if not, create a default one
        viewModelScope.launch {
            sessions.collect { sessionList ->
                if (sessionList.isEmpty()) {
                    // Create an initial Cozy session so the user is greeted immediately
                    startNewSession(SundayModelType.COZY)
                } else if (_activeSessionId.value == null) {
                    selectSession(sessionList.first().id)
                }
            }
        }
    }

    fun selectSession(sessionId: String) {
        viewModelScope.launch {
            _activeSessionId.value = sessionId
            val session = db.chatDao().getSessionById(sessionId)
            if (session != null) {
                _currentModelType.value = SundayModelType.fromId(session.modelId)
            }
        }
    }

    fun startNewSession(modelType: SundayModelType) {
        viewModelScope.launch {
            _currentModelType.value = modelType
            val newSessionId = repository.createSession(modelType)
            _activeSessionId.value = newSessionId
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            val currentSessions = sessions.value.filter { it.id != sessionId }
            if (_activeSessionId.value == sessionId) {
                if (currentSessions.isNotEmpty()) {
                    selectSession(currentSessions.first().id)
                } else {
                    _activeSessionId.value = null
                    // Cozy default will be re-created automatically by the collectible init block
                }
            }
        }
    }

    fun renameSession(sessionId: String, newTitle: String) {
        viewModelScope.launch {
            repository.updateSessionTitle(sessionId, newTitle)
        }
    }

    fun sendMessage(text: String) {
        val sessionId = _activeSessionId.value ?: return
        if (text.trim().isEmpty() || _isSending.value) return

        viewModelScope.launch {
            _isSending.value = true
            repository.sendMessage(sessionId, text.trim())
            _isSending.value = false
        }
    }
}
