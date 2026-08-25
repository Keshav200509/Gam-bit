package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.PlayerProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerProgressDao {
    @Query("SELECT * FROM player_progress WHERE id = 1 LIMIT 1")
    fun getProgress(): Flow<PlayerProgressEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(progress: PlayerProgressEntity)

    @Query("DELETE FROM player_progress")
    suspend fun reset()
}
