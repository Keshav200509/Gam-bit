package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.local.dao.GameDao
import com.example.data.local.dao.GameSaveDao
import com.example.data.local.dao.PlayerProgressDao
import com.example.data.local.dao.AIMemoryDao
import com.example.data.local.entity.AIMemoryEntity
import com.example.data.local.entity.GameEntity
import com.example.data.local.entity.GameSaveEntity
import com.example.data.local.entity.PlayerProgressEntity

import com.example.data.local.dao.UserProfileDao
import com.example.data.local.entity.UserProfileEntity

@Database(entities = [GameEntity::class, GameSaveEntity::class, PlayerProgressEntity::class, AIMemoryEntity::class, UserProfileEntity::class], version = 4, exportSchema = false)
abstract class GambitDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun gameSaveDao(): GameSaveDao
    abstract fun playerProgressDao(): PlayerProgressDao
    abstract fun aiMemoryDao(): AIMemoryDao
    abstract fun userProfileDao(): UserProfileDao
}
