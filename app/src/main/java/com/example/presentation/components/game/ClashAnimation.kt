package com.example.presentation.components.game

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ClashResult
import com.example.presentation.components.SoundAndHapticEntryPoint
import com.example.ui.theme.AICrimson
import com.example.ui.theme.Background
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.PlayerTeal
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ClashAnimation(
    playerToken: Int,
    opponentToken: Int,
    playerStrength: Int,
    opponentStrength: Int,
    result: ClashResult,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current.applicationContext
    val entryPoint = remember(context) {
        EntryPointAccessors.fromApplication(context, SoundAndHapticEntryPoint::class.java)
    }
    val soundManager = entryPoint.soundManager()
    val hapticFeedback = entryPoint.hapticFeedback()

    // Animation States
    val containerAlpha = remember { Animatable(1f) }
    val tokenScale = remember { Animatable(0f) }
    val flipRotation = remember { Animatable(180f) } // 180 = face down, 0 = face up
    val bonusOffset = remember { Animatable(60f) } // Offset for bonus numbers
    val bonusAlpha = remember { Animatable(0f) }
    
    val pStrengthValue = remember { mutableStateOf(playerToken) }
    val oStrengthValue = remember { mutableStateOf(opponentToken) }

    // Shake offsets for Clashing
    val shakeX = remember { Animatable(0f) }
    val shakeY = remember { Animatable(0f) }

    // Outcome reveal states
    val winnerGlowPulse = remember { Animatable(0f) }
    val playerShatterScale = remember { Animatable(1f) }
    val opponentShatterScale = remember { Animatable(1f) }
    val playerAlpha = remember { Animatable(1f) }
    val opponentAlpha = remember { Animatable(1f) }

    // Spark / Particle trigger
    var showSparks by remember { mutableStateOf(false) }

    val winner = remember(result) {
        when (result) {
            is ClashResult.PlayerWins, ClashResult.PlayerDefends -> "player"
            is ClashResult.AIWins, ClashResult.AIDefends -> "opponent"
            else -> "tie"
        }
    }

    LaunchedEffect(Unit) {
        // --- 0ms: Both tokens appear in center, face-down (scale 0 → 1.2 → 1.0, 300ms) ---
        soundManager.playTokenPlace()
        hapticFeedback.tokenSelected()
        tokenScale.animateTo(
            targetValue = 1.0f,
            animationSpec = keyframes {
                durationMillis = 300
                0f at 0 with FastOutSlowInEasing
                1.2f at 200 with FastOutSlowInEasing
                1.0f at 300
            }
        )

        // --- 500ms: Reveal transition — tokens flip over to show values (300ms 3D rotation) ---
        delay(200)
        soundManager.playScreenTransition()
        flipRotation.animateTo(0f, animationSpec = tween(300, easing = LinearEasing))

        // --- 1000ms: Adjacency/lock bonuses added (numbers float in & merge, 400ms) ---
        delay(200)
        bonusAlpha.snapTo(1f)
        // Merge animation
        launch {
            bonusOffset.animateTo(0f, animationSpec = tween(400, easing = EaseInQuad))
            bonusAlpha.animateTo(0f, animationSpec = tween(100))
        }

        delay(400) // Wait for merge to complete
        // Update display strengths to total calculated values
        pStrengthValue.value = playerStrength
        oStrengthValue.value = opponentStrength
        soundManager.playLockToggle() // High-pitched merge sound effect
        hapticFeedback.lockToggled()

        // --- 1600ms: Clashing (shake, clash sounds, particle sparks fly, 500ms) ---
        delay(200)
        showSparks = true

        // Play sound & vibration based on outcome
        when (winner) {
            "player" -> {
                soundManager.playClashWin()
                hapticFeedback.clashWon()
            }
            "opponent" -> {
                soundManager.playClashLose()
                hapticFeedback.clashLost()
            }
            else -> {
                soundManager.playClashTie()
                hapticFeedback.clashTied()
            }
        }

        // Severe clash shake loop
        launch {
            val shakeDuration = 500L
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < shakeDuration) {
                shakeX.snapTo((Math.random() * 20 - 10).toFloat())
                shakeY.snapTo((Math.random() * 20 - 10).toFloat())
                delay(30)
            }
            shakeX.animateTo(0f, animationSpec = tween(50))
            shakeY.animateTo(0f, animationSpec = tween(50))
        }

        delay(700) // End clashing

        // --- 2300ms: Outcome revealed — loser token shatters, winner glows ---
        when (winner) {
            "player" -> {
                launch {
                    opponentShatterScale.animateTo(0f, animationSpec = tween(300, easing = EaseOutQuad))
                    opponentAlpha.animateTo(0f, animationSpec = tween(300))
                }
                winnerGlowPulse.animateTo(1f, animationSpec = tween(400, easing = EaseOutQuad))
            }
            "opponent" -> {
                launch {
                    playerShatterScale.animateTo(0f, animationSpec = tween(300, easing = EaseOutQuad))
                    playerAlpha.animateTo(0f, animationSpec = tween(300))
                }
                winnerGlowPulse.animateTo(1f, animationSpec = tween(400, easing = EaseOutQuad))
            }
            "tie" -> {
                // Both shatter
                launch {
                    playerShatterScale.animateTo(0f, animationSpec = tween(300, easing = EaseOutQuad))
                    playerAlpha.animateTo(0f, animationSpec = tween(300))
                }
                launch {
                    opponentShatterScale.animateTo(0f, animationSpec = tween(300, easing = EaseOutQuad))
                    opponentAlpha.animateTo(0f, animationSpec = tween(300))
                }
            }
        }

        // --- 3000ms: Fade out & onComplete() ---
        delay(700)
        containerAlpha.animateTo(0f, animationSpec = tween(300))
        onComplete()
    }

    if (containerAlpha.value > 0f) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f * containerAlpha.value))
                .alpha(containerAlpha.value),
            contentAlignment = Alignment.Center
        ) {
            // Spark Canvas background
            if (showSparks) {
                ParticleEffect(
                    trigger = showSparks,
                    type = when (winner) {
                        "player" -> ParticleType.VICTORY
                        "opponent" -> ParticleType.DEFEAT
                        else -> ParticleType.STREAK
                    },
                    onComplete = {}
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "CLASH RESOLUTION",
                    color = GoldAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                    modifier = Modifier.padding(bottom = 40.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // PLAYER TOKEN (LEFT)
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                rotationY = flipRotation.value
                                translationX = shakeX.value
                                translationY = shakeY.value
                            }
                            .scale(tokenScale.value * playerShatterScale.value)
                            .alpha(playerAlpha.value),
                        contentAlignment = Alignment.Center
                    ) {
                        if (flipRotation.value > 90f) {
                            // CARD BACK (Face down)
                            TokenCardBack(color = PlayerTeal)
                        } else {
                            // CARD FRONT (Face up)
                            TokenCardFront(
                                value = playerToken,
                                strength = pStrengthValue.value,
                                label = "YOU",
                                color = PlayerTeal,
                                isWinner = winner == "player",
                                glowPulse = winnerGlowPulse.value
                            )
                        }

                        // Floating Adjacency Bonus indicator
                        if (bonusAlpha.value > 0f && playerStrength > playerToken) {
                            val bonusAmt = playerStrength - playerToken
                            Text(
                                text = "+$bonusAmt",
                                color = PlayerTeal,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .offset(y = (-bonusOffset.value).dp)
                                    .alpha(bonusAlpha.value)
                            )
                        }
                    }

                    Text(
                        text = "VS",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.graphicsLayer {
                            translationX = shakeX.value / 2f
                            translationY = shakeY.value / 2f
                        }
                    )

                    // OPPONENT TOKEN (RIGHT)
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                rotationY = flipRotation.value
                                translationX = -shakeX.value
                                translationY = -shakeY.value
                            }
                            .scale(tokenScale.value * opponentShatterScale.value)
                            .alpha(opponentAlpha.value),
                        contentAlignment = Alignment.Center
                    ) {
                        if (flipRotation.value > 90f) {
                            // CARD BACK (Face down)
                            TokenCardBack(color = AICrimson)
                        } else {
                            // CARD FRONT (Face up)
                            TokenCardFront(
                                value = opponentToken,
                                strength = oStrengthValue.value,
                                label = "AI",
                                color = AICrimson,
                                isWinner = winner == "opponent",
                                glowPulse = winnerGlowPulse.value
                            )
                        }

                        // Floating Adjacency Bonus indicator
                        if (bonusAlpha.value > 0f && opponentStrength > opponentToken) {
                            val bonusAmt = opponentStrength - opponentToken
                            Text(
                                text = "+$bonusAmt",
                                color = AICrimson,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .offset(y = (-bonusOffset.value).dp)
                                    .alpha(bonusAlpha.value)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Clash Description text banner
                if (flipRotation.value < 90f) {
                    val desc = when (result) {
                        is ClashResult.PlayerWins -> "PLAYER WINS THE CLASH!"
                        is ClashResult.AIWins -> "AI WINS THE CLASH!"
                        is ClashResult.Tie -> "MUTUAL DESTRUCTION!"
                        is ClashResult.PlayerDefends -> "PLAYER DEFENDS CELL!"
                        is ClashResult.AIDefends -> "AI DEFENDS CELL!"
                        else -> "CLASH!"
                    }

                    val color = when (winner) {
                        "player" -> PlayerTeal
                        "opponent" -> AICrimson
                        else -> GoldAccent
                    }

                    Text(
                        text = desc,
                        color = color,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun TokenCardBack(
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.size(100.dp, 140.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, color.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F111A)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(color.copy(alpha = 0.15f), shape = CircleShape)
                    .border(1.5.dp, color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "?",
                    color = color,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun TokenCardFront(
    value: Int,
    strength: Int,
    label: String,
    color: Color,
    isWinner: Boolean,
    glowPulse: Float,
    modifier: Modifier = Modifier
) {
    val containerBorder = if (isWinner && glowPulse > 0f) {
        BorderStroke((2 + 4 * glowPulse).dp, color)
    } else {
        BorderStroke(2.dp, color.copy(alpha = 0.7f))
    }

    Card(
        modifier = modifier
            .size(100.dp, 140.dp)
            .graphicsLayer {
                // Keep the card upright even if we rotate the container
                rotationY = 0f
            },
        shape = RoundedCornerShape(16.dp),
        border = containerBorder,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF07080C)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Label (YOU/AI)
            Text(
                text = label,
                color = color.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            // Huge Strength circle
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(color.copy(alpha = 0.1f), shape = CircleShape)
                    .border(1.5.dp, color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$strength",
                    color = color,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Small original token base value
            Text(
                text = "BASE: $value",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
