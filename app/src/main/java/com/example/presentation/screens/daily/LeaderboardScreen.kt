package com.example.presentation.screens.daily

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firestore.GameResult
import com.example.data.model.DailyChallengeResult
import com.example.presentation.components.AnimatedBackground
import com.example.presentation.components.SoundAndHapticEntryPoint
import com.example.presentation.components.UserAvatar
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class LeaderboardFilter {
    TODAY, THIS_WEEK, ALL_TIME
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    viewModel: DailyChallengeViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current.applicationContext
    val entryPoint = remember(context) {
        EntryPointAccessors.fromApplication(context, SoundAndHapticEntryPoint::class.java)
    }
    val soundManager = entryPoint.soundManager()
    val hapticFeedback = entryPoint.hapticFeedback()

    var selectedFilter by remember { mutableStateOf(LeaderboardFilter.TODAY) }
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val goldColor = Color(0xFFFFD700)

    // Generate weekly & all-time mock leaderboard so it never shows empty
    val weeklyLeaderboard = remember {
        listOf(
            DailyChallengeResult("1", "", 120, 42, GameResult.WIN, 1, displayName = "Hyperion", photoUrl = null),
            DailyChallengeResult("2", "", 115, 50, GameResult.WIN, 2, displayName = "Kaelen", photoUrl = null),
            DailyChallengeResult("3", "", 110, 48, GameResult.WIN, 3, displayName = "Sovereign", photoUrl = null),
            DailyChallengeResult("4", "", 105, 52, GameResult.WIN, 4, displayName = "Valkyrie", photoUrl = null),
            DailyChallengeResult("5", "", 98, 55, GameResult.WIN, 5, displayName = "Satoshi", photoUrl = null)
        )
    }

    val allTimeLeaderboard = remember {
        listOf(
            DailyChallengeResult("11", "", 1450, 400, GameResult.WIN, 1, displayName = "Zeus_GAMBIT", photoUrl = null),
            DailyChallengeResult("12", "", 1320, 420, GameResult.WIN, 2, displayName = "Ares", photoUrl = null),
            DailyChallengeResult("13", "", 1290, 450, GameResult.WIN, 3, displayName = "Athena", photoUrl = null),
            DailyChallengeResult("14", "", 1210, 410, GameResult.WIN, 4, displayName = "Hermes", photoUrl = null),
            DailyChallengeResult("15", "", 1180, 500, GameResult.WIN, 5, displayName = "Hades", photoUrl = null)
        )
    }

    val currentList = when (selectedFilter) {
        LeaderboardFilter.TODAY -> state.leaderboard
        LeaderboardFilter.THIS_WEEK -> weeklyLeaderboard
        LeaderboardFilter.ALL_TIME -> allTimeLeaderboard
    }

    // Determine if the user is on the leaderboard list
    val currentUserId = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid }
    val isUserInLeaderboard = currentUserId != null && currentList.any { it.uid == currentUserId }
    val userResult = state.userResult

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "LEADERBOARD",
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
                actions = {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                soundManager.playMenuClick()
                                hapticFeedback.menuClick()
                                isRefreshing = true
                                delay(1000) // Simulate refresh
                                isRefreshing = false
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
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
            AnimatedBackground(
                colors = listOf(
                    Color(0xFF030712), // Deep Space Black
                    Color(0xFF0F172A), // Slate Dark
                    Color(0xFF1E1135), // Dark Royal Purple
                    Color(0xFF030712)
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Segmented Filter Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LeaderboardFilter.entries.forEach { filter ->
                        val isSelected = filter == selectedFilter
                        val tabTitle = when (filter) {
                            LeaderboardFilter.TODAY -> "TODAY"
                            LeaderboardFilter.THIS_WEEK -> "THIS WEEK"
                            LeaderboardFilter.ALL_TIME -> "ALL TIME"
                        }

                        Button(
                            onClick = {
                                soundManager.playMenuClick()
                                hapticFeedback.menuSelect()
                                selectedFilter = filter
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) goldColor else Color.Transparent,
                                contentColor = if (isSelected) Color.Black else Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = tabTitle,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Pinned User Rank Card (if they completed today but are not in the top 100 list)
                if (selectedFilter == LeaderboardFilter.TODAY && !isUserInLeaderboard && userResult != null) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = goldColor.copy(alpha = 0.15f),
                            contentColor = Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, goldColor.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(goldColor.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = state.userRank?.let { "#$it" } ?: "-",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = goldColor
                                    )
                                }

                                UserAvatar(
                                    displayName = userResult.displayName,
                                    photoUrl = userResult.photoUrl,
                                    modifier = Modifier.size(36.dp)
                                )

                                Column {
                                    Text(
                                        text = if (userResult.displayName.isNotEmpty()) userResult.displayName else "You",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Your Score",
                                        fontSize = 11.sp,
                                        color = Color.LightGray
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Result badge
                                val outcomeText = when (userResult.result) {
                                    GameResult.WIN -> "W"
                                    GameResult.LOSS -> "L"
                                    GameResult.DRAW -> "D"
                                }
                                val outcomeColor = when (userResult.result) {
                                    GameResult.WIN -> Color(0xFF4CAF50)
                                    GameResult.LOSS -> Color(0xFFF44336)
                                    GameResult.DRAW -> Color(0xFFFF9800)
                                }

                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(outcomeColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = outcomeText,
                                        color = outcomeColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = "${userResult.playerScore} pts",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = goldColor
                                )
                            }
                        }
                    }
                }

                // Pull-to-refresh spinner (simulated)
                if (isRefreshing) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = goldColor, modifier = Modifier.size(24.dp))
                    }
                }

                // Leaderboard List
                if (currentList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No submissions yet today.\nBe the first to score!",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("leaderboard_list"),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(currentList) { item ->
                            val isMe = item.uid == currentUserId
                            LeaderboardRowItem(entry = item, isMe = isMe)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderboardRowItem(entry: DailyChallengeResult, isMe: Boolean) {
    val goldColor = Color(0xFFFFD700)
    val rankColor = when (entry.rank) {
        1 -> Color(0xFFFFD700) // Gold
        2 -> Color(0xFFC0C0C0) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> Color.LightGray
    }

    val outcomeText = when (entry.result) {
        GameResult.WIN -> "W"
        GameResult.LOSS -> "L"
        GameResult.DRAW -> "D"
    }
    val outcomeColor = when (entry.result) {
        GameResult.WIN -> Color(0xFF4CAF50)
        GameResult.LOSS -> Color(0xFFF44336)
        GameResult.DRAW -> Color(0xFFFF9800)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isMe) goldColor.copy(alpha = 0.08f) else Color(0xFF1E293B).copy(alpha = 0.5f),
            contentColor = Color.White
        ),
        border = if (isMe) androidx.compose.foundation.BorderStroke(1.dp, goldColor.copy(alpha = 0.5f)) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Rank number
                Text(
                    text = "${entry.rank}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = rankColor,
                    modifier = Modifier.width(28.dp),
                    textAlign = TextAlign.Center
                )

                UserAvatar(
                    displayName = entry.displayName,
                    photoUrl = entry.photoUrl,
                    modifier = Modifier.size(36.dp)
                )

                Column {
                    Text(
                        text = if (entry.displayName.isNotEmpty()) entry.displayName else "Player",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isMe) goldColor else Color.White
                    )
                    Text(
                        text = "vs AI (${entry.aiScore} pts)",
                        fontSize = 11.sp,
                        color = Color.LightGray.copy(alpha = 0.7f)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Outcome badge
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(outcomeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = outcomeText,
                        color = outcomeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "${entry.playerScore} pts",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = goldColor
                )
            }
        }
    }
}
