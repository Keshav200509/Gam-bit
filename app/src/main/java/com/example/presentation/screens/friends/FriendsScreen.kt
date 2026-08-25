package com.example.presentation.screens.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Friendship
import com.example.data.model.PvPInvite
import com.example.data.model.UserProfile
import com.example.domain.model.Arena
import com.example.domain.model.GameLevel
import com.example.presentation.components.FriendCard
import com.example.presentation.components.IncomingInviteNotification
import com.example.presentation.components.PvPInviteDialog
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    viewModel: FriendsViewModel,
    currentUserProfile: UserProfile?,
    onBack: () -> Unit,
    onNavigateToPvPMatch: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    var showAddFriendDialog by remember { mutableStateOf(false) }
    var selectedFriendForInvite by remember { mutableStateOf<UserProfile?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(state.activeMatchId) {
        state.activeMatchId?.let { matchId ->
            onNavigateToPvPMatch(matchId)
            viewModel.clearActiveMatch()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agents Roster", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddFriendDialog = true },
                modifier = Modifier.testTag("add_friend_fab")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add Friend")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Tab Selection Row
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.testTag("tab_friends")
                    ) {
                        Box(Modifier.padding(vertical = 12.dp)) {
                            Text("FRIENDS (${state.friends.size})", fontWeight = FontWeight.Bold)
                        }
                    }
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.testTag("tab_requests")
                    ) {
                        Box(Modifier.padding(vertical = 12.dp)) {
                            Text("REQUESTS (${state.pendingRequests.size})", fontWeight = FontWeight.Bold)
                        }
                    }
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        modifier = Modifier.testTag("tab_invites")
                    ) {
                        Box(Modifier.padding(vertical = 12.dp)) {
                            Text("INVITES (${state.incomingInvites.size})", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (state.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    when (selectedTab) {
                        0 -> FriendsTabContent(
                            friends = state.friends,
                            onPlay = { selectedFriendForInvite = it },
                            onRemove = { viewModel.removeFriend(it.uid) }
                        )
                        1 -> RequestsTabContent(
                            pendingRequests = state.pendingRequests,
                            onAccept = { viewModel.acceptRequest(it.id) },
                            onReject = { viewModel.rejectRequest(it.id) }
                        )
                        2 -> InvitesTabContent(
                            incomingInvites = state.incomingInvites,
                            onAccept = { viewModel.acceptPvPInvite(it) },
                            onReject = { viewModel.rejectPvPInvite(it.id) }
                        )
                    }
                }
            }

            // Notification Overlay for incoming PvP Invite
            state.incomingInvites.firstOrNull()?.let { incomingInvite ->
                IncomingInviteNotification(
                    invite = incomingInvite,
                    onAccept = { viewModel.acceptPvPInvite(it) },
                    onReject = { viewModel.rejectPvPInvite(it) },
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }

            // Add Friend Dialog
            if (showAddFriendDialog) {
                AddFriendDialog(
                    searchedFriend = state.searchedFriend,
                    onSearch = { viewModel.searchByFriendCode(it) },
                    onSendRequest = { viewModel.sendFriendRequest(it) },
                    onDismiss = { showAddFriendDialog = false },
                    error = state.error,
                    onClearSearchedFriend = { viewModel.clearSearchedFriend() }
                )
            }

            // PvP Invite Configuration Dialog
            selectedFriendForInvite?.let { friend ->
                PvPInviteDialog(
                    friendName = friend.displayName,
                    currentUserProfile = currentUserProfile,
                    onSendInvite = { arena, level ->
                        viewModel.sendPvPInvite(friend.uid, friend.displayName, arena, level)
                    },
                    onDismiss = { selectedFriendForInvite = null }
                )
            }
        }
    }
}

@Composable
fun FriendsTabContent(
    friends: List<UserProfile>,
    onPlay: (UserProfile) -> Unit,
    onRemove: (UserProfile) -> Unit
) {
    if (friends.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Your friends list is empty. Share your code to build a roster!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(friends, key = { it.uid }) { friend ->
                FriendCard(
                    friend = friend,
                    onPlayClick = { onPlay(friend) },
                    onRemoveFriend = { onRemove(friend) }
                )
            }
        }
    }
}

@Composable
fun RequestsTabContent(
    pendingRequests: List<Friendship>,
    onAccept: (Friendship) -> Unit,
    onReject: (Friendship) -> Unit
) {
    if (pendingRequests.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No pending incoming friend requests.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(pendingRequests, key = { it.id }) { request ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pending_request_card_${request.id}"),
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
                            Text(
                                text = "Request Received",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Agent: ${request.requesterUid.take(6).uppercase()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onAccept(request) },
                                modifier = Modifier.testTag("accept_request_button_${request.id}")
                            ) {
                                Text("Accept")
                            }
                            FilledTonalButton(
                                onClick = { onReject(request) },
                                modifier = Modifier.testTag("reject_request_button_${request.id}")
                            ) {
                                Text("Reject")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InvitesTabContent(
    incomingInvites: List<PvPInvite>,
    onAccept: (PvPInvite) -> Unit,
    onReject: (PvPInvite) -> Unit
) {
    if (incomingInvites.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No pending incoming PvP battle invites.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(incomingInvites, key = { it.id }) { invite ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("incoming_invite_card_${invite.id}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "${invite.fromName} challenges you!",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val arenaFormatted = invite.arena.name.lowercase().replaceFirstChar { 
                            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
                        }
                        val levelFormatted = invite.level.name.replace("LEVEL_", "Level ")
                        Text(
                            text = "Play $arenaFormatted on $levelFormatted",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = { onReject(invite) },
                                modifier = Modifier.testTag("reject_invite_button_${invite.id}")
                            ) {
                                Text("Reject")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { onAccept(invite) },
                                modifier = Modifier.testTag("accept_invite_button_${invite.id}")
                            ) {
                                Text("Accept Clash")
                            }
                        }
                    }
                }
            }
        }
    }
}
