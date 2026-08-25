package com.example.presentation.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.components.AnimatedBackground
import com.example.presentation.components.UserAvatar
import com.example.presentation.components.SoundAndHapticEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.delay
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToQuickPlay: () -> Unit,
    onNavigateToArena: () -> Unit,
    onNavigateToPvP: () -> Unit,
    onNavigateToDailyChallenge: () -> Unit,
    onNavigateToFriends: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current.applicationContext
    val entryPoint = remember(context) {
        EntryPointAccessors.fromApplication(context, SoundAndHapticEntryPoint::class.java)
    }
    val soundManager = entryPoint.soundManager()

    // 1. play entry sound
    LaunchedEffect(Unit) {
        soundManager.playLogoAppear()
    }

    // 2. staggered button animation states
    val visibleButtons = remember { mutableStateListOf(false, false, false, false) }
    LaunchedEffect(Unit) {
        for (i in visibleButtons.indices) {
            delay(100L)
            visibleButtons[i] = true
        }
    }

    // 3. Logo entry animation
    var logoVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        logoVisible = true
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            HomeBottomNavigation(
                onProfileClick = onNavigateToProfile,
                onStatsClick = onNavigateToStats,
                onSettingsClick = onNavigateToSettings
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // AAA moving background
            AnimatedBackground(
                colors = listOf(
                    Color(0xFF030712), // Deep Space Black
                    Color(0xFF0B1528), // Dark Cosmic Blue
                    Color(0xFF1E1E38), // Muted Purple
                    Color(0xFF030712)
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header: Profile + Friends
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Profile Info
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        UserAvatar(
                            displayName = state.userProfile?.displayName ?: "Player",
                            photoUrl = state.userProfile?.photoUrl
                        )
                        Column {
                            Text(
                                text = state.userProfile?.displayName ?: "Player",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Rating: ${state.userProfile?.stats?.pvpRating ?: 1000}",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Wins: ${state.userProfile?.stats?.totalWins ?: 0}",
                                    color = Color.LightGray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // Friends with notification badge
                    val totalInvitesAndRequests = state.friendRequestCount + state.pvpInviteCount
                    Box {
                        IconButton(
                            onClick = onNavigateToFriends,
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.08f), CircleShape)
                                .testTag("friends_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = "Friends",
                                tint = Color.White
                            )
                        }
                        if (totalInvitesAndRequests > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = (-4).dp)
                                    .background(Color.Red, CircleShape)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = totalInvitesAndRequests.toString(),
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Middle: Logo Area
                AnimatedVisibility(
                    visible = logoVisible,
                    enter = fadeIn(animationSpec = tween(800)) + scaleIn(initialScale = 0.8f, animationSpec = tween(800)),
                    modifier = Modifier.padding(vertical = 24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "GAMBIT",
                            color = Color.White,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 8.sp,
                            modifier = Modifier.testTag("app_logo")
                        )
                        Text(
                            text = "ULTIMATE EDITION",
                            color = Color(0xFFFFD700), // Pure Gold Accent
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp,
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Bottom: Main Game Modes
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Button 1: Quick Play (AI level 1, Ascendency)
                    MenuOptionButton(
                        visible = visibleButtons[0],
                        title = "QUICK PLAY",
                        subtitle = "Instant match vs AI",
                        icon = Icons.Default.PlayArrow,
                        glowColor = MaterialTheme.colorScheme.primary,
                        onClick = onNavigateToQuickPlay,
                        testTag = "quick_play_button"
                    )

                    // Button 2: Arena
                    MenuOptionButton(
                        visible = visibleButtons[1],
                        title = "ARENA",
                        subtitle = "Choose arena & level",
                        icon = Icons.Default.EmojiEvents,
                        glowColor = Color(0xFFE91E63), // Pink glow
                        onClick = onNavigateToArena,
                        testTag = "arena_button"
                    )

                    // Button 3: PvP
                    MenuOptionButton(
                        visible = visibleButtons[2],
                        title = "PVP MATCH",
                        subtitle = "Play with friends",
                        icon = Icons.Default.Handshake,
                        glowColor = Color(0xFF2196F3), // Blue glow
                        onClick = onNavigateToPvP,
                        testTag = "pvp_button"
                    )

                    // Button 4: Daily Challenge
                    MenuOptionButton(
                        visible = visibleButtons[3],
                        title = "DAILY CHALLENGE",
                        subtitle = "New puzzle every day",
                        icon = Icons.Default.CalendarToday,
                        glowColor = Color(0xFFFFD700), // Gold glow
                        onClick = onNavigateToDailyChallenge,
                        badge = {
                            if (state.dailyChallengeAvailable) {
                                PulsingGoldDot()
                            }
                        },
                        testTag = "daily_challenge_button"
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun MenuOptionButton(
    visible: Boolean,
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    glowColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: @Composable (() -> Unit)? = null,
    testTag: String = ""
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.95f, animationSpec = tween(400)),
        modifier = modifier
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val isHovered by interactionSource.collectIsHoveredAsState()

        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.98f else if (isHovered) 1.02f else 1.0f,
            animationSpec = tween(100),
            label = "scale"
        )

        val borderAlpha by animateFloatAsState(
            targetValue = if (isPressed) 1.0f else if (isHovered) 0.8f else 0.4f,
            animationSpec = tween(150),
            label = "border"
        )

        val context = LocalContext.current.applicationContext
        val entryPoint = remember(context) {
            EntryPointAccessors.fromApplication(context, SoundAndHapticEntryPoint::class.java)
        }
        val soundManager = entryPoint.soundManager()
        val hapticFeedback = entryPoint.hapticFeedback()

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF111827).copy(alpha = 0.85f),
                contentColor = Color.White
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.5.dp,
                color = glowColor.copy(alpha = borderAlpha)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = {
                        soundManager.playMenuClick()
                        hapticFeedback.menuClick()
                        onClick()
                    }
                )
                .testTag(testTag)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(glowColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = glowColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = Color.White
                        )
                        Text(
                            text = subtitle,
                            fontSize = 12.sp,
                            color = Color.LightGray
                        )
                    }
                }
                if (badge != null) {
                    badge()
                }
            }
        }
    }
}

