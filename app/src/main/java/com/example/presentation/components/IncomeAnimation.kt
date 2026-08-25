package com.example.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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

    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutLinearInEasing)
        )
    }

    val offsetDecimal = animProgress.value // 0.0 to 1.0
    val offsetDp = (offsetDecimal * -60).dp // Rising up 60dp
    val alpha = 1f - offsetDecimal // Fading out

    val color = if (owner == CellOwner.PLAYER) PlayerTeal else AICrimson

    Box(
        modifier = modifier
            .offset(y = offsetDp)
            .alpha(alpha),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "+$value",
            color = color,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace
        )
    }
}
