package com.example.presentation.screens.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.GameEntity
import com.example.domain.achievements.Achievement
import com.example.ui.theme.AICrimson
import com.example.ui.theme.Border
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.PlayerTeal
import com.example.ui.theme.Surface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatsScreen(
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val games by viewModel.allGames.collectAsStateWithLifecycle()
    val stats by viewModel.aggregateStats.collectAsStateWithLifecycle()
    val unlockedAchievements by viewModel.unlockedAchievements.collectAsStateWithLifecycle()

    var showClearDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF06070B))
    ) {
        if (games.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                IconButton(
                    onClick = onBackClicked,
                    modifier = Modifier.testTag("stats_back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Go back",
                        tint = PlayerTeal
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Leaderboard,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "NO HISTORICAL RECORDS",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Complete operational missions to log strategic telemetry.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentPadding = PaddingValues(top = 24.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onBackClicked,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .testTag("stats_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Go back",
                                    tint = PlayerTeal
                                )
                            }
                            Text(
                                text = "STRATEGIC TELEMETRY",
                                style = MaterialTheme.typography.titleMedium,
                                color = PlayerTeal,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                        }

                        IconButton(
                            onClick = { showClearDialog = true },
                            modifier = Modifier.testTag("clear_stats_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = "Clear logs",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Grid of 4 aggregate cards
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            StatCard(
                                title = "GAMES PLAYED",
                                value = stats.totalGames.toString(),
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "WIN RATE",
                                value = String.format(Locale.US, "%.1f%%", stats.winRate),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            StatCard(
                                title = "AVG SCORE",
                                value = String.format(Locale.US, "%.1f", stats.avgScore),
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "BEST STREAK",
                                value = "${stats.longestStreak} W",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Simple Bar Chart Showing Wins vs Losses
                item {
                    WinsLossesChartCard(
                        wins = stats.wins,
                        losses = stats.losses,
                        draws = stats.draws
                    )
                }

                // Achievements Header
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "OPERATIONAL ACHIEVEMENTS",
                            style = MaterialTheme.typography.labelLarge,
                            color = GoldAccent,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Achievements Grid/List (Rows of 2)
                val achievements = Achievement.values().toList()
                items(achievements.chunked(2)) { pair: List<Achievement> ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pair.forEach { achievement ->
                            val isUnlocked = unlockedAchievements.contains(achievement.id)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isUnlocked) Surface else Surface.copy(alpha = 0.4f))
                                    .border(
                                        1.dp,
                                        if (isUnlocked) GoldAccent.copy(alpha = 0.5f) else Border.copy(alpha = 0.3f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isUnlocked) Icons.Default.CheckCircle else Icons.Default.Lock,
                                            contentDescription = if (isUnlocked) "Unlocked" else "Locked",
                                            tint = if (isUnlocked) GoldAccent else TextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = achievement.title,
                                            color = if (isUnlocked) TextPrimary else TextMuted,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = achievement.description,
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                        if (pair.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                // Historical log header
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "RECENT OPERATIONS (LAST 10)",
                            style = MaterialTheme.typography.labelLarge,
                            color = GoldAccent,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Recent games list
                val recentGames = games.take(10)
                items(recentGames) { game ->
                    GameLogItem(game = game)
                }
            }
        }
    }

    // Confirmation Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text(
                    text = "CLEAR ALL HISTORICAL LOGS?",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "This will wipe out all recorded history. Your stats, averages, and streaks will reset to zero.",
                    color = TextPrimary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearStats()
                        showClearDialog = false
                    },
                    modifier = Modifier.testTag("confirm_clear_button")
                ) {
                    Text("CONFIRM CLEAR", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("CANCEL", color = PlayerTeal)
                }
            },
            containerColor = Surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.dp, Border, RoundedCornerShape(16.dp))
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = PlayerTeal,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun WinsLossesChartCard(
    wins: Int,
    losses: Int,
    draws: Int
) {
    val total = wins + losses + draws
    val winsPct = if (total > 0) wins.toFloat() / total else 0f
    val lossesPct = if (total > 0) losses.toFloat() / total else 0f
    val drawsPct = if (total > 0) draws.toFloat() / total else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .border(1.dp, Border, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "ENGAGEMENT COMPARISON",
            style = MaterialTheme.typography.labelMedium,
            color = TextMuted,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Horizontal Stacked Bar Chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF141722))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                val winsWidth = w * winsPct
                val lossesWidth = w * lossesPct

                // Draw Wins (Teal)
                drawRoundRect(
                    color = PlayerTeal,
                    size = androidx.compose.ui.geometry.Size(winsWidth, h),
                    cornerRadius = CornerRadius(0f)
                )

                // Draw Draws (Muted gray)
                if (draws > 0) {
                    val drawsWidth = w * drawsPct
                    drawRoundRect(
                        color = Color.Gray,
                        topLeft = androidx.compose.ui.geometry.Offset(winsWidth, 0f),
                        size = androidx.compose.ui.geometry.Size(drawsWidth, h),
                        cornerRadius = CornerRadius(0f)
                    )
                }

                // Draw Losses (Crimson)
                if (losses > 0) {
                    drawRoundRect(
                        color = AICrimson,
                        topLeft = androidx.compose.ui.geometry.Offset(w - lossesWidth, 0f),
                        size = androidx.compose.ui.geometry.Size(lossesWidth, h),
                        cornerRadius = CornerRadius(0f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Legend row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LegendItem(label = "WINS ($wins)", color = PlayerTeal)
            LegendItem(label = "DRAWS ($draws)", color = Color.Gray)
            LegendItem(label = "LOSSES ($losses)", color = AICrimson)
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun GameLogItem(game: GameEntity) {
    val date = Date(game.timestamp)
    val formatter = SimpleDateFormat("MMM dd, HH:mm", Locale.US)
    val formattedDate = formatter.format(date)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = game.result,
                    color = when (game.result) {
                        "WIN" -> PlayerTeal
                        "LOSE" -> AICrimson
                        else -> TextMuted
                    },
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
                Text(
                    text = "• $formattedDate",
                    color = TextMuted,
                    fontSize = 12.sp
                )
                if (game.isDailyChallenge) {
                    Surface(
                        color = GoldAccent.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.border(1.dp, GoldAccent, RoundedCornerShape(4.dp))
                    ) {
                        Text(
                            text = "DAILY",
                            color = GoldAccent,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Clashes Won: ${game.clashesWon}/${game.clashesWon + game.clashesLost} | Scouts: ${game.scoutUses} | Locks: ${game.lockUses}",
                color = TextMuted,
                fontSize = 11.sp
            )
        }

        Text(
            text = "${game.playerScore} - ${game.aiScore}",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Black
        )
    }
}
