package com.explorebyte.ar.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object ApiService {
    const val BASE_URL = "https://api.groq.com/"

    val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    suspend fun getGroqChatCompletions(
        token: String,
        request: GroqRequest
    ): HttpResponse {
        return client.post("${BASE_URL}openai/v1/chat/completions") {
            header("Authorization", token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }
}
