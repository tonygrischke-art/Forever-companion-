package com.aetheria.forevercompanion.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aetheria.forevercompanion.data.local.dao.BondProgressDao
import com.aetheria.forevercompanion.data.local.dao.CompanionDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber

@HiltWorker
class BondProgressWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val companionDao: CompanionDao,
    private val bondProgressDao: BondProgressDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Timber.d("BondProgressWorker: Running")
        return try {
            // FIX: Use firstOrNull() instead of collect() — collect() never returns on a Flow<T>
            val companion = companionDao.getActiveCompanion().firstOrNull()
                ?: return Result.success()

            bondProgressDao.addXP(companion.id, PASSIVE_XP_PER_CYCLE)
            bondProgressDao.incrementDays(companion.id)

            Timber.d("BondProgressWorker: Awarded $PASSIVE_XP_PER_CYCLE XP to ${companion.name}")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "BondProgressWorker failed")
            Result.retry()
        }
    }

    companion object {
        const val PASSIVE_XP_PER_CYCLE = 10
    }
}
