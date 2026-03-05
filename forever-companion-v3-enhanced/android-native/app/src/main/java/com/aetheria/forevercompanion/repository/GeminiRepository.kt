package com.aetheria.forevercompanion.repository

import com.aetheria.forevercompanion.data.dao.ChatMessageDao
import com.aetheria.forevercompanion.data.entity.ChatMessageEntity
import com.aetheria.forevercompanion.service.GeminiMessage
import com.aetheria.forevercompanion.service.GeminiResponse
import com.aetheria.forevercompanion.service.GeminiService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiRepository @Inject constructor(
    private val geminiService: GeminiService,
    private val chatMessageDao: ChatMessageDao
) {
    fun getMessages(companionId: Long): Flow<List<ChatMessageEntity>> =
        chatMessageDao.getMessagesForCompanion(companionId)

    suspend fun sendMessage(companionId: Long, userText: String): GeminiResponse {
        chatMessageDao.insertMessage(
            ChatMessageEntity(companionId = companionId, role = "user", text = userText)
        )
        val history = chatMessageDao.getRecentMessages(companionId, limit = 20)
            .map { GeminiMessage(role = it.role, text = it.text) }
        val response = geminiService.sendMessage(
            userMessage = userText,
            conversationHistory = history.dropLast(1)
        )
        if (!response.isError) {
            chatMessageDao.insertMessage(
                ChatMessageEntity(companionId = companionId, role = "model", text = response.text)
            )
        }
        return response
    }

    suspend fun clearHistory(companionId: Long) =
        chatMessageDao.clearMessagesForCompanion(companionId)
}
