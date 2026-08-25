package com.example.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AICrimson
import com.example.ui.theme.GoldAccent

@Composable
fun RoundTimer(
    timeRemaining: Long,
    scoutMode: Boolean,
    lockMode: Boolean,
    onTimerExpired: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isVisible = timeRemaining < 30L
    val isPaused = scoutMode || lockMode
    
    val ringColor = when {
        timeRemaining <= 15L -> AICrimson
        else -> GoldAccent
    }
    
    val progressFraction = if (isVisible) (timeRemaining.toFloat() / 30f).coerceIn(0f, 1f) else 1f
    val sweepAngle by animateFloatAsState(
        targetValue = progressFraction * 360f,
        label = "timer_sweep"
    )
    
    LaunchedEffect(timeRemaining) {
        if (timeRemaining <= 0L) {
            onTimerExpired()
        }
    }
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (isVisible && !isPaused) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp)
            ) {
                val strokeWidth = 4.dp.toPx()
                val diameter = size.minDimension - strokeWidth
                val topLeftOffset = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
                val size2D = Size(diameter, diameter)
                
                // Track
                drawArc(
                    color = ringColor.copy(alpha = 0.1f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeftOffset,
                    size = size2D,
                    style = Stroke(width = strokeWidth)
                )
                
                // Progress
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeftOffset,
                    size = size2D,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }
        
        content()
    }
}
