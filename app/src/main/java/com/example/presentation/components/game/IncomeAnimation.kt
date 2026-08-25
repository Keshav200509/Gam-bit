package com.example.presentation.components.game

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.CellOwner
import com.example.ui.theme.AICrimson
import com.example.ui.theme.PlayerTeal

@Composable
fun IncomeAnimation(
    value: Int,
    owner: CellOwner,
    trigger: Boolean,
    modifier: Modifier = Modifier
) {
    if (!trigger || owner == CellOwner.NONE) return

    val animTime = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        animTime.animateTo(
            targetValue = 800f, // 800ms duration
            animationSpec = tween(durationMillis = 800, easing = LinearEasing)
        )
    }

    val progress = animTime.value / 800f // 0.0 to 1.0

    // 1. Travel upward 50dp
    val travelDp = (progress * -50).dp

    // 2. Scale starts at 0.5 -> 1.2 -> 1.0 (pop effect)
    // We can define this using Keyframes:
    val scale = when {
        progress < 0.3f -> {
            // Scale from 0.5 to 1.2
            val localProgress = progress / 0.3f
            0.5f + (1.2f - 0.5f) * localProgress
        }
        progress < 0.6f -> {
            // Settle from 1.2 to 1.0
            val localProgress = (progress - 0.3f) / 0.3f
            1.2f - (1.2f - 1.0f) * localProgress
        }
        else -> 1.0f
    }

    // 3. Alpha fades out linearly over the last 300ms (from 500ms to 800ms, progress 0.625 to 1.0)
    val alpha = when {
        progress < 0.625f -> 1.0f
        else -> {
            val fadeProgress = (progress - 0.625f) / (1f - 0.625f)
            1.0f - fadeProgress
        }
    }

    val text = if (owner == CellOwner.PLAYER) "+$value" else "-$value"
    val color = if (owner == CellOwner.PLAYER) PlayerTeal else AICrimson

    if (progress < 1.0f) {
        Box(
            modifier = modifier
                .offset(y = travelDp)
                .scale(scale)
                .alpha(alpha),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = color,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
