package com.example.domain

import com.example.domain.model.Position
import com.example.domain.model.ScoutResult
import com.example.domain.model.Token
import com.example.domain.usecase.UseScout
import org.junit.Assert.assertEquals
import org.junit.Test

class ScoutUseCaseTest {

    private val useScout = UseScout()

    @Test
    fun testScoutReturnsContestedWhenAIPresent() {
        val aiPlacements = mapOf(
            Position(1, 1) to Token(3),
            Position(2, 2) to Token(5)
        )

        val result = useScout.execute(Position(1, 1), aiPlacements)
        assertEquals(ScoutResult.CONTESTED, result)
    }

    @Test
    fun testScoutReturnsClearWhenAINotPresent() {
        val aiPlacements = mapOf(
            Position(1, 1) to Token(3)
        )

        val result = useScout.execute(Position(0, 0), aiPlacements)
        assertEquals(ScoutResult.CLEAR, result)
    }
}
