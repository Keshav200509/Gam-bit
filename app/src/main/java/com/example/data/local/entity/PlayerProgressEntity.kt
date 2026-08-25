package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.PlayerProgress

@Entity(tableName = "player_progress")
data class PlayerProgressEntity(
    @PrimaryKey val id: Int = 1,
    val arena1Level1Wins: Int,
    val arena1Level2Wins: Int,
    val arena1Level3Wins: Int,
    val arena2Level1Wins: Int,
    val arena2Level2Wins: Int,
    val arena2Level3Wins: Int,
    val arena3Level1Wins: Int,
    val arena3Level2Wins: Int,
    val arena3Level3Wins: Int,
    val bestScoresJson: String,
    val arena2Unlocked: Boolean,
    val arena3Unlocked: Boolean,
    val championUnlocked: Boolean
) {
    fun toDomain(): PlayerProgress {
        val scores = mutableMapOf<String, Int>()
        try {
            if (bestScoresJson.isNotEmpty()) {
                val json = org.json.JSONObject(bestScoresJson)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    scores[key] = json.getInt(key)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return PlayerProgress(
            arena1Level1Wins = arena1Level1Wins,
            arena1Level2Wins = arena1Level2Wins,
            arena1Level3Wins = arena1Level3Wins,
            arena2Level1Wins = arena2Level1Wins,
            arena2Level2Wins = arena2Level2Wins,
            arena2Level3Wins = arena2Level3Wins,
            arena3Level1Wins = arena3Level1Wins,
            arena3Level2Wins = arena3Level2Wins,
            arena3Level3Wins = arena3Level3Wins,
            bestScores = scores,
            arena2Unlocked = arena2Unlocked,
            arena3Unlocked = arena3Unlocked,
            championUnlocked = championUnlocked
        )
    }

    companion object {
        fun fromDomain(progress: PlayerProgress): PlayerProgressEntity {
            val json = org.json.JSONObject()
            for ((key, value) in progress.bestScores) {
                json.put(key, value)
            }
            return PlayerProgressEntity(
                id = 1,
                arena1Level1Wins = progress.arena1Level1Wins,
                arena1Level2Wins = progress.arena1Level2Wins,
                arena1Level3Wins = progress.arena1Level3Wins,
                arena2Level1Wins = progress.arena2Level1Wins,
                arena2Level2Wins = progress.arena2Level2Wins,
                arena2Level3Wins = progress.arena2Level3Wins,
                arena3Level1Wins = progress.arena3Level1Wins,
                arena3Level2Wins = progress.arena3Level2Wins,
                arena3Level3Wins = progress.arena3Level3Wins,
                bestScoresJson = json.toString(),
                arena2Unlocked = progress.arena2Unlocked,
                arena3Unlocked = progress.arena3Unlocked,
                championUnlocked = progress.championUnlocked
            )
        }
    }
}
