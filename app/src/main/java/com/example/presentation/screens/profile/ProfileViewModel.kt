package com.example.presentation.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firestore.GameRecord
import com.example.data.firestore.UserProfileRepository
import com.example.data.firestore.GameHistoryRepository
import com.example.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = false,
    val profile: UserProfile? = null,
    val gameHistory: List<GameRecord> = emptyList(),
    val searchResults: List<UserProfile> = emptyList(),
    val searchedFriend: UserProfile? = null,
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: UserProfileRepository,
    private val historyRepository: GameHistoryRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        val currentUid = auth.currentUser?.uid
        if (currentUid == null) {
            _uiState.update { it.copy(error = "User not logged in") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            profileRepository.getUserProfile(currentUid)
                .catch { t ->
                    _uiState.update { it.copy(isLoading = false, error = t.message) }
                }
                .collect { profile ->
                    if (profile != null) {
                        _uiState.update { it.copy(profile = profile, isLoading = false) }
                        loadGameHistory(profile.uid)
                    } else {
                        // Profile doesn't exist yet, try to create it using Auth details
                        val currentUser = auth.currentUser
                        val dispName = currentUser?.displayName ?: "Gambit Player"
                        val result = profileRepository.createProfile(currentUid, dispName)
                        result.fold(
                            onSuccess = { created ->
                                _uiState.update { it.copy(profile = created, isLoading = false) }
                                loadGameHistory(created.uid)
                            },
                            onFailure = { err ->
                                _uiState.update { it.copy(isLoading = false, error = "Failed to create profile: ${err.message}") }
                            }
                        )
                    }
                }
        }
    }

    private fun loadGameHistory(userId: String) {
        viewModelScope.launch {
            val result = historyRepository.getGameHistory(userId)
            result.fold(
                onSuccess = { history ->
                    _uiState.update { it.copy(gameHistory = history) }
                },
                onFailure = { err ->
                    _uiState.update { it.copy(error = "Failed to load history: ${err.message}") }
                }
            )
        }
    }

    fun updateDisplayName(newName: String) {
        val currentProfile = _uiState.value.profile ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val updated = currentProfile.copy(displayName = newName)
            val result = profileRepository.updateProfile(updated)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(profile = updated, isLoading = false) }
                },
                onFailure = { err ->
                    _uiState.update { it.copy(isLoading = false, error = err.message) }
                }
            )
        }
    }

    fun findFriendByCode(code: String) {
        if (code.trim().length != 6) {
            _uiState.update { it.copy(error = "Friend code must be 6 characters") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, searchedFriend = null) }
            val result = profileRepository.findByFriendCode(code.trim().uppercase())
            result.fold(
                onSuccess = { friend ->
                    if (friend != null) {
                        _uiState.update { it.copy(searchedFriend = friend, isLoading = false) }
                    } else {
                        _uiState.update { it.copy(error = "No player found with that code", isLoading = false) }
                    }
                },
                onFailure = { err ->
                    _uiState.update { it.copy(isLoading = false, error = err.message) }
                }
            )
        }
    }

    fun searchUsers(query: String) {
        if (query.trim().isEmpty()) {
            _uiState.update { it.copy(searchResults = emptyList()) }
            return
        }
        viewModelScope.launch {
            val result = profileRepository.searchUsers(query)
            result.fold(
                onSuccess = { results ->
                    _uiState.update { it.copy(searchResults = results) }
                },
                onFailure = { err ->
                    _uiState.update { it.copy(error = err.message) }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSearchedFriend() {
        _uiState.update { it.copy(searchedFriend = null) }
    }

    fun signOut() {
        auth.signOut()
    }
}
