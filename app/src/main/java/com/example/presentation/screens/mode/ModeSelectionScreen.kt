package com.example.presentation.screens.mode

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ArenaProgress
import com.example.domain.model.Arena
import com.example.domain.model.GameLevel
import com.example.presentation.components.AnimatedBackground
import com.example.presentation.components.GlowButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeSelectionScreen(
    viewModel: ModeSelectionViewModel,
    onBack: () -> Unit,
    onPlay: (Arena, GameLevel) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val progress = state.userProfile?.arenaProgress ?: ArenaProgress()
    val selectedArena = state.selectedArena
    val selectedLevel = state.selectedLevel

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "CHOOSE ARENA",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.08f), CircleShape)
                            .testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Elegant slow-shifting cosmic background
            AnimatedBackground(
                colors = listOf(
                    Color(0xFF030712), // Deep Space Black
                    Color(0xFF0F172A), // Slate Dark
                    Color(0xFF1E1E38), // Muted Cosmic Indigo
                    Color(0xFF030712)
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Arena lists
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Arena.entries.forEach { arena ->
                        val isUnlocked = isArenaUnlocked(progress, arena)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(
                                    animationSpec = spring(
                                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                                        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                                    )
                                )
                        ) {
                            ArenaCard(
                                arena = arena,
                                isUnlocked = isUnlocked,
                                isSelected = selectedArena == arena,
                                onClick = { viewModel.selectArena(arena) }
                            )

                            AnimatedVisibility(
                                visible = selectedArena == arena,
                                enter = expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
                                exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    GameLevel.entries.forEach { level ->
                                        val isLvlUnlocked = isLevelUnlocked(progress, arena, level)
                                        val wins = getWinsFor(progress, arena, level)
                                        val bestScoreKey = "${arena.name.lowercase()}_level${level.level}"
                                        val bestScore = progress.bestScores[bestScoreKey] ?: 0

                                        LevelCard(
                                            level = level,
                                            arena = arena,
                                            wins = wins,
                                            bestScore = bestScore,
                                            isUnlocked = isLvlUnlocked,
                                            isSelected = selectedLevel == level,
                                            onClick = { viewModel.selectLevel(level) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Bottom CTA: PLAY BUTTON
                val currentGlowColor = when (selectedArena) {
                    Arena.ASCENDENCY -> Color(0xFF00ADB5)
                    Arena.CONFRONTATION -> Color(0xFF9E9E9E)
                    Arena.OBLIVION -> Color(0xFFE63946)
                }

                val playButtonEnabled = isLevelUnlocked(progress, selectedArena, selectedLevel)

                GlowButton(
                    text = "PLAY",
                    icon = Icons.Default.PlayArrow,
                    onClick = { onPlay(selectedArena, selectedLevel) },
                    enabled = playButtonEnabled,
                    glowColor = currentGlowColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("mode_selection_play_button")
                )
            }
        }
    }
}

private fun getWinsFor(progress: ArenaProgress, arena: Arena, level: GameLevel): Int {
    return when (arena) {
        Arena.ASCENDENCY -> when (level) {
            GameLevel.LEVEL_1 -> progress.arena1Level1Wins
            GameLevel.LEVEL_2 -> progress.arena1Level2Wins
            GameLevel.LEVEL_3 -> progress.arena1Level3Wins
        }
        Arena.CONFRONTATION -> when (level) {
            GameLevel.LEVEL_1 -> progress.arena2Level1Wins
            GameLevel.LEVEL_2 -> progress.arena2Level2Wins
            GameLevel.LEVEL_3 -> progress.arena2Level3Wins
        }
        Arena.OBLIVION -> when (level) {
            GameLevel.LEVEL_1 -> progress.arena3Level1Wins
            GameLevel.LEVEL_2 -> progress.arena3Level2Wins
            GameLevel.LEVEL_3 -> progress.arena3Level3Wins
        }
    }
}

private fun isArenaUnlocked(progress: ArenaProgress, arena: Arena): Boolean {
    return when (arena) {
        Arena.ASCENDENCY -> true
        Arena.CONFRONTATION -> progress.arena2Unlocked
        Arena.OBLIVION -> progress.arena3Unlocked
    }
}

private fun isLevelUnlocked(progress: ArenaProgress, arena: Arena, level: GameLevel): Boolean {
    if (!isArenaUnlocked(progress, arena)) return false
    return when (level) {
        GameLevel.LEVEL_1 -> true
        GameLevel.LEVEL_2 -> getWinsFor(progress, arena, GameLevel.LEVEL_1) >= 2
        GameLevel.LEVEL_3 -> getWinsFor(progress, arena, GameLevel.LEVEL_2) >= 2
    }
}
