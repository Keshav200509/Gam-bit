package com.example.domain

import com.example.domain.ai.AICapability
import com.example.domain.model.Board
import com.example.domain.model.IntelHint
import com.example.domain.model.IntelType
import com.example.domain.model.Position
import com.example.domain.model.ScoutResult
import com.example.domain.usecase.CalculateProbabilities
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Random

class ProbabilityCalculatorTest {

    private val calculateProbabilities = CalculateProbabilities()

    @Test
    fun testScoutClearOverridesProbability() {
        val board = Board()
        val pos = Position(1, 1)
        val rand = Random(42)

        val result = calculateProbabilities.execute(
            position = pos,
            playerToken = 4,
            board = board,
            intel = null,
            scoutResult = ScoutResult.CLEAR,
            aiCapability = AICapability.STEADY,
            trickiness = 0.2f,
            random = rand
        )

        assertEquals(100, result.uncontested)
        assertEquals(0, result.win)
        assertEquals(0, result.lose)
        assertEquals(0, result.tie)
    }

    @Test
    fun testScoutContestedSetsUncontestedToZero() {
        val board = Board()
        val pos = Position(1, 1)
        val rand = Random(42)

        val result = calculateProbabilities.execute(
            position = pos,
            playerToken = 3,
            board = board,
            intel = null,
            scoutResult = ScoutResult.CONTESTED,
            aiCapability = AICapability.STEADY,
            trickiness = 0.2f,
            random = rand
        )

        assertEquals(0, result.uncontested)
        val sum = result.win + result.lose + result.tie
        assertEquals(100, sum)
    }

    @Test
    fun testNormalProbabilitySumsToOneHundred() {
        val board = Board()
        val pos = Position(2, 2)
        val rand = Random(42)

        val result = calculateProbabilities.execute(
            position = pos,
            playerToken = 5,
            board = board,
            intel = null,
            scoutResult = null,
            aiCapability = AICapability.PEAK,
            trickiness = 0.5f,
            random = rand
        )

        val total = result.uncontested + result.win + result.lose + result.tie
        assertEquals(100, total)
    }
}
