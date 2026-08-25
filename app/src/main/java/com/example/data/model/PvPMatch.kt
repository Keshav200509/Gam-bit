package com.example.data.model

import com.example.domain.model.Arena
import com.example.domain.model.Board
import com.example.domain.model.GameLevel

data class PvPMatch(
    val matchId: String = "",
    val player1: MatchPlayer = MatchPlayer(),
    val player2: MatchPlayer = MatchPlayer(),
    val arena: Arena = Arena.ASCENDENCY,
    val level: GameLevel = GameLevel.LEVEL_1,
    val board: Board = Board(),  // 5x5 grid
    val currentRound: Int = 1,
    val matchPhase: MatchPhase = MatchPhase.WAITING,
    val roundPhase: RoundPhase = RoundPhase.PLACEMENT,
    val player1Placements: Map<String, Int> = emptyMap(),  // "row,col" -> token
    val player2Placements: Map<String, Int> = emptyMap(),
    val player1Locks: List<String> = emptyList(),
    val player2Locks: List<String> = emptyList(),
    val player1Score: Int = 0,
    val player2Score: Int = 0,
    val roundResults: List<RoundResult> = emptyList(),
    val winnerUid: String? = null,
    val scouts: Map<String, Map<String, String>> = emptyMap(),  // uid -> "row,col" -> "YES"/"NO"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class MatchPlayer(
    val uid: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val isReady: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis()
)

enum class MatchPhase { WAITING, IN_PROGRESS, COMPLETED, ABANDONED }
enum class RoundPhase { 
    PLACEMENT,    // Both players placing tokens
    REVEAL,       // Both committed, revealing
    RESOLUTION,   // Resolving clashes
    INCOME,       // Showing income summary
    TRANSITION    // Transitioning to next round
}

data class RoundResult(
    val roundNumber: Int = 1,
    val player1Placements: Map<String, Int> = emptyMap(),
    val player2Placements: Map<String, Int> = emptyMap(),
    val clashes: List<ClashOutcome> = emptyList(),
    val player1Income: Int = 0,
    val player2Income: Int = 0
)

data class ClashOutcome(
    val position: String = "",  // "row,col"
    val player1Token: Int? = null,
    val player2Token: Int? = null,
    val winner: String? = null,  // "player1", "player2", or null (tie)
    val player1Strength: Int = 0,
    val player2Strength: Int = 0
)
