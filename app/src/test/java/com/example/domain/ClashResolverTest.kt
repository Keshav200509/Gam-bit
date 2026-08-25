package com.example.domain

import com.example.domain.model.Board
import com.example.domain.model.Cell
import com.example.domain.model.CellOwner
import com.example.domain.model.ClashResult
import com.example.domain.model.Position
import com.example.domain.model.Token
import com.example.domain.usecase.ResolveClash
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClashResolverTest {

    private val resolveClash = ResolveClash()

    @Test
    fun testNoOnePlacedOnCell() {
        val board = Board()
        val pos = Position(0, 0)
        
        // Neutral cell
        val resNeutral = resolveClash(pos, emptyMap(), emptyMap(), board, emptyList(), emptyList())
        assertTrue(resNeutral is ClashResult.Tie)

        // Player owned cell
        val boardPlayer = board.set(pos, Cell(value = 1, owner = CellOwner.PLAYER))
        val resPlayer = resolveClash(pos, emptyMap(), emptyMap(), boardPlayer, emptyList(), emptyList())
        assertTrue(resPlayer is ClashResult.PlayerDefends)

        // AI owned cell
        val boardAi = board.set(pos, Cell(value = 1, owner = CellOwner.AI))
        val resAi = resolveClash(pos, emptyMap(), emptyMap(), boardAi, emptyList(), emptyList())
        assertTrue(resAi is ClashResult.AIDefends)
    }

    @Test
    fun testOnlyPlayerPlaced() {
        val board = Board()
        val pos = Position(0, 0)
        val placements = mapOf(pos to Token(value = 3))

        // Neutral cell claim
        val resNeutral = resolveClash(pos, placements, emptyMap(), board, emptyList(), emptyList())
        assertTrue(resNeutral is ClashResult.PlayerUncontested)

        // Own cell defense
        val boardPlayer = board.set(pos, Cell(value = 1, owner = CellOwner.PLAYER))
        val resDef = resolveClash(pos, placements, emptyMap(), boardPlayer, emptyList(), emptyList())
        assertTrue(resDef is ClashResult.PlayerDefends)
    }

    @Test
    fun testOnlyAIPlaced() {
        val board = Board()
        val pos = Position(0, 0)
        val placements = mapOf(pos to Token(value = 3))

        // Neutral cell claim
        val resNeutral = resolveClash(pos, emptyMap(), placements, board, emptyList(), emptyList())
        assertTrue(resNeutral is ClashResult.AIUncontested)

        // Own cell defense
        val boardAi = board.set(pos, Cell(value = 1, owner = CellOwner.AI))
        val resDef = resolveClash(pos, emptyMap(), placements, boardAi, emptyList(), emptyList())
        assertTrue(resDef is ClashResult.AIDefends)
    }

    @Test
    fun testClashPlayerWins() {
        val board = Board()
        val pos = Position(0, 0)
        val pPlacements = mapOf(pos to Token(value = 5))
        val aPlacements = mapOf(pos to Token(value = 3))

        val result = resolveClash(pos, pPlacements, aPlacements, board, emptyList(), emptyList())
        assertTrue(result is ClashResult.PlayerWins)
        val wins = result as ClashResult.PlayerWins
        assertEquals(5, wins.playerStrength)
        assertEquals(3, wins.aiStrength)
    }

    @Test
    fun testClashAIWins() {
        val board = Board()
        val pos = Position(0, 0)
        val pPlacements = mapOf(pos to Token(value = 2))
        val aPlacements = mapOf(pos to Token(value = 4))

        val result = resolveClash(pos, pPlacements, aPlacements, board, emptyList(), emptyList())
        assertTrue(result is ClashResult.AIWins)
        val wins = result as ClashResult.AIWins
        assertEquals(2, wins.playerStrength)
        assertEquals(4, wins.aiStrength)
    }

    @Test
    fun testClashTieResetsOwnership() {
        val pos = Position(0, 0)
        val pPlacements = mapOf(pos to Token(value = 3))
        val aPlacements = mapOf(pos to Token(value = 3))

        // Set cell owned by player initially, then tie should reset it to unowned
        val board = Board().set(pos, Cell(value = 1, owner = CellOwner.PLAYER))
        val result = resolveClash(pos, pPlacements, aPlacements, board, emptyList(), emptyList())
        assertTrue(result is ClashResult.Tie)
    }

    @Test
    fun testClashLockAndAdjacencyBonuses() {
        val pos = Position(2, 2)
        // Position(2, 2) neighbors are: (1,2), (3,2), (2,1), (2,3)
        // Let's make player own two neighbor cells to get a +2 adjacency bonus
        var board = Board()
        board = board.set(Position(1, 2), Cell(value = 1, owner = CellOwner.PLAYER))
        board = board.set(Position(2, 1), Cell(value = 1, owner = CellOwner.PLAYER))

        val pPlacements = mapOf(pos to Token(value = 3)) // strength = 3 (base) + 2 (adjacency) + 2 (lock) = 7
        val aPlacements = mapOf(pos to Token(value = 5)) // strength = 5 (base) = 5

        // Player locks Position(2, 2)
        val result = resolveClash(pos, pPlacements, aPlacements, board, listOf(pos), emptyList())
        
        // Player strength (7) > AI strength (5), so Player wins!
        assertTrue(result is ClashResult.PlayerWins)
        val wins = result as ClashResult.PlayerWins
        assertEquals(7, wins.playerStrength)
        assertEquals(5, wins.aiStrength)
    }
}
