package com.example.data.model

import com.example.domain.model.Arena
import com.example.domain.model.GameLevel
import com.example.data.firestore.GameResult

data class DailyChallenge(
    val date: String,  // "YYYY-MM-DD"
    val boardSeed: Long,
    val aiSeed: Long,
    val arena: Arena,
    val level: GameLevel,
    val createdAt: Long = System.currentTimeMillis()
)

data class DailyChallengeResult(
    val uid: String,
    val date: String,
    val playerScore: Int,
    val aiScore: Int,
    val result: GameResult,
    val rank: Int? = null,  // rank on leaderboard
    val completedAt: Long = System.currentTimeMillis(),
    val displayName: String = "",
    val photoUrl: String? = null
)
