package com.example.domain

data class NewGamePlusConfig(
    val aiCapabilityFixed: Int = 10, // Always PEAK
    val aiHasMemory: Boolean = true, // Models player across games
    val intelReliability: String = "SUSPECT", // Always worst
    val maxLocksPerRound: Int = 1,
    val appliesToAllArenas: Boolean = true
)
