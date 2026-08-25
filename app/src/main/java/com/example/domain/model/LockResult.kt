package com.example.domain.model

sealed interface LockResult {
    data class Added(val updatedLocks: List<Position>) : LockResult
    data class Removed(val updatedLocks: List<Position>) : LockResult
    object LimitReached : LockResult
    object NotOwned : LockResult
}
