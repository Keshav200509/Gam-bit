package com.example.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawWithCache

@Composable
fun AnimatedBackground(
    colors: List<Color>,
    animationDurationMs: Int = 8000
) {
    if (colors.size < 2) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.firstOrNull() ?: Color.Black)
        )
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "background_shift")
    val animValue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(animationDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient_animation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithCache {
                val width = size.width
                val height = size.height

                // Slowly shift start and end coordinates of gradient to create a fluid, alive texture
                val startX = width * -0.2f + (width * 0.4f * animValue)
                val startY = height * -0.2f + (height * 0.4f * (1f - animValue))
                val endX = width * 0.8f + (width * 0.4f * (1f - animValue))
                val endY = height * 0.8f + (height * 0.4f * animValue)

                val brush = Brush.linearGradient(
                    colors = colors,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY)
                )

                onDrawBehind {
                    drawRect(brush)
                }
            }
    )
}
