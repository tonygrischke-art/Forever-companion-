package com.aetheria.forevercompanion.data.local.dao

import androidx.room.*
import com.aetheria.forevercompanion.data.local.entities.UserPreferencesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PreferencesDao {
    @Query("SELECT * FROM user_preferences WHERE `key` = :key LIMIT 1")
    fun observe(key: String): Flow<UserPreferencesEntity?>

    @Query("SELECT value FROM user_preferences WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(pref: UserPreferencesEntity)

    @Query("DELETE FROM user_preferences WHERE `key` = :key")
    suspend fun delete(key: String)

    @Query("SELECT * FROM user_preferences")
    suspend fun getAll(): List<UserPreferencesEntity>
}
