package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "living_page_settings")

class UserPreferencesRepository(private val context: Context) {

    private val KEY_DAILY_COUNT = intPreferencesKey("daily_generation_count")
    private val KEY_LAST_DATE = stringPreferencesKey("daily_generation_date")
    private val KEY_CUSTOM_GEMINI_KEY = stringPreferencesKey("custom_gemini_api_key")
    private val KEY_CUSTOM_VIDEO_KEY = stringPreferencesKey("custom_video_api_key")
    private val KEY_FONT_SIZE = floatPreferencesKey("reader_font_size")
    private val KEY_THEME_MODE = stringPreferencesKey("reader_theme_mode") // "cream", "parchment", "night"

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    val remainingGenerations: Flow<Int> = context.dataStore.data.map { preferences ->
        val today = getTodayDateString()
        val savedDate = preferences[KEY_LAST_DATE] ?: ""
        if (savedDate != today) {
            5
        } else {
            val count = preferences[KEY_DAILY_COUNT] ?: 0
            (5 - count).coerceAtLeast(0)
        }
    }

    suspend fun canGenerateVideo(): Boolean {
        val prefs = context.dataStore.data.first()
        val today = getTodayDateString()
        val savedDate = prefs[KEY_LAST_DATE] ?: ""
        if (savedDate != today) return true
        val count = prefs[KEY_DAILY_COUNT] ?: 0
        return count < 5
    }

    suspend fun recordGeneration(): Boolean {
        var success = false
        context.dataStore.edit { preferences ->
            val today = getTodayDateString()
            val savedDate = preferences[KEY_LAST_DATE] ?: ""
            val currentCount = if (savedDate != today) 0 else (preferences[KEY_DAILY_COUNT] ?: 0)
            if (currentCount < 5) {
                preferences[KEY_LAST_DATE] = today
                preferences[KEY_DAILY_COUNT] = currentCount + 1
                success = true
            } else {
                success = false
            }
        }
        return success
    }

    val geminiApiKey: Flow<String> = context.dataStore.data.map { preferences ->
        val customKey = preferences[KEY_CUSTOM_GEMINI_KEY]?.trim().orEmpty()
        if (customKey.isNotEmpty()) {
            customKey
        } else {
            try {
                BuildConfig.GEMINI_API_KEY
            } catch (e: Exception) {
                ""
            }
        }
    }

    suspend fun saveGeminiApiKey(key: String) {
        context.dataStore.edit { it[KEY_CUSTOM_GEMINI_KEY] = key.trim() }
    }

    val videoApiKey: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_CUSTOM_VIDEO_KEY].orEmpty()
    }

    suspend fun saveVideoApiKey(key: String) {
        context.dataStore.edit { it[KEY_CUSTOM_VIDEO_KEY] = key.trim() }
    }

    val readerFontSize: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[KEY_FONT_SIZE] ?: 18f
    }

    suspend fun saveReaderFontSize(size: Float) {
        context.dataStore.edit { it[KEY_FONT_SIZE] = size }
    }

    val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_THEME_MODE] ?: "cream"
    }

    suspend fun saveThemeMode(mode: String) {
        context.dataStore.edit { it[KEY_THEME_MODE] = mode }
    }
}
