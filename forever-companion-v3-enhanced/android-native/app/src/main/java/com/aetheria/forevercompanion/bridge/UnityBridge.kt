package com.aetheria.forevercompanion.bridge

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnityBridge @Inject constructor() {

    companion object {
        private const val TAG = "UnityBridge"
        private const val UNITY_OBJECT = "AetherController"
        const val MOOD_HAPPY    = "happy"
        const val MOOD_SAD      = "sad"
        const val MOOD_EXCITED  = "excited"
        const val MOOD_THINKING = "thinking"
        const val MOOD_IDLE     = "idle"
    }

    fun onGeminiResponse(rawText: String) {
        val mood = parseMood(rawText)
        val cleanText = rawText.replace(Regex("\\[MOOD:[a-z]+\\]"), "").trim()
        sendToUnity(UNITY_OBJECT, "OnAetherSpeak", cleanText)
        sendToUnity(UNITY_OBJECT, "OnMoodChange", mood)
        Log.d(TAG, "Aether → Unity | mood=$mood")
    }

    fun onUserMessage(text: String) {
        sendToUnity(UNITY_OBJECT, "OnUserMessage", text)
        sendToUnity(UNITY_OBJECT, "OnMoodChange", MOOD_THINKING)
    }

    fun setIdleState() = sendToUnity(UNITY_OBJECT, "OnMoodChange", MOOD_IDLE)

    private fun parseMood(text: String): String =
        Regex("\\[MOOD:([a-z]+)\\]").find(text)?.groupValues?.get(1) ?: MOOD_IDLE

    private fun sendToUnity(gameObject: String, method: String, message: String) {
        try {
            val unityPlayer = Class.forName("com.unity3d.player.UnityPlayer")
            val sendMsg = unityPlayer.getMethod(
                "UnitySendMessage",
                String::class.java, String::class.java, String::class.java
            )
            sendMsg.invoke(null, gameObject, method, message)
        } catch (e: ClassNotFoundException) {
            Log.w(TAG, "Unity not loaded — skipping $method($message)")
        } catch (e: Exception) {
            Log.e(TAG, "UnitySendMessage failed: ${e.message}")
        }
    }
}
