package com.example.presentation.screens.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun EditProfileDialog(
    currentDisplayName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var displayName by remember { mutableStateOf(currentDisplayName) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Edit Profile", style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = {
                        displayName = it
                        if (it.trim().isEmpty()) {
                            errorText = "Display name cannot be empty"
                        } else if (it.length > 20) {
                            errorText = "Display name cannot exceed 20 characters"
                        } else {
                            errorText = null
                        }
                    },
                    label = { Text("Display Name") },
                    isError = errorText != null,
                    supportingText = {
                        errorText?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_display_name_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (displayName.trim().isNotEmpty() && displayName.length <= 20) {
                        onSave(displayName.trim())
                    }
                },
                enabled = errorText == null && displayName.trim().isNotEmpty(),
                modifier = Modifier.testTag("save_profile_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_profile_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
