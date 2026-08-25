package com.example.domain

import com.example.domain.ai.AICapability
import com.example.domain.ai.AILockSelector
import com.example.domain.model.Board
import com.example.domain.model.Cell
import com.example.domain.model.CellOwner
import com.example.domain.model.Position
import com.example.domain.model.Token
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class AILockSelectorTest {

    @Test
    fun testSelectLocksWithZeroPlacements() {
        val lockSelector = AILockSelector()
        val locks = lockSelector.selectLocks(emptyMap(), Board(), AICapability.PEAK, Random())
        assertTrue(locks.isEmpty())
    }

    @Test
    fun testLockPrioritizationAndSloppyModifiers() {
        val random = Random(42)
        val lockSelector = AILockSelector()

        // Create placements on 3 different positions
        val placements = mapOf(
            Position(0, 0) to Token(value = 5),
            Position(2, 2) to Token(value = 4),
            Position(4, 4) to Token(value = 3)
        )

        // Setup board:
        // Position(0, 0) is owned by AI (value = 1), player-adjacent neighbors = 0
        //   Score: +30 (owner == AI) - 15 * 1 (value) - 20 * 0 (player neighbors) = +15
        // Position(2, 2) is neutral (value = 3), player-adjacent neighbors = 1
        //   Score: +0 - 15 * 3 - 20 * 1 = -65
        // Position(4, 4) is owned by Player (value = 2), player-adjacent neighbors = 0
        //   Score: +0 - 15 * 2 - 20 * 0 = -30
        var board = Board()
        board = board.set(Position(0, 0), Cell(value = 1, owner = CellOwner.AI))
        board = board.set(Position(2, 2), Cell(value = 3, owner = CellOwner.NONE))
        board = board.set(Position(1, 2), Cell(value = 1, owner = CellOwner.PLAYER)) // neighbor of (2, 2)
        board = board.set(Position(4, 4), Cell(value = 2, owner = CellOwner.PLAYER))

        // Safest = Position(0, 0) [+15], Next Safest = Position(4, 4) [-30], Worst = Position(2, 2) [-65]
        val peakLocks = lockSelector.selectLocks(placements, board, AICapability.PEAK, random)

        // PEAK must return top 2 safely ordered: Position(0, 0) first, Position(4, 4) second
        assertEquals(2, peakLocks.size)
        assertEquals(Position(0, 0), peakLocks[0])
        assertEquals(Position(4, 4), peakLocks[1])

        // Verify SLOPPY can swap choices (30% chance) deterministically
        val mockRandomSwap = object : Random() {
            override fun nextDouble() = 0.1 // < 0.30 triggers swap
        }
        val sloppyLocksSwap = lockSelector.selectLocks(placements, board, AICapability.SLOPPY, mockRandomSwap)
        assertEquals(2, sloppyLocksSwap.size)
        // Position(2, 2) is index 2, swapped to index 0
        assertEquals(Position(2, 2), sloppyLocksSwap[0])

        val mockRandomNoSwap = object : Random() {
            override fun nextDouble() = 0.9 // >= 0.30 avoids swap
        }
        val sloppyLocksNoSwap = lockSelector.selectLocks(placements, board, AICapability.SLOPPY, mockRandomNoSwap)
        assertEquals(2, sloppyLocksNoSwap.size)
        assertEquals(Position(0, 0), sloppyLocksNoSwap[0])
    }
}
