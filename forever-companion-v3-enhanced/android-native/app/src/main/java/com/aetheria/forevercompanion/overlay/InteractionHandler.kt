package com.aetheria.forevercompanion.overlay

import com.aetheria.forevercompanion.data.local.dao.CompanionDao
import com.aetheria.forevercompanion.data.local.dao.ConversationDao
import com.aetheria.forevercompanion.data.local.dao.DialogueDao
import com.aetheria.forevercompanion.data.local.entities.ConversationEntity
import com.aetheria.forevercompanion.data.local.entities.ConversationRole
import com.aetheria.forevercompanion.data.local.entities.DialogueCategory
import com.aetheria.forevercompanion.data.local.entities.PetMood
import com.aetheria.forevercompanion.data.local.entities.RelationshipPhase
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InteractionHandler @Inject constructor(
    private val petStateManager: PetStateManager,
    private val companionDao: CompanionDao,
    private val conversationDao: ConversationDao,
    private val dialogueDao: DialogueDao
) {
    suspend fun handleTap(mood: PetMood, context: String?): String {
        val companionId = petStateManager.currentState.value.companion?.id
        val phase = petStateManager.currentState.value.bondProgress?.relationshipPhase
            ?: RelationshipPhase.STRANGER

        val category = when (mood) {
            PetMood.FOCUS -> DialogueCategory.FOCUS_MODE
            PetMood.EXCITED -> DialogueCategory.PLAYFUL
            PetMood.SLEEPY -> DialogueCategory.LATE_NIGHT
            PetMood.CONCERNED -> DialogueCategory.CONCERNED
            PetMood.PROUD -> DialogueCategory.PROUD
            else -> DialogueCategory.GREETING
        }

        // FIX: Pass allowed phases list instead of relying on broken enum ordering
        val dialogue = dialogueDao.getRandom(category, mood, phase.allowedPhases())
            ?: dialogueDao.getRandomByCategory(DialogueCategory.GREETING)

        val message = dialogue?.text ?: getFallbackMessage(mood)

        dialogue?.let { dialogueDao.markUsed(it.id) }

        if (companionId != null) {
            conversationDao.insert(
                ConversationEntity(
                    companionId = companionId,
                    role = ConversationRole.COMPANION,
                    message = message,
                    mood = mood,
                    contextAppPackage = context,
                    wasDisplayed = true
                )
            )
            companionDao.incrementInteractions(companionId)
        }

        Timber.d("Tap handled: $message")
        return message
    }

    suspend fun recordInteraction(type: String) {
        val companionId = petStateManager.currentState.value.companion?.id ?: return
        Timber.d("Interaction recorded: $type for companion $companionId")
    }

    private fun getFallbackMessage(mood: PetMood): String = when (mood) {
        PetMood.HAPPY -> "Hey there! ✨"
        PetMood.FOCUS -> "You've got this! 💪"
        PetMood.EXCITED -> "Yay! Let's go! 🎉"
        PetMood.SLEEPY -> "Getting late… 🌙"
        PetMood.PROTECTIVE -> "I'm watching over you 🛡️"
        PetMood.CURIOUS -> "Ooh, what's this? 🔍"
        PetMood.PROUD -> "Look at you go! ⭐"
        PetMood.CONCERNED -> "Hey, you okay? 💙"
    }
}

/**
 * Returns all relationship phases allowed up to and including the current phase.
 * This replaces the broken "phase <= :phase" Room enum comparison.
 */
fun RelationshipPhase.allowedPhases(): List<RelationshipPhase> = when (this) {
    RelationshipPhase.STRANGER -> listOf(RelationshipPhase.STRANGER)
    RelationshipPhase.FRIEND -> listOf(RelationshipPhase.STRANGER, RelationshipPhase.FRIEND)
    RelationshipPhase.COMPANION -> RelationshipPhase.entries.toList()
}
