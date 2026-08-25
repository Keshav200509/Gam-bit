package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.AIMemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AIMemoryDao {
    @Query("SELECT * FROM ai_memory WHERE id = 1 LIMIT 1")
    fun getAIMemory(): Flow<AIMemoryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(memory: AIMemoryEntity)

    @Query("DELETE FROM ai_memory")
    suspend fun reset()
}
