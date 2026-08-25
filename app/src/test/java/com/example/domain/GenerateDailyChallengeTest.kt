package com.example.domain

import com.example.domain.usecase.GenerateDailyChallenge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class GenerateDailyChallengeTest {

    private val generator = GenerateDailyChallenge()

    @Test
    fun testGenerateDailyChallengeDeterministic() {
        val date = "2026-07-15"
        val challenge1 = generator(date)
        val challenge2 = generator(date)

        assertEquals(challenge1.date, challenge2.date)
        assertEquals(challenge1.boardSeed, challenge2.boardSeed)
        assertEquals(challenge1.aiSeed, challenge2.aiSeed)
        assertEquals(challenge1.arena, challenge2.arena)
        assertEquals(challenge1.level, challenge2.level)
    }

    @Test
    fun testDifferentDatesGenerateDifferentSeeds() {
        val date1 = "2026-07-15"
        val date2 = "2026-07-16"

        val challenge1 = generator(date1)
        val challenge2 = generator(date2)

        assertNotEquals(challenge1.boardSeed, challenge2.boardSeed)
        assertNotEquals(challenge1.aiSeed, challenge2.aiSeed)
    }
}
