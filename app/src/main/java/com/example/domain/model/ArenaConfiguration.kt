package com.example.domain.model

data class ArenaConfiguration(
    val arena: Arena,
    val level: GameLevel,
    val aiCapabilityMin: Int,
    val aiCapabilityMax: Int,
    val intelTrickinessMultiplier: Float,
    val maxLocksPerRound: Int,
    val intelEnabled: Boolean,
    val scoutEnabled: Boolean,
    val probabilityPreviewEnabled: Boolean,
    val humanMovesFirst: Boolean,
    val aiBluffRate: Float
) {
    companion object {
        fun get(arena: Arena, level: GameLevel): ArenaConfiguration {
            return when (arena) {
                Arena.ASCENDENCY -> {
                    when (level) {
                        GameLevel.LEVEL_1 -> ArenaConfiguration(
                            arena = arena,
                            level = level,
                            aiCapabilityMin = -10,
                            aiCapabilityMax = 5,
                            intelTrickinessMultiplier = 0.7f,
                            maxLocksPerRound = 2,
                            intelEnabled = true,
                            scoutEnabled = true,
                            probabilityPreviewEnabled = true,
                            humanMovesFirst = true,
                            aiBluffRate = 0.15f
                        )
                        GameLevel.LEVEL_2 -> ArenaConfiguration(
                            arena = arena,
                            level = level,
                            aiCapabilityMin = -5,
                            aiCapabilityMax = 10,
                            intelTrickinessMultiplier = 1.0f,
                            maxLocksPerRound = 2,
                            intelEnabled = true,
                            scoutEnabled = true,
                            probabilityPreviewEnabled = true,
                            humanMovesFirst = true,
                            aiBluffRate = 0.15f
                        )
                        GameLevel.LEVEL_3 -> ArenaConfiguration(
                            arena = arena,
                            level = level,
                            aiCapabilityMin = 0,
                            aiCapabilityMax = 10,
                            intelTrickinessMultiplier = 1.3f,
                            maxLocksPerRound = 1,
                            intelEnabled = true,
                            scoutEnabled = true,
                            probabilityPreviewEnabled = true,
                            humanMovesFirst = true,
                            aiBluffRate = 0.15f
                        )
                    }
                }
                Arena.CONFRONTATION -> {
                    when (level) {
                        GameLevel.LEVEL_1 -> ArenaConfiguration(
                            arena = arena,
                            level = level,
                            aiCapabilityMin = -10,
                            aiCapabilityMax = 10,
                            intelTrickinessMultiplier = 1.0f,
                            maxLocksPerRound = 2,
                            intelEnabled = true,
                            scoutEnabled = true,
                            probabilityPreviewEnabled = true,
                            humanMovesFirst = false,
                            aiBluffRate = 0.15f
                        )
                        GameLevel.LEVEL_2 -> ArenaConfiguration(
                            arena = arena,
                            level = level,
                            aiCapabilityMin = -5,
                            aiCapabilityMax = 10,
                            intelTrickinessMultiplier = 1.3f,
                            maxLocksPerRound = 2,
                            intelEnabled = true,
                            scoutEnabled = true,
                            probabilityPreviewEnabled = true,
                            humanMovesFirst = false,
                            aiBluffRate = 0.15f
                        )
                        GameLevel.LEVEL_3 -> ArenaConfiguration(
                            arena = arena,
                            level = level,
                            aiCapabilityMin = 0,
                            aiCapabilityMax = 10,
                            intelTrickinessMultiplier = 1.5f,
                            maxLocksPerRound = 1,
                            intelEnabled = true,
                            scoutEnabled = true,
                            probabilityPreviewEnabled = true,
                            humanMovesFirst = false,
                            aiBluffRate = 0.15f
                        )
                    }
                }
                Arena.OBLIVION -> {
                    when (level) {
                        GameLevel.LEVEL_1 -> ArenaConfiguration(
                            arena = arena,
                            level = level,
                            aiCapabilityMin = -10,
                            aiCapabilityMax = 5,
                            intelTrickinessMultiplier = 1.0f,
                            maxLocksPerRound = 1,
                            intelEnabled = false,
                            scoutEnabled = false,
                            probabilityPreviewEnabled = false,
                            humanMovesFirst = false,
                            aiBluffRate = 0.10f
                        )
                        GameLevel.LEVEL_2 -> ArenaConfiguration(
                            arena = arena,
                            level = level,
                            aiCapabilityMin = -5,
                            aiCapabilityMax = 10,
                            intelTrickinessMultiplier = 1.0f,
                            maxLocksPerRound = 1,
                            intelEnabled = false,
                            scoutEnabled = false,
                            probabilityPreviewEnabled = false,
                            humanMovesFirst = false,
                            aiBluffRate = 0.20f
                        )
                        GameLevel.LEVEL_3 -> ArenaConfiguration(
                            arena = arena,
                            level = level,
                            aiCapabilityMin = 0,
                            aiCapabilityMax = 10,
                            intelTrickinessMultiplier = 1.0f,
                            maxLocksPerRound = 0,
                            intelEnabled = false,
                            scoutEnabled = false,
                            probabilityPreviewEnabled = false,
                            humanMovesFirst = false,
                            aiBluffRate = 0.30f
                        )
                    }
                }
            }
        }
    }
}
