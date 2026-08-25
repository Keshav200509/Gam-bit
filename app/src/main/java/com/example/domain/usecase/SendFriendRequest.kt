package com.example.domain.usecase

import com.example.data.firestore.FriendsRepository
import com.example.data.model.Friendship
import javax.inject.Inject

class SendFriendRequest @Inject constructor(
    private val friendsRepository: FriendsRepository
) {
    suspend operator fun invoke(fromUid: String, toUid: String): Result<Friendship> {
        if (fromUid.trim() == toUid.trim()) {
            return Result.failure(Exception("You cannot send a friend request to yourself"))
        }
        return friendsRepository.sendFriendRequest(fromUid, toUid)
    }
}
