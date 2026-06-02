package com.speaksnap.english.data.remote.dto

import com.google.gson.annotations.SerializedName

// === OpenAI-compatible Chat Completions ===
data class OpenAIChatRequest(
    val model: String,
    val messages: List<OpenAIMessage>,
    @SerializedName("max_tokens") val maxTokens: Int = 4096,
    val temperature: Double = 0.7,
    val stream: Boolean = false
)

data class OpenAIMessage(
    val role: String,
    val content: Any // String or List<OpenAIContentPart>
)

data class OpenAIContentPart(
    val type: String,
    val text: String? = null,
    @SerializedName("image_url") val imageUrl: OpenAIImageUrl? = null
)

data class OpenAIImageUrl(
    val url: String,
    val detail: String = "auto"
)

data class OpenAIChatResponse(
    val id: String,
    val `object`: String = "chat.completion",
    val created: Long,
    val model: String,
    val choices: List<OpenAIChoice>,
    val usage: OpenAIUsage?
)

data class OpenAIChoice(
    val index: Int,
    val message: OpenAIRespMessage,
    @SerializedName("finish_reason") val finishReason: String?
)

data class OpenAIRespMessage(
    val role: String,
    val content: String
)

data class OpenAIUsage(
    @SerializedName("prompt_tokens") val promptTokens: Int,
    @SerializedName("completion_tokens") val completionTokens: Int,
    @SerializedName("total_tokens") val totalTokens: Int
)

// === TTS (Doubao) ===
data class TTSRequest(
    val model: String = "doubao-tts",
    val input: String,
    val voice: String = "zh_female_qingxin",
    @SerializedName("response_format") val responseFormat: String = "mp3",
    val speed: Double = 1.0
)

// === ASR (Doubao) ===
data class ASRRequest(
    val model: String = "doubao-asr",
    val audio: String // base64
)
