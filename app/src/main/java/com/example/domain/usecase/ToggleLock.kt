package com.example.domain.usecase

import com.example.domain.model.LockResult
import com.example.domain.model.Position
import com.example.domain.model.Token
import javax.inject.Inject

class ToggleLock @Inject constructor() {
    fun execute(
        position: Position,
        currentLocks: List<Position>,
        playerPlacements: Map<Position, Token>
    ): LockResult {
        // Can only lock cells where player has placed a token
        if (!playerPlacements.containsKey(position)) {
            return LockResult.NotOwned
        }

        return if (currentLocks.contains(position)) {
            // Remove the lock
            val updated = currentLocks.filter { it != position }
            LockResult.Removed(updated)
        } else {
            // Check limit of 2 locks
            if (currentLocks.size >= 2) {
                LockResult.LimitReached
            } else {
                // Add the lock
                val updated = currentLocks + position
                LockResult.Added(updated)
            }
        }
    }
}
