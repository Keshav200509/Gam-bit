package com.example.domain.ai

import com.example.domain.model.Board
import com.example.domain.model.CellOwner
import com.example.domain.model.Position
import com.example.domain.model.Token
import com.example.domain.model.ArenaConfiguration
import java.util.Random
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Arena3AIPlanner handles the decision-making process for the AI in Arena 3 (Oblivion).
 * Because Arena 3 features simultaneous commit without any information hints or scouting,
 * the AI focuses entirely on board-state value maximization, adjacency building, and bluffing.
 */
@Singleton
class Arena3AIPlanner @Inject constructor(
    private val random: Random
) {
    /**
     * Arena 3 AI: Pure board-state optimization + bluffing.
     * No player behavior modeling (AI is blind to player's move).
     * 
     * Strategy:
     * 1. Score all non-AI-owned cells based on board state only:
     *    - Base: cell value × 20
     *    - +10 per AI-adjacent neighbor
     *    - +15 if cell is player-owned (steal value)
     *    - +8 if cell is high-value (3)
     * 2. Apply bluff rate from config:
     *    - With probability = bluffRate, reverse the token assignment
     *    - (Top cell gets token 1 instead of 5 — baiting the player)
     * 3. Apply capability variance
     * 4. Assign tokens 5,4,3,2,1 to top 5 cells
     * 5. Return placements + strategy info
     */
    fun planPlacements(
        board: Board,
        capability: AICapability,
        config: ArenaConfiguration,
        random: Random
    ): AIPlan {
        // Collect all non-AI owned cells as candidates
        val candidates = mutableListOf<Position>()
        for (r in 0 until 5) {
            for (c in 0 until 5) {
                val pos = Position(r, c)
                if (board.get(pos).owner != CellOwner.AI) {
                    candidates.add(pos)
                }
            }
        }

        // 1. Score all non-AI-owned cells based on board state only
        val scoredCandidates = candidates.map { pos ->
            val cell = board.get(pos)
            var score = cell.value * 20

            // +10 per AI-adjacent neighbor
            val neighbors = board.neighborsOf(pos)
            val aiNeighbors = neighbors.count { board.get(it).owner == CellOwner.AI }
            score += aiNeighbors * 10

            // +15 if cell is player-owned (steal value)
            if (cell.owner == CellOwner.PLAYER) {
                score += 15
            }

            // +8 if cell is high-value (3)
            if (cell.value == 3) {
                score += 8
            }

            pos to score
        }.toMutableList()

        // Sort by score descending
        scoredCandidates.sortByDescending { it.second }
        val sortedPositions = scoredCandidates.map { it.first }.toMutableList()

        // 3. Apply capability variance: If SLOPPY, shuffle top candidates to introduce slight suboptimality
        if (capability == AICapability.SLOPPY && sortedPositions.size >= 2) {
            val shuffleCount = minOf(3, sortedPositions.size)
            val sublist = sortedPositions.subList(0, shuffleCount).toMutableList()
            sublist.shuffle(random)
            for (i in 0 until shuffleCount) {
                sortedPositions[i] = sublist[i]
            }
        }

        // 4. Assign tokens 5,4,3,2,1 to top 5 cells (with bluff rate check)
        val placements = mutableMapOf<Position, Token>()
        val tokenCount = minOf(sortedPositions.size, 5)

        // Apply bluff rate: reverse token order
        val isBluffing = random.nextDouble() < config.aiBluffRate

        for (i in 0 until tokenCount) {
            val tokenVal = if (isBluffing && tokenCount >= 5) {
                when (i) {
                    0 -> 1   // top candidate gets 1 instead of 5
                    1 -> 4
                    2 -> 3
                    3 -> 2
                    4 -> 5   // last candidate gets 5 instead of 1
                    else -> 1
                }
            } else {
                when (i) {
                    0 -> 5
                    1 -> 4
                    2 -> 3
                    3 -> 2
                    4 -> 1
                    else -> 1
                }
            }
            placements[sortedPositions[i]] = Token(value = tokenVal)
        }

        // Select locks for these placements
        val lockSelector = AILockSelector()
        val locks = lockSelector.selectLocks(placements, board, capability, random)
            .take(config.maxLocksPerRound)

        return AIPlan(
            placements = placements,
            locks = locks,
            strategy = if (isBluffing) AIStrategy.AGGRESSIVE_EXPANSION else AIStrategy.STEALING_CELLS
        )
    }
}
