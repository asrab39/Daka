package com.example.data.repository

import com.example.BuildConfig
import com.example.data.api.Candidate
import com.example.data.api.Content
import com.example.data.api.GeminiApiService
import com.example.data.api.GeminiClient
import com.example.data.api.GenerateContentRequest
import com.example.data.api.GenerationConfig
import com.example.data.api.InlineData
import com.example.data.api.Part
import com.example.data.local.ConversationDao
import com.example.data.model.ConversationEntity
import com.example.data.model.MessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ChatRepository(
    private val conversationDao: ConversationDao,
    private val geminiService: GeminiApiService = GeminiClient.service
) {
    val allConversations: Flow<List<ConversationEntity>> = conversationDao.getAllConversations()

    fun getMessages(conversationId: Long): Flow<List<MessageEntity>> =
        conversationDao.getMessagesForConversation(conversationId)

    fun searchConversations(query: String): Flow<List<ConversationEntity>> =
        conversationDao.searchConversations(query)

    suspend fun getConversation(id: Long): ConversationEntity? = withContext(Dispatchers.IO) {
        conversationDao.getConversationById(id)
    }

    suspend fun createConversation(
        title: String = "New Chat",
        model: String = "gemini-3.5-flash",
        systemPrompt: String? = null
    ): Long = withContext(Dispatchers.IO) {
        val conversation = ConversationEntity(
            title = title,
            model = model,
            systemPrompt = systemPrompt,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        conversationDao.insertConversation(conversation)
    }

    suspend fun updateConversation(conversation: ConversationEntity) = withContext(Dispatchers.IO) {
        conversationDao.updateConversation(conversation)
    }

    suspend fun deleteConversation(id: Long) = withContext(Dispatchers.IO) {
        conversationDao.deleteConversationById(id)
    }

    suspend fun deleteAllConversations() = withContext(Dispatchers.IO) {
        conversationDao.deleteAllConversations()
    }

    suspend fun deleteMessage(id: Long) = withContext(Dispatchers.IO) {
        conversationDao.deleteMessageById(id)
    }

    suspend fun sendMessage(
        conversationId: Long,
        userText: String,
        imageBase64: String? = null,
        customApiKey: String? = null,
        modelOverride: String? = null,
        systemPromptOverride: String? = null,
        temperature: Float = 0.7f
    ): Result<MessageEntity> = withContext(Dispatchers.IO) {
        // 1. Resolve API key
        val effectiveApiKey = if (!customApiKey.isNullOrBlank()) {
            customApiKey.trim()
        } else {
            try {
                BuildConfig.GEMINI_API_KEY
            } catch (e: Exception) {
                ""
            }
        }

        // 2. Persist User Message to Room
        val userMsg = MessageEntity(
            conversationId = conversationId,
            role = "user",
            content = userText,
            timestamp = System.currentTimeMillis(),
            imageBase64 = imageBase64
        )
        conversationDao.insertMessage(userMsg)

        // 3. Update Conversation Title & Timestamp
        val existingConv = conversationDao.getConversationById(conversationId)
        val currentTitle = existingConv?.title ?: "New Chat"
        val updatedTitle = if (currentTitle == "New Chat" || currentTitle.isBlank()) {
            val preview = userText.trim().take(36)
            if (userText.length > 36) "$preview..." else preview
        } else {
            currentTitle
        }
        conversationDao.updateConversationTitleAndTimestamp(
            id = conversationId,
            title = updatedTitle,
            updatedAt = System.currentTimeMillis()
        )

        // If no API key configured, generate helpful onboarding / simulation response
        if (effectiveApiKey.isBlank() || effectiveApiKey == "MY_GEMINI_API_KEY") {
            val fallbackResponse = buildMockGeminiResponse(userText, imageBase64 != null)
            val modelMsg = MessageEntity(
                conversationId = conversationId,
                role = "model",
                content = fallbackResponse,
                timestamp = System.currentTimeMillis(),
                tokens = (userText.length + fallbackResponse.length) / 4,
                latencyMs = 450
            )
            val savedId = conversationDao.insertMessage(modelMsg)
            return@withContext Result.success(modelMsg.copy(id = savedId))
        }

        // 4. Build Multi-turn History
        val pastMessages = conversationDao.getMessagesForConversationSnapshot(conversationId)
        val contentsList = mutableListOf<Content>()

        // Take last 12 messages for context to avoid token bloat
        val contextWindow = pastMessages.takeLast(12)
        for (msg in contextWindow) {
            val parts = mutableListOf<Part>()
            if (!msg.imageBase64.isNullOrBlank()) {
                parts.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = msg.imageBase64)))
            }
            if (msg.content.isNotBlank()) {
                parts.add(Part(text = msg.content))
            }
            val role = if (msg.role == "user") "user" else "model"
            if (parts.isNotEmpty()) {
                contentsList.add(Content(role = role, parts = parts))
            }
        }

        val effectiveModel = modelOverride ?: existingConv?.model ?: "gemini-3.5-flash"
        val systemInstruction = (systemPromptOverride ?: existingConv?.systemPrompt ?: DEFAULT_SYSTEM_PROMPT).let { prompt ->
            if (prompt.isNotBlank()) Content(parts = listOf(Part(text = prompt))) else null
        }

        val request = GenerateContentRequest(
            contents = contentsList,
            generationConfig = GenerationConfig(
                temperature = temperature,
                topP = 0.95f,
                topK = 40
            ),
            systemInstruction = systemInstruction
        )

        val startTime = System.currentTimeMillis()
        try {
            val response = geminiService.generateContent(
                model = effectiveModel,
                apiKey = effectiveApiKey,
                request = request
            )
            val latency = System.currentTimeMillis() - startTime
            val replyText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "I received your request, but could not generate a textual reply."

            val totalTokens = response.usageMetadata?.totalTokenCount
                ?: ((userText.length + replyText.length) / 4)

            val assistantMsg = MessageEntity(
                conversationId = conversationId,
                role = "model",
                content = replyText,
                timestamp = System.currentTimeMillis(),
                tokens = totalTokens,
                latencyMs = latency
            )
            val savedId = conversationDao.insertMessage(assistantMsg)
            Result.success(assistantMsg.copy(id = savedId))
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            val errorDetails = e.localizedMessage ?: "Unknown error connecting to Gemini API"
            val errorMsg = MessageEntity(
                conversationId = conversationId,
                role = "error",
                content = "⚠️ **Gemini Connection Error**\n\n$errorDetails\n\n*Tip: Check your API key in Settings (⚙️) or verify your internet connection.*",
                timestamp = System.currentTimeMillis(),
                latencyMs = latency
            )
            val savedId = conversationDao.insertMessage(errorMsg)
            Result.failure(Exception(errorDetails))
        }
    }

    private fun buildMockGeminiResponse(userQuery: String, hasImage: Boolean): String {
        val q = userQuery.lowercase()
        return when {
            hasImage -> """
                ✨ **Gemini Multimodal Analysis**
                
                I noticed you attached an image! To perform real-time visual reasoning and optical character recognition with Google Gemini, please ensure your **Gemini API Key** is configured in **Settings (⚙️)**.
                
                Here is what I can do with your images:
                - 🔍 **Visual Code & Diagram Inspection**
                - 📊 **Chart & UI Wireframe Analysis**
                - 📝 **Extract & Translate Handwritten Text**
                - 🎨 **Generate Creative Captions & Stories**
            """.trimIndent()

            q.contains("who are you") || q.contains("من أنت") || q.contains("نفسك") || q.contains("clone") -> """
                ✨ **Google Gemini AI Assistant (Clone Replica)**
                
                I am your personal replica of **Google Gemini**, built with Google DeepMind intelligence! 
                
                I mirror the full capabilities of Google AI Studio's coding and creative companion:
                - 🚀 **Full-Stack Coding & Debugging** (Kotlin, Python, TypeScript, Compose & more)
                - 🧠 **Deep Reasoning & Architecture** (System design, algorithms, analysis)
                - 🎨 **Creative Brainstorming & Writing** (Articles, ideas, translations)
                - ⚡ **Multi-Session Memory & Export** (Room database local persistence)
                
                You can switch between **Gemini 3.5 Flash**, **Gemini 3.1 Pro**, and **Flash Lite** anytime using the model selector at the top!
            """.trimIndent()

            q.contains("code") || q.contains("kotlin") || q.contains("compose") || q.contains("برمجة") -> """
                Here is a sample Jetpack Compose component crafted with Google Gemini AI Studio design principles:
                
                ```kotlin
                @Composable
                fun GeminiGlowingCard(
                    title: String,
                    description: String,
                    onClick: () -> Unit
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clickable { onClick() },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                ```
                
                *You can tap the **Run/Preview** button on any code block to inspect the code structure or **Copy** to use it directly in your project!*
            """.trimIndent()

            else -> """
                ✨ **Hello! I am your Google Gemini Assistant.**
                
                I am ready to help you with code development, logical problem solving, creative composition, or deep discussions.
                
                > *"Intelligence is the ability to adapt to change and generate new solutions."*
                
                How would you like to proceed? You can ask me to write code, review an architecture, explain a concept, or translate text.
            """.trimIndent()
        }
    }

    companion object {
        const val DEFAULT_SYSTEM_PROMPT = """You are Gemini, an advanced, highly intelligent AI coding assistant and creative partner developed by Google DeepMind.
You possess deep expertise in software engineering, Kotlin/Jetpack Compose, algorithm design, creative writing, scientific analysis, and problem-solving.
Always format your responses with structured, clean Markdown. When providing code, specify the language (e.g. ```kotlin, ```python, ```json) and write clean, idiomatic, robust code with clear comments. Be helpful, concise, and insightful."""
    }
}
