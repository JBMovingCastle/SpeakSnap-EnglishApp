package com.speaksnap.english.data.remote

import com.speaksnap.english.data.remote.dto.OpenAIChatRequest
import com.speaksnap.english.data.remote.dto.OpenAIChatResponse
import okhttp3.ResponseBody
import retrofit2.http.*

/** 豆包/火山方舟 (Ark) — 语音TTS+ASR+对话 */
interface DoubaoApiService {

    @POST("api/v3/chat/completions")
    suspend fun chat(
        @Header("Authorization") apiKey: String,
        @Body request: OpenAIChatRequest
    ): OpenAIChatResponse

    @POST("api/v3/audio/speech")
    suspend fun tts(
        @Header("Authorization") apiKey: String,
        @Body request: com.speaksnap.english.data.remote.dto.TTSRequest
    ): ResponseBody
}
