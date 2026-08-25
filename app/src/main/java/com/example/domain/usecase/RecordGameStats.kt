package com.example.domain.usecase

import com.example.data.local.entity.GameEntity
import com.example.data.repository.StatsRepository
import javax.inject.Inject

class RecordGameStats @Inject constructor(
    private val statsRepository: StatsRepository
) {
    suspend operator fun invoke(
        playerScore: Int,
        aiScore: Int,
        roundsPlayed: Int,
        clashesWon: Int,
        clashesLost: Int,
        intelAccuracy: Float,
        scoutUses: Int,
        lockUses: Int,
        isDailyChallenge: Boolean = false,
        dailyChallengeSeed: Long = 0L
    ) {
        val result = when {
            playerScore > aiScore -> "WIN"
            playerScore < aiScore -> "LOSE"
            else -> "DRAW"
        }

        val gameEntity = GameEntity(
            playerScore = playerScore,
            aiScore = aiScore,
            result = result,
            roundsPlayed = roundsPlayed,
            clashesWon = clashesWon,
            clashesLost = clashesLost,
            intelAccuracy = intelAccuracy,
            scoutUses = scoutUses,
            lockUses = lockUses,
            isDailyChallenge = isDailyChallenge,
            dailyChallengeSeed = dailyChallengeSeed
        )

        statsRepository.saveGame(gameEntity)
    }
}
