package com.aetheria.forevercompanion.context

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import com.aetheria.forevercompanion.data.local.entities.AppCategory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppContextMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _currentApp = MutableStateFlow("android")
    val currentApp: StateFlow<String> = _currentApp

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    // Known app-to-category mappings
    private val categoryMap = mapOf(
        "com.google.android.gm" to AppCategory.PRODUCTIVITY,
        "com.microsoft.teams" to AppCategory.PRODUCTIVITY,
        "com.slack" to AppCategory.PRODUCTIVITY,
        "com.notion.id" to AppCategory.PRODUCTIVITY,
        "com.todoist" to AppCategory.PRODUCTIVITY,
        "com.instagram.android" to AppCategory.SOCIAL,
        "com.twitter.android" to AppCategory.SOCIAL,
        "com.snapchat.android" to AppCategory.SOCIAL,
        "com.discord" to AppCategory.SOCIAL,
        "com.facebook.katana" to AppCategory.SOCIAL,
        "com.miHoYo.GenshinImpact" to AppCategory.GAMING,
        "com.roblox.client" to AppCategory.GAMING,
        "com.mojang.minecraftpe" to AppCategory.GAMING,
        "com.netflix.mediaclient" to AppCategory.ENTERTAINMENT,
        "com.spotify.music" to AppCategory.ENTERTAINMENT,
        "com.youtube.android" to AppCategory.ENTERTAINMENT,
        "com.google.android.youtube" to AppCategory.ENTERTAINMENT,
        "org.khanacademy.android" to AppCategory.EDUCATION,
        "com.duolingo" to AppCategory.EDUCATION,
        "com.samsung.android.dialer" to AppCategory.UTILITY,
        "com.android.dialer" to AppCategory.UTILITY,
        "com.android.settings" to AppCategory.UTILITY,
    )

    fun startMonitoring() {
        scope.launch {
            while (true) {
                val foregroundApp = getForegroundApp()
                if (foregroundApp != null && foregroundApp != _currentApp.value) {
                    _currentApp.value = foregroundApp
                    Timber.d("App context: $foregroundApp")
                }
                delay(2000) // Poll every 2 seconds
            }
        }
    }

    private fun getForegroundApp(): String? {
        val usm = usageStatsManager ?: return null
        val now = System.currentTimeMillis()
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 5000, now)
        return stats?.maxByOrNull { it.lastTimeUsed }?.packageName
    }

    fun getAppCategory(packageName: String): AppCategory {
        // Check our known map first
        categoryMap[packageName]?.let { return it }

        // Try system category
        return try {
            val info = context.packageManager.getApplicationInfo(packageName, 0)
            when (context.packageManager.getApplicationLabel(info).toString().lowercase()) {
                else -> AppCategory.UNKNOWN
            }
        } catch (e: PackageManager.NameNotFoundException) {
            AppCategory.UNKNOWN
        }
    }

    fun getAppName(packageName: String): String {
        return try {
            val info = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            packageName
        }
    }
}
