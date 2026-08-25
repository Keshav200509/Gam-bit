package com.example.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.achievements.Achievement
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun AchievementNotification(
    achievement: Achievement,
    onDismiss: () -> Unit
) {
    val icon = getAchievementEmoji(achievement.id)

    LaunchedEffect(achievement) {
        delay(4000)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .border(2.dp, GoldAccent, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(
                containerColor = Surface
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon circle
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(GoldAccent.copy(alpha = 0.2f), Color.Transparent)
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .border(1.dp, GoldAccent, RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = icon, fontSize = 24.sp)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1.0f)) {
                    Text(
                        text = "ACHIEVEMENT UNLOCKED!",
                        color = GoldAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = achievement.title,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = achievement.description,
                        color = TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

// Queue Manager to hold multiple achievements and show them one at a time
@Composable
fun AchievementNotificationQueue(
    queue: List<Achievement>,
    onDismissFirst: () -> Unit
) {
    AnimatedVisibility(
        visible = queue.isNotEmpty(),
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        if (queue.isNotEmpty()) {
            AchievementNotification(
                achievement = queue.first(),
                onDismiss = onDismissFirst
            )
        }
    }
}

fun getAchievementEmoji(id: String): String {
    return when (id) {
        "first_win" -> "🏆"
        "strategist" -> "🎖️"
        "perfectionist" -> "🎯"
        "comeback_kid" -> "🔄"
        "scout_master" -> "📡"
        "lock_master" -> "🔒"
        "intel_analyst" -> "🧠"
        "bluff_caller" -> "👁️"
        "speed_demon" -> "⏱️"
        "iron_player" -> "⚓"
        "champion" -> "👑"
        "arena_master" -> "🌋"
        else -> "🌟"
    }
}
