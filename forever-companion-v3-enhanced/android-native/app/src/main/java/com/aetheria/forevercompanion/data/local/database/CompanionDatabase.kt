package com.aetheria.forevercompanion.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.aetheria.forevercompanion.data.local.converters.Converters
import com.aetheria.forevercompanion.data.local.dao.*
import com.aetheria.forevercompanion.data.local.entities.*

@Database(
    entities = [
        CompanionEntity::class,
        BondProgressEntity::class,
        EmotionalStateEntity::class,
        ConversationEntity::class,
        MemorySnapshotEntity::class,
        AccessoryEntity::class,
        CurrencyBalanceEntity::class,
        CurrencyTransactionEntity::class,
        AchievementEntity::class,
        AppUsageHistoryEntity::class,
        WellnessEventEntity::class,
        UserPreferencesEntity::class,
        DialogueEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CompanionDatabase : RoomDatabase() {
    abstract fun companionDao(): CompanionDao
    abstract fun bondProgressDao(): BondProgressDao
    abstract fun emotionalStateDao(): EmotionalStateDao
    abstract fun conversationDao(): ConversationDao
    abstract fun memorySnapshotDao(): MemorySnapshotDao
    abstract fun accessoryDao(): AccessoryDao
    abstract fun currencyDao(): CurrencyDao
    abstract fun achievementDao(): AchievementDao
    abstract fun appUsageDao(): AppUsageDao
    abstract fun wellnessDao(): WellnessDao
    abstract fun preferencesDao(): PreferencesDao
    abstract fun dialogueDao(): DialogueDao
}
