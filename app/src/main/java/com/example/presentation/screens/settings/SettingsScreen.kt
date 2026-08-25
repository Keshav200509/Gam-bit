package com.example.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.AnimationSpeed
import com.example.domain.model.DifficultyPreset
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val soundEnabled by viewModel.soundEnabled.collectAsStateWithLifecycle()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsStateWithLifecycle()
    val animationSpeed by viewModel.animationSpeed.collectAsStateWithLifecycle()
    val aiCapabilityVisible by viewModel.aiCapabilityVisible.collectAsStateWithLifecycle()
    val difficultyPreset by viewModel.difficultyPreset.collectAsStateWithLifecycle()

    val profile by viewModel.currentUserProfile.collectAsStateWithLifecycle()
    val email = viewModel.currentUserEmail
    val masterVolume by viewModel.masterVolume.collectAsStateWithLifecycle()
    val ambientMusicEnabled by viewModel.ambientMusicEnabled.collectAsStateWithLifecycle()
    val showProbabilityTooltips by viewModel.showProbabilityTooltips.collectAsStateWithLifecycle()
    val confirmBeforeCommit by viewModel.confirmBeforeCommit.collectAsStateWithLifecycle()
    val displayTheme by viewModel.displayTheme.collectAsStateWithLifecycle()
    val boardSize by viewModel.boardSize.collectAsStateWithLifecycle()
    val reduceMotion by viewModel.reduceMotion.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    var showResetStatsDialog by remember { mutableStateOf(false) }
    var showResetProgressDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var tempDisplayName by remember { mutableStateOf("") }

    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            scope.launch {
                snackbarHostState.showSnackbar(msg)
                viewModel.clearStatusMessage()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF06070B),
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClicked,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Go back",
                            tint = PlayerTeal
                        )
                    }
                    Text(
                        text = "SETTINGS & SYSTEM CONFIG",
                        style = MaterialTheme.typography.titleMedium,
                        color = PlayerTeal,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                }

                // SECTION 1: ACCOUNT
                SettingsCategory(title = "COMMANDER PROFILE") {
                    val currentName = profile?.displayName ?: "Gambit Player"
                    val friendCode = profile?.friendCode ?: "------"

                    // Callsign edit row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("CALLSIGN", style = MaterialTheme.typography.labelSmall, color = GoldAccent)
                            Text(currentName, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                tempDisplayName = currentName
                                showEditNameDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Surface),
                            border = BorderStroke(1.dp, Border),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("edit_callsign_button")
                        ) {
                            Text("EDIT", color = PlayerTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = Border.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))

                    // Email row (read-only)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("REGISTERED EMAIL", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(email, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    HorizontalDivider(color = Border.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))

                    // Friend code row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("SECURE FRIEND CODE", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(friendCode, style = MaterialTheme.typography.bodyLarge, color = GoldAccent, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = "Copy code",
                                    tint = PlayerTeal,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable {
                                            clipboardManager.setText(AnnotatedString(friendCode))
                                            scope.launch { snackbarHostState.showSnackbar("Friend code copied to clipboard.") }
                                        }
                                        .testTag("copy_friend_code_button")
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Border.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))

                    // Account Actions (Sign Out / Delete)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.signOut(onBackClicked) },
                            colors = ButtonDefaults.buttonColors(containerColor = Surface),
                            border = BorderStroke(1.dp, Border),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sign_out_button")
                        ) {
                            Text("SIGN OUT", color = PlayerTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { showDeleteAccountDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Surface),
                            border = BorderStroke(1.dp, AICrimson.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("delete_account_button")
                        ) {
                            Text("DELETE ACCOUNT", color = AICrimson, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SECTION 2: AUDIO & FEEDBACK
                SettingsCategory(title = "AUDIO & FEEDBACK") {
                    // Master volume slider
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null, tint = PlayerTeal, modifier = Modifier.size(24.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Master Volume", style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                Text("Adjust absolute system audio levels", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            }
                        }
                        Slider(
                            value = masterVolume,
                            onValueChange = { viewModel.setMasterVolume(it) },
                            colors = SliderDefaults.colors(activeTrackColor = PlayerTeal, inactiveTrackColor = Border, thumbColor = PlayerTeal),
                            modifier = Modifier.fillMaxWidth().testTag("master_volume_slider")
                        )
                    }

                    HorizontalDivider(color = Border.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))

                    SettingsToggleItem(
                        icon = Icons.Default.MusicNote,
                        title = "Ambient Music",
                        description = "Enable soft background tactical synthesizer loop",
                        checked = ambientMusicEnabled,
                        onCheckedChange = { viewModel.setAmbientMusicEnabled(it) },
                        testTag = "ambient_music_toggle"
                    )

                    HorizontalDivider(color = Border.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))

                    SettingsToggleItem(
                        icon = Icons.Default.VolumeMute,
                        title = "Sound Effects",
                        description = "Enable immediate action-audio feedback",
                        checked = soundEnabled,
                        onCheckedChange = { viewModel.setSoundEnabled(it) },
                        testTag = "sound_toggle"
                    )

                    HorizontalDivider(color = Border.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))

                    SettingsToggleItem(
                        icon = Icons.Default.Fingerprint,
                        title = "Haptic Tactility",
                        description = "Trigger structural device vibrations on major turns",
                        checked = hapticsEnabled,
                        onCheckedChange = { viewModel.setHapticsEnabled(it) },
                        testTag = "haptics_toggle"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SECTION 3: TACTICAL ENGINE (GAMEPLAY)
                SettingsCategory(title = "TACTICAL ENGINE") {
                    // Animation speed selector
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = PlayerTeal, modifier = Modifier.size(24.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Resolution Animation Speed", style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                Text("Controls pacing of game clash animations", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AnimationSpeed.values().forEach { speed ->
                                val isSelected = animationSpeed == speed
                                Button(
                                    onClick = { viewModel.setAnimationSpeed(speed) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) PlayerTeal else Surface,
                                        contentColor = if (isSelected) Color.Black else Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    border = if (!isSelected) BorderStroke(1.dp, Border) else null,
                                    modifier = Modifier.weight(1f).height(38.dp).testTag("anim_speed_${speed.name.lowercase()}")
                                ) {
                                    Text(speed.name, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Border.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))

                    SettingsToggleItem(
                        icon = Icons.Default.Visibility,
                        title = "AI Mindset Indicator",
                        description = "Expose fluctuating AI MINDSTATE (PEAK/SLOPPY)",
                        checked = aiCapabilityVisible,
                        onCheckedChange = { viewModel.setAiCapabilityVisible(it) },
                        testTag = "ai_visibility_toggle"
                    )

                    HorizontalDivider(color = Border.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))

                    SettingsToggleItem(
                        icon = Icons.Default.Info,
                        title = "Probability Tooltips",
                        description = "Display immediate victory odds when selecting a cell",
                        checked = showProbabilityTooltips,
                        onCheckedChange = { viewModel.setShowProbabilityTooltips(it) },
                        testTag = "probability_tooltips_toggle"
                    )

                    HorizontalDivider(color = Border.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))

                    SettingsToggleItem(
                        icon = Icons.Default.DoneAll,
                        title = "Confirm Before Commit",
                        description = "Display confirmation step before launching simultaneous turns",
                        checked = confirmBeforeCommit,
                        onCheckedChange = { viewModel.setConfirmBeforeCommit(it) },
                        testTag = "confirm_commit_toggle"
                    )

                    HorizontalDivider(color = Border.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))

                    // Difficulty preset
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = PlayerTeal, modifier = Modifier.size(24.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Difficulty Level", style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                Text("Adjust baseline local neural planner intensity", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DifficultyPreset.values().forEach { preset ->
                                val isSelected = difficultyPreset == preset
                                Button(
                                    onClick = { viewModel.setDifficultyPreset(preset) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) PlayerTeal else Surface,
                                        contentColor = if (isSelected) Color.Black else Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    border = if (!isSelected) BorderStroke(1.dp, Border) else null,
                                    modifier = Modifier.weight(1f).height(38.dp).testTag("difficulty_preset_${preset.name.lowercase()}")
                                ) {
                                    Text(preset.name, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SECTION 4: DISPLAY SYSTEM
                SettingsCategory(title = "DISPLAY SYSTEM") {
                    // Theme row
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = PlayerTeal, modifier = Modifier.size(24.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Aesthetic Theme", style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                Text("Toggle AMOLED black or Dark Slate look", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("dark", "amoled").forEach { themeOption ->
                                val isSelected = displayTheme == themeOption
                                Button(
                                    onClick = { viewModel.setDisplayTheme(themeOption) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) PlayerTeal else Surface,
                                        contentColor = if (isSelected) Color.Black else Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    border = if (!isSelected) BorderStroke(1.dp, Border) else null,
                                    modifier = Modifier.weight(1f).height(38.dp).testTag("theme_btn_$themeOption")
                                ) {
                                    Text(themeOption.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Border.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))

                    // Board size selector
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(imageVector = Icons.Default.GridOn, contentDescription = null, tint = PlayerTeal, modifier = Modifier.size(24.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Display Grid Density", style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                Text("Resize cells for different screen layouts", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("small", "medium", "large").forEach { sizeOption ->
                                val isSelected = boardSize == sizeOption
                                Button(
                                    onClick = { viewModel.setBoardSize(sizeOption) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) PlayerTeal else Surface,
                                        contentColor = if (isSelected) Color.Black else Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    border = if (!isSelected) BorderStroke(1.dp, Border) else null,
                                    modifier = Modifier.weight(1f).height(38.dp).testTag("grid_size_$sizeOption")
                                ) {
                                    Text(sizeOption.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Border.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))

                    SettingsToggleItem(
                        icon = Icons.Default.Accessibility,
                        title = "Reduce Motion",
                        description = "Disable intensive animated viewport scaling effects",
                        checked = reduceMotion,
                        onCheckedChange = { viewModel.setReduceMotion(it) },
                        testTag = "reduce_motion_toggle"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SECTION 5: OPERATIONAL DATA
                SettingsCategory(title = "OPERATIONAL REGISTRY") {
                    // Reset stats
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Purge Combat Logs", style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text("Wipe winrates, win streaks, and match records", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                        Button(
                            onClick = { showResetStatsDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Surface),
                            border = BorderStroke(1.dp, AICrimson.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("reset_stats_button")
                        ) {
                            Text("PURGE", color = AICrimson, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = Border.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))

                    // Reset Progress
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Reset Arena Unlock Progress", style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text("Relock arenas and reset champion milestones", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                        Button(
                            onClick = { showResetProgressDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Surface),
                            border = BorderStroke(1.dp, AICrimson.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("reset_progress_button")
                        ) {
                            Text("RESET", color = AICrimson, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = Border.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))

                    // Clear Cache
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Flush Temporary Assets", style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text("Clear stored vector caches to optimize memory footprint", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                        Button(
                            onClick = { viewModel.clearCache() },
                            colors = ButtonDefaults.buttonColors(containerColor = Surface),
                            border = BorderStroke(1.dp, Border),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("clear_cache_button")
                        ) {
                            Text("FLUSH", color = PlayerTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SECTION 6: SYSTEM ABOUT
                SettingsCategory(title = "SYSTEM ABOUT") {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("SYSTEM DEPLOYMENT", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                            Text("GAMBIT v1.0.0-PROD", style = MaterialTheme.typography.bodyMedium, color = PlayerTeal, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("INTELLIGENCE MODULE", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                            Text("LOCAL NEURAL PLANNER", style = MaterialTheme.typography.bodyMedium, color = GoldAccent, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("ENGINEERING TEAM", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                            Text("AI STUDIO BUILDER", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        }
                        HorizontalDivider(color = Border.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))
                        Text("LEGAL DOCUMENTS", style = MaterialTheme.typography.labelSmall, color = GoldAccent, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Privacy Policy", style = MaterialTheme.typography.bodySmall, color = PlayerTeal, modifier = Modifier.clickable { scope.launch { snackbarHostState.showSnackbar("Opening Privacy Policy...") } })
                            Text("|", style = MaterialTheme.typography.bodySmall, color = Border)
                            Text("Terms of Service", style = MaterialTheme.typography.bodySmall, color = PlayerTeal, modifier = Modifier.clickable { scope.launch { snackbarHostState.showSnackbar("Opening Terms of Service...") } })
                            Text("|", style = MaterialTheme.typography.bodySmall, color = PlayerTeal, modifier = Modifier.clickable { scope.launch { snackbarHostState.showSnackbar("Contacting Tactical Support...") } })
                        }
                    }
                }
            }
        }
    }

    // Edit Name Dialog
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("RE-ASSIGN COMMANDER CALLSIGN", color = PlayerTeal, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column {
                    OutlinedTextField(
                        value = tempDisplayName,
                        onValueChange = { tempDisplayName = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = PlayerTeal,
                            unfocusedBorderColor = Border
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("edit_callsign_field")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Must be 3-20 alphanumeric characters.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateDisplayName(tempDisplayName)
                        showEditNameDialog = false
                    },
                    modifier = Modifier.testTag("confirm_callsign_button")
                ) {
                    Text("CONFIRM", color = PlayerTeal, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("CANCEL", color = TextMuted)
                }
            },
            containerColor = Surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.dp, Border, RoundedCornerShape(16.dp)).testTag("edit_callsign_dialog")
        )
    }

    // Reset Stats Dialog
    if (showResetStatsDialog) {
        AlertDialog(
            onDismissRequest = { showResetStatsDialog = false },
            title = { Text("CONFIRM DESTRUCTION", color = AICrimson, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text("This will permanently erase all local stats, win/loss history, and match records. This action cannot be undone.", color = Color.White) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetStats()
                        showResetStatsDialog = false
                    },
                    modifier = Modifier.testTag("confirm_reset_stats")
                ) {
                    Text("PURGE STATS", color = AICrimson, fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetStatsDialog = false }) {
                    Text("CANCEL", color = PlayerTeal)
                }
            },
            containerColor = Surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.dp, Border, RoundedCornerShape(16.dp)).testTag("reset_stats_dialog")
        )
    }

    // Reset Progress Dialog
    if (showResetProgressDialog) {
        AlertDialog(
            onDismissRequest = { showResetProgressDialog = false },
            title = { Text("CONFIRM ARENA LOCKDOWN", color = AICrimson, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text("This will permanently lock Arena 2 and Arena 3. All best score records and champion milestones will be completely wiped.", color = Color.White) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetProgress()
                        showResetProgressDialog = false
                    },
                    modifier = Modifier.testTag("confirm_reset_progress")
                ) {
                    Text("LOCK ARENAS", color = AICrimson, fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetProgressDialog = false }) {
                    Text("CANCEL", color = PlayerTeal)
                }
            },
            containerColor = Surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.dp, Border, RoundedCornerShape(16.dp)).testTag("reset_progress_dialog")
        )
    }

    // Delete Account Dialog
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("CONFIRM SYSTEM TERMINATION", color = AICrimson, fontWeight = FontWeight.Black, fontSize = 16.sp) },
            text = { Text("WARNING: You are deleting your account. This will completely erase all profile data, stats, achievements, and authentication credentials.", color = Color.White) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAccount(onBackClicked)
                        showDeleteAccountDialog = false
                    },
                    modifier = Modifier.testTag("confirm_delete_account")
                ) {
                    Text("DELETE FOREVER", color = AICrimson, fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("CANCEL", color = PlayerTeal)
                }
            },
            containerColor = Surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.dp, Border, RoundedCornerShape(16.dp)).testTag("delete_account_dialog")
        )
    }
}

@Composable
fun SettingsCategory(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .border(1.dp, Border, RoundedCornerShape(16.dp))
            .padding(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = GoldAccent,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 12.dp)
        )
        Column(content = content)
    }
}

@Composable
fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PlayerTeal,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = PlayerTeal,
                uncheckedThumbColor = Color.LightGray,
                uncheckedTrackColor = Color(0xFF1B1D28)
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

