package com.aetheria.forevercompanion.data.local.dao

import androidx.room.*
import com.aetheria.forevercompanion.data.local.entities.EmotionalStateEntity
import com.aetheria.forevercompanion.data.local.entities.PetMood
import kotlinx.coroutines.flow.Flow

@Dao
interface EmotionalStateDao {
    @Query("SELECT * FROM emotional_states WHERE companionId = :id ORDER BY timestamp DESC LIMIT 1")
    fun getLatest(id: String): Flow<EmotionalStateEntity?>

    @Query("SELECT * FROM emotional_states WHERE companionId = :id AND timestamp > :since ORDER BY timestamp DESC")
    suspend fun getSince(id: String, since: Long): List<EmotionalStateEntity>

    @Insert
    suspend fun insert(state: EmotionalStateEntity)

    @Query("DELETE FROM emotional_states WHERE timestamp < :before")
    suspend fun pruneOlderThan(before: Long)
}
