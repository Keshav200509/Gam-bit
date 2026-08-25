package com.example.presentation.screens.arena

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.Arena
import com.example.domain.model.ArenaConfiguration
import com.example.domain.model.GameLevel
import com.example.domain.model.PlayerProgress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArenaMapScreen(
    onNavigateToGame: (Arena, GameLevel) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToFriends: () -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: ArenaMapViewModel = hiltViewModel()
) {
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    var selectedArena by remember { mutableStateOf<Arena?>(Arena.ASCENDENCY) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "GAMBIT ARENAS",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp
                        )
                    )
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToFriends,
                        modifier = Modifier.testTag("friends_button")
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.People,
                            contentDescription = "Social & Friends",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(
                        onClick = onNavigateToProfile,
                        modifier = Modifier.testTag("profile_button")
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Person,
                            contentDescription = "Profile & Stats",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                )
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    Text(
                        text = "Select an arena and conquer its tactical levels to unlock new battlegrounds.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }

                items(Arena.values()) { arena ->
                    val isSelected = selectedArena == arena
                    val isUnlocked = progress.isArenaUnlocked(arena)

                    ArenaCard(
                        arena = arena,
                        isSelected = isSelected,
                        isUnlocked = isUnlocked,
                        progress = progress,
                        onClick = {
                            if (isUnlocked) {
                                selectedArena = if (isSelected) null else arena
                            }
                        },
                        onPlayLevel = { level ->
                            onNavigateToGame(arena, level)
                        }
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.resetProgress() },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reset Campaign Progress")
                    }
                }
            }
        }
    }
}

@Composable
fun ArenaCard(
    arena: Arena,
    isSelected: Boolean,
    isUnlocked: Boolean,
    progress: PlayerProgress,
    onClick: () -> Unit,
    onPlayLevel: (GameLevel) -> Unit
) {
    val accentColor = when (arena) {
        Arena.ASCENDENCY -> Color(0xFF00CBC6) // Teal
        Arena.CONFRONTATION -> MaterialTheme.colorScheme.primary // Slate / Slate Slate Blue
        Arena.OBLIVION -> Color(0xFFFF4D4D) // Crimson
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(300))
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 6.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .clickable { onClick() }
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (isUnlocked) accentColor else Color.Gray)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1.0f)) {
                    Text(
                        text = arena.displayName.uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else Color.Gray
                        )
                    )
                    Text(
                        text = arena.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!isUnlocked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AnimatedVisibility(visible = isSelected && isUnlocked) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)

                    GameLevel.values().forEach { level ->
                        LevelRow(
                            arena = arena,
                            level = level,
                            progress = progress,
                            accentColor = accentColor,
                            onPlay = { onPlayLevel(level) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LevelRow(
    arena: Arena,
    level: GameLevel,
    progress: PlayerProgress,
    accentColor: Color,
    onPlay: () -> Unit
) {
    val config = ArenaConfiguration.get(arena, level)
    val isLevelUnlocked = progress.isUnlocked(arena, level)
    val wins = progress.getWins(arena, level)
    val bestScoreKey = "${arena.name.lowercase()}_level${level.level}"
    val bestScore = progress.bestScores[bestScoreKey] ?: 0

    val aiRangeText = "AI Capability: ${config.aiCapabilityMin}% to ${config.aiCapabilityMax}%"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.0f)) {
            Text(
                text = "Level ${level.level}: ${level.displayName}",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (isLevelUnlocked) MaterialTheme.colorScheme.onSurface else Color.Gray
                )
            )

            if (isLevelUnlocked) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "Wins: $wins/${PlayerProgress.WINS_TO_ADVANCE}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (wins >= PlayerProgress.WINS_TO_ADVANCE) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (bestScore > 0) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Best Score",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Best: $bestScore",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = aiRangeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            } else {
                Text(
                    text = "Unlock by winning previous levels",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        if (isLevelUnlocked) {
            Button(
                onClick = onPlay,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("PLAY", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Locked",
                tint = Color.Gray,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
