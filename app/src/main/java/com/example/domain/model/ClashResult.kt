package com.example.domain.model

sealed class ClashResult {
    data class PlayerWins(val playerStrength: Int, val aiStrength: Int) : ClashResult()
    data class AIWins(val playerStrength: Int, val aiStrength: Int) : ClashResult()
    data object Tie : ClashResult()
    data object PlayerUncontested : ClashResult()
    data object AIUncontested : ClashResult()
    data object PlayerDefends : ClashResult()
    data object AIDefends : ClashResult()
}
