package com.example.domain.ai

import com.example.domain.model.Board
import com.example.domain.model.CellOwner
import java.util.Random

/**
 * AIStrategy defines the strategic posture of the AI opponent based on the current board ownership.
 * It strictly maintains domain-layer purity with zero Android or Jetpack Compose imports.
 */
enum class AIStrategy {
    AGGRESSIVE_EXPANSION,
    TERRITORY_FORTIFICATION,
    STEALING_CELLS,
    SECURING_HIGH_VALUE,
    SPREADING_THIN,
    BUILDING_CLUSTER;

    companion object {
        fun chooseStrategy(board: Board, random: Random): AIStrategy {
            var aiCount = 0
            var playerCount = 0
            var neutralCount = 0

            for (r in 0 until 5) {
                for (c in 0 until 5) {
                    val cell = board.cells[r][c]
                    when (cell.owner) {
                        CellOwner.AI -> aiCount++
                        CellOwner.PLAYER -> playerCount++
                        CellOwner.NONE -> neutralCount++
                    }
                }
            }

            return when {
                aiCount <= playerCount - 2 -> AGGRESSIVE_EXPANSION
                aiCount >= playerCount + 2 -> TERRITORY_FORTIFICATION
                playerCount >= 3 -> {
                    if (random.nextDouble() < 0.40) {
                        STEALING_CELLS
                    } else {
                        SECURING_HIGH_VALUE
                    }
                }
                neutralCount > 10 -> SPREADING_THIN
                else -> BUILDING_CLUSTER
            }
        }
    }
}
