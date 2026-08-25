package com.example.domain

import com.example.domain.ai.AICapability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class AICapabilityTest {

    @Test
    fun testCapabilityRangesAndDisplayName() {
        assertEquals(6, AICapability.PEAK.minModifier)
        assertEquals(10, AICapability.PEAK.maxModifier)
        assertEquals("AI: PEAK", AICapability.PEAK.displayName)

        assertEquals(0, AICapability.SHARP.minModifier)
        assertEquals(5, AICapability.SHARP.maxModifier)
        assertEquals("AI: SHARP", AICapability.SHARP.displayName)

        assertEquals(-5, AICapability.STEADY.minModifier)
        assertEquals(-1, AICapability.STEADY.maxModifier)
        assertEquals("AI: STEADY", AICapability.STEADY.displayName)

        assertEquals(-10, AICapability.SLOPPY.minModifier)
        assertEquals(-6, AICapability.SLOPPY.maxModifier)
        assertEquals("AI: SLOPPY", AICapability.SLOPPY.displayName)
    }

    @Test
    fun testRandomCapabilityDistributionAndModifiers() {
        val random = Random(12345)
        
        // Sample 1000 trials to verify modifiers are within appropriate bounds
        for (i in 0 until 1000) {
            val capability = AICapability.randomCapability(random)
            val mod = capability.modifier
            
            // Absolute variance must always remain within [-10, +10]
            assertTrue("Modifier $mod out of bounds", mod in -10..10)
            assertTrue("Modifier $mod doesn't match min boundary", mod >= capability.minModifier)
            assertTrue("Modifier $mod doesn't match max boundary", mod <= capability.maxModifier)
        }
    }
}
