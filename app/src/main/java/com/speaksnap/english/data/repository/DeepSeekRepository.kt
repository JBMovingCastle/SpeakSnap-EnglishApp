package com.speaksnap.english.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.speaksnap.english.data.remote.DeepSeekApiService
import com.speaksnap.english.data.remote.NetworkModule
import com.speaksnap.english.data.remote.dto.*
import java.io.ByteArrayOutputStream
import java.io.File

class DeepSeekRepository(
    private val api: DeepSeekApiService = NetworkModule.createDeepSeekService(),
    private val gson: Gson = Gson()
) {

    suspend fun analyzeImage(apiKey: String, imagePath: String, model: String = "deepseek-chat"): Result<ExtractedContent> {
        return try {
            val base64Image = compressAndEncode(imagePath)
            val messages = listOf(
                DeepSeekMessage(role = "system", content = OCR_SYSTEM_PROMPT),
                DeepSeekMessage(role = "user", content = listOf(
                    DeepSeekContentPart(type = "image_url", imageUrl = ImageUrl(url = "data:image/jpeg;base64,$base64Image")),
                    DeepSeekContentPart(type = "text", text = "Please analyze this image and extract all English learning content in the specified JSON format.")
                ))
            )
            val request = DeepSeekRequest(model = model, maxTokens = 4096, messages = messages)
            val response = api.createChatCompletion("Bearer $apiKey", request)
            val text = response.choices.firstOrNull()?.message?.content ?: ""
            val extracted = parseExtractedContent(text)
            Result.success(extracted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateConversation(
        apiKey: String,
        caseStudy: CaseStudyItem,
        conversationHistory: List<Pair<String, String>>,
        userMessage: String,
        model: String = "deepseek-chat"
    ): Result<String> {
        return try {
            val messages = mutableListOf<DeepSeekMessage>()
            messages.add(DeepSeekMessage(role = "system", content = CONVERSATION_SYSTEM_PROMPT.format(
                caseStudy.title, caseStudy.scenario, caseStudy.keyPoints.joinToString(", ")
            )))
            conversationHistory.forEach { (role, msg) ->
                messages.add(DeepSeekMessage(role = if (role == "ai") "assistant" else "user", content = msg))
            }
            messages.add(DeepSeekMessage(role = "user", content = userMessage))

            val request = DeepSeekRequest(model = model, maxTokens = 1024, messages = messages)
            val response = api.createChatCompletion("Bearer $apiKey", request)
            Result.success(response.choices.firstOrNull()?.message?.content ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun analyzeText(apiKey: String, text: String, title: String, model: String = "deepseek-chat"): Result<ExtractedContent> {
        return try {
            val messages = listOf(
                DeepSeekMessage(role = "system", content = TEXT_ANALYSIS_SYSTEM_PROMPT),
                DeepSeekMessage(role = "user", content = "Title: $title\n\nContent:\n$text")
            )
            val request = DeepSeekRequest(model = model, maxTokens = 4096, messages = messages)
            val response = api.createChatCompletion("Bearer $apiKey", request)
            val responseText = response.choices.firstOrNull()?.message?.content ?: ""
            val extracted = parseExtractedContent(responseText)
            Result.success(extracted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun compressAndEncode(imagePath: String, maxDimension: Int = 1568): String {
        val file = File(imagePath)
        return if (file.exists()) {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            val scaled = if (bitmap != null) {
                val ratio = minOf(maxDimension.toFloat() / bitmap.width, maxDimension.toFloat() / bitmap.height, 1f)
                if (ratio < 1f) Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true) else bitmap
            } else null
            val outputStream = ByteArrayOutputStream()
            scaled?.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            scaled?.recycle()
            bitmap?.recycle()
            Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        } else imagePath
    }

    private fun parseExtractedContent(responseText: String): ExtractedContent {
        return try {
            val jsonStr = responseText.replace("```json", "").replace("```", "").trim()
            val json = JsonParser.parseString(jsonStr).asJsonObject

            ExtractedContent(
                title = json.get("title")?.asString ?: "",
                words = json.getAsJsonArray("words")?.map { el ->
                    val obj = el.asJsonObject
                    WordItem(word = obj.get("word")?.asString ?: "", phonetic = obj.get("phonetic")?.asString ?: "", meaning = obj.get("meaning")?.asString ?: "", example = obj.get("example")?.asString ?: "")
                } ?: emptyList(),
                phrases = json.getAsJsonArray("phrases")?.map { el ->
                    val obj = el.asJsonObject
                    PhraseItem(phrase = obj.get("phrase")?.asString ?: "", meaning = obj.get("meaning")?.asString ?: "", usage = obj.get("usage")?.asString ?: "")
                } ?: emptyList(),
                grammarPoints = json.getAsJsonArray("grammar_points")?.map { el ->
                    val obj = el.asJsonObject
                    GrammarItem(title = obj.get("title")?.asString ?: "", explanation = obj.get("explanation")?.asString ?: "", example = obj.get("example")?.asString ?: "")
                } ?: emptyList(),
                caseStudies = json.getAsJsonArray("case_studies")?.map { el ->
                    val obj = el.asJsonObject
                    CaseStudyItem(title = obj.get("title")?.asString ?: "", scenario = obj.get("scenario")?.asString ?: "", keyPoints = obj.getAsJsonArray("key_points")?.map { it.asString } ?: emptyList(), starterDialogue = obj.get("starter_dialogue")?.asString ?: "")
                } ?: emptyList()
            )
        } catch (e: Exception) {
            ExtractedContent(title = "Extracted Content", words = listOf(WordItem(word = "Parsing error — raw response", meaning = responseText.take(500))))
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
