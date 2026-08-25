package com.example.domain.usecase

import com.example.domain.model.Board
import com.example.domain.model.CellModifier
import com.example.domain.model.CellOwner
import com.example.domain.model.Position
import javax.inject.Inject

class CalculateIncome @Inject constructor() {
    operator fun invoke(board: Board, roundNumber: Int = 1, roundMultiplier: Int = 1): Pair<Int, Int> {
        var playerIncome = 0
        var aiIncome = 0

        for (row in 0 until 5) {
            for (col in 0 until 5) {
                val cell = board.get(Position(row, col))
                if (cell.owner == CellOwner.NONE) continue

                var cellIncome = cell.value

                // Golden Cell generates 2x its value
                if (cell.modifier is CellModifier.GoldenCell) {
                    cellIncome *= 2
                }

                // Volatile Cell generates 2x its value ONLY at game end (round 12)
                if (cell.modifier is CellModifier.Volatile && roundNumber == 12) {
                    cellIncome *= 2
                }

                when (cell.owner) {
                    CellOwner.PLAYER -> playerIncome += cellIncome
                    CellOwner.AI -> aiIncome += cellIncome
                    else -> {}
                }
            }
        }
        return Pair(playerIncome * roundMultiplier, aiIncome * roundMultiplier)
    }
}
