package com.aetheria.forevercompanion.data.local.dao

import androidx.room.*
import com.aetheria.forevercompanion.data.local.entities.AppUsageHistoryEntity
import com.aetheria.forevercompanion.data.local.entities.AppCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface AppUsageDao {
    @Query("SELECT * FROM app_usage_history ORDER BY sessionStart DESC LIMIT :limit")
    fun getRecent(limit: Int = 50): Flow<List<AppUsageHistoryEntity>>

    @Query("SELECT * FROM app_usage_history WHERE sessionStart > :since ORDER BY duration DESC")
    suspend fun getSince(since: Long): List<AppUsageHistoryEntity>

    @Query("SELECT SUM(duration) FROM app_usage_history WHERE category = :cat AND sessionStart > :since")
    suspend fun getTotalTimeInCategory(cat: AppCategory, since: Long): Long?

    @Insert
    suspend fun insert(usage: AppUsageHistoryEntity)

    @Query("DELETE FROM app_usage_history WHERE sessionStart < :before")
    suspend fun pruneOlderThan(before: Long)
}
