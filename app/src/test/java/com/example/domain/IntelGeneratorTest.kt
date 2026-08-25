package com.example.domain

import com.example.domain.ai.AIPlan
import com.example.domain.model.Board
import com.example.domain.model.IntelType
import com.example.domain.model.Position
import com.example.domain.model.Token
import com.example.domain.usecase.GenerateIntel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class IntelGeneratorTest {

    private val generateIntel = GenerateIntel()

    @Test
    fun testConfidenceLabelsBasedOnTrickiness() {
        val aiPlan = AIPlan(
            placements = mapOf(Position(0, 0) to Token(5)),
            locks = emptyList(),
            strategy = com.example.domain.ai.AIStrategy.AGGRESSIVE_EXPANSION
        )
        val board = Board()
        val rand = Random(42)

        // HIGH
        val intelHigh = generateIntel.generate(aiPlan, board, 0.1f, rand)
        assertEquals("Source confidence: HIGH", intelHigh.reliabilityLabel)

        // MODERATE
        val intelMod = generateIntel.generate(aiPlan, board, 0.3f, rand)
        assertEquals("Source confidence: MODERATE — corroborate before committing", intelMod.reliabilityLabel)

        // MIXED
        val intelMixed = generateIntel.generate(aiPlan, board, 0.5f, rand)
        assertEquals("Source confidence: MIXED — partial corroboration only", intelMixed.reliabilityLabel)

        // LOW
        val intelLow = generateIntel.generate(aiPlan, board, 0.75f, rand)
        assertEquals("Source confidence: LOW — single unverified source", intelLow.reliabilityLabel)

        // SUSPECT
        val intelSuspect = generateIntel.generate(aiPlan, board, 0.95f, rand)
        assertEquals("Source confidence: SUSPECT — possible counter-intel", intelSuspect.reliabilityLabel)
    }

    @Test
    fun testIntelGenerationDistributionAndAccuracy() {
        val aiPlan = AIPlan(
            placements = mapOf(Position(0, 0) to Token(5)),
            locks = emptyList(),
            strategy = com.example.domain.ai.AIStrategy.AGGRESSIVE_EXPANSION
        )
        val board = Board()
        val rand = Random(12345)

        var trueCount = 0
        val iterations = 100

        for (i in 0 until iterations) {
            val intel = generateIntel.generate(aiPlan, board, 0.5f, rand)
            assertNotNull(intel.text)
            assertTrue(intel.text.isNotEmpty())
            if (intel.isTrue) {
                trueCount++
            }
        }

        // With 70% expected accuracy, 100 samples should be within [50, 90] with high confidence
        assertTrue("True intel count should be around 70%: actual is $trueCount", trueCount in 50..90)
    }
}
