package com.speaksnap.english.data.repository

import android.util.Base64
import com.speaksnap.english.data.remote.DoubaoApiService
import com.speaksnap.english.data.remote.NetworkModule
import com.speaksnap.english.data.remote.dto.*
import java.io.File

/**
 * 豆包/火山引擎 Ark — 专长：语音TTS + ASR + 对话
 * 推荐模型：doubao-1.5-pro-32k (对话) / doubao-tts (语音合成)
 *
 * API: https://ark.cn-beijing.volces.com/api/v3/
 * 文档: https://www.volcengine.com/docs/82379
 */
class DoubaoRepository(
    private val api: DoubaoApiService = NetworkModule.doubaoService()
) {

    /** 文字对话 — 用于学习场景对话练习 */
    suspend fun chat(
        apiKey: String,
        messages: List<OpenAIMessage>,
        model: String = "doubao-1.5-pro-32k"
    ): Result<String> = runCatching {
        val resp = api.chat("Bearer $apiKey", OpenAIChatRequest(model, messages, 4096, 0.8))
        resp.choices.firstOrNull()?.message?.content ?: ""
    }

    /** 语音合成 TTS — 把单词/例句转成语音 */
    suspend fun synthesizeSpeech(
        apiKey: String,
        text: String,
        voice: String = "zh_female_qingxin",
        speed: Double = 0.9
    ): Result<ByteArray> = runCatching {
        val body = api.tts("Bearer $apiKey", TTSRequest(input = text, voice = voice, speed = speed))
        body.bytes()
    }

    /** 跟读评分 — 学生读，AI 打分 */
    suspend fun pronunciationScore(
        apiKey: String,
        referenceText: String,
        userTranscript: String,
        model: String = "doubao-1.5-pro-32k"
    ): Result<String> = runCatching {
        val msgs = listOf(
            OpenAIMessage("system", """
你是一个英语发音教练。学生跟读指定文本后，你需要评估发音准确度。
评分规则：
- 对比参考文本和学生的实际发音
- 指出发音错误的具体单词
- 给出 1-10 分的评分
- 用中文给出改进建议
回复格式：
**评分**：X/10
**发音错误**：[错误单词列表]
**改进建议**：[建议]
            """.trimIndent()),
            OpenAIMessage("user", "参考文本：$referenceText\n学生发音：$userTranscript\n请评分")
        )
        val resp = api.chat("Bearer $apiKey", OpenAIChatRequest(model, msgs, 1024, 0.3))
        resp.choices.firstOrNull()?.message?.content ?: "评分失败"
    }

    /** AI 英语对话老师 — 基于场景的对话练习 */
    suspend fun generateConversation(
        apiKey: String,
        caseStudy: CaseStudyItem,
        history: List<Pair<String, String>>,
        userMessage: String,
        model: String = "doubao-1.5-pro-32k"
    ): Result<String> = runCatching {
        val msgs = mutableListOf<OpenAIMessage>()
        msgs.add(OpenAIMessage("system", CONVERSATION_PROMPT.format(
            caseStudy.title, caseStudy.scenario, caseStudy.keyPoints.joinToString(", ")
        )))
        history.forEach { (role, msg) ->
            msgs.add(OpenAIMessage(if (role == "ai") "assistant" else "user", msg))
        }
        msgs.add(OpenAIMessage("user", userMessage))
        val resp = api.chat("Bearer $apiKey", OpenAIChatRequest(model, msgs, 1024, 0.8))
        resp.choices.firstOrNull()?.message?.content ?: ""
    }

    companion object {
        private val CONVERSATION_PROMPT = """
You are a native English teacher. Practice scenario: %s
Context: %s | Keywords: %s

Rules:
- 2-4 sentences per response, natural conversational tone
- Gently correct grammar mistakes (add 💡 Tip: ...)
- Use the keywords naturally
- Supportive and encouraging
- If student writes in Chinese, reply in English and guide them back
        """.trimIndent()
    }
}
