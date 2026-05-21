package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey val id: String, // UUID as String
    val title: String,
    val modelId: String, // e.g., "cozy", "pro", "artist"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String, // foreign reference to chat_sessions.id
    val role: String, // "user" or "assistant"
    val text: String,
    val imageB64: String? = null, // Holds the base64 generated image for the Artist model
    val timestamp: Long = System.currentTimeMillis()
)
