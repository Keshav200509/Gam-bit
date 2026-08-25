package com.example.domain

import com.example.domain.ai.AIEngine
import com.example.domain.model.Board
import com.example.domain.model.Cell
import com.example.domain.model.CellOwner
import com.example.domain.model.ClashResult
import com.example.domain.model.DifficultyPreset
import com.example.domain.model.GamePhase
import com.example.domain.model.Position
import com.example.domain.model.RoundState
import com.example.domain.model.Score
import com.example.domain.model.Token
import com.example.domain.usecase.CalculateIncome
import com.example.domain.usecase.GenerateBoard
import com.example.domain.usecase.GenerateIntel
import com.example.domain.usecase.ResolveClash
import com.example.domain.usecase.ApplyLateGameEvents
import java.util.Random
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameEngine @Inject constructor(
    private val generateBoard: GenerateBoard,
    private val resolveClash: ResolveClash,
    private val calculateIncome: CalculateIncome,
    private val aiEngine: AIEngine,
    private val generateIntel: GenerateIntel,
    private val applyLateGameEvents: ApplyLateGameEvents,
    private val random: Random
) {
    var roundState: RoundState = RoundState()
        private set

    var score: Score = Score(0, 0)
        private set

    var difficultyPreset: DifficultyPreset = DifficultyPreset.VETERAN
        private set

    var activeConfig: com.example.domain.model.ArenaConfiguration? = null
        private set

    var isNewGamePlus: Boolean = false
        private set

    var aiMemory: com.example.domain.ai.AIMemory? = null
        private set

    fun restoreGameState(savedScore: Score, savedRoundState: RoundState, preset: DifficultyPreset = DifficultyPreset.VETERAN, isNewGamePlus: Boolean = false, aiMemory: com.example.domain.ai.AIMemory? = null) {
        this.score = savedScore
        this.roundState = savedRoundState
        this.difficultyPreset = preset
        this.isNewGamePlus = isNewGamePlus
        this.aiMemory = aiMemory
    }

    fun startNewGame(preset: DifficultyPreset = DifficultyPreset.VETERAN, seed: Long? = null): RoundState {
        score = Score(0, 0)
        this.difficultyPreset = preset
        this.activeConfig = null
        this.isNewGamePlus = false
        this.aiMemory = null
        if (seed != null) {
            random.setSeed(seed)
        } else {
            random.setSeed(System.currentTimeMillis())
        }
        val initialBoard = generateBoard()
        roundState = RoundState(
            roundNumber = 1,
            phase = GamePhase.Intel,
            board = initialBoard
        )
        val plan = generateAiPlacementsForCurrentRound()
        generateIntelForCurrentRound(plan)
        return roundState
    }

    fun startNewGameWithConfig(
        config: com.example.domain.model.ArenaConfiguration,
        seed: Long? = null,
        isNewGamePlus: Boolean = false,
        aiMemory: com.example.domain.ai.AIMemory? = null
    ): RoundState {
        score = Score(0, 0)
        this.isNewGamePlus = isNewGamePlus
        this.aiMemory = aiMemory
        
        val effectiveConfig = if (isNewGamePlus) {
            config.copy(
                aiCapabilityMin = 10,
                aiCapabilityMax = 10,
                intelTrickinessMultiplier = 2.0f,
                maxLocksPerRound = 1
            )
        } else {
            config
        }
        this.activeConfig = effectiveConfig
        
        this.difficultyPreset = when (effectiveConfig.level) {
            com.example.domain.model.GameLevel.LEVEL_1 -> DifficultyPreset.ROOKIE
            com.example.domain.model.GameLevel.LEVEL_2 -> DifficultyPreset.VETERAN
            com.example.domain.model.GameLevel.LEVEL_3 -> DifficultyPreset.MASTER
        }
        if (seed != null) {
            random.setSeed(seed)
        } else {
            random.setSeed(System.currentTimeMillis())
        }
        val initialBoard = generateBoard()
        roundState = RoundState(
            roundNumber = 1,
            phase = GamePhase.Intel,
            board = initialBoard
        )
        val plan = generateAiPlacementsForCurrentRound()
        generateIntelForCurrentRound(plan)
        return roundState
    }

    fun startNewRound(): RoundState {
        if (roundState.roundNumber >= 12) {
            roundState = roundState.copy(phase = GamePhase.GameOver)
            return roundState
        }

        val nextRoundNumber = roundState.roundNumber + 1

        // Apply late-game events
        val event = applyLateGameEvents.apply(
            board = roundState.board,
            roundNumber = nextRoundNumber,
            playerLocks = emptyList(),
            aiLocks = emptyList()
        )

        roundState = RoundState(
            roundNumber = nextRoundNumber,
            phase = GamePhase.Intel,
            board = event.modifiedBoard, // Use the modified board
            playerPlacements = emptyMap(),
            aiPlacements = emptyMap(),
            playerLocks = event.modifiedPlayerLocks,
            aiLocks = event.modifiedAiLocks,
            scoutUsed = false,
            scoutedPosition = null,
            scoutResult = null,
            incomeMultiplier = event.incomeMultiplier,
            lateGameMessage = event.message,
            lateGameEventType = event.type
        )

        val plan = generateAiPlacementsForCurrentRound()
        generateIntelForCurrentRound(plan)
        return roundState
    }

    fun transitionToPlacing(): RoundState {
        roundState = roundState.copy(phase = GamePhase.Placing)
        return roundState
    }

    fun commitPlayerPlacements(
        placements: Map<Position, Token>,
        locks: List<Position>
    ): RoundState {
        roundState = roundState.copy(
            playerPlacements = placements,
            playerLocks = locks,
            phase = GamePhase.Revealing
        )
        return roundState
    }

    fun transitionToResolving(): RoundState {
        roundState = roundState.copy(phase = GamePhase.Resolving)
        return roundState
    }

    /**
     * Resolves all clashes on the board, updating ownership and returns the final resolved state.
     */
    fun resolveRound(): RoundState {
        var updatedBoard = roundState.board
        
        // Resolve every position on the 5x5 board
        for (r in 0 until 5) {
            for (c in 0 until 5) {
                val pos = Position(r, c)
                val cell = updatedBoard.get(pos)
                
                val result = resolveClash(
                    position = pos,
                    playerPlacements = roundState.playerPlacements,
                    aiPlacements = roundState.aiPlacements,
                    board = updatedBoard,
                    playerLocks = roundState.playerLocks,
                    aiLocks = roundState.aiLocks
                )

                val newOwner = when (result) {
                    is ClashResult.PlayerWins -> CellOwner.PLAYER
                    is ClashResult.PlayerUncontested -> CellOwner.PLAYER
                    is ClashResult.PlayerDefends -> CellOwner.PLAYER
                    is ClashResult.AIWins -> CellOwner.AI
                    is ClashResult.AIUncontested -> CellOwner.AI
                    is ClashResult.AIDefends -> CellOwner.AI
                    is ClashResult.Tie -> CellOwner.NONE // Mutual destruction resets ownership
                }

                if (newOwner != cell.owner) {
                    updatedBoard = updatedBoard.set(pos, cell.copy(owner = newOwner))
                }
            }
        }

        roundState = roundState.copy(
            board = updatedBoard,
            phase = GamePhase.IncomeSummary
        )
        return roundState
    }

    fun collectIncome(): Pair<Int, Int> {
        val (playerIncome, aiIncome) = calculateIncome(roundState.board, roundState.roundNumber, roundState.incomeMultiplier)
        score = Score(
            playerScore = score.playerScore + playerIncome,
            aiScore = score.aiScore + aiIncome
        )
        return Pair(playerIncome, aiIncome)
    }

    fun transitionToTransitionOrGameOver(): RoundState {
        val allOwned = roundState.board.cells.flatten().all { it.owner != CellOwner.NONE }
        val nextPhase = if (roundState.roundNumber >= 12 || allOwned) GamePhase.GameOver else GamePhase.RoundTransition
        roundState = roundState.copy(phase = nextPhase)
        return roundState
    }

    private fun generateAiPlacementsForCurrentRound(): com.example.domain.ai.AIPlan {
        val config = activeConfig
        val aiMove = if (config != null) {
            aiEngine.makeMove(roundState.board, random, config, isNewGamePlus, aiMemory)
        } else {
            aiEngine.makeMove(roundState.board, random, difficultyPreset)
        }
        val aiPlacements = aiMove.plan.placements
        val aiLocks = aiMove.plan.locks

        roundState = roundState.copy(
            aiPlacements = aiPlacements,
            aiLocks = aiLocks,
            aiCapabilityName = aiMove.capability.name,
            aiCapabilityModifier = aiMove.capability.modifier
        )
        return aiMove.plan
    }

    private fun generateIntelForCurrentRound(aiPlan: com.example.domain.ai.AIPlan) {
        val round = roundState.roundNumber
        var trickiness = (round - 1) / 11.0f

        val config = activeConfig
        if (config != null) {
            trickiness *= config.intelTrickinessMultiplier
        } else {
            // Scale trickiness based on Difficulty Preset
            when (difficultyPreset) {
                DifficultyPreset.ROOKIE -> trickiness *= 0.7f
                DifficultyPreset.MASTER -> trickiness *= 1.3f
                DifficultyPreset.VETERAN -> {}
            }
        }
        trickiness = trickiness.coerceIn(0.0f, 1.0f)

        val isArena1 = config?.arena == com.example.domain.model.Arena.ASCENDENCY

        val intelHintObj = generateIntel.generate(
            aiPlan = aiPlan,
            board = roundState.board,
            trickiness = trickiness,
            random = random,
            isArena1 = isArena1
        )

        roundState = roundState.copy(
            intel = intelHintObj,
            intelHint = intelHintObj.text,
            intelConfidence = if (isNewGamePlus) "SUSPECT — counter-intel likely" else intelHintObj.reliabilityLabel
        )
    }

    fun applyScout(position: Position): RoundState {
        val isArena1 = activeConfig?.arena == com.example.domain.model.Arena.ASCENDENCY
        val result = if (isArena1) {
            val targetRegion = getRegionName(position.row, position.col)
            val isTargetingRegion = roundState.aiPlacements.keys.any { getRegionName(it.row, it.col) == targetRegion }
            if (isTargetingRegion) com.example.domain.model.ScoutResult.CONTESTED else com.example.domain.model.ScoutResult.CLEAR
        } else {
            val aiToken = roundState.aiPlacements[position]
            val hasToken = aiToken != null
            if (hasToken) com.example.domain.model.ScoutResult.CONTESTED else com.example.domain.model.ScoutResult.CLEAR
        }
        
        roundState = roundState.copy(
            scoutUsed = true,
            scoutedPosition = position,
            scoutResult = result
        )
        return roundState
    }

    private fun getRegionName(row: Int, col: Int): String {
        return when {
            row == 2 && col == 2 -> "center"
            row < 2 && col < 2 -> "northwest"
            row < 2 && col > 2 -> "northeast"
            row > 2 && col < 2 -> "southwest"
            row > 2 && col > 2 -> "southeast"
            row < 2 -> "north"
            row > 2 -> "south"
            col < 2 -> "west"
            else -> "east"
        }
    }
}
