package com.example.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firestore.FriendsRepository
import com.example.data.firestore.PvPInviteRepository
import com.example.data.firestore.UserProfileRepository
import com.example.data.repository.DailyChallengeRepository
import com.example.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val userProfile: UserProfile? = null,
    val friendRequestCount: Int = 0,
    val pvpInviteCount: Int = 0,
    val dailyChallengeAvailable: Boolean = true,
    val dailyChallengeCompleted: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val profileRepository: UserProfileRepository,
    private val friendsRepository: FriendsRepository,
    private val inviteRepository: PvPInviteRepository,
    private val dailyChallengeRepository: DailyChallengeRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _uiState.update { it.copy(error = "User not authenticated") }
            return
        }

        // 1. Observe User Profile
        viewModelScope.launch {
            profileRepository.getUserProfile(uid)
                .catch { e -> _uiState.update { it.copy(error = e.message) } }
                .collect { profile ->
                    if (profile != null) {
                        val completed = dailyChallengeRepository.isChallengeCompletedToday(profile.stats.lastDailyChallengeDate)
                        _uiState.update {
                            it.copy(
                                userProfile = profile,
                                dailyChallengeCompleted = completed,
                                dailyChallengeAvailable = !completed
                            )
                        }
                    }
                }
        }

        // 2. Observe Friend Requests (Pending incoming)
        viewModelScope.launch {
            friendsRepository.getPendingRequests(uid)
                .catch { e -> _uiState.update { it.copy(error = e.message) } }
                .collect { list ->
                    _uiState.update { it.copy(friendRequestCount = list.size) }
                }
        }

        // 3. Observe PvP Invites (Pending incoming)
        viewModelScope.launch {
            inviteRepository.getIncomingInvites(uid)
                .catch { e -> _uiState.update { it.copy(error = e.message) } }
                .collect { invites ->
                    _uiState.update { it.copy(pvpInviteCount = invites.size) }
                }
        }
    }
}
