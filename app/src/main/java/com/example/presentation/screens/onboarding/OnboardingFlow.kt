package com.example.presentation.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firestore.UserProfileRepository
import com.example.data.local.PreferencesManager
import com.google.firebase.auth.FirebaseAuth
import com.example.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val profileRepository: UserProfileRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    val defaultDisplayName: String
        get() {
            val rawName = auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
                ?: auth.currentUser?.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
                ?: "Commander"
            val sanitized = rawName.filter { it.isLetterOrDigit() }
            return if (sanitized.length in 3..20) sanitized else "Commander"
        }

    fun completeOnboarding(displayName: String, avatarName: String, onFinished: () -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            // If offline/no auth, just save local preference and finish
            viewModelScope.launch {
                preferencesManager.setOnboardingCompleted(true)
                onFinished()
            }
            return
        }

        viewModelScope.launch {
            _isSubmitting.value = true
            _error.value = null
            try {
                // Check if profile exists; if not create, if exists update
                val currentProfileResult = profileRepository.createProfile(uid, displayName)
                currentProfileResult.fold(
                    onSuccess = { profile ->
                        val updatedProfile = profile.copy(displayName = displayName, photoUrl = avatarName)
                        profileRepository.updateProfile(updatedProfile)
                        preferencesManager.setOnboardingCompleted(true)
                        onFinished()
                    },
                    onFailure = { e ->
                        _error.value = e.localizedMessage ?: "Failed to set up profile."
                    }
                )
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Failed to set up profile."
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun skipOnboarding(onFinished: () -> Unit) {
        viewModelScope.launch {
            preferencesManager.setOnboardingCompleted(true)
            onFinished()
        }
    }
}

@Composable
fun OnboardingFlow(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    var currentScreen by remember { mutableIntStateOf(0) }
    var displayName by remember { mutableStateOf(viewModel.defaultDisplayName) }
    var selectedAvatar by remember { mutableStateOf("Vanguard") }

    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val error by viewModel.error.collectAsState()

    val avatars = listOf(
        AvatarOption("Vanguard", Icons.Default.Shield, PlayerTeal),
        AvatarOption("Infiltrator", Icons.Default.Visibility, IntelPurple),
        AvatarOption("Sentinel", Icons.Default.Security, GoldAccent),
        AvatarOption("Dreadnought", Icons.Default.FlashOn, AICrimson),
        AvatarOption("Stryker", Icons.Default.TrackChanges, Color.Magenta)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF06070B))
            .padding(24.dp)
            .testTag("onboarding_container"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp)
        ) {
            // Header: Header line and Skip Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GAMBIT // PROTOCOL TRAINING",
                    style = MaterialTheme.typography.labelMedium,
                    color = PlayerTeal,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                if (currentScreen < 4) {
                    TextButton(
                        onClick = { viewModel.skipOnboarding(onFinished) },
                        modifier = Modifier.testTag("skip_button")
                    ) {
                        Text(
                            text = "SKIP",
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }

            // Screen Content Area with beautiful transition
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(
                    targetState = currentScreen,
                    animationSpec = tween(400),
                    label = "screen_transition"
                ) { screen ->
                    when (screen) {
                        0 -> OnboardingScreenWelcome()
                        1 -> OnboardingScreenBoard()
                        2 -> OnboardingScreenFourSystems()
                        3 -> OnboardingScreenThreeArenas()
                        4 -> OnboardingScreenSetup(
                            displayName = displayName,
                            onDisplayNameChange = { displayName = it },
                            selectedAvatar = selectedAvatar,
                            onAvatarSelect = { selectedAvatar = it },
                            avatars = avatars,
                            error = error,
                            isSubmitting = isSubmitting
                        )
                    }
                }
            }

            // Bottom Actions: Indicator dots and Nav buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Slide Progress indicator dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag("progress_dots")
                ) {
                    for (i in 0 until 5) {
                        val isSelected = i == currentScreen
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 24.dp else 8.dp,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "dot_width"
                        )
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(if (isSelected) PlayerTeal else Border)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Bottom Buttons
                if (currentScreen < 4) {
                    Button(
                        onClick = { currentScreen++ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PlayerTeal,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("next_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "NEXT MODULE",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            if (displayName.trim().length in 3..20) {
                                viewModel.completeOnboarding(displayName.trim(), selectedAvatar, onFinished)
                            }
                        },
                        enabled = displayName.trim().length in 3..20 && !isSubmitting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PlayerTeal,
                            contentColor = Color.Black,
                            disabledContainerColor = Border,
                            disabledContentColor = TextMuted
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("begin_button")
                    ) {
                        Text(
                            text = if (isSubmitting) "PROVISIONING..." else "BEGIN SIMULATION",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

// SCREEN 1: WELCOME
@Composable
fun OnboardingScreenWelcome() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "welcome_logo")
        val logoScale by infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )

        Box(
            modifier = Modifier
                .size(160.dp)
                .scale(logoScale),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = PlayerTeal.copy(alpha = 0.1f),
                    radius = size.width / 2
                )
                drawCircle(
                    color = PlayerTeal,
                    radius = size.width / 2,
                    style = Stroke(2.dp.toPx())
                )
            }
            Text(
                text = "G.",
                color = PlayerTeal,
                fontSize = 90.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-4).sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "WELCOME TO GAMBIT",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            letterSpacing = 1.5.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "A CALCULATED-RISK STRATEGY GAME\nFOR COMPETITIVE MINDS",
            style = MaterialTheme.typography.bodyLarge,
            color = PlayerTeal,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Outwit your opponent on a tactical grid where intelligence counters bluffing, and simultaneous decision-making decides victory.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

// SCREEN 2: THE BOARD
@Composable
fun OnboardingScreenBoard() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Grid animation simulation
        Box(
            modifier = Modifier
                .height(200.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(160.dp)) {
                val cellSize = 26.dp.toPx()
                val gap = 4.dp.toPx()
                val startX = (size.width - (cellSize * 5 + gap * 4)) / 2
                val startY = (size.height - (cellSize * 5 + gap * 4)) / 2

                for (r in 0 until 5) {
                    for (c in 0 until 5) {
                        val rx = startX + c * (cellSize + gap)
                        val ry = startY + r * (cellSize + gap)
                        // Accent highlight on some cells
                        val isHighValue = (r == 2 && c == 2) || (r == 1 && c == 3) || (r == 3 && c == 1)
                        val cellColor = if (isHighValue) PlayerTeal.copy(alpha = 0.25f) else Color(0xFF151722)
                        val cellBorder = if (isHighValue) PlayerTeal else Border.copy(alpha = 0.5f)

                        drawRect(
                            color = cellColor,
                            topLeft = Offset(rx, ry),
                            size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
                        )
                        drawRect(
                            color = cellBorder,
                            topLeft = Offset(rx, ry),
                            size = androidx.compose.ui.geometry.Size(cellSize, cellSize),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "THE TACTICAL GRID",
            style = MaterialTheme.typography.titleLarge,
            color = GoldAccent,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "CLAIM CELLS TO ACCUMULATE POINTS",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "The board is a 5×5 grid. Each cell is worth 1, 2, or 3 points. Claim cells by placing your tokens. Every round, owned cells generate points for your score.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

// SCREEN 3: FOUR SYSTEMS
@Composable
fun OnboardingScreenFourSystems() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "FOUR INTEL SYSTEMS",
            style = MaterialTheme.typography.titleLarge,
            color = PlayerTeal,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "REDUCE RANDOMNESS. INCREASE CONTROL.",
            style = MaterialTheme.typography.bodySmall,
            color = GoldAccent,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SystemCard(
                    icon = Icons.Default.Info,
                    title = "INTEL HINT",
                    desc = "A 70% accurate analysis of the opponent's movements.",
                    color = IntelPurple
                )
                SystemCard(
                    icon = Icons.Default.LocationSearching,
                    title = "SCOUT MODE",
                    desc = "Directly scan a single cell for AI presence.",
                    color = PlayerTeal
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SystemCard(
                    icon = Icons.Default.TrendingUp,
                    title = "PROBABILITY",
                    desc = "Hover to see calculated win, tie, or loss odds.",
                    color = Color.Gray
                )
                SystemCard(
                    icon = Icons.Default.Lock,
                    title = "TACTICAL LOCK",
                    desc = "Lock 2 tokens to bypass clashes or add +2 strength.",
                    color = GoldAccent
                )
            }
        }
    }
}

@Composable
fun SystemCard(
    icon: ImageVector,
    title: String,
    desc: String,
    color: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = color,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            lineHeight = 16.sp
        )
    }
}

// SCREEN 4: THREE ARENAS
@Composable
fun OnboardingScreenThreeArenas() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "THREE OPERATIONAL ARENAS",
            style = MaterialTheme.typography.titleLarge,
            color = GoldAccent,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ArenaRowCard(
                title = "ASCENDENCY",
                desc = "The baseline region. Balanced points, structured AI movements.",
                color = PlayerTeal
            )
            ArenaRowCard(
                title = "CONFRONTATION",
                desc = "Intense defensive fortify zones that require higher token strength.",
                color = IntelPurple
            )
            ArenaRowCard(
                title = "OBLIVION",
                desc = "Volatile modifiers, sudden score destructions, chaotic battles.",
                color = AICrimson
            )
        }
    }
}

