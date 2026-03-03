package com.aetheria.forevercompanion.data.local.dao

import androidx.room.*
import com.aetheria.forevercompanion.data.local.entities.ConversationEntity
import com.aetheria.forevercompanion.data.local.entities.UserReaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations WHERE companionId = :id ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(id: String, limit: Int = 50): Flow<List<ConversationEntity>>

    @Insert
    suspend fun insert(conversation: ConversationEntity)

    @Query("UPDATE conversations SET userReaction = :reaction WHERE id = :id")
    suspend fun updateReaction(id: String, reaction: UserReaction)

    @Query("SELECT COUNT(*) FROM conversations WHERE companionId = :id")
    suspend fun countAll(id: String): Int
}