@Composable
fun PulsingGoldDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    androidx.compose.foundation.Canvas(modifier = Modifier.size(12.dp)) {
        drawCircle(
            color = Color(0xFFFFD700),
            radius = size.minDimension / 2,
            alpha = alpha
        )
    }
}

@Composable
fun HomeBottomNavigation(
    onProfileClick: () -> Unit,
    onStatsClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFF030712),
        contentColor = Color.White,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        val context = LocalContext.current.applicationContext
        val entryPoint = remember(context) {
            EntryPointAccessors.fromApplication(context, SoundAndHapticEntryPoint::class.java)
        }
        val soundManager = entryPoint.soundManager()
        val hapticFeedback = entryPoint.hapticFeedback()

        NavigationBarItem(
            selected = false,
            onClick = {
                soundManager.playMenuClick()
                hapticFeedback.menuClick()
                onProfileClick()
            },
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile", tint = Color.LightGray) },
            label = { Text("Profile", color = Color.LightGray) },
            modifier = Modifier.testTag("nav_profile")
        )
        NavigationBarItem(
            selected = false,
            onClick = {
                soundManager.playMenuClick()
                hapticFeedback.menuClick()
                onStatsClick()
            },
            icon = { Icon(Icons.Default.BarChart, contentDescription = "Stats", tint = Color.LightGray) },
            label = { Text("Stats", color = Color.LightGray) },
            modifier = Modifier.testTag("nav_stats")
        )
        NavigationBarItem(
            selected = false,
            onClick = {
                soundManager.playMenuClick()
                hapticFeedback.menuClick()
                onSettingsClick()
            },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.LightGray) },
            label = { Text("Settings", color = Color.LightGray) },
            modifier = Modifier.testTag("nav_settings")
        )
    }
}
