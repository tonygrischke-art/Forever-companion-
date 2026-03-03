package com.aetheria.forevercompanion.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aetheria.forevercompanion.data.local.dao.BondProgressDao
import com.aetheria.forevercompanion.data.local.dao.CompanionDao
import com.aetheria.forevercompanion.data.local.entities.BondProgressEntity
import com.aetheria.forevercompanion.data.local.entities.RelationshipPhase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
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
        try {
            // Passive XP for time spent together (awarded every 6 hours)
            val companion = companionDao.getById(getActiveCompanionId() ?: return Result.success())
                ?: return Result.success()

            bondProgressDao.addXP(companion.id, PASSIVE_XP_PER_CYCLE)

            // Check if we should level up
            val progress = bondProgressDao.getByCompanionId(companion.id)
            Timber.d("BondProgressWorker: Awarded $PASSIVE_XP_PER_CYCLE XP to ${companion.name}")
            return Result.success()
        } catch (e: Exception) {
            Timber.e(e, "BondProgressWorker failed")
            return Result.retry()
        }
    }

    private suspend fun getActiveCompanionId(): String? {
        // Retrieve via flow's first emission
        var id: String? = null
        companionDao.getActiveCompanion().collect { companion ->
            id = companion?.id
            return@collect
        }
        return id
    }

    companion object {
        const val PASSIVE_XP_PER_CYCLE = 10
    }
}
