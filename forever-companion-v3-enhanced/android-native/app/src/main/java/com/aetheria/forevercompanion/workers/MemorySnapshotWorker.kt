package com.aetheria.forevercompanion.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aetheria.forevercompanion.data.local.dao.AppUsageDao
import com.aetheria.forevercompanion.data.local.dao.CompanionDao
import com.aetheria.forevercompanion.data.local.dao.EmotionalStateDao
import com.aetheria.forevercompanion.data.local.dao.MemorySnapshotDao
import com.aetheria.forevercompanion.data.local.entities.MemorySnapshotEntity
import com.aetheria.forevercompanion.data.local.entities.PetMood
import com.google.gson.Gson
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltWorker
class MemorySnapshotWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val companionDao: CompanionDao,
    private val appUsageDao: AppUsageDao,
    private val emotionalStateDao: EmotionalStateDao,
    private val memorySnapshotDao: MemorySnapshotDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Timber.d("MemorySnapshotWorker: Creating daily snapshot")
        try {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val oneDayAgo = System.currentTimeMillis() - 86_400_000L

            // Get companion
            var companionId: String? = null
            companionDao.getActiveCompanion().collect { c ->
                companionId = c?.id
                return@collect
            }
            val id = companionId ?: return Result.success()

            // Get today's usage
            val usageToday = appUsageDao.getSince(oneDayAgo)
            val topApps = usageToday.sortedByDescending { it.duration }
                .take(5).map { it.packageName }
            val totalScreenTime = usageToday.sumOf { it.duration }

            // Determine dominant mood
            val moodSince = emotionalStateDao.getSince(id, oneDayAgo)
            val dominantMood = moodSince.groupBy { it.mood }
                .maxByOrNull { it.value.size }?.key ?: PetMood.HAPPY

            val snapshot = MemorySnapshotEntity(
                companionId = id,
                date = today,
                summary = "Day with $dominantMood mood. Screen time: ${totalScreenTime / 60_000}min.",
                topAppsUsed = Gson().toJson(topApps),
                totalScreenTime = totalScreenTime,
                dominantMood = dominantMood,
            significantMoments = "[]"
            )

            memorySnapshotDao.insert(snapshot)
            Timber.d("MemorySnapshotWorker: Snapshot saved for $today")
            return Result.success()
        } catch (e: Exception) {
            Timber.e(e, "MemorySnapshotWorker failed")
            return Result.retry()
        }
    }
}
