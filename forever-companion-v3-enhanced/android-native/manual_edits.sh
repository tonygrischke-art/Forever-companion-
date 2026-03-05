#!/bin/bash
# ╔══════════════════════════════════════════════════════════════════╗
# ║   FOREVER COMPANION — Manual Edits Script                        ║
# ║   Creates AppDatabase.kt + DatabaseModule.kt from scratch        ║
# ║   Run from: ~/forever-companion-v3-enhanced/android-native       ║
# ║   Usage: bash manual_edits.sh                                    ║
# ╚══════════════════════════════════════════════════════════════════╝

set -e

PROJECT_ROOT="$(pwd)"
APP_SRC="$PROJECT_ROOT/app/src/main/java/com/aetheria/forevercompanion"

echo ""
echo "🔧 Running manual edits for Forever Companion..."
echo ""

mkdir -p "$APP_SRC/data"
mkdir -p "$APP_SRC/di"

# ════════════════════════════════════════════════════════════════════
# FILE 1: AppDatabase.kt  (created from scratch)
# ════════════════════════════════════════════════════════════════════
cat > "$APP_SRC/data/AppDatabase.kt" << 'KOTLIN'
package com.aetheria.forevercompanion.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.aetheria.forevercompanion.data.dao.ChatMessageDao
import com.aetheria.forevercompanion.data.entity.ChatMessageEntity

@Database(
    entities = [
        ChatMessageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "forever_companion_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
KOTLIN
echo "✅ AppDatabase.kt created"

# ════════════════════════════════════════════════════════════════════
# FILE 2: DatabaseModule.kt  (Hilt provider for DB + DAOs)
# ════════════════════════════════════════════════════════════════════
cat > "$APP_SRC/di/DatabaseModule.kt" << 'KOTLIN'
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
KOTLIN
echo "✅ DatabaseModule.kt created"

# ════════════════════════════════════════════════════════════════════
# VERIFY all required files exist
# ════════════════════════════════════════════════════════════════════
echo ""
echo "🔍 Verifying all files are in place..."
echo ""

FILES=(
    "$APP_SRC/data/AppDatabase.kt"
    "$APP_SRC/data/entity/ChatMessageEntity.kt"
    "$APP_SRC/data/dao/ChatMessageDao.kt"
    "$APP_SRC/di/DatabaseModule.kt"
    "$APP_SRC/di/GeminiModule.kt"
    "$APP_SRC/service/GeminiService.kt"
    "$APP_SRC/repository/GeminiRepository.kt"
    "$APP_SRC/viewmodel/ChatViewModel.kt"
    "$APP_SRC/ui/chat/ChatScreen.kt"
    "$APP_SRC/bridge/UnityBridge.kt"
)

ALL_GOOD=true
for f in "${FILES[@]}"; do
    if [ -f "$f" ]; then
        echo "  ✅ $(basename $f)"
    else
        echo "  ❌ MISSING: $f"
        ALL_GOOD=false
    fi
done

echo ""
if [ "$ALL_GOOD" = true ]; then
    echo "════════════════════════════════════════════════════════════"
    echo "  🎉 All files present! You're ready to build."
    echo "════════════════════════════════════════════════════════════"
    echo ""
    echo "  Next step — build the APK:"
    echo "  ./gradlew assembleDebug"
    echo ""
else
    echo "════════════════════════════════════════════════════════════"
    echo "  ⚠️  Some files are missing."
    echo "  Run setup_gemini_unity.sh first, then re-run this script."
    echo "════════════════════════════════════════════════════════════"
fi
