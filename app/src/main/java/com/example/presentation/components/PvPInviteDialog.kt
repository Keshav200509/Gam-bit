package com.example.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.UserProfile
import com.example.domain.model.Arena
import com.example.domain.model.GameLevel

@Composable
fun PvPInviteDialog(
    friendName: String,
    currentUserProfile: UserProfile?,
    onSendInvite: (Arena, GameLevel) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedArena by remember { mutableStateOf(Arena.ASCENDENCY) }
    var selectedLevel by remember { mutableStateOf(GameLevel.LEVEL_1) }

    val progress = currentUserProfile?.arenaProgress

    // Determine unlocked arenas & levels
    val isArena2Unlocked = progress?.arena2Unlocked ?: false
    val isArena3Unlocked = progress?.arena3Unlocked ?: false

    val isLevel2Unlocked = when (selectedArena) {
        Arena.ASCENDENCY -> (progress?.arena1Level1Wins ?: 0) > 0
        Arena.CONFRONTATION -> (progress?.arena2Level1Wins ?: 0) > 0
        Arena.OBLIVION -> (progress?.arena3Level1Wins ?: 0) > 0
    }

    val isLevel3Unlocked = when (selectedArena) {
        Arena.ASCENDENCY -> (progress?.arena1Level2Wins ?: 0) > 0
        Arena.CONFRONTATION -> (progress?.arena2Level2Wins ?: 0) > 0
        Arena.OBLIVION -> (progress?.arena3Level2Wins ?: 0) > 0
    }

    // Adjust selected level if it becomes locked after switching arena
    LaunchedEffect(selectedArena) {
        if (selectedLevel == GameLevel.LEVEL_2 && !isLevel2Unlocked) {
            selectedLevel = GameLevel.LEVEL_1
        } else if (selectedLevel == GameLevel.LEVEL_3 && !isLevel3Unlocked) {
            selectedLevel = GameLevel.LEVEL_1
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Invite $friendName to Duel", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Arena Selector
                Text("Select Arena", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Arena.values().forEach { arena ->
                        val isUnlocked = when (arena) {
                            Arena.ASCENDENCY -> true
                            Arena.CONFRONTATION -> isArena2Unlocked
                            Arena.OBLIVION -> isArena3Unlocked
                        }
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = isUnlocked) { selectedArena = arena }
                                .testTag("select_arena_${arena.name}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedArena == arena) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (arena) {
                                        Arena.ASCENDENCY -> "Arena 1: Ascendency"
                                        Arena.CONFRONTATION -> "Arena 2: Confrontation"
                                        Arena.OBLIVION -> "Arena 3: Oblivion"
                                    },
                                    fontWeight = if (selectedArena == arena) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isUnlocked) {
                                        if (selectedArena == arena) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    }
                                )
                                if (!isUnlocked) {
                                    Text("LOCKED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                // Level Selector
                Text("Select Difficulty Level", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GameLevel.values().forEach { level ->
                        val isUnlocked = when (level) {
                            GameLevel.LEVEL_1 -> true
                            GameLevel.LEVEL_2 -> isLevel2Unlocked
                            GameLevel.LEVEL_3 -> isLevel3Unlocked
                        }

                        Button(
                            onClick = { selectedLevel = level },
                            enabled = isUnlocked,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedLevel == level) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = if (selectedLevel == level) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("select_level_${level.name}")
                        ) {
                            Text(
                                text = when (level) {
                                    GameLevel.LEVEL_1 -> "Lvl 1"
                                    GameLevel.LEVEL_2 -> "Lvl 2"
                                    GameLevel.LEVEL_3 -> "Lvl 3"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSendInvite(selectedArena, selectedLevel)
                    onDismiss()
                },
                modifier = Modifier.testTag("send_invite_confirm_button")
            ) {
                Text("Send Invite")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("dismiss_invite_button")) {
                Text("Cancel")
            }
        }
    )
}
