package com.example.domain.ai

import com.example.data.local.dao.AIMemoryDao
import com.example.data.local.entity.AIMemoryEntity
import com.example.domain.model.Position
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIMemorySystem @Inject constructor(
    private val aiMemoryDao: AIMemoryDao
) {
    val memoryFlow: Flow<AIMemory> = aiMemoryDao.getAIMemory().map { entity ->
        entity?.toDomain() ?: AIMemory()
    }

    suspend fun getMemoryOnce(): AIMemory {
        return memoryFlow.first()
    }

    suspend fun recordGame(
        placementTimes: List<Long>,
        placements: List<Position>,
        lockUses: Int,
        scoutUses: Int
    ) {
        val current = getMemoryOnce()
        val newCount = current.totalGamesTracked + 1

        val totalTime = current.averagePlacementTimeMs * current.totalPlacementsCount + placementTimes.sum()
        val newPlacementsCount = current.totalPlacementsCount + placementTimes.size
        val newAverageTime = if (newPlacementsCount > 0) totalTime / newPlacementsCount else 0L

        var ne = current.northEastPlacements
        var se = current.southEastPlacements
        var c = current.centralPlacements
        var nw = current.northWestPlacements
        var sw = current.southWestPlacements
        var n = current.northPlacements
        var s = current.southPlacements
        var e = current.eastPlacements
        var w = current.westPlacements

        for (pos in placements) {
            when (getCellRegion(pos.row, pos.col)) {
                "northeast" -> ne++
                "southeast" -> se++
                "center" -> c++
                "northwest" -> nw++
                "southwest" -> sw++
                "north" -> n++
                "south" -> s++
                "east" -> e++
                "west" -> w++
            }
        }

        val updated = current.copy(
            averagePlacementTimeMs = newAverageTime,
            totalPlacementsCount = newPlacementsCount,
            northEastPlacements = ne,
            southEastPlacements = se,
            centralPlacements = c,
            northWestPlacements = nw,
            southWestPlacements = sw,
            northPlacements = n,
            southPlacements = s,
            eastPlacements = e,
            westPlacements = w,
            totalLockUses = current.totalLockUses + lockUses,
            totalScoutUses = current.totalScoutUses + scoutUses,
            totalGamesTracked = newCount
        )

        aiMemoryDao.save(updated.toEntity())
    }

    suspend fun resetMemory() {
        aiMemoryDao.reset()
    }

    private fun getCellRegion(row: Int, col: Int): String {
        return when {
            row == 2 && col == 2 -> "center"
            row < 2 && col < 2 -> "northwest"
            row < 2 && col > 2 -> "northeast"
            row > 2 && col < 2 -> "southwest"
            row > 2 && col > 2 -> "southeast"
            row < 2 -> "north"
            row > 2 -> "south"
            col < 2 -> "west"
            else -> "east"
        }
    }
}

data class AIMemory(
    val averagePlacementTimeMs: Long = 0L,
    val totalPlacementsCount: Int = 0,
    val northEastPlacements: Int = 0,
    val southEastPlacements: Int = 0,
    val centralPlacements: Int = 0,
    val northWestPlacements: Int = 0,
    val southWestPlacements: Int = 0,
    val northPlacements: Int = 0,
    val southPlacements: Int = 0,
    val eastPlacements: Int = 0,
    val westPlacements: Int = 0,
    val totalLockUses: Int = 0,
    val totalScoutUses: Int = 0,
    val totalGamesTracked: Int = 0
) {
    fun getPreferredRegion(): String? {
        val regionMap = mapOf(
            "northeast" to northEastPlacements,
            "southeast" to southEastPlacements,
            "center" to centralPlacements,
            "northwest" to northWestPlacements,
            "southwest" to southWestPlacements,
            "north" to northPlacements,
            "south" to southPlacements,
            "east" to eastPlacements,
            "west" to westPlacements
        )
        val maxVal = regionMap.values.maxOrNull() ?: 0
        if (maxVal == 0) return null
        return regionMap.entries.firstOrNull { it.value == maxVal }?.key
    }

    fun toEntity(): AIMemoryEntity = AIMemoryEntity(
        id = 1,
        averagePlacementTimeMs = averagePlacementTimeMs,
        totalPlacementsCount = totalPlacementsCount,
        northEastPlacements = northEastPlacements,
        southEastPlacements = southEastPlacements,
        centralPlacements = centralPlacements,
        northWestPlacements = northWestPlacements,
        southWestPlacements = southWestPlacements,
        northPlacements = northPlacements,
        southPlacements = southPlacements,
        eastPlacements = eastPlacements,
        westPlacements = westPlacements,
        totalLockUses = totalLockUses,
        totalScoutUses = totalScoutUses,
        totalGamesTracked = totalGamesTracked
    )
}

fun AIMemoryEntity.toDomain(): AIMemory = AIMemory(
    averagePlacementTimeMs = averagePlacementTimeMs,
    totalPlacementsCount = totalPlacementsCount,
    northEastPlacements = northEastPlacements,
    southEastPlacements = southEastPlacements,
    centralPlacements = centralPlacements,
    northWestPlacements = northWestPlacements,
    southWestPlacements = southWestPlacements,
    northPlacements = northPlacements,
    southPlacements = southPlacements,
    eastPlacements = eastPlacements,
    westPlacements = westPlacements,
    totalLockUses = totalLockUses,
    totalScoutUses = totalScoutUses,
    totalGamesTracked = totalGamesTracked
)
