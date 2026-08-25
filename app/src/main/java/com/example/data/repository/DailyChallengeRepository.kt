package com.example.data.repository

import com.example.data.model.DailyChallenge
import com.example.data.model.DailyChallengeResult
import com.example.domain.usecase.GenerateDailyChallenge
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

interface DailyChallengeRepository {
    fun isChallengeCompletedToday(lastCompletedDate: String?): Boolean
    fun getTodayDateString(): String
    fun getYesterdayDateString(): String

    fun getTodayChallenge(): Flow<DailyChallenge?>
    fun getDailyLeaderboard(date: String, limit: Int = 100): Flow<List<DailyChallengeResult>>
    fun getUserDailyResult(uid: String, date: String): Flow<DailyChallengeResult?>
    
    suspend fun submitResult(result: DailyChallengeResult): Result<Unit>
    suspend fun hasUserCompletedToday(uid: String): Boolean
    suspend fun getUserStreak(uid: String): Int  // consecutive days completed
}

@Singleton
class DailyChallengeRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val generateDailyChallenge: GenerateDailyChallenge
) : DailyChallengeRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    override fun isChallengeCompletedToday(lastCompletedDate: String?): Boolean {
        if (lastCompletedDate.isNullOrEmpty()) return false
        return lastCompletedDate == getTodayDateString()
    }

    override fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    override fun getYesterdayDateString(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DATE, -1)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(cal.time)
    }

    override fun getTodayChallenge(): Flow<DailyChallenge?> = callbackFlow {
        val today = getTodayDateString()
        val docRef = db.collection("daily_challenges").document(today)
        
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val challenge = snapshot.toObject(DailyChallenge::class.java)
                trySend(challenge)
            } else {
                // Generate and save to firestore
                repositoryScope.launch {
                    try {
                        val generated = generateDailyChallenge(today)
                        db.collection("daily_challenges").document(today).set(generated).await()
                        // listener will fire again with the saved document
                    } catch (e: Exception) {
                        e.printStackTrace()
                        trySend(generateDailyChallenge(today)) // Fallback to locally generated if network fails
                    }
                }
            }
        }
        awaitClose { listener.remove() }
    }

    override fun getDailyLeaderboard(date: String, limit: Int): Flow<List<DailyChallengeResult>> = callbackFlow {
        val query = db.collection("daily_results")
            .whereEqualTo("date", date)
            
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.toObjects(DailyChallengeResult::class.java)
                // Sort in memory to avoid missing index crash in firestore
                val sorted = list.sortedByDescending { it.playerScore }
                    .take(limit)
                    .mapIndexed { index, item -> item.copy(rank = index + 1) }
                trySend(sorted)
            } else {
                trySend(emptyList())
            }
        }
        awaitClose { listener.remove() }
    }

    override fun getUserDailyResult(uid: String, date: String): Flow<DailyChallengeResult?> = callbackFlow {
        val docRef = db.collection("daily_results").document("${uid}_${date}")
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val result = snapshot.toObject(DailyChallengeResult::class.java)
                trySend(result)
            } else {
                trySend(null)
            }
        }
        awaitClose { listener.remove() }
    }

    override suspend fun submitResult(result: DailyChallengeResult): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            db.collection("daily_results").document("${result.uid}_${result.date}").set(result).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun hasUserCompletedToday(uid: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val doc = db.collection("daily_results").document("${uid}_${getTodayDateString()}").get().await()
            doc.exists()
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getUserStreak(uid: String): Int = withContext(Dispatchers.IO) {
        try {
            val query = db.collection("daily_results").whereEqualTo("uid", uid).get().await()
            val results = query.toObjects(DailyChallengeResult::class.java).sortedByDescending { it.date }
            val dates = results.map { it.date }.toSet()
            
            val todayStr = getTodayDateString()
            val yesterdayStr = getYesterdayDateString()
            
            var streak = 0
            val checkDate = Calendar.getInstance()
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            
            val completedToday = dates.contains(todayStr)
            val completedYesterday = dates.contains(yesterdayStr)
            
            if (completedToday) {
                streak = 1
                checkDate.add(Calendar.DATE, -1)
                while (dates.contains(sdf.format(checkDate.time))) {
                    streak++
                    checkDate.add(Calendar.DATE, -1)
                }
            } else if (completedYesterday) {
                streak = 1
                checkDate.add(Calendar.DATE, -2)
                while (dates.contains(sdf.format(checkDate.time))) {
                    streak++
                    checkDate.add(Calendar.DATE, -1)
                }
            } else {
                streak = 0
            }
            streak
        } catch (e: Exception) {
            0
        }
    }
}
