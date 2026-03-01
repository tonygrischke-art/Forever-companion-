package com.aetheria.forevercompanion.data.local.di

import android.content.Context
import androidx.room.Room
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

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CompanionDatabase =
        Room.databaseBuilder(context, CompanionDatabase::class.java, "forever_companion_db")
            .fallbackToDestructiveMigration()
            .build()

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
}
