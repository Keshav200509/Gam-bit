package com.example.domain

import com.example.domain.ai.AIEngine
import com.example.domain.ai.Arena3AIPlanner
import com.example.domain.model.Board
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class AIEngineTest {

    @Test
    fun testMakeMoveOrchestration() {
        val random = Random(123)
        val engine = AIEngine(Arena3AIPlanner(random))
        val board = Board()

        val aiMove = engine.makeMove(board, random)

        // Verify all parts of the orchestration are non-null and correctly shaped
        assertNotNull(aiMove)
        assertNotNull(aiMove.capability)
        assertNotNull(aiMove.plan)
        assertNotNull(aiMove.plan.strategy)
        
        // Placements must be exactly 5
        assertEquals(5, aiMove.plan.placements.size)
        
        // Locks must be at most 2
        assertTrue(aiMove.plan.locks.size <= 2)
    }
}
