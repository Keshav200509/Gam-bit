package com.example.domain.ai

import com.example.domain.model.Board
import com.example.domain.model.CellOwner
import com.example.domain.model.Position
import com.example.domain.model.Token
import com.example.domain.model.ArenaConfiguration
import java.util.Random
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Arena1AIPlanner @Inject constructor(
    private val random: Random
) {
    /**
     * Plans AI placements AFTER seeing player's placements.
     * Strategy:
     * 1. Identify player's undefended cells (player-owned cells with no player token placed on them)
     * 2. Identify player's clusters (for adjacency disruption)
     * 3. Identify high-value neutral cells the player ignored
     * 4. Score all non-AI-owned cells:
     *    - Base: cell value × 15
     *    - +20 if cell is player-owned AND undefended (steal opportunity)
     *    - +12 if cell is adjacent to 2+ player cells (break cluster)
     *    - +8 if cell is high-value (3) and neutral
     *    - +5 per AI-adjacent neighbor (build own adjacency)
     *    - -10 if cell is player-defended (player has token there — will clash)
     * 5. Sort by score, apply capability variance
     * 6. Assign tokens 5,4 to top 2 targets, 3,2 to next 2, 1 to last
     * 7. 20% bluff chance: top target gets token 1 instead of 5
     */
    fun planPlacements(
        board: Board,
        playerPlacements: Map<Position, Token>,
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

        // 1. Identify player's undefended cells (player-owned cells with no player token placed on them)
        val playerUndefended = mutableSetOf<Position>()
        for (r in 0 until 5) {
            for (c in 0 until 5) {
                val pos = Position(r, c)
                val cell = board.get(pos)
                if (cell.owner == CellOwner.PLAYER && !playerPlacements.containsKey(pos)) {
                    playerUndefended.add(pos)
                }
            }
        }

        // 2. Identify player's clusters (for adjacency disruption): adjacent to 2+ player cells
        val playerClusters = mutableSetOf<Position>()
        for (r in 0 until 5) {
            for (c in 0 until 5) {
                val pos = Position(r, c)
                val neighbors = board.neighborsOf(pos)
                val playerCellNeighbors = neighbors.count { board.get(it).owner == CellOwner.PLAYER }
                if (playerCellNeighbors >= 2) {
                    playerClusters.add(pos)
                }
            }
        }

        // 4. Score all non-AI-owned cells
        val scoredCandidates = candidates.map { pos ->
            val cell = board.get(pos)
            var score = cell.value * 15

            // +20 if cell is player-owned AND undefended (steal opportunity)
            if (cell.owner == CellOwner.PLAYER && playerUndefended.contains(pos)) {
                score += 20
            }

            // +12 if cell is adjacent to 2+ player cells (break cluster)
            if (playerClusters.contains(pos)) {
                score += 12
            }

            // +8 if cell is high-value (3) and neutral
            if (cell.value == 3 && cell.owner == CellOwner.NONE) {
                score += 8
            }

            // +5 per AI-adjacent neighbor (build own adjacency)
            val neighbors = board.neighborsOf(pos)
            val aiNeighbors = neighbors.count { board.get(it).owner == CellOwner.AI }
            score += aiNeighbors * 5

            // -10 if cell is player-defended (player has token there — will clash)
            if (playerPlacements.containsKey(pos)) {
                score -= 10
            }

            pos to score
        }.toMutableList()

        // 5. Sort by score descending
        scoredCandidates.sortByDescending { it.second }
        val sortedPositions = scoredCandidates.map { it.first }.toMutableList()

        // Apply capability variance: If SLOPPY, shuffle top 2-3 candidates
        if (capability == AICapability.SLOPPY && sortedPositions.size >= 2) {
            val shuffleCount = minOf(3, sortedPositions.size)
            val sublist = sortedPositions.subList(0, shuffleCount).toMutableList()
            sublist.shuffle(random)
            for (i in 0 until shuffleCount) {
                sortedPositions[i] = sublist[i]
            }
        }

        // 6. Assign tokens 5,4 to top 2 targets, 3,2 to next 2, 1 to last
        val placements = mutableMapOf<Position, Token>()
        val tokenCount = minOf(sortedPositions.size, 5)

        // 7. 20% bluff chance (scaled to config.aiBluffRate): top target gets token 1 instead of 5
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

        return AIPlan(placements, locks, AIStrategy.STEALING_CELLS)
    }
}
