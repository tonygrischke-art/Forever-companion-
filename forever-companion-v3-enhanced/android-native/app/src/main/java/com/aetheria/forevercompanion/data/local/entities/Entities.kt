package com.aetheria.forevercompanion.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.aetheria.forevercompanion.data.local.converters.Converters
import java.util.UUID

/**
 * ==============================================
 * CORE PET ENTITY
 * ==============================================
 */

@Entity(tableName = "companions")
data class CompanionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val species: CompanionSpecies = CompanionSpecies.NEBULA_KIT,
    val evolutionStage: EvolutionStage = EvolutionStage.WISP,
    val createdAt: Long = System.currentTimeMillis(),
    val lastInteractionTime: Long = System.currentTimeMillis(),
    val totalInteractions: Int = 0,
    val isActive: Boolean = true
)

enum class CompanionSpecies {
    NEBULA_KIT,    // The fox made of star matter
    PIXEL_SPRITE,  // Future species
    CHROME_WISP    // Future species
}

enum class EvolutionStage {
    WISP,              // Days 1-7: Small floating ball
    KIT,               // Days 8-29: Full fox form
    ASTRAL_SENTINEL    // Day 30+: Majestic three-tailed form
}

/**
 * ==============================================
 * BONDING SYSTEM
 * ==============================================
 */

@Entity(tableName = "bond_progress")
data class BondProgressEntity(
    @PrimaryKey val companionId: String,
    val currentLevel: Int = 1,
    val currentXP: Int = 0,
    val xpToNextLevel: Int = 100,
    val relationshipPhase: RelationshipPhase = RelationshipPhase.STRANGER,
    val daysTogether: Int = 0,
    val evolutionPath: EvolutionPath = EvolutionPath.BALANCED,
    val lastLevelUpTime: Long = System.currentTimeMillis()
)

enum class RelationshipPhase {
    STRANGER,   // Days 1-3: Polite, observing
    FRIEND,     // Weeks 1-2: Using "we", inside jokes
    COMPANION   // Month 1+: Deep memory, proactive support
}

enum class EvolutionPath {
    BALANCED,       // Equal across all categories
    CHRONOS,        // Productivity-heavy user
    LYRIC,          // Social/chat-heavy user
    NEON            // Gaming/media-heavy user
}

/**
 * ==============================================
 * EMOTIONAL & MOOD SYSTEM
 * ==============================================
 */

@Entity(tableName = "emotional_states")
data class EmotionalStateEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val companionId: String,
    val mood: PetMood,
    val intensity: Float, // 0.0 to 1.0
    val trigger: MoodTrigger,
    val contextAppPackage: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val duration: Long = 0 // How long this mood lasted
)

enum class PetMood {
    HAPPY,          // Default neutral state
    FOCUS,          // In productivity apps
    EXCITED,        // In social/entertainment apps
    SLEEPY,         // Late at night
    PROTECTIVE,     // Late night + stress scrolling
    CURIOUS,        // Exploring new apps
    PROUD,          // After user achievements
    CONCERNED       // Wellness triggers
}

enum class MoodTrigger {
    APP_CONTEXT,         // Changed due to app switch
    TIME_OF_DAY,         // Changed due to hour
    USER_INTERACTION,    // Changed due to tap/pet
    BATTERY_LEVEL,       // Low battery
    CHARGING_STATE,      // Plugged in
    SCREEN_TIME,         // Too much usage
    ACHIEVEMENT,         // User milestone
    ACCESSORY_EQUIPPED   // Wearing behavior-modifying accessory
}

/**
 * ==============================================
 * CONVERSATION & MEMORY
 * ==============================================
 */

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val companionId: String,
    val role: ConversationRole,
    val message: String,
    val mood: PetMood,
    val contextAppPackage: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val wasDisplayed: Boolean = false,
    val userReaction: UserReaction? = null
)

enum class ConversationRole {
    USER,
    COMPANION
}

enum class UserReaction {
    LIKED,      // User explicitly liked the message
    DISMISSED,  // User swiped away
    IGNORED,    // No interaction
    RESPONDED   // User typed back
}

@Entity(tableName = "memory_snapshots")
data class MemorySnapshotEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val companionId: String,
    val date: String, // YYYY-MM-DD
    val summary: String, // AI-generated summary
    val topAppsUsed: String, // JSON list
    val totalScreenTime: Long,
    val dominantMood: PetMood,
    val significantMoments: String?, // JSON list of highlights
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * ==============================================
 * ACCESSORY SYSTEM
 * ==============================================
 */

@Entity(tableName = "accessories")
data class AccessoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val category: AccessoryCategory,
    val rarity: AccessoryRarity,
    val priceAetherShards: Int,
    val priceStarCredits: Int?,
    val visualAssetId: String, // Reference to Unity model or emoji
    val isUnlocked: Boolean = false,
    val isEquipped: Boolean = false,
    val unlockCondition: String?, // JSON object
    val behaviorModifiers: String?, // JSON object
    val equippedAt: Long? = null,
    val timesUsed: Int = 0
)

enum class AccessoryCategory {
    FUNCTIONAL,     // Changes core behavior
    STYLE,          // Aesthetic + dialogue
    ACHIEVEMENT,    // Milestone unlocks
    INTERACTION,    // Reacts to apps
    LEGENDARY       // Hybrid advanced features
}

