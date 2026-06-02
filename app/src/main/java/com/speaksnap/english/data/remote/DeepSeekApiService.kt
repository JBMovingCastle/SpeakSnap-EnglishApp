package com.speaksnap.english.data.remote

import com.speaksnap.english.data.remote.dto.DeepSeekRequest
import com.speaksnap.english.data.remote.dto.DeepSeekResponse
import retrofit2.http.*

interface DeepSeekApiService {

    @POST("v1/chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") apiKey: String,
        @Body request: DeepSeekRequest
    ): DeepSeekResponse
}
