package com.example.presentation.theme

import androidx.compose.animation.core.*
import androidx.compose.ui.unit.IntSize

object GambitAnimations {
    // Entrance animations
    val logoFadeIn = tween<Float>(durationMillis = 800, easing = FastOutSlowInEasing)
    const val buttonStaggerDelay = 100 // ms between each button
    
    // Card animations
    val cardExpandDuration = tween<IntSize>(300)
    val cardFadeIn = tween<Float>(400)
    
    // Glow pulse
    val glowPulseSpec = infiniteRepeatable<Float>(
        animation = tween(2000, easing = LinearEasing),
        repeatMode = RepeatMode.Reverse
    )
}