enum class AccessoryRarity {
    COMMON,
    RARE,
    EPIC,
    LEGENDARY
}

/**
 * ==============================================
 * CURRENCY SYSTEM
 * ==============================================
 */

@Entity(tableName = "currency_balance")
data class CurrencyBalanceEntity(
    @PrimaryKey val userId: String = "local_user",
    val aetherShards: Int = 0,      // Earned through care
    val starCredits: Int = 0,        // Premium currency
    val lifetimeEarnedShards: Int = 0,
    val lifetimeSpentShards: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "currency_transactions")
data class CurrencyTransactionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val currencyType: CurrencyType,
    val amount: Int,
    val transactionType: TransactionType,
    val reason: String,
    val relatedEntityId: String?, // Accessory ID, achievement ID, etc.
    val timestamp: Long = System.currentTimeMillis()
)

enum class CurrencyType {
    AETHER_SHARDS,
    STAR_CREDITS
}

enum class TransactionType {
    EARNED,
    SPENT,
    GRANTED  // Admin/promo
}

/**
 * ==============================================
 * ACHIEVEMENTS & MILESTONES
 * ==============================================
 */

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val category: AchievementCategory,
    val requirement: Int,
    val currentProgress: Int = 0,
    val isCompleted: Boolean = false,
    val rewardAetherShards: Int,
    val rewardAccessoryId: String?,
    val completedAt: Long? = null
)

enum class AchievementCategory {
    PRODUCTIVITY,   // 50 productivity streaks
    SOCIAL_BALANCE, // 500 balanced social interactions
    WELLNESS,       // 30 days mindful usage
    EXPLORATION,    // Visit locations
    TIME_TOGETHER,  // Hours with pet
    EDUCATION       // Time in learning apps
}

/**
 * ==============================================
 * CONTEXT & APP TRACKING
 * ==============================================
 */

@Entity(tableName = "app_usage_history")
data class AppUsageHistoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val packageName: String,
    val appName: String,
    val category: AppCategory,
    val sessionStart: Long,
    val sessionEnd: Long,
    val duration: Long,
    val petMoodDuring: PetMood
)

enum class AppCategory {
    PRODUCTIVITY,
    SOCIAL,
    GAMING,
    UTILITY,
    EDUCATION,
    ENTERTAINMENT,
    HEALTH_FITNESS,
    UNKNOWN
}

/**
 * ==============================================
 * WELLNESS TRACKING
 * ==============================================
 */

@Entity(tableName = "wellness_events")
data class WellnessEventEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val eventType: WellnessEventType,
    val triggerTime: Long,
    val userResponse: WellnessResponse?,
    val contextData: String?, // JSON - screen time, scroll pattern, etc.
    val interventionMessage: String?,
    val wasEffective: Boolean = false
)

enum class WellnessEventType {
    LATE_NIGHT_CONCERN_L1,   // Level 1: Hint
    LATE_NIGHT_CONCERN_L2,   // Level 2: Nudge
    LATE_NIGHT_CONCERN_L3,   // Level 3: Deep concern
    BREAK_REMINDER,
    HYDRATION_REMINDER,
    POSTURE_CHECK,
    STRESS_SCROLLING_DETECTED
}

enum class WellnessResponse {
    ACCEPTED,      // User took the suggestion
    POSTPONED,     // "5 more minutes"
    DISMISSED,     // Closed notification
    IGNORED        // No response
}

/**
 * ==============================================
 * USER PREFERENCES
 * ==============================================
 */

@Entity(tableName = "user_preferences")
data class UserPreferencesEntity(
    @PrimaryKey val key: String,
    val value: String,
    val lastUpdated: Long = System.currentTimeMillis()
)

// Common preference keys
object PreferenceKeys {
    const val USER_NAME = "user_name"
    const val WELLNESS_REMINDERS_ENABLED = "wellness_enabled"
    const val LATE_NIGHT_CONCERN_ENABLED = "late_night_concern"
    const val LATE_NIGHT_THRESHOLD_HOUR = "late_night_hour" // Default: 23
    const val OVERLAY_SIZE = "overlay_size" // small, medium, large
    const val RENDER_MODE = "render_mode" // 2d, 3d
    const val HAPTIC_FEEDBACK_ENABLED = "haptic_feedback"
    const val SOUND_EFFECTS_ENABLED = "sound_effects"
    const val DIALOGUE_FREQUENCY = "dialogue_frequency" // low, medium, high
    const val PROACTIVE_MODE_ENABLED = "proactive_mode"
}

/**
 * ==============================================
 * DIALOGUE SYSTEM
 * ==============================================
 */

@Entity(tableName = "dialogues")
data class DialogueEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val category: DialogueCategory,
    val mood: PetMood,
    val relationshipPhase: RelationshipPhase,
    val text: String,
    val usageCount: Int = 0,
    val lastUsed: Long? = null,
    val isContextual: Boolean = false, // Requires specific context
    val contextTags: String? = null // JSON list: ["late_night", "spotify", etc.]
)

enum class DialogueCategory {
    GREETING,
    FAREWELL,
    FOCUS_MODE,
    SOCIAL_MODE,
    LATE_NIGHT,
    ACHIEVEMENT,
    WELLNESS,
    PLAYFUL,
    CONCERNED,
    PROUD,
    MEMORY_RECALL
}
