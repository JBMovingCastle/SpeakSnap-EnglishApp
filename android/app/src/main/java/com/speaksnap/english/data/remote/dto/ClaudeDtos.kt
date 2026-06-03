package com.speaksnap.english.data.remote.dto

import com.google.gson.annotations.SerializedName

// --- Request ---
data class ClaudeRequest(
    val model: String = "claude-sonnet-4-20250514",
    @SerializedName("max_tokens") val maxTokens: Int = 4096,
    val system: String? = null,
    val messages: List<ClaudeMessage>
)

data class ClaudeMessage(
    val role: String, // "user" | "assistant"
    val content: List<ClaudeContentBlock>
)

data class ClaudeContentBlock(
    val type: String, // "text" | "image"
    val text: String? = null,
    val source: ImageSource? = null
)

data class ImageSource(
    val type: String = "base64",
    @SerializedName("media_type") val mediaType: String = "image/jpeg",
    val data: String // base64 encoded image
)

// --- Response ---
data class ClaudeResponse(
    val id: String,
    val type: String = "message",
    val role: String,
    val content: List<ClaudeResponseContent>,
    val model: String,
    @SerializedName("stop_reason") val stopReason: String? = null,
    val usage: ClaudeUsage
)

data class ClaudeResponseContent(
    val type: String,
    val text: String
)

data class ClaudeUsage(
    @SerializedName("input_tokens") val inputTokens: Int,
    @SerializedName("output_tokens") val outputTokens: Int
)

// --- Domain-level extracted content models ---
data class ExtractedContent(
    val title: String = "",
    val words: List<WordItem> = emptyList(),
    val phrases: List<PhraseItem> = emptyList(),
    val grammarPoints: List<GrammarItem> = emptyList(),
    val caseStudies: List<CaseStudyItem> = emptyList()
)

data class WordItem(
    val word: String,
    val phonetic: String = "",
    val meaning: String = "",
    val example: String = ""
)

data class PhraseItem(
    val phrase: String,
    val meaning: String = "",
    val usage: String = ""
)

data class GrammarItem(
    val title: String,
    val explanation: String = "",
    val example: String = ""
)

data class CaseStudyItem(
    val title: String,
    val scenario: String = "",
    val keyPoints: List<String> = emptyList(),
    val starterDialogue: String = ""
)
