package com.aetheria.forevercompanion.data.local.dao

import androidx.room.*
import com.aetheria.forevercompanion.data.local.entities.CompanionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CompanionDao {
    @Query("SELECT * FROM companions WHERE isActive = 1 LIMIT 1")
    fun getActiveCompanion(): Flow<CompanionEntity?>

    @Query("SELECT * FROM companions WHERE id = :id")
    suspend fun getById(id: String): CompanionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(companion: CompanionEntity)

    @Update
    suspend fun update(companion: CompanionEntity)

    @Query("UPDATE companions SET totalInteractions = totalInteractions + 1, lastInteractionTime = :time WHERE id = :id")
    suspend fun incrementInteractions(id: String, time: Long = System.currentTimeMillis())

    @Delete
    suspend fun delete(companion: CompanionEntity)
}
