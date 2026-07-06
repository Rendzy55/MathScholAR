package com.explorebyte.ar.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {

    /**
     * Groq Chat Completions API (OpenAI-compatible).
     * Docs: https://console.groq.com/docs/api-reference#chat-completions
     */
    @POST("openai/v1/chat/completions")
    suspend fun getGroqChatCompletions(
        @Header("Authorization") token: String,
        @Body request: GroqRequest
    ): Response<GroqResponse>

    companion object {
        const val BASE_URL = "https://api.groq.com/"
    }
}
