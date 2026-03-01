package com.aetheria.forevercompanion.ui.home

import androidx.lifecycle.ViewModel
import com.aetheria.forevercompanion.overlay.PetState
import com.aetheria.forevercompanion.overlay.PetStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    petStateManager: PetStateManager
) : ViewModel() {
    val petState: StateFlow<PetState> = petStateManager.currentState
}
