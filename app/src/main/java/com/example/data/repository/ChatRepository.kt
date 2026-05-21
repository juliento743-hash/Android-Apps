package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
import com.example.data.api.*
import com.example.data.local.ChatDao
import com.example.data.local.ChatMessage
import com.example.data.local.ChatSession
import com.example.data.models.SundayModelType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ChatRepository(private val chatDao: ChatDao) {

    val sessions: Flow<List<ChatSession>> = chatDao.getSessionsFlow()

    fun getMessagesFlow(sessionId: String): Flow<List<ChatMessage>> {
        return chatDao.getMessagesFlowForSession(sessionId)
    }

    suspend fun createSession(modelType: SundayModelType): String = withContext(Dispatchers.IO) {
        val sessionId = java.util.UUID.randomUUID().toString()
        val title = "Nueva charla - ${modelType.displayName}"
        val session = ChatSession(
            id = sessionId,
            title = title,
            modelId = modelType.id
        )
        chatDao.insertSession(session)
        
        // Add a cozy initial greeting based on the model persona
        val initialGreeting = when(modelType) {
            SundayModelType.COZY -> "¡Hola! ☕ Qué alegría que te pases por aquí este domingo. ¿Cómo estás hoy? ¿Te apetece conversar, leer alguna sugerencia relajante o simplemente reflexionar sobre tu fin de semana?"
            SundayModelType.DEEP -> "Hola. 🧠 Estoy listo para ayudarte a organizar tu semana. Cuéntame: ¿qué objetivos tienes para los próximos días, o qué tareas/proyectos te gustaría estructurar y resolver hoy?"
            SundayModelType.ARTIST -> "¡Hola! 🎨 Soy tu lienzo creativo hoy. ¿Qué imagen relajante, otoñal, acogedora o mágica de domingo te gustaría que dibujáramos hoy? Describe tu idea y la haré realidad."
        }
        
        chatDao.insertMessage(
            ChatMessage(
                sessionId = sessionId,
                role = "assistant",
                text = initialGreeting
            )
        )
        sessionId
    }

    suspend fun updateSessionTitle(sessionId: String, title: String) = withContext(Dispatchers.IO) {
        chatDao.updateSessionTitle(sessionId, title)
    }

    suspend fun deleteSession(sessionId: String) = withContext(Dispatchers.IO) {
        chatDao.deleteMessagesForSession(sessionId)
        chatDao.deleteSession(sessionId)
    }

    /**
     * Sends a message to the specified chat session using its configured model.
     * Returns true if successful, false if not.
     */
    suspend fun sendMessage(sessionId: String, textContent: String, customApiKey: String? = null): Boolean = withContext(Dispatchers.IO) {
        val apiKey = if (!customApiKey.isNullOrEmpty()) customApiKey else BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e("ChatRepository", "API Key is missing or default placeholder")
            insertErrorMessage(sessionId, "Error: Configura tu clave GEMINI_API_KEY en la sección Secrets de AI Studio para activar las respuestas.")
            return@withContext false
        }

        val session = chatDao.getSessionById(sessionId) ?: return@withContext false
        val modelType = SundayModelType.fromId(session.modelId)

        // 1. Insert User Message to Room
        val userMessage = ChatMessage(
            sessionId = sessionId,
            role = "user",
            text = textContent
        )
        chatDao.insertMessage(userMessage)

        // 2. Fetch Chat History for Context
        val history = chatDao.getMessagesForSession(sessionId)
        
        // 3. Build API Request
        val systemInstruction = Content(parts = listOf(Part(text = modelType.systemPrompt)))
        
        val request = if (modelType.isImageGenerator) {
            // Sunday Artist ignores general text history to focus solely on the latest art request
            val englishPromptEnhancement = "Create a cozy, beautiful illustration based on: $textContent. Soft warm lighting, atmospheric, high-detail aesthetic art, vector or warm illustration style. No text in image."
            GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = englishPromptEnhancement)))),
                generationConfig = GenerationConfig(
                    temperature = modelType.temperature,
                    responseModalities = listOf("TEXT", "IMAGE"),
                    imageConfig = ImageConfig(aspectRatio = "1:1", imageSize = "1K")
                ),
                systemInstruction = systemInstruction
            )
        } else {
            // Chat models: map history to alternating Contents
            // Standardizing roles for the API: user -> "user", assistant -> "model"
            val contentsList = history.map { msg ->
                Content(
                    parts = listOf(Part(text = msg.text)),
                    role = if (msg.role == "user") "user" else "model"
                )
            }
            GenerateContentRequest(
                contents = contentsList,
                generationConfig = GenerationConfig(temperature = modelType.temperature),
                systemInstruction = systemInstruction
            )
        }

        try {
            // 4. API Call
            val response = RetrofitClient.service.generateContent(
                model = modelType.serviceModelName,
                apiKey = apiKey,
                request = request
            )

            // 5. Parse Response
            val firstCandidate = response.candidates?.firstOrNull()
            val responseContent = firstCandidate?.content
            val firstPart = responseContent?.parts?.firstOrNull()

            if (firstPart == null) {
                insertErrorMessage(sessionId, "Mmm... Sunday no pudo elaborar una respuesta. Por favor, inténtalo de nuevo.")
                return@withContext false
            }

            var imageB64: String? = null
            var responseText = firstPart.text ?: ""

            // Check if there is generated image data (inlineData)
            val inlineData = firstPart.inlineData
            if (inlineData != null && inlineData.mimeType.startsWith("image/")) {
                imageB64 = inlineData.data
                responseText = responseText.ifEmpty { "¡Aquí tienes tu ilustración de domingo inspirada en tu idea! 🎨✨" }
            }

            // In case the artist model returned the image in another part
            if (imageB64 == null) {
                val imagePart = responseContent.parts.firstOrNull { it.inlineData != null }
                if (imagePart != null && imagePart.inlineData?.mimeType?.startsWith("image/") == true) {
                    imageB64 = imagePart.inlineData.data
                    responseText = responseText.ifEmpty { "¡Aquí tienes tu ilustración de domingo inspirada en tu idea! 🎨✨" }
                }
            }

            // Fallback for Sunday Artist model: in case it returned just text instead of image inline data
            if (modelType.isImageGenerator && imageB64 == null && responseText.isNotEmpty()) {
                // Keep the response text as explanation
                responseText = "Estaba pensando en tu idea: \"$responseText\". (Nota: no se generó imagen binaria, prueba con otra idea sencilla)."
            }

            // Update session title automatically if it was the first user message
            val userMsgCount = history.count { it.role == "user" }
            if (userMsgCount == 1) {
                val newTitle = if (textContent.length > 22) textContent.take(20) + "..." else textContent
                chatDao.updateSessionTitle(sessionId, newTitle)
            }

            // 6. Save Assistant Message to database
            chatDao.insertMessage(
                ChatMessage(
                    sessionId = sessionId,
                    role = "assistant",
                    text = responseText,
                    imageB64 = imageB64
                )
            )
            true
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error sending message", e)
            insertErrorMessage(sessionId, "Ups, ha ocurrido un error de conexión: ${e.localizedMessage ?: "Revisa tu conexión a internet o los límites de la API."}")
            false
        }
    }

    private suspend fun insertErrorMessage(sessionId: String, errorText: String) {
        chatDao.insertMessage(
            ChatMessage(
                sessionId = sessionId,
                role = "assistant",
                text = errorText
            )
        )
    }
}
