package com.example.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.CellModifier
import com.example.domain.model.CellOwner
import com.example.domain.model.Position
import com.example.domain.model.RoundState
import com.example.ui.theme.AICrimson
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.PlayerTeal
import com.example.ui.theme.TextMuted
import kotlin.math.abs
import kotlin.math.max

@Composable
fun MomentumBar(
    playerScore: Int,
    aiScore: Int,
    lastRoundPlayerDelta: Int,
    lastRoundAiDelta: Int,
    scoreHistory: List<Pair<Int, Int>>,
    roundState: RoundState,
    modifier: Modifier = Modifier
) {
    // 1. Calculate projected income for both players
    val (playerProjectedIncome, aiProjectedIncome) = remember(roundState.board, roundState.roundNumber, roundState.incomeMultiplier) {
        var pIncome = 0
        var aIncome = 0
        for (r in 0 until 5) {
            for (c in 0 until 5) {
                val cell = roundState.board.get(Position(r, c))
                if (cell.owner == CellOwner.NONE) continue
                var cellVal = cell.value
                if (cell.modifier is CellModifier.GoldenCell) {
                    cellVal *= 2
                }
                if (cell.modifier is CellModifier.Volatile && roundState.roundNumber == 12) {
                    cellVal *= 2
                }
                
                when (cell.owner) {
                    CellOwner.PLAYER -> pIncome += cellVal
                    CellOwner.AI -> aIncome += cellVal
                    else -> {}
                }
            }
        }
        Pair(pIncome * roundState.incomeMultiplier, aIncome * roundState.incomeMultiplier)
    }

    // 2. Determine momentum direction and arrow
    val playerLeadLastRound = lastRoundPlayerDelta - lastRoundAiDelta
    val momentumDirection = when {
        playerLeadLastRound > 0 -> 1   // Player momentum (Up)
        playerLeadLastRound < 0 -> -1  // AI momentum (Down)
        else -> 0                      // Neutral/Balanced (Flat)
    }

    val (momentumIcon, momentumColor, momentumText) = when (momentumDirection) {
        1 -> Triple(Icons.Default.TrendingUp, PlayerTeal, "MOMENTUM: PLAYER (+${playerLeadLastRound})")
        -1 -> Triple(Icons.Default.TrendingDown, AICrimson, "MOMENTUM: MACHINE (+${abs(playerLeadLastRound)})")
        else -> Triple(Icons.Default.TrendingFlat, GoldAccent, "MOMENTUM: STABLE")
    }

    // 3. Proportional score bar layout ratios
    val animatedPlayerScore by animateFloatAsState(targetValue = playerScore.toFloat(), label = "p_score")
    val animatedAiScore by animateFloatAsState(targetValue = aiScore.toFloat(), label = "ai_score")
    val totalScore = animatedPlayerScore + animatedAiScore
    val scoreRatio = if (totalScore == 0f) 0.5f else animatedPlayerScore / totalScore

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .background(Color(0xFF0F111A), shape = RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, Color(0xFF1F2233)), shape = RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Row A: Scores, Momentum Indicator, AI State
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Player score Column
                Column(horizontalAlignment = Alignment.Start) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(PlayerTeal, shape = RoundedCornerShape(50))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "YOU",
                            color = TextMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    com.example.presentation.components.game.AnimatedScore(
                        targetScore = playerScore,
                        color = PlayerTeal,
                        fontSize = 20.sp,
                        modifier = Modifier.testTag("player_score")
                    )
                }

                // Momentum badge in the center
                Surface(
                    color = momentumColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, momentumColor.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = momentumIcon,
                            contentDescription = "Momentum Arrow",
                            tint = momentumColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = momentumText,
                            color = momentumColor,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.3.sp
                        )
                    }
                }

                // AI score Column
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "AI MACHINE",
                            color = TextMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(AICrimson, shape = RoundedCornerShape(50))
                        )
                    }
                    com.example.presentation.components.game.AnimatedScore(
                        targetScore = aiScore,
                        color = AICrimson,
                        fontSize = 20.sp,
                        modifier = Modifier.testTag("ai_score")
                    )
                }
            }

            // Row B: Proportional Score Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF1B1D2A))
            ) {
                Box(
                    modifier = Modifier
                        .weight(max(0.01f, scoreRatio))
                        .fillMaxHeight()
                        .background(PlayerTeal)
                )
                Box(
                    modifier = Modifier
                        .weight(max(0.01f, 1f - scoreRatio))
                        .fillMaxHeight()
                        .background(AICrimson)
                )
            }

            // Row C: Projected income & 8-bar historical sparkline
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Projection readout
                Column {
                    Text(
                        text = "+$playerProjectedIncome PROJECTED  |  AI +$aiProjectedIncome PROJECTED",
                        color = Color(0xFF8C90A6),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Sparkline representation (Last 8 rounds)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.height(18.dp)
                ) {
                    val maxHistoryBars = 8
                    val paddedHistory = remember(scoreHistory) {
                        val list = scoreHistory.takeLast(maxHistoryBars)
                        val needed = maxHistoryBars - list.size
                        List(needed) { Pair(-1, -1) } + list
                    }

                    // Calculate max absolute difference to scale the sparkline height dynamically
                    val maxDiff = remember(scoreHistory) {
                        val diffs = scoreHistory.map { abs(it.first - it.second) }
                        max(10, diffs.maxOrNull() ?: 10)
                    }

                    paddedHistory.forEachIndexed { _, scorePair ->
                        if (scorePair.first == -1) {
                            // Inactive pad bar
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(2.dp)
                                    .background(Color(0xFF202330), shape = RoundedCornerShape(0.5.dp))
                            )
                        } else {
                            // Active history bar
                            val p = scorePair.first
                            val a = scorePair.second
                            val diff = p - a
                            
                            // Height proportional to the diff
                            val ratio = abs(diff).toFloat() / maxDiff.toFloat()
                            val barHeightDp = max(3f, ratio * 16f).dp
                            val barColor = when {
                                diff > 0 -> PlayerTeal
                                diff < 0 -> AICrimson
                                else -> Color(0xFF7C829E)
                            }

                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(barHeightDp)
                                    .background(barColor, shape = RoundedCornerShape(0.5.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}
