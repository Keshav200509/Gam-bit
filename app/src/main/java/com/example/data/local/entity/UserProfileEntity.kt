package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.UserProfile
import com.example.data.model.UserStats
import com.example.data.model.ArenaProgress
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val uid: String,
    val displayName: String,
    val photoUrl: String?,
    val friendCode: String,
    val statsJson: String,
    val arenaProgressJson: String,
    val achievementsJson: String,
    val createdAt: Long,
    val lastActive: Long
) {
    fun toDomain(): UserProfile {
        val statsObj = try {
            val json = JSONObject(statsJson)
            UserStats(
                totalGames = json.optInt("totalGames", 0),
                totalWins = json.optInt("totalWins", 0),
                totalLosses = json.optInt("totalLosses", 0),
                currentStreak = json.optInt("currentStreak", 0),
                bestStreak = json.optInt("bestStreak", 0),
                pvpGames = json.optInt("pvpGames", 0),
                pvpWins = json.optInt("pvpWins", 0),
                pvpRating = json.optInt("pvpRating", 1000),
                dailyChallengeStreak = json.optInt("dailyChallengeStreak", 0),
                lastDailyChallengeDate = json.optString("lastDailyChallengeDate", null).takeIf { it != "null" && it.isNotEmpty() }
            )
        } catch (e: Exception) {
            UserStats()
        }

        val progressObj = try {
            val json = JSONObject(arenaProgressJson)
            val bestScoresMap = mutableMapOf<String, Int>()
            val bestScoresJson = json.optJSONObject("bestScores")
            if (bestScoresJson != null) {
                val keys = bestScoresJson.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    bestScoresMap[key] = bestScoresJson.getInt(key)
                }
            }
            ArenaProgress(
                arena1Level1Wins = json.optInt("arena1Level1Wins", 0),
                arena1Level2Wins = json.optInt("arena1Level2Wins", 0),
                arena1Level3Wins = json.optInt("arena1Level3Wins", 0),
                arena2Level1Wins = json.optInt("arena2Level1Wins", 0),
                arena2Level2Wins = json.optInt("arena2Level2Wins", 0),
                arena2Level3Wins = json.optInt("arena2Level3Wins", 0),
                arena3Level1Wins = json.optInt("arena3Level1Wins", 0),
                arena3Level2Wins = json.optInt("arena3Level2Wins", 0),
                arena3Level3Wins = json.optInt("arena3Level3Wins", 0),
                arena2Unlocked = json.optBoolean("arena2Unlocked", false),
                arena3Unlocked = json.optBoolean("arena3Unlocked", false),
                championUnlocked = json.optBoolean("championUnlocked", false),
                bestScores = bestScoresMap
            )
        } catch (e: Exception) {
            ArenaProgress()
        }

        val achievementsList = try {
            val array = JSONArray(achievementsJson)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }

        return UserProfile(
            uid = uid,
            displayName = displayName,
            photoUrl = photoUrl,
            friendCode = friendCode,
            stats = statsObj,
            arenaProgress = progressObj,
            achievements = achievementsList,
            createdAt = createdAt,
            lastActive = lastActive
        )
    }

    companion object {
        fun fromDomain(profile: UserProfile): UserProfileEntity {
            val statsJsonObj = JSONObject().apply {
                put("totalGames", profile.stats.totalGames)
                put("totalWins", profile.stats.totalWins)
                put("totalLosses", profile.stats.totalLosses)
                put("currentStreak", profile.stats.currentStreak)
                put("bestStreak", profile.stats.bestStreak)
                put("pvpGames", profile.stats.pvpGames)
                put("pvpWins", profile.stats.pvpWins)
                put("pvpRating", profile.stats.pvpRating)
                put("dailyChallengeStreak", profile.stats.dailyChallengeStreak)
                put("lastDailyChallengeDate", profile.stats.lastDailyChallengeDate)
            }

            val bestScoresJsonObj = JSONObject().apply {
                profile.arenaProgress.bestScores.forEach { (k, v) ->
                    put(k, v)
                }
            }

            val progressJsonObj = JSONObject().apply {
                put("arena1Level1Wins", profile.arenaProgress.arena1Level1Wins)
                put("arena1Level2Wins", profile.arenaProgress.arena1Level2Wins)
                put("arena1Level3Wins", profile.arenaProgress.arena1Level3Wins)
                put("arena2Level1Wins", profile.arenaProgress.arena2Level1Wins)
                put("arena2Level2Wins", profile.arenaProgress.arena2Level2Wins)
                put("arena2Level3Wins", profile.arenaProgress.arena2Level3Wins)
                put("arena3Level1Wins", profile.arenaProgress.arena3Level1Wins)
                put("arena3Level2Wins", profile.arenaProgress.arena3Level2Wins)
                put("arena3Level3Wins", profile.arenaProgress.arena3Level3Wins)
                put("arena2Unlocked", profile.arenaProgress.arena2Unlocked)
                put("arena3Unlocked", profile.arenaProgress.arena3Unlocked)
                put("championUnlocked", profile.arenaProgress.championUnlocked)
                put("bestScores", bestScoresJsonObj)
            }

            val achievementsArray = JSONArray().apply {
                profile.achievements.forEach { put(it) }
            }

            return UserProfileEntity(
                uid = profile.uid,
                displayName = profile.displayName,
                photoUrl = profile.photoUrl,
                friendCode = profile.friendCode,
                statsJson = statsJsonObj.toString(),
                arenaProgressJson = progressJsonObj.toString(),
                achievementsJson = achievementsArray.toString(),
                createdAt = profile.createdAt,
                lastActive = profile.lastActive
            )
        }
    }
}
