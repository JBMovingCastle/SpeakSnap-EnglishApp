package com.speaksnap.english.data.remote

import com.speaksnap.english.data.remote.dto.OpenAIChatRequest
import com.speaksnap.english.data.remote.dto.OpenAIChatResponse
import retrofit2.http.*

interface DeepSeekApiService {
    @POST("v1/chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") apiKey: String,
        @Body request: OpenAIChatRequest
    ): OpenAIChatResponse
}
