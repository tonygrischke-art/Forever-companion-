package com.aetheria.forevercompanion.bridge.disabled

import android.content.Context
import android.util.Log

/**
 * Unity bridge stub — Unity SDK not included in this build.
 * All methods are no-ops to allow compilation without Unity libraries.
 */
class UnityBridge(private val context: Context) {

    companion object {
        private const val TAG = "UnityBridge"
        const val DISABLED = true
    }

    fun initialize(): Boolean {
        Log.d(TAG, "UnityBridge disabled - Unity SDK not present")
        return false
    }

    fun sendMessage(gameObject: String, method: String, message: String) {
        Log.d(TAG, "UnityBridge disabled: $gameObject.$method($message)")
    }

    fun setEmotion(emotion: String) {
        Log.d(TAG, "UnityBridge disabled: setEmotion($emotion)")
    }

    fun triggerAnimation(animation: String) {
        Log.d(TAG, "UnityBridge disabled: triggerAnimation($animation)")
    }

    fun updatePetState(state: String, value: Float) {
        Log.d(TAG, "UnityBridge disabled: updatePetState($state, $value)")
    }

    fun destroy() {
        Log.d(TAG, "UnityBridge disabled: destroy()")
    }
}
