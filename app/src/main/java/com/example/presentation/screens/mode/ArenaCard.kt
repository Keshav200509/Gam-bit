package com.example.presentation.screens.mode

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Dangerous
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
import com.example.presentation.components.SoundAndHapticEntryPoint
import dagger.hilt.android.EntryPointAccessors

@Composable
fun ArenaCard(
    arena: Arena,
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

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.98f
            isSelected -> 1.02f
            isHovered -> 1.01f
            else -> 1.0f
        },
        animationSpec = tween(150),
        label = "arena_card_scale"
    )

    val borderAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1.0f else if (isHovered) 0.6f else 0.2f,
        animationSpec = tween(150),
        label = "arena_card_border"
    )

    // Arena visual styles
    val accentColor = when (arena) {
        Arena.ASCENDENCY -> Color(0xFF00ADB5) // Teal
        Arena.CONFRONTATION -> Color(0xFF9E9E9E) // Neutral Grey
        Arena.OBLIVION -> Color(0xFFE63946) // Crimson
    }

    val icon = when (arena) {
        Arena.ASCENDENCY -> Icons.Default.Shield
        Arena.CONFRONTATION -> Icons.Default.CompareArrows
        Arena.OBLIVION -> Icons.Default.Dangerous
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B).copy(alpha = if (isSelected) 0.95f else 0.7f),
            contentColor = Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isUnlocked) accentColor.copy(alpha = borderAlpha) else Color.Gray.copy(alpha = 0.3f)
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
            .testTag("arena_card_${arena.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .graphicsLayer {
                            // subtle glow background for selected state
                            alpha = if (isUnlocked) 1.0f else 0.5f
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = arena.displayName,
                        tint = if (isUnlocked) accentColor else Color.Gray,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Column {
                    Text(
                        text = arena.displayName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isUnlocked) Color.White else Color.Gray
                    )
                    Text(
                        text = arena.description,
                        fontSize = 12.sp,
                        color = if (isUnlocked) Color.LightGray else Color.Gray.copy(alpha = 0.6f)
                    )
                }
            }

            if (!isUnlocked) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
