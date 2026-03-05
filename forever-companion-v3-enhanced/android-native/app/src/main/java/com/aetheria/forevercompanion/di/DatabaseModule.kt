package com.aetheria.forevercompanion.di

import android.content.Context
import androidx.room.Room
import com.aetheria.forevercompanion.data.AppDatabase
import com.aetheria.forevercompanion.data.dao.ChatMessageDao
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "forever_companion_db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideChatMessageDao(db: AppDatabase): ChatMessageDao = db.chatMessageDao()
}
