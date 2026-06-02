package com.speaksnap.english.data.remote.dto

import com.google.gson.annotations.SerializedName

// --- DeepSeek Request (OpenAI-compatible) ---
data class DeepSeekRequest(
    val model: String = "deepseek-chat",
    val messages: List<DeepSeekMessage>,
    @SerializedName("max_tokens") val maxTokens: Int = 4096,
    val temperature: Double = 0.7,
    val stream: Boolean = false
)

data class DeepSeekMessage(
    val role: String, // "system" | "user" | "assistant"
    val content: Any // String or List<DeepSeekContentPart>
)

data class DeepSeekContentPart(
    val type: String, // "text" | "image_url"
    val text: String? = null,
    @SerializedName("image_url") val imageUrl: ImageUrl? = null
)

data class ImageUrl(
    val url: String, // "data:image/jpeg;base64,..." or URL
    val detail: String = "auto"
)

// --- DeepSeek Response ---
data class DeepSeekResponse(
    val id: String,
    val `object`: String = "chat.completion",
    val created: Long,
    val model: String,
    val choices: List<DeepSeekChoice>,
    val usage: DeepSeekUsage?
)

data class DeepSeekChoice(
    val index: Int,
    val message: DeepSeekRespMessage,
    @SerializedName("finish_reason") val finishReason: String?
)

data class DeepSeekRespMessage(
    val role: String,
    val content: String
)

data class DeepSeekUsage(
    @SerializedName("prompt_tokens") val promptTokens: Int,
    @SerializedName("completion_tokens") val completionTokens: Int,
    @SerializedName("total_tokens") val totalTokens: Int
)
