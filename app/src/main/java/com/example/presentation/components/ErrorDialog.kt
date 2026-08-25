package com.example.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AICrimson
import com.example.ui.theme.Border
import com.example.ui.theme.PlayerTeal
import com.example.ui.theme.Surface

@Composable
fun ErrorDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = "Error Icon",
                tint = AICrimson,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = title.uppercase(),
                color = AICrimson,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                letterSpacing = 1.sp
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("error_dialog_dismiss_button")
            ) {
                Text(
                    text = "OK",
                    color = PlayerTeal,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = onRetry?.let { retryAction ->
            {
                TextButton(
                    onClick = retryAction,
                    modifier = Modifier.testTag("error_dialog_retry_button")
                ) {
                    Text(
                        text = "RETRY",
                        color = PlayerTeal,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        containerColor = Surface,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .border(1.dp, Border, RoundedCornerShape(16.dp))
            .testTag("error_dialog")
    )
}
