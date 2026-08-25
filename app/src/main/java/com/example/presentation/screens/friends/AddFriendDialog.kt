package com.example.presentation.screens.friends

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.UserProfile
import com.example.presentation.components.UserAvatar

@Composable
fun AddFriendDialog(
    searchedFriend: UserProfile?,
    onSearch: (String) -> Unit,
    onSendRequest: (String) -> Unit,
    onDismiss: () -> Unit,
    error: String?,
    onClearSearchedFriend: () -> Unit
) {
    var friendCode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = {
            onClearSearchedFriend()
            onDismiss()
        },
        title = { Text("Add Friend", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column {
                OutlinedTextField(
                    value = friendCode,
                    onValueChange = {
                        friendCode = it.trim().uppercase()
                    },
                    label = { Text("Friend Code") },
                    placeholder = { Text("6-character code") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("friend_code_input")
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onSearch(friendCode) },
                    enabled = friendCode.length == 6,
                    modifier = Modifier
                        .align(Alignment.End)
                        .testTag("search_friend_button")
                ) {
                    Text("Search")
                }

                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                searchedFriend?.let { friend ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(displayName = friend.displayName, photoUrl = friend.photoUrl, size = 40.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(friend.displayName, style = MaterialTheme.typography.bodyLarge)
                                Text("Code: ${friend.friendCode}", style = MaterialTheme.typography.bodySmall)
                            }
                            Button(
                                onClick = {
                                    onSendRequest(friend.uid)
                                    onClearSearchedFriend()
                                    onDismiss()
                                },
                                modifier = Modifier.testTag("send_request_button")
                            ) {
                                Text("Add")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = {
                    onClearSearchedFriend()
                    onDismiss()
                },
                modifier = Modifier.testTag("close_add_friend_button")
            ) {
                Text("Close")
            }
        }
    )
}
