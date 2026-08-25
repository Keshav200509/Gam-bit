package com.example.domain.model

data class ProbabilityBreakdown(
    val win: Int,
    val lose: Int,
    val tie: Int,
    val uncontested: Int
)
