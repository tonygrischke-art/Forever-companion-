package com.aetheria.forevercompanion.data.local.converters

import androidx.room.TypeConverter
import com.aetheria.forevercompanion.data.local.entities.*

class Converters {
    @TypeConverter fun moodToStr(v: PetMood): String = v.name
    @TypeConverter fun strToMood(v: String): PetMood = PetMood.valueOf(v)

    @TypeConverter fun moodTriggerToStr(v: MoodTrigger): String = v.name
    @TypeConverter fun strToMoodTrigger(v: String): MoodTrigger = MoodTrigger.valueOf(v)

    @TypeConverter fun specToStr(v: CompanionSpecies): String = v.name
    @TypeConverter fun strToSpec(v: String): CompanionSpecies = CompanionSpecies.valueOf(v)

    @TypeConverter fun evolToStr(v: EvolutionStage): String = v.name
    @TypeConverter fun strToEvol(v: String): EvolutionStage = EvolutionStage.valueOf(v)

    @TypeConverter fun relToStr(v: RelationshipPhase): String = v.name
    @TypeConverter fun strToRel(v: String): RelationshipPhase = RelationshipPhase.valueOf(v)

    @TypeConverter fun evolPathToStr(v: EvolutionPath): String = v.name
    @TypeConverter fun strToEvolPath(v: String): EvolutionPath = EvolutionPath.valueOf(v)

    @TypeConverter fun conRoleToStr(v: ConversationRole): String = v.name
    @TypeConverter fun strToConRole(v: String): ConversationRole = ConversationRole.valueOf(v)

    @TypeConverter fun reactToStr(v: UserReaction?): String? = v?.name
    @TypeConverter fun strToReact(v: String?): UserReaction? = v?.let { UserReaction.valueOf(it) }

    @TypeConverter fun accCatToStr(v: AccessoryCategory): String = v.name
    @TypeConverter fun strToAccCat(v: String): AccessoryCategory = AccessoryCategory.valueOf(v)

    @TypeConverter fun accRarToStr(v: AccessoryRarity): String = v.name
    @TypeConverter fun strToAccRar(v: String): AccessoryRarity = AccessoryRarity.valueOf(v)

    @TypeConverter fun curTypeToStr(v: CurrencyType): String = v.name
    @TypeConverter fun strToCurType(v: String): CurrencyType = CurrencyType.valueOf(v)

    @TypeConverter fun txTypeToStr(v: TransactionType): String = v.name
    @TypeConverter fun strToTxType(v: String): TransactionType = TransactionType.valueOf(v)

    @TypeConverter fun achCatToStr(v: AchievementCategory): String = v.name
    @TypeConverter fun strToAchCat(v: String): AchievementCategory = AchievementCategory.valueOf(v)

    @TypeConverter fun appCatToStr(v: AppCategory): String = v.name
    @TypeConverter fun strToAppCat(v: String): AppCategory = AppCategory.valueOf(v)

    @TypeConverter fun wellTypeToStr(v: WellnessEventType): String = v.name
    @TypeConverter fun strToWellType(v: String): WellnessEventType = WellnessEventType.valueOf(v)

    @TypeConverter fun wellRespToStr(v: WellnessResponse?): String? = v?.name
    @TypeConverter fun strToWellResp(v: String?): WellnessResponse? = v?.let { WellnessResponse.valueOf(it) }

    @TypeConverter fun dlgCatToStr(v: DialogueCategory): String = v.name
    @TypeConverter fun strToDlgCat(v: String): DialogueCategory = DialogueCategory.valueOf(v)
}
