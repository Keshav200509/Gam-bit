package com.example.data.firestore

import com.example.data.model.InviteStatus
import com.example.data.model.PvPInvite
import com.example.domain.model.Arena
import com.example.domain.model.GameLevel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface PvPInviteRepository {
    fun getIncomingInvites(uid: String): Flow<List<PvPInvite>>
    fun getOutgoingInvites(uid: String): Flow<List<PvPInvite>>
    
    suspend fun sendInvite(
        fromUid: String, fromName: String,
        toUid: String, toName: String,
        arena: Arena, level: GameLevel
    ): Result<PvPInvite>
    
    suspend fun acceptInvite(invite: PvPInvite): Result<String>  // returns matchId
    suspend fun rejectInvite(inviteId: String): Result<Unit>
    suspend fun cancelInvite(inviteId: String): Result<Unit>
    
    // Cleanup
    suspend fun expireOldInvites(): Result<Unit>
}

@Singleton
class PvPInviteRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val pvpMatchRepository: com.example.data.realtime.PvPMatchRepository
) : PvPInviteRepository {

    override fun getIncomingInvites(uid: String): Flow<List<PvPInvite>> = callbackFlow {
        val listener = db.collection("pvp_invites")
            .whereEqualTo("toUid", uid)
            .whereEqualTo("status", InviteStatus.PENDING.name)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val now = System.currentTimeMillis()
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(PvPInvite::class.java)?.copy(id = doc.id)
                    }.filter { it.expiresAt > now }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    override fun getOutgoingInvites(uid: String): Flow<List<PvPInvite>> = callbackFlow {
        val listener = db.collection("pvp_invites")
            .whereEqualTo("fromUid", uid)
            .whereEqualTo("status", InviteStatus.PENDING.name)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val now = System.currentTimeMillis()
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(PvPInvite::class.java)?.copy(id = doc.id)
                    }.filter { it.expiresAt > now }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun sendInvite(
        fromUid: String,
        fromName: String,
        toUid: String,
        toName: String,
        arena: Arena,
        level: GameLevel
    ): Result<PvPInvite> = withContext(Dispatchers.IO) {
        try {
            val id = UUID.randomUUID().toString()
            val invite = PvPInvite(
                id = id,
                fromUid = fromUid,
                fromName = fromName,
                toUid = toUid,
                toName = toName,
                arena = arena,
                level = level,
                status = InviteStatus.PENDING,
                createdAt = System.currentTimeMillis(),
                expiresAt = System.currentTimeMillis() + (5 * 60 * 1000)
            )
            db.collection("pvp_invites").document(id).set(invite).await()
            Result.success(invite)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun acceptInvite(invite: PvPInvite): Result<String> = withContext(Dispatchers.IO) {
        try {
            val matchId = UUID.randomUUID().toString()
            
            // Atomic check of invite status
            val docRef = db.collection("pvp_invites").document(invite.id)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (snapshot.exists()) {
                    val currentStatus = snapshot.getString("status")
                    if (currentStatus == InviteStatus.PENDING.name) {
                        transaction.update(docRef, "status", InviteStatus.ACCEPTED.name)
                        transaction.update(docRef, "matchId", matchId)
                    } else {
                        throw Exception("Invite is no longer pending")
                    }
                } else {
                    throw Exception("Invite not found")
                }
            }.await()

            // Create match document in Firestore
            val matchData = hashMapOf(
                "matchId" to matchId,
                "player1Uid" to invite.fromUid,
                "player1Name" to invite.fromName,
                "player2Uid" to invite.toUid,
                "player2Name" to invite.toName,
                "arena" to invite.arena.name,
                "level" to invite.level.name,
                "status" to "CREATED",
                "createdAt" to System.currentTimeMillis()
            )
            db.collection("matches").document(matchId).set(matchData).await()

            // Initialize the matching game node in Firebase Realtime Database
            val player1 = com.example.data.model.MatchPlayer(
                uid = invite.fromUid,
                displayName = invite.fromName,
                isReady = false
            )
            val player2 = com.example.data.model.MatchPlayer(
                uid = invite.toUid,
                displayName = invite.toName,
                isReady = false
            )
            pvpMatchRepository.createMatch(
                matchId = matchId,
                player1 = player1,
                player2 = player2,
                arena = invite.arena,
                level = invite.level
            ).getOrThrow()

            Result.success(matchId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun rejectInvite(inviteId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            db.collection("pvp_invites").document(inviteId)
                .update("status", InviteStatus.REJECTED.name)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cancelInvite(inviteId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            db.collection("pvp_invites").document(inviteId)
                .update("status", InviteStatus.CANCELLED.name)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun expireOldInvites(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val query = db.collection("pvp_invites")
                .whereEqualTo("status", InviteStatus.PENDING.name)
                .whereLessThan("expiresAt", now)
                .get()
                .await()
            
            db.runBatch { batch ->
                for (doc in query.documents) {
                    batch.update(doc.reference, "status", InviteStatus.EXPIRED.name)
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
