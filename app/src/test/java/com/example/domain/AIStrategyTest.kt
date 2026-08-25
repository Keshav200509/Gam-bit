package com.example.domain

import com.example.domain.ai.AIStrategy
import com.example.domain.model.Board
import com.example.domain.model.Cell
import com.example.domain.model.CellOwner
import com.example.domain.model.Position
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class AIStrategyTest {

    @Test
    fun testStrategySelectionBasedOnBoardState() {
        val random = Random(42)
        var board = Board()

        // 1. Initially, neutral count = 25 (> 10), players own 0 cells. Strategy should be SPREADING_THIN
        val initStrategy = AIStrategy.chooseStrategy(board, random)
        assertEquals(AIStrategy.SPREADING_THIN, initStrategy)

        // 2. AI has 2+ fewer cells than player -> AGGRESSIVE_EXPANSION
        // Let's set player owns 3 cells, AI owns 1 cell
        board = board.set(Position(0, 0), Cell(value = 1, owner = CellOwner.PLAYER))
        board = board.set(Position(0, 1), Cell(value = 1, owner = CellOwner.PLAYER))
        board = board.set(Position(0, 2), Cell(value = 1, owner = CellOwner.PLAYER))
        board = board.set(Position(4, 4), Cell(value = 1, owner = CellOwner.AI))

        val aggressiveStrategy = AIStrategy.chooseStrategy(board, random)
        assertEquals(AIStrategy.AGGRESSIVE_EXPANSION, aggressiveStrategy)

        // 3. AI has 2+ more cells than player -> TERRITORY_FORTIFICATION
        // Let's set AI owns 3 cells, player owns 1 cell
        var board2 = Board()
        board2 = board2.set(Position(0, 0), Cell(value = 1, owner = CellOwner.PLAYER))
        board2 = board2.set(Position(4, 4), Cell(value = 1, owner = CellOwner.AI))
        board2 = board2.set(Position(4, 3), Cell(value = 1, owner = CellOwner.AI))
        board2 = board2.set(Position(4, 2), Cell(value = 1, owner = CellOwner.AI))

        val fortificationStrategy = AIStrategy.chooseStrategy(board2, random)
        assertEquals(AIStrategy.TERRITORY_FORTIFICATION, fortificationStrategy)

        // 4. Equal score, Player has 3+ cells. Check 40% STEALING_CELLS vs SECURING_HIGH_VALUE
        var board3 = Board()
        // Player owns 3, AI owns 3 (so difference is 0)
        board3 = board3.set(Position(0, 0), Cell(value = 1, owner = CellOwner.PLAYER))
        board3 = board3.set(Position(0, 1), Cell(value = 1, owner = CellOwner.PLAYER))
        board3 = board3.set(Position(0, 2), Cell(value = 1, owner = CellOwner.PLAYER))
        board3 = board3.set(Position(4, 4), Cell(value = 1, owner = CellOwner.AI))
        board3 = board3.set(Position(4, 3), Cell(value = 1, owner = CellOwner.AI))
        board3 = board3.set(Position(4, 2), Cell(value = 1, owner = CellOwner.AI))

        val mockRandomSteal = object : Random() {
            override fun nextDouble() = 0.1
        }
        val strategySteal = AIStrategy.chooseStrategy(board3, mockRandomSteal)
        assertEquals(AIStrategy.STEALING_CELLS, strategySteal)

        val mockRandomSecure = object : Random() {
            override fun nextDouble() = 0.9
        }
        val strategySecure = AIStrategy.chooseStrategy(board3, mockRandomSecure)
        assertEquals(AIStrategy.SECURING_HIGH_VALUE, strategySecure)
    }
}
