package com.example.presentation.screens.champion

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun ChampionScreen(
    onNavigateBack: () -> Unit,
    onNavigateToNewGamePlus: () -> Unit
) {
    // Pulse animation for gold elements
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    val textAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "textAlpha"
    )

    // Screen entrance animations
    var animateIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animateIn = true
    }

    val screenScale by animateFloatAsState(
        targetValue = if (animateIn) 1.0f else 0.8f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "screenScale"
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (animateIn) 1.0f else 0.0f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "contentAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Background,
                        Color(0xFF150D0A), // Reddish-tint for AI/Challenge theme
                        Background
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .scale(screenScale)
                .alpha(contentAlpha),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Unlocked Icon / Badge
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(glowScale)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(GoldAccent.copy(alpha = 0.4f), Color.Transparent)
                        ),
                        shape = RoundedCornerShape(50.dp)
                    )
                    .border(2.dp, GoldAccent, RoundedCornerShape(50.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🏆",
                    fontSize = 48.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "CHAMPION UNLOCKED",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = GoldAccent,
                letterSpacing = 3.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "You have conquered all 9 configurations of GAMBIT.",
                fontSize = 18.sp,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "You outplayed the machine in every arena, at every level.\nThere is nothing left to prove.",
                fontSize = 14.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Box for the dramatic announcement
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AICrimson.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .background(Surface, RoundedCornerShape(12.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NEW GAME+",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AICrimson,
                        letterSpacing = 4.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "The AI is now configured to its absolute limit.\nIt remembers your playstyle, lock frequency, timing, and preferred regions across sessions.\nIntel reliability is SUSPECT.",
                        fontSize = 13.sp,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "“The AI remembers you now.”",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AICrimson,
                        modifier = Modifier.alpha(textAlpha),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateBack,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextPrimary
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("ARENA MAP", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onNavigateToNewGamePlus,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AICrimson,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("START NG+", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
