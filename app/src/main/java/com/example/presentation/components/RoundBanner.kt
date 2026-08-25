package com.example.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberBasicTooltipState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.PlayerTeal
import kotlinx.coroutines.delay

@Composable
fun RoundBanner(
    roundNumber: Int,
    isVisible: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var animateIn by remember { mutableStateOf(false) }
    var finishedCalled by remember { mutableStateOf(false) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            animateIn = true
            delay(1200) // Show for 1.2s
            animateIn = false
            if (!finishedCalled) {
                finishedCalled = true
                onFinished()
            }
        }
    }

    val roundsRemaining = 12 - roundNumber

    AnimatedVisibility(
        visible = isVisible && animateIn,
        enter = fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.8f, animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 1.1f, animationSpec = tween(300)),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (!finishedCalled) {
                        finishedCalled = true
                        onFinished()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .padding(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0F111A))
                    .padding(horizontal = 24.dp, vertical = 32.dp)
            ) {
                Text(
                    text = "ROUND $roundNumber",
                    color = PlayerTeal,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("round_banner_title")
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                if (roundsRemaining <= 3) {
                    Text(
                        text = "FINAL STRETCH\n$roundsRemaining ROUNDS REMAINING",
                        color = GoldAccent,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        letterSpacing = 1.sp,
                        modifier = Modifier.testTag("round_banner_subtitle")
                    )
                } else {
                    Text(
                        text = "$roundsRemaining ROUNDS REMAINING",
                        color = Color(0xFF8B92A6),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("round_banner_subtitle")
                    )
                }
            }
        }
    }
}
