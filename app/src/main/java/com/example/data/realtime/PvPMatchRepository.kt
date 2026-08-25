package com.example.data.realtime

import com.example.data.model.*
import com.example.data.mapper.*
import com.example.domain.model.Arena
import com.example.domain.model.Board
import com.example.domain.model.GameLevel
import com.example.domain.model.ScoutResult
import com.example.domain.usecase.GenerateBoard
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

interface PvPMatchRepository {
    fun observeMatch(matchId: String): Flow<PvPMatch?>
    fun observeMatchPhase(matchId: String): Flow<MatchPhase>
    
    suspend fun createMatch(
        matchId: String? = null,
        player1: MatchPlayer,
        player2: MatchPlayer,
        arena: Arena,
        level: GameLevel
    ): Result<String>  // returns matchId
    
    suspend fun joinMatch(matchId: String, player: MatchPlayer): Result<Unit>
    suspend fun setPlayerReady(matchId: String, uid: String, ready: Boolean): Result<Unit>
    
    suspend fun submitPlacements(
        matchId: String, 
        uid: String, 
        placements: Map<String, Int>,
        locks: List<String>
    ): Result<Unit>
    
    suspend fun submitScoutResult(
        matchId: String,
        uid: String,
        position: String,
        result: ScoutResult
    ): Result<Unit>
    
    suspend fun updateRoundPhase(matchId: String, phase: RoundPhase): Result<Unit>
    suspend fun resolveRound(matchId: String): Result<Unit>
    suspend fun advanceRound(matchId: String): Result<Unit>
    suspend fun completeMatch(matchId: String, winnerUid: String?): Result<Unit>
    
    suspend fun abandonMatch(matchId: String, uid: String): Result<Unit>
    
    // Presence
    suspend fun updatePresence(matchId: String, uid: String): Result<Unit>
    suspend fun checkOpponentPresence(matchId: String, opponentUid: String): Boolean
}

