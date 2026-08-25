package com.example.presentation.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firestore.GameRecord
import com.example.domain.achievements.Achievement
import com.example.presentation.components.UserAvatar
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    onSignOut: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agent Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
                    }
                    IconButton(onClick = {
                        viewModel.signOut()
                        onSignOut()
                    }) {
                        Icon(Icons.Default.Logout, contentDescription = "Sign Out", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                state.profile?.let { profile ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        // User Header Card
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("user_profile_header_card"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    UserAvatar(
                                        displayName = profile.displayName,
                                        photoUrl = profile.photoUrl,
                                        size = 72.dp
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = profile.displayName,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable {
                                                clipboardManager.setText(AnnotatedString(profile.friendCode))
                                            }
                                        ) {
                                            Text(
                                                text = "Code: ${profile.friendCode}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Outlined.ContentCopy,
                                                contentDescription = "Copy friend code",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Tab Selection Row
                        item {
                            TabRow(selectedTabIndex = selectedTab) {
                                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                                    Box(Modifier.padding(vertical = 12.dp)) { Text("Stats") }
                                }
                                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                                    Box(Modifier.padding(vertical = 12.dp)) { Text("Achievements") }
                                }
                                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                                    Box(Modifier.padding(vertical = 12.dp)) { Text("History") }
                                }
                            }
                        }

                        when (selectedTab) {
                            0 -> {
                                item {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        StatsRow(label = "PvP Rating (ELO)", value = profile.stats.pvpRating.toString(), icon = Icons.Default.EmojiEvents, color = Color(0xFFFFB300))
                                        StatsRow(label = "Total Games", value = profile.stats.totalGames.toString(), icon = Icons.Default.SportsEsports)
                                        val winPercentage = if (profile.stats.totalGames > 0) {
                                            "${(profile.stats.totalWins * 100 / profile.stats.totalGames)}%"
                                        } else "0%"
                                        StatsRow(label = "Win rate", value = winPercentage, icon = Icons.Default.Percent)
                                        StatsRow(label = "Current Streak", value = profile.stats.currentStreak.toString(), icon = Icons.Default.LocalFireDepartment, color = Color(0xFFFF5722))
                                        StatsRow(label = "Best Streak", value = profile.stats.bestStreak.toString(), icon = Icons.Default.Timeline)
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Arena Progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        
                                        ProgressCard("Arena 1 (Ascendency)", profile.arenaProgress.arena1Level1Wins + profile.arenaProgress.arena1Level2Wins + profile.arenaProgress.arena1Level3Wins, true)
                                        ProgressCard("Arena 2 (Confrontation)", profile.arenaProgress.arena2Level1Wins + profile.arenaProgress.arena2Level2Wins + profile.arenaProgress.arena2Level3Wins, profile.arenaProgress.arena2Unlocked)
                                        ProgressCard("Arena 3 (Oblivion)", profile.arenaProgress.arena3Level1Wins + profile.arenaProgress.arena3Level2Wins + profile.arenaProgress.arena3Level3Wins, profile.arenaProgress.arena3Unlocked)
                                    }
                                }
                            }
                            1 -> {
                                items(Achievement.values()) { achievement ->
                                    val isUnlocked = profile.achievements.contains(achievement.id)
                                    AchievementCard(achievement = achievement, isUnlocked = isUnlocked)
                                }
                            }
                            2 -> {
                                if (state.gameHistory.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "No clashing history registered yet.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else {
                                    items(state.gameHistory) { record ->
                                        GameRecordCard(record = record)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showEditDialog && state.profile != null) {
                EditProfileDialog(
                    currentDisplayName = state.profile!!.displayName,
                    onDismiss = { showEditDialog = false },
                    onSave = { newName ->
                        viewModel.updateDisplayName(newName)
                        showEditDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun StatsRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            }
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun ProgressCard(
    title: String,
    totalWins: Int,
    isUnlocked: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isUnlocked) 1f else 0.5f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(if (isUnlocked) "$totalWins victories achieved" else "Locked (Win previous arena level 3)", style = MaterialTheme.typography.bodySmall)
            }
            Icon(
                imageVector = if (isUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                contentDescription = null,
                tint = if (isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AchievementCard(
    achievement: Achievement,
    isUnlocked: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .alpha(if (isUnlocked) 1f else 0.4f),
        shape = RoundedCornerShape(12.dp),
        border = if (isUnlocked) BorderStroke(1.dp, Color(0xFFFFD700)) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isUnlocked) Icons.Default.EmojiEvents else Icons.Default.Lock,
                contentDescription = null,
                tint = if (isUnlocked) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(achievement.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(achievement.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun GameRecordCard(record: GameRecord) {
    val isWin = record.result == "WIN"
    val isLoss = record.result == "LOSS"

    val color = when {
        isWin -> Color(0xFF4CAF50)
        isLoss -> Color(0xFFF44336)
        else -> Color(0xFF9E9E9E)
    }

    val formattedDate = remember(record.timestamp) {
        try {
            val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            sdf.format(Date(record.timestamp))
        } catch (e: Exception) {
            ""
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (record.isPvP) "PvP Clash vs ${record.opponentName ?: "Player"}" else "AI Skirmish",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(formattedDate, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${record.playerScore} - ${record.aiScore}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Box(
                    modifier = Modifier
                        .background(color.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = record.result,
                        color = color,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}
