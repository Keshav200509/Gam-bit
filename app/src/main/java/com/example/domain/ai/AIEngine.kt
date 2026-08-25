package com.example.domain.ai

import com.example.domain.model.Board
import com.example.domain.model.DifficultyPreset
import com.example.domain.model.Arena
import java.util.Random
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AIEngine acts as the orchestrator for all AI decisions: capability selection, strategy choice,
 * position evaluation/planning, and lock selection.
 * It strictly maintains domain-layer purity with zero Android or Jetpack Compose imports.
 */
@Singleton
class AIEngine @Inject constructor(
    private val arena3AIPlanner: Arena3AIPlanner
) {

    private val planner = AIPlanner()

    fun makeMove(board: Board, random: Random, preset: DifficultyPreset = DifficultyPreset.VETERAN): AIMove {
        val capability = AICapability.randomCapability(random, preset)
        val strategy = AIStrategy.chooseStrategy(board, random)
        val plan = planner.planPlacements(board, capability, strategy, random)
        return AIMove(plan, capability)
    }

    fun makeMove(
        board: Board,
        random: Random,
        config: com.example.domain.model.ArenaConfiguration,
        isNewGamePlus: Boolean = false,
        aiMemory: AIMemory? = null
    ): AIMove {
        val capability = if (isNewGamePlus) {
            AICapability.PEAK.apply { modifier = 10 }
        } else {
            AICapability.randomCapabilityFromConfig(random, config)
        }
        
        if (config.arena == Arena.OBLIVION) {
            val plan = arena3AIPlanner.planPlacements(board, capability, config, random)
            return AIMove(plan, capability)
        }
        val strategy = AIStrategy.chooseStrategy(board, random)
        val plan = planner.planPlacements(board, capability, strategy, random, isNewGamePlus, aiMemory)
        return AIMove(plan, capability)
    }
}
