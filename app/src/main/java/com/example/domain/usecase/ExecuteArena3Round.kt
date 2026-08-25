package com.example.domain.usecase

import com.example.domain.ai.AICapability
import com.example.domain.ai.Arena3AIPlanner
import com.example.domain.model.Board
import com.example.domain.model.CellOwner
import com.example.domain.model.ClashResult
import com.example.domain.model.Position
import com.example.domain.model.Token
import com.example.domain.model.ArenaConfiguration
import java.util.Random
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ExecuteArena3Round handles the turn execution flow for Arena 3 (Oblivion).
 * Both players commit their placements simultaneously with zero information sharing tools.
 */
@Singleton
class ExecuteArena3Round @Inject constructor(
    private val aiPlanner: Arena3AIPlanner,
    private val clashResolver: ResolveClash,
    private val incomeCalculator: CalculateIncome,
    private val random: Random
) {
    /**
     * Arena 3 round flow:
     * 1. AI commits at round start (board state only, no player info)
     * 2. Player places tokens with NO information tools
     *    - No intel panel
     *    - No scout button
     *    - No probability tooltip on long-press
     *    - Lock available (if config.maxLocksPerRound > 0)
     * 3. Player commits
     * 4. Simultaneous reveal
     * 5. Resolve clashes
     * 6. Calculate income
     */
    suspend operator fun invoke(
        currentBoard: Board,
        playerPlacements: Map<Position, Token>,
        playerLocks: List<Position>,
        roundNumber: Int,
        config: ArenaConfiguration
    ): RoundResult {
        // AI capability evaluation
        val capability = AICapability.randomCapabilityFromConfig(random, config)
        
        // Plan AI placements based purely on board state (no player input)
        val aiPlan = aiPlanner.planPlacements(
            board = currentBoard,
            capability = capability,
            config = config,
            random = random
        )

        var updatedBoard = currentBoard
        
        // Resolve clashes on the 5x5 grid
        for (r in 0 until 5) {
            for (c in 0 until 5) {
                val pos = Position(r, c)
                val cell = updatedBoard.get(pos)
                
                val result = clashResolver(
                    position = pos,
                    playerPlacements = playerPlacements,
                    aiPlacements = aiPlan.placements,
                    board = updatedBoard,
                    playerLocks = playerLocks,
                    aiLocks = aiPlan.locks
                )

                val newOwner = when (result) {
                    is ClashResult.PlayerWins -> CellOwner.PLAYER
                    is ClashResult.PlayerUncontested -> CellOwner.PLAYER
                    is ClashResult.PlayerDefends -> CellOwner.PLAYER
                    is ClashResult.AIWins -> CellOwner.AI
                    is ClashResult.AIUncontested -> CellOwner.AI
                    is ClashResult.AIDefends -> CellOwner.AI
                    is ClashResult.Tie -> CellOwner.NONE
                }

                if (newOwner != cell.owner) {
                    updatedBoard = updatedBoard.set(pos, cell.copy(owner = newOwner))
                }
            }
        }

        // Calculate final income after resolution
        val (playerIncome, aiIncome) = incomeCalculator(updatedBoard)

        return RoundResult(
            updatedBoard = updatedBoard,
            aiPlacements = aiPlan.placements,
            aiLocks = aiPlan.locks,
            playerIncome = playerIncome,
            aiIncome = aiIncome,
            capability = capability
        )
    }
}
