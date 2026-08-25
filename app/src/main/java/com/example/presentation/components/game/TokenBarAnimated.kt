package com.example.presentation.components.game

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.components.SoundAndHapticEntryPoint
import com.example.ui.theme.Background
import com.example.ui.theme.PlayerTeal
import dagger.hilt.android.EntryPointAccessors

@Composable
fun TokenBarAnimated(
    selectedToken: Int?,
    usedTokens: Set<Int>,
    onTokenClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current.applicationContext
    val entryPoint = remember(context) {
        EntryPointAccessors.fromApplication(context, SoundAndHapticEntryPoint::class.java)
    }
    val soundManager = entryPoint.soundManager()
    val hapticFeedback = entryPoint.hapticFeedback()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (tokenValue in 1..5) {
            val isUsed = usedTokens.contains(tokenValue)
            val isSelected = selectedToken == tokenValue

            TokenSlotAnimated(
                value = tokenValue,
                isSelected = isSelected,
                isUsed = isUsed,
                onClick = {
                    if (!isUsed) {
                        // Play sound and haptic based on selection vs deselection
                        if (isSelected) {
                            soundManager.playTokenSelect() // Deselect sound is also select but maybe different or same
                            hapticFeedback.tokenSelected()
                        } else {
                            soundManager.playTokenSelect()
                            hapticFeedback.tokenSelected()
                        }
                        onTokenClick(tokenValue)
                    }
                }
            )
        }
    }
}

@Composable
fun TokenSlotAnimated(
    value: Int,
    isSelected: Boolean,
    isUsed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Pulse animation for the glowing border of the selected token
    val infiniteTransition = rememberInfiniteTransition(label = "selected_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    // Smooth scaling factor
    val scaleFactor by animateFloatAsState(
        targetValue = if (isSelected) 1.2f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "token_scale"
    )

    val opacity = if (isUsed) 0.25f else 1.0f

    val containerColor = when {
        isSelected -> PlayerTeal
        else -> Color(0xFF111420)
    }

    val textColor = when {
        isSelected -> Color(0xFF07080C)
        isUsed -> PlayerTeal.copy(alpha = 0.4f)
        else -> PlayerTeal
    }

    val borderStroke = when {
        isSelected -> BorderStroke(2.dp, PlayerTeal)
        else -> BorderStroke(1.dp, PlayerTeal.copy(alpha = 0.5f))
    }

    Box(
        modifier = modifier
            .size(56.dp)
            .scale(scaleFactor)
            .alpha(opacity)
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .border(borderStroke, shape = RoundedCornerShape(12.dp))
            .then(
                if (!isUsed) {
                    Modifier.clickable(onClick = onClick)
                } else Modifier
            )
            .testTag("token_slot_$value"),
        contentAlignment = Alignment.Center
    ) {
        // Pulsing background glow for selected token
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 4.dp,
                        color = PlayerTeal.copy(alpha = glowAlpha),
                        shape = RoundedCornerShape(12.dp)
                    )
            )
        }

        Text(
            text = value.toString(),
            color = textColor,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black
        )

        // Draw strikethrough for used tokens
        if (isUsed) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(
                    color = PlayerTeal.copy(alpha = 0.6f),
                    start = Offset(12.dp.toPx(), size.height - 12.dp.toPx()),
                    end = Offset(size.width - 12.dp.toPx(), 12.dp.toPx()),
                    strokeWidth = 3.dp.toPx()
                )
            }
        }
    }
}
