package com.example.ui

import android.app.Application
import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
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
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

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

    // Accounts System & Settings SharedPreferences Local Storage
    private val prefs = application.getSharedPreferences("sunday_ai_prefs", Context.MODE_PRIVATE)

    private val _userName = MutableStateFlow(prefs.getString("user_name", "Usuario de Domingo") ?: "Usuario de Domingo")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userEmoji = MutableStateFlow(prefs.getString("user_emoji", "☕") ?: "☕")
    val userEmoji: StateFlow<String> = _userEmoji.asStateFlow()

    private val _isProMember = MutableStateFlow(prefs.getBoolean("is_pro_member", false))
    val isProMember: StateFlow<Boolean> = _isProMember.asStateFlow()

    private val _userApiKey = MutableStateFlow(prefs.getString("user_api_key", "") ?: "")
    val userApiKey: StateFlow<String> = _userApiKey.asStateFlow()

    // Gemini Live sound preferences and settings
    private val _liveVoiceSoundActive = MutableStateFlow(prefs.getBoolean("live_voice_sound", true))
    val liveVoiceSoundActive: StateFlow<Boolean> = _liveVoiceSoundActive.asStateFlow()

    private val _selectedVoiceGender = MutableStateFlow(prefs.getString("live_voice_gender", "FEMALE") ?: "FEMALE")
    val selectedVoiceGender: StateFlow<String> = _selectedVoiceGender.asStateFlow()

    // Live Voice screen active states
    private val _isLiveActive = MutableStateFlow(false)
    val isLiveActive: StateFlow<Boolean> = _isLiveActive.asStateFlow()

    private val _liveSpeakingStatus = MutableStateFlow("Silencioso") // "Escuchando", "Pensando", "Hablando", "Silencioso"
    val liveSpeakingStatus: StateFlow<String> = _liveSpeakingStatus.asStateFlow()

    private val _lastLiveText = MutableStateFlow("")
    val lastLiveText: StateFlow<String> = _lastLiveText.asStateFlow()

    // Text to Speech support
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    init {
        // Initialize Android Local TextToSpeech engine
        tts = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsInitialized = true
                val result = tts?.setLanguage(Locale("es", "ES"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    val resultLatam = tts?.setLanguage(Locale("es"))
                    Log.d("ChatViewModel", "TTS localized to Spanish generic: $resultLatam")
                }
            } else {
                Log.e("ChatViewModel", "TTS Initialization failed")
            }
        }

        // Auto select the first session if one exists, but do not create example sessions automatically!
        viewModelScope.launch {
            sessions.collect { sessionList ->
                if (_activeSessionId.value == null && sessionList.isNotEmpty()) {
                    selectSession(sessionList.first().id)
                }
            }
        }
    }

    override fun onInit(status: Int) {
        // Init handler handled directly in lambda
    }

    fun updateProfile(name: String, emoji: String) {
        _userName.value = name
        _userEmoji.value = emoji
        prefs.edit()
            .putString("user_name", name)
            .putString("user_emoji", emoji)
            .apply()
    }

    fun updateProStatus(isPro: Boolean) {
        _isProMember.value = isPro
        prefs.edit().putBoolean("is_pro_member", isPro).apply()
    }

    fun updateApiKey(apiKey: String) {
        _userApiKey.value = apiKey
        prefs.edit().putString("user_api_key", apiKey).apply()
    }

    fun updateVoiceConfig(soundActive: Boolean, voiceGender: String) {
        _liveVoiceSoundActive.value = soundActive
        _selectedVoiceGender.value = voiceGender
        prefs.edit()
            .putBoolean("live_voice_sound", soundActive)
            .putString("live_voice_gender", voiceGender)
            .apply()
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
        sendMessageInternal(text, false)
    }

    private fun sendMessageInternal(text: String, isFromLiveMode: Boolean) {
        val sessionId = _activeSessionId.value ?: return
        if (text.trim().isEmpty() || _isSending.value) return

        viewModelScope.launch {
            _isSending.value = true
            if (isFromLiveMode) {
                _liveSpeakingStatus.value = "Pensando"
            }
            
            val success = repository.sendMessage(sessionId, text.trim(), _userApiKey.value)
            _isSending.value = false

            if (success) {
                if (isFromLiveMode) {
                    _liveSpeakingStatus.value = "Hablando"
                    val updatedMessages = repository.getMessagesFlow(sessionId).first()
                    val lastResponse = updatedMessages.lastOrNull { it.role == "assistant" }
                    if (lastResponse != null) {
                        _lastLiveText.value = lastResponse.text
                        speak(lastResponse.text)
                    } else {
                        _liveSpeakingStatus.value = "Escuchando"
                    }
                }
            } else {
                if (isFromLiveMode) {
                    _liveSpeakingStatus.value = "Silencioso"
                }
            }
        }
    }

    // Live Mode specific triggers
    fun setLiveActive(active: Boolean) {
        _isLiveActive.value = active
        if (!active) {
            _liveSpeakingStatus.value = "Silencioso"
            stopLiveSpeech()
        }
    }

    fun startLiveSession(modelType: SundayModelType) {
        viewModelScope.launch {
            var sessionId = _activeSessionId.value
            // If active model is different, or there's no active session, start a clean one for live discussion
            if (sessionId == null || _currentModelType.value != modelType) {
                _currentModelType.value = modelType
                sessionId = repository.createSession(modelType)
                _activeSessionId.value = sessionId
            }
            
            setLiveActive(true)
            _liveSpeakingStatus.value = "Escuchando"
            
            // Speak initial greeting message from the assistant
            val updatedMessages = repository.getMessagesFlow(sessionId).first()
            val lastAssistantMsg = updatedMessages.lastOrNull { it.role == "assistant" }
            if (lastAssistantMsg != null) {
                _lastLiveText.value = lastAssistantMsg.text
                speak(lastAssistantMsg.text)
            }
        }
    }

    fun sendLiveVoiceInput(text: String) {
        _lastLiveText.value = text
        sendMessageInternal(text, true)
    }

    fun speak(text: String) {
        if (isTtsInitialized && _liveVoiceSoundActive.value) {
            try {
                tts?.stop()
                // Speak out using default locale parameters (fully compatible with older API and modern versions)
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "sunday_live_utterance")
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error speaking text", e)
            }
        }
    }

    fun stopLiveSpeech() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error stopping TTS", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error shutting down TTS", e)
        }
    }
}
