package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val playerScore: Int,
    val aiScore: Int,
    val result: String, // "WIN", "LOSE", "DRAW"
    val roundsPlayed: Int,
    val clashesWon: Int,
    val clashesLost: Int,
    val intelAccuracy: Float,
    val scoutUses: Int,
    val lockUses: Int,
    val isDailyChallenge: Boolean = false,
    val dailyChallengeSeed: Long = 0L
)
