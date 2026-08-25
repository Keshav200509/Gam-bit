package com.example.data.repository

import com.example.data.local.dao.GameDao
import com.example.data.local.entity.GameEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatsRepository @Inject constructor(
    private val gameDao: GameDao
) {
    val allGames: Flow<List<GameEntity>> = gameDao.getAllGames()

    fun getRecentGames(limit: Int): Flow<List<GameEntity>> = gameDao.getRecentGames(limit)

    suspend fun saveGame(game: GameEntity) {
        gameDao.insertGame(game)
    }

    suspend fun clearAllGames() {
        gameDao.clearAllGames()
    }
}
