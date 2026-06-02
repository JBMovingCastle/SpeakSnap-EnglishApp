package com.speaksnap.english.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.speaksnap.english.data.remote.ClaudeApiService
import com.speaksnap.english.data.remote.NetworkModule
import com.speaksnap.english.data.remote.dto.*
import java.io.ByteArrayOutputStream
import java.io.File

class ClaudeRepository(
    private val api: ClaudeApiService = NetworkModule.createAnthropicService(),
    private val gson: Gson = Gson()
) {

    /**
     * Send an image to Claude Vision for OCR and structured content extraction.
     * Returns parsed ExtractedContent with words, phrases, grammar, and case studies.
     */
    suspend fun analyzeImage(apiKey: String, imagePath: String): Result<ExtractedContent> {
        return try {
            val base64Image = compressAndEncode(imagePath)
            val request = ClaudeRequest(
                model = "claude-sonnet-4-20250514",
                maxTokens = 4096,
                system = OCR_SYSTEM_PROMPT,
                messages = listOf(
                    ClaudeMessage(
                        role = "user",
                        content = listOf(
                            ClaudeContentBlock(
                                type = "image",
                                source = ImageSource(mediaType = "image/jpeg", data = base64Image)
                            ),
                            ClaudeContentBlock(
                                type = "text",
                                text = "Please analyze this image and extract all English learning content in the specified JSON format."
                            )
                        )
                    )
                )
            )
            val response = api.createMessage(apiKey, request = request)
            val text = response.content.firstOrNull()?.text ?: ""
            val extracted = parseExtractedContent(text)
            Result.success(extracted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generate a conversational practice response based on a case study context.
     */
    suspend fun generateConversation(
        apiKey: String,
        caseStudy: CaseStudyItem,
        conversationHistory: List<Pair<String, String>>, // (role, message)
        userMessage: String
    ): Result<String> {
        return try {
            val messages = mutableListOf<ClaudeMessage>()

            // Build conversation from history
            conversationHistory.forEach { (role, msg) ->
                messages.add(
                    ClaudeMessage(
                        role = if (role == "ai") "assistant" else "user",
                        content = listOf(ClaudeContentBlock(type = "text", text = msg))
                    )
                )
            }
            // Add current user message
            messages.add(
                ClaudeMessage(
                    role = "user",
                    content = listOf(ClaudeContentBlock(type = "text", text = userMessage))
                )
            )

            val request = ClaudeRequest(
                model = "claude-sonnet-4-20250514",
                maxTokens = 1024,
                system = CONVERSATION_SYSTEM_PROMPT.format(
                    caseStudy.title,
                    caseStudy.scenario,
                    caseStudy.keyPoints.joinToString(", ")
                ),
                messages = messages
            )
            val response = api.createMessage(apiKey, request = request)
            Result.success(response.content.firstOrNull()?.text ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Manually input text for AI to structure into learning content.
     */
    suspend fun analyzeText(apiKey: String, text: String, title: String): Result<ExtractedContent> {
        return try {
            val request = ClaudeRequest(
                model = "claude-sonnet-4-20250514",
                maxTokens = 4096,
                system = TEXT_ANALYSIS_SYSTEM_PROMPT,
                messages = listOf(
                    ClaudeMessage(
                        role = "user",
                        content = listOf(
                            ClaudeContentBlock(type = "text", text = "Title: $title\n\nContent:\n$text")
                        )
                    )
                )
            )
            val response = api.createMessage(apiKey, request = request)
            val responseText = response.content.firstOrNull()?.text ?: ""
            val extracted = parseExtractedContent(responseText)
            Result.success(extracted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Private helpers ---

    private fun compressAndEncode(imagePath: String, maxDimension: Int = 1568): String {
        val file = File(imagePath)
        return if (file.exists()) {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            val scaled = if (bitmap != null) {
                val ratio = minOf(maxDimension.toFloat() / bitmap.width, maxDimension.toFloat() / bitmap.height, 1f)
                if (ratio < 1f) {
                    Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
                } else bitmap
            } else null

            val outputStream = ByteArrayOutputStream()
            scaled?.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            scaled?.recycle()
            bitmap?.recycle()
            Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        } else {
            // Fallback: if path isn't a file, it might already be a base64 string
            imagePath
        }
    }

    private fun parseExtractedContent(responseText: String): ExtractedContent {
        return try {
            // Extract JSON from response (Claude may wrap it in markdown code blocks)
            val jsonStr = responseText
                .replace("```json", "")
                .replace("```", "")
                .trim()
            val json = JsonParser.parseString(jsonStr).asJsonObject

            ExtractedContent(
                title = json.get("title")?.asString ?: "",
                words = json.getAsJsonArray("words")?.map { el ->
                    val obj = el.asJsonObject
                    WordItem(
                        word = obj.get("word")?.asString ?: "",
                        phonetic = obj.get("phonetic")?.asString ?: "",
                        meaning = obj.get("meaning")?.asString ?: "",
                        example = obj.get("example")?.asString ?: ""
                    )
                } ?: emptyList(),
                phrases = json.getAsJsonArray("phrases")?.map { el ->
                    val obj = el.asJsonObject
                    PhraseItem(
                        phrase = obj.get("phrase")?.asString ?: "",
                        meaning = obj.get("meaning")?.asString ?: "",
                        usage = obj.get("usage")?.asString ?: ""
                    )
                } ?: emptyList(),
                grammarPoints = json.getAsJsonArray("grammar_points")?.map { el ->
                    val obj = el.asJsonObject
                    GrammarItem(
                        title = obj.get("title")?.asString ?: "",
                        explanation = obj.get("explanation")?.asString ?: "",
                        example = obj.get("example")?.asString ?: ""
                    )
                } ?: emptyList(),
                caseStudies = json.getAsJsonArray("case_studies")?.map { el ->
                    val obj = el.asJsonObject
                    CaseStudyItem(
                        title = obj.get("title")?.asString ?: "",
                        scenario = obj.get("scenario")?.asString ?: "",
                        keyPoints = obj.getAsJsonArray("key_points")?.map { it.asString } ?: emptyList(),
                        starterDialogue = obj.get("starter_dialogue")?.asString ?: ""
                    )
                } ?: emptyList()
            )
        } catch (e: Exception) {
            // If JSON parsing fails, return raw text as fallback
            ExtractedContent(
                title = "Extracted Content",
                words = listOf(WordItem(word = "Parsing error — raw response", meaning = responseText.take(500)))
            )
        }
    }

    companion object {
        private val OCR_SYSTEM_PROMPT = """
You are an expert English teacher. Analyze the provided image of textbook/notes and extract ALL learning content.

Return a JSON object with this exact structure:
{
  "title": "Course unit title",
  "words": [{"word": "...", "phonetic": "/.../", "meaning": "Chinese meaning", "example": "English example sentence"}],
  "phrases": [{"phrase": "...", "meaning": "Chinese meaning", "usage": "When/how to use"}],
  "grammar_points": [{"title": "Grammar topic", "explanation": "Simple explanation in Chinese", "example": "Example sentence"}],
  "case_studies": [{"title": "Case name", "scenario": "Scenario description", "key_points": ["point1", "point2"], "starter_dialogue": "Opening dialogue line"}]
}

IMPORTANT:
- Extract EVERY English word, phrase, grammar point visible in the image.
- Provide Chinese meanings for all words and phrases.
- Include phonetic transcriptions for words.
- For case studies, create realistic business/daily-life dialogue scenarios based on the content.
- Return ONLY valid JSON, no other text.
        """.trimIndent()

        private val TEXT_ANALYSIS_SYSTEM_PROMPT = """
You are an expert English teacher. Analyze the provided text/notes and structure it into learning content.

Return a JSON object with this exact structure:
{
  "title": "Topic title",
  "words": [{"word": "...", "phonetic": "/.../", "meaning": "Chinese meaning", "example": "Example sentence"}],
  "phrases": [{"phrase": "...", "meaning": "Chinese meaning", "usage": "Usage notes"}],
  "grammar_points": [{"title": "Grammar topic", "explanation": "Explanation", "example": "Example"}],
  "case_studies": [{"title": "Case name", "scenario": "Description", "key_points": ["..."], "starter_dialogue": "..."}]
}

Return ONLY valid JSON, no other text.
        """.trimIndent()

        private val CONVERSATION_SYSTEM_PROMPT = """
You are an AI English teacher leading a conversation practice session.

Context: %s
Scenario: %s
Key vocabulary to practice: %s

Guidelines:
- Keep responses concise (2-4 sentences).
- Naturally incorporate the key vocabulary.
- Provide gentle corrections if the student makes grammar mistakes (add 💡 Tip: ...).
- Maintain a supportive, encouraging tone.
- Adjust difficulty based on the student's level.
- Speak as a native English speaker would in a real conversation.
        """.trimIndent()
    }
}
