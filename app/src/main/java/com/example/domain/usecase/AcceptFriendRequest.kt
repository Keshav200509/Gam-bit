package com.example.domain.usecase

import com.example.data.firestore.FriendsRepository
import javax.inject.Inject

class AcceptFriendRequest @Inject constructor(
    private val friendsRepository: FriendsRepository
) {
    suspend operator fun invoke(friendshipId: String): Result<Unit> {
        return friendsRepository.acceptFriendRequest(friendshipId)
    }
}
