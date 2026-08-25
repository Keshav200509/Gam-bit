package com.example.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AICrimson
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.PlayerTeal

enum class ClashStage {
    NONE,
    TOKENS_APPEAR,
    STRENGTHS_SHOWN,
    OUTCOME_REVEALED
}

@Composable
fun Modifier.clashWinAnimation(trigger: Boolean): Modifier {
    if (!trigger) return this
    val scaleAnim = remember { Animatable(1.0f) }
    LaunchedEffect(trigger) {
        scaleAnim.animateTo(1.1f, animationSpec = tween(250, easing = FastOutSlowInEasing))
        scaleAnim.animateTo(1.0f, animationSpec = tween(250, easing = FastOutSlowInEasing))
    }
    val glowColor by animateColorAsState(
        targetValue = PlayerTeal,
        animationSpec = tween(500),
        label = "win_glow"
    )
    return this
        .scale(scaleAnim.value)
        .border(2.dp, glowColor, RoundedCornerShape(8.dp))
}

@Composable
fun Modifier.clashLoseAnimation(trigger: Boolean): Modifier {
    if (!trigger) return this
    val scaleAnim = remember { Animatable(1.0f) }
    LaunchedEffect(trigger) {
        scaleAnim.animateTo(0.9f, animationSpec = tween(250, easing = FastOutSlowInEasing))
        scaleAnim.animateTo(1.0f, animationSpec = tween(250, easing = FastOutSlowInEasing))
    }
    val glowColor by animateColorAsState(
        targetValue = AICrimson,
        animationSpec = tween(500),
        label = "lose_glow"
    )
    return this
        .scale(scaleAnim.value)
        .border(2.dp, glowColor, RoundedCornerShape(8.dp))
}

@Composable
fun Modifier.clashTieAnimation(trigger: Boolean): Modifier {
    if (!trigger) return this
    val scaleAnim = remember { Animatable(1.0f) }
    LaunchedEffect(trigger) {
        scaleAnim.animateTo(1.05f, animationSpec = tween(150, easing = FastOutSlowInEasing))
        scaleAnim.animateTo(0.95f, animationSpec = tween(150, easing = FastOutSlowInEasing))
        scaleAnim.animateTo(1.0f, animationSpec = tween(200, easing = FastOutSlowInEasing))
    }
    val glowColor by animateColorAsState(
        targetValue = GoldAccent,
        animationSpec = tween(500),
        label = "tie_glow"
    )
    return this
        .scale(scaleAnim.value)
        .border(2.dp, glowColor, RoundedCornerShape(8.dp))
}

@Composable
fun Modifier.shakeAnimation(trigger: Boolean): Modifier {
    if (!trigger) return this
    val translationX = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        for (i in 0..4) {
            translationX.animateTo(if (i % 2 == 0) 6f else -6f, animationSpec = tween(40))
        }
        translationX.animateTo(0f, animationSpec = tween(40))
    }
    return this.graphicsLayer(translationX = translationX.value)
}
