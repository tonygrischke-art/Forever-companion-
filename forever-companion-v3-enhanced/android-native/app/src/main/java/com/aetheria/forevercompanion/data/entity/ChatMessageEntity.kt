package com.aetheria.forevercompanion.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val companionId: Long,
    val role: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
