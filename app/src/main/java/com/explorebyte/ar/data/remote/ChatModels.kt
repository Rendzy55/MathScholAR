package com.explorebyte.ar.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body untuk Groq Chat Completions API.
 * Format kompatibel dengan OpenAI API.
 * Docs: https://console.groq.com/docs/api-reference#chat-completions
 */
@Serializable
data class GroqRequest(
    @SerialName("model") val model: String = "llama-3.3-70b-versatile",
    @SerialName("messages") val messages: List<ChatMessage>,
    @SerialName("temperature") val temperature: Double = 0.7,
    @SerialName("max_tokens") val maxTokens: Int = 2048,
    @SerialName("stream") val stream: Boolean = false
)

@Serializable
data class ChatMessage(
    @SerialName("role") val role: String,
    @SerialName("content") val content: String
)

/**
 * Response dari Groq Chat Completions API.
 */
@Serializable
data class GroqResponse(
    @SerialName("id") val id: String? = null,
    @SerialName("choices") val choices: List<GroqChoice>? = null,
    @SerialName("error") val error: GroqApiError? = null
)

@Serializable
data class GroqChoice(
    @SerialName("message") val message: ChatMessage,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class GroqApiError(
    @SerialName("message") val message: String? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("code") val code: String? = null
)
