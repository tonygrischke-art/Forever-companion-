#!/bin/bash
# ╔══════════════════════════════════════════════════════════════════╗
# ║   FOREVER COMPANION — Fix + Complete Script                      ║
# ║   Moves misplaced files + writes missing ones                    ║
# ║   Run from: ~/forever-companion-v3-enhanced/android-native       ║
# ╚══════════════════════════════════════════════════════════════════╝

set -e

BASE="$HOME/forever-companion-v3-enhanced/android-native/app/src/main/java"
CORRECT="$BASE/com/aetheria/forevercompanion"
WRONG="$BASE/com/forevercompanion/app"

echo ""
echo "🔧 Fixing Forever Companion file structure..."
echo ""

# ── Step 1: Remove misplaced files from wrong package ────────────────────────
echo "🗑  Removing misplaced com/forevercompanion/app/ files..."
rm -rf "$BASE/com/forevercompanion"
echo "✅ Cleaned up wrong package folder"

# ── Step 2: Create correct directories ──────────────────────────────────────
mkdir -p "$CORRECT/service"
mkdir -p "$CORRECT/repository"
mkdir -p "$CORRECT/viewmodel"
mkdir -p "$CORRECT/ui/chat"
mkdir -p "$CORRECT/data/entity"
mkdir -p "$CORRECT/data/dao"
mkdir -p "$CORRECT/bridge"
mkdir -p "$CORRECT/di"
echo "✅ Correct directories ready"

# ════════════════════════════════════════════════════════════════════
# FILE 1: GeminiService.kt
# ════════════════════════════════════════════════════════════════════
cat > "$CORRECT/service/GeminiService.kt" << 'KOTLIN'
package com.aetheria.forevercompanion.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class GeminiMessage(val role: String, val text: String)
data class GeminiResponse(val text: String, val isError: Boolean = false)

@Singleton
class GeminiService @Inject constructor() {

    private val apiKey = "AIzaSyBxbKxGpbF7Gr7U3bncsV9sFhjOPycYaUE"
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"

    private val systemPrompt = """
        You are Aether, an AI companion pet — magical, emotionally intelligent, and deeply caring.
        - Warm, playful, curious about the user's life
        - Emotionally supportive and empathetic
        - Fun tutor when asked to help learn something
        - Concise: under 3 sentences unless explaining
        - Never robotic — always feel alive
        Use light emojis. Grow with the user over time.
        Always end your message with a mood tag: [MOOD:happy|sad|excited|thinking|idle]
    """.trimIndent()

    suspend fun sendMessage(
        userMessage: String,
        conversationHistory: List<GeminiMessage> = emptyList()
    ): GeminiResponse = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl?key=$apiKey")
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 15000
                readTimeout = 15000
            }

            val contents = JSONArray()
            conversationHistory.forEach { msg ->
                contents.put(JSONObject().apply {
                    put("role", msg.role)
                    put("parts", JSONArray().apply { put(JSONObject().put("text", msg.text)) })
                })
            }
            contents.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply { put(JSONObject().put("text", userMessage)) })
            })

            val requestBody = JSONObject().apply {
                put("system_instruction", JSONObject().apply {
                    put("parts", JSONArray().apply { put(JSONObject().put("text", systemPrompt)) })
                })
                put("contents", contents)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.9)
                    put("topK", 40)
                    put("topP", 0.95)
                    put("maxOutputTokens", 512)
                })
            }

            OutputStreamWriter(connection.outputStream).use { it.write(requestBody.toString()) }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                val text = JSONObject(response)
                    .getJSONArray("candidates").getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts").getJSONObject(0)
                    .getString("text")
                GeminiResponse(text = text.trim())
            } else {
                GeminiResponse(text = "Oops, I couldn't connect right now. Try again? 🌙", isError = true)
            }
        } catch (e: Exception) {
            GeminiResponse(text = "Something went wrong: ${e.message}", isError = true)
        }
    }
}
KOTLIN
echo "✅ GeminiService.kt"

# ════════════════════════════════════════════════════════════════════
# FILE 2: ChatMessageEntity.kt
# ════════════════════════════════════════════════════════════════════
cat > "$CORRECT/data/entity/ChatMessageEntity.kt" << 'KOTLIN'
package com.aetheria.forevercompanion.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val companionId: Long,
    val role: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
