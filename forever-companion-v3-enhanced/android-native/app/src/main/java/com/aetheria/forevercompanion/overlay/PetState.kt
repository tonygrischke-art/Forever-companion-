package com.aetheria.forevercompanion.overlay

import com.aetheria.forevercompanion.data.local.entities.CompanionEntity
import com.aetheria.forevercompanion.data.local.entities.BondProgressEntity
import com.aetheria.forevercompanion.data.local.entities.PetMood
import com.aetheria.forevercompanion.data.local.entities.EvolutionStage

data class PetState(
    val companion: CompanionEntity? = null,
    val bondProgress: BondProgressEntity? = null,
    val currentMood: PetMood = PetMood.HAPPY,
    val evolutionStage: EvolutionStage = EvolutionStage.WISP,
    val isLoaded: Boolean = false
)
