package com.example.domain

import com.example.domain.model.LockResult
import com.example.domain.model.Position
import com.example.domain.model.Token
import com.example.domain.usecase.ToggleLock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LockUseCaseTest {

    private val toggleLock = ToggleLock()

    @Test
    fun testCannotLockWhenNoPlacement() {
        val playerPlacements = mapOf(
            Position(0, 0) to Token(3)
        )
        val currentLocks = emptyList<Position>()

        // Position(1, 1) has no token placed by the player
        val result = toggleLock.execute(Position(1, 1), currentLocks, playerPlacements)
        assertEquals(LockResult.NotOwned, result)
    }

    @Test
    fun testLockToggleAddingSuccessfully() {
        val pos = Position(0, 0)
        val playerPlacements = mapOf(
            pos to Token(3)
        )
        val currentLocks = emptyList<Position>()

        val result = toggleLock.execute(pos, currentLocks, playerPlacements)
        assertTrue(result is LockResult.Added)
        assertEquals(listOf(pos), (result as LockResult.Added).updatedLocks)
    }

    @Test
    fun testLockToggleRemovingSuccessfully() {
        val pos = Position(0, 0)
        val playerPlacements = mapOf(
            pos to Token(3)
        )
        val currentLocks = listOf(pos)

        val result = toggleLock.execute(pos, currentLocks, playerPlacements)
        assertTrue(result is LockResult.Removed)
        assertTrue((result as LockResult.Removed).updatedLocks.isEmpty())
    }

    @Test
    fun testCannotExceedTwoLocksLimit() {
        val p1 = Position(0, 0)
        val p2 = Position(0, 1)
        val p3 = Position(0, 2)

        val playerPlacements = mapOf(
            p1 to Token(3),
            p2 to Token(4),
            p3 to Token(5)
        )
        val currentLocks = listOf(p1, p2)

        // Attempting to lock a third position p3
        val result = toggleLock.execute(p3, currentLocks, playerPlacements)
        assertEquals(LockResult.LimitReached, result)
    }
}
