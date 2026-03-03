package com.aetheria.forevercompanion

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.aetheria.forevercompanion.workers.BondProgressWorker
import com.aetheria.forevercompanion.workers.MemorySnapshotWorker
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Forever Companion Application
 * 
 * The Aetheria Project V3 - Main application entry point
 * Initializes all core systems for the hybrid AI companion
 */
@HiltAndroidApp
class ForeverCompanionApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        
        // Initialize logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        Timber.d("Forever Companion V3 - The Aetheria Project - Initializing...")
        
        // Create notification channels
        createNotificationChannels()
        
        // Schedule background workers
        scheduleBackgroundWork()
        
        Timber.d("Application initialization complete")
    }

    override val workManagerConfiguration: Configuration
        get() {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.ERROR)
            .build()
    }

    /**
     * Create notification channels for different pet notifications
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            
            // Companion Presence Channel - For always-visible notification
            val presenceChannel = NotificationChannel(
                CHANNEL_COMPANION_PRESENCE,
                "Companion Presence",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows your companion is active and available"
                setShowBadge(false)
                enableVibration(false)
                enableLights(false)
            }
            
            // Wellness Reminders Channel - For late-night concern, breaks, etc.
            val wellnessChannel = NotificationChannel(
                CHANNEL_WELLNESS,
                "Wellness Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Gentle reminders from your companion about your wellbeing"
                setShowBadge(true)
                enableVibration(true)
            }
            
            // Bond Milestones Channel - For evolution, level ups, achievements
            val milestonesChannel = NotificationChannel(
                CHANNEL_MILESTONES,
                "Bond Milestones",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Important moments in your journey together"
                setShowBadge(true)
                enableVibration(true)
                enableLights(true)
            }
            
            // Memory Snapshots Channel - Daily summaries
            val memoriesChannel = NotificationChannel(
                CHANNEL_MEMORIES,
                "Memory Snapshots",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Daily moments captured by your companion"
                setShowBadge(false)
            }
            
            notificationManager.createNotificationChannels(listOf(
                presenceChannel,
                wellnessChannel,
                milestonesChannel,
                memoriesChannel
            ))
            
            Timber.d("Notification channels created successfully")
        }
    }

    /**
     * Schedule periodic background work for:
     * - Bond progress calculation
     * - Memory snapshot creation
     * - Evolution checks
     */
    private fun scheduleBackgroundWork() {
        val workManager = WorkManager.getInstance(this)
        
        // Bond Progress Worker - Runs every 6 hours
        val bondProgressWork = PeriodicWorkRequestBuilder<BondProgressWorker>(
            6, TimeUnit.HOURS
        ).build()
        
        workManager.enqueueUniquePeriodicWork(
            "bond_progress_calculation",
            ExistingPeriodicWorkPolicy.KEEP,
            bondProgressWork
        )
        
        // Memory Snapshot Worker - Runs once daily at midnight
        val memorySnapshotWork = PeriodicWorkRequestBuilder<MemorySnapshotWorker>(
            24, TimeUnit.HOURS
        ).build()
        
        workManager.enqueueUniquePeriodicWork(
            "daily_memory_snapshot",
            ExistingPeriodicWorkPolicy.KEEP,
            memorySnapshotWork
        )
        
        Timber.d("Background workers scheduled successfully")
    }

    companion object {
        // Notification Channel IDs
        const val CHANNEL_COMPANION_PRESENCE = "companion_presence"
        const val CHANNEL_WELLNESS = "wellness_reminders"
        const val CHANNEL_MILESTONES = "bond_milestones"
        const val CHANNEL_MEMORIES = "memory_snapshots"
        
        // Notification IDs
        const val NOTIFICATION_ID_FOREGROUND = 1001
        const val NOTIFICATION_ID_WELLNESS = 1002
        const val NOTIFICATION_ID_MILESTONE = 1003
        const val NOTIFICATION_ID_MEMORY = 1004
    }
}
