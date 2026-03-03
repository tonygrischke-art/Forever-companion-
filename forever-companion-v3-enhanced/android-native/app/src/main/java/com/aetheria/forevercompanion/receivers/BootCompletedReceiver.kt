package com.aetheria.forevercompanion.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aetheria.forevercompanion.overlay.CompanionOverlayService
import timber.log.Timber

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Timber.d("BootCompletedReceiver: Device booted, starting companion")
            val serviceIntent = Intent(context, CompanionOverlayService::class.java).apply {
                action = CompanionOverlayService.ACTION_START_COMPANION
            }
            context.startForegroundService(serviceIntent)
        }
    }
}
