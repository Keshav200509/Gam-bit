package com.example.domain.usecase

import com.example.data.model.DailyChallenge
import com.example.domain.model.Arena
import com.example.domain.model.GameLevel
import javax.inject.Inject

class GenerateDailyChallenge @Inject constructor() {
    operator fun invoke(dateStr: String): DailyChallenge {
        val boardSeed = dateStr.hashCode().toLong()
        val aiSeed = (dateStr + "ai").hashCode().toLong()
        
        val arenas = Arena.entries
        val arenaIndex = (dateStr.hashCode() % arenas.size).let { if (it < 0) it + arenas.size else it }
        val arena = arenas[arenaIndex]
        
        return DailyChallenge(
            date = dateStr,
            boardSeed = boardSeed,
            aiSeed = aiSeed,
            arena = arena,
            level = GameLevel.LEVEL_2
        )
    }
}
