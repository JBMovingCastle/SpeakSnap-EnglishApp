package com.speaksnap.english.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "speaksnap_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        // ====== OCR (图片识别) ======
        private val K_OCR_PROVIDER = stringPreferencesKey("ocr_provider")
        private val K_OCR_KEY      = stringPreferencesKey("ocr_key")
        private val K_OCR_MODEL    = stringPreferencesKey("ocr_model")

        // ====== Text (文本分析) ======
        private val K_TEXT_PROVIDER = stringPreferencesKey("text_provider")
        private val K_TEXT_KEY      = stringPreferencesKey("text_key")
        private val K_TEXT_MODEL    = stringPreferencesKey("text_model")

        // ====== Conversation (对话) ======
        private val K_CONVO_PROVIDER = stringPreferencesKey("convo_provider")
        private val K_CONVO_KEY      = stringPreferencesKey("convo_key")
        private val K_CONVO_MODEL    = stringPreferencesKey("convo_model")

        // ====== Doubao extra (voice TTS/ASR) ======
        private val K_DOUBAO_KEY = stringPreferencesKey("doubao_key")

        // ====== Claude extra ======
        private val K_CLAUDE_KEY   = stringPreferencesKey("claude_key")
        private val K_CLAUDE_MODEL = stringPreferencesKey("claude_model")

        // ====== General ======
        private val K_USER         = stringPreferencesKey("user_name")
        private val K_ACCENT       = stringPreferencesKey("accent")
        private val K_FIRST_LAUNCH = booleanPreferencesKey("first_launch")
    }

    // ---- defaults ----
    private fun ocrDefault()  = "tongyi"
    private fun textDefault() = "deepseek"
    private fun convoDefault()= "doubao"

    // ==================================================================
    //                         OCR Task
    // ==================================================================
    fun getOcrProvider() = blocking { context.dataStore.data.map { it[K_OCR_PROVIDER] ?: ocrDefault() }.first() }
    suspend fun getOcrApiKey(): String = when (getOcrProvider()) {
        "tongyi"   -> blocking { context.dataStore.data.map { it[K_OCR_KEY] ?: "" }.first() }
        "deepseek" -> blocking { context.dataStore.data.map { it[K_TEXT_KEY] ?: "" }.first() }
        "claude"   -> blocking { context.dataStore.data.map { it[K_CLAUDE_KEY] ?: "" }.first() }
        else -> ""
    }
    fun getOcrModel(): String = when (getOcrProvider()) {
        "tongyi"   -> "qwen-vl-max"
        "deepseek" -> "deepseek-chat"
        "claude"   -> blocking { context.dataStore.data.map { it[K_CLAUDE_MODEL] ?: "claude-sonnet-4-20250514" }.first() }
        else -> "qwen-vl-max"
    }
    val ocrProvider: Flow<String> = context.dataStore.data.map { it[K_OCR_PROVIDER] ?: ocrDefault() }
    val ocrKey: Flow<String> = context.dataStore.data.map { it[K_OCR_KEY] ?: "" }
    suspend fun setOcrProvider(v: String) { context.dataStore.edit { it[K_OCR_PROVIDER] = v } }
    suspend fun setOcrKey(v: String) { context.dataStore.edit { it[K_OCR_KEY] = v } }

    // ==================================================================
    //                         Text Task
    // ==================================================================
    fun getTextProvider() = blocking { context.dataStore.data.map { it[K_TEXT_PROVIDER] ?: textDefault() }.first() }
    suspend fun getTextApiKey(): String = when (getTextProvider()) {
        "deepseek" -> blocking { context.dataStore.data.map { it[K_TEXT_KEY] ?: "" }.first() }
        "tongyi"   -> blocking { context.dataStore.data.map { it[K_OCR_KEY] ?: "" }.first() }
        "claude"   -> blocking { context.dataStore.data.map { it[K_CLAUDE_KEY] ?: "" }.first() }
        else -> ""
    }
    fun getTextModel(): String = when (getTextProvider()) {
        "deepseek" -> "deepseek-chat"
        "tongyi"   -> "qwen-max"
        "claude"   -> blocking { context.dataStore.data.map { it[K_CLAUDE_MODEL] ?: "claude-sonnet-4-20250514" }.first() }
        else -> "deepseek-chat"
    }
    val textProvider: Flow<String> = context.dataStore.data.map { it[K_TEXT_PROVIDER] ?: textDefault() }
    val textKey: Flow<String> = context.dataStore.data.map { it[K_TEXT_KEY] ?: "" }
    suspend fun setTextProvider(v: String) { context.dataStore.edit { it[K_TEXT_PROVIDER] = v } }
    suspend fun setTextKey(v: String) { context.dataStore.edit { it[K_TEXT_KEY] = v } }

    // ==================================================================
    //                      Conversation Task
    // ==================================================================
    fun getConversationProvider() = blocking { context.dataStore.data.map { it[K_CONVO_PROVIDER] ?: convoDefault() }.first() }
    suspend fun getConversationApiKey(): String = when (getConversationProvider()) {
        "doubao"   -> blocking { context.dataStore.data.map { it[K_DOUBAO_KEY] ?: "" }.first() }
        "deepseek" -> blocking { context.dataStore.data.map { it[K_TEXT_KEY] ?: "" }.first() }
        "claude"   -> blocking { context.dataStore.data.map { it[K_CLAUDE_KEY] ?: "" }.first() }
        else -> ""
    }
    fun getConversationModel(): String = when (getConversationProvider()) {
        "doubao"   -> "doubao-1.5-pro-32k"
        "deepseek" -> "deepseek-chat"
        "claude"   -> blocking { context.dataStore.data.map { it[K_CLAUDE_MODEL] ?: "claude-sonnet-4-20250514" }.first() }
        else -> "doubao-1.5-pro-32k"
    }
    val convoProvider: Flow<String> = context.dataStore.data.map { it[K_CONVO_PROVIDER] ?: convoDefault() }
    val convoKey: Flow<String> = context.dataStore.data.map { it[K_CONVO_KEY] ?: "" }
    suspend fun setConvoProvider(v: String) { context.dataStore.edit { it[K_CONVO_PROVIDER] = v } }
    suspend fun setConvoKey(v: String) { context.dataStore.edit { it[K_CONVO_KEY] = v } }

    // ==================================================================
    //                   Doubao extra (voice TTS/ASR)
    // ==================================================================
    val doubaoKey: Flow<String> = context.dataStore.data.map { it[K_DOUBAO_KEY] ?: "" }
    suspend fun getDoubaoApiKey(): String = blocking { context.dataStore.data.map { it[K_DOUBAO_KEY] ?: "" }.first() }
    suspend fun setDoubaoKey(v: String) { context.dataStore.edit { it[K_DOUBAO_KEY] = v } }

    // ==================================================================
    //                         Claude
    // ==================================================================
    val claudeKey: Flow<String> = context.dataStore.data.map { it[K_CLAUDE_KEY] ?: "" }
    suspend fun setClaudeKey(v: String) { context.dataStore.edit { it[K_CLAUDE_KEY] = v } }
    suspend fun setClaudeModel(v: String) { context.dataStore.edit { it[K_CLAUDE_MODEL] = v } }

    // ==================================================================
    //                         General
    // ==================================================================
    val userName: Flow<String> = context.dataStore.data.map { it[K_USER] ?: "Alex" }
    val accent: Flow<String> = context.dataStore.data.map { it[K_ACCENT] ?: "american" }
    val isFirstLaunch: Flow<Boolean> = context.dataStore.data.map { it[K_FIRST_LAUNCH] ?: true }
    suspend fun getUserName(): String = userName.first()
    suspend fun setUserName(v: String) { context.dataStore.edit { it[K_USER] = v } }
    suspend fun setAccent(v: String) { context.dataStore.edit { it[K_ACCENT] = v } }
    suspend fun setFirstLaunchDone() { context.dataStore.edit { it[K_FIRST_LAUNCH] = false } }

    // helper for sync reads in suspend context
    private fun <T> blocking(block: suspend () -> T): T = kotlinx.coroutines.runBlocking { block() }
}
