package com.aetheria.forevercompanion.data.local.di

import android.content.Context
import androidx.room.Room
import com.aetheria.forevercompanion.data.AppDatabase
import com.aetheria.forevercompanion.data.dao.ChatMessageDao
import com.aetheria.forevercompanion.data.local.dao.*
import com.aetheria.forevercompanion.data.local.database.CompanionDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    // Primary companion database
    @Provides
    @Singleton
    fun provideCompanionDatabase(@ApplicationContext context: Context): CompanionDatabase =
        Room.databaseBuilder(context, CompanionDatabase::class.java, "forever_companion_db")
            .fallbackToDestructiveMigration()
            .build()

    // Legacy chat database — separate file to avoid schema collision
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "chat_legacy_db")
            .fallbackToDestructiveMigration()
            .build()

    // CompanionDatabase DAOs
    @Provides fun provideCompanionDao(db: CompanionDatabase) = db.companionDao()
    @Provides fun provideBondProgressDao(db: CompanionDatabase) = db.bondProgressDao()
    @Provides fun provideEmotionalStateDao(db: CompanionDatabase) = db.emotionalStateDao()
    @Provides fun provideConversationDao(db: CompanionDatabase) = db.conversationDao()
    @Provides fun provideMemorySnapshotDao(db: CompanionDatabase) = db.memorySnapshotDao()
    @Provides fun provideAccessoryDao(db: CompanionDatabase) = db.accessoryDao()
    @Provides fun provideCurrencyDao(db: CompanionDatabase) = db.currencyDao()
    @Provides fun provideAchievementDao(db: CompanionDatabase) = db.achievementDao()
    @Provides fun provideAppUsageDao(db: CompanionDatabase) = db.appUsageDao()
    @Provides fun provideWellnessDao(db: CompanionDatabase) = db.wellnessDao()
    @Provides fun providePreferencesDao(db: CompanionDatabase) = db.preferencesDao()
    @Provides fun provideDialogueDao(db: CompanionDatabase) = db.dialogueDao()

    // Legacy chat DAO
    @Provides fun provideChatMessageDao(db: AppDatabase): ChatMessageDao = db.chatMessageDao()
}
