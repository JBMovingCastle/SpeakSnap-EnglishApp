package com.speaksnap.english.data.remote

import com.speaksnap.english.data.remote.dto.OpenAIChatRequest
import com.speaksnap.english.data.remote.dto.OpenAIChatResponse
import retrofit2.http.*

/** 阿里通义千问 (DashScope) — 图片OCR/视觉理解最强 */
interface TongyiApiService {

    @POST("compatible-mode/v1/chat/completions")
    suspend fun chat(
        @Header("Authorization") apiKey: String,
        @Body request: OpenAIChatRequest
    ): OpenAIChatResponse
}
