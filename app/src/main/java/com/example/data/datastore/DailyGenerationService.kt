package com.example.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Context.generationDataStore by preferencesDataStore(name = "daily_generation_service_prefs")

data class DailyUsage(
    val usedCount: Int,
    val maxDaily: Int = 5,
    val remainingCount: Int,
    val isLimitReached: Boolean,
    val lastDate: String
)

interface DailyGenerationService {
    val usageFlow: Flow<DailyUsage>
    suspend fun canGenerate(): Boolean
    suspend fun tryConsumeGeneration(): Boolean
    suspend fun resetUsageForToday()
}

class DailyGenerationServiceImpl(
    private val context: Context
) : DailyGenerationService {

    companion object {
        const val MAX_DAILY_LIMIT = 5
        private val KEY_USED_GENERATIONS = intPreferencesKey("daily_used_generations")
        private val KEY_LAST_USAGE_DATE = stringPreferencesKey("daily_last_usage_date")
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    override val usageFlow: Flow<DailyUsage> = context.generationDataStore.data.map { preferences ->
        val today = getTodayDateString()
        val lastDate = preferences[KEY_LAST_USAGE_DATE] ?: ""
        val used = if (lastDate == today) {
            preferences[KEY_USED_GENERATIONS] ?: 1 // Начальное значение 1 (осталось 4 из 5)
        } else {
            0
        }
        val remaining = (MAX_DAILY_LIMIT - used).coerceIn(0, MAX_DAILY_LIMIT)
        DailyUsage(
            usedCount = used,
            maxDaily = MAX_DAILY_LIMIT,
            remainingCount = remaining,
            isLimitReached = remaining <= 0,
            lastDate = if (lastDate.isEmpty()) today else lastDate
        )
    }

    override suspend fun canGenerate(): Boolean {
        val current = usageFlow.first()
        return !current.isLimitReached
    }

    override suspend fun tryConsumeGeneration(): Boolean {
        val today = getTodayDateString()
        var allowed = false

        context.generationDataStore.edit { preferences ->
            val lastDate = preferences[KEY_LAST_USAGE_DATE] ?: ""
            val currentUsed = if (lastDate == today) {
                preferences[KEY_USED_GENERATIONS] ?: 1
            } else {
                0
            }

            if (currentUsed < MAX_DAILY_LIMIT) {
                preferences[KEY_LAST_USAGE_DATE] = today
                preferences[KEY_USED_GENERATIONS] = currentUsed + 1
                allowed = true
            } else {
                allowed = false
            }
        }

        return allowed
    }

    override suspend fun resetUsageForToday() {
        val today = getTodayDateString()
        context.generationDataStore.edit { preferences ->
            preferences[KEY_LAST_USAGE_DATE] = today
            preferences[KEY_USED_GENERATIONS] = 0
        }
    }
}
