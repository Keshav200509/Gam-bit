package com.example.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.UserProfile
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FriendCard(
    friend: UserProfile,
    onPlayClick: () -> Unit,
    onRemoveFriend: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    val isOnline = remember(friend.lastActive) {
        val now = System.currentTimeMillis()
        now - friend.lastActive < 5 * 60 * 1000 // active in last 5 min
    }

    val lastActiveStr = remember(friend.lastActive) {
        if (isOnline) {
            "Online"
        } else {
            val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            "Active: " + sdf.format(Date(friend.lastActive))
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = { showMenu = true }
            )
            .testTag("friend_card_${friend.uid}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    UserAvatar(displayName = friend.displayName, photoUrl = friend.photoUrl)
                    // Online status green dot indicator
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (isOnline) Color(0xFF4CAF50) else Color(0xFF9E9E9E))
                            .align(Alignment.BottomEnd)
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(friend.displayName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text(lastActiveStr, style = MaterialTheme.typography.bodySmall, color = if (isOnline) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Box {
                IconButton(
                    onClick = onPlayClick,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("play_friend_${friend.uid}")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Send PvP Invite")
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Remove Friend") },
                        onClick = {
                            onRemoveFriend()
                            showMenu = false
                        },
                        modifier = Modifier.testTag("menu_remove_friend")
                    )
                }
            }
        }
    }
}
