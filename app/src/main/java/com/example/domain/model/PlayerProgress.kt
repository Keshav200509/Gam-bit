package com.example.domain.model

data class PlayerProgress(
    val arena1Level1Wins: Int = 0,
    val arena1Level2Wins: Int = 0,
    val arena1Level3Wins: Int = 0,
    val arena2Level1Wins: Int = 0,
    val arena2Level2Wins: Int = 0,
    val arena2Level3Wins: Int = 0,
    val arena3Level1Wins: Int = 0,
    val arena3Level2Wins: Int = 0,
    val arena3Level3Wins: Int = 0,
    val bestScores: Map<String, Int> = emptyMap(), // key: "arena_level"
    val arena2Unlocked: Boolean = false,
    val arena3Unlocked: Boolean = false,
    val championUnlocked: Boolean = false
) {
    fun getWins(arena: Arena, level: GameLevel): Int {
        return when (arena) {
            Arena.ASCENDENCY -> when (level) {
                GameLevel.LEVEL_1 -> arena1Level1Wins
                GameLevel.LEVEL_2 -> arena1Level2Wins
                GameLevel.LEVEL_3 -> arena1Level3Wins
            }
            Arena.CONFRONTATION -> when (level) {
                GameLevel.LEVEL_1 -> arena2Level1Wins
                GameLevel.LEVEL_2 -> arena2Level2Wins
                GameLevel.LEVEL_3 -> arena2Level3Wins
            }
            Arena.OBLIVION -> when (level) {
                GameLevel.LEVEL_1 -> arena3Level1Wins
                GameLevel.LEVEL_2 -> arena3Level2Wins
                GameLevel.LEVEL_3 -> arena3Level3Wins
            }
        }
    }

    fun isArenaUnlocked(arena: Arena): Boolean {
        return when (arena) {
            Arena.ASCENDENCY -> true
            Arena.CONFRONTATION -> arena2Unlocked || arena1Level3Wins >= 1
            Arena.OBLIVION -> arena3Unlocked || arena2Level3Wins >= 1
        }
    }

    fun isUnlocked(arena: Arena, level: GameLevel): Boolean {
        return when (arena) {
            Arena.ASCENDENCY -> {
                when (level) {
                    GameLevel.LEVEL_1 -> true
                    GameLevel.LEVEL_2 -> arena1Level1Wins >= WINS_TO_ADVANCE
                    GameLevel.LEVEL_3 -> arena1Level2Wins >= WINS_TO_ADVANCE
                }
            }
            Arena.CONFRONTATION -> {
                if (!isArenaUnlocked(Arena.CONFRONTATION)) return false
                when (level) {
                    GameLevel.LEVEL_1 -> true
                    GameLevel.LEVEL_2 -> arena2Level1Wins >= WINS_TO_ADVANCE
                    GameLevel.LEVEL_3 -> arena2Level2Wins >= WINS_TO_ADVANCE
                }
            }
            Arena.OBLIVION -> {
                if (!isArenaUnlocked(Arena.OBLIVION)) return false
                when (level) {
                    GameLevel.LEVEL_1 -> true
                    GameLevel.LEVEL_2 -> arena3Level1Wins >= WINS_TO_ADVANCE
                    GameLevel.LEVEL_3 -> arena3Level2Wins >= WINS_TO_ADVANCE
                }
            }
        }
    }

    fun recordWin(arena: Arena, level: GameLevel, score: Int): PlayerProgress {
        var a1l1 = arena1Level1Wins
        var a1l2 = arena1Level2Wins
        var a1l3 = arena1Level3Wins
        var a2l1 = arena2Level1Wins
        var a2l2 = arena2Level2Wins
        var a2l3 = arena2Level3Wins
        var a3l1 = arena3Level1Wins
        var a3l2 = arena3Level2Wins
        var a3l3 = arena3Level3Wins

        when (arena) {
            Arena.ASCENDENCY -> {
                when (level) {
                    GameLevel.LEVEL_1 -> a1l1++
                    GameLevel.LEVEL_2 -> a1l2++
                    GameLevel.LEVEL_3 -> a1l3++
                }
            }
            Arena.CONFRONTATION -> {
                when (level) {
                    GameLevel.LEVEL_1 -> a2l1++
                    GameLevel.LEVEL_2 -> a2l2++
                    GameLevel.LEVEL_3 -> a2l3++
                }
            }
            Arena.OBLIVION -> {
                when (level) {
                    GameLevel.LEVEL_1 -> a3l1++
                    GameLevel.LEVEL_2 -> a3l2++
                    GameLevel.LEVEL_3 -> a3l3++
                }
            }
        }

        val key = "${arena.name.lowercase()}_level${level.level}"
        val currentBest = bestScores[key] ?: 0
        val newBestScores = bestScores.toMutableMap().apply {
            if (score > currentBest) {
                put(key, score)
            }
        }

        val a2UnlockedNew = arena2Unlocked || (a1l3 >= 1)
        val a3UnlockedNew = arena3Unlocked || (a2l3 >= 1)
        val championUnlNew = championUnlocked || (
            a1l1 > 0 && a1l2 > 0 && a1l3 > 0 &&
            a2l1 > 0 && a2l2 > 0 && a2l3 > 0 &&
            a3l1 > 0 && a3l2 > 0 && a3l3 > 0
        )

        return copy(
            arena1Level1Wins = a1l1,
            arena1Level2Wins = a1l2,
            arena1Level3Wins = a1l3,
            arena2Level1Wins = a2l1,
            arena2Level2Wins = a2l2,
            arena2Level3Wins = a2l3,
            arena3Level1Wins = a3l1,
            arena3Level2Wins = a3l2,
            arena3Level3Wins = a3l3,
            bestScores = newBestScores,
            arena2Unlocked = a2UnlockedNew,
            arena3Unlocked = a3UnlockedNew,
            championUnlocked = championUnlNew
        )
    }

    companion object {
        const val WINS_TO_ADVANCE = 2
    }
}
