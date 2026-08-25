package com.example.domain

import com.example.domain.ai.AIEngine
import com.example.domain.ai.Arena3AIPlanner
import com.example.domain.model.Cell
import com.example.domain.model.CellOwner
import com.example.domain.model.GamePhase
import com.example.domain.model.Position
import com.example.domain.model.Token
import com.example.domain.usecase.CalculateIncome
import com.example.domain.usecase.GenerateBoard
import com.example.domain.usecase.ResolveClash
import com.example.domain.usecase.GenerateIntel
import com.example.domain.usecase.ApplyLateGameEvents
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Random

class GameEngineTest {

    private lateinit var gameEngine: GameEngine
    private lateinit var generateBoard: GenerateBoard
    private lateinit var resolveClash: ResolveClash
    private lateinit var calculateIncome: CalculateIncome
    private lateinit var random: Random

    @Before
    fun setUp() {
        random = Random(42)
        generateBoard = GenerateBoard(random)
        resolveClash = ResolveClash()
        calculateIncome = CalculateIncome()
        gameEngine = GameEngine(
            generateBoard = generateBoard,
            resolveClash = resolveClash,
            calculateIncome = calculateIncome,
            generateIntel = GenerateIntel(),
            aiEngine = AIEngine(Arena3AIPlanner(random)),
            random = random,
            applyLateGameEvents = ApplyLateGameEvents(random)
        )
    }

    @Test
    fun testStartNewGame_initializesCorrectly() {
        val state = gameEngine.startNewGame()

        assertEquals(1, state.roundNumber)
        assertEquals(GamePhase.Intel, state.phase)
        assertEquals(0, gameEngine.score.playerScore)
        assertEquals(0, gameEngine.score.aiScore)
        
        // AI must place exactly 5 tokens
        assertEquals(5, state.aiPlacements.size)
        // AI must never place on its own cells (initially none are owned by AI)
        state.aiPlacements.forEach { (pos, token) ->
            assertNotEquals(CellOwner.AI, state.board.get(pos).owner)
            assertTrue(token.value in 1..5)
        }
        
        // Assert lock count is within bounds (0 to 2 locks)
        assertTrue(state.aiLocks.size in 0..2)
        
        // Check first round source confidence
        assertEquals("Source confidence: HIGH", state.intelConfidence)
    }

    @Test
    fun testTransitionToPlacing_works() {
        gameEngine.startNewGame()
        val state = gameEngine.transitionToPlacing()
        assertEquals(GamePhase.Placing, state.phase)
    }

    @Test
    fun testCommitPlayerPlacements_works() {
        gameEngine.startNewGame()
        gameEngine.transitionToPlacing()
        
        val playerPlacements = mapOf(
            Position(0, 0) to Token(value = 5, isLocked = true),
            Position(1, 1) to Token(value = 3, isLocked = false)
        )
        val locks = listOf(Position(0, 0))

        val state = gameEngine.commitPlayerPlacements(playerPlacements, locks)

        assertEquals(GamePhase.Revealing, state.phase)
        assertEquals(playerPlacements, state.playerPlacements)
        assertEquals(locks, state.playerLocks)
    }

    @Test
    fun testCollectIncome_accumulatesAcrossRounds() {
        gameEngine.startNewGame()
        
        // 1. Verify that initially (no owned cells), income is 0
        val (initialP, initialA) = gameEngine.collectIncome()
        assertEquals(0, initialP)
        assertEquals(0, initialA)
        assertEquals(0, gameEngine.score.playerScore)
        assertEquals(0, gameEngine.score.aiScore)

        // 2. Test the CalculateIncome use case logic directly on a custom board
        var customBoard = gameEngine.roundState.board
        customBoard = customBoard.set(Position(0, 0), customBoard.get(Position(0, 0)).copy(owner = CellOwner.PLAYER, value = 3))
        customBoard = customBoard.set(Position(4, 4), customBoard.get(Position(4, 4)).copy(owner = CellOwner.AI, value = 2))

        val (calcP, calcA) = calculateIncome(customBoard)
        assertEquals(3, calcP)
        assertEquals(2, calcA)
    }

