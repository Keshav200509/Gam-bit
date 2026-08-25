package com.example.domain.ai

/**
 * AIMove combines the AI pre-committed plan and the dynamic capability level for the round.
 * It strictly maintains domain-layer purity with zero Android or Jetpack Compose imports.
 */
data class AIMove(
    val plan: AIPlan,
    val capability: AICapability
)
