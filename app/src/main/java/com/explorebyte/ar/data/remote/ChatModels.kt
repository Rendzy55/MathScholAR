package com.explorebyte.ar.data.remote

import com.google.gson.annotations.SerializedName

/**
 * Request body untuk Groq Chat Completions API.
 * Format kompatibel dengan OpenAI API.
 * Docs: https://console.groq.com/docs/api-reference#chat-completions
 */
data class GroqRequest(
    @SerializedName("model") val model: String = "llama-3.3-70b-versatile",
    @SerializedName("messages") val messages: List<ChatMessage>,
    @SerializedName("temperature") val temperature: Double = 0.7,
    @SerializedName("max_tokens") val maxTokens: Int = 2048,
    @SerializedName("stream") val stream: Boolean = false
)

data class ChatMessage(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String
)

/**
 * Response dari Groq Chat Completions API.
 */
data class GroqResponse(
    @SerializedName("id") val id: String?,
    @SerializedName("choices") val choices: List<GroqChoice>?,
    @SerializedName("error") val error: GroqApiError?
)

data class GroqChoice(
    @SerializedName("message") val message: ChatMessage,
    @SerializedName("finish_reason") val finishReason: String?
)

data class GroqApiError(
    @SerializedName("message") val message: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("code") val code: String?
)
