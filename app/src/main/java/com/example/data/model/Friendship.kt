package com.example.data.model

data class Friendship(
    val id: String = "",  // "{uid1}_{uid2}" with lower uid first
    val user1Uid: String = "",
    val user2Uid: String = "",
    val status: FriendshipStatus = FriendshipStatus.PENDING,  // PENDING, ACCEPTED, BLOCKED
    val requesterUid: String = "",  // who sent the request
    val createdAt: Long = System.currentTimeMillis(),
    val acceptedAt: Long? = null
)

enum class FriendshipStatus { PENDING, ACCEPTED, BLOCKED }
