package com.speaksnap.english.data.repository

import android.util.Base64
import com.speaksnap.english.data.remote.dto.CaseStudyItem
import com.speaksnap.english.data.remote.dto.ExtractedContent
import com.speaksnap.english.data.remote.dto.OpenAIMessage
import java.io.File
import java.io.FileOutputStream

/**
 * 统一 AI 服务入口 — 按任务类型智能路由
 *
 *             OCR/视觉       文本分析       语音/对话
 * 默认路由：  通义千问 Qwen    DeepSeek V3    豆包 Doubao
 * 备选：     DeepSeek/Claude  Tongyi/Claude  DeepSeek/Claude
 *
 * 用户可在设置中为每个任务单独选择服务商。
 */
class AIService(
    private val settings: SettingsRepository,
    private val tongyi:   TongyiRepository   = TongyiRepository(),
    private val deepseek: DeepSeekRepository = DeepSeekRepository(),
    private val doubao:   DoubaoRepository   = DoubaoRepository(),
    private val claude:   ClaudeRepository   = ClaudeRepository()
) {
    // ========== 拍照 OCR → 默认 阿里通义千问 ==========

    suspend fun analyzeImage(imagePath: String): Result<ExtractedContent> {
        val p = settings.getOcrProvider()
        val key = settings.getOcrApiKey()
        val model = settings.getOcrModel()
        if (key.isBlank()) return Result.failure(err("OCR"))
        return when (p) {
            "tongyi"   -> tongyi.analyzeImage(key, imagePath, model)
            "deepseek" -> deepseek.analyzeImage(key, imagePath, model)
            "claude"   -> claude.analyzeImage(key, imagePath)
            else       -> tongyi.analyzeImage(key, imagePath, model)
        }
    }

    // ========== 文本分析 → 默认 DeepSeek ==========

    suspend fun analyzeText(text: String, title: String): Result<ExtractedContent> {
        val p = settings.getTextProvider()
        val key = settings.getTextApiKey()
        val model = settings.getTextModel()
        if (key.isBlank()) return Result.failure(err("文本分析"))
        return when (p) {
            "deepseek" -> deepseek.analyzeText(key, text, title, model)
            "tongyi"   -> tongyi.analyzeText(key, text, title, model)
            "claude"   -> claude.analyzeText(key, text, title)
            else       -> deepseek.analyzeText(key, text, title, model)
        }
    }

    // ========== AI 对话 → 默认豆包 ==========

    suspend fun generateConversation(
        caseStudy: CaseStudyItem,
        history: List<Pair<String, String>>,
        userMessage: String
    ): Result<String> {
        val p = settings.getConversationProvider()
        val key = settings.getConversationApiKey()
        val model = settings.getConversationModel()
        if (key.isBlank()) return Result.failure(err("对话"))
        return when (p) {
            "doubao"   -> doubao.generateConversation(key, caseStudy, history, userMessage, model)
            "deepseek" -> deepseek.generateConversation(key, caseStudy, history, userMessage, model)
            "claude"   -> claude.generateConversation(key, caseStudy, history, userMessage)
            else       -> doubao.generateConversation(key, caseStudy, history, userMessage, model)
        }
    }

    // ========== 豆包语音 TTS — 单词/句子朗读 ==========

    suspend fun speakWord(word: String, example: String? = null): Result<ByteArray> {
        val key = settings.getDoubaoApiKey()
        if (key.isBlank()) return Result.failure(err("语音"))
        val text = if (example != null) "$word. $example" else word
        return doubao.synthesizeSpeech(key, text)
    }

    // ========== 豆包语音评分 — 跟读纠音 ==========

    suspend fun evaluatePronunciation(reference: String, userTranscript: String): Result<String> {
        val key = settings.getDoubaoApiKey()
        if (key.isBlank()) return Result.failure(err("语音评分"))
        return doubao.pronunciationScore(key, reference, userTranscript)
    }

    // ========== 保存语音到缓存 ==========

    fun saveSpeech(bytes: ByteArray, cacheDir: File): String {
        val file = File(cacheDir, "speaksnap_speech_${System.currentTimeMillis()}.mp3")
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { it.write(bytes) }
        return file.absolutePath
    }

    private fun err(task: String) = Exception("请在设置中配置${task}的 API Key")
}
