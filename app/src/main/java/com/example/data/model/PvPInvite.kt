package com.example.data.model

import com.example.domain.model.Arena
import com.example.domain.model.GameLevel
import java.util.UUID

data class PvPInvite(
    val id: String = UUID.randomUUID().toString(),
    val fromUid: String = "",
    val fromName: String = "",
    val toUid: String = "",
    val toName: String = "",
    val arena: Arena = Arena.ASCENDENCY,
    val level: GameLevel = GameLevel.LEVEL_1,
    val status: InviteStatus = InviteStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (5 * 60 * 1000),  // 5 min expiry
    val matchId: String? = null  // set when accepted
)

enum class InviteStatus { PENDING, ACCEPTED, REJECTED, EXPIRED, CANCELLED }
