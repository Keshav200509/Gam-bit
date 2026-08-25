package com.example.presentation.components.game

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.PlayerTeal
import com.example.ui.theme.AICrimson
import kotlinx.coroutines.isActive
import java.util.Random

enum class ParticleType {
    VICTORY, DEFEAT, GOLDEN_CELL, STREAK
}

private data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var color: Color,
    var size: Float,
    var alpha: Float = 1f,
    var life: Float = 1f, // 1.0 down to 0.0
    var decay: Float = 0.01f,
    var angle: Float = 0f,
    var speed: Float = 0f
)

@Composable
fun ParticleEffect(
    trigger: Boolean,
    type: ParticleType,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    originX: Float? = null, // Center of the specific cell/source if available
    originY: Float? = null
) {
    if (!trigger) return

    val particles = remember { mutableStateListOf<Particle>() }
    val random = remember { Random() }

    val durationMs = when (type) {
        ParticleType.VICTORY -> 3000L
        ParticleType.DEFEAT -> 3000L
        ParticleType.GOLDEN_CELL -> 1000L
        ParticleType.STREAK -> 1500L
    }

    LaunchedEffect(trigger) {
        val count = when (type) {
            ParticleType.VICTORY -> 50
            ParticleType.DEFEAT -> 30
            ParticleType.GOLDEN_CELL -> 20
            ParticleType.STREAK -> 25
        }

        particles.clear()

        // Initialize particles
        for (i in 0 until count) {
            val p = when (type) {
                ParticleType.VICTORY -> {
                    // Teal confetti falling from the top
                    Particle(
                        x = random.nextFloat(), // normalized, will map to width in draw
                        y = -0.1f, // just above screen
                        vx = (random.nextFloat() - 0.5f) * 0.02f,
                        vy = 0.02f + random.nextFloat() * 0.03f,
                        color = if (random.nextBoolean()) PlayerTeal else PlayerTeal.copy(alpha = 0.6f),
                        size = 8f + random.nextFloat() * 12f,
                        decay = 0.005f + random.nextFloat() * 0.01f
                    )
                }
                ParticleType.DEFEAT -> {
                    // Crimson embers rising from the bottom
                    Particle(
                        x = random.nextFloat(), // normalized
                        y = 1.1f, // just below screen
                        vx = (random.nextFloat() - 0.5f) * 0.01f,
                        vy = -(0.01f + random.nextFloat() * 0.02f),
                        color = if (random.nextBoolean()) AICrimson else Color(0xFFFF5252),
                        size = 6f + random.nextFloat() * 10f,
                        decay = 0.008f + random.nextFloat() * 0.012f
                    )
                }
                ParticleType.GOLDEN_CELL -> {
                    // Gold sparkles radiating from center or cell
                    val ox = originX ?: 0.5f
                    val oy = originY ?: 0.5f
                    val angle = (random.nextFloat() * 2 * Math.PI).toFloat()
                    val speed = 0.01f + random.nextFloat() * 0.03f
                    Particle(
                        x = ox,
                        y = oy,
                        vx = Math.cos(angle.toDouble()).toFloat() * speed,
                        vy = Math.sin(angle.toDouble()).toFloat() * speed,
                        color = if (random.nextBoolean()) GoldAccent else Color(0xFFFFD700),
                        size = 4f + random.nextFloat() * 8f,
                        decay = 0.02f + random.nextFloat() * 0.03f
                    )
                }
                ParticleType.STREAK -> {
                    // Teal particles spiraling outwards
                    val ox = originX ?: 0.5f
                    val oy = originY ?: 0.5f
                    val angle = (random.nextFloat() * 2 * Math.PI).toFloat()
                    val speed = 0.005f + random.nextFloat() * 0.015f
                    Particle(
                        x = ox,
                        y = oy,
                        vx = Math.cos(angle.toDouble()).toFloat() * speed,
                        vy = Math.sin(angle.toDouble()).toFloat() * speed,
                        color = PlayerTeal,
                        size = 5f + random.nextFloat() * 7f,
                        decay = 0.015f + random.nextFloat() * 0.02f,
                        angle = angle,
                        speed = speed
                    )
                }
            }
            particles.add(p)
        }

        // Animation Loop (roughly 60fps)
        val startTime = System.currentTimeMillis()
        while (isActive && System.currentTimeMillis() - startTime < durationMs) {
            withFrameNanos { frameTime ->
                // Update particles
                for (i in particles.indices) {
                    val p = particles[i]
                    p.life -= p.decay
                    if (p.life <= 0f) {
                        p.alpha = 0f
                    } else {
                        p.alpha = p.life
                        p.x += p.vx
                        p.y += p.vy

                        if (type == ParticleType.STREAK) {
                            // Spiral effect: rotate velocity vector slightly
                            p.angle += 0.1f
                            p.vx = Math.cos(p.angle.toDouble()).toFloat() * (p.speed + (1f - p.life) * 0.02f)
                            p.vy = Math.sin(p.angle.toDouble()).toFloat() * (p.speed + (1f - p.life) * 0.02f)
                        } else if (type == ParticleType.VICTORY) {
                            // Gentle wind flutter
                            p.vx += (random.nextFloat() - 0.5f) * 0.001f
                        } else if (type == ParticleType.DEFEAT) {
                            // Embers flicker
                            p.vx += (random.nextFloat() - 0.5f) * 0.001f
                        }
                    }
                }
            }
        }
        onComplete()
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        particles.forEach { p ->
            if (p.alpha > 0f) {
                val px = p.x * width
                val py = p.y * height
                drawCircle(
                    color = p.color.copy(alpha = p.alpha),
                    radius = p.size,
                    center = Offset(px, py)
                )
            }
        }
    }
}
