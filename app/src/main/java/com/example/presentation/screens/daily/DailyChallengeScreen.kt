package com.example.presentation.screens.daily

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Arena
import com.example.domain.model.GameLevel
import com.example.data.model.DailyChallengeResult
import com.example.presentation.components.AnimatedBackground
import com.example.presentation.components.GlowButton
import com.example.presentation.components.SoundAndHapticEntryPoint
import com.example.presentation.components.UserAvatar
import dagger.hilt.android.EntryPointAccessors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyChallengeScreen(
    viewModel: DailyChallengeViewModel,
    onBack: () -> Unit,
    onStartChallenge: (Arena, GameLevel) -> Unit,
    onNavigateToLeaderboard: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current.applicationContext
    val entryPoint = remember(context) {
        EntryPointAccessors.fromApplication(context, SoundAndHapticEntryPoint::class.java)
    }
    val soundManager = entryPoint.soundManager()
    val hapticFeedback = entryPoint.hapticFeedback()

    val goldColor = Color(0xFFFFD700)
    val goldOrange = Color(0xFFFFA500)

    // Pulsing alpha for the gold shimmer border
    val infiniteTransition = rememberInfiniteTransition(label = "gold_pulse")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gold_border_alpha"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "DAILY CHALLENGE",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.08f), CircleShape)
                            .testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Cosmic Deep Gold Background
            AnimatedBackground(
                colors = listOf(
                    Color(0xFF030712), // Deep Space Black
                    Color(0xFF1E1300), // Very Muted Gold/Brown Dark Space
                    Color(0xFF0F172A), // Slate Dark
                    Color(0xFF030712)
                )
            )

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = goldColor)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Date & Streak Banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E293B).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TODAY'S MISSION",
                                color = goldColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                text = state.todayChallenge?.date ?: "",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .background(goldColor.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Streak",
                                tint = goldOrange,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${state.streak} DAY STREAK",
                                color = goldColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    // Main Challenge Card with Pulsing Gold Border
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF111827).copy(alpha = 0.9f),
                            contentColor = Color.White
                        ),
                        border = BorderStroke(
                            width = 2.dp,
                            color = goldColor.copy(alpha = borderAlpha)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .testTag("challenge_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "SAME FOR ALL PLAYERS",
                                color = Color.LightGray.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )

                            Text(
                                text = state.todayChallenge?.arena?.displayName ?: "CONFRONTATION",
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "DIFFICULTY:",
                                    color = Color.LightGray,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = state.todayChallenge?.level?.displayName ?: "LEVEL 2",
                                    color = goldColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "Compete on a level playing field with a pre-set deterministic layout. Score maximum points to lead the daily leaderboard!",
                                color = Color.LightGray,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                            // If not completed: START button
                            if (!state.hasCompleted) {
                                GlowButton(
                                    text = "START CHALLENGE",
                                    icon = Icons.Default.PlayArrow,
                                    onClick = {
                                        soundManager.playMenuClick()
                                        hapticFeedback.menuClick()
                                        state.todayChallenge?.let {
                                            onStartChallenge(it.arena, it.level)
                                        }
                                    },
                                    glowColor = goldColor,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("start_challenge_button")
                                )
                            } else {
                                // Completed state summary
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "CHALLENGE COMPLETED",
                                        color = Color(0xFF4CAF50),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("YOUR SCORE", color = Color.LightGray, fontSize = 11.sp)
                                            Text(
                                                "${state.userResult?.playerScore ?: 0}",
                                                color = Color.White,
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                        Text("VS", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("AI SCORE", color = Color.LightGray, fontSize = 11.sp)
                                            Text(
                                                "${state.userResult?.aiScore ?: 0}",
                                                color = Color.White,
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                    if (state.userRank != null) {
                                        Text(
                                            text = "Rank: #${state.userRank} on Leaderboard",
                                            color = goldColor,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Countdown timer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Countdown",
                            tint = Color.LightGray,
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 6.dp)
                        )
                        Text(
                            text = "Next challenge in: ",
                            color = Color.LightGray,
                            fontSize = 13.sp
                        )
                        Text(
                            text = state.countdown,
                            color = goldColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Leaderboard Preview Card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1E293B).copy(alpha = 0.5f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = "Leaderboard",
                                        tint = goldColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "DAILY LEADERBOARD",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }

                                TextButton(onClick = {
                                    soundManager.playMenuClick()
                                    hapticFeedback.menuClick()
                                    onNavigateToLeaderboard()
                                }) {
                                    Text("VIEW FULL", color = goldColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (state.leaderboard.isEmpty()) {
                                Text(
                                    text = "No submissions yet today. Be the first!",
                                    color = Color.Gray,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp)
                                )
                            } else {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    state.leaderboard.take(5).forEach { entry ->
                                        LeaderboardPreviewRow(entry = entry)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun LeaderboardPreviewRow(entry: DailyChallengeResult) {
    val goldColor = Color(0xFFFFD700)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "#${entry.rank}",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (entry.rank == 1) goldColor else Color.LightGray,
                modifier = Modifier.width(28.dp)
            )

            UserAvatar(
                displayName = entry.displayName,
                photoUrl = entry.photoUrl,
                modifier = Modifier.size(28.dp)
            )

            Text(
                text = if (entry.displayName.isNotEmpty()) entry.displayName else "Player",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        Text(
            text = "${entry.playerScore} pts",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = goldColor
        )
    }
}
