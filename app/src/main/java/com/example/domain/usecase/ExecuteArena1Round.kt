package com.example.domain.usecase

import com.example.domain.ai.AICapability
import com.example.domain.ai.AIPlan
import com.example.domain.ai.Arena1AIPlanner
import com.example.domain.model.Board
import com.example.domain.model.CellOwner
import com.example.domain.model.ClashResult
import com.example.domain.model.Position
import com.example.domain.model.Token
import com.example.domain.model.ArenaConfiguration
import java.util.Random
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExecuteArena1Round @Inject constructor(
    private val aiPlanner: Arena1AIPlanner,
    private val clashResolver: ResolveClash,
    private val incomeCalculator: CalculateIncome,
    private val intelGenerator: GenerateIntel,
    private val random: Random
) {
    /**
     * Arena 1 round flow:
     * 1. Generate intel from AI's INTENDED strategy (before seeing player)
     *    - The AI pre-commits to a STRATEGY but not specific placements
     * 2. Player reads intel, uses scout/probability/lock, places 5 tokens
     * 3. Player commits
     * 4. AI sees player's placements, calculates optimal response
     * 5. AI places tokens
     * 6. Simultaneous reveal (player already sees their own, AI tokens appear)
     * 7. Resolve clashes
     * 8. Calculate income
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
        
        // Plan AI placements based on what player placed
        val aiPlan = aiPlanner.planPlacements(
            board = currentBoard,
            playerPlacements = playerPlacements,
            capability = capability,
            config = config,
            random = random
        )

        var updatedBoard = currentBoard
        
        // Resolve clashes
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

        // Calculate income
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

data class RoundResult(
    val updatedBoard: Board,
    val aiPlacements: Map<Position, Token>,
    val aiLocks: List<Position>,
    val playerIncome: Int,
    val aiIncome: Int,
    val capability: AICapability
)
