package com.speaksnap.english.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.speaksnap.english.data.remote.NetworkModule
import com.speaksnap.english.data.remote.TongyiApiService
import com.speaksnap.english.data.remote.dto.*
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * 阿里通义千问 — 专长：图片 OCR / 视觉理解
 * 推荐模型：qwen-vl-max (最强视觉) / qwen-vl-plus (性价比)
 *
 * API: https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions
 * 文档: https://help.aliyun.com/zh/model-studio
 */
class TongyiRepository(
    private val api: TongyiApiService = NetworkModule.tongyiService()
) {

    suspend fun analyzeImage(
        apiKey: String,
        imagePath: String,
        model: String = "qwen-vl-max"
    ): Result<ExtractedContent> = runCatching {
        val b64 = compressAndEncode(imagePath)
        val messages = listOf(
            OpenAIMessage("system", OCR_SYSTEM_PROMPT),
            OpenAIMessage("user", listOf(
                OpenAIContentPart("image_url", imageUrl = OpenAIImageUrl("data:image/jpeg;base64,$b64")),
                OpenAIContentPart("text", "提取所有英语学习内容，返回JSON")
            ))
        )
        val resp = api.chat("Bearer $apiKey", OpenAIChatRequest(model, messages, 4096))
        parse(resp.choices.firstOrNull()?.message?.content ?: "")
    }

    suspend fun analyzeText(
        apiKey: String, text: String, title: String,
        model: String = "qwen-max"
    ): Result<ExtractedContent> = runCatching {
        val resp = api.chat("Bearer $apiKey", OpenAIChatRequest(model, listOf(
            OpenAIMessage("system", TEXT_ANALYSIS_PROMPT),
            OpenAIMessage("user", "Title: $title\n\nContent:\n$text")
        ), 4096))
        parse(resp.choices.firstOrNull()?.message?.content ?: "")
    }

    private fun compressAndEncode(path: String, maxDim: Int = 2048): String {
        val f = File(path)
        return if (f.exists()) {
            val bmp = BitmapFactory.decodeFile(path)
            val ratio = minOf(maxDim.toFloat() / (bmp?.width ?: 1), 1f)
            val scaled = if (bmp != null && ratio < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * ratio).toInt(), (bmp.height * ratio).toInt(), true) else bmp
            val os = ByteArrayOutputStream()
            scaled?.compress(Bitmap.CompressFormat.JPEG, 80, os)
            scaled?.recycle(); bmp?.recycle()
            Base64.encodeToString(os.toByteArray(), Base64.NO_WRAP)
        } else path
    }

    private fun parse(text: String): ExtractedContent = try {
        val j = JsonParser.parseString(text.replace("```json", "").replace("```", "").trim()).asJsonObject
        ExtractedContent(
            title = j.get("title")?.asString ?: "",
            words = j.getAsJsonArray("words")?.map {
                val o = it.asJsonObject
                WordItem(o.get("word")?.asString ?: "", o.get("phonetic")?.asString ?: "", o.get("meaning")?.asString ?: "", o.get("example")?.asString ?: "")
            } ?: emptyList(),
            phrases = j.getAsJsonArray("phrases")?.map {
                val o = it.asJsonObject
                PhraseItem(o.get("phrase")?.asString ?: "", o.get("meaning")?.asString ?: "", o.get("usage")?.asString ?: "")
            } ?: emptyList(),
            grammarPoints = j.getAsJsonArray("grammar_points")?.map {
                val o = it.asJsonObject
                GrammarItem(o.get("title")?.asString ?: "", o.get("explanation")?.asString ?: "", o.get("example")?.asString ?: "")
            } ?: emptyList(),
            caseStudies = j.getAsJsonArray("case_studies")?.map {
                val o = it.asJsonObject
                CaseStudyItem(o.get("title")?.asString ?: "", o.get("scenario")?.asString ?: "", o.getAsJsonArray("key_points")?.map { it.asString } ?: emptyList(), o.get("starter_dialogue")?.asString ?: "")
            } ?: emptyList()
        )
    } catch (e: Exception) {
        ExtractedContent("解析失败", listOf(WordItem("raw", meaning = text.take(500))))
    }

    companion object {
        val OCR_SYSTEM_PROMPT = """
You are an expert English teacher. Analyze the textbook/notes image and extract ALL content.

Return ONLY this JSON:
{
  "title": "Unit title",
  "words": [{"word":"...","phonetic":"/.../","meaning":"中文释义","example":"English sentence"}],
  "phrases": [{"phrase":"...","meaning":"中文释义","usage":"用法说明"}],
  "grammar_points": [{"title":"语法点","explanation":"中文解释","example":"例句"}],
  "case_studies": [{"title":"场景名","scenario":"场景描述","key_points":["..."],"starter_dialogue":"开场对话"}]
}
Extract every word/phrase visible. Give Chinese meanings + phonetics. ONLY valid JSON.
        """.trimIndent()

        val TEXT_ANALYSIS_PROMPT = """
You are an expert English teacher. Structure the provided notes into learning content.
Return ONLY valid JSON with words, phrases, grammar_points, case_studies (same format as image analysis).
Give Chinese meanings for all items.
        """.trimIndent()
    }
}
