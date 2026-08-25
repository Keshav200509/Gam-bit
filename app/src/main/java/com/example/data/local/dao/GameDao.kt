package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.GameEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameEntity)

    @Query("SELECT * FROM games ORDER BY timestamp DESC")
    fun getAllGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentGames(limit: Int): Flow<List<GameEntity>>

    @Query("DELETE FROM games")
    suspend fun clearAllGames()
}
