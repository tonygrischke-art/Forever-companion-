package com.aetheria.forevercompanion.data.local.dao

import androidx.room.*
import com.aetheria.forevercompanion.data.local.entities.WellnessEventEntity
import com.aetheria.forevercompanion.data.local.entities.WellnessEventType
import kotlinx.coroutines.flow.Flow

@Dao
interface WellnessDao {
    @Query("SELECT * FROM wellness_events ORDER BY triggerTime DESC LIMIT :limit")
    fun getRecent(limit: Int = 20): Flow<List<WellnessEventEntity>>

    @Query("SELECT * FROM wellness_events WHERE eventType = :type AND triggerTime > :since LIMIT 1")
    suspend fun getLatestOfType(type: WellnessEventType, since: Long): WellnessEventEntity?

    @Insert
    suspend fun insert(event: WellnessEventEntity)

    @Update
    suspend fun update(event: WellnessEventEntity)
}
