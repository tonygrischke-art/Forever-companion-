package com.aetheria.forevercompanion.di

import com.aetheria.forevercompanion.bridge.UnityBridge
import com.aetheria.forevercompanion.data.dao.ChatMessageDao
import com.aetheria.forevercompanion.repository.GeminiRepository
import com.aetheria.forevercompanion.service.GeminiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GeminiModule {

    @Provides @Singleton
    fun provideGeminiService(): GeminiService = GeminiService()

    @Provides @Singleton
    fun provideUnityBridge(): UnityBridge = UnityBridge()

    @Provides @Singleton
    fun provideGeminiRepository(
        geminiService: GeminiService,
        chatMessageDao: ChatMessageDao
    ): GeminiRepository = GeminiRepository(geminiService, chatMessageDao)
}
