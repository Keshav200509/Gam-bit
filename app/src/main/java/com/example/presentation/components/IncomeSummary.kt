package com.example.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Board
import com.example.domain.model.CellOwner
import com.example.ui.theme.AICrimson
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.PlayerTeal
import com.example.ui.theme.Surface

@Composable
fun IncomeSummary(
    isVisible: Boolean,
    board: Board,
    playerIncome: Int,
    aiIncome: Int,
    roundNumber: Int,
    aiCapabilityModifier: Int,
    modifier: Modifier = Modifier
) {
    val playerCellsCount = board.cells.flatten().count { it.owner == CellOwner.PLAYER }
    val aiCellsCount = board.cells.flatten().count { it.owner == CellOwner.AI }
    val roundsLeft = 12 - roundNumber

    val capModSign = if (aiCapabilityModifier >= 0) "+$aiCapabilityModifier" else "$aiCapabilityModifier"

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .background(Color(0xFF0C101B), shape = RoundedCornerShape(12.dp))
                .border(1.5.dp, GoldAccent.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp))
                .padding(14.dp)
                .testTag("income_summary_overlay")
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "ROUND RESOLUTION SUMMARY",
                    color = GoldAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Player Income Column
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "YOU",
                            color = PlayerTeal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "+$playerIncome pts",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "from $playerCellsCount cells",
                            color = PlayerTeal.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .width(1.dp)
                            .background(Color(0xFF1F2633))
                    )

                    // AI Income Column
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "AI MACHINE",
                            color = AICrimson,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "+$aiIncome pts",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "from $aiCellsCount cells",
                            color = AICrimson.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF151926), shape = RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Rounds Left: $roundsLeft",
                            color = Color(0xFF8B92A6),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "AI Capability: $capModSign%",
                            color = if (aiCapabilityModifier >= 0) AICrimson else PlayerTeal,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
