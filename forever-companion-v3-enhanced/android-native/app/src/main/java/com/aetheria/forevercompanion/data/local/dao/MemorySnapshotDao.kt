package com.aetheria.forevercompanion.data.local.dao

import androidx.room.*
import com.aetheria.forevercompanion.data.local.entities.MemorySnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemorySnapshotDao {
    @Query("SELECT * FROM memory_snapshots WHERE companionId = :id ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(id: String, limit: Int = 7): Flow<List<MemorySnapshotEntity>>

    @Query("SELECT * FROM memory_snapshots WHERE companionId = :id AND date = :date LIMIT 1")
    suspend fun getByDate(id: String, date: String): MemorySnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(snapshot: MemorySnapshotEntity)
}
