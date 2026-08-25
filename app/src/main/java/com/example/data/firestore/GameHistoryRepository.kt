package com.example.data.firestore

import com.example.data.local.dao.GameDao
import com.example.data.local.entity.GameEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class GameRecord(
    val id: String = "",
    val userId: String = "",
    val opponentId: String? = null, // null if vs AI
    val opponentName: String? = null,
    val playerScore: Int = 0,
    val aiScore: Int = 0,
    val result: String = "", // WIN, LOSS, DRAW
    val isPvP: Boolean = false,
    val roundsPlayed: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

interface GameHistoryRepository {
    suspend fun saveGameRecord(record: GameRecord): Result<Unit>
    suspend fun getGameHistory(userId: String): Result<List<GameRecord>>
}

@Singleton
class GameHistoryRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val gameDao: GameDao
) : GameHistoryRepository {

    override suspend fun saveGameRecord(record: GameRecord): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Save to Firestore
            val docRef = db.collection("games").document()
            val recordWithId = record.copy(id = docRef.id)
            docRef.set(recordWithId).await()

            // Also save locally to Room
            val localEntity = GameEntity(
                timestamp = record.timestamp,
                playerScore = record.playerScore,
                aiScore = record.aiScore,
                result = record.result,
                roundsPlayed = record.roundsPlayed,
                clashesWon = 0,
                clashesLost = 0,
                intelAccuracy = 0.0f,
                scoutUses = 0,
                lockUses = 0
            )
            gameDao.insertGame(localEntity)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getGameHistory(userId: String): Result<List<GameRecord>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = db.collection("games")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            val records = snapshot.documents.mapNotNull { it.toObject(GameRecord::class.java) }
            Result.success(records)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
