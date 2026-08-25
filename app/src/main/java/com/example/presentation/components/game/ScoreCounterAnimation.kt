package com.example.presentation.components.game

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedScore(
    targetScore: Int,
    color: Color,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 24.sp
) {
    // Smoothly animate the target score from current to target over 800ms
    val animatedValue by animateIntAsState(
        targetValue = targetScore,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "score_count_up"
    )

    // Convert the animated value into a string representing digits
    val scoreString = animatedValue.toString()

    Row(modifier = modifier) {
        // Iterate through each character in the score string
        scoreString.forEachIndexed { index, char ->
            // Use key-based identification to ensure individual digits roll correctly
            AnimatedContent(
                targetState = char,
                transitionSpec = {
                    if (targetState > initialState) {
                        // Roll up
                        (slideInVertically { height -> -height } + fadeIn() togetherWith
                                slideOutVertically { height -> height } + fadeOut())
                    } else {
                        // Roll down
                        (slideInVertically { height -> height } + fadeIn() togetherWith
                                slideOutVertically { height -> -height } + fadeOut())
                    }.using(
                        SizeTransform(clip = false)
                    )
                },
                label = "digit_roll"
            ) { digitChar ->
                Text(
                    text = digitChar.toString(),
                    color = color,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace // Monospace prevents layout jitter during rolls
                )
            }
        }
    }
}
