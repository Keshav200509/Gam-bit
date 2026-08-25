package com.example.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.PlayerTeal

@Composable
fun RoundProgressDots(
    currentRound: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (r in 1..12) {
            val isCompleted = r < currentRound
            val isCurrent = r == currentRound

            val baseModifier = Modifier.size(if (isCurrent) 10.dp else 8.dp)
            val finalModifier = if (isCurrent) {
                baseModifier.scale(pulseScale)
            } else {
                baseModifier
            }

            val bgModifier = when {
                isCompleted -> {
                    Modifier.background(PlayerTeal, shape = RoundedCornerShape(50))
                }
                isCurrent -> {
                    Modifier.background(GoldAccent, shape = RoundedCornerShape(50))
                }
                else -> {
                    Modifier
                        .border(1.dp, Color(0xFF343A40), shape = RoundedCornerShape(50))
                        .background(Color.Transparent)
                }
            }

            Box(
                modifier = finalModifier.then(bgModifier)
            )
        }
    }
}
