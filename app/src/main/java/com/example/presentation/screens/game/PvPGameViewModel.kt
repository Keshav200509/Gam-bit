package com.example.presentation.screens.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.realtime.PvPMatchRepository
import com.example.domain.model.Board
import com.example.domain.model.Position
import com.example.domain.model.ScoutResult
import com.example.presentation.SoundManager
import com.example.presentation.GambitHapticFeedback
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PvPGameUiState(
    val isLoading: Boolean = false,
    val match: PvPMatch? = null,
    val myUid: String = "",
    val myRole: String = "", // "player1" or "player2"
    val error: String? = null,
    val selectedToken: Int? = null,
    val scoutMode: Boolean = false,
    val lockMode: Boolean = false,
    val myScouts: Map<String, String> = emptyMap(), // "row,col" -> "YES" / "NO"
    val opponentPresence: Boolean = false
)

@HiltViewModel
class PvPGameViewModel @Inject constructor(
    private val matchRepository: PvPMatchRepository,
    private val auth: FirebaseAuth,
    val soundManager: SoundManager,
    val hapticFeedback: GambitHapticFeedback
) : ViewModel() {

    private val _uiState = MutableStateFlow(PvPGameUiState())
    val uiState: StateFlow<PvPGameUiState> = _uiState.asStateFlow()

    private var matchId: String? = null
    private var observationJob: kotlinx.coroutines.Job? = null
    private var presenceJob: kotlinx.coroutines.Job? = null

    fun startMatchObservation(id: String) {
        matchId = id
        _uiState.update { it.copy(isLoading = true) }
        val currentUid = auth.currentUser?.uid ?: ""

        observationJob?.cancel()
        observationJob = viewModelScope.launch {
            matchRepository.observeMatch(id)
                .collect { match ->
                    if (match != null) {
                        val myRole = when (currentUid) {
                            match.player1.uid -> "player1"
                            match.player2.uid -> "player2"
                            else -> ""
                        }

                        // Extract my scouts
                        val scoutsNode = match.scouts[currentUid] ?: emptyMap()

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                match = match,
                                myUid = currentUid,
                                myRole = myRole,
                                myScouts = scoutsNode
                            )
                        }

                        // Auto-trigger clash resolution if BOTH players are ready
                        // and we are in PLACEMENT phase and both have placed 5 tokens
                        if (match.roundPhase == RoundPhase.PLACEMENT &&
                            match.player1.isReady && match.player2.isReady &&
                            match.player1Placements.size == 5 && match.player2Placements.size == 5
                        ) {
                            // Run the atomic resolution
                            resolvePendingRound(id)
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false, error = "Match not found or deleted") }
                    }
                }
        }

        // Start presence tracking
        presenceJob?.cancel()
        presenceJob = viewModelScope.launch {
            while (true) {
                matchRepository.updatePresence(id, currentUid)
                val state = _uiState.value
                val opponentUid = if (state.myRole == "player1") state.match?.player2?.uid else state.match?.player1?.uid
                if (opponentUid != null) {
                    val active = matchRepository.checkOpponentPresence(id, opponentUid)
                    _uiState.update { it.copy(opponentPresence = active) }
                }
                kotlinx.coroutines.delay(5000) // check every 5s
            }
        }
    }

    fun selectToken(token: Int?) {
        _uiState.update { it.copy(selectedToken = token, scoutMode = false, lockMode = false) }
    }

    fun toggleScoutMode() {
        _uiState.update { it.copy(scoutMode = !it.scoutMode, selectedToken = null, lockMode = false) }
    }

    fun toggleLockMode() {
        _uiState.update { it.copy(lockMode = !it.lockMode, selectedToken = null, scoutMode = false) }
    }

    fun handleCellClick(row: Int, col: Int) {
        val state = _uiState.value
        val match = state.match ?: return
        val posStr = "$row,$col"

        if (state.myRole.isEmpty()) return

        // 1. Scout Mode action
        if (state.scoutMode) {
            if (state.myScouts.size >= 1) {
                _uiState.update { it.copy(error = "Scout budget exhausted (max 1 per round)") }
                return
            }
            performScout(row, col)
            _uiState.update { it.copy(scoutMode = false) }
            return
        }

        // 2. Lock Mode action
        if (state.lockMode) {
            toggleLock(row, col)
            _uiState.update { it.copy(lockMode = false) }
            return
        }

        // 3. Placement Action
        val selectedToken = state.selectedToken
        if (selectedToken != null) {
            // Place token
            placeToken(row, col, selectedToken)
            _uiState.update { it.copy(selectedToken = null) }
        } else {
            // Remove existing token if owned by me
            val placements = if (state.myRole == "player1") match.player1Placements else match.player2Placements
            if (placements.containsKey(posStr)) {
                removeToken(row, col)
            }
        }
    }

    private fun placeToken(row: Int, col: Int, tokenVal: Int) {
        val mId = matchId ?: return
        val state = _uiState.value
        val match = state.match ?: return
        val posStr = "$row,$col"

        val currentPlacements = if (state.myRole == "player1") {
            match.player1Placements.toMutableMap()
        } else {
            match.player2Placements.toMutableMap()
        }

        // Check if token value is already used
        if (currentPlacements.values.contains(tokenVal)) {
            // Remove it from its previous position
            val previousPos = currentPlacements.filterValues { it == tokenVal }.keys.firstOrNull()
            if (previousPos != null) {
                currentPlacements.remove(previousPos)
            }
        }

        currentPlacements[posStr] = tokenVal

        viewModelScope.launch {
            val locks = if (state.myRole == "player1") match.player1Locks else match.player2Locks
            matchRepository.submitPlacements(mId, state.myUid, currentPlacements, locks)
            soundManager.playSound(com.example.presentation.GameSound.TOKEN_PLACE)
            hapticFeedback.tokenPlaced()
        }
    }

    private fun removeToken(row: Int, col: Int) {
        val mId = matchId ?: return
        val state = _uiState.value
        val match = state.match ?: return
        val posStr = "$row,$col"

        val currentPlacements = if (state.myRole == "player1") {
            match.player1Placements.toMutableMap()
        } else {
            match.player2Placements.toMutableMap()
        }

        if (currentPlacements.containsKey(posStr)) {
            currentPlacements.remove(posStr)
            val currentLocks = if (state.myRole == "player1") {
                match.player1Locks.filter { it != posStr }
            } else {
                match.player2Locks.filter { it != posStr }
            }

            viewModelScope.launch {
                matchRepository.submitPlacements(mId, state.myUid, currentPlacements, currentLocks)
                soundManager.playSound(com.example.presentation.GameSound.TOKEN_PLACE)
                hapticFeedback.tokenPlaced()
            }
        }
    }

    private fun toggleLock(row: Int, col: Int) {
        val mId = matchId ?: return
        val state = _uiState.value
        val match = state.match ?: return
        val posStr = "$row,$col"

        val placements = if (state.myRole == "player1") match.player1Placements else match.player2Placements
        if (!placements.containsKey(posStr)) {
            _uiState.update { it.copy(error = "You can only lock cells where you placed a token") }
            return
        }

        val currentLocks = if (state.myRole == "player1") {
            match.player1Locks.toMutableList()
        } else {
            match.player2Locks.toMutableList()
        }

        if (currentLocks.contains(posStr)) {
            currentLocks.remove(posStr)
        } else {
            if (currentLocks.size >= 2) {
                _uiState.update { it.copy(error = "Lock budget exhausted (max 2 per round)") }
                return
            }
            currentLocks.add(posStr)
        }

        viewModelScope.launch {
            matchRepository.submitPlacements(mId, state.myUid, placements, currentLocks)
            soundManager.playSound(com.example.presentation.GameSound.LOCK)
            hapticFeedback.lockToggled()
        }
    }

    private fun performScout(row: Int, col: Int) {
        val mId = matchId ?: return
        val state = _uiState.value
        val match = state.match ?: return
        val posStr = "$row,$col"

        val opponentPlacements = if (state.myRole == "player1") match.player2Placements else match.player1Placements
        val hasToken = opponentPlacements.containsKey(posStr)
        val result = if (hasToken) ScoutResult.CONTESTED else ScoutResult.CLEAR

        viewModelScope.launch {
            matchRepository.submitScoutResult(mId, state.myUid, posStr, result)
            soundManager.playSound(com.example.presentation.GameSound.SCOUT)
            hapticFeedback.scoutUsed()
        }
    }

    fun commitReady() {
        val mId = matchId ?: return
        val state = _uiState.value
        val match = state.match ?: return

        val placements = if (state.myRole == "player1") match.player1Placements else match.player2Placements
        if (placements.size < 5) {
            _uiState.update { it.copy(error = "You must place all 5 tokens before committing") }
            return
        }

        viewModelScope.launch {
            matchRepository.setPlayerReady(mId, state.myUid, true)
            soundManager.playSound(com.example.presentation.GameSound.LOCK)
            hapticFeedback.lockToggled()
        }
    }

    private fun resolvePendingRound(id: String) {
        viewModelScope.launch {
            matchRepository.resolveRound(id)
            soundManager.playSound(com.example.presentation.GameSound.ROUND_COMPLETE)
        }
    }

    fun advanceRound() {
        val mId = matchId ?: return
        viewModelScope.launch {
            matchRepository.advanceRound(mId)
            soundManager.playSound(com.example.presentation.GameSound.TOKEN_PLACE)
        }
    }

    fun abandonMatch() {
        val mId = matchId ?: return
        val state = _uiState.value
        viewModelScope.launch {
            matchRepository.abandonMatch(mId, state.myUid)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        observationJob?.cancel()
        presenceJob?.cancel()
    }
}
