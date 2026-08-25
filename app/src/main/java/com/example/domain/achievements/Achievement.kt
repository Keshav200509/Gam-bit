package com.example.domain.achievements

enum class Achievement(
    val id: String,
    val title: String,
    val description: String
) {
    FIRST_WIN("first_win", "First Win", "Win your first game"),
    STRATEGIST("strategist", "Strategist", "Win 10 games"),
    PERFECTIONIST("perfectionist", "Perfectionist", "Win a game by 50+ points"),
    COMEBACK_KID("comeback_kid", "Comeback Kid", "Win after being behind by 20+ points at round 8"),
    SCOUT_MASTER("scout_master", "Scout Master", "Use scout effectively 10 times (scouted clear -> claimed uncontested)"),
    LOCK_MASTER("lock_master", "Lock Master", "Win 5 games using both locks every game"),
    INTEL_ANALYST("intel_analyst", "Intel Analyst", "Trust intel correctly 5 times (acted on accurate intel, won the clash)"),
    BLUFF_CALLER("bluff_caller", "Bluff Caller", "Correctly identify a bluff intel 3 times (acted against intel, won)"),
    SPEED_DEMON("speed_demon", "Speed Demon", "Win a game in under 6 minutes"),
    IRON_PLAYER("iron_player", "Iron Player", "Play 50 games"),
    CHAMPION("champion", "Champion", "Win all 9 configurations"),
    ARENA_MASTER("arena_master", "Arena Master", "Win level 3 of all 3 arenas")
}
