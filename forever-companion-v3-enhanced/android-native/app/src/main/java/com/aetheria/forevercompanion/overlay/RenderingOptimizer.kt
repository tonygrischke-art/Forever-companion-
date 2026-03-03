package com.aetheria.forevercompanion.overlay

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RenderingOptimizer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    enum class RenderMode { HIGH, BALANCED, LOW_POWER }

    fun getCurrentMode(): RenderMode {
        val batteryLevel = getBatteryLevel()
        return when {
            batteryLevel < 15 -> RenderMode.LOW_POWER
            batteryLevel < 30 -> RenderMode.BALANCED
            else -> RenderMode.HIGH
        }
    }

    fun getTargetFps(): Int = when (getCurrentMode()) {
        RenderMode.HIGH -> 60
        RenderMode.BALANCED -> 30
        RenderMode.LOW_POWER -> 15
    }

    fun shouldRenderParticles(): Boolean = getCurrentMode() == RenderMode.HIGH

    private fun getBatteryLevel(): Int {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) (level * 100 / scale) else 100
    }
}
