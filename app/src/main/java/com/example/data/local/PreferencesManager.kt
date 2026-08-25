package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.DifficultyPreset
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gambit_preferences")

enum class AnimationSpeed {
    SLOW, NORMAL, FAST;

    val delayFactor: Float
        get() = when (this) {
            SLOW -> 1.5f
            NORMAL -> 1.0f
            FAST -> 0.5f
        }
}

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        private val KEY_SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        private val KEY_HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        private val KEY_ANIMATION_SPEED = stringPreferencesKey("animation_speed")
        private val KEY_AI_CAPABILITY_VISIBLE = booleanPreferencesKey("ai_capability_visible")
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val KEY_DIFFICULTY_PRESET = stringPreferencesKey("difficulty_preset")
        
        // Achievement & Stats Keys
        private val KEY_UNLOCKED_ACHIEVEMENTS = stringSetPreferencesKey("unlocked_achievements")
        private val KEY_SCOUT_EFFECTIVE_COUNT = intPreferencesKey("scout_effective_count")
        private val KEY_LOCK_MASTER_GAMES_COUNT = intPreferencesKey("lock_master_games_count")
        private val KEY_INTEL_ANALYST_COUNT = intPreferencesKey("intel_analyst_count")
        private val KEY_BLUFF_CALLER_COUNT = intPreferencesKey("bluff_caller_count")
        private val KEY_TOTAL_WINS = intPreferencesKey("total_wins")
        private val KEY_TOTAL_GAMES = intPreferencesKey("total_games")
        private val KEY_NEW_GAME_PLUS_ENABLED = booleanPreferencesKey("new_game_plus_enabled")
    }

    val newGamePlusEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_NEW_GAME_PLUS_ENABLED] ?: false
    }

    val soundEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_SOUND_ENABLED] ?: true
    }

    val hapticsEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_HAPTICS_ENABLED] ?: true
    }

    val animationSpeed: Flow<AnimationSpeed> = dataStore.data.map { preferences ->
        val name = preferences[KEY_ANIMATION_SPEED] ?: AnimationSpeed.NORMAL.name
        try {
            AnimationSpeed.valueOf(name)
        } catch (e: Exception) {
            AnimationSpeed.NORMAL
        }
    }

    val aiCapabilityVisible: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_AI_CAPABILITY_VISIBLE] ?: true
    }

    val onboardingCompleted: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_ONBOARDING_COMPLETED] ?: false
    }

    val difficultyPreset: Flow<DifficultyPreset> = dataStore.data.map { preferences ->
        val name = preferences[KEY_DIFFICULTY_PRESET] ?: DifficultyPreset.VETERAN.name
        try {
            DifficultyPreset.valueOf(name)
        } catch (e: Exception) {
            DifficultyPreset.VETERAN
        }
    }

    // Achievement Flows
    val unlockedAchievements: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[KEY_UNLOCKED_ACHIEVEMENTS] ?: emptySet()
    }

    val scoutEffectiveCount: Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_SCOUT_EFFECTIVE_COUNT] ?: 0
    }

    val lockMasterGamesCount: Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_LOCK_MASTER_GAMES_COUNT] ?: 0
    }

    val intelAnalystCount: Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_INTEL_ANALYST_COUNT] ?: 0
    }

    val bluffCallerCount: Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_BLUFF_CALLER_COUNT] ?: 0
    }

    val totalWins: Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_TOTAL_WINS] ?: 0
    }

    val totalGames: Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_TOTAL_GAMES] ?: 0
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_SOUND_ENABLED] = enabled
        }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_HAPTICS_ENABLED] = enabled
        }
    }

    suspend fun setAnimationSpeed(speed: AnimationSpeed) {
        dataStore.edit { preferences ->
            preferences[KEY_ANIMATION_SPEED] = speed.name
        }
    }

    suspend fun setAiCapabilityVisible(visible: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_AI_CAPABILITY_VISIBLE] = visible
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setDifficultyPreset(preset: DifficultyPreset) {
        dataStore.edit { preferences ->
            preferences[KEY_DIFFICULTY_PRESET] = preset.name
        }
    }

    // Setters & Mutators for Achievements and Stats
    suspend fun unlockAchievement(id: String): Boolean {
        var unlocked = false
        dataStore.edit { preferences ->
            val current = preferences[KEY_UNLOCKED_ACHIEVEMENTS] ?: emptySet()
            if (!current.contains(id)) {
                preferences[KEY_UNLOCKED_ACHIEVEMENTS] = current + id
                unlocked = true
            }
        }
        return unlocked
    }

    suspend fun incrementScoutEffectiveCount() {
        dataStore.edit { preferences ->
            val current = preferences[KEY_SCOUT_EFFECTIVE_COUNT] ?: 0
            preferences[KEY_SCOUT_EFFECTIVE_COUNT] = current + 1
        }
    }

    suspend fun incrementLockMasterGamesCount() {
        dataStore.edit { preferences ->
            val current = preferences[KEY_LOCK_MASTER_GAMES_COUNT] ?: 0
            preferences[KEY_LOCK_MASTER_GAMES_COUNT] = current + 1
        }
    }

    suspend fun incrementIntelAnalystCount() {
        dataStore.edit { preferences ->
            val current = preferences[KEY_INTEL_ANALYST_COUNT] ?: 0
            preferences[KEY_INTEL_ANALYST_COUNT] = current + 1
        }
    }

    suspend fun incrementBluffCallerCount() {
        dataStore.edit { preferences ->
            val current = preferences[KEY_BLUFF_CALLER_COUNT] ?: 0
            preferences[KEY_BLUFF_CALLER_COUNT] = current + 1
        }
    }

    suspend fun incrementTotalWins() {
        dataStore.edit { preferences ->
            val current = preferences[KEY_TOTAL_WINS] ?: 0
            preferences[KEY_TOTAL_WINS] = current + 1
        }
    }

    suspend fun incrementTotalGames() {
        dataStore.edit { preferences ->
            val current = preferences[KEY_TOTAL_GAMES] ?: 0
            preferences[KEY_TOTAL_GAMES] = current + 1
        }
    }

    suspend fun setNewGamePlusEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_NEW_GAME_PLUS_ENABLED] = enabled
        }
    }

    suspend fun resetAchievements() {
        dataStore.edit { preferences ->
            preferences[KEY_UNLOCKED_ACHIEVEMENTS] = emptySet()
            preferences[KEY_SCOUT_EFFECTIVE_COUNT] = 0
            preferences[KEY_LOCK_MASTER_GAMES_COUNT] = 0
            preferences[KEY_INTEL_ANALYST_COUNT] = 0
            preferences[KEY_BLUFF_CALLER_COUNT] = 0
            preferences[KEY_TOTAL_WINS] = 0
            preferences[KEY_TOTAL_GAMES] = 0
            preferences[KEY_NEW_GAME_PLUS_ENABLED] = false
        }
    }
}
