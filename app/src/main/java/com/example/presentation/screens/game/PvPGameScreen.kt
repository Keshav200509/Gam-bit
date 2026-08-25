package com.example.presentation.screens.game

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.domain.model.CellModifier
import com.example.domain.model.CellOwner
import com.example.domain.model.Position
import com.example.ui.theme.AICrimson
import com.example.ui.theme.Border
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.IntelPurple
import com.example.ui.theme.PlayerTeal
import com.example.ui.theme.Surface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PvPGameScreen(
    viewModel: PvPGameViewModel,
    matchId: String,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(matchId) {
        viewModel.startMatchObservation(matchId)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "GAMBIT MULTIPLAYER",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp
                            )
                        )
                        Text(
                            text = "Arena: ${state.match?.arena?.name ?: "LOADING..."}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.abandonMatch()
                            onBack()
                        },
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Exit Match", tint = MaterialTheme.colorScheme.error)
                    }
                },
                actions = {
                    // Opponent Connection Indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        val pulse = rememberInfiniteTransition(label = "pulse")
                        val alphaPulse by pulse.animateFloat(
                            initialValue = 0.4f,
                            targetValue = 1.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulse"
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .alpha(if (state.opponentPresence) alphaPulse else 0.5f)
                                .background(
                                    color = if (state.opponentPresence) Color(0xFF4CAF50) else Color(0xFFFF9800),
                                    shape = RoundedCornerShape(50)
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (state.opponentPresence) "Opponent Active" else "Opponent Idle",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (state.opponentPresence) Color(0xFF4CAF50) else Color(0xFFFF9800)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F111A)
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0F111A), Color(0xFF05060B))
                    )
                )
        ) {
            val match = state.match
            if (state.isLoading || match == null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = PlayerTeal)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Synchronizing match state with Neural Net...", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                }
            } else {
                val isP1 = state.myRole == "player1"
                val myPlacements = if (isP1) match.player1Placements else match.player2Placements
                val myLocks = if (isP1) match.player1Locks else match.player2Locks
                val myReady = if (isP1) match.player1.isReady else match.player2.isReady

                val oppPlacements = if (isP1) match.player2Placements else match.player1Placements
                val oppLocks = if (isP1) match.player2Locks else match.player1Locks
                val oppReady = if (isP1) match.player2.isReady else match.player1.isReady
                val oppName = if (isP1) match.player2.displayName else match.player1.displayName

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Match Scoreboard Section
                    PvPScoreboard(
                        p1Name = match.player1.displayName + if (isP1) " (You)" else "",
                        p1Score = match.player1Score,
                        p1Ready = match.player1.isReady,
                        p2Name = match.player2.displayName + if (!isP1) " (You)" else "",
                        p2Score = match.player2Score,
                        p2Ready = match.player2.isReady
                    )

                    // Round & Stage Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ROUND ${match.currentRound} OF 12",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent,
                            fontFamily = FontFamily.Monospace
                        )
                        
                        Box(
                            modifier = Modifier
                                .background(
                                    color = when (match.roundPhase) {
                                        RoundPhase.PLACEMENT -> PlayerTeal.copy(alpha = 0.15f)
                                        else -> AICrimson.copy(alpha = 0.15f)
                                    },
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = when (match.roundPhase) {
                                        RoundPhase.PLACEMENT -> PlayerTeal.copy(alpha = 0.4f)
                                        else -> AICrimson.copy(alpha = 0.4f)
                                    },
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = match.roundPhase.name,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = if (match.roundPhase == RoundPhase.PLACEMENT) PlayerTeal else AICrimson,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // 5x5 Grid Board
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        PvPGridBoard(
                            board = match.board,
                            myPlacements = myPlacements,
                            myLocks = myLocks,
                            oppPlacements = oppPlacements,
                            oppLocks = oppLocks,
                            myScouts = state.myScouts,
                            roundPhase = match.roundPhase,
                            myRole = state.myRole,
                            selectedToken = state.selectedToken,
                            scoutMode = state.scoutMode,
                            lockMode = state.lockMode,
                            onCellClick = { r, c -> viewModel.handleCellClick(r, c) }
                        )
                    }

                    if (match.roundPhase == RoundPhase.PLACEMENT) {
                        // Unused Tokens selection dock
                        PvPTokenDock(
                            myPlacements = myPlacements,
                            selectedToken = state.selectedToken,
                            onSelectToken = { viewModel.selectToken(it) }
                        )

                        // Action Panel: Scout / Lock / Commit Buttons
                        PvPActionPanel(
                            scoutMode = state.scoutMode,
                            lockMode = state.lockMode,
                            scoutBudgetUsed = state.myScouts.isNotEmpty(),
                            lockBudgetUsed = myLocks.size >= 2,
                            locksCount = myLocks.size,
                            myReady = myReady,
                            opponentReady = oppReady,
                            opponentName = oppName,
                            onToggleScout = { viewModel.toggleScoutMode() },
                            onToggleLock = { viewModel.toggleLockMode() },
                            onCommit = { viewModel.commitReady() }
                        )
                    } else {
                        // Reveal / Clash Outcomes list
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .background(Color(0xFF0B0D15), shape = RoundedCornerShape(12.dp))
                                .border(1.dp, Border, shape = RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "BATTLE RESOLUTION & INCOME",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent,
                                fontFamily = FontFamily.Monospace
                            )

                            val latestResult = match.roundResults.lastOrNull()
                            if (latestResult != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Income Collected This Round:", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                                    Text(
                                        text = "+${latestResult.player1Income} You  /  +${latestResult.player2Income} Opponent",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PlayerTeal
                                    )
                                }

                                Divider(color = Border, thickness = 0.5.dp)

                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(latestResult.clashes) { clash ->
                                        val winnerLabel = when (clash.winner) {
                                            "player1" -> if (isP1) "You Win" else "Opponent Wins"
                                            "player2" -> if (!isP1) "You Win" else "Opponent Wins"
                                            else -> "Tie (Mutual Destruction)"
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Clash at Cell [${clash.position}]:",
                                                fontSize = 11.sp,
                                                color = Color.White.copy(alpha = 0.5f),
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = "$winnerLabel (${clash.player1Token ?: 0} vs ${clash.player2Token ?: 0})",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (clash.winner == "player1" && isP1 || clash.winner == "player2" && !isP1) PlayerTeal else AICrimson,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text("No clashes recorded yet.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.4f))
                            }

                            Button(
                                onClick = { viewModel.advanceRound() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("next_round_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PlayerTeal,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (match.currentRound >= 12) "VIEW MATCH SUMMARY" else "PROCEED TO ROUND ${match.currentRound + 1}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PvPScoreboard(
    p1Name: String,
    p1Score: Int,
    p1Ready: Boolean,
    p2Name: String,
    p2Score: Int,
    p2Ready: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0B0D15), shape = RoundedCornerShape(12.dp))
            .border(1.dp, Border, shape = RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Player 1
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
            Text(
                text = p1Name,
                fontSize = 12.sp,
                color = PlayerTeal,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$p1Score",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (p1Ready) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF152A20), shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("READY", fontSize = 8.sp, color = PlayerTeal, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Versus Divider
        Text(
            text = "VS",
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = Color.White.copy(alpha = 0.3f),
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Player 2
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            Text(
                text = p2Name,
                fontSize = 12.sp,
                color = AICrimson,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (p2Ready) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF2C1418), shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("READY", fontSize = 8.sp, color = AICrimson, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$p2Score",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun PvPGridBoard(
    board: com.example.domain.model.Board,
    myPlacements: Map<String, Int>,
    myLocks: List<String>,
    oppPlacements: Map<String, Int>,
    oppLocks: List<String>,
    myScouts: Map<String, String>,
    roundPhase: RoundPhase,
    myRole: String,
    selectedToken: Int?,
    scoutMode: Boolean,
    lockMode: Boolean,
    onCellClick: (Int, Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(Color(0xFF07080C), shape = RoundedCornerShape(12.dp))
            .border(1.dp, Border, shape = RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (r in 0 until 5) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (c in 0 until 5) {
                    val posStr = "$r,$c"
                    val cell = board.get(Position(r, c))

                    // Placements owned by player 1 (PLAYER) vs player 2 (AI)
                    val p1TokenVal = if (myRole == "player1") myPlacements[posStr] else oppPlacements[posStr]
                    val p2TokenVal = if (myRole == "player2") myPlacements[posStr] else oppPlacements[posStr]

                    val p1Locked = if (myRole == "player1") myLocks.contains(posStr) else oppLocks.contains(posStr)
                    val p2Locked = if (myRole == "player2") myLocks.contains(posStr) else oppLocks.contains(posStr)

                    // Scouting status
                    val scoutedResultStr = myScouts[posStr] // "YES" or "NO"
                    val isScouted = scoutedResultStr != null

                    // Determine cell color based on owner
                    val ownerColor = when (cell.owner) {
                        CellOwner.PLAYER -> PlayerTeal.copy(alpha = 0.12f)
                        CellOwner.AI -> AICrimson.copy(alpha = 0.12f)
                        CellOwner.NONE -> Surface
                    }

                    // Border determination
                    val cellBorderColor = when {
                        scoutMode -> IntelPurple.copy(alpha = 0.6f)
                        lockMode && myPlacements.containsKey(posStr) -> GoldAccent
                        isScouted -> if (scoutedResultStr == "YES") AICrimson else PlayerTeal
                        myRole == "player1" && myLocks.contains(posStr) -> GoldAccent
                        myRole == "player2" && myLocks.contains(posStr) -> GoldAccent
                        cell.owner == CellOwner.PLAYER -> PlayerTeal.copy(alpha = 0.4f)
                        cell.owner == CellOwner.AI -> AICrimson.copy(alpha = 0.4f)
                        else -> Border
                    }

                    val borderWidth = if (scoutMode || lockMode || isScouted) 1.5.dp else 1.dp

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ownerColor)
                            .border(BorderStroke(borderWidth, cellBorderColor), shape = RoundedCornerShape(8.dp))
                            .clickable { onCellClick(r, c) }
                            .testTag("cell_${r}_${c}"),
                        contentAlignment = Alignment.Center
                    ) {
                        // Cell Points displaying
                        Text(
                            text = cell.value.toString(),
                            color = when (cell.owner) {
                                CellOwner.PLAYER -> PlayerTeal.copy(alpha = 0.85f)
                                CellOwner.AI -> AICrimson.copy(alpha = 0.85f)
                                else -> Color(0xFF5F657A)
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 4.dp, end = 6.dp)
                        )

                        // Special region tags
                        if (cell.modifier is CellModifier.GoldenCell) {
                            Text(
                                text = "★ GOLD",
                                color = GoldAccent,
                                fontSize = 6.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 4.dp)
                            )
                        } else if (cell.modifier is CellModifier.Volatile) {
                            Text(
                                text = "⚡ VOLATILE",
                                color = Color(0xFFFF3B30),
                                fontSize = 6.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 4.dp)
                            )
                        } else if (cell.modifier is CellModifier.Battleground) {
                            Text(
                                text = "⚔ WAR",
                                color = Color(0xFF8E92A8),
                                fontSize = 6.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 4.dp)
                            )
                        }

                        // Scout indicators on cells
                        if (isScouted) {
                            Text(
                                text = if (scoutedResultStr == "YES") "⚠️ AI" else "✓ SAFE",
                                color = if (scoutedResultStr == "YES") AICrimson else PlayerTeal,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(top = 4.dp, start = 4.dp)
                                    .background(Color.Black.copy(alpha = 0.85f), shape = RoundedCornerShape(2.dp))
                                    .padding(horizontal = 2.dp, vertical = 1.dp)
                            )
                        }

                        // Owner labels
                        if (cell.owner != CellOwner.NONE) {
                            Text(
                                text = if (cell.owner == CellOwner.PLAYER) "P1" else "P2",
                                color = if (cell.owner == CellOwner.PLAYER) PlayerTeal.copy(alpha = 0.6f) else AICrimson.copy(alpha = 0.6f),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(bottom = 4.dp, start = 4.dp)
                            )
                        }

                        // Tokens in cells
                        if (roundPhase == RoundPhase.PLACEMENT) {
                            // Only draw MY token
                            val myTokenVal = myPlacements[posStr]
                            val myLocked = myLocks.contains(posStr)
                            if (myTokenVal != null) {
                                PvPTokenBadge(value = myTokenVal, isLocked = myLocked, isPlayer = myRole == "player1")
                            }
                        } else {
                            // Draw BOTH tokens in Reveal Phase
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (p1TokenVal != null) {
                                    PvPTokenBadge(value = p1TokenVal, isLocked = p1Locked, isPlayer = true, size = 18)
                                }
                                if (p2TokenVal != null) {
                                    PvPTokenBadge(value = p2TokenVal, isLocked = p2Locked, isPlayer = false, size = 18)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PvPTokenBadge(
    value: Int,
    isLocked: Boolean,
    isPlayer: Boolean,
    size: Int = 28
) {
    val baseColor = if (isPlayer) PlayerTeal else AICrimson
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(baseColor.copy(alpha = 0.25f), shape = RoundedCornerShape(50))
            .border(BorderStroke(1.5.dp, baseColor), shape = RoundedCornerShape(50)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = value.toString(),
                color = Color.White,
                fontSize = (size / 2.2).sp,
                fontWeight = FontWeight.Black
            )
            if (isLocked && size >= 24) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = GoldAccent,
                    modifier = Modifier.size(8.dp)
                )
            }
        }
    }
}

@Composable
fun PvPTokenDock(
    myPlacements: Map<String, Int>,
    selectedToken: Int?,
    onSelectToken: (Int?) -> Unit
) {
    val usedTokens = myPlacements.values.toSet()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "SELECT RECON TOKEN FOR DEPLOYMENT",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.5f),
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (tokenVal in 1..5) {
                val isUsed = usedTokens.contains(tokenVal)
                val isSelected = selectedToken == tokenVal

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(50))
                        .alpha(if (isUsed) 0.3f else 1.0f)
                        .background(
                            if (isSelected) PlayerTeal.copy(alpha = 0.3f) else Color(0xFF0B0D15)
                        )
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) PlayerTeal else Border,
                            shape = RoundedCornerShape(50)
                        )
                        .clickable(enabled = !isUsed) {
                            if (isSelected) onSelectToken(null) else onSelectToken(tokenVal)
                        }
                        .testTag("token_$tokenVal"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tokenVal.toString(),
                        color = if (isSelected) PlayerTeal else Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
fun PvPActionPanel(
    scoutMode: Boolean,
    lockMode: Boolean,
    scoutBudgetUsed: Boolean,
    lockBudgetUsed: Boolean,
    locksCount: Int,
    myReady: Boolean,
    opponentReady: Boolean,
    opponentName: String,
    onToggleScout: () -> Unit,
    onToggleLock: () -> Unit,
    onCommit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Scout Button
        Button(
            onClick = onToggleScout,
            enabled = !scoutBudgetUsed && !myReady,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .testTag("scout_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (scoutMode) IntelPurple else Color(0xFF0B0D15),
                contentColor = if (scoutMode) Color.Black else IntelPurple
            ),
            border = BorderStroke(1.dp, if (scoutMode) IntelPurple else Border),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Radar, contentDescription = "Scout", modifier = Modifier.size(18.dp))
                Text("SCOUT (0/1)", fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }

        // Lock Button
        Button(
            onClick = onToggleLock,
            enabled = !lockBudgetUsed && !myReady,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .testTag("lock_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (lockMode) GoldAccent else Color(0xFF0B0D15),
                contentColor = if (lockMode) Color.Black else GoldAccent
            ),
            border = BorderStroke(1.dp, if (lockMode) GoldAccent else Border),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Lock, contentDescription = "Lock", modifier = Modifier.size(18.dp))
                Text("LOCK ($locksCount/2)", fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }

        // Commit/Ready Button
        Button(
            onClick = onCommit,
            enabled = !myReady,
            modifier = Modifier
                .weight(2f)
                .fillMaxHeight()
                .testTag("commit_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (myReady) Color(0xFF1B231D) else PlayerTeal,
                contentColor = if (myReady) PlayerTeal else Color.Black
            ),
            border = BorderStroke(1.dp, if (myReady) PlayerTeal.copy(alpha = 0.5f) else Color.Transparent),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.FlashOn, contentDescription = "Commit", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (myReady) {
                        if (opponentReady) "RESOLVING..." else "AWAITING $opponentName"
                    } else "COMMIT STRATEGY",
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
