package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.ConversationEntity
import com.example.data.model.MessageEntity
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Locale

class GeminiViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ChatRepository
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    private val _activeConversationId = MutableStateFlow<Long?>(null)
    val activeConversationId: StateFlow<Long?> = _activeConversationId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _attachedImageBase64 = MutableStateFlow<String?>(null)
    val attachedImageBase64: StateFlow<String?> = _attachedImageBase64.asStateFlow()

    private val _activeModel = MutableStateFlow("gemini-3.5-flash")
    val activeModel: StateFlow<String> = _activeModel.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _customApiKey = MutableStateFlow("")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _systemPrompt = MutableStateFlow(ChatRepository.DEFAULT_SYSTEM_PROMPT)
    val systemPrompt: StateFlow<String> = _systemPrompt.asStateFlow()

    private val _temperature = MutableStateFlow(0.7f)
    val temperature: StateFlow<Float> = _temperature.asStateFlow()

    private val _codeInspectorTarget = MutableStateFlow<Pair<String, String>?>(null)
    val codeInspectorTarget: StateFlow<Pair<String, String>?> = _codeInspectorTarget.asStateFlow()

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ChatRepository(database.conversationDao())

        // Initialize TextToSpeech
        tts = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                isTtsInitialized = true
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val conversations: StateFlow<List<ConversationEntity>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) repository.allConversations
            else repository.searchConversations(query)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<MessageEntity>> = _activeConversationId
        .flatMapLatest { convId ->
            if (convId != null) repository.getMessages(convId)
            else flowOf(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onInputTextChange(text: String) {
        _inputText.value = text
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onModelSelected(model: String) {
        _activeModel.value = model
    }

    fun onApiKeyChange(key: String) {
        _customApiKey.value = key
    }

    fun onSystemPromptChange(prompt: String) {
        _systemPrompt.value = prompt
    }

    fun onTemperatureChange(temp: Float) {
        _temperature.value = temp
    }

    fun openSettings() {
        _showSettingsDialog.value = true
    }

    fun closeSettings() {
        _showSettingsDialog.value = false
    }

    fun openCodeInspector(language: String, code: String) {
        _codeInspectorTarget.value = language to code
    }

    fun closeCodeInspector() {
        _codeInspectorTarget.value = null
    }

    fun selectConversation(id: Long) {
        _activeConversationId.value = id
        viewModelScope.launch {
            val conv = repository.getConversation(id)
            if (conv != null) {
                _activeModel.value = conv.model
                if (!conv.systemPrompt.isNullOrBlank()) {
                    _systemPrompt.value = conv.systemPrompt
                }
            }
        }
    }

    fun startNewChat() {
        _activeConversationId.value = null
        _inputText.value = ""
        _attachedImageBase64.value = null
    }

    fun deleteConversation(id: Long) {
        viewModelScope.launch {
            repository.deleteConversation(id)
            if (_activeConversationId.value == id) {
                _activeConversationId.value = null
            }
        }
    }

    fun clearAllConversations() {
        viewModelScope.launch {
            repository.deleteAllConversations()
            _activeConversationId.value = null
        }
    }

    fun clearCurrentChat() {
        val currentId = _activeConversationId.value ?: return
        deleteConversation(currentId)
    }

    fun attachImageUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (originalBitmap != null) {
                    // Resize to max 1024 to keep payload reasonable
                    val maxDimension = 1024
                    val scale = (maxDimension.toFloat() / maxOf(originalBitmap.width, originalBitmap.height)).coerceAtMost(1f)
                    val scaledBitmap = if (scale < 1f) {
                        Bitmap.createScaledBitmap(
                            originalBitmap,
                            (originalBitmap.width * scale).toInt(),
                            (originalBitmap.height * scale).toInt(),
                            true
                        )
                    } else {
                        originalBitmap
                    }

                    val stream = ByteArrayOutputStream()
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                    val byteArray = stream.toByteArray()
                    _attachedImageBase64.value = Base64.encodeToString(byteArray, Base64.NO_WRAP)
                }
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    fun removeAttachedImage() {
        _attachedImageBase64.value = null
    }

    fun sendMessage(presetPrompt: String? = null) {
        val textToSend = presetPrompt ?: _inputText.value.trim()
        val imageToSend = _attachedImageBase64.value

        if (textToSend.isBlank() && imageToSend == null) return
        if (_isLoading.value) return

        _isLoading.value = true
        _inputText.value = ""
        _attachedImageBase64.value = null

        viewModelScope.launch {
            var currentConvId = _activeConversationId.value
            if (currentConvId == null) {
                currentConvId = repository.createConversation(
                    title = "New Chat",
                    model = _activeModel.value,
                    systemPrompt = _systemPrompt.value
                )
                _activeConversationId.value = currentConvId
            }

            repository.sendMessage(
                conversationId = currentConvId,
                userText = textToSend,
                imageBase64 = imageToSend,
                customApiKey = _customApiKey.value,
                modelOverride = _activeModel.value,
                systemPromptOverride = _systemPrompt.value,
                temperature = _temperature.value
            )

            _isLoading.value = false
        }
    }

    fun speakText(text: String) {
        if (!isTtsInitialized) return
        val cleanText = text.replace(Regex("```[\\s\\S]*?```"), "Code block omitted.")
            .replace(Regex("[#*_>`]"), "")
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "gemini_tts")
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }
}
