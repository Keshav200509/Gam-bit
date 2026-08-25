package com.example.domain.achievements

import com.example.domain.model.PlayerProgress

typealias AchievementId = String

enum class GameResult {
    WIN, LOSE, DRAW
}

data class GameStats(
    val playerScore: Int,
    val aiScore: Int,
    val isWin: Boolean,
    val wasBehindAtRound8: Boolean,
    val durationMs: Long,
    val lockUsesCount: Int,
    val totalGamesPlayed: Int,
    val totalWinsCount: Int,
    val scoutMasterGamesCount: Int = 0,
    val lockMasterGamesCount: Int = 0,
    val trustedIntelCount: Int = 0,
    val identifiedBluffCount: Int = 0
)

class AchievementChecker {
    fun checkAchievements(
        progress: PlayerProgress,
        gameResult: GameResult,
        gameStats: GameStats
    ): List<AchievementId> {
        val unlocked = mutableListOf<AchievementId>()

        // FIRST_WIN: total wins == 1 (or isWin and previous wins was 0)
        if (gameStats.totalWinsCount == 1 || (gameStats.isWin && gameStats.totalWinsCount >= 1)) {
            unlocked.add("first_win")
        }

        // STRATEGIST: total wins == 10
        if (gameStats.totalWinsCount >= 10) {
            unlocked.add("strategist")
        }

        // PERFECTIONIST: won by 50+ points
        if (gameStats.isWin && (gameStats.playerScore - gameStats.aiScore) >= 50) {
            unlocked.add("perfectionist")
        }

        // COMEBACK_KID: won after being behind by 20+ at round 8
        if (gameStats.isWin && gameStats.wasBehindAtRound8) {
            unlocked.add("comeback_kid")
        }

        // SCOUT_MASTER: used scout effectively 10 times (scouted clear → claimed uncontested)
        if (gameStats.scoutMasterGamesCount >= 10) {
            unlocked.add("scout_master")
        }

        // LOCK_MASTER: won 5 games using both locks every game
        if (gameStats.lockMasterGamesCount >= 5) {
            unlocked.add("lock_master")
        }

        // INTEL_ANALYST: trusted accurate intel correctly 5 times
        if (gameStats.trustedIntelCount >= 5) {
            unlocked.add("intel_analyst")
        }

        // BLUFF_CALLER: identified bluff intel 3 times
        if (gameStats.identifiedBluffCount >= 3) {
            unlocked.add("bluff_caller")
        }

        // SPEED_DEMON: won in under 6 minutes
        if (gameStats.isWin && gameStats.durationMs < 6 * 60 * 1000) {
            unlocked.add("speed_demon")
        }

        // IRON_PLAYER: played 50 games
        if (gameStats.totalGamesPlayed >= 50) {
            unlocked.add("iron_player")
        }

        // CHAMPION: won all 9 configurations
        val hasAllWins = progress.arena1Level1Wins > 0 && progress.arena1Level2Wins > 0 && progress.arena1Level3Wins > 0 &&
                progress.arena2Level1Wins > 0 && progress.arena2Level2Wins > 0 && progress.arena2Level3Wins > 0 &&
                progress.arena3Level1Wins > 0 && progress.arena3Level2Wins > 0 && progress.arena3Level3Wins > 0
        if (progress.championUnlocked || hasAllWins) {
            unlocked.add("champion")
        }

        // ARENA_MASTER: won level 3 of all 3 arenas
        val hasLevel3Wins = progress.arena1Level3Wins > 0 && progress.arena2Level3Wins > 0 && progress.arena3Level3Wins > 0
        if (hasLevel3Wins) {
            unlocked.add("arena_master")
        }

        return unlocked
    }
}
