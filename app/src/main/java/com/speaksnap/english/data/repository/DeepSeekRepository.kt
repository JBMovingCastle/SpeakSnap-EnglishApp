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

/**
 * DeepSeek — 专长：文本分析/推理，性价比极高
 * deepseek-chat (V3): ¥1/百万token，支持图片
 * deepseek-reasoner (R1): ¥4/百万token，深度推理
 */
class DeepSeekRepository(
    private val api: DeepSeekApiService = NetworkModule.deepseekService()
) {

    suspend fun analyzeImage(apiKey: String, imagePath: String, model: String = "deepseek-chat"): Result<ExtractedContent> = runCatching {
        val b64 = compressAndEncode(imagePath)
        val msgs = listOf(
            OpenAIMessage("system", OCR_PROMPT),
            OpenAIMessage("user", listOf(
                OpenAIContentPart("image_url", imageUrl = OpenAIImageUrl("data:image/jpeg;base64,$b64")),
                OpenAIContentPart("text", "提取所有英语学习内容，返回JSON")
            ))
        )
        val resp = api.createChatCompletion("Bearer $apiKey", OpenAIChatRequest(model, msgs, 4096))
        parse(resp.choices.firstOrNull()?.message?.content ?: "")
    }

    suspend fun analyzeText(apiKey: String, text: String, title: String, model: String = "deepseek-chat"): Result<ExtractedContent> = runCatching {
        val resp = api.createChatCompletion("Bearer $apiKey", OpenAIChatRequest(model, listOf(
            OpenAIMessage("system", TEXT_PROMPT),
            OpenAIMessage("user", "Title: $title\n\nContent:\n$text")
        ), 4096))
        parse(resp.choices.firstOrNull()?.message?.content ?: "")
    }

    suspend fun generateConversation(
        apiKey: String, caseStudy: CaseStudyItem,
        history: List<Pair<String, String>>, userMessage: String,
        model: String = "deepseek-chat"
    ): Result<String> = runCatching {
        val msgs = mutableListOf<OpenAIMessage>()
        msgs.add(OpenAIMessage("system", CONVO_PROMPT.format(caseStudy.title, caseStudy.scenario, caseStudy.keyPoints.joinToString(", "))))
        history.forEach { (r, m) -> msgs.add(OpenAIMessage(if (r == "ai") "assistant" else "user", m)) }
        msgs.add(OpenAIMessage("user", userMessage))
        val resp = api.createChatCompletion("Bearer $apiKey", OpenAIChatRequest(model, msgs, 1024, 0.8))
        resp.choices.firstOrNull()?.message?.content ?: ""
    }

    private fun compressAndEncode(path: String, maxDim: Int = 1568): String {
        val f = File(path)
        return if (f.exists()) {
            val bmp = BitmapFactory.decodeFile(path)
            val ratio = minOf(maxDim.toFloat() / (bmp?.width ?: 1), 1f)
            val s = if (bmp != null && ratio < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * ratio).toInt(), (bmp.height * ratio).toInt(), true) else bmp
            val os = ByteArrayOutputStream(); s?.compress(Bitmap.CompressFormat.JPEG, 80, os); s?.recycle(); bmp?.recycle()
            Base64.encodeToString(os.toByteArray(), Base64.NO_WRAP)
        } else path
    }

    private fun parse(text: String): ExtractedContent = try {
        val j = JsonParser.parseString(text.replace("```json", "").replace("```", "").trim()).asJsonObject
        ExtractedContent(
            title = j.get("title")?.asString ?: "",
            words = j.getAsJsonArray("words")?.map { val o=it.asJsonObject; WordItem(o.get("word")?.asString?:"",o.get("phonetic")?.asString?:"",o.get("meaning")?.asString?:"",o.get("example")?.asString?:"") }?:emptyList(),
            phrases = j.getAsJsonArray("phrases")?.map { val o=it.asJsonObject; PhraseItem(o.get("phrase")?.asString?:"",o.get("meaning")?.asString?:"",o.get("usage")?.asString?:"") }?:emptyList(),
            grammarPoints = j.getAsJsonArray("grammar_points")?.map { val o=it.asJsonObject; GrammarItem(o.get("title")?.asString?:"",o.get("explanation")?.asString?:"",o.get("example")?.asString?:"") }?:emptyList(),
            caseStudies = j.getAsJsonArray("case_studies")?.map { val o=it.asJsonObject; CaseStudyItem(o.get("title")?.asString?:"",o.get("scenario")?.asString?:"",o.getAsJsonArray("key_points")?.map{it.asString}?:emptyList(),o.get("starter_dialogue")?.asString?:"") }?:emptyList()
        )
    } catch (e: Exception) { ExtractedContent("parse error", listOf(WordItem("raw", meaning=text.take(500)))) }

    companion object {
        val OCR_PROMPT = """You are an expert English teacher. Analyze the image and extract ALL English learning content. Return ONLY JSON with: title, words[{word,phonetic,meaning,example}], phrases[{phrase,meaning,usage}], grammar_points[{title,explanation,example}], case_studies[{title,scenario,key_points[],starter_dialogue}]. Chinese meanings for all. ONLY valid JSON.""".trimIndent()
        val TEXT_PROMPT = """You are an expert English teacher. Structure the text into learning content. Return ONLY JSON with words, phrases, grammar_points, case_studies. Chinese meanings for all items.""".trimIndent()
        val CONVO_PROMPT = """Native English teacher. Scenario: %s | Context: %s | Keywords: %s. 2-4 sentences, correct mistakes with 💡 Tip, supportive tone.""".trimIndent()
    }
}
