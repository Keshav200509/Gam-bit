package com.example.domain.ai

import com.example.domain.model.DifficultyPreset
import java.util.Random

/**
 * AICapability represents the dynamic fluctuation of the AI's cognitive capabilities.
 * It strictly maintains domain-layer purity with zero Android or Jetpack Compose imports.
 */
enum class AICapability(
    val minModifier: Int,
    val maxModifier: Int,
    val displayName: String,
    val colorHex: Long
) {
    PEAK(6, 10, "AI: PEAK", 0xFFF59E0BL),
    SHARP(0, 5, "AI: SHARP", 0xFFEF4444L),
    STEADY(-5, -1, "AI: STEADY", 0xFFEF4444L),
    SLOPPY(-10, -6, "AI: SLOPPY", 0xFFA78BFAL);

    var modifier: Int = 0
        internal set

    companion object {
        fun randomCapability(random: Random, preset: DifficultyPreset = DifficultyPreset.VETERAN): AICapability {
            val possibleValues = when (preset) {
                DifficultyPreset.ROOKIE -> values().filter { it != PEAK }
                DifficultyPreset.MASTER -> values().filter { it != SLOPPY }
                DifficultyPreset.VETERAN -> values().toList()
            }
            val chosen = possibleValues[random.nextInt(possibleValues.size)]
            // Generate modifier within the specific range
            chosen.modifier = random.nextInt(chosen.maxModifier - chosen.minModifier + 1) + chosen.minModifier
            return chosen
        }

        fun randomCapabilityFromConfig(random: Random, config: com.example.domain.model.ArenaConfiguration): AICapability {
            val modifier = if (config.aiCapabilityMax >= config.aiCapabilityMin) {
                random.nextInt(config.aiCapabilityMax - config.aiCapabilityMin + 1) + config.aiCapabilityMin
            } else {
                0
            }
            val chosen = when {
                modifier >= 6 -> PEAK
                modifier >= 0 -> SHARP
                modifier >= -5 -> STEADY
                else -> SLOPPY
            }
            chosen.modifier = modifier
            return chosen
        }
    }
}
