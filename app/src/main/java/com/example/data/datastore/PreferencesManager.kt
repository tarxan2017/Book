package com.example.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Context.dataStore by preferencesDataStore(name = "living_page_preferences")

class PreferencesManager(private val context: Context) {

    companion object {
        private val KEY_IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        private val KEY_GENERATIONS_TODAY = intPreferencesKey("generations_today")
        private val KEY_LAST_GENERATION_DATE = stringPreferencesKey("last_generation_date")
        private val KEY_GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        private val KEY_VIDEO_API_KEY = stringPreferencesKey("video_api_key")
        private val KEY_READER_FONT_SIZE = floatPreferencesKey("reader_font_size")
        private val KEY_TTS_SPEED = floatPreferencesKey("tts_speed")
        const val MAX_DAILY_GENERATIONS = 5
    }

    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_IS_DARK_THEME] ?: true // По умолчанию темная тема как на экранах 1-6
    }

    suspend fun setDarkTheme(isDark: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_DARK_THEME] = isDark
        }
    }

    val geminiApiKey: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_GEMINI_API_KEY] ?: ""
    }

    suspend fun setGeminiApiKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_GEMINI_API_KEY] = key
        }
    }

    val videoApiKey: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_VIDEO_API_KEY] ?: ""
    }

    suspend fun setVideoApiKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_VIDEO_API_KEY] = key
        }
    }

    val readerFontSize: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[KEY_READER_FONT_SIZE] ?: 17f
    }

    suspend fun setReaderFontSize(size: Float) {
        context.dataStore.edit { preferences ->
            preferences[KEY_READER_FONT_SIZE] = size
        }
    }

    val ttsSpeed: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[KEY_TTS_SPEED] ?: 1.0f
    }

    suspend fun setTtsSpeed(speed: Float) {
        context.dataStore.edit { preferences ->
            preferences[KEY_TTS_SPEED] = speed
        }
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    val remainingGenerations: Flow<Int> = context.dataStore.data.map { preferences ->
        val today = getTodayDateString()
        val lastDate = preferences[KEY_LAST_GENERATION_DATE] ?: ""
        val used = if (lastDate == today) {
            preferences[KEY_GENERATIONS_TODAY] ?: 1 // Из макета 4 из 5 осталось, значит 1 использована
        } else {
            0
        }
        (MAX_DAILY_GENERATIONS - used).coerceIn(0, MAX_DAILY_GENERATIONS)
    }

    suspend fun consumeGeneration(): Boolean {
        val today = getTodayDateString()
        var success = false
        context.dataStore.edit { preferences ->
            val lastDate = preferences[KEY_LAST_GENERATION_DATE] ?: ""
            val used = if (lastDate == today) {
                preferences[KEY_GENERATIONS_TODAY] ?: 1
            } else {
                0
            }

            if (used < MAX_DAILY_GENERATIONS) {
                preferences[KEY_LAST_GENERATION_DATE] = today
                preferences[KEY_GENERATIONS_TODAY] = used + 1
                success = true
            } else {
                success = false
            }
        }
        return success
    }

    suspend fun getRemainingGenerationsSync(): Int {
        return remainingGenerations.first()
    }
}
