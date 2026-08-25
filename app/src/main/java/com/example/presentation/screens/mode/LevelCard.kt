package com.example.presentation.screens.mode

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Arena
import com.example.domain.model.GameLevel
import com.example.presentation.components.SoundAndHapticEntryPoint
import dagger.hilt.android.EntryPointAccessors

@Composable
fun LevelCard(
    level: GameLevel,
    arena: Arena,
    wins: Int,
    bestScore: Int,
    isUnlocked: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current.applicationContext
    val entryPoint = remember(context) {
        EntryPointAccessors.fromApplication(context, SoundAndHapticEntryPoint::class.java)
    }
    val soundManager = entryPoint.soundManager()
    val hapticFeedback = entryPoint.hapticFeedback()

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1.0f,
        animationSpec = tween(150),
        label = "level_card_scale"
    )

    val accentColor = when (arena) {
        Arena.ASCENDENCY -> Color(0xFF00ADB5)
        Arena.CONFRONTATION -> Color(0xFF9E9E9E)
        Arena.OBLIVION -> Color(0xFFE63946)
    }

    val capabilityRange = when (level) {
        GameLevel.LEVEL_1 -> "AI Capability: -10 to 0"
        GameLevel.LEVEL_2 -> "AI Capability: -5 to +5"
        GameLevel.LEVEL_3 -> "AI Capability: 0 to +10"
    }

    val winsProgress = (wins / 2f).coerceIn(0f, 1f)
    val isComplete = wins >= 2

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B).copy(alpha = if (isSelected) 0.9f else 0.5f),
            contentColor = Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isUnlocked) accentColor.copy(alpha = if (isSelected) 1.0f else 0.4f) else Color.Gray.copy(alpha = 0.2f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                enabled = isUnlocked,
                onClick = {
                    soundManager.playMenuClick()
                    hapticFeedback.menuSelect()
                    onClick()
                }
            )
            .testTag("level_card_${level.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Large Level Number
                Text(
                    text = level.level.toString(),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isUnlocked) accentColor else Color.Gray,
                    modifier = Modifier.width(32.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = level.displayName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUnlocked) Color.White else Color.Gray
                        )
                        if (isComplete) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = capabilityRange,
                        fontSize = 11.sp,
                        color = Color.LightGray.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Progress bar & details
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LinearProgressIndicator(
                            progress = { winsProgress },
                            color = if (isComplete) Color(0xFF4CAF50) else accentColor,
                            trackColor = Color.White.copy(alpha = 0.1f),
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                        )
                        Text(
                            text = "Wins: $wins/2",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.LightGray
                        )
                    }

                    if (bestScore > 0) {
                        Text(
                            text = "Best Score: $bestScore",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFFD700), // Gold for best score
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            if (!isUnlocked) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
