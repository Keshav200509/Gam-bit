package com.example.domain.ai

import com.example.domain.model.Position
import com.example.domain.model.Token

/**
 * AIPlan represents the pre-committed move of the AI opponent for a round.
 * It strictly maintains domain-layer purity with zero Android or Jetpack Compose imports.
 */
data class AIPlan(
    val placements: Map<Position, Token>,
    val locks: List<Position>,
    val strategy: AIStrategy
)
