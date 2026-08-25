package com.example.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.LateGameEventType
import com.example.domain.model.RoundState
import com.example.ui.theme.GoldAccent

@Composable
fun LateGameEventBanner(
    roundState: RoundState,
    modifier: Modifier = Modifier
) {
    val eventType = roundState.lateGameEventType
    if (eventType == LateGameEventType.NONE || roundState.lateGameMessage.isEmpty()) return

    // Pulse animation for critical alerts
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_trans")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Configuration of banner graphics based on event type
    val (icon, tintColor, gradientBrush, labelText) = when (eventType) {
        LateGameEventType.GOLDEN_CELL -> {
            Quadruple(
                Icons.Default.Star,
                GoldAccent,
                Brush.horizontalGradient(listOf(Color(0xFF2C2205), Color(0xFF141103))),
                "GOLDEN CELL ANOMALY"
            )
        }
        LateGameEventType.VOLATILE -> {
            Quadruple(
                Icons.Default.Bolt,
                Color(0xFFFF3B30),
                Brush.horizontalGradient(listOf(Color(0xFF2B0B0C), Color(0xFF150304))),
                "VOLATILE FUSION CORE"
            )
        }
        LateGameEventType.DOUBLE_STAKES -> {
            Quadruple(
                Icons.Default.LocalFireDepartment,
                Color(0xFFFF9500),
                Brush.horizontalGradient(listOf(Color(0xFF2E1702), Color(0xFF140800))),
                "DOUBLE INCOME STAKES"
            )
        }
        LateGameEventType.FINAL_GAMBIT -> {
            Quadruple(
                Icons.Default.Gavel,
                Color(0xFFE52E2E),
                Brush.horizontalGradient(listOf(Color(0xFF2B0303), Color(0xFF100101))),
                "FINAL GAMBIT MODE"
            )
        }
        else -> Quadruple(Icons.Default.Star, Color.White, Brush.horizontalGradient(listOf(Color.Black, Color.Black)), "")
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .scale(pulseScale)
            .testTag("late_game_event_banner"),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.2.dp, tintColor.copy(alpha = 0.6f)),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(gradientBrush)
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Glow effect around the icon
                Surface(
                    color = tintColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(36.dp),
                    border = BorderStroke(1.dp, tintColor.copy(alpha = 0.3f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = labelText,
                            tint = tintColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = labelText,
                        color = tintColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = roundState.lateGameMessage,
                        color = Color(0xFFDCDFEF),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif,
                        lineHeight = 13.sp
                    )
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
