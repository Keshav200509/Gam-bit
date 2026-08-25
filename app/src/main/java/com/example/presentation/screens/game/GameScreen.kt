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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import com.example.presentation.components.ClashStage
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
import com.example.domain.model.CellOwner
import com.example.domain.model.GamePhase
import com.example.presentation.components.AIStateIndicator
import com.example.presentation.components.ActionBar
import com.example.presentation.components.game.AnimatedGameBoard
import com.example.presentation.components.IncomeSummary
import com.example.presentation.components.StatusBar
import com.example.presentation.components.game.TokenBarAnimated
import com.example.presentation.components.game.IntelPanelAnimated
import com.example.presentation.components.ProbabilityTooltip
import com.example.presentation.components.RoundProgressDots
import com.example.presentation.components.MomentumBar
import com.example.presentation.components.LateGameEventBanner
import com.example.presentation.screens.result.ResultScreen
import com.example.presentation.components.game.RoundTransitionOverlay
import com.example.presentation.components.game.ClashAnimation
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearOutSlowInEasing
import com.example.ui.theme.AICrimson
import com.example.ui.theme.Border
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.PlayerTeal
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ArrowBack
import com.example.domain.model.Arena
import com.example.domain.model.GameLevel
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.Surface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun GameScreen(
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
            .background(Color(0xFF07080C))
            .padding(horizontal = 10.dp, vertical = 6.dp)
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
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.isDailyChallenge) {
                        Surface(
                            color = GoldAccent.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, GoldAccent)
                        ) {
                            Text(
                                text = "DAILY CHALLENGE",
                                color = GoldAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
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
                                text = "GAMBIT",
                                color = PlayerTeal,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.startDailyChallenge() },
                            modifier = Modifier.size(32.dp).testTag("daily_challenge_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = "Play Daily Challenge",
                                tint = GoldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onNavigateToStats,
                            modifier = Modifier.size(32.dp).testTag("stats_nav_button")
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
                            modifier = Modifier.size(32.dp).testTag("settings_nav_button")
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

                RoundProgressDots(currentRound = roundState.roundNumber)

                // Arena Header Details
                val arena = uiState.currentArena
                val level = uiState.currentLevel
                val isArena1 = arena == Arena.ASCENDENCY
                val headerText = "ARENA ${arena.ordinal + 1}: ${arena.displayName.uppercase()} • LVL ${level.level}"

                Text(
                    text = headerText,
                    color = when (arena) {
                        Arena.ASCENDENCY -> Color(0xFF00CBC6)
                        Arena.CONFRONTATION -> PlayerTeal
                        Arena.OBLIVION -> Color(0xFFFF4D4D)
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )

                // Show "AI CALCULATING..." in the status bar if the AI is reactive planning in Arena 1
                val isAiPlanning = isArena1 && roundState.phase == GamePhase.Revealing && roundState.aiPlacements.isEmpty()
                val statusOverride = if (isAiPlanning) "AI CALCULATING COUNTER-PLANS..." else null

                StatusBar(
                    phase = roundState.phase,
                    roundNumber = roundState.roundNumber,
                    overrideText = statusOverride
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Momentum and Score Header Bar
                MomentumBar(
                    playerScore = uiState.playerScore,
                    aiScore = uiState.aiScore,
                    lastRoundPlayerDelta = uiState.lastRoundPlayerDelta,
                    lastRoundAiDelta = uiState.lastRoundAiDelta,
                    scoreHistory = uiState.scoreHistory,
                    roundState = roundState
                )

                // Late-Game Event Alert Banner
                LateGameEventBanner(roundState = roundState)
            }

            // B. CENTER CONTENT AREA (Dynamic based on phase)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (roundState.phase == GamePhase.Intel) {
                    // Intel decryption panel
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Surface),
                        border = BorderStroke(1.5.dp, PlayerTeal.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .testTag("intel_report_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Intel Alert",
                                    tint = PlayerTeal,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "DECRYPTED INTEL REPORT",
                                    color = PlayerTeal,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                            }

                            Divider(color = Border)

                            // Tactical Hint Text
                            Text(
                                text = roundState.intelHint,
                                color = Color.White,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("intel_hint")
                            )

                            // AI Capability & Reliability
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val capColor = when (roundState.aiCapabilityName) {
                                    "PEAK" -> GoldAccent
                                    "SHARP" -> AICrimson
                                    "STEADY" -> AICrimson.copy(alpha = 0.7f)
                                    else -> Color(0xFF9D4EDD)
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(Color(0xFF151926), shape = RoundedCornerShape(6.dp))
                                        .border(1.dp, Color(0xFF1F2633), shape = RoundedCornerShape(6.dp))
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "CPU VARIANCE",
                                            color = Color(0xFF8B92A6),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "${roundState.aiCapabilityName} (${if (roundState.aiCapabilityModifier >= 0) "+" else ""}${roundState.aiCapabilityModifier}%)",
                                            color = capColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.testTag("intel_ai_capability")
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(Color(0xFF151926), shape = RoundedCornerShape(6.dp))
                                        .border(1.dp, Color(0xFF1F2633), shape = RoundedCornerShape(6.dp))
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "RELIABILITY",
                                            color = Color(0xFF8B92A6),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = roundState.intelConfidence.uppercase(),
                                            color = when {
                                                roundState.intelConfidence.contains("HIGH", ignoreCase = true) -> PlayerTeal
                                                roundState.intelConfidence.contains("MODERATE", ignoreCase = true) -> GoldAccent
                                                else -> AICrimson
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.testTag("intel_confidence")
                                        )
                                    }
                                }
                            }

                            // Begin operation button
                            Button(
                                onClick = { viewModel.startPlacing() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PlayerTeal,
                                    contentColor = Color(0xFF07080C)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("begin_placing_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Begin operations", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "BEGIN PLACING PHASE",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Tactical compact intel pill above the board
                        IntelPanelAnimated(
                            roundState = roundState,
                            accuracyCount = uiState.intelAccuracyCount,
                            totalCount = uiState.intelTotalCount
                        )

                        AnimatedGameBoard(
                            board = roundState.board,
                            playerPlacements = roundState.playerPlacements,
                            aiPlacements = if (roundState.phase == GamePhase.Placing) emptyMap() else roundState.aiPlacements,
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
                                .fillMaxWidth()
                                .widthIn(max = 420.dp)
                                .aspectRatio(1f)
                                .testTag("game_board"),
                            intelHintRegion = com.example.presentation.components.getRegionFromIntel(roundState.intel)
                        )
                    }

                    // Floating Probability Tooltip on long-press
                    if (uiState.hoveredPosition != null && uiState.activeProbabilityBreakdown != null) {
                        val pos = uiState.hoveredPosition!!
                        val breakdown = uiState.activeProbabilityBreakdown!!
                        val cellVal = roundState.board.get(pos).value
                        val playerTokenVal = roundState.playerPlacements[pos]?.value ?: uiState.selectedToken ?: 1
                        val neighbors = pos.neighbors()
                        val playerNeighborsCount = neighbors.count { roundState.board.get(it).owner == CellOwner.PLAYER }
                        val playerFortifyBonus = if (roundState.board.get(pos).owner == CellOwner.PLAYER) roundState.board.get(pos).fortify else 0
                        val isLocked = roundState.playerLocks.contains(pos) || (roundState.playerPlacements[pos]?.isLocked == true)
                        val playerLockBonus = if (isLocked) 2 else 0
                        val totalStrength = playerTokenVal + playerNeighborsCount + playerFortifyBonus + playerLockBonus

                        ProbabilityTooltip(
                            position = pos,
                            playerToken = playerTokenVal,
                            cellValue = cellVal,
                            playerStrength = totalStrength,
                            breakdown = breakdown,
                            onDismissRequest = { viewModel.dismissProbabilityTooltip() }
                        )
                    }
                }
            }

            // C. CONTROLS FOOTER PANEL (Dynamic based on phase)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                when (roundState.phase) {
                    is GamePhase.Placing -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
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
                                isArena3 = uiState.currentArena == Arena.OBLIVION,
                                timeRemaining = uiState.timeRemaining,
                                timerActive = uiState.timerActive,
                                onTimerExpired = { viewModel.onCommitClicked(isAutoCommit = true) }
                            )

                            // Token Bar component
                            val usedTokenValues = roundState.playerPlacements.values.map { it.value }.toSet()
                            TokenBarAnimated(
                                selectedToken = uiState.selectedToken,
                                usedTokens = usedTokenValues,
                                onTokenClick = { token -> viewModel.onTokenSelected(token) },
                                modifier = Modifier.testTag("token_bar")
                            )
                        }
                    }
                    is GamePhase.Revealing -> {
                        Text(
                            text = "TACTICAL REVEAL ACTIVE\nPREPARING COMBAT RESOLUTION...",
                            color = GoldAccent,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp,
                            modifier = Modifier
                                .padding(vertical = 12.dp)
                                .testTag("revealing_label")
                        )
                    }
                    is GamePhase.Resolving -> {
                        Text(
                            text = "CLASH SIMULATOR ACTIVE\nENGAGING ACTIVE SECTORS...",
                            color = AICrimson,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp,
                            modifier = Modifier
                                .padding(vertical = 12.dp)
                                .testTag("resolving_label")
                        )
                    }
                    is GamePhase.IncomeSummary -> {
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
                                contentColor = Color(0xFF07080C)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .padding(horizontal = 4.dp)
                                .testTag("start_next_round_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Next wave", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "START ROUND ${roundState.roundNumber + 1}",
                                fontSize = 14.sp,
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
            val roundsRemaining = 12 - uiState.bannerRoundNumber
            RoundTransitionOverlay(
                roundNumber = uiState.bannerRoundNumber,
                roundsRemaining = roundsRemaining,
                onComplete = { viewModel.onRoundBannerFinished() }
            )
        }
    }
}
