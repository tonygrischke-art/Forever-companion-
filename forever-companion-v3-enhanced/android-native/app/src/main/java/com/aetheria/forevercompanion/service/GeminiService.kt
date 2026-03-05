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
