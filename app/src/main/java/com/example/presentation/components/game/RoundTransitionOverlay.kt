package com.example.presentation.components.game

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.components.SoundAndHapticEntryPoint
import com.example.ui.theme.GoldAccent
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RoundTransitionOverlay(
    roundNumber: Int,
    roundsRemaining: Int,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current.applicationContext
    val entryPoint = remember(context) {
        EntryPointAccessors.fromApplication(context, SoundAndHapticEntryPoint::class.java)
    }
    val soundManager = entryPoint.soundManager()

    // Animation progress states
    val overlayAlpha = remember { Animatable(0f) }
    val roundLabelAlpha = remember { Animatable(0f) }
    val roundNumberScale = remember { Animatable(0.6f) }
    val roundNumberAlpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        soundManager.playRoundComplete()
        // Fast, smooth entrance (150ms)
        launch { overlayAlpha.animateTo(0.85f, animationSpec = tween(150)) }
        launch { roundLabelAlpha.animateTo(0.7f, animationSpec = tween(150)) }
        launch { roundNumberAlpha.animateTo(1f, animationSpec = tween(150)) }
        launch {
            roundNumberScale.animateTo(
                targetValue = 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
        launch { subtitleAlpha.animateTo(1.0f, animationSpec = tween(150)) }

        // Brief display hold (500ms)
        delay(600)

        // Fast fade out (150ms)
        launch { overlayAlpha.animateTo(0f, animationSpec = tween(150)) }
        launch { roundLabelAlpha.animateTo(0f, animationSpec = tween(150)) }
        launch { roundNumberScale.animateTo(0.9f, animationSpec = tween(150)) }
        launch { roundNumberAlpha.animateTo(0f, animationSpec = tween(150)) }
        launch { subtitleAlpha.animateTo(0f, animationSpec = tween(150)) }

        delay(150)
        onComplete()
    }

    if (overlayAlpha.value > 0f) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = overlayAlpha.value))
                .clickable { onComplete() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "ROUND",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                    modifier = Modifier.alpha(roundLabelAlpha.value)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "$roundNumber",
                    color = if (roundsRemaining <= 3) GoldAccent else Color.White,
                    fontSize = 96.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .scale(roundNumberScale.value)
                        .alpha(roundNumberAlpha.value)
                )

                Spacer(modifier = Modifier.height(16.dp))

                val subtitleText = if (roundsRemaining <= 3) {
                    "FINAL STRETCH • $roundsRemaining ROUNDS LEFT"
                } else {
                    "$roundsRemaining ROUNDS REMAINING"
                }

                Text(
                    text = subtitleText,
                    color = if (roundsRemaining <= 3) GoldAccent else Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.sp,
                    modifier = Modifier.alpha(subtitleAlpha.value)
                )

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "TAP TO SKIP",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
