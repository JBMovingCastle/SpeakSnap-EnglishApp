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
        // Provider
        private val KEY_PROVIDER = stringPreferencesKey("ai_provider") // "deepseek" | "claude"

        // DeepSeek
        private val KEY_DEEPSEEK_KEY = stringPreferencesKey("deepseek_api_key")
        private val KEY_DEEPSEEK_MODEL = stringPreferencesKey("deepseek_model")

        // Claude
        private val KEY_CLAUDE_KEY = stringPreferencesKey("anthropic_api_key")
        private val KEY_CLAUDE_MODEL = stringPreferencesKey("claude_model")

        // General
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_ACCENT = stringPreferencesKey("accent")
        private val KEY_FIRST_LAUNCH = booleanPreferencesKey("first_launch")
    }

    // ---- Provider ----
    val activeProvider: Flow<String> = context.dataStore.data.map { it[KEY_PROVIDER] ?: "deepseek" }

    suspend fun getActiveProvider(): String = activeProvider.first()

    suspend fun setProvider(provider: String) {
        context.dataStore.edit { it[KEY_PROVIDER] = provider }
    }

    // ---- Convenience: get active key & model based on current provider ----
    suspend fun getActiveApiKey(): String = when (getActiveProvider()) {
        "deepseek" -> deepseekApiKey.first()
        else -> claudeApiKey.first()
    }

    suspend fun getActiveModel(): String = when (getActiveProvider()) {
        "deepseek" -> deepseekModel.first()
        else -> claudeModel.first()
    }

    // ---- DeepSeek ----
    val deepseekApiKey: Flow<String> = context.dataStore.data.map { it[KEY_DEEPSEEK_KEY] ?: "" }
    val deepseekModel: Flow<String> = context.dataStore.data.map { it[KEY_DEEPSEEK_MODEL] ?: "deepseek-chat" }

    suspend fun setDeepSeekApiKey(key: String) { context.dataStore.edit { it[KEY_DEEPSEEK_KEY] = key } }
    suspend fun setDeepSeekModel(model: String) { context.dataStore.edit { it[KEY_DEEPSEEK_MODEL] = model } }

    // ---- Claude ----
    val claudeApiKey: Flow<String> = context.dataStore.data.map { it[KEY_CLAUDE_KEY] ?: "" }
    val claudeModel: Flow<String> = context.dataStore.data.map { it[KEY_CLAUDE_MODEL] ?: "claude-sonnet-4-20250514" }

    suspend fun setClaudeApiKey(key: String) { context.dataStore.edit { it[KEY_CLAUDE_KEY] = key } }
    suspend fun setClaudeModel(model: String) { context.dataStore.edit { it[KEY_CLAUDE_MODEL] = model } }

    // ---- General ----
    val userName: Flow<String> = context.dataStore.data.map { it[KEY_USER_NAME] ?: "Alex" }
    val accent: Flow<String> = context.dataStore.data.map { it[KEY_ACCENT] ?: "american" }
    val isFirstLaunch: Flow<Boolean> = context.dataStore.data.map { it[KEY_FIRST_LAUNCH] ?: true }

    suspend fun getUserName(): String = userName.first()
    suspend fun setUserName(name: String) { context.dataStore.edit { it[KEY_USER_NAME] = name } }
    suspend fun setAccent(accent: String) { context.dataStore.edit { it[KEY_ACCENT] = accent } }
    suspend fun setFirstLaunchDone() { context.dataStore.edit { it[KEY_FIRST_LAUNCH] = false } }
}
