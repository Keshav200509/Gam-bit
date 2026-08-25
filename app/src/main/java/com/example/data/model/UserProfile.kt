package com.example.data.model

import com.example.domain.model.Arena
import com.example.domain.model.GameLevel

data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val friendCode: String = "",  // 6-character code for adding friends
    val stats: UserStats = UserStats(),
    val arenaProgress: ArenaProgress = ArenaProgress(),
    val achievements: List<String> = emptyList(),  // achievement IDs
    val createdAt: Long = System.currentTimeMillis(),
    val lastActive: Long = System.currentTimeMillis()
)

data class UserStats(
    val totalGames: Int = 0,
    val totalWins: Int = 0,
    val totalLosses: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val pvpGames: Int = 0,
    val pvpWins: Int = 0,
    val pvpRating: Int = 1000,  // ELO-style rating
    val dailyChallengeStreak: Int = 0,
    val lastDailyChallengeDate: String? = null  // YYYY-MM-DD
)

data class ArenaProgress(
    val arena1Level1Wins: Int = 0,
    val arena1Level2Wins: Int = 0,
    val arena1Level3Wins: Int = 0,
    val arena2Level1Wins: Int = 0,
    val arena2Level2Wins: Int = 0,
    val arena2Level3Wins: Int = 0,
    val arena3Level1Wins: Int = 0,
    val arena3Level2Wins: Int = 0,
    val arena3Level3Wins: Int = 0,
    val arena2Unlocked: Boolean = false,
    val arena3Unlocked: Boolean = false,
    val championUnlocked: Boolean = false,
    val bestScores: Map<String, Int> = emptyMap()
)
