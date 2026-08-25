package com.example.domain.ai

import com.example.domain.model.Board
import com.example.domain.model.CellOwner
import com.example.domain.model.Position
import com.example.domain.model.Token
import java.util.Random

/**
 * AIPlanner implements the core tactical evaluation and token placement strategy for the AI opponent.
 * It strictly maintains domain-layer purity with zero Android or Jetpack Compose imports.
 */
class AIPlanner {

    private val lockSelector = AILockSelector()

    fun planPlacements(
        board: Board,
        capability: AICapability,
        strategy: AIStrategy,
        random: Random,
        isNewGamePlus: Boolean = false,
        aiMemory: AIMemory? = null
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

        // Score candidates based on tactical criteria
        val scoredCandidates = candidates.map { pos ->
            val cell = board.get(pos)
            var score = cell.value * 15

            // Neighbors analysis
            val neighbors = board.neighborsOf(pos)
            val aiNeighbors = neighbors.count { board.get(it).owner == CellOwner.AI }
            val playerNeighbors = neighbors.count { board.get(it).owner == CellOwner.PLAYER }

            score += aiNeighbors * 8
            score += playerNeighbors * 5

            if (cell.owner == CellOwner.PLAYER) {
                score += 12 // stealing bonus
            }

            // AI Memory / New Game Plus adaptations
            if (isNewGamePlus && aiMemory != null) {
                val preferredRegion = aiMemory.getPreferredRegion()
                if (preferredRegion != null && getCellRegion(pos.row, pos.col) == preferredRegion) {
                    score += 15 // Counter player's preferred region!
                }
                
                // If player is lock heavy, place even higher emphasis on securing their cells or playing carefully
                val avgLocks = aiMemory.totalLockUses.toFloat() / maxOf(1, aiMemory.totalGamesTracked)
                if (avgLocks >= 1.0f && cell.owner == CellOwner.PLAYER) {
                    score += 8
                }
            }

            // Strategy modifiers
            score += when (strategy) {
                AIStrategy.AGGRESSIVE_EXPANSION -> {
                    if (cell.owner == CellOwner.NONE) 10 else 0
                }
                AIStrategy.TERRITORY_FORTIFICATION -> {
                    if (aiNeighbors >= 1) 10 else 0
                }
                AIStrategy.STEALING_CELLS -> {
                    if (cell.owner == CellOwner.PLAYER) 12 else 0
                }
                AIStrategy.SECURING_HIGH_VALUE -> {
                    if (cell.value >= 2) 15 else 0
                }
                AIStrategy.SPREADING_THIN -> {
                    if (aiNeighbors <= 1) 10 else 0
                }
                AIStrategy.BUILDING_CLUSTER -> {
                    if (aiNeighbors >= 1) 10 else 0
                }
            }

            pos to score
        }.toMutableList()

        // Sort candidates by score descending
        scoredCandidates.sortByDescending { it.second }

        val sortedPositions = scoredCandidates.map { it.first }.toMutableList()

        // If capability is SLOPPY, shuffle top 2-3 candidates (swap positions)
        if (capability == AICapability.SLOPPY && sortedPositions.size >= 2) {
            val shuffleCount = minOf(3, sortedPositions.size)
            val sublist = sortedPositions.subList(0, shuffleCount).toMutableList()
            sublist.shuffle(random)
            for (i in 0 until shuffleCount) {
                sortedPositions[i] = sublist[i]
            }
        }

        // Assign tokens: top 5 candidates get tokens 5, 4, 3, 2, 1
        val placements = mutableMapOf<Position, Token>()
        val tokenCount = minOf(sortedPositions.size, 5)
        
        // 20% bluff chance: top candidate gets token 1 instead of 5, and candidate #5 gets token 5
        val isBluffing = random.nextDouble() < 0.20

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
        val locks = lockSelector.selectLocks(placements, board, capability, random)

        return AIPlan(placements, locks, strategy)
    }

    private fun getCellRegion(row: Int, col: Int): String {
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
