package com.example.domain.model

sealed interface GamePhase {
    object Intel : GamePhase
    object Placing : GamePhase
    object Revealing : GamePhase
    object Resolving : GamePhase
    object IncomeSummary : GamePhase
    object RoundTransition : GamePhase
    object GameOver : GamePhase
}