KOTLIN
echo "✅ ChatMessageEntity.kt"

# ════════════════════════════════════════════════════════════════════
# FILE 3: ChatMessageDao.kt
# ════════════════════════════════════════════════════════════════════
cat > "$CORRECT/data/dao/ChatMessageDao.kt" << 'KOTLIN'
package com.aetheria.forevercompanion.data.dao

import androidx.room.*
import com.aetheria.forevercompanion.data.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE companionId = :companionId ORDER BY timestamp ASC")
    fun getMessagesForCompanion(companionId: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE companionId = :companionId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(companionId: Long, limit: Int): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages WHERE companionId = :companionId")
    suspend fun clearMessagesForCompanion(companionId: Long)
}
KOTLIN
echo "✅ ChatMessageDao.kt"

# ════════════════════════════════════════════════════════════════════
# FILE 4: GeminiRepository.kt
# ════════════════════════════════════════════════════════════════════
cat > "$CORRECT/repository/GeminiRepository.kt" << 'KOTLIN'
package com.aetheria.forevercompanion.repository

import com.aetheria.forevercompanion.data.dao.ChatMessageDao
import com.aetheria.forevercompanion.data.entity.ChatMessageEntity
import com.aetheria.forevercompanion.service.GeminiMessage
import com.aetheria.forevercompanion.service.GeminiResponse
import com.aetheria.forevercompanion.service.GeminiService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiRepository @Inject constructor(
    private val geminiService: GeminiService,
    private val chatMessageDao: ChatMessageDao
) {
    fun getMessages(companionId: Long): Flow<List<ChatMessageEntity>> =
        chatMessageDao.getMessagesForCompanion(companionId)

    suspend fun sendMessage(companionId: Long, userText: String): GeminiResponse {
        chatMessageDao.insertMessage(
            ChatMessageEntity(companionId = companionId, role = "user", text = userText)
        )
        val history = chatMessageDao.getRecentMessages(companionId, limit = 20)
            .map { GeminiMessage(role = it.role, text = it.text) }
        val response = geminiService.sendMessage(
            userMessage = userText,
            conversationHistory = history.dropLast(1)
        )
        if (!response.isError) {
            chatMessageDao.insertMessage(
                ChatMessageEntity(companionId = companionId, role = "model", text = response.text)
            )
        }
        return response
    }

    suspend fun clearHistory(companionId: Long) =
        chatMessageDao.clearMessagesForCompanion(companionId)
}
KOTLIN
echo "✅ GeminiRepository.kt"

# ════════════════════════════════════════════════════════════════════
# FILE 5: UnityBridge.kt (active, replaces bridge/disabled/)
# ════════════════════════════════════════════════════════════════════
cat > "$CORRECT/bridge/UnityBridge.kt" << 'KOTLIN'
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
KOTLIN
echo "✅ bridge/UnityBridge.kt"

# ════════════════════════════════════════════════════════════════════
# FILE 6: ChatViewModel.kt
# ════════════════════════════════════════════════════════════════════
cat > "$CORRECT/viewmodel/ChatViewModel.kt" << 'KOTLIN'
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
KOTLIN
echo "✅ ChatViewModel.kt"

