package com.example.data.firestore

import com.example.data.local.dao.UserProfileDao
import com.example.data.local.entity.UserProfileEntity
import com.example.data.model.UserProfile
import com.example.data.model.UserStats
import com.example.data.model.ArenaProgress
import com.example.domain.model.Arena
import com.example.domain.model.GameLevel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

enum class GameResult { WIN, LOSS, DRAW }

interface UserProfileRepository {
    fun getUserProfile(uid: String): Flow<UserProfile?>
    fun getCurrentUserProfile(): Flow<UserProfile?>
    suspend fun createProfile(uid: String, displayName: String): Result<UserProfile>
    suspend fun updateProfile(profile: UserProfile): Result<Unit>
    suspend fun updateStats(gameResult: GameResult, isPvP: Boolean): Result<UserStats>
    suspend fun updateArenaProgress(arena: Arena, level: GameLevel, won: Boolean, score: Int): Result<ArenaProgress>
    suspend fun unlockAchievement(achievementId: String): Result<Unit>
    suspend fun findByFriendCode(code: String): Result<UserProfile?>
    suspend fun searchUsers(query: String): Result<List<UserProfile>>
}

@Singleton
class UserProfileRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val userProfileDao: UserProfileDao
) : UserProfileRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    override fun getUserProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        // Emit cache first
        val cached = userProfileDao.getUserProfile(uid)?.toDomain()
        if (cached != null) {
            trySend(cached)
        }

        val docRef = db.collection("users").document(uid)
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val profile = snapshot.toObject(UserProfile::class.java)
                if (profile != null) {
                    repositoryScope.launch {
                        userProfileDao.save(UserProfileEntity.fromDomain(profile))
                    }
                    trySend(profile)
                }
            } else {
                trySend(null)
            }
        }
        awaitClose { listener.remove() }
    }

    override fun getCurrentUserProfile(): Flow<UserProfile?> {
        val currentUid = auth.currentUser?.uid
        return if (currentUid != null) {
            getUserProfile(currentUid)
        } else {
            flowOf(null)
        }
    }

    override suspend fun createProfile(uid: String, displayName: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val doc = db.collection("users").document(uid).get().await()
            if (doc.exists()) {
                val existing = doc.toObject(UserProfile::class.java)
                if (existing != null) {
                    userProfileDao.save(UserProfileEntity.fromDomain(existing))
                    return@withContext Result.success(existing)
                }
            }

            var code = generateFriendCode()
            var codeUnique = false
            var attempts = 0
            while (!codeUnique && attempts < 5) {
                val query = db.collection("users").whereEqualTo("friendCode", code).get().await()
                if (query.isEmpty) {
                    codeUnique = true
                } else {
                    code = generateFriendCode()
                    attempts++
                }
            }

            val profile = UserProfile(
                uid = uid,
                displayName = displayName,
                friendCode = code,
                createdAt = System.currentTimeMillis(),
                lastActive = System.currentTimeMillis()
            )

            db.collection("users").document(uid).set(profile).await()
            userProfileDao.save(UserProfileEntity.fromDomain(profile))
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProfile(profile: UserProfile): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val updated = profile.copy(lastActive = System.currentTimeMillis())
            db.collection("users").document(profile.uid).set(updated).await()
            userProfileDao.save(UserProfileEntity.fromDomain(updated))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateStats(gameResult: GameResult, isPvP: Boolean): Result<UserStats> = withContext(Dispatchers.IO) {
        try {
            val uid = auth.currentUser?.uid ?: return@withContext Result.failure(Exception("Not authenticated"))
            val docRef = db.collection("users").document(uid)
            
            var updatedStats = UserStats()
            
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val profile = snapshot.toObject(UserProfile::class.java) ?: UserProfile()
                val currentStats = profile.stats
                
                val totalGames = currentStats.totalGames + 1
                val isWin = gameResult == GameResult.WIN
                val isLoss = gameResult == GameResult.LOSS
                
                val totalWins = currentStats.totalWins + (if (isWin) 1 else 0)
                val totalLosses = currentStats.totalLosses + (if (isLoss) 1 else 0)
                
                val currentStreak = if (isWin) currentStats.currentStreak + 1 else 0
                val bestStreak = maxOf(currentStats.bestStreak, currentStreak)
                
                val pvpGames = currentStats.pvpGames + (if (isPvP) 1 else 0)
                val pvpWins = currentStats.pvpWins + (if (isPvP && isWin) 1 else 0)
                
                var pvpRating = currentStats.pvpRating
                if (isPvP) {
                    val opponentRating = 1000.0
                    val expected = 1.0 / (1.0 + Math.pow(10.0, (opponentRating - currentStats.pvpRating) / 400.0))
                    val actual = when (gameResult) {
                        GameResult.WIN -> 1.0
                        GameResult.LOSS -> 0.0
                        GameResult.DRAW -> 0.5
                    }
                    val ratingChange = (32 * (actual - expected)).toInt()
                    pvpRating = maxOf(100, currentStats.pvpRating + ratingChange)
                }
                
                val newStats = currentStats.copy(
                    totalGames = totalGames,
                    totalWins = totalWins,
                    totalLosses = totalLosses,
                    currentStreak = currentStreak,
                    bestStreak = bestStreak,
                    pvpGames = pvpGames,
                    pvpWins = pvpWins,
                    pvpRating = pvpRating
                )
                
                updatedStats = newStats
                transaction.update(docRef, "stats", newStats)
            }.await()
            
            // Update cache
            val localProfile = userProfileDao.getUserProfile(uid)?.toDomain()
            if (localProfile != null) {
                userProfileDao.save(UserProfileEntity.fromDomain(localProfile.copy(stats = updatedStats)))
            }
            Result.success(updatedStats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateArenaProgress(arena: Arena, level: GameLevel, won: Boolean, score: Int): Result<ArenaProgress> = withContext(Dispatchers.IO) {
        try {
            val uid = auth.currentUser?.uid ?: return@withContext Result.failure(Exception("Not authenticated"))
            val docRef = db.collection("users").document(uid)
            
            var updatedProgress = ArenaProgress()
            
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val profile = snapshot.toObject(UserProfile::class.java) ?: UserProfile()
                val currentProgress = profile.arenaProgress
                
                var a1l1 = currentProgress.arena1Level1Wins
                var a1l2 = currentProgress.arena1Level2Wins
                var a1l3 = currentProgress.arena1Level3Wins
                var a2l1 = currentProgress.arena2Level1Wins
                var a2l2 = currentProgress.arena2Level2Wins
                var a2l3 = currentProgress.arena2Level3Wins
                var a3l1 = currentProgress.arena3Level1Wins
                var a3l2 = currentProgress.arena3Level2Wins
                var a3l3 = currentProgress.arena3Level3Wins
                
                if (won) {
                    when (arena) {
                        Arena.ASCENDENCY -> {
                            when (level) {
                                GameLevel.LEVEL_1 -> a1l1++
                                GameLevel.LEVEL_2 -> a1l2++
                                GameLevel.LEVEL_3 -> a1l3++
                            }
                        }
                        Arena.CONFRONTATION -> {
                            when (level) {
                                GameLevel.LEVEL_1 -> a2l1++
                                GameLevel.LEVEL_2 -> a2l2++
                                GameLevel.LEVEL_3 -> a2l3++
                            }
                        }
                        Arena.OBLIVION -> {
                            when (level) {
                                GameLevel.LEVEL_1 -> a3l1++
                                GameLevel.LEVEL_2 -> a3l2++
                                GameLevel.LEVEL_3 -> a3l3++
                            }
                        }
                    }
                }
                
                val key = "${arena.name.lowercase()}_level${level.level}"
                val currentBest = currentProgress.bestScores[key] ?: 0
                val newBestScores = currentProgress.bestScores.toMutableMap().apply {
                    if (score > currentBest) {
                        put(key, score)
                    }
                }
                
                val a2UnlockedNew = currentProgress.arena2Unlocked || (a1l3 >= 1)
                val a3UnlockedNew = currentProgress.arena3Unlocked || (a2l3 >= 1)
                val championUnlNew = currentProgress.championUnlocked || (
                    a1l1 > 0 && a1l2 > 0 && a1l3 > 0 &&
                    a2l1 > 0 && a2l2 > 0 && a2l3 > 0 &&
                    a3l1 > 0 && a3l2 > 0 && a3l3 > 0
                )
                
                val newProgress = ArenaProgress(
                    arena1Level1Wins = a1l1,
                    arena1Level2Wins = a1l2,
                    arena1Level3Wins = a1l3,
                    arena2Level1Wins = a2l1,
                    arena2Level2Wins = a2l2,
                    arena2Level3Wins = a2l3,
                    arena3Level1Wins = a3l1,
                    arena3Level2Wins = a3l2,
                    arena3Level3Wins = a3l3,
                    bestScores = newBestScores,
                    arena2Unlocked = a2UnlockedNew,
                    arena3Unlocked = a3UnlockedNew,
                    championUnlocked = championUnlNew
                )
                
                updatedProgress = newProgress
                transaction.update(docRef, "arenaProgress", newProgress)
            }.await()
            
            // Update cache
            val localProfile = userProfileDao.getUserProfile(uid)?.toDomain()
            if (localProfile != null) {
                userProfileDao.save(UserProfileEntity.fromDomain(localProfile.copy(arenaProgress = updatedProgress)))
            }
            Result.success(updatedProgress)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unlockAchievement(achievementId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val uid = auth.currentUser?.uid ?: return@withContext Result.failure(Exception("Not authenticated"))
            val docRef = db.collection("users").document(uid)
            
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val profile = snapshot.toObject(UserProfile::class.java) ?: UserProfile()
                val achievements = profile.achievements.toMutableList()
                if (!achievements.contains(achievementId)) {
                    achievements.add(achievementId)
                    transaction.update(docRef, "achievements", achievements)
                }
            }.await()
            
            // Update cache
            val localProfile = userProfileDao.getUserProfile(uid)?.toDomain()
            if (localProfile != null) {
                val achievements = localProfile.achievements.toMutableList()
                if (!achievements.contains(achievementId)) {
                    achievements.add(achievementId)
                    userProfileDao.save(UserProfileEntity.fromDomain(localProfile.copy(achievements = achievements)))
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun findByFriendCode(code: String): Result<UserProfile?> = withContext(Dispatchers.IO) {
        try {
            val query = db.collection("users")
                .whereEqualTo("friendCode", code)
                .get()
                .await()
            val profile = query.documents.firstOrNull()?.toObject(UserProfile::class.java)
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchUsers(query: String): Result<List<UserProfile>> = withContext(Dispatchers.IO) {
        try {
            val querySnapshot = db.collection("users")
                .whereGreaterThanOrEqualTo("displayName", query)
                .whereLessThanOrEqualTo("displayName", query + "\uf8ff")
                .limit(10)
                .get()
                .await()
            val profiles = querySnapshot.documents.mapNotNull { it.toObject(UserProfile::class.java) }
            Result.success(profiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun generateFriendCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }
}
