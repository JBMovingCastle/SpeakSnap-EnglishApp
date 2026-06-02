package com.speaksnap.english.data.repository

import com.speaksnap.english.data.remote.dto.CaseStudyItem
import com.speaksnap.english.data.remote.dto.ExtractedContent

/**
 * Unified AI service — routes to Claude or DeepSeek based on user settings.
 * All screens use this single entry point.
 */
class AIService(
    private val settingsRepo: SettingsRepository,
    private val claudeRepo: ClaudeRepository = ClaudeRepository(),
    private val deepseekRepo: DeepSeekRepository = DeepSeekRepository()
) {
    suspend fun getActiveProvider(): String = settingsRepo.getActiveProvider()

    suspend fun analyzeImage(imagePath: String): Result<ExtractedContent> {
        val provider = settingsRepo.getActiveProvider()
        val apiKey = settingsRepo.getActiveApiKey()
        val model = settingsRepo.getActiveModel()

        if (apiKey.isBlank()) return Result.failure(Exception("请先在设置中输入 API Key"))

        return when (provider) {
            "deepseek" -> deepseekRepo.analyzeImage(apiKey, imagePath, model)
            else -> claudeRepo.analyzeImage(apiKey, imagePath)
        }
    }

    suspend fun generateConversation(
        caseStudy: CaseStudyItem,
        conversationHistory: List<Pair<String, String>>,
        userMessage: String
    ): Result<String> {
        val provider = settingsRepo.getActiveProvider()
        val apiKey = settingsRepo.getActiveApiKey()
        val model = settingsRepo.getActiveModel()

        if (apiKey.isBlank()) return Result.failure(Exception("请先在设置中输入 API Key"))

        return when (provider) {
            "deepseek" -> deepseekRepo.generateConversation(apiKey, caseStudy, conversationHistory, userMessage, model)
            else -> claudeRepo.generateConversation(apiKey, caseStudy, conversationHistory, userMessage)
        }
    }

    suspend fun analyzeText(text: String, title: String): Result<ExtractedContent> {
        val provider = settingsRepo.getActiveProvider()
        val apiKey = settingsRepo.getActiveApiKey()
        val model = settingsRepo.getActiveModel()

        if (apiKey.isBlank()) return Result.failure(Exception("请先在设置中输入 API Key"))

        return when (provider) {
            "deepseek" -> deepseekRepo.analyzeText(apiKey, text, title, model)
            else -> claudeRepo.analyzeText(apiKey, text, title)
        }
    }
}
