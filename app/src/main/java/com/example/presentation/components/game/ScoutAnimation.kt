package com.example.presentation.components.game

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ScoutResult
import com.example.ui.theme.AICrimson
import com.example.ui.theme.IntelPurple
import com.example.ui.theme.PlayerTeal
import kotlinx.coroutines.delay

@Composable
fun ScoutAnimation(
    isScouted: Boolean,
    result: ScoutResult?,
    modifier: Modifier = Modifier
) {
    if (!isScouted) return

    // Scanning wave animation state
    val waveProgress = remember { Animatable(0f) }
    val waveAlpha = remember { Animatable(1f) }

    // Result trigger & animation states
    var showResult by remember { mutableStateOf(false) }

    val resultScale = remember { Animatable(0f) }
    val resultShake = remember { Animatable(0f) }

    // Run scanning animation then show result
    LaunchedEffect(isScouted, result) {
        if (isScouted) {
            // Reset states
            showResult = false
            waveProgress.snapTo(0f)
            waveAlpha.snapTo(1f)
            resultScale.snapTo(0f)
            resultShake.snapTo(0f)

            // 1. Purple scanning wave radiates from center (300ms)
            waveProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(300, easing = LinearEasing)
            )
            waveAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(150)
            )

            // 2. Result appears
            showResult = true
            if (result == ScoutResult.CLEAR) {
                // Clear: scale bounce
                resultScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            } else if (result == ScoutResult.CONTESTED) {
                // Contested: scale then shake
                resultScale.animateTo(1f, animationSpec = tween(150))
                // Shake 5 times
                for (i in 0 until 5) {
                    val target = if (i % 2 == 0) 8f else -8f
                    resultShake.animateTo(target, animationSpec = tween(40))
                }
                resultShake.animateTo(0f, animationSpec = tween(40))
            }
        }
    }

    // Border color animates to result color
    val targetBorderColor = when (result) {
        ScoutResult.CLEAR -> PlayerTeal
        ScoutResult.CONTESTED -> AICrimson
        else -> Color.Transparent
    }

    val animatedBorderColor by animateColorAsState(
        targetValue = if (showResult) targetBorderColor else Color.Transparent,
        animationSpec = tween(300),
        label = "scout_border_color"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .border(
                width = if (showResult) 2.dp else 0.dp,
                color = animatedBorderColor,
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        // Purple scanning wave
        if (waveProgress.value > 0f && waveAlpha.value > 0f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val maxRadius = size.minDimension / 1.1f
                drawCircle(
                    color = IntelPurple,
                    radius = maxRadius * waveProgress.value,
                    alpha = waveAlpha.value * 0.4f
                )
            }
        }

        // Scout Result Indicator
        if (showResult && result != null) {
            val text = if (result == ScoutResult.CLEAR) "✓" else "!"
            val color = if (result == ScoutResult.CLEAR) PlayerTeal else AICrimson

            Box(
                modifier = Modifier
                    .scale(resultScale.value)
                    .graphicsLayer(translationX = resultShake.value)
                    .size(24.dp)
                    .background(color.copy(alpha = 0.2f), shape = CircleShape)
                    .border(1.dp, color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    color = color,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.offset(y = if (result == ScoutResult.CLEAR) (-1).dp else 0.dp)
                )
            }
        }
    }
}