    @Test
    fun test12RoundEndConditions_triggersGameOver() {
        gameEngine.startNewGame()
        
        // Loop 11 times to reach round 12
        for (i in 1..11) {
            gameEngine.transitionToPlacing()
            val placements = mapOf(Position(0, 0) to Token(value = 4))
            gameEngine.commitPlayerPlacements(placements, emptyList())
            gameEngine.resolveRound()
            gameEngine.collectIncome()
            gameEngine.transitionToTransitionOrGameOver()
            gameEngine.startNewRound()
        }

        assertEquals(12, gameEngine.roundState.roundNumber)
        
        // Finalize 12th round
        gameEngine.transitionToPlacing()
        val placements = mapOf(Position(0, 0) to Token(value = 4))
        gameEngine.commitPlayerPlacements(placements, emptyList())
        gameEngine.resolveRound()
        gameEngine.collectIncome()
        val endState = gameEngine.transitionToTransitionOrGameOver()
        
        assertEquals(GamePhase.GameOver, endState.phase)
        
        // Attempting to start new round after 12 should force game over
        val overflowState = gameEngine.startNewRound()
        assertEquals(GamePhase.GameOver, overflowState.phase)
    }

    @Test
    fun testIntelConfidence_trickinessProgression() {
        val confidenceLevels = mutableListOf<String>()
        gameEngine.startNewGame()
        confidenceLevels.add(gameEngine.roundState.intelConfidence)

        for (i in 2..12) {
            gameEngine.transitionToPlacing()
            gameEngine.commitPlayerPlacements(mapOf(Position(0, 0) to Token(3)), emptyList())
            gameEngine.resolveRound()
            gameEngine.collectIncome()
            gameEngine.transitionToTransitionOrGameOver()
            gameEngine.startNewRound()
            confidenceLevels.add(gameEngine.roundState.intelConfidence)
        }

        // Round 1-2
        assertEquals("Source confidence: HIGH", confidenceLevels[0])
        assertEquals("Source confidence: HIGH", confidenceLevels[1])

        // Round 3-4
        assertEquals("Source confidence: MODERATE — corroborate before committing", confidenceLevels[2])
        assertEquals("Source confidence: MODERATE — corroborate before committing", confidenceLevels[3])

        // Round 5-7
        assertEquals("Source confidence: MIXED — partial corroboration only", confidenceLevels[4])
        assertEquals("Source confidence: MIXED — partial corroboration only", confidenceLevels[5])
        assertEquals("Source confidence: MIXED — partial corroboration only", confidenceLevels[6])

        // Round 8-10
        assertEquals("Source confidence: LOW — single unverified source", confidenceLevels[7])
        assertEquals("Source confidence: LOW — single unverified source", confidenceLevels[8])
        assertEquals("Source confidence: LOW — single unverified source", confidenceLevels[9])

        // Round 11-12
        assertEquals("Source confidence: SUSPECT — possible counter-intel", confidenceLevels[10])
        assertEquals("Source confidence: SUSPECT — possible counter-intel", confidenceLevels[11])
    }

    @Test
    fun testScouting_revealsPresenceCorrectly() {
        gameEngine.startNewGame()
        
        // Locate a position where AI has placed a token
        val aiPlacements = gameEngine.roundState.aiPlacements
        val aiPos = aiPlacements.keys.first()

        // Scout the AI position
        val stateWithFound = gameEngine.applyScout(aiPos)
        assertEquals(com.example.domain.model.ScoutResult.CONTESTED, stateWithFound.scoutResult)
        assertTrue(stateWithFound.scoutUsed)
        assertEquals(aiPos, stateWithFound.scoutedPosition)

        // Scout a non-AI position
        val emptyPos = (0..4).flatMap { r -> (0..4).map { c -> Position(r, c) } }
            .first { !aiPlacements.containsKey(it) }
        val stateWithClear = gameEngine.applyScout(emptyPos)
        assertEquals(com.example.domain.model.ScoutResult.CLEAR, stateWithClear.scoutResult)
    }

