package com.example.domain.model

data class RoundState(
    val roundNumber: Int = 1,
    val phase: GamePhase = GamePhase.Intel,
    val board: Board = Board(),
    val playerPlacements: Map<Position, Token> = emptyMap(),
    val aiPlacements: Map<Position, Token> = emptyMap(),
    val playerLocks: List<Position> = emptyList(),
    val aiLocks: List<Position> = emptyList(),
    val scoutUsed: Boolean = false,
    val scoutedPosition: Position? = null,
    val scoutResult: ScoutResult? = null,
    val intelHint: String = "",
    val intelConfidence: String = "",
    val aiCapabilityName: String = "STEADY",
    val aiCapabilityModifier: Int = 0,
    val intel: IntelHint? = null,
    val incomeMultiplier: Int = 1,
    val lateGameMessage: String = "",
    val lateGameEventType: LateGameEventType = LateGameEventType.NONE
)