@Singleton
class PvPMatchRepositoryImpl @Inject constructor(
    private val db: FirebaseDatabase,
    private val generateBoard: GenerateBoard,
    private val resolveClash: com.example.domain.usecase.ResolveClash,
    private val calculateIncome: com.example.domain.usecase.CalculateIncome
) : PvPMatchRepository {

    override fun observeMatch(matchId: String): Flow<PvPMatch?> = callbackFlow {
        val ref = db.getReference("matches/$matchId")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val value = snapshot.value as? Map<String, Any?>
                if (value != null) {
                    trySend(value.toPvPMatch())
                } else {
                    trySend(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override fun observeMatchPhase(matchId: String): Flow<MatchPhase> = callbackFlow {
        val ref = db.getReference("matches/$matchId/matchPhase")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val value = snapshot.value as? String
                if (value != null) {
                    trySend(MatchPhase.valueOf(value))
                } else {
                    trySend(MatchPhase.WAITING)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun createMatch(
        matchId: String?,
        player1: MatchPlayer,
        player2: MatchPlayer,
        arena: Arena,
        level: GameLevel
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val id = matchId ?: java.util.UUID.randomUUID().toString()
            val board = generateBoard()
            val match = PvPMatch(
                matchId = id,
                player1 = player1,
                player2 = player2,
                arena = arena,
                level = level,
                board = board,
                matchPhase = MatchPhase.WAITING,
                roundPhase = RoundPhase.PLACEMENT,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            db.getReference("matches/$id").setValue(match.toMap()).await()
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun joinMatch(matchId: String, player: MatchPlayer): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val ref = db.getReference("matches/$matchId")
            val snapshot = ref.get().await()
            val value = snapshot.value as? Map<String, Any?> ?: return@withContext Result.failure(Exception("Match not found"))
            val match = value.toPvPMatch()
            
            if (match.player1.uid.isEmpty() || match.player1.uid == player.uid) {
                ref.child("player1").setValue(player.toMap()).await()
            } else if (match.player2.uid.isEmpty() || match.player2.uid == player.uid) {
                ref.child("player2").setValue(player.toMap()).await()
            } else {
                return@withContext Result.failure(Exception("Match is already full"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setPlayerReady(matchId: String, uid: String, ready: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val ref = db.getReference("matches/$matchId")
            val snapshot = ref.get().await()
            val value = snapshot.value as? Map<String, Any?> ?: return@withContext Result.failure(Exception("Match not found"))
            val match = value.toPvPMatch()

            if (uid == match.player1.uid) {
                ref.child("player1/ready").setValue(ready).await()
            } else if (uid == match.player2.uid) {
                ref.child("player2/ready").setValue(ready).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun submitPlacements(
        matchId: String,
        uid: String,
        placements: Map<String, Int>,
        locks: List<String>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val ref = db.getReference("matches/$matchId")
            val result = suspendCancellableCoroutine<Result<Unit>> { continuation ->
                ref.runTransaction(object : Transaction.Handler {
                    override fun doTransaction(currentData: MutableData): Transaction.Result {
                        val value = currentData.value as? Map<String, Any?> ?: return Transaction.success(currentData)
                        val match = value.toPvPMatch()
                        val updated = value.toMutableMap()
                        if (uid == match.player1.uid) {
                            updated["player1Placements"] = placements
                            updated["player1Locks"] = locks
                        } else if (uid == match.player2.uid) {
                            updated["player2Placements"] = placements
                            updated["player2Locks"] = locks
                        }
                        updated["updatedAt"] = System.currentTimeMillis()
                        currentData.value = updated
                        return Transaction.success(currentData)
                    }

                    override fun onComplete(
                        error: DatabaseError?,
                        committed: Boolean,
                        currentData: DataSnapshot?
                    ) {
                        if (error != null) {
                            continuation.resume(Result.failure(error.toException()))
                        } else if (!committed) {
                            continuation.resume(Result.failure(Exception("Placements submission transaction failed")))
                        } else {
                            continuation.resume(Result.success(Unit))
                        }
                    }
                })
            }
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun submitScoutResult(
        matchId: String,
        uid: String,
        position: String,
        result: ScoutResult
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            db.getReference("matches/$matchId/scouts/$uid/$position").setValue(result.name).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateRoundPhase(matchId: String, phase: RoundPhase): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            db.getReference("matches/$matchId/roundPhase").setValue(phase.name).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resolveRound(matchId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val ref = db.getReference("matches/$matchId")
            val snapshot = ref.get().await()
            val value = snapshot.value as? Map<String, Any?> ?: return@withContext Result.failure(Exception("Match not found"))
            val match = value.toPvPMatch()

            if (match.roundPhase != RoundPhase.PLACEMENT) {
                return@withContext Result.success(Unit)
            }

            if (match.player1Placements.size < 5 || match.player2Placements.size < 5) {
                return@withContext Result.failure(Exception("Both players must place 5 tokens before resolving"))
            }

            val p1Placements = match.player1Placements.entries.associate { (posStr, tokenVal) ->
                val (r, c) = posStr.split(",").map { it.toInt() }
                val isLocked = match.player1Locks.contains(posStr)
                com.example.domain.model.Position(r, c) to com.example.domain.model.Token(value = tokenVal, isLocked = isLocked)
            }
            val p2Placements = match.player2Placements.entries.associate { (posStr, tokenVal) ->
                val (r, c) = posStr.split(",").map { it.toInt() }
                val isLocked = match.player2Locks.contains(posStr)
                com.example.domain.model.Position(r, c) to com.example.domain.model.Token(value = tokenVal, isLocked = isLocked)
            }

            var currentBoard = match.board
            val clashesList = mutableListOf<ClashOutcome>()

            for (r in 0 until 5) {
                for (c in 0 until 5) {
                    val pos = com.example.domain.model.Position(r, c)
                    val posStr = "$r,$c"
                    val p1TokenVal = match.player1Placements[posStr]
                    val p2TokenVal = match.player2Placements[posStr]

                    if (p1TokenVal != null || p2TokenVal != null) {
                        val clashResult = resolveClash(
                            position = pos,
                            playerPlacements = p1Placements,
                            aiPlacements = p2Placements,
                            board = currentBoard,
                            playerLocks = match.player1Locks.map { 
                                val (row, col) = it.split(",").map { s -> s.toInt() }
                                com.example.domain.model.Position(row, col) 
                            },
                            aiLocks = match.player2Locks.map { 
                                val (row, col) = it.split(",").map { s -> s.toInt() }
                                com.example.domain.model.Position(row, col) 
                            }
                        )

                        val (newOwner, winnerStr) = when (clashResult) {
                            is com.example.domain.model.ClashResult.PlayerWins, is com.example.domain.model.ClashResult.PlayerUncontested, com.example.domain.model.ClashResult.PlayerDefends -> {
                                com.example.domain.model.CellOwner.PLAYER to "player1"
                            }
                            is com.example.domain.model.ClashResult.AIWins, is com.example.domain.model.ClashResult.AIUncontested, com.example.domain.model.ClashResult.AIDefends -> {
                                com.example.domain.model.CellOwner.AI to "player2"
                            }
                            com.example.domain.model.ClashResult.Tie -> {
                                com.example.domain.model.CellOwner.NONE to null
                            }
                        }

                        val cell = currentBoard.get(pos)
                        val isDefended = (clashResult == com.example.domain.model.ClashResult.PlayerDefends || clashResult == com.example.domain.model.ClashResult.AIDefends)
                        val newFortify = if (isDefended) {
                            (cell.fortify + 1).coerceAtMost(3)
                        } else {
                            0
                        }

                        currentBoard = currentBoard.set(pos, cell.copy(owner = newOwner, fortify = newFortify))

                        val p1Strength = if (p1TokenVal != null) {
                            val isLocked = match.player1Locks.contains(posStr)
                            p1TokenVal + 
                            (if (isLocked) 2 else 0) +
                            (if (cell.owner == com.example.domain.model.CellOwner.PLAYER) cell.fortify else 0) +
                            (if (cell.modifier is com.example.domain.model.CellModifier.Battleground && cell.owner == com.example.domain.model.CellOwner.PLAYER) 1 else 0) +
                            (if (cell.modifier is com.example.domain.model.CellModifier.Volatile) 0 else pos.neighbors().count { currentBoard.get(it).owner == com.example.domain.model.CellOwner.PLAYER })
                        } else 0

                        val p2Strength = if (p2TokenVal != null) {
                            val isLocked = match.player2Locks.contains(posStr)
                            p2TokenVal + 
                            (if (isLocked) 2 else 0) +
                            (if (cell.owner == com.example.domain.model.CellOwner.AI) cell.fortify else 0) +
                            (if (cell.modifier is com.example.domain.model.CellModifier.Battleground && cell.owner == com.example.domain.model.CellOwner.AI) 1 else 0) +
                            (if (cell.modifier is com.example.domain.model.CellModifier.Volatile) 0 else pos.neighbors().count { currentBoard.get(it).owner == com.example.domain.model.CellOwner.AI })
                        } else 0

                        clashesList.add(
                            ClashOutcome(
                                position = posStr,
                                player1Token = p1TokenVal,
                                player2Token = p2TokenVal,
                                winner = winnerStr,
                                player1Strength = p1Strength,
                                player2Strength = p2Strength
                            )
                        )
                    }
                }
            }

            val (p1Income, p2Income) = calculateIncome(currentBoard, match.currentRound, 1)

            val updatedP1Score = match.player1Score + p1Income
            val updatedP2Score = match.player2Score + p2Income

            val newRoundResult = RoundResult(
                roundNumber = match.currentRound,
                player1Placements = match.player1Placements,
                player2Placements = match.player2Placements,
                clashes = clashesList,
                player1Income = p1Income,
                player2Income = p2Income
            )

            val updatedRoundResults = match.roundResults + newRoundResult

            val finalMatchPhase = if (match.currentRound >= 12) MatchPhase.COMPLETED else MatchPhase.IN_PROGRESS
            val winnerUid = if (finalMatchPhase == MatchPhase.COMPLETED) {
                when {
                    updatedP1Score > updatedP2Score -> match.player1.uid
                    updatedP2Score > updatedP1Score -> match.player2.uid
                    else -> null
                }
            } else null

            val updatedMatch = match.copy(
                board = currentBoard,
                player1Score = updatedP1Score,
                player2Score = updatedP2Score,
                roundResults = updatedRoundResults,
                roundPhase = RoundPhase.REVEAL,
                matchPhase = finalMatchPhase,
                winnerUid = winnerUid,
                updatedAt = System.currentTimeMillis()
            )

            ref.setValue(updatedMatch.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun advanceRound(matchId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val ref = db.getReference("matches/$matchId")
            val result = suspendCancellableCoroutine<Result<Unit>> { continuation ->
                ref.runTransaction(object : Transaction.Handler {
                    override fun doTransaction(currentData: MutableData): Transaction.Result {
                        val value = currentData.value as? Map<String, Any?> ?: return Transaction.success(currentData)
                        val match = value.toPvPMatch()
                        val updated = value.toMutableMap()
                        
                        updated["currentRound"] = match.currentRound + 1
                        updated["roundPhase"] = RoundPhase.PLACEMENT.name
                        updated["player1Placements"] = emptyMap<String, Int>()
                        updated["player2Placements"] = emptyMap<String, Int>()
                        updated["player1Locks"] = emptyList<String>()
                        updated["player2Locks"] = emptyList<String>()
                        updated["player1"] = match.player1.copy(isReady = false).toMap()
                        updated["player2"] = match.player2.copy(isReady = false).toMap()
                        updated["scouts"] = emptyMap<String, Any>()
                        updated["updatedAt"] = System.currentTimeMillis()
                        
                        currentData.value = updated
                        return Transaction.success(currentData)
                    }

                    override fun onComplete(
                        error: DatabaseError?,
                        committed: Boolean,
                        currentData: DataSnapshot?
                    ) {
                        if (error != null) {
                            continuation.resume(Result.failure(error.toException()))
                        } else {
                            continuation.resume(Result.success(Unit))
                        }
                    }
                })
            }
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun completeMatch(matchId: String, winnerUid: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val ref = db.getReference("matches/$matchId")
            ref.child("matchPhase").setValue(MatchPhase.COMPLETED.name).await()
            ref.child("winnerUid").setValue(winnerUid).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun abandonMatch(matchId: String, uid: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val ref = db.getReference("matches/$matchId")
            ref.child("matchPhase").setValue(MatchPhase.ABANDONED.name).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePresence(matchId: String, uid: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val ref = db.getReference("matches/$matchId")
            val snapshot = ref.get().await()
            val value = snapshot.value as? Map<String, Any?> ?: return@withContext Result.failure(Exception("Match not found"))
            val match = value.toPvPMatch()

            val key = when (uid) {
                match.player1.uid -> "player1"
                match.player2.uid -> "player2"
                else -> null
            }

            if (key != null) {
                ref.child("$key/lastSeen").setValue(System.currentTimeMillis()).await()
                ref.child("$key/lastSeen").onDisconnect().setValue(0L)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun checkOpponentPresence(matchId: String, opponentUid: String): Boolean {
        return try {
            val snapshot = db.getReference("matches/$matchId").get().await()
            val value = snapshot.value as? Map<String, Any?> ?: return false
            val match = value.toPvPMatch()
            val opponent = if (opponentUid == match.player1.uid) match.player1 else if (opponentUid == match.player2.uid) match.player2 else null
            if (opponent != null) {
                val now = System.currentTimeMillis()
                now - opponent.lastSeen < 10000 // seen in last 10s
            } else false
        } catch (e: Exception) {
            false
        }
    }
}
