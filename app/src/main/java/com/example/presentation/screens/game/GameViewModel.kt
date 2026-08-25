package com.example.presentation.screens.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.SavedStateHandle
import com.example.data.local.PreferencesManager
import com.example.domain.DailyChallenge
import com.example.domain.achievements.Achievement
import com.example.domain.GameEngine
import com.example.domain.model.CellOwner
import com.example.domain.model.ClashResult
import com.example.domain.model.ScoutResult
import com.example.domain.model.GamePhase
import com.example.domain.model.Position
import com.example.domain.model.RoundState
import com.example.domain.model.Token
import com.example.domain.model.Arena
import com.example.domain.model.GameLevel
import com.example.domain.model.ArenaConfiguration
import com.example.domain.usecase.ResolveClash
import com.example.domain.usecase.UseScout
import com.example.domain.usecase.ToggleLock
import com.example.domain.usecase.CalculateProbabilities
import com.example.domain.model.LockResult
import com.example.domain.model.ProbabilityBreakdown
import com.example.presentation.GambitHapticFeedback
import com.example.presentation.SoundManager
import com.example.presentation.GameSound
import com.example.presentation.components.ClashStage
import com.example.domain.usecase.StartRoundTimer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GameUiState(
    val roundState: RoundState,
    val playerScore: Int = 0,
    val aiScore: Int = 0,
    val selectedToken: Int? = null,
    val scoutMode: Boolean = false,
    val lockMode: Boolean = false,
    val playerIncomeThisRound: Int = 0,
    val aiIncomeThisRound: Int = 0,
    val currentResolvingPosition: Position? = null,
    val clashResults: Map<Position, ClashResult> = emptyMap(),
    val hoveredPosition: Position? = null,
    val activeProbabilityBreakdown: ProbabilityBreakdown? = null,
    val clashStage: ClashStage = ClashStage.NONE,
    val showRoundTransitionBanner: Boolean = false,
    val bannerRoundNumber: Int = 1,
    val showResumeDialog: Boolean = false,
    val isDailyChallenge: Boolean = false,
    val dailyChallengeSeed: Long = 0L,
    val currentArena: Arena = Arena.ASCENDENCY,
    val currentLevel: GameLevel = GameLevel.LEVEL_1,
    val currentConfig: ArenaConfiguration = ArenaConfiguration.get(Arena.ASCENDENCY, GameLevel.LEVEL_1),
    val timeRemaining: Long = 60L,
    val timerActive: Boolean = false,
    val intelAccuracyCount: Int = 0,
    val intelTotalCount: Int = 0,
    val scoreHistory: List<Pair<Int, Int>> = emptyList(),
    val lastRoundPlayerDelta: Int = 0,
    val lastRoundAiDelta: Int = 0
)

