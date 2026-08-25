package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyChallengeTest {

    @Test
    fun testGetCurrentSeedIsDeterministic() {
        val seed1 = DailyChallenge.getCurrentSeed()
        val seed2 = DailyChallenge.getCurrentSeed()
        assertEquals(seed1, seed2)
    }
}
