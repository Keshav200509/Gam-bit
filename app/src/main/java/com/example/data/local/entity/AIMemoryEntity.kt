package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_memory")
data class AIMemoryEntity(
    @PrimaryKey val id: Int = 1,
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
)
