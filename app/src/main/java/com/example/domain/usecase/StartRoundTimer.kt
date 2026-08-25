package com.example.domain.usecase

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class StartRoundTimer @Inject constructor() {
    operator fun invoke(
        initialSeconds: Long = 60L,
        isPaused: () -> Boolean
    ): Flow<Long> = flow {
        var seconds = initialSeconds
        while (seconds >= 0) {
            emit(seconds)
            var elapsed = 0
            while (elapsed < 1000) {
                delay(100)
                if (!isPaused()) {
                    elapsed += 100
                }
            }
            seconds--
        }
    }
}
