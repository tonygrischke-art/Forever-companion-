package com.aetheria.forevercompanion.ai

sealed class EmotionalStateDelta {
    data class MoodChange(
        val newMood: String,
        val intensity: Float,
        val triggerAnimation: String
    ) : EmotionalStateDelta()

    data class AnimationTrigger(
        val animationName: String,
        val layer: Int = 0
    ) : EmotionalStateDelta()

    data class AvatarUpdate(
        val avatarUrl: String,
        val currentOutfit: String?
    ) : EmotionalStateDelta()

    data class LookTarget(
        val x: Float,
        val y: Float,
        val z: Float
    ) : EmotionalStateDelta()
}
