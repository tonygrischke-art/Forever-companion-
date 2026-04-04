package com.aetheria.forevercompanion.overlay

import com.aetheria.forevercompanion.data.local.dao.BondProgressDao
import com.aetheria.forevercompanion.data.local.dao.CompanionDao
import com.aetheria.forevercompanion.data.local.dao.EmotionalStateDao
import com.aetheria.forevercompanion.data.local.entities.EmotionalStateEntity
import com.aetheria.forevercompanion.data.local.entities.MoodTrigger
import com.aetheria.forevercompanion.data.local.entities.PetMood
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PetStateManager @Inject constructor(
    private val companionDao: CompanionDao,
    private val bondProgressDao: BondProgressDao,
    private val emotionalStateDao: EmotionalStateDao
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _currentState = MutableStateFlow(PetState())
    val currentState: StateFlow<PetState> = _currentState

    init {
        scope.launch {
            // FIX: Use flatMapLatest instead of nested collect() to avoid memory leaks
            // and ensure bond progress updates are always tied to the latest companion.
            companionDao.getActiveCompanion()
                .filterNotNull()
                .flatMapLatest { companion ->
                    bondProgressDao.getByCompanionId(companion.id).map { bond ->
                        PetState(
                            companion = companion,
                            bondProgress = bond,
                            evolutionStage = companion.evolutionStage,
                            isLoaded = true
                        )
                    }
                }
                .catch { e -> Timber.e(e, "PetStateManager flow error") }
                .collect { newState ->
                    _currentState.value = newState
                }
        }
    }

    suspend fun recordEmotionalState(
        mood: PetMood,
        trigger: MoodTrigger,
        contextAppPackage: String?
    ) {
        val companionId = _currentState.value.companion?.id ?: return
        val state = EmotionalStateEntity(
            companionId = companionId,
            mood = mood,
            intensity = 0.8f,
            trigger = trigger,
            contextAppPackage = contextAppPackage
        )
        emotionalStateDao.insert(state)
        _currentState.value = _currentState.value.copy(currentMood = mood)
        Timber.d("Emotional state recorded: $mood (trigger: $trigger)")
    }
}
