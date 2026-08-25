package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.GameSaveEntity

@Dao
interface GameSaveDao {
    @Query("SELECT * FROM game_saves WHERE id = 1")
    suspend fun getSave(): GameSaveEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSave(save: GameSaveEntity)

    @Query("DELETE FROM game_saves WHERE id = 1")
    suspend fun deleteSave()
}
