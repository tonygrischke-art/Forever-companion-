package com.aetheria.forevercompanion.data.local.dao

import androidx.room.*
import com.aetheria.forevercompanion.data.local.entities.DialogueEntity
import com.aetheria.forevercompanion.data.local.entities.DialogueCategory
import com.aetheria.forevercompanion.data.local.entities.PetMood
import com.aetheria.forevercompanion.data.local.entities.RelationshipPhase

@Dao
interface DialogueDao {

    // FIX: Replaced "relationshipPhase <= :phase" (broken enum string comparison) with
    // explicit IN list matching allowed phases up to the current one.
    // STRANGER phase gets only STRANGER dialogues.
    // FRIEND phase gets STRANGER + FRIEND dialogues.
    // COMPANION phase gets all dialogues.
    @Query("""
        SELECT * FROM dialogues 
        WHERE category = :cat 
          AND mood = :mood 
          AND relationshipPhase IN (:allowedPhases)
        ORDER BY RANDOM() LIMIT 1
    """)
    suspend fun getRandom(
        cat: DialogueCategory,
        mood: PetMood,
        allowedPhases: List<RelationshipPhase>
    ): DialogueEntity?

    @Query("SELECT * FROM dialogues WHERE category = :cat ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomByCategory(cat: DialogueCategory): DialogueEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dialogues: List<DialogueEntity>)

    @Query("UPDATE dialogues SET usageCount = usageCount + 1, lastUsed = :time WHERE id = :id")
    suspend fun markUsed(id: String, time: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM dialogues")
    suspend fun count(): Int
}
