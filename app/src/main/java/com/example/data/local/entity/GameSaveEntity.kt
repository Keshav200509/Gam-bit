package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_saves")
data class GameSaveEntity(
    @PrimaryKey val id: Int = 1,
    val roundStateJson: String,
    val timestamp: Long = System.currentTimeMillis()
)
