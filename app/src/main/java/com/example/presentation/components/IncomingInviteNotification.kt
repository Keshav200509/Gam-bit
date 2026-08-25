package com.example.presentation.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.PvPInvite
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun IncomingInviteNotification(
    invite: PvPInvite?,
    onAccept: (PvPInvite) -> Unit,
    onReject: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember(invite) { mutableStateOf(invite != null) }

    LaunchedEffect(invite) {
        if (invite != null) {
            visible = true
            delay(5000)
            visible = false
            onReject(invite.id)
        }
    }

    AnimatedVisibility(
        visible = visible && invite != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        if (invite != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("incoming_invite_notification"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${invite.fromName} invites you!",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val arenaFormatted = invite.arena.name.lowercase().replaceFirstChar { 
                            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
                        }
                        val levelFormatted = invite.level.name.replace("LEVEL_", "Level ")
                        Text(
                            text = "Play $arenaFormatted - $levelFormatted",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                visible = false
                                onAccept(invite)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("accept_invite_notification_button")
                        ) {
                            Text("Accept")
                        }

                        FilledTonalButton(
                            onClick = {
                                visible = false
                                onReject(invite.id)
                            },
                            modifier = Modifier.testTag("reject_invite_notification_button")
                        ) {
                            Text("Reject")
                        }
                    }
                }
            }
        }
    }
}
