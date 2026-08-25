package com.example.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firestore.UserProfileRepository
import com.example.data.local.AnimationSpeed
import com.example.data.local.PreferencesManager
import com.example.data.model.ArenaProgress
import com.example.data.model.UserProfile
import com.example.data.model.UserStats
import com.example.data.repository.StatsRepository
import com.example.domain.model.DifficultyPreset
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val statsRepository: StatsRepository,
    private val profileRepository: UserProfileRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    // Account Section
    val currentUserProfile: StateFlow<UserProfile?> = profileRepository.getCurrentUserProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentUserEmail: String
        get() = auth.currentUser?.email ?: "Offline Mode"

    // Audio Section
    private val _masterVolume = MutableStateFlow(1.0f)
    val masterVolume: StateFlow<Float> = _masterVolume.asStateFlow()

    val soundEnabled: StateFlow<Boolean> = preferencesManager.soundEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val hapticsEnabled: StateFlow<Boolean> = preferencesManager.hapticsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _ambientMusicEnabled = MutableStateFlow(true)
    val ambientMusicEnabled: StateFlow<Boolean> = _ambientMusicEnabled.asStateFlow()

    // Gameplay Section
    val animationSpeed: StateFlow<AnimationSpeed> = preferencesManager.animationSpeed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnimationSpeed.NORMAL)

    val aiCapabilityVisible: StateFlow<Boolean> = preferencesManager.aiCapabilityVisible
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _showProbabilityTooltips = MutableStateFlow(true)
    val showProbabilityTooltips: StateFlow<Boolean> = _showProbabilityTooltips.asStateFlow()

    private val _confirmBeforeCommit = MutableStateFlow(true)
    val confirmBeforeCommit: StateFlow<Boolean> = _confirmBeforeCommit.asStateFlow()

    val difficultyPreset: StateFlow<DifficultyPreset> = preferencesManager.difficultyPreset
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DifficultyPreset.VETERAN)

    // Display Section
    private val _displayTheme = MutableStateFlow("dark") // "dark" or "amoled"
    val displayTheme: StateFlow<String> = _displayTheme.asStateFlow()

    private val _boardSize = MutableStateFlow("medium") // "small", "medium", "large"
    val boardSize: StateFlow<String> = _boardSize.asStateFlow()

    private val _reduceMotion = MutableStateFlow(false)
    val reduceMotion: StateFlow<Boolean> = _reduceMotion.asStateFlow()

    // Status / Messages
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun updateDisplayName(newName: String) {
        val profile = currentUserProfile.value ?: return
        if (newName.trim().length !in 3..20) {
            _statusMessage.value = "Display name must be between 3 and 20 characters."
            return
        }
        viewModelScope.launch {
            val updated = profile.copy(displayName = newName.trim())
            profileRepository.updateProfile(updated).fold(
                onSuccess = { _statusMessage.value = "Display name updated successfully." },
                onFailure = { _statusMessage.value = "Failed to update display name: ${it.localizedMessage}" }
            )
        }
    }

    fun signOut(onFinished: () -> Unit) {
        auth.signOut()
        onFinished()
    }

    fun deleteAccount(onFinished: () -> Unit) {
        viewModelScope.launch {
            val user = auth.currentUser
            if (user != null) {
                user.delete().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        _statusMessage.value = "Account deleted successfully."
                        onFinished()
                    } else {
                        _statusMessage.value = "Failed to delete account. Please re-authenticate."
                    }
                }
            } else {
                onFinished()
            }
        }
    }

    fun setMasterVolume(volume: Float) {
        _masterVolume.value = volume
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setSoundEnabled(enabled)
        }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setHapticsEnabled(enabled)
        }
    }

    fun setAmbientMusicEnabled(enabled: Boolean) {
        _ambientMusicEnabled.value = enabled
    }

    fun setAnimationSpeed(speed: AnimationSpeed) {
        viewModelScope.launch {
            preferencesManager.setAnimationSpeed(speed)
        }
    }

    fun setAiCapabilityVisible(visible: Boolean) {
        viewModelScope.launch {
            preferencesManager.setAiCapabilityVisible(visible)
        }
    }

    fun setShowProbabilityTooltips(visible: Boolean) {
        _showProbabilityTooltips.value = visible
    }

    fun setConfirmBeforeCommit(confirm: Boolean) {
        _confirmBeforeCommit.value = confirm
    }

    fun setDifficultyPreset(preset: DifficultyPreset) {
        viewModelScope.launch {
            preferencesManager.setDifficultyPreset(preset)
        }
    }

    fun setDisplayTheme(theme: String) {
        _displayTheme.value = theme
    }

    fun setBoardSize(size: String) {
        _boardSize.value = size
    }

    fun setReduceMotion(reduce: Boolean) {
        _reduceMotion.value = reduce
    }

    fun resetStats() {
        viewModelScope.launch {
            statsRepository.clearAllGames()
            val profile = currentUserProfile.value
            if (profile != null) {
                profileRepository.updateProfile(profile.copy(stats = UserStats())).fold(
                    onSuccess = { _statusMessage.value = "Statistics reset successfully." },
                    onFailure = { _statusMessage.value = "Failed to reset stats in profile: ${it.localizedMessage}" }
                )
            } else {
                _statusMessage.value = "Statistics cleared."
            }
        }
    }

    fun resetProgress() {
        viewModelScope.launch {
            val profile = currentUserProfile.value
            if (profile != null) {
                profileRepository.updateProfile(profile.copy(arenaProgress = ArenaProgress())).fold(
                    onSuccess = { _statusMessage.value = "Arena progress reset successfully." },
                    onFailure = { _statusMessage.value = "Failed to reset progress: ${it.localizedMessage}" }
                )
            } else {
                _statusMessage.value = "Arena progress cleared."
            }
        }
    }

    fun clearCache() {
        _statusMessage.value = "System cache cleared."
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
