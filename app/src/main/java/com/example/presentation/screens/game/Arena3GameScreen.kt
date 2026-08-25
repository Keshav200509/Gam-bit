package com.example.presentation.screens.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.domain.model.CellOwner
import com.example.domain.model.GamePhase
import com.example.presentation.components.AIStateIndicator
import com.example.presentation.components.ActionBar
import com.example.presentation.components.GameBoard
import com.example.presentation.components.IncomeSummary
import com.example.presentation.components.StatusBar
import com.example.presentation.components.TokenBar
import com.example.presentation.components.RoundProgressDots
import com.example.presentation.screens.result.ResultScreen
import com.example.presentation.components.RoundBanner
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearOutSlowInEasing
import com.example.ui.theme.AICrimson
import com.example.ui.theme.Border
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.PlayerTeal
import com.example.ui.theme.Surface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun Arena3GameScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GameViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val roundState = uiState.roundState

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                viewModel.onPause()
            } else if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val animatedPlayerScore by animateIntAsState(
        targetValue = uiState.playerScore,
        animationSpec = tween(1000, easing = LinearOutSlowInEasing),
        label = "player_score_anim"
    )
    val animatedAiScore by animateIntAsState(
        targetValue = uiState.aiScore,
        animationSpec = tween(1000, easing = LinearOutSlowInEasing),
        label = "ai_score_anim"
    )

    // Show Resume simulation dialog if a save exists
    if (uiState.showResumeDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(
                    text = "RECOVER SIMULATION?",
                    color = GoldAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace
                )
            },
            text = {
                Text(
                    text = "An active tactical session from round ${uiState.roundState.roundNumber} was detected in persistence. Do you wish to resume this operation or initialize a new simulation?",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            containerColor = Surface,
            confirmButton = {
                TextButton(
                    onClick = { viewModel.resumeGame() },
                    modifier = Modifier.testTag("resume_confirm_button")
                ) {
                    Text(text = "RESUME SESSION", color = PlayerTeal, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.discardSaveAndNewGame() },
                    modifier = Modifier.testTag("resume_dismiss_button")
                ) {
                    Text(text = "NEW RUN", color = AICrimson, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // 1. If Game is Over, show the full Result Screen
    if (roundState.phase == GamePhase.GameOver) {
        ResultScreen(
            playerScore = uiState.playerScore,
            aiScore = uiState.aiScore,
            board = roundState.board,
            onPlayAgain = { viewModel.playAgain() },
            modifier = modifier
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF080910)) // Darker background for Oblivion (#080910)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // A. STATUS BAR & SCORE BAR HEADERS
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.size(32.dp).testTag("back_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back to Arena Map",
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "GAMBIT // OBLIVION LABS",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onNavigateToStats,
                            modifier = Modifier.size(36.dp).testTag("stats_nav_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Leaderboard,
                                contentDescription = "Stats",
                                tint = PlayerTeal,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier.size(36.dp).testTag("settings_nav_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = PlayerTeal,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                RoundProgressDots(currentRound = roundState.roundNumber)

                Spacer(modifier = Modifier.height(4.dp))

                // Arena Header Details (Custom Arena 3)
                val level = uiState.currentLevel
                val headerText = "ARENA 3: OBLIVION — LEVEL ${level.level}"
                val subtitleText = "No hints. No scout. No mercy."

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = headerText,
                        color = Color(0xFFFF4D4D), // Crimson header color
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = subtitleText,
                        color = TextMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                StatusBar(
                    phase = roundState.phase,
                    roundNumber = roundState.roundNumber,
                    overrideText = null
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Score stats row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(PlayerTeal, shape = RoundedCornerShape(50))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "YOU: $animatedPlayerScore",
                            color = PlayerTeal,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.testTag("player_score")
                        )
                    }

                    AIStateIndicator(
                        capabilityName = roundState.aiCapabilityName,
                        capabilityModifier = roundState.aiCapabilityModifier,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "AI MACHINE: $animatedAiScore",
                            color = AICrimson,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.testTag("ai_score")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(AICrimson, shape = RoundedCornerShape(50))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Proportional score bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF151722))
                ) {
                    val total = (animatedPlayerScore + animatedAiScore).toFloat()
                    val ratio = if (total == 0f) 0.5f else animatedPlayerScore.toFloat() / total

                    Box(
                        modifier = Modifier
                            .weight(ratio)
                            .fillMaxHeight()
                            .background(PlayerTeal)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f - ratio)
                            .fillMaxHeight()
                            .background(AICrimson)
                    )
                }
            }

            // B. CENTER CONTENT AREA (Dynamic based on phase)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Crimson Border Glow on board container
                    Box(
                        modifier = Modifier
                            .border(
                                width = 2.dp,
                                color = Color(0xFFFF4D4D),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(4.dp)
                    ) {
                        GameBoard(
                            board = roundState.board,
                            playerPlacements = roundState.playerPlacements,
                            aiPlacements = if (roundState.phase == GamePhase.Placing) emptyMap() else roundState.aiPlacements, // Hide AI placements during Placing
                            scoutedPosition = roundState.scoutedPosition,
                            scoutResult = roundState.scoutResult,
                            playerLocks = roundState.playerLocks,
                            scoutMode = uiState.scoutMode,
                            lockMode = uiState.lockMode,
                            currentResolvingPosition = uiState.currentResolvingPosition,
                            clashStage = uiState.clashStage,
                            clashResults = uiState.clashResults,
                            phase = roundState.phase,
                            onCellClick = { pos -> viewModel.onCellClicked(pos) },
                            onCellLongPress = { pos -> viewModel.onCellLongPressed(pos) },
                            modifier = Modifier
                                .sizeIn(maxWidth = 400.dp, maxHeight = 400.dp)
                                .aspectRatio(1f)
                                .testTag("game_board")
                        )
                    }
                }
            }

            // C. CONTROLS FOOTER PANEL (Dynamic based on phase)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                when (roundState.phase) {
                    is GamePhase.Placing -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (roundState.playerPlacements.isEmpty()) {
                                Text(
                                    text = "Place at least 1 token on the board to commit",
                                    color = TextMuted,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }

                            // Extracted Action Bar
                            ActionBar(
                                scoutMode = uiState.scoutMode,
                                scoutUsed = roundState.scoutUsed,
                                lockMode = uiState.lockMode,
                                numLocksUsed = roundState.playerPlacements.values.count { it.isLocked },
                                hasPlacements = roundState.playerPlacements.isNotEmpty(),
                                onScoutClick = { viewModel.onScoutToggled() },
                                onLockClick = { viewModel.onLockToggled() },
                                onClearClick = { viewModel.onClearClicked() },
                                onCommitClick = { viewModel.onCommitClicked() },
                                isArena3 = true
                            )

                            // Token Bar component
                            val usedTokenValues = roundState.playerPlacements.values.map { it.value }.toSet()
                            TokenBar(
                                selectedToken = uiState.selectedToken,
                                usedTokens = usedTokenValues,
                                onTokenClick = { token -> viewModel.onTokenSelected(token) },
                                modifier = Modifier.testTag("token_bar")
                            )
                        }
                    }
                    is GamePhase.Revealing -> {
                        Text(
                            text = "TACTICAL REVEAL ACTIVE\nPREPARING COMBAT RESOLUTION VECTORS...",
                            color = GoldAccent,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp,
                            modifier = Modifier
                                .padding(vertical = 24.dp)
                                .testTag("revealing_label")
                        )
                    }
                    is GamePhase.Resolving -> {
                        Text(
                            text = "CLASH SIMULATOR ACTIVE\nENGAGING ACTIVE SECTORS...",
                            color = AICrimson,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp,
                            modifier = Modifier
                                .padding(vertical = 24.dp)
                                .testTag("resolving_label")
                        )
                    }
                    is GamePhase.IncomeSummary -> {
                        // Income summary inline overlay
                        IncomeSummary(
                            isVisible = true,
                            board = roundState.board,
                            playerIncome = uiState.playerIncomeThisRound,
                            aiIncome = uiState.aiIncomeThisRound,
                            roundNumber = roundState.roundNumber,
                            aiCapabilityModifier = roundState.aiCapabilityModifier
                        )
                    }
                    is GamePhase.RoundTransition -> {
                        Button(
                            onClick = { viewModel.startNextRound() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PlayerTeal,
                                contentColor = Color(0xFF080910)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                .testTag("start_next_round_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Next wave")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "START ROUND ${roundState.roundNumber + 1}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                    else -> {}
                }
            }
        }

        if (uiState.showRoundTransitionBanner) {
            RoundBanner(
                roundNumber = uiState.bannerRoundNumber,
                isVisible = uiState.showRoundTransitionBanner,
                onFinished = { viewModel.onRoundBannerFinished() }
            )
        }
    }
}
