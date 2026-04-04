package com.aetheria.forevercompanion.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aetheria.forevercompanion.data.dao.ChatMessageDao
import com.aetheria.forevercompanion.data.entity.ChatMessageEntity

// NOTE: This legacy database is kept only to support ChatViewModel/GeminiRepository.
// It uses a distinct filename "chat_legacy_db" to avoid collision with CompanionDatabase.
@Database(
    entities = [ChatMessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatMessageDao(): ChatMessageDao
}