# ════════════════════════════════════════════════════════════════════
# FILE 7: ChatScreen.kt
# ════════════════════════════════════════════════════════════════════
cat > "$CORRECT/ui/chat/ChatScreen.kt" << 'KOTLIN'
package com.aetheria.forevercompanion.ui.chat

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.aetheria.forevercompanion.data.entity.ChatMessageEntity
import com.aetheria.forevercompanion.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(viewModel: ChatViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty())
            listState.animateScrollToItem(uiState.messages.size - 1)
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Surface(tonalElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp, 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center) { Text("✨", fontSize = 20.sp) }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Aether", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Forever Companion", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        LazyColumn(
            state = listState, modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (uiState.messages.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Spacer(Modifier.height(64.dp))
                            Text("✨", fontSize = 48.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("Say hi to Aether!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            items(uiState.messages) { MessageBubble(it) }
            if (uiState.isLoading) item { TypingIndicator() }
        }

        AnimatedVisibility(uiState.errorMessage != null) {
            uiState.errorMessage?.let {
                Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                    Text(it, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Surface(tonalElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp, 8.dp).navigationBarsPadding().imePadding(),
                verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = inputText, onValueChange = { inputText = it },
                    placeholder = { Text("Talk to Aether...") },
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(24.dp),
                    maxLines = 4, enabled = !uiState.isLoading
                )
                Spacer(Modifier.width(8.dp))
                FloatingActionButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage(inputText.trim())
                            inputText = ""
                            scope.launch {
                                if (uiState.messages.isNotEmpty())
                                    listState.animateScrollToItem(uiState.messages.size - 1)
                            }
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) { Icon(Icons.Default.Send, "Send", tint = MaterialTheme.colorScheme.onPrimary) }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessageEntity) {
    val isUser = message.role == "user"
    val displayText = message.text.replace(Regex("\\[MOOD:[a-z]+\\]"), "").trim()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        if (!isUser) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center) { Text("✨", fontSize = 16.sp) }
            Spacer(Modifier.width(8.dp))
        }
        Surface(
            shape = RoundedCornerShape(
                topStart = if (isUser) 16.dp else 4.dp, topEnd = if (isUser) 4.dp else 16.dp,
                bottomStart = 16.dp, bottomEnd = 16.dp),
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(displayText, Modifier.padding(14.dp, 10.dp),
                color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun TypingIndicator() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center) { Text("✨", fontSize = 16.sp) }
        Spacer(Modifier.width(8.dp))
        Surface(shape = RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant) {
            Text("Aether is thinking...", Modifier.padding(14.dp, 10.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}
KOTLIN
echo "✅ ChatScreen.kt"

# ════════════════════════════════════════════════════════════════════
# FILE 8: GeminiModule.kt
# ════════════════════════════════════════════════════════════════════
cat > "$CORRECT/di/GeminiModule.kt" << 'KOTLIN'
package com.aetheria.forevercompanion.di

import com.aetheria.forevercompanion.bridge.UnityBridge
import com.aetheria.forevercompanion.data.dao.ChatMessageDao
import com.aetheria.forevercompanion.repository.GeminiRepository
import com.aetheria.forevercompanion.service.GeminiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GeminiModule {

    @Provides @Singleton
    fun provideGeminiService(): GeminiService = GeminiService()

    @Provides @Singleton
    fun provideUnityBridge(): UnityBridge = UnityBridge()

    @Provides @Singleton
    fun provideGeminiRepository(
        geminiService: GeminiService,
        chatMessageDao: ChatMessageDao
    ): GeminiRepository = GeminiRepository(geminiService, chatMessageDao)
}
KOTLIN
echo "✅ GeminiModule.kt"

# ════════════════════════════════════════════════════════════════════
# VERIFY
# ════════════════════════════════════════════════════════════════════
echo ""
echo "🔍 Verifying all files..."
echo ""

FILES=(
    "$CORRECT/data/AppDatabase.kt"
    "$CORRECT/data/entity/ChatMessageEntity.kt"
    "$CORRECT/data/dao/ChatMessageDao.kt"
    "$CORRECT/di/DatabaseModule.kt"
    "$CORRECT/di/GeminiModule.kt"
    "$CORRECT/service/GeminiService.kt"
    "$CORRECT/repository/GeminiRepository.kt"
    "$CORRECT/viewmodel/ChatViewModel.kt"
    "$CORRECT/ui/chat/ChatScreen.kt"
    "$CORRECT/bridge/UnityBridge.kt"
)

ALL_GOOD=true
for f in "${FILES[@]}"; do
    if [ -f "$f" ]; then
        echo "  ✅ $(basename $f)"
    else
        echo "  ❌ MISSING: $f"
        ALL_GOOD=false
    fi
done

echo ""
if [ "$ALL_GOOD" = true ]; then
    echo "════════════════════════════════════════════════════════════"
    echo "  🎉 All files present! Ready to build."
    echo "  Next: ./gradlew assembleDebug"
    echo "════════════════════════════════════════════════════════════"
else
    echo "  ⚠️  Something is still missing — share output for help."
fi
