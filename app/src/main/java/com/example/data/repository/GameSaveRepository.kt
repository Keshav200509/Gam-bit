package com.example.data.repository

import com.example.data.local.dao.GameSaveDao
import com.example.data.local.entity.GameSaveEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameSaveRepository @Inject constructor(
    private val gameSaveDao: GameSaveDao
) {
    suspend fun getSave(): GameSaveEntity? {
        return gameSaveDao.getSave()
    }

    suspend fun saveGame(roundStateJson: String) {
        gameSaveDao.insertSave(GameSaveEntity(roundStateJson = roundStateJson))
    }

    suspend fun deleteSave() {
        gameSaveDao.deleteSave()
    }
}