@Composable
fun ArenaRowCard(
    title: String,
    desc: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = color,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                lineHeight = 16.sp
            )
        }
    }
}

// SCREEN 5: AVATAR SETUP
@Composable
fun OnboardingScreenSetup(
    displayName: String,
    onDisplayNameChange: (String) -> Unit,
    selectedAvatar: String,
    onAvatarSelect: (String) -> Unit,
    avatars: List<AvatarOption>,
    error: String?,
    isSubmitting: Boolean
) {
    val scrollState = rememberScrollState()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "ESTABLISH SYSTEM IDENTITY",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Text input for Display Name
        OutlinedTextField(
            value = displayName,
            onValueChange = onDisplayNameChange,
            label = { Text("COMMANDER CALLSIGN", color = TextMuted) },
            singleLine = true,
            enabled = !isSubmitting,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PlayerTeal,
                unfocusedBorderColor = Border,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = PlayerTeal,
                unfocusedLabelColor = TextMuted
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_name_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Callsign must be 3–20 alphanumeric characters.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "SELECT AVATAR PROTOCOL",
            style = MaterialTheme.typography.labelLarge,
            color = GoldAccent,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Avatar option grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
        ) {
            items(avatars.size) { index ->
                val option = avatars[index]
                val isSelected = selectedAvatar == option.name
                val borderStroke = if (isSelected) BorderStroke(1.5.dp, option.color) else BorderStroke(1.dp, Border)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) option.color.copy(alpha = 0.15f) else Surface)
                        .border(borderStroke.width, borderStroke.brush, RoundedCornerShape(12.dp))
                        .clickable(enabled = !isSubmitting) { onAvatarSelect(option.name) }
                        .padding(8.dp)
                        .testTag("avatar_option_${option.name.lowercase()}")
                ) {
                    Icon(
                        imageVector = option.icon,
                        contentDescription = option.name,
                        tint = if (isSelected) option.color else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = option.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) option.color else Color.White
                    )
                }
            }
        }

        if (error != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error,
                color = AICrimson,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

data class AvatarOption(
    val name: String,
    val icon: ImageVector,
    val color: Color
)
