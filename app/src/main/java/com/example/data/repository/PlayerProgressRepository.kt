package com.example.data.repository

import com.example.data.local.dao.PlayerProgressDao
import com.example.data.local.entity.PlayerProgressEntity
import com.example.domain.model.Arena
import com.example.domain.model.GameLevel
import com.example.domain.model.PlayerProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerProgressRepository @Inject constructor(
    private val playerProgressDao: PlayerProgressDao
) {
    val progress: Flow<PlayerProgress> = playerProgressDao.getProgress().map { entity ->
        entity?.toDomain() ?: PlayerProgress()
    }

    suspend fun getProgressOnce(): PlayerProgress {
        return progress.first()
    }

    suspend fun recordWin(arena: Arena, level: GameLevel, score: Int) {
        val currentProgress = getProgressOnce()
        val updatedProgress = currentProgress.recordWin(arena, level, score)
        playerProgressDao.save(PlayerProgressEntity.fromDomain(updatedProgress))
    }

    suspend fun resetProgress() {
        playerProgressDao.reset()
    }
}
