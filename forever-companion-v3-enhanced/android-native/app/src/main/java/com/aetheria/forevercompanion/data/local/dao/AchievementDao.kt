package com.aetheria.forevercompanion.data.local.dao

import androidx.room.*
import com.aetheria.forevercompanion.data.local.entities.AchievementEntity
import com.aetheria.forevercompanion.data.local.entities.AchievementCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements ORDER BY isCompleted ASC, category ASC")
    fun getAll(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE isCompleted = 0")
    fun getInProgress(): Flow<List<AchievementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(achievement: AchievementEntity)

    @Update
    suspend fun update(achievement: AchievementEntity)

    @Query("UPDATE achievements SET currentProgress = currentProgress + :increment WHERE id = :id AND isCompleted = 0")
    suspend fun incrementProgress(id: String, increment: Int = 1)

    @Query("UPDATE achievements SET isCompleted = 1, completedAt = :time WHERE id = :id")
    suspend fun markComplete(id: String, time: Long = System.currentTimeMillis())
}
