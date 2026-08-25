package com.example.domain.usecase

import com.example.data.firestore.GameHistoryRepository
import com.example.data.firestore.GameRecord
import com.example.data.firestore.UserProfileRepository
import com.example.data.firestore.GameResult as RepoGameResult
import com.example.data.model.UserProfile
import com.example.domain.achievements.AchievementChecker
import com.example.domain.achievements.GameStats
import com.example.domain.achievements.GameResult as CheckGameResult
import com.example.domain.model.Arena
import com.example.domain.model.GameLevel
import com.example.domain.model.PlayerProgress
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class RecordGameResult @Inject constructor(
    private val profileRepository: UserProfileRepository,
    private val historyRepository: GameHistoryRepository
) {
    private val achievementChecker = AchievementChecker()

    suspend operator fun invoke(
        playerScore: Int,
        aiScore: Int,
        arena: Arena?,
        level: GameLevel?,
        isPvP: Boolean,
        wasBehindAtRound8: Boolean,
        durationMs: Long,
        lockUsesCount: Int,
        scoutUsesCount: Int,
        intelCorrectCount: Int
    ): Result<Pair<UserProfile, List<String>>> {
        try {
            val profile = profileRepository.getCurrentUserProfile().first()
                ?: return Result.failure(Exception("No user profile found"))

            val won = playerScore > aiScore
            val isDraw = playerScore == aiScore
            val repoResult = when {
                won -> RepoGameResult.WIN
                isDraw -> RepoGameResult.DRAW
                else -> RepoGameResult.LOSS
            }

            // 1. Update stats in repository
            val statsResult = profileRepository.updateStats(repoResult, isPvP)
            val updatedStats = statsResult.getOrThrow()

            // 2. Update arena progress if vs AI
            val updatedProgress = if (!isPvP && arena != null && level != null) {
                profileRepository.updateArenaProgress(arena, level, won, playerScore).getOrThrow()
            } else {
                profile.arenaProgress
            }

            // 3. Construct domain helper model for Achievements checking
            val dummyPlayerProgress = PlayerProgress(
                arena1Level1Wins = updatedProgress.arena1Level1Wins,
                arena1Level2Wins = updatedProgress.arena1Level2Wins,
                arena1Level3Wins = updatedProgress.arena1Level3Wins,
                arena2Level1Wins = updatedProgress.arena2Level1Wins,
                arena2Level2Wins = updatedProgress.arena2Level2Wins,
                arena2Level3Wins = updatedProgress.arena2Level3Wins,
                arena3Level1Wins = updatedProgress.arena3Level1Wins,
                arena3Level2Wins = updatedProgress.arena3Level2Wins,
                arena3Level3Wins = updatedProgress.arena3Level3Wins,
                bestScores = updatedProgress.bestScores,
                arena2Unlocked = updatedProgress.arena2Unlocked,
                arena3Unlocked = updatedProgress.arena3Unlocked,
                championUnlocked = updatedProgress.championUnlocked
            )

            val checkResult = when {
                won -> CheckGameResult.WIN
                isDraw -> CheckGameResult.DRAW
                else -> CheckGameResult.LOSE
            }

            val gameStats = GameStats(
                playerScore = playerScore,
                aiScore = aiScore,
                isWin = won,
                wasBehindAtRound8 = wasBehindAtRound8,
                durationMs = durationMs,
                lockUsesCount = lockUsesCount,
                totalGamesPlayed = updatedStats.totalGames,
                totalWinsCount = updatedStats.totalWins,
                scoutMasterGamesCount = if (scoutUsesCount >= 3) 1 else 0,
                lockMasterGamesCount = if (lockUsesCount >= 2 && won) 1 else 0,
                trustedIntelCount = intelCorrectCount,
                identifiedBluffCount = 0
            )

            // 4. Check achievements
            val potentialUnlocks = achievementChecker.checkAchievements(
                dummyPlayerProgress,
                checkResult,
                gameStats
            )

            // Calculate only new achievements
            val newlyUnlocked = potentialUnlocks.filter { !profile.achievements.contains(it) }

            // 5. Unlock achievements in DB
            newlyUnlocked.forEach { achId ->
                profileRepository.unlockAchievement(achId).getOrThrow()
            }

            // 6. Save game record to history
            val gameRecord = GameRecord(
                userId = profile.uid,
                playerScore = playerScore,
                aiScore = aiScore,
                result = repoResult.name,
                isPvP = isPvP,
                roundsPlayed = 12,
                timestamp = System.currentTimeMillis()
            )
            historyRepository.saveGameRecord(gameRecord).getOrThrow()

            // Fetch the fully updated user profile to return
            val finalProfile = profileRepository.getCurrentUserProfile().first() ?: profile

            return Result.success(Pair(finalProfile, newlyUnlocked))
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}