@OptIn(kotlinx.coroutines.FlowPreview::class)
@HiltViewModel
class GameViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val gameEngine: GameEngine,
    private val resolveClash: ResolveClash,
    private val useScout: UseScout,
    private val toggleLock: ToggleLock,
    private val calculateProbabilities: CalculateProbabilities,
    val soundManager: SoundManager,
    val hapticFeedback: GambitHapticFeedback,
    private val gameSaveRepository: com.example.data.repository.GameSaveRepository,
    private val recordGameStats: com.example.domain.usecase.RecordGameStats,
    private val preferencesManager: PreferencesManager,
    private val executeArena1Round: com.example.domain.usecase.ExecuteArena1Round,
    private val executeArena3Round: com.example.domain.usecase.ExecuteArena3Round,
    private val startRoundTimer: StartRoundTimer,
    private val dailyChallengeRepository: com.example.data.repository.DailyChallengeRepository,
    private val profileRepository: com.example.data.firestore.UserProfileRepository,
    private val auth: com.google.firebase.auth.FirebaseAuth
) : ViewModel() {

    // Telemetry Statistics Trackers
    private var clashesWonCount = 0
    private var clashesLostCount = 0
    private var scoutUsesCount = 0
    private var lockUsesCount = 0
    private var intelTrueCount = 0
    private var roundsTracked = 0

    private var scoutedClearPositionThisRound: Position? = null
    private var wasBehindAtRound8 = false
    private var gameStartTime = System.currentTimeMillis()

    private var isResolutionPaused = false
    private var timerJob: kotlinx.coroutines.Job? = null

    fun onPause() {
        isResolutionPaused = true
    }

    fun onResume() {
        isResolutionPaused = false
    }

    private val _uiState: MutableStateFlow<GameUiState>
    val uiState: StateFlow<GameUiState>

    init {
        val arenaArg = savedStateHandle.get<String>("arenaName")
        val levelArg = savedStateHandle.get<String>("levelName")

        val initialArena = if (arenaArg != null) {
            try { Arena.valueOf(arenaArg) } catch (e: Exception) { Arena.ASCENDENCY }
        } else {
            Arena.ASCENDENCY
        }

        val initialLevel = if (levelArg != null) {
            try { GameLevel.valueOf(levelArg) } catch (e: Exception) { GameLevel.LEVEL_1 }
        } else {
            GameLevel.LEVEL_1
        }

        val config = ArenaConfiguration.get(initialArena, initialLevel)
        var startState = gameEngine.startNewGameWithConfig(config)
        if (!config.intelEnabled && startState.phase == GamePhase.Intel) {
            startState = gameEngine.transitionToPlacing()
        }

        _uiState = MutableStateFlow(
            GameUiState(
                roundState = startState,
                playerScore = gameEngine.score.playerScore,
                aiScore = gameEngine.score.aiScore,
                currentArena = initialArena,
                currentLevel = initialLevel,
                currentConfig = config
            )
        )
        uiState = _uiState.asStateFlow()

        val isDailyArg = savedStateHandle.get<Boolean>("isDaily") ?: false
        if (isDailyArg) {
            startDailyChallenge()
        } else {
            // Query database to check if a prior simulation save exists
            viewModelScope.launch {
                try {
                    val save = gameSaveRepository.getSave()
                    if (save != null) {
                        _uiState.update { it.copy(showResumeDialog = true) }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Auto-save: Save game state on every change, debounced 500ms
        viewModelScope.launch {
            uiState
                .map { Triple(it.roundState, com.example.domain.model.Score(it.playerScore, it.aiScore), Pair(it.currentArena, it.currentLevel)) }
                .distinctUntilChanged()
                .debounce(500)
                .collect { (roundState, score, arenaAndLevel) ->
                    try {
                        if (roundState.phase != GamePhase.GameOver && !uiState.value.showResumeDialog) {
                            val serialized = com.example.data.mapper.GameStateMapper.serialize(
                                score = score,
                                roundState = roundState,
                                arena = arenaAndLevel.first.name,
                                level = arenaAndLevel.second.name
                            )
                            gameSaveRepository.saveGame(serialized)
                        } else if (roundState.phase == GamePhase.GameOver) {
                            gameSaveRepository.deleteSave()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
        }

        // Observe game phases to automatically start/cancel the round timer
        viewModelScope.launch {
            uiState
                .map { it.roundState.phase }
                .distinctUntilChanged()
                .collect { phase ->
                    if (phase == GamePhase.Placing) {
                        startTimer()
                    } else {
                        timerJob?.cancel()
                        _uiState.update { it.copy(timerActive = false) }
                    }
                }
        }
    }

    fun resumeGame() {
        viewModelScope.launch {
            try {
                val save = gameSaveRepository.getSave()
                if (save != null) {
                    val pair = com.example.data.mapper.GameStateMapper.deserialize(save.roundStateJson)
                    if (pair != null) {
                        val (savedScore, savedRoundState) = pair
                        val (arenaName, levelName) = com.example.data.mapper.GameStateMapper.deserializeArenaAndLevel(save.roundStateJson)
                        val restoredArena = try { Arena.valueOf(arenaName) } catch (e: Exception) { Arena.ASCENDENCY }
                        val restoredLevel = try { GameLevel.valueOf(levelName) } catch (e: Exception) { GameLevel.LEVEL_1 }
                        val config = ArenaConfiguration.get(restoredArena, restoredLevel)

                        gameEngine.restoreGameState(savedScore, savedRoundState)
                        gameEngine.startNewGameWithConfig(config)
                        gameEngine.restoreGameState(savedScore, savedRoundState)
                        
                        roundsTracked = savedRoundState.roundNumber - 1
                        scoutUsesCount = if (savedRoundState.scoutUsed) 1 else 0
                        
                        var roundState = savedRoundState
                        if (!config.intelEnabled && roundState.phase == GamePhase.Intel) {
                            roundState = gameEngine.transitionToPlacing()
                        }

                        _uiState.update { state ->
                            state.copy(
                                roundState = roundState,
                                playerScore = savedScore.playerScore,
                                aiScore = savedScore.aiScore,
                                currentArena = restoredArena,
                                currentLevel = restoredLevel,
                                currentConfig = config,
                                showResumeDialog = false
                            )
                        }
                    } else {
                        discardSaveAndNewGame()
                    }
                } else {
                    discardSaveAndNewGame()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                discardSaveAndNewGame()
            }
        }
    }

    fun discardSaveAndNewGame() {
        viewModelScope.launch {
            try {
                gameSaveRepository.deleteSave()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            clashesWonCount = 0
            clashesLostCount = 0
            scoutUsesCount = 0
            lockUsesCount = 0
            intelTrueCount = 0
            roundsTracked = 0

            scoutedClearPositionThisRound = null
            wasBehindAtRound8 = false
            gameStartTime = System.currentTimeMillis()

            val currentState = uiState.value
            val config = ArenaConfiguration.get(currentState.currentArena, currentState.currentLevel)
            var newRoundState = gameEngine.startNewGameWithConfig(config)
            if (!config.intelEnabled && newRoundState.phase == GamePhase.Intel) {
                newRoundState = gameEngine.transitionToPlacing()
            }
            _uiState.update { state ->
                state.copy(
                    roundState = newRoundState,
                    playerScore = gameEngine.score.playerScore,
                    aiScore = gameEngine.score.aiScore,
                    showResumeDialog = false
                )
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            startRoundTimer(60L) { uiState.value.scoutMode || uiState.value.lockMode }
                .collect { time ->
                    _uiState.update { state ->
                        state.copy(
                            timeRemaining = time,
                            timerActive = true
                        )
                    }
                    if (time == 10L) {
                        soundManager.playSound(GameSound.TIMER_TICK_SOFT)
                    } else if (time in 1L..5L) {
                        soundManager.playSound(GameSound.TIMER_TICK_URGENT)
                    } else if (time == 0L) {
                        soundManager.playSound(GameSound.TIMER_EXPIRED)
                        onCommitClicked(isAutoCommit = true)
                    }
                }
        }
    }

    fun startPlacing() {
        if (uiState.value.roundState.phase == GamePhase.Intel) {
            val nextRoundState = gameEngine.transitionToPlacing()
            val placedValues = nextRoundState.playerPlacements.values.map { it.value }.toSet()
            val firstAvailable = (1..5).firstOrNull { it !in placedValues } ?: 1
            _uiState.update { state ->
                state.copy(
                    roundState = nextRoundState,
                    selectedToken = firstAvailable
                )
            }
        }
    }

    fun onTokenSelected(token: Int) {
        if (uiState.value.roundState.phase != GamePhase.Placing) return
        _uiState.update { state ->
            state.copy(
                selectedToken = if (state.selectedToken == token) null else token
            )
        }
        soundManager.playSound(GameSound.TOKEN_PLACE)
        hapticFeedback.tokenPlaced()
    }

    fun onCellClicked(position: Position) {
        val currentState = uiState.value
        val phase = currentState.roundState.phase
        if (phase != GamePhase.Placing) return

        // 1. Scout Mode handling
        if (currentState.scoutMode) {
            val result = useScout.execute(position, currentState.roundState.aiPlacements)
            if (result == ScoutResult.CLEAR) {
                scoutedClearPositionThisRound = position
            } else {
                scoutedClearPositionThisRound = null
            }
            _uiState.update { state ->
                state.copy(
                    roundState = state.roundState.copy(
                        scoutUsed = true,
                        scoutedPosition = position,
                        scoutResult = result
                    ),
                    scoutMode = false
                )
            }
            soundManager.playSound(GameSound.SCOUT)
            hapticFeedback.scoutUsed()
            return
        }

        // 2. Lock Mode handling
        if (currentState.lockMode) {
            val lockResult = toggleLock.execute(
                position = position,
                currentLocks = currentState.roundState.playerLocks,
                playerPlacements = currentState.roundState.playerPlacements
            )
            when (lockResult) {
                is LockResult.Added -> {
                    val updatedPlacements = currentState.roundState.playerPlacements.toMutableMap()
                    val token = updatedPlacements[position]
                    if (token != null) {
                        updatedPlacements[position] = token.copy(isLocked = true)
                    }
                    _uiState.update { state ->
                        state.copy(
                            roundState = state.roundState.copy(
                                playerPlacements = updatedPlacements,
                                playerLocks = lockResult.updatedLocks
                            )
                        )
                    }
                    soundManager.playSound(GameSound.LOCK)
                    hapticFeedback.lockToggled()
                }
                is LockResult.Removed -> {
                    val updatedPlacements = currentState.roundState.playerPlacements.toMutableMap()
                    val token = updatedPlacements[position]
                    if (token != null) {
                        updatedPlacements[position] = token.copy(isLocked = false)
                    }
                    _uiState.update { state ->
                        state.copy(
                            roundState = state.roundState.copy(
                                playerPlacements = updatedPlacements,
                                playerLocks = lockResult.updatedLocks
                            )
                        )
                    }
                    soundManager.playSound(GameSound.LOCK)
                    hapticFeedback.lockToggled()
                }
                LockResult.LimitReached -> {
                    // Maximum of 2 locks reached
                }
                LockResult.NotOwned -> {
                    // Must have a token to lock
                }
            }
            return
        }

        // 3. Regular Token Placement/Removal
        val existingToken = currentState.roundState.playerPlacements[position]
        val selected = currentState.selectedToken

        if (existingToken != null) {
            // Clicked on a cell with an existing token -> Pick it up & select it
            val updatedPlacements = currentState.roundState.playerPlacements.toMutableMap()
            updatedPlacements.remove(position)

            // Also remove from locks if it was locked
            val updatedLocks = currentState.roundState.playerLocks.filter { it != position }

            _uiState.update { state ->
                state.copy(
                    selectedToken = existingToken.value,
                    roundState = state.roundState.copy(
                        playerPlacements = updatedPlacements,
                        playerLocks = updatedLocks
                    )
                )
            }
            soundManager.playSound(GameSound.TOKEN_PLACE)
            hapticFeedback.tokenPlaced()
        } else if (selected != null) {
            // Place selected token on cell
            val updatedPlacements = currentState.roundState.playerPlacements.toMutableMap()
            
            // If the selected token is already placed elsewhere, remove it first
            val existingPosForToken = updatedPlacements.entries.find { it.value.value == selected }?.key
            if (existingPosForToken != null) {
                updatedPlacements.remove(existingPosForToken)
            }

            updatedPlacements[position] = Token(value = selected, isLocked = false)

            // Find NEXT available unplaced token
            val placedValues = updatedPlacements.values.map { it.value }.toSet()
            val nextToken = (1..5).firstOrNull { it !in placedValues }

            _uiState.update { state ->
                state.copy(
                    selectedToken = nextToken,
                    roundState = state.roundState.copy(
                        playerPlacements = updatedPlacements
                    )
                )
            }
            soundManager.playSound(GameSound.TOKEN_PLACE)
            hapticFeedback.tokenPlaced()
        } else {
            // No token selected, but clicked an empty cell -> Auto-place the lowest available token
            val placedValues = currentState.roundState.playerPlacements.values.map { it.value }.toSet()
            val availableToken = (1..5).firstOrNull { it !in placedValues }
            if (availableToken != null) {
                val updatedPlacements = currentState.roundState.playerPlacements.toMutableMap()
                updatedPlacements[position] = Token(value = availableToken, isLocked = false)
                val newPlaced = updatedPlacements.values.map { it.value }.toSet()
                val nextToken = (1..5).firstOrNull { it !in newPlaced }
                _uiState.update { state ->
                    state.copy(
                        selectedToken = nextToken,
                        roundState = state.roundState.copy(
                            playerPlacements = updatedPlacements
                        )
                    )
                }
                soundManager.playSound(GameSound.TOKEN_PLACE)
                hapticFeedback.tokenPlaced()
            }
        }
    }

    fun onCellLongPressed(position: Position) {
        val currentState = uiState.value
        val phase = currentState.roundState.phase
        if (phase != GamePhase.Placing) return
        if (!currentState.currentConfig.probabilityPreviewEnabled) return

        val roundState = currentState.roundState
        val trickiness = (roundState.roundNumber - 1) / 11.0f
        val capability = try {
            com.example.domain.ai.AICapability.valueOf(roundState.aiCapabilityName).apply {
                modifier = roundState.aiCapabilityModifier
            }
        } catch (e: Exception) {
            com.example.domain.ai.AICapability.STEADY.apply {
                modifier = roundState.aiCapabilityModifier
            }
        }
        val playerTokenVal = currentState.roundState.playerPlacements[position]?.value ?: currentState.selectedToken ?: 1

        val breakdown = calculateProbabilities.execute(
            position = position,
            playerToken = playerTokenVal,
            board = roundState.board,
            intel = roundState.intel,
            scoutResult = if (roundState.scoutedPosition == position) roundState.scoutResult else null,
            aiCapability = capability,
            trickiness = trickiness,
            random = java.util.Random()
        )

        _uiState.update { state ->
            state.copy(
                hoveredPosition = position,
                activeProbabilityBreakdown = breakdown
            )
        }
    }

    fun dismissProbabilityTooltip() {
        _uiState.update { state ->
            state.copy(
                hoveredPosition = null,
                activeProbabilityBreakdown = null
            )
        }
    }

    fun onScoutToggled() {
        val currentState = uiState.value
        val phase = currentState.roundState.phase
        if (phase != GamePhase.Placing) return
        if (!currentState.currentConfig.scoutEnabled) return
        if (currentState.roundState.scoutUsed) return // Already used this round
        _uiState.update { state ->
            state.copy(
                scoutMode = !state.scoutMode,
                lockMode = false
            )
        }
    }

    fun onLockToggled() {
        val currentState = uiState.value
        val phase = currentState.roundState.phase
        if (phase != GamePhase.Placing) return
        if (currentState.currentConfig.maxLocksPerRound == 0) return
        _uiState.update { state ->
            state.copy(
                lockMode = !state.lockMode,
                scoutMode = false
            )
        }
    }

    fun onClearClicked() {
        val phase = uiState.value.roundState.phase
        if (phase != GamePhase.Placing) return
        _uiState.update { state ->
            state.copy(
                selectedToken = null,
                scoutMode = false,
                lockMode = false,
                roundState = state.roundState.copy(
                    playerPlacements = emptyMap(),
                    playerLocks = emptyList()
                )
            )
        }
    }

    fun onCommitClicked(isAutoCommit: Boolean = false) {
        val state = uiState.value
        if (state.roundState.phase != GamePhase.Placing) return
        if (!isAutoCommit && state.roundState.playerPlacements.isEmpty()) return // Must place at least 1 token

        timerJob?.cancel() // Cancel the active timer immediately

        viewModelScope.launch {
            var clashesWonThisRound = 0
            // 1. Commit Player placements -> Transition to Revealing
            var committedState = gameEngine.commitPlayerPlacements(
                placements = state.roundState.playerPlacements,
                locks = state.roundState.playerPlacements.filter { it.value.isLocked }.keys.toList()
            )
            
            if (state.currentArena == com.example.domain.model.Arena.ASCENDENCY) {
                // Temporarily clear AI placements to simulate calculation delay
                _uiState.update { it.copy(
                    roundState = committedState.copy(aiPlacements = emptyMap(), aiLocks = emptyList()),
                    scoutMode = false,
                    lockMode = false
                ) }
                
                // 400ms artificial delay for visual calculation feedback
                delay(400)
                
                // Calculate optimal AI response based on player placements
                val arena1Result = executeArena1Round(
                    currentBoard = committedState.board,
                    playerPlacements = committedState.playerPlacements,
                    playerLocks = committedState.playerLocks,
                    roundNumber = committedState.roundNumber,
                    config = state.currentConfig
                )
                
                // Update committedState with actual AI placements and lock choices
                committedState = committedState.copy(
                    aiPlacements = arena1Result.aiPlacements,
                    aiLocks = arena1Result.aiLocks,
                    aiCapabilityName = arena1Result.capability.name,
                    aiCapabilityModifier = arena1Result.capability.modifier
                )
                
                // Sync back to gameEngine
                gameEngine.restoreGameState(
                    savedScore = gameEngine.score,
                    savedRoundState = committedState
                )
            }

            // Record placements metadata for tracking
            if (committedState.scoutUsed) {
                scoutUsesCount++
            }
            lockUsesCount += committedState.playerLocks.size
            if (committedState.intel?.isTrue == true) {
                intelTrueCount++
            }
            roundsTracked++

            _uiState.update { it.copy(
                roundState = committedState, 
                scoutMode = false, 
                lockMode = false,
                intelAccuracyCount = intelTrueCount,
                intelTotalCount = roundsTracked
            ) }

            // Show AI placements in Revealing phase for 600ms
            delay(600)

            // 2. Transition to Resolving phase
            val resolvingState = gameEngine.transitionToResolving()
            _uiState.update { it.copy(roundState = resolvingState) }

            // Find all positions that have a token placed by player or AI
            val activePositions = (committedState.playerPlacements.keys + committedState.aiPlacements.keys)
                .distinct()
                .sortedWith(compareBy({ it.row }, { it.col })) // Top-left to bottom-right order

            var currentBoard = committedState.board
            val clashResults = mutableMapOf<Position, ClashResult>()

            // Resolve each clash one by one with granular animation sub-stages
            for (pos in activePositions) {
                while (isResolutionPaused) { delay(100) }
                // Stage 2.1: BOTH tokens appear on the cell (200ms)
                _uiState.update { 
                    it.copy(
                        currentResolvingPosition = pos,
                        clashStage = ClashStage.TOKENS_APPEAR
                    )
                }
                soundManager.playSound(GameSound.TOKEN_PLACE)
                hapticFeedback.tokenPlaced()
                delay(200)

                while (isResolutionPaused) { delay(100) }
                // Stage 2.2: Strength numbers appear below each token (100ms)
                _uiState.update { 
                    it.copy(clashStage = ClashStage.STRENGTHS_SHOWN)
                }
                delay(100)

                while (isResolutionPaused) { delay(100) }
                // Stage 2.3: Winner token scales up, loser token fades out (200ms)
                val result = resolveClash(
                    position = pos,
                    playerPlacements = committedState.playerPlacements,
                    aiPlacements = committedState.aiPlacements,
                    board = currentBoard,
                    playerLocks = committedState.playerLocks,
                    aiLocks = committedState.aiLocks
                )
                clashResults[pos] = result

                _uiState.update { 
                    it.copy(
                        clashStage = ClashStage.OUTCOME_REVEALED,
                        clashResults = clashResults.toMap()
                    )
                }

                // Play sound and trigger custom haptic patterns based on the result
                when (result) {
                    is ClashResult.PlayerWins, is ClashResult.PlayerUncontested, is ClashResult.PlayerDefends -> {
                        soundManager.playSound(GameSound.CLASH_WIN)
                        hapticFeedback.clashWon()
                        clashesWonCount++
                        clashesWonThisRound++

                        if (pos == scoutedClearPositionThisRound && result is ClashResult.PlayerUncontested) {
                            try {
                                preferencesManager.incrementScoutEffectiveCount()
                                val scoutCount = preferencesManager.scoutEffectiveCount.first()
                                if (scoutCount >= 10) {
                                    preferencesManager.unlockAchievement(Achievement.SCOUT_MASTER.id)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    is ClashResult.AIWins, is ClashResult.AIUncontested, is ClashResult.AIDefends -> {
                        soundManager.playSound(GameSound.CLASH_LOSE)
                        hapticFeedback.clashLost()
                        clashesLostCount++
                    }
                    is ClashResult.Tie -> {
                        soundManager.playSound(GameSound.CLASH_TIE)
                        hapticFeedback.clashTied()
                    }
                }
                delay(200)

                while (isResolutionPaused) { delay(100) }
                // Stage 2.4: Cell background animates to winner color (100ms)
                val newOwner = when (result) {
                    is ClashResult.PlayerWins -> CellOwner.PLAYER
                    is ClashResult.PlayerUncontested -> CellOwner.PLAYER
                    is ClashResult.PlayerDefends -> CellOwner.PLAYER
                    is ClashResult.AIWins -> CellOwner.AI
                    is ClashResult.AIUncontested -> CellOwner.AI
                    is ClashResult.AIDefends -> CellOwner.AI
                    is ClashResult.Tie -> CellOwner.NONE
                }

                val currentCell = currentBoard.get(pos)
                currentBoard = currentBoard.set(pos, currentCell.copy(owner = newOwner))

                // Update UI state with updated board and clash result
                _uiState.update {
                    it.copy(
                        roundState = it.roundState.copy(board = currentBoard),
                        clashResults = clashResults.toMap()
                    )
                }
                delay(100)

                while (isResolutionPaused) { delay(100) }
                // Stage 2.5: Brief pause before next clash (100ms)
                delay(100)
            }

            // Clear current resolving position indicators
            _uiState.update { 
                it.copy(
                    currentResolvingPosition = null,
                    clashStage = ClashStage.NONE
                )
            }

            // 3. Complete round resolution inside the engine and update state to IncomeSummary
            val finalResolvedState = gameEngine.resolveRound()
            _uiState.update { it.copy(roundState = finalResolvedState) }

            // 4. Collect income and accumulate scores
            val (pIncome, aIncome) = gameEngine.collectIncome()
            _uiState.update {
                val newHistory = it.scoreHistory + Pair(gameEngine.score.playerScore, gameEngine.score.aiScore)
                it.copy(
                    playerScore = gameEngine.score.playerScore,
                    aiScore = gameEngine.score.aiScore,
                    playerIncomeThisRound = pIncome,
                    aiIncomeThisRound = aIncome,
                    scoreHistory = newHistory,
                    lastRoundPlayerDelta = pIncome,
                    lastRoundAiDelta = aIncome
                )
            }

            // Track Intel Analyst / Bluff Caller for this round
            if (clashesWonThisRound > 0 && committedState.intel != null) {
                try {
                    if (committedState.intel.isTrue) {
                        preferencesManager.incrementIntelAnalystCount()
                        val count = preferencesManager.intelAnalystCount.first()
                        if (count >= 5) {
                            preferencesManager.unlockAchievement(Achievement.INTEL_ANALYST.id)
                        }
                    } else {
                        preferencesManager.incrementBluffCallerCount()
                        val count = preferencesManager.bluffCallerCount.first()
                        if (count >= 3) {
                            preferencesManager.unlockAchievement(Achievement.BLUFF_CALLER.id)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Show income summary & floating income text for 2 seconds
            delay(2000)

            // 5. Transition to next round (with transition banner) or GameOver
            val endRoundState = gameEngine.transitionToTransitionOrGameOver()

            // Check for Comeback Kid candidate on round 8 transition
            if (endRoundState.roundNumber == 8) {
                val diff = gameEngine.score.aiScore - gameEngine.score.playerScore
                if (diff >= 20) {
                    wasBehindAtRound8 = true
                }
            }

            if (endRoundState.phase == GamePhase.GameOver) {
                val isWin = gameEngine.score.playerScore > gameEngine.score.aiScore
                if (isWin) {
                    soundManager.playSound(GameSound.GAME_OVER_WIN)
                } else {
                    soundManager.playSound(GameSound.GAME_OVER_LOSE)
                }
                hapticFeedback.gameOver()

                // Record final tactical metrics inside persistence
                try {
                    val accuracyRate = if (roundsTracked > 0) intelTrueCount.toFloat() / roundsTracked else 0.7f
                    val isDaily = uiState.value.isDailyChallenge
                    val dailySeed = uiState.value.dailyChallengeSeed

                    recordGameStats(
                        playerScore = gameEngine.score.playerScore,
                        aiScore = gameEngine.score.aiScore,
                        roundsPlayed = endRoundState.roundNumber,
                        clashesWon = clashesWonCount,
                        clashesLost = clashesLostCount,
                        intelAccuracy = accuracyRate,
                        scoutUses = scoutUsesCount,
                        lockUses = lockUsesCount,
                        isDailyChallenge = isDaily,
                        dailyChallengeSeed = dailySeed
                    )

                    if (isDaily) {
                        val uid = auth.currentUser?.uid ?: ""
                        if (uid.isNotEmpty()) {
                            viewModelScope.launch {
                                try {
                                    val profile = profileRepository.getUserProfile(uid).first()
                                    val gameResult = when {
                                        gameEngine.score.playerScore > gameEngine.score.aiScore -> com.example.data.firestore.GameResult.WIN
                                        gameEngine.score.playerScore < gameEngine.score.aiScore -> com.example.data.firestore.GameResult.LOSS
                                        else -> com.example.data.firestore.GameResult.DRAW
                                    }
                                    val resultObj = com.example.data.model.DailyChallengeResult(
                                        uid = uid,
                                        date = dailyChallengeRepository.getTodayDateString(),
                                        playerScore = gameEngine.score.playerScore,
                                        aiScore = gameEngine.score.aiScore,
                                        result = gameResult,
                                        displayName = profile?.displayName ?: "Player",
                                        photoUrl = profile?.photoUrl
                                    )
                                    dailyChallengeRepository.submitResult(resultObj)

                                    if (profile != null) {
                                        val updatedProfile = profile.copy(
                                            stats = profile.stats.copy(
                                                lastDailyChallengeDate = dailyChallengeRepository.getTodayDateString()
                                            )
                                        )
                                        profileRepository.updateProfile(updatedProfile)
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }

                    // Increment persistent total games and wins
                    preferencesManager.incrementTotalGames()
                    if (isWin) {
                        preferencesManager.incrementTotalWins()
                        preferencesManager.unlockAchievement(Achievement.FIRST_WIN.id)
                    }

                    // Check achievements using values from PreferencesManager
                    val winsCount = preferencesManager.totalWins.first()
                    if (winsCount >= 10) {
                        preferencesManager.unlockAchievement(Achievement.STRATEGIST.id)
                    }

                    val gamesCount = preferencesManager.totalGames.first()
                    if (gamesCount >= 50) {
                        preferencesManager.unlockAchievement(Achievement.IRON_PLAYER.id)
                    }

                    val scoreDiff = gameEngine.score.playerScore - gameEngine.score.aiScore
                    if (isWin && scoreDiff >= 50) {
                        preferencesManager.unlockAchievement(Achievement.PERFECTIONIST.id)
                    }

                    // Lock master: win with heavy lock usage
                    if (isWin && lockUsesCount >= 18) { // Locked most rounds
                        preferencesManager.incrementLockMasterGamesCount()
                        val lockGames = preferencesManager.lockMasterGamesCount.first()
                        if (lockGames >= 5) {
                            preferencesManager.unlockAchievement(Achievement.LOCK_MASTER.id)
                        }
                    }

                    // Check for Comeback Kid
                    if (isWin && wasBehindAtRound8) {
                        preferencesManager.unlockAchievement(Achievement.COMEBACK_KID.id)
                    }

                    // Check for Speed Demon (win in under 6 minutes)
                    val durationMs = System.currentTimeMillis() - gameStartTime
                    if (isWin && durationMs < 6 * 60 * 1000) {
                        preferencesManager.unlockAchievement(Achievement.SPEED_DEMON.id)
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }

                _uiState.update {
                    it.copy(
                        roundState = endRoundState,
                        clashResults = emptyMap()
                    )
                }
            } else {
                // Show Round Transition Banner for the upcoming round automatically
                _uiState.update {
                    it.copy(
                        roundState = endRoundState,
                        showRoundTransitionBanner = true,
                        bannerRoundNumber = endRoundState.roundNumber
                    )
                }
            }
        }
    }

    fun onRoundBannerFinished() {
        val config = uiState.value.currentConfig
        var newRoundState = gameEngine.startNewRound()
        if (!config.intelEnabled && newRoundState.phase == GamePhase.Intel) {
            newRoundState = gameEngine.transitionToPlacing()
        }
        val firstAvailable = if (newRoundState.phase == GamePhase.Placing) 1 else null
        _uiState.update { state ->
            state.copy(
                showRoundTransitionBanner = false,
                roundState = newRoundState,
                selectedToken = firstAvailable,
                playerIncomeThisRound = 0,
                aiIncomeThisRound = 0,
                clashResults = emptyMap()
            )
        }
        soundManager.playSound(GameSound.ROUND_COMPLETE)
        hapticFeedback.roundComplete()
    }

    fun startNextRound() {
        if (uiState.value.roundState.phase == GamePhase.RoundTransition) {
            var newRoundState = gameEngine.startNewRound()
            if (!uiState.value.currentConfig.intelEnabled && newRoundState.phase == GamePhase.Intel) {
                newRoundState = gameEngine.transitionToPlacing()
            }
            val firstAvailable = if (newRoundState.phase == GamePhase.Placing) 1 else null
            _uiState.update { state ->
                state.copy(
                    showRoundTransitionBanner = false,
                    roundState = newRoundState,
                    selectedToken = firstAvailable,
                    playerIncomeThisRound = 0,
                    aiIncomeThisRound = 0,
                    clashResults = emptyMap()
                )
            }
        }
    }

    fun playAgain() {
        clashesWonCount = 0
        clashesLostCount = 0
        scoutUsesCount = 0
        lockUsesCount = 0
        intelTrueCount = 0
        roundsTracked = 0

        scoutedClearPositionThisRound = null
        wasBehindAtRound8 = false
        gameStartTime = System.currentTimeMillis()

        viewModelScope.launch {
            try {
                gameSaveRepository.deleteSave()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        _uiState.update {
            GameUiState(
                roundState = gameEngine.startNewGame(),
                playerScore = gameEngine.score.playerScore,
                aiScore = gameEngine.score.aiScore,
                intelAccuracyCount = 0,
                intelTotalCount = 0
            )
        }
    }

    fun startDailyChallenge() {
        clashesWonCount = 0
        clashesLostCount = 0
        scoutUsesCount = 0
        lockUsesCount = 0
        intelTrueCount = 0
        roundsTracked = 0

        scoutedClearPositionThisRound = null
        wasBehindAtRound8 = false
        gameStartTime = System.currentTimeMillis()

        viewModelScope.launch {
            try {
                gameSaveRepository.deleteSave()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val preset = preferencesManager.difficultyPreset.first()
            val seed = DailyChallenge.getCurrentSeed()
            val initialRound = gameEngine.startNewGame(preset = preset, seed = seed)

            _uiState.update {
                GameUiState(
                    roundState = initialRound,
                    playerScore = gameEngine.score.playerScore,
                    aiScore = gameEngine.score.aiScore,
                    isDailyChallenge = true,
                    dailyChallengeSeed = seed,
                    intelAccuracyCount = 0,
                    intelTotalCount = 0
                )
            }
        }
    }
}
