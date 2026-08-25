package com.example.presentation.screens.result

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.example.domain.model.Board
import com.example.domain.model.CellOwner
import com.example.ui.theme.AICrimson
import com.example.ui.theme.Border
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.PlayerTeal
import com.example.ui.theme.Surface

@Composable
fun ResultScreen(
    playerScore: Int,
    aiScore: Int,
    board: Board,
    onPlayAgain: () -> Unit,
    modifier: Modifier = Modifier
) {
    val margin = kotlin.math.abs(playerScore - aiScore)
    val title = when {
        playerScore > aiScore -> "YOU OUTPLAYED THE MACHINE"
        aiScore > playerScore -> "THE MACHINE WINS"
        else -> "STALEMATE"
    }
    
    val titleColor = when {
        playerScore > aiScore -> PlayerTeal
        aiScore > playerScore -> AICrimson
        else -> GoldAccent
    }

    // Territory Calculations
    val playerCells = board.cells.flatten().count { it.owner == CellOwner.PLAYER }
    val playerPtsPerRound = board.cells.flatten().filter { it.owner == CellOwner.PLAYER }.sumOf { it.value }

    val aiCells = board.cells.flatten().count { it.owner == CellOwner.AI }
    val aiPtsPerRound = board.cells.flatten().filter { it.owner == CellOwner.AI }.sumOf { it.value }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF07080C))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Decrypted cyber logo/badge
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(titleColor.copy(alpha = 0.1f), shape = RoundedCornerShape(50))
                    .border(2.dp, titleColor.copy(alpha = 0.5f), shape = RoundedCornerShape(50)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "G",
                    color = titleColor,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Results Title
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = titleColor,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp,
                modifier = Modifier.testTag("result_title")
            )

            // Final score card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface, shape = RoundedCornerShape(12.dp))
                    .border(1.dp, Border, shape = RoundedCornerShape(12.dp))
                    .padding(20.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "FINAL SCOREBOARD",
                        color = Color(0xFF8B92A6),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("YOU", color = PlayerTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("$playerScore", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black)
                        }

                        Text(
                            text = "vs",
                            color = Border,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("MACHINE", color = AICrimson, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("$aiScore", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF151926), shape = RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when {
                                playerScore > aiScore -> "VICTORY MARGIN: $margin POINTS"
                                aiScore > playerScore -> "DEFEAT MARGIN: $margin POINTS"
                                else -> "OPERATIONAL EQUILIBRIUM REACHED"
                            },
                            color = titleColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // Operations breakdown metrics
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface, shape = RoundedCornerShape(12.dp))
                    .border(1.dp, Border, shape = RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "FIELD INTEL SUMMARY",
                        color = Color(0xFF8B92A6),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    Text(
                        text = "Your territory: $playerCells cells ($playerPtsPerRound pts/round)",
                        color = PlayerTeal,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.testTag("player_territory_breakdown")
                    )

                    Text(
                        text = "Machine territory: $aiCells cells ($aiPtsPerRound pts/round)",
                        color = AICrimson,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.testTag("ai_territory_breakdown")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Play Again Button
            Button(
                onClick = onPlayAgain,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PlayerTeal,
                    contentColor = Color(0xFF07080C)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("play_again_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh, 
                    contentDescription = "Restart simulation",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "RESTART SIMULATION",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
