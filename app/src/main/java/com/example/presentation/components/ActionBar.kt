package com.example.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Border
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.PlayerTeal
import com.example.ui.theme.Surface

@Composable
fun ActionBar(
    scoutMode: Boolean,
    scoutUsed: Boolean,
    lockMode: Boolean,
    numLocksUsed: Int,
    hasPlacements: Boolean,
    onScoutClick: () -> Unit,
    onLockClick: () -> Unit,
    onClearClick: () -> Unit,
    onCommitClick: () -> Unit,
    modifier: Modifier = Modifier,
    isArena3: Boolean = false,
    timeRemaining: Long = 60L,
    timerActive: Boolean = false,
    onTimerExpired: () -> Unit = {}
) {
    val maxLocks = if (isArena3) 1 else 2
    val scoutPurple = Color(0xFFB5179E) // Sleek neon purple for scouting
    val scoutBorderColor = when {
        scoutUsed -> Border.copy(alpha = 0.3f)
        scoutMode -> scoutPurple
        else -> scoutPurple.copy(alpha = 0.6f)
    }
    
    val lockBorderColor = when {
        numLocksUsed >= maxLocks && !lockMode -> Border.copy(alpha = 0.3f)
        lockMode -> GoldAccent
        else -> GoldAccent.copy(alpha = 0.6f)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. SCOUT button (purple outline) - Hidden in Arena 3 (Oblivion)
        if (!isArena3) {
            OutlinedButton(
                onClick = onScoutClick,
                enabled = !scoutUsed,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (scoutMode) scoutPurple.copy(alpha = 0.15f) else Color.Transparent,
                    contentColor = if (scoutUsed) Color.Gray else if (scoutMode) scoutPurple else Color.White
                ),
                border = BorderStroke(1.5.dp, scoutBorderColor),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("scout_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Search, 
                    contentDescription = "Scout",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (scoutUsed) "Used" else "Scout", 
                    fontSize = 12.sp, 
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 2. LOCK button (gold outline)
        OutlinedButton(
            onClick = onLockClick,
            enabled = numLocksUsed < maxLocks || lockMode,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (lockMode) GoldAccent.copy(alpha = 0.15f) else Color.Transparent,
                contentColor = if (numLocksUsed >= maxLocks && !lockMode) Color.Gray else if (lockMode) GoldAccent else Color.White
            ),
            border = BorderStroke(1.5.dp, lockBorderColor),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("lock_button")
        ) {
            Icon(
                imageVector = Icons.Default.Lock, 
                contentDescription = "Lock",
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Lock (${maxLocks - numLocksUsed})", 
                fontSize = 12.sp, 
                fontWeight = FontWeight.Bold
            )
        }

        // 3. CLEAR button (grey outline)
        IconButton(
            onClick = onClearClick,
            modifier = Modifier
                .size(48.dp)
                .border(BorderStroke(1.5.dp, Color(0xFF495057)), shape = RoundedCornerShape(10.dp))
                .background(Surface, shape = RoundedCornerShape(10.dp))
                .testTag("clear_button")
        ) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Clear placements",
                tint = Color(0xFFCED4DA)
            )
        }

        // 4. COMMIT button (teal filled, disabled until at least 1 token placed)
        RoundTimer(
            timeRemaining = timeRemaining,
            scoutMode = scoutMode,
            lockMode = lockMode,
            onTimerExpired = onTimerExpired,
            modifier = Modifier
                .weight(1.2f)
                .height(48.dp)
        ) {
            Button(
                onClick = onCommitClick,
                enabled = hasPlacements,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PlayerTeal,
                    contentColor = Color(0xFF07080C),
                    disabledContainerColor = PlayerTeal.copy(alpha = 0.25f),
                    disabledContentColor = Color.White.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("commit_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow, 
                    contentDescription = "Commit",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "COMMIT", 
                    fontSize = 13.sp, 
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
