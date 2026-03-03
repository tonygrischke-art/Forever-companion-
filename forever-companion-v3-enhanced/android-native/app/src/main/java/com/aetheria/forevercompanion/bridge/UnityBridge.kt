package com.aetheria.forevercompanion.bridge

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.aetheria.forevercompanion.ai.EmotionalStateDelta
import com.unity3d.player.UnityPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.json.JSONObject
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Native-first bridge: Core AI/Memory logic stays in Android.
 * Unity receives only visual deltas (emotions, animations).
 */
class UnityBridge private constructor(private val context: Context) {

    private var unityPlayer: UnityPlayer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val messageQueue = ConcurrentLinkedQueue<UnityMessage>()

    companion object {
        @Volatile private var instance: UnityBridge? = null

        fun getInstance(context: Context): UnityBridge =
            instance ?: synchronized(this) {
                instance ?: UnityBridge(context.applicationContext).also { instance = it }
            }

        @JvmStatic
        fun receiveFromUnity(jsonMessage: String) {
            instance?.handleUnityMessage(jsonMessage)
        }
    }

    fun initializeUnity(): UnityPlayer? {
        if (unityPlayer != null) return unityPlayer
        unityPlayer = UnityPlayer(context)
        sendInitialState()
        return unityPlayer
    }

    fun sendDelta(delta: EmotionalStateDelta) {
        val message = when (delta) {
            is EmotionalStateDelta.MoodChange -> UnityMessage(
                type = "MOOD_DELTA",
                payload = JSONObject().apply {
                    put("mood", delta.newMood)
                    put("intensity", delta.intensity)
                    put("trigger", delta.triggerAnimation)
                }.toString()
            )
            is EmotionalStateDelta.AnimationTrigger -> UnityMessage(
                type = "ANIM_TRIGGER",
                payload = JSONObject().apply {
                    put("animation", delta.animationName)
                    put("layer", delta.layer)
                }.toString()
            )
            is EmotionalStateDelta.AvatarUpdate -> UnityMessage(
                type = "AVATAR_UPDATE",
                payload = JSONObject().apply {
                    put("glbUrl", delta.avatarUrl)
                    put("outfitId", delta.currentOutfit)
                }.toString()
            )
            is EmotionalStateDelta.LookTarget -> UnityMessage(
                type = "LOOK_AT",
                payload = JSONObject().apply {
                    put("x", delta.x)
                    put("y", delta.y)
                    put("z", delta.z)
                }.toString()
            )
        }
        queueMessage(message)
    }

    private fun handleUnityMessage(json: String) {
        try {
            val obj = JSONObject(json)
            when (obj.getString("action")) {
                "REQUEST_PURCHASE" -> {
                    val productId = obj.getString("productId")
                    (context as? PurchaseRequestListener)?.onPurchaseRequested(productId)
                }
                "UNITY_READY" -> sendInitialState()
            }
        } catch (e: Exception) {
            android.util.Log.e("UnityBridge", "Error parsing Unity message: ${e.message}")
        }
    }

    private fun sendInitialState() {
        // TODO: Load companion state from Room and push to Unity via sendDelta()
        android.util.Log.d("UnityBridge", "sendInitialState() - awaiting Room integration")
    }

    private fun queueMessage(message: UnityMessage) {
        messageQueue.offer(message)
        processQueue()
    }

    private fun processQueue() {
        unityPlayer ?: return
        while (messageQueue.isNotEmpty()) {
            val msg = messageQueue.poll() ?: break
            val json = JSONObject().apply {
                put("type", msg.type)
                put("payload", msg.payload)
            }.toString()
            UnityPlayer.UnitySendMessage("AndroidBridge", "ReceiveDelta", json)
        }
    }

    fun release() {
        unityPlayer?.quit()
        unityPlayer = null
        scope.cancel()
        instance = null
    }

    interface PurchaseRequestListener {
        fun onPurchaseRequested(productId: String)
    }

    data class UnityMessage(val type: String, val payload: String)
}
