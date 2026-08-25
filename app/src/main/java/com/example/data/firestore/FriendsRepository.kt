package com.example.data.firestore

import com.example.data.model.Friendship
import com.example.data.model.FriendshipStatus
import com.example.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface FriendsRepository {
    fun getFriendsList(uid: String): Flow<List<UserProfile>>
    fun getPendingRequests(uid: String): Flow<List<Friendship>>
    fun getSentRequests(uid: String): Flow<List<Friendship>>
    
    suspend fun sendFriendRequest(fromUid: String, toUid: String): Result<Friendship>
    suspend fun acceptFriendRequest(friendshipId: String): Result<Unit>
    suspend fun rejectFriendRequest(friendshipId: String): Result<Unit>
    suspend fun removeFriend(friendshipId: String): Result<Unit>
    
    suspend fun findUserByFriendCode(code: String): Result<UserProfile?>
    suspend fun areFriends(uid1: String, uid2: String): Boolean
}

@Singleton
class FriendsRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val userProfileRepository: UserProfileRepository
) : FriendsRepository {

    override fun getFriendsList(uid: String): Flow<List<UserProfile>> = callbackFlow {
        val query1 = db.collection("friendships")
            .whereEqualTo("user1Uid", uid)
            .whereEqualTo("status", FriendshipStatus.ACCEPTED.name)
        
        val query2 = db.collection("friendships")
            .whereEqualTo("user2Uid", uid)
            .whereEqualTo("status", FriendshipStatus.ACCEPTED.name)
        
        var list1 = emptyList<Friendship>()
        var list2 = emptyList<Friendship>()
        
        fun update() {
            val friendUids = (list1 + list2).map { 
                if (it.user1Uid == uid) it.user2Uid else it.user1Uid 
            }.distinct()
            
            if (friendUids.isEmpty()) {
                trySend(emptyList())
                return
            }
            
            db.collection("users")
                .whereIn("uid", friendUids)
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null) {
                        val profiles = snapshot.documents.mapNotNull { it.toObject(UserProfile::class.java) }
                        trySend(profiles)
                    }
                }
        }
        
        val listener1 = query1.addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null) {
                list1 = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Friendship::class.java)?.copy(id = doc.id)
                }
                update()
            }
        }
        
        val listener2 = query2.addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null) {
                list2 = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Friendship::class.java)?.copy(id = doc.id)
                }
                update()
            }
        }
        
        awaitClose {
            listener1.remove()
            listener2.remove()
        }
    }

    override fun getPendingRequests(uid: String): Flow<List<Friendship>> = callbackFlow {
        val query1 = db.collection("friendships")
            .whereEqualTo("user1Uid", uid)
            .whereEqualTo("status", FriendshipStatus.PENDING.name)
        val query2 = db.collection("friendships")
            .whereEqualTo("user2Uid", uid)
            .whereEqualTo("status", FriendshipStatus.PENDING.name)
        
        var list1 = emptyList<Friendship>()
        var list2 = emptyList<Friendship>()
        
        fun update() {
            val combined = (list1 + list2).distinctBy { it.id }
            val incoming = combined.filter { it.requesterUid != uid }
            trySend(incoming)
        }
        
        val listener1 = query1.addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null) {
                list1 = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Friendship::class.java)?.copy(id = doc.id)
                }
                update()
            }
        }
        
        val listener2 = query2.addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null) {
                list2 = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Friendship::class.java)?.copy(id = doc.id)
                }
                update()
            }
        }
        
        awaitClose {
            listener1.remove()
            listener2.remove()
        }
    }

    override fun getSentRequests(uid: String): Flow<List<Friendship>> = callbackFlow {
        val query1 = db.collection("friendships")
            .whereEqualTo("user1Uid", uid)
            .whereEqualTo("status", FriendshipStatus.PENDING.name)
        val query2 = db.collection("friendships")
            .whereEqualTo("user2Uid", uid)
            .whereEqualTo("status", FriendshipStatus.PENDING.name)
        
        var list1 = emptyList<Friendship>()
        var list2 = emptyList<Friendship>()
        
        fun update() {
            val combined = (list1 + list2).distinctBy { it.id }
            val sent = combined.filter { it.requesterUid == uid }
            trySend(sent)
        }
        
        val listener1 = query1.addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null) {
                list1 = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Friendship::class.java)?.copy(id = doc.id)
                }
                update()
            }
        }
        
        val listener2 = query2.addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null) {
                list2 = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Friendship::class.java)?.copy(id = doc.id)
                }
                update()
            }
        }
        
        awaitClose {
            listener1.remove()
            listener2.remove()
        }
    }

    override suspend fun sendFriendRequest(fromUid: String, toUid: String): Result<Friendship> = withContext(Dispatchers.IO) {
        try {
            if (fromUid == toUid) {
                return@withContext Result.failure(Exception("You cannot send a friend request to yourself"))
            }

            val user1Uid = if (fromUid < toUid) fromUid else toUid
            val user2Uid = if (fromUid < toUid) toUid else fromUid
            val friendshipId = "${user1Uid}_${user2Uid}"
            
            val docRef = db.collection("friendships").document(friendshipId)
            val snapshot = docRef.get().await()
            
            if (snapshot.exists()) {
                val existing = snapshot.toObject(Friendship::class.java)
                if (existing != null) {
                    when (existing.status) {
                        FriendshipStatus.ACCEPTED -> return@withContext Result.failure(Exception("Already friends"))
                        FriendshipStatus.PENDING -> return@withContext Result.failure(Exception("Friend request already pending"))
                        FriendshipStatus.BLOCKED -> return@withContext Result.failure(Exception("Friendship blocked"))
                    }
                }
            }

            val newFriendship = Friendship(
                id = friendshipId,
                user1Uid = user1Uid,
                user2Uid = user2Uid,
                status = FriendshipStatus.PENDING,
                requesterUid = fromUid,
                createdAt = System.currentTimeMillis()
            )

            docRef.set(newFriendship).await()
            Result.success(newFriendship)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun acceptFriendRequest(friendshipId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val docRef = db.collection("friendships").document(friendshipId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (snapshot.exists()) {
                    transaction.update(docRef, "status", FriendshipStatus.ACCEPTED.name)
                    transaction.update(docRef, "acceptedAt", System.currentTimeMillis())
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun rejectFriendRequest(friendshipId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val docRef = db.collection("friendships").document(friendshipId)
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeFriend(friendshipId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val docRef = db.collection("friendships").document(friendshipId)
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun findUserByFriendCode(code: String): Result<UserProfile?> {
        return userProfileRepository.findByFriendCode(code)
    }

    override suspend fun areFriends(uid1: String, uid2: String): Boolean = withContext(Dispatchers.IO) {
        val user1Uid = if (uid1 < uid2) uid1 else uid2
        val user2Uid = if (uid1 < uid2) uid2 else uid1
        val friendshipId = "${user1Uid}_${user2Uid}"
        
        try {
            val doc = db.collection("friendships").document(friendshipId).get().await()
            val friendship = doc.toObject(Friendship::class.java)
            friendship != null && friendship.status == FriendshipStatus.ACCEPTED
        } catch (e: Exception) {
            false
        }
    }
}
