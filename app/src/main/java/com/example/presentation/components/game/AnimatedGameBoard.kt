package com.example.presentation.components.game

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Board
import com.example.domain.model.CellModifier
import com.example.domain.model.CellOwner
import com.example.domain.model.ClashResult
import com.example.domain.model.GamePhase
import com.example.domain.model.Position
import com.example.domain.model.ScoutResult
import com.example.domain.model.Token
import com.example.presentation.components.ClashStage
import com.example.presentation.components.TokenBadge
import com.example.presentation.components.clashWinAnimation
import com.example.presentation.components.clashLoseAnimation
import com.example.presentation.components.clashTieAnimation
import com.example.presentation.components.shakeAnimation
import com.example.presentation.components.SoundAndHapticEntryPoint
import com.example.ui.theme.AICrimson
import com.example.ui.theme.Border
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.IntelPurple
import com.example.ui.theme.PlayerTeal
import com.example.ui.theme.Surface
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.delay

@Composable
fun AnimatedGameBoard(
    board: Board,
    playerPlacements: Map<Position, Token>,
    aiPlacements: Map<Position, Token>,
    scoutedPosition: Position?,
    scoutResult: ScoutResult?,
    playerLocks: List<Position>,
    scoutMode: Boolean,
    lockMode: Boolean,
    currentResolvingPosition: Position?,
    clashStage: ClashStage,
    clashResults: Map<Position, ClashResult>,
    phase: GamePhase,
    onCellClick: (Position) -> Unit,
    onCellLongPress: (Position) -> Unit,
    modifier: Modifier = Modifier,
    intelHintRegion: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(Color(0xFF07080C), shape = RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, Border), shape = RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (row in 0 until 5) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (col in 0 until 5) {
                    val position = Position(row, col)
                    val cell = board.get(position)
                    val playerToken = playerPlacements[position]
                    val aiToken = aiPlacements[position]
                    val isScouted = scoutedPosition == position
                    val isResolving = currentResolvingPosition == position
                    val isLocked = playerLocks.contains(position) || (playerToken?.isLocked == true)

                    val hasAdjacencyBonus = remember(board, cell.owner, position) {
                        if (cell.owner == CellOwner.NONE) false
                        else {
                            val neighbors = position.neighbors()
                            neighbors.any { board.get(it).owner == cell.owner }
                        }
                    }

                    val inIntelRegion = remember(intelHintRegion, row, col) {
                        intelHintRegion != null && getRegion(row, col) == intelHintRegion.lowercase()
                    }

                    AnimatedGameBoardCell(
                        position = position,
                        value = cell.value,
                        owner = cell.owner,
                        playerToken = playerToken,
                        aiToken = aiToken,
                        isScouted = isScouted,
                        scoutResult = scoutResult,
                        isLocked = isLocked,
                        scoutMode = scoutMode,
                        lockMode = lockMode,
                        isResolving = isResolving,
                        clashStage = clashStage,
                        clashResult = clashResults[position],
                        phase = phase,
                        board = board,
                        hasAdjacencyBonus = hasAdjacencyBonus,
                        onClick = { onCellClick(position) },
                        onLongClick = { onCellLongPress(position) },
                        modifier = Modifier.weight(1f),
                        inIntelRegion = inIntelRegion
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnimatedGameBoardCell(
    position: Position,
    value: Int,
    owner: CellOwner,
    playerToken: Token?,
    aiToken: Token?,
    isScouted: Boolean,
    scoutResult: ScoutResult?,
    isLocked: Boolean,
    scoutMode: Boolean,
    lockMode: Boolean,
    isResolving: Boolean,
    clashStage: ClashStage,
    clashResult: ClashResult?,
    phase: GamePhase,
    board: Board,
    hasAdjacencyBonus: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    inIntelRegion: Boolean = false
) {
    val context = LocalContext.current.applicationContext
    val entryPoint = remember(context) {
        EntryPointAccessors.fromApplication(context, SoundAndHapticEntryPoint::class.java)
    }
    val soundManager = entryPoint.soundManager()
    val hapticFeedback = entryPoint.hapticFeedback()

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 1. Cell Entrance Animation (Staggered scale 0.8 -> 1.0, 50ms delay per cell)
    val entranceScale = remember { Animatable(0.8f) }
    LaunchedEffect(Unit) {
        val index = position.row * 5 + position.col
        delay(index * 50L)
        entranceScale.animateTo(1.0f, animationSpec = tween(200, easing = EaseOutBack))
    }

    // 2. Cell Press Animation (Scale to 0.95, spring physics)
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cell_press"
    )

    // Resolving Pulsing Indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_resolving")
    val resolvingPulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(450),
            repeatMode = RepeatMode.Reverse
        ),
        label = "resolving_pulse"
    )

    // Locks pulsating alpha
    val lockPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lock_pulse"
    )

    // 3. Glow Pulse on High-Value cells (Subtle, 2s cycle)
    val isHighValue = value >= 3
    val highValuePulseAlpha by if (isHighValue) {
        infiniteTransition.animateFloat(
            initialValue = 0.05f,
            targetValue = 0.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "high_value_pulse"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    // Accumulating scale factors
    val baseScale = pressScale * entranceScale.value
    val finalScale = if (isResolving && clashStage == ClashStage.TOKENS_APPEAR) {
        baseScale * resolvingPulseScale
    } else {
        baseScale
    }

    val cellScale by animateFloatAsState(
        targetValue = finalScale,
        label = "cell_scale"
    )

    val cell = board.get(position)
    val isGoldenCell = cell.modifier is CellModifier.GoldenCell
    val isVolatile = cell.modifier is CellModifier.Volatile
    val isBattleground = cell.modifier is CellModifier.Battleground

    // 4. Owner Change Animation (Background color crossfade, 300ms)
    val targetBgColor = when (owner) {
        CellOwner.PLAYER -> PlayerTeal.copy(alpha = 0.15f)
        CellOwner.AI -> AICrimson.copy(alpha = 0.15f)
        CellOwner.NONE -> Surface
    }

    val animatedBgColor by animateColorAsState(
        targetValue = when {
            isGoldenCell -> GoldAccent.copy(alpha = 0.1f)
            isVolatile -> Color(0xFFFF3B30).copy(alpha = 0.08f)
            isBattleground -> Color(0xFF151722)
            else -> targetBgColor
        },
        animationSpec = tween(300),
        label = "owner_bg_crossfade"
    )

    val modifierBorderColor = when {
        isGoldenCell -> GoldAccent.copy(alpha = 0.6f)
        isVolatile -> Color(0xFFFF3B30).copy(alpha = 0.6f)
        isBattleground -> Color(0xFF5C5F74).copy(alpha = 0.5f)
        else -> null
    }

    // Border configuration
    val borderColor: Color
    val borderWidth: androidx.compose.ui.unit.Dp

    when {
        isResolving -> {
            borderColor = GoldAccent
            borderWidth = 2.5.dp
        }
        isLocked -> {
            borderColor = GoldAccent
            borderWidth = 2.dp
        }
        isScouted -> {
            borderColor = if (scoutResult == ScoutResult.CONTESTED) AICrimson else PlayerTeal
            borderWidth = 2.dp
        }
        lockMode && playerToken != null -> {
            borderColor = GoldAccent.copy(alpha = lockPulseAlpha)
            borderWidth = 2.dp
        }
        scoutMode -> {
            borderColor = IntelPurple.copy(alpha = 0.6f)
            borderWidth = 1.5.dp
        }
        owner == CellOwner.PLAYER -> {
            borderColor = modifierBorderColor ?: PlayerTeal.copy(alpha = 0.4f)
            borderWidth = if (modifierBorderColor != null) 1.5.dp else 1.dp
        }
        owner == CellOwner.AI -> {
            borderColor = modifierBorderColor ?: AICrimson.copy(alpha = 0.4f)
            borderWidth = if (modifierBorderColor != null) 1.5.dp else 1.dp
        }
        else -> {
            borderColor = modifierBorderColor ?: Border
            borderWidth = if (modifierBorderColor != null) 1.5.dp else 1.dp
        }
    }

    // Strengths matching ResolveClash
    val playerStrength = remember(playerToken, position, board, owner, isLocked, isVolatile, isBattleground) {
        if (playerToken == null) 0 else {
            val adjacencyBonus = if (isVolatile) 0 else {
                val neighbors = position.neighbors()
                neighbors.count { board.get(it).owner == CellOwner.PLAYER }
            }
            val fortifyBonus = if (isVolatile) 0 else {
                if (board.get(position).owner == CellOwner.PLAYER) board.get(position).fortify else 0
            }
            val lockBonus = if (isLocked) 2 else 0
            val battlegroundBonus = if (isBattleground && board.get(position).owner == CellOwner.PLAYER) 1 else 0
            playerToken.value + adjacencyBonus + fortifyBonus + lockBonus + battlegroundBonus
        }
    }

    val aiStrength = remember(aiToken, position, board, owner, isVolatile, isBattleground) {
        if (aiToken == null) 0 else {
            val adjacencyBonus = if (isVolatile) 0 else {
                val neighbors = position.neighbors()
                neighbors.count { board.get(it).owner == CellOwner.AI }
            }
            val fortifyBonus = if (isVolatile) 0 else {
                if (board.get(position).owner == CellOwner.AI) board.get(position).fortify else 0
            }
            val lockBonus = if (aiToken.isLocked) 2 else 0
            val battlegroundBonus = if (isBattleground && board.get(position).owner == CellOwner.AI) 1 else 0
            aiToken.value + adjacencyBonus + fortifyBonus + lockBonus + battlegroundBonus
        }
    }

    var cellModifier = modifier
        .fillMaxHeight()
        .scale(cellScale)
        .clip(RoundedCornerShape(8.dp))
        .background(animatedBgColor)
        .border(BorderStroke(borderWidth, borderColor), shape = RoundedCornerShape(8.dp))
        .combinedClickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            onClick = {
                // Play sound and trigger feedback
                if (scoutMode) {
                    soundManager.playScoutActivate()
                    hapticFeedback.scoutUsed()
                } else if (lockMode && playerToken != null) {
                    soundManager.playLockToggle()
                    hapticFeedback.lockToggled()
                } else {
                    soundManager.playMenuClick()
                    hapticFeedback.menuClick()
                }
                onClick()
            },
            onLongClick = onLongClick
        )
        .testTag("cell_${position.row}_${position.col}")

    // Existing resolving animators for safety
    if (isResolving && clashStage == ClashStage.OUTCOME_REVEALED && clashResult != null) {
        cellModifier = when (clashResult) {
            is ClashResult.PlayerWins, is ClashResult.PlayerUncontested, is ClashResult.PlayerDefends -> {
                cellModifier.clashWinAnimation(trigger = true)
            }
            is ClashResult.AIWins, is ClashResult.AIUncontested, is ClashResult.AIDefends -> {
                cellModifier.clashLoseAnimation(trigger = true)
            }
            is ClashResult.Tie -> {
                cellModifier.clashTieAnimation(trigger = true)
            }
        }
    }

    val isIntelRegionCell = inIntelRegion && phase == GamePhase.Placing

    Box(modifier = cellModifier) {
        // High Value glow pulse overlay underlay
        if (isHighValue && highValuePulseAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isGoldenCell) GoldAccent.copy(alpha = highValuePulseAlpha)
                        else PlayerTeal.copy(alpha = highValuePulseAlpha * 0.4f)
                    )
            )
        }

        // Region tint overlay
        if (isIntelRegionCell) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(BorderStroke(1.dp, Color(0xFFA78BFA).copy(alpha = 0.4f)), shape = RoundedCornerShape(8.dp))
                    .background(Color(0xFFA78BFA).copy(alpha = 0.12f))
            )
        }

        // Value in top right corner
        Text(
            text = value.toString(),
            color = when {
                owner == CellOwner.PLAYER -> PlayerTeal
                owner == CellOwner.AI -> AICrimson
                isIntelRegionCell -> Color(0xFFA78BFA)
                else -> Color(0xFF5F657A)
            },
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 4.dp, end = 6.dp)
        )

        // Adjacency dot indicator
        if (hasAdjacencyBonus) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 6.dp, start = 6.dp)
                    .size(8.dp)
                    .background(GoldAccent, shape = RoundedCornerShape(50))
            )
        }

        // Top left Lock or Scout overlay animations
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 4.dp, start = 4.dp)
        ) {
            if (isLocked) {
                LockAnimation(isLocked = isLocked)
            } else if (isScouted) {
                ScoutAnimation(isScouted = isScouted, result = scoutResult)
            } else if (scoutMode) {
                Text(
                    text = "⊕",
                    color = IntelPurple,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Cell modifier text
        if (cell.modifier !is CellModifier.None) {
            val (modText, modColor) = when (cell.modifier) {
                is CellModifier.GoldenCell -> Pair("★ GOLD", GoldAccent)
                is CellModifier.Volatile -> Pair("⚡ VOLATILE", Color(0xFFFF3B30))
                is CellModifier.Battleground -> Pair("⚔ WAR", Color(0xFF8E92A8))
                else -> Pair("", Color.White)
            }
            if (modText.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(modColor.copy(alpha = 0.12f), shape = RoundedCornerShape(4.dp))
                        .border(1.dp, modColor.copy(alpha = 0.35f), shape = RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = modText,
                        color = modColor,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Tokens display (staggered and animated)
        if (isResolving && clashStage >= ClashStage.TOKENS_APPEAR) {
            var playerTokenScale = 1.0f
            var aiTokenScale = 1.0f
            var playerTokenAlpha = 1.0f
            var aiTokenAlpha = 1.0f

            var tokenContainerModifier: Modifier = Modifier.align(Alignment.Center)

            if (clashStage == ClashStage.OUTCOME_REVEALED && clashResult != null) {
                when (clashResult) {
                    is ClashResult.PlayerWins, is ClashResult.PlayerUncontested, is ClashResult.PlayerDefends -> {
                        playerTokenScale = 1.25f
                        aiTokenAlpha = 0.0f
                    }
                    is ClashResult.AIWins, is ClashResult.AIUncontested, is ClashResult.AIDefends -> {
                        aiTokenScale = 1.25f
                        playerTokenAlpha = 0.0f
                    }
                    is ClashResult.Tie -> {
                        playerTokenAlpha = 0.0f
                        aiTokenAlpha = 0.0f
                        tokenContainerModifier = tokenContainerModifier.shakeAnimation(trigger = true)
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = tokenContainerModifier
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Token Entrance & Removal animations
                    val playerTokenScaleState by animateFloatAsState(
                        targetValue = if (playerToken != null) playerTokenScale else 0f,
                        animationSpec = if (playerToken != null) spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow) else tween(150),
                        label = "player_token_scale"
                    )
                    val playerTokenAlphaState by animateFloatAsState(
                        targetValue = if (playerToken != null) playerTokenAlpha else 0f,
                        animationSpec = tween(150),
                        label = "player_token_alpha"
                    )

                    if (playerToken != null) {
                        TokenBadge(
                            token = playerToken,
                            isPlayer = true,
                            size = 20,
                            modifier = Modifier
                                .scale(playerTokenScaleState)
                                .alpha(playerTokenAlphaState)
                        )
                    }

                    val aiTokenScaleState by animateFloatAsState(
                        targetValue = if (aiToken != null) aiTokenScale else 0f,
                        animationSpec = if (aiToken != null) spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow) else tween(150),
                        label = "ai_token_scale"
                    )
                    val aiTokenAlphaState by animateFloatAsState(
                        targetValue = if (aiToken != null) aiTokenAlpha else 0f,
                        animationSpec = tween(150),
                        label = "ai_token_alpha"
                    )

                    if (aiToken != null) {
                        TokenBadge(
                            token = aiToken,
                            isPlayer = false,
                            size = 20,
                            modifier = Modifier
                                .scale(aiTokenScaleState)
                                .alpha(aiTokenAlphaState)
                        )
                    }
                }

                if (clashStage >= ClashStage.STRENGTHS_SHOWN) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (playerToken != null) {
                            Text(
                                text = "S:$playerStrength",
                                color = PlayerTeal,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.alpha(playerTokenAlpha)
                            )
                        }
                        if (aiToken != null) {
                            Text(
                                text = "S:$aiStrength",
                                color = AICrimson,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.alpha(aiTokenAlpha)
                            )
                        }
                    }
                }
            }
        } else {
            // General token display with scale on entry / exit
            val hasPlayerToken = playerToken != null
            val playerTokenScaleState by animateFloatAsState(
                targetValue = if (hasPlayerToken) 1.0f else 0f,
                animationSpec = if (hasPlayerToken) spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow) else tween(150),
                label = "player_token_normal_scale"
            )

            val hasAiToken = aiToken != null
            val aiTokenScaleState by animateFloatAsState(
                targetValue = if (hasAiToken) 1.0f else 0f,
                animationSpec = if (hasAiToken) spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow) else tween(150),
                label = "ai_token_normal_scale"
            )

            if (playerToken != null && aiToken != null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TokenBadge(token = playerToken, isPlayer = true, size = 20, modifier = Modifier.scale(playerTokenScaleState))
                    TokenBadge(token = aiToken, isPlayer = false, size = 20, modifier = Modifier.scale(aiTokenScaleState))
                }
            } else if (playerToken != null) {
                TokenBadge(
                    token = playerToken,
                    isPlayer = true,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .scale(playerTokenScaleState)
                )
            } else if (aiToken != null) {
                TokenBadge(
                    token = aiToken,
                    isPlayer = false,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .scale(aiTokenScaleState)
                )
            }
        }

        // Owner label display with crossfade
        if (owner != CellOwner.NONE) {
            Text(
                text = if (owner == CellOwner.PLAYER) "YOU" else "AI",
                color = if (owner == CellOwner.PLAYER) PlayerTeal.copy(alpha = 0.7f) else AICrimson.copy(alpha = 0.7f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
            )
        }

        // Floating Income summary animation
        if (phase == GamePhase.IncomeSummary && owner != CellOwner.NONE) {
            IncomeAnimation(
                value = value,
                owner = owner,
                trigger = true,
                modifier = Modifier.align(Alignment.Center)
            )
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
