package com.example.domain

import com.example.domain.ai.AICapability
import com.example.domain.ai.AIPlanner
import com.example.domain.ai.AIStrategy
import com.example.domain.model.Board
import com.example.domain.model.Cell
import com.example.domain.model.CellOwner
import com.example.domain.model.Position
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class AIPlannerTest {

    @Test
    fun testPlannerDoesNotPlaceOnOwnCellsAndUsesFiveTokens() {
        val random = Random(42)
        val planner = AIPlanner()

        // Setup a board where AI owns several cells
        var board = Board()
        board = board.set(Position(0, 0), Cell(value = 1, owner = CellOwner.AI))
        board = board.set(Position(0, 1), Cell(value = 2, owner = CellOwner.AI))
        board = board.set(Position(0, 2), Cell(value = 3, owner = CellOwner.AI))

        val plan = planner.planPlacements(board, AICapability.PEAK, AIStrategy.SPREADING_THIN, random)

        // 1. Verify exactly 5 tokens are placed
        assertEquals(5, plan.placements.size)

        // 2. Verify all assigned token values are within 1..5 and are unique
        val tokenValues = plan.placements.values.map { it.value }.sorted()
        assertEquals(listOf(1, 2, 3, 4, 5), tokenValues)

        // 3. Verify no placements are on AI owned cells
        plan.placements.keys.forEach { pos ->
            val owner = board.get(pos).owner
            assertFalse("AI placed on its own cell at $pos which is owned by AI", owner == CellOwner.AI)
        }
    }

    @Test
    fun testPlannerHandlesSloppyShuffleAndBluffs() {
        val board = Board()
        val planner = AIPlanner()

        // Verify bluffing generates a bluff placement list [1, 4, 3, 2, 5] (which contains token 1 at index 0)
        // Since bluff is a 20% random chance, we can execute with multiple seeds to guarantee catching a bluff
        var bluffed = false
        var normal = false

        for (seed in 1..200) {
            val random = Random(seed.toLong())
            val plan = planner.planPlacements(board, AICapability.PEAK, AIStrategy.SECURING_HIGH_VALUE, random)
            
            // Collect placements ordered by highest score candidate first
            // Since we don't have the internal scoring map directly in test, we can check if the first cell (highest scored) has token value 1
            // SECURING_HIGH_VALUE with initial Board (which has values 1 except some generated) 
            // Let's force a high value 3-point cell to be the absolute top candidate by manual setup
            var customBoard = Board()
            customBoard = customBoard.set(Position(2, 2), Cell(value = 3, owner = CellOwner.NONE))
            
            val testPlan = planner.planPlacements(customBoard, AICapability.PEAK, AIStrategy.SECURING_HIGH_VALUE, random)
            val firstToken = testPlan.placements[Position(2, 2)]?.value
            
            if (firstToken == 1) {
                bluffed = true
            } else if (firstToken == 5) {
                normal = true
            }
        }

        assertTrue("Expected to encounter a bluffed placement", bluffed)
        assertTrue("Expected to encounter a normal placement", normal)
    }
}
