package com.example.domain.usecase

import com.example.domain.model.Board
import com.example.domain.model.CellModifier
import com.example.domain.model.CellOwner
import com.example.domain.model.ClashResult
import com.example.domain.model.Position
import com.example.domain.model.Token
import javax.inject.Inject

class ResolveClash @Inject constructor() {
    operator fun invoke(
        position: Position,
        playerPlacements: Map<Position, Token>,
        aiPlacements: Map<Position, Token>,
        board: Board,
        playerLocks: List<Position>,
        aiLocks: List<Position>
    ): ClashResult {
        val cell = board.get(position)
        val playerToken = playerPlacements[position]
        val aiToken = aiPlacements[position]

        if (playerToken == null && aiToken == null) {
            // No one placed on this cell
            return if (cell.owner == CellOwner.PLAYER) ClashResult.PlayerDefends
            else if (cell.owner == CellOwner.AI) ClashResult.AIDefends
            else ClashResult.Tie // Or neutral, let's treat as Tie/none
        }

        if (playerToken != null && aiToken == null) {
            return if (cell.owner == CellOwner.PLAYER) {
                ClashResult.PlayerDefends
            } else {
                ClashResult.PlayerUncontested
            }
        }

        if (aiToken != null && playerToken == null) {
            return if (cell.owner == CellOwner.AI) {
                ClashResult.AIDefends
            } else {
                ClashResult.AIUncontested
            }
        }

        // Both placed -> Clash
        val playerStrength = calculateStrength(
            position = position,
            token = playerToken!!,
            owner = CellOwner.PLAYER,
            board = board,
            isLocked = playerToken.isLocked || playerLocks.contains(position)
        )

        val aiStrength = calculateStrength(
            position = position,
            token = aiToken!!,
            owner = CellOwner.AI,
            board = board,
            isLocked = aiToken.isLocked || aiLocks.contains(position)
        )

        return when {
            playerStrength > aiStrength -> {
                if (cell.owner == CellOwner.PLAYER) ClashResult.PlayerDefends
                else ClashResult.PlayerWins(playerStrength, aiStrength)
            }
            aiStrength > playerStrength -> {
                if (cell.owner == CellOwner.AI) ClashResult.AIDefends
                else ClashResult.AIWins(playerStrength, aiStrength)
            }
            else -> {
                ClashResult.Tie
            }
        }
    }

    private fun calculateStrength(
        position: Position,
        token: Token,
        owner: CellOwner,
        board: Board,
        isLocked: Boolean
    ): Int {
        val cell = board.get(position)
        val isVolatile = cell.modifier is CellModifier.Volatile
        val isBattleground = cell.modifier is CellModifier.Battleground
        
        // 1. Token base value
        val base = token.value
        
        // 2. Adjacency bonus (friendly neighbors currently owned on board)
        val adjacencyBonus = if (isVolatile) 0 else {
            val neighbors = position.neighbors()
            neighbors.count { board.get(it).owner == owner }
        }
        
        // 3. Fortify bonus (if defending own cell)
        val fortifyBonus = if (isVolatile) 0 else {
            if (cell.owner == owner) cell.fortify else 0
        }
        
        // 4. Lock bonus (+2 if locked)
        val lockBonus = if (isLocked) 2 else 0

        // 5. Battleground bonus (+1 to defender)
        val battlegroundBonus = if (isBattleground && cell.owner == owner) 1 else 0

        return base + adjacencyBonus + fortifyBonus + lockBonus + battlegroundBonus
    }
}
