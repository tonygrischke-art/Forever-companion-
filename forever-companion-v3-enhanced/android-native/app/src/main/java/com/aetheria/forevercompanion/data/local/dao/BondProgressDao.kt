package com.aetheria.forevercompanion.data.local.dao

import androidx.room.*
import com.aetheria.forevercompanion.data.local.entities.BondProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BondProgressDao {
    @Query("SELECT * FROM bond_progress WHERE companionId = :id")
    fun getByCompanionId(id: String): Flow<BondProgressEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(progress: BondProgressEntity)

    @Update
    suspend fun update(progress: BondProgressEntity)

    @Query("UPDATE bond_progress SET currentXP = currentXP + :xp WHERE companionId = :id")
    suspend fun addXP(id: String, xp: Int)

    @Query("UPDATE bond_progress SET daysTogether = daysTogether + 1 WHERE companionId = :id")
    suspend fun incrementDays(id: String)
}
