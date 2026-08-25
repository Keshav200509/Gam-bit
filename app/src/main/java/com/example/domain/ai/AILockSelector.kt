package com.example.domain.ai

import com.example.domain.model.Board
import com.example.domain.model.CellOwner
import com.example.domain.model.Position
import com.example.domain.model.Token
import java.util.Random

/**
 * AILockSelector selects up to 2 cells to lock for the AI opponent.
 * It strictly maintains domain-layer purity with zero Android or Jetpack Compose imports.
 */
class AILockSelector {

    fun selectLocks(
        placements: Map<Position, Token>,
        board: Board,
        capability: AICapability,
        random: Random
    ): List<Position> {
        if (placements.isEmpty()) return emptyList()

        // Score each placement for lock safety
        val scoredPlacements = placements.keys.map { pos ->
            var score = 0

            val cell = board.get(pos)

            // +30 if defending own cell
            if (cell.owner == CellOwner.AI) {
                score += 30
            }

            // -15 per cell value
            score -= 15 * cell.value

            // -20 per player-adjacent neighbor
            val neighbors = board.neighborsOf(pos)
            val playerNeighbors = neighbors.count { board.get(it).owner == CellOwner.PLAYER }
            score -= 20 * playerNeighbors

            pos to score
        }.toMutableList()

        // Sort by lock safety score descending (highest = safest/best to lock)
        scoredPlacements.sortByDescending { it.second }

        // If SLOPPY: 30% chance to swap #1 and #3 (index 0 and 2) (suboptimal lock choice)
        if (capability == AICapability.SLOPPY && scoredPlacements.size >= 3) {
            if (random.nextDouble() < 0.30) {
                val temp = scoredPlacements[0]
                scoredPlacements[0] = scoredPlacements[2]
                scoredPlacements[2] = temp
            }
        }

        // Return top 2 positions (or fewer if fewer placements)
        val limit = minOf(2, scoredPlacements.size)
        return scoredPlacements.take(limit).map { it.first }
    }
}
