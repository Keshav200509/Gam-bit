package com.example.presentation.screens.mode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firestore.UserProfileRepository
import com.example.data.model.ArenaProgress
import com.example.data.model.UserProfile
import com.example.domain.model.Arena
import com.example.domain.model.GameLevel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ModeSelectionUiState(
    val userProfile: UserProfile? = null,
    val selectedArena: Arena = Arena.ASCENDENCY,
    val selectedLevel: GameLevel = GameLevel.LEVEL_1,
    val error: String? = null
)

@HiltViewModel
class ModeSelectionViewModel @Inject constructor(
    private val profileRepository: UserProfileRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModeSelectionUiState())
    val uiState: StateFlow<ModeSelectionUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _uiState.update { it.copy(error = "User not authenticated") }
            return
        }

        viewModelScope.launch {
            profileRepository.getUserProfile(uid)
                .catch { e -> _uiState.update { it.copy(error = e.message) } }
                .collect { profile ->
                    _uiState.update { it.copy(userProfile = profile) }
                }
        }
    }

    fun selectArena(arena: Arena) {
        _uiState.update { it.copy(selectedArena = arena, selectedLevel = GameLevel.LEVEL_1) }
    }

    fun selectLevel(level: GameLevel) {
        _uiState.update { it.copy(selectedLevel = level) }
    }
}
