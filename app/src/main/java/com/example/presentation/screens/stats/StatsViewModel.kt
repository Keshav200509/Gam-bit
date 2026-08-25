package com.example.presentation.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PreferencesManager
import com.example.data.local.entity.GameEntity
import com.example.data.repository.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AggregateStats(
    val totalGames: Int = 0,
    val winRate: Float = 0f,
    val avgScore: Float = 0f,
    val longestStreak: Int = 0,
    val currentStreak: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
    val avgClashesWon: Float = 0f,
    val avgIntelAccuracy: Float = 0f
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val statsRepository: StatsRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    val allGames: StateFlow<List<GameEntity>> = statsRepository.allGames
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unlockedAchievements: StateFlow<Set<String>> = preferencesManager.unlockedAchievements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val aggregateStats: StateFlow<AggregateStats> = allGames.map { games ->
        if (games.isEmpty()) return@map AggregateStats()

        val total = games.size
        val wins = games.count { it.result == "WIN" }
        val losses = games.count { it.result == "LOSE" }
        val draws = games.count { it.result == "DRAW" }
        val winRate = if (total > 0) (wins.toFloat() / total * 100) else 0f
        val avgScore = games.map { it.playerScore }.average().toFloat()
        val avgClashesWon = games.map { it.clashesWon }.average().toFloat()
        val avgIntelAccuracy = games.map { it.intelAccuracy }.average().toFloat()

        // Calculate streaks (chronological order)
        val sortedGames = games.sortedBy { it.timestamp }
        var maxStreak = 0
        var currentStreak = 0

        for (game in sortedGames) {
            if (game.result == "WIN") {
                currentStreak++
                if (currentStreak > maxStreak) {
                    maxStreak = currentStreak
                }
            } else {
                currentStreak = 0
            }
        }

        // Current active streak (from latest games backward)
        var activeStreak = 0
        val descendingGames = games.sortedByDescending { it.timestamp }
        for (game in descendingGames) {
            if (game.result == "WIN") {
                activeStreak++
            } else {
                break
            }
        }

        AggregateStats(
            totalGames = total,
            winRate = winRate,
            avgScore = avgScore,
            longestStreak = maxStreak,
            currentStreak = activeStreak,
            wins = wins,
            losses = losses,
            draws = draws,
            avgClashesWon = avgClashesWon,
            avgIntelAccuracy = avgIntelAccuracy
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AggregateStats())

    fun clearStats() {
        viewModelScope.launch {
            statsRepository.clearAllGames()
        }
    }
}
