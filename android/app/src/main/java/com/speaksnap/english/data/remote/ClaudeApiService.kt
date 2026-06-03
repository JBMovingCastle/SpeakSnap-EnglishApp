package com.speaksnap.english.data.remote

import com.speaksnap.english.data.remote.dto.ClaudeRequest
import com.speaksnap.english.data.remote.dto.ClaudeResponse
import retrofit2.http.*

interface ClaudeApiService {

    @POST("v1/messages")
    suspend fun createMessage(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") anthropicVersion: String = "2023-06-01",
        @Body request: ClaudeRequest
    ): ClaudeResponse
}
