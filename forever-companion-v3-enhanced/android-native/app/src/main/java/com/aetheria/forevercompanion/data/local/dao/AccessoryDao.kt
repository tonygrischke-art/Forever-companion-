package com.aetheria.forevercompanion.data.local.dao

import androidx.room.*
import com.aetheria.forevercompanion.data.local.entities.AccessoryEntity
import com.aetheria.forevercompanion.data.local.entities.AccessoryCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface AccessoryDao {
    @Query("SELECT * FROM accessories ORDER BY rarity DESC, name ASC")
    fun getAll(): Flow<List<AccessoryEntity>>

    @Query("SELECT * FROM accessories WHERE isUnlocked = 1")
    fun getUnlocked(): Flow<List<AccessoryEntity>>

    @Query("SELECT * FROM accessories WHERE isEquipped = 1")
    fun getEquipped(): Flow<List<AccessoryEntity>>

    @Query("SELECT * FROM accessories WHERE category = :cat")
    fun getByCategory(cat: AccessoryCategory): Flow<List<AccessoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(accessory: AccessoryEntity)

    @Update
    suspend fun update(accessory: AccessoryEntity)

    @Query("UPDATE accessories SET isEquipped = 0")
    suspend fun unequipAll()

    @Query("UPDATE accessories SET isEquipped = 1, equippedAt = :time, timesUsed = timesUsed + 1 WHERE id = :id")
    suspend fun equip(id: String, time: Long = System.currentTimeMillis())

    @Query("UPDATE accessories SET isUnlocked = 1 WHERE id = :id")
    suspend fun unlock(id: String)
}
