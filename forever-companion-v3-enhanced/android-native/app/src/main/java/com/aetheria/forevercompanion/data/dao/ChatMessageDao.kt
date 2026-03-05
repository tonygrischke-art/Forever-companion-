package com.aetheria.forevercompanion.data.dao

import androidx.room.*
import com.aetheria.forevercompanion.data.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE companionId = :companionId ORDER BY timestamp ASC")
    fun getMessagesForCompanion(companionId: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE companionId = :companionId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(companionId: Long, limit: Int): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages WHERE companionId = :companionId")
    suspend fun clearMessagesForCompanion(companionId: Long)
}