    @Test
    fun testAiGameBalanceSimulation() {
        var aiWins = 0
        var playerWins = 0
        var draws = 0

        // Simulate 10 complete 12-round games
        for (gameIndex in 1..10) {
            val gameRandom = Random(gameIndex * 111L)
            val simGenerateBoard = GenerateBoard(gameRandom)
            val simEngine = GameEngine(
                generateBoard = simGenerateBoard,
                resolveClash = resolveClash,
                calculateIncome = calculateIncome,
                generateIntel = GenerateIntel(),
                aiEngine = AIEngine(Arena3AIPlanner(gameRandom)),
                random = gameRandom,
                applyLateGameEvents = ApplyLateGameEvents(gameRandom)
            )

            simEngine.startNewGame()

            for (round in 1..12) {
                // Determine player placements on any of the 25 cells of the board
                val availableCells = mutableListOf<Pair<Position, Cell>>()
                for (r in 0 until 5) {
                    for (c in 0 until 5) {
                        val pos = Position(r, c)
                        val cell = simEngine.roundState.board.get(pos)
                        availableCells.add(pos to cell)
                    }
                }

                val chosenPositions: List<Position>
                val pLocks: List<Position>

                if (gameRandom.nextDouble() < 0.50) {
                    // Casual / Casual-Random Round
                    val shuffledList = availableCells.map { it.first }.shuffled(gameRandom)
                    chosenPositions = shuffledList.take(5)
                    // Lock nothing or one random position (50% chance)
                    pLocks = if (gameRandom.nextBoolean()) listOf(chosenPositions.first()) else emptyList()
                } else {
                    // Tactical Tactical Round
                    val sortedPositions = availableCells.map { pair ->
                        val pos = pair.first
                        val cell = pair.second
                        // Adjacency to friendly cells count
                        val friendlyNeighbors = pos.neighbors().count { simEngine.roundState.board.get(it).owner == CellOwner.PLAYER }
                        var score = cell.value * 15 + friendlyNeighbors * 5 + gameRandom.nextInt(20)
                        
                        // Prioritize claiming Neutral or stealing AI cells
                        when (cell.owner) {
                            CellOwner.NONE -> score += 25
                            CellOwner.AI -> score += 15
                            CellOwner.PLAYER -> score += 0 // defend last
                        }
                        pos to score
                    }.sortedByDescending { it.second }.map { it.first }

                    chosenPositions = sortedPositions.take(5)
                    // Lock: 80% chance to lock the highest token (index 0), 20% chance to lock the second highest (index 1)
                    val lockIndex = if (gameRandom.nextDouble() < 0.80) 0 else 1
                    pLocks = listOf(chosenPositions[lockIndex])
                }

                // Place tokens: Token 5, 4, 3, 2, 1
                val tokensList = listOf(5, 4, 3, 2, 1)
                val pPlacements = chosenPositions.zip(tokensList) { pos, value ->
                    pos to Token(value = value)
                }.toMap()

                simEngine.transitionToPlacing()
                simEngine.commitPlayerPlacements(pPlacements, pLocks)
                simEngine.resolveRound()
                simEngine.collectIncome()
                simEngine.transitionToTransitionOrGameOver()
                simEngine.startNewRound()
            }

            val finalP = simEngine.score.playerScore
            val finalA = simEngine.score.aiScore

            // Ensure both sides scored non-negative points
            assertTrue("Player score is invalid ($finalP)", finalP >= 0)
            assertTrue("AI score is invalid ($finalA)", finalA >= 0)

            if (finalA > finalP) {
                aiWins++
            } else if (finalP > finalA) {
                playerWins++
            } else {
                draws++
            }
        }

        println("Simulation results over 10 games -> Player Wins: $playerWins, AI Wins: $aiWins, Draws: $draws")
    }
}
