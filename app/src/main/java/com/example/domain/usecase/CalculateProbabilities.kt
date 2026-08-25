package com.example.domain.usecase

import com.example.domain.ai.AICapability
import com.example.domain.model.Board
import com.example.domain.model.CellOwner
import com.example.domain.model.IntelActualData
import com.example.domain.model.IntelHint
import com.example.domain.model.IntelType
import com.example.domain.model.Position
import com.example.domain.model.ProbabilityBreakdown
import com.example.domain.model.ScoutResult
import java.util.Random
import javax.inject.Inject

class CalculateProbabilities @Inject constructor() {

    fun execute(
        position: Position,
        playerToken: Int,
        board: Board,
        intel: IntelHint?,
        scoutResult: ScoutResult?,
        aiCapability: AICapability,
        trickiness: Float,
        random: Random
    ): ProbabilityBreakdown {
        val cell = board.get(position)
        val cellValue = cell.value

        // Neighbors calculations
        val neighbors = position.neighbors()
        val aiNeighborsCount = neighbors.count { board.get(it).owner == CellOwner.AI }
        val playerNeighborsCount = neighbors.count { board.get(it).owner == CellOwner.PLAYER }

        // 1. Calculate AI contest probability
        // base 0.15 + cell value * 0.06 + AI adjacency * 0.05 + player adjacency * 0.04 + 0.1 if player-owned
        var contestProb = 0.15 + (cellValue * 0.06) + (aiNeighborsCount * 0.05) + (playerNeighborsCount * 0.04)
        if (cell.owner == CellOwner.PLAYER) {
            contestProb += 0.1
        }

        // 2. If intel mentions this region, add 0.08
        val cellRegion = getRegion(position.row, position.col)
        if (intel != null) {
            val mentionsRegion = when (val data = intel.actualData) {
                is IntelActualData.TokenRegion -> data.region == cellRegion
                is IntelActualData.CellValue -> data.region == cellRegion
                is IntelActualData.Strategy -> false
            }
            if (mentionsRegion) {
                contestProb += 0.08
            }
        }

        // 3. If scout result exists for this cell, override
        if (scoutResult != null) {
            contestProb = when (scoutResult) {
                ScoutResult.CLEAR -> 0.0
                ScoutResult.CONTESTED -> 1.0
            }
        }

        // Clamp contest probability
        contestProb = contestProb.coerceIn(0.0, 1.0)

        // 4. Estimate AI token distribution (weighted by cell value, with bluff factor)
        val weights = DoubleArray(5) { 0.2 } // Initial uniform weights for tokens 1..5

        // Boost higher/lower tokens based on cell value
        if (cellValue == 3) {
            weights[4] += 0.2 // token 5
            weights[3] += 0.1 // token 4
        } else if (cellValue == 1) {
            weights[0] += 0.2 // token 1
            weights[1] += 0.1 // token 2
        }

        // Incorporate intel if it's about a token heading to this region
        if (intel != null && intel.type == IntelType.TOKEN_REGION) {
            val data = intel.actualData as? IntelActualData.TokenRegion
            if (data != null && data.region == cellRegion) {
                val tokenIndex = data.tokenValue - 1
                if (tokenIndex in 0..4) {
                    if (intel.isTrue) {
                        weights[tokenIndex] += 0.4
                    } else {
                        weights[tokenIndex] = (weights[tokenIndex] - 0.15).coerceAtLeast(0.05)
                    }
                }
            }
        }

        // Normalize weights
        var sumWeights = weights.sum()
        if (sumWeights <= 0.0) {
            weights.fill(0.2)
            sumWeights = 1.0
        }
        for (i in weights.indices) {
            weights[i] /= sumWeights
        }

        // 5. Apply capability randomness: blend with 0.3 uniform based on |modifier|/20
        val modAbs = kotlin.math.abs(aiCapability.modifier)
        val blendFactor = 0.3 * (modAbs / 20.0)
        for (i in weights.indices) {
            weights[i] = (1.0 - blendFactor) * weights[i] + blendFactor * 0.2
        }

        // Re-normalize weights after blending
        sumWeights = weights.sum()
        for (i in weights.indices) {
            weights[i] /= sumWeights
        }

        // 6. Calculate clash outcomes
        // Player strength: playerToken + playerNeighborsCount + cell.fortify
        val playerFortifyBonus = if (cell.owner == CellOwner.PLAYER) cell.fortify else 0
        val playerStrength = playerToken + playerNeighborsCount + playerFortifyBonus

        var winProb = 0.0
        var loseProb = 0.0
        var tieProb = 0.0

        val aiFortifyBonus = if (cell.owner == CellOwner.AI) cell.fortify else 0

        for (aiVal in 1..5) {
            val aiStrength = aiVal + aiNeighborsCount + aiFortifyBonus
            val weight = weights[aiVal - 1]
            when {
                playerStrength > aiStrength -> winProb += weight
                playerStrength < aiStrength -> loseProb += weight
                else -> tieProb += weight
            }
        }

        // Scale by contest probability
        val scaledWin = contestProb * winProb
        val scaledLose = contestProb * loseProb
        val scaledTie = contestProb * tieProb
        val uncontested = 1.0 - contestProb

        // Convert to percentage and ensure they sum to exactly 100
        val uncontestedPct = (uncontested * 100.0).coerceIn(0.0, 100.0)
        val winPct = (scaledWin * 100.0).coerceIn(0.0, 100.0)
        val losePct = (scaledLose * 100.0).coerceIn(0.0, 100.0)
        val tiePct = (scaledTie * 100.0).coerceIn(0.0, 100.0)

        val totalSum = uncontestedPct + winPct + losePct + tiePct
        val u = kotlin.math.round(uncontestedPct / totalSum * 100.0).toInt()
        val w = kotlin.math.round(winPct / totalSum * 100.0).toInt()
        val l = kotlin.math.round(losePct / totalSum * 100.0).toInt()
        val t = 100 - u - w - l

        return ProbabilityBreakdown(win = w, lose = l, tie = t, uncontested = u)
    }

    private fun getRegion(row: Int, col: Int): String {
        return when {
            row == 2 && col == 2 -> "center"
            row < 2 && col < 2 -> "northwest"
            row < 2 && col > 2 -> "northeast"
            row > 2 && col < 2 -> "southwest"
            row > 2 && col > 2 -> "southeast"
            row < 2 -> "north"
            row > 2 -> "south"
            col < 2 -> "west"
            else -> "east"
        }
    }
}
