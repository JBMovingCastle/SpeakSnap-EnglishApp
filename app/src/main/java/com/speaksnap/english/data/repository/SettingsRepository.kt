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
        private val KEY_API_KEY = stringPreferencesKey("anthropic_api_key")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_MODEL = stringPreferencesKey("model")
        private val KEY_ACCENT = stringPreferencesKey("accent")
        private val KEY_FIRST_LAUNCH = booleanPreferencesKey("first_launch")
    }

    val apiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_API_KEY] ?: ""
    }

    val userName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_NAME] ?: "Alex"
    }

    val model: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_MODEL] ?: "claude-sonnet-4-20250514"
    }

    val accent: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_ACCENT] ?: "american"
    }

    val isFirstLaunch: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_FIRST_LAUNCH] ?: true
    }

    suspend fun getApiKey(): String = apiKey.first()

    suspend fun setApiKey(key: String) {
        context.dataStore.edit { it[KEY_API_KEY] = key }
    }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { it[KEY_USER_NAME] = name }
    }

    suspend fun setModel(model: String) {
        context.dataStore.edit { it[KEY_MODEL] = model }
    }

    suspend fun setAccent(accent: String) {
        context.dataStore.edit { it[KEY_ACCENT] = accent }
    }

    suspend fun setFirstLaunchDone() {
        context.dataStore.edit { it[KEY_FIRST_LAUNCH] = false }
    }
}
