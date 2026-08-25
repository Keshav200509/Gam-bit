package com.example.domain.usecase

import com.example.domain.ai.AIPlan
import com.example.domain.ai.AIStrategy
import com.example.domain.model.Board
import com.example.domain.model.IntelActualData
import com.example.domain.model.IntelHint
import com.example.domain.model.IntelType
import com.example.domain.model.Position
import java.util.Random
import javax.inject.Inject

class GenerateIntel @Inject constructor() {

    fun generate(aiPlan: AIPlan, board: Board, trickiness: Float, random: Random, isArena1: Boolean = false): IntelHint {
        if (isArena1) {
            val isTrue = random.nextDouble() < 0.70
            val regions = listOf("northwest", "northeast", "southwest", "southeast", "north", "south", "east", "west", "center")
            val reliabilityLabel = when {
                trickiness <= 0.15f -> "Source confidence: HIGH"
                trickiness <= 0.35f -> "Source confidence: MODERATE — corroborate before committing"
                trickiness <= 0.6f -> "Source confidence: MIXED — partial corroboration only"
                trickiness <= 0.85f -> "Source confidence: LOW — single unverified source"
                else -> "Source confidence: SUSPECT — possible counter-intel"
            }

            // 3 specific types of intel for Arena 1
            val typeIndex = random.nextInt(3)
            return when (typeIndex) {
                0 -> { // Region focus
                    val region = if (isTrue && aiPlan.placements.isNotEmpty()) {
                        val entry = aiPlan.placements.entries.toList()[random.nextInt(aiPlan.placements.size)]
                        getRegion(entry.key.row, entry.key.col)
                    } else {
                        regions[random.nextInt(regions.size)]
                    }
                    IntelHint(
                        text = "The AI plans to focus on $region this round",
                        isTrue = isTrue,
                        reliabilityLabel = reliabilityLabel,
                        type = IntelType.TOKEN_REGION,
                        actualData = IntelActualData.TokenRegion(5, region)
                    )
                }
                1 -> { // Strategy detail
                    val strategy = aiPlan.strategy
                    val desc = if (isTrue) {
                        when (strategy) {
                            AIStrategy.STEALING_CELLS -> "steal your cells"
                            AIStrategy.BUILDING_CLUSTER -> "build adjacent clusters"
                            AIStrategy.SECURING_HIGH_VALUE -> "secure high-value zones"
                            else -> "expand territory"
                        }
                    } else {
                        val fakeDesc = listOf("steal your cells", "build adjacent clusters", "secure high-value zones", "fortify positions")
                        fakeDesc[random.nextInt(fakeDesc.size)]
                    }
                    IntelHint(
                        text = "The AI intends to $desc this round",
                        isTrue = isTrue,
                        reliabilityLabel = reliabilityLabel,
                        type = IntelType.STRATEGY,
                        actualData = IntelActualData.Strategy(strategy.name)
                    )
                }
                else -> { // Target types
                    val desc = if (isTrue) {
                        when (aiPlan.strategy) {
                            AIStrategy.STEALING_CELLS -> "undefended"
                            AIStrategy.SECURING_HIGH_VALUE -> "high-value"
                            else -> "contested"
                        }
                    } else {
                        listOf("high-value", "contested", "undefended")[random.nextInt(3)]
                    }
                    IntelHint(
                        text = "The AI's strategy targets $desc cells this round",
                        isTrue = isTrue,
                        reliabilityLabel = reliabilityLabel,
                        type = IntelType.CELL_VALUE,
                        actualData = IntelActualData.CellValue("center", 3)
                    )
                }
            }
        }

        // 70% chance of being true
        val isTrue = random.nextDouble() < 0.70

        // Get all 9 regions
        val regions = listOf("northwest", "northeast", "southwest", "southeast", "north", "south", "east", "west", "center")

        // Reliability label based on trickiness:
        // HIGH (0-0.2), MODERATE (0.2-0.4), MIXED (0.4-0.6), LOW (0.6-0.8), SUSPECT (0.8-1.0)
        val reliabilityLabel = when {
            trickiness <= 0.15f -> "Source confidence: HIGH"
            trickiness <= 0.35f -> "Source confidence: MODERATE — corroborate before committing"
            trickiness <= 0.6f -> "Source confidence: MIXED — partial corroboration only"
            trickiness <= 0.85f -> "Source confidence: LOW — single unverified source"
            else -> "Source confidence: SUSPECT — possible counter-intel"
        }

        // Pick an IntelType randomly
        val intelType = IntelType.values()[random.nextInt(IntelType.values().size)]

        return when (intelType) {
            IntelType.TOKEN_REGION -> {
                if (isTrue && aiPlan.placements.isNotEmpty()) {
                    // Pick a random actual placement
                    val entry = aiPlan.placements.entries.toList()[random.nextInt(aiPlan.placements.size)]
                    val tokenValue = entry.value.value
                    val region = getRegion(entry.key.row, entry.key.col)
                    val text = "Sources suggest the AI's token $tokenValue is heading toward $region"
                    IntelHint(
                        text = text,
                        isTrue = true,
                        reliabilityLabel = reliabilityLabel,
                        type = IntelType.TOKEN_REGION,
                        actualData = IntelActualData.TokenRegion(tokenValue, region)
                    )
                } else {
                    // Generate bluff: different token or region from any true placement
                    var fakeToken = random.nextInt(5) + 1
                    var fakeRegion = regions[random.nextInt(regions.size)]
                    
                    // Keep searching until we have a bluff combination
                    while (aiPlan.placements.entries.any { getRegion(it.key.row, it.key.col) == fakeRegion && it.value.value == fakeToken }) {
                        fakeToken = random.nextInt(5) + 1
                        fakeRegion = regions[random.nextInt(regions.size)]
                    }

                    val text = "Sources suggest the AI's token $fakeToken is heading toward $fakeRegion"
                    IntelHint(
                        text = text,
                        isTrue = false,
                        reliabilityLabel = reliabilityLabel,
                        type = IntelType.TOKEN_REGION,
                        actualData = IntelActualData.TokenRegion(fakeToken, fakeRegion)
                    )
                }
            }
            IntelType.STRATEGY -> {
                if (isTrue) {
                    val strategy = aiPlan.strategy
                    val strategyLabel = strategy.name.replace('_', ' ').lowercase()
                    val text = "The AI appears focused on $strategyLabel this round"
                    IntelHint(
                        text = text,
                        isTrue = true,
                        reliabilityLabel = reliabilityLabel,
                        type = IntelType.STRATEGY,
                        actualData = IntelActualData.Strategy(strategy.name)
                    )
                } else {
                    val realStrategy = aiPlan.strategy
                    val otherStrategies = AIStrategy.values().filter { it != realStrategy }
                    val fakeStrategy = otherStrategies[random.nextInt(otherStrategies.size)]
                    val strategyLabel = fakeStrategy.name.replace('_', ' ').lowercase()
                    val text = "The AI appears focused on $strategyLabel this round"
                    IntelHint(
                        text = text,
                        isTrue = false,
                        reliabilityLabel = reliabilityLabel,
                        type = IntelType.STRATEGY,
                        actualData = IntelActualData.Strategy(fakeStrategy.name)
                    )
                }
            }
            IntelType.CELL_VALUE -> {
                if (isTrue && aiPlan.placements.isNotEmpty()) {
                    val entry = aiPlan.placements.entries.toList()[random.nextInt(aiPlan.placements.size)]
                    val pos = entry.key
                    val cellValue = board.get(pos).value
                    val region = getRegion(pos.row, pos.col)
                    val text = "Intel indicates the AI is eyeing $region, where a $cellValue-point cell sits vulnerable"
                    IntelHint(
                        text = text,
                        isTrue = true,
                        reliabilityLabel = reliabilityLabel,
                        type = IntelType.CELL_VALUE,
                        actualData = IntelActualData.CellValue(region, cellValue)
                    )
                } else {
                    var fakeRegion = regions[random.nextInt(regions.size)]
                    var fakeValue = random.nextInt(3) + 1 // 1, 2, or 3 points
                    
                    // Ensure it is a bluff (different from any real AI placement cell value in that region)
                    while (aiPlan.placements.entries.any { getRegion(it.key.row, it.key.col) == fakeRegion && board.get(it.key).value == fakeValue }) {
                        fakeRegion = regions[random.nextInt(regions.size)]
                        fakeValue = random.nextInt(3) + 1
                    }

                    val text = "Intel indicates the AI is eyeing $fakeRegion, where a $fakeValue-point cell sits vulnerable"
                    IntelHint(
                        text = text,
                        isTrue = false,
                        reliabilityLabel = reliabilityLabel,
                        type = IntelType.CELL_VALUE,
                        actualData = IntelActualData.CellValue(fakeRegion, fakeValue)
                    )
                }
            }
        }
    }

    private fun getRegion(row: Int, col: Int): String {
        return when {
            row == 2 && col == 2 -> "center"
            row < 2 && col < 2 -> "northwest"
            row < 2 && col > 2 -> "northeast"
            row > 2 && col < 2 -> "southwest"
            row > 2 && col > 2 -> "southeast"
            row < 2 -> "north"
            row > 2 -> "south"
            col < 2 -> "west"
            else -> "east"
        }
    }
}
