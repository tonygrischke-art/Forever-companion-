package com.aetheria.forevercompanion.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aetheria.forevercompanion.bridge.UnityBridge
import com.aetheria.forevercompanion.data.entity.ChatMessageEntity
import com.aetheria.forevercompanion.repository.GeminiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessageEntity> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val companionId: Long = 1L
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val geminiRepository: GeminiRepository,
    private val unityBridge: UnityBridge
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            geminiRepository.getMessages(_uiState.value.companionId)
                .collectLatest { messages -> _uiState.update { it.copy(messages = messages) } }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            unityBridge.onUserMessage(text)
            val response = geminiRepository.sendMessage(_uiState.value.companionId, text)
            if (!response.isError) unityBridge.onGeminiResponse(response.text)
            else unityBridge.setIdleState()
            _uiState.update {
                it.copy(isLoading = false, errorMessage = if (response.isError) response.text else null)
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            geminiRepository.clearHistory(_uiState.value.companionId)
            unityBridge.setIdleState()
        }
    }
}
