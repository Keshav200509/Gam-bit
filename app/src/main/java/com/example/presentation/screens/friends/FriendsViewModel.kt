package com.example.presentation.screens.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firestore.FriendsRepository
import com.example.data.firestore.PvPInviteRepository
import com.example.data.firestore.UserProfileRepository
import com.example.data.model.Friendship
import com.example.data.model.PvPInvite
import com.example.data.model.UserProfile
import com.example.domain.model.Arena
import com.example.domain.model.GameLevel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FriendsUiState(
    val isLoading: Boolean = false,
    val friends: List<UserProfile> = emptyList(),
    val pendingRequests: List<Friendship> = emptyList(),
    val sentRequests: List<Friendship> = emptyList(),
    val incomingInvites: List<PvPInvite> = emptyList(),
    val outgoingInvites: List<PvPInvite> = emptyList(),
    val searchResults: List<UserProfile> = emptyList(),
    val searchedFriend: UserProfile? = null,
    val error: String? = null,
    val activeMatchId: String? = null,
    val currentUserProfile: UserProfile? = null
)

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val friendsRepository: FriendsRepository,
    private val inviteRepository: PvPInviteRepository,
    private val profileRepository: UserProfileRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    private val jobs = mutableListOf<Job>()

    init {
        startRealtimeListeners()
    }

    fun startRealtimeListeners() {
        val currentUid = auth.currentUser?.uid
        if (currentUid == null) {
            _uiState.update { it.copy(error = "User not logged in") }
            return
        }

        // Clear existing jobs
        jobs.forEach { it.cancel() }
        jobs.clear()

        // 1. Friends list listener
        jobs.add(viewModelScope.launch {
            friendsRepository.getFriendsList(currentUid)
                .catch { t -> _uiState.update { it.copy(error = t.message) } }
                .collect { list ->
                    _uiState.update { it.copy(friends = list) }
                }
        })

        // 2. Pending incoming requests
        jobs.add(viewModelScope.launch {
            friendsRepository.getPendingRequests(currentUid)
                .catch { t -> _uiState.update { it.copy(error = t.message) } }
                .collect { list ->
                    _uiState.update { it.copy(pendingRequests = list) }
                }
        })

        // 3. Sent pending requests
        jobs.add(viewModelScope.launch {
            friendsRepository.getSentRequests(currentUid)
                .catch { t -> _uiState.update { it.copy(error = t.message) } }
                .collect { list ->
                    _uiState.update { it.copy(sentRequests = list) }
                }
        })

        // 4. Incoming PvP invites
        jobs.add(viewModelScope.launch {
            inviteRepository.getIncomingInvites(currentUid)
                .catch { t -> _uiState.update { it.copy(error = t.message) } }
                .collect { list ->
                    _uiState.update { it.copy(incomingInvites = list) }
                }
        })

        // 5. Outgoing PvP invites
        jobs.add(viewModelScope.launch {
            inviteRepository.getOutgoingInvites(currentUid)
                .catch { t -> _uiState.update { it.copy(error = t.message) } }
                .collect { list ->
                    _uiState.update { it.copy(outgoingInvites = list) }
                }
        })

        // 6. Current user profile listener
        jobs.add(viewModelScope.launch {
            profileRepository.getUserProfile(currentUid)
                .catch { t -> _uiState.update { it.copy(error = t.message) } }
                .collect { profile ->
                    _uiState.update { it.copy(currentUserProfile = profile) }
                }
        })

        // Run an initial clean up for old invites
        viewModelScope.launch {
            inviteRepository.expireOldInvites()
        }
    }

    fun searchByFriendCode(code: String) {
        if (code.trim().length != 6) {
            _uiState.update { it.copy(error = "Friend code must be 6 characters") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, searchedFriend = null) }
            val result = friendsRepository.findUserByFriendCode(code.trim().uppercase())
            result.fold(
                onSuccess = { friend ->
                    if (friend != null) {
                        if (friend.uid == auth.currentUser?.uid) {
                            _uiState.update { it.copy(error = "You cannot search for yourself", isLoading = false) }
                        } else {
                            _uiState.update { it.copy(searchedFriend = friend, isLoading = false) }
                        }
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

    fun sendFriendRequest(toUid: String) {
        val currentUid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = friendsRepository.sendFriendRequest(currentUid, toUid)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, searchedFriend = null) }
                },
                onFailure = { err ->
                    _uiState.update { it.copy(isLoading = false, error = err.message) }
                }
            )
        }
    }

    fun acceptRequest(friendshipId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = friendsRepository.acceptFriendRequest(friendshipId)
            result.fold(
                onSuccess = { _uiState.update { it.copy(isLoading = false) } },
                onFailure = { err -> _uiState.update { it.copy(isLoading = false, error = err.message) } }
            )
        }
    }

    fun rejectRequest(friendshipId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = friendsRepository.rejectFriendRequest(friendshipId)
            result.fold(
                onSuccess = { _uiState.update { it.copy(isLoading = false) } },
                onFailure = { err -> _uiState.update { it.copy(isLoading = false, error = err.message) } }
            )
        }
    }

    fun removeFriend(friendUid: String) {
        val currentUid = auth.currentUser?.uid ?: return
        val user1Uid = if (currentUid < friendUid) currentUid else friendUid
        val user2Uid = if (currentUid < friendUid) friendUid else currentUid
        val friendshipId = "${user1Uid}_${user2Uid}"

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = friendsRepository.removeFriend(friendshipId)
            result.fold(
                onSuccess = { _uiState.update { it.copy(isLoading = false) } },
                onFailure = { err -> _uiState.update { it.copy(isLoading = false, error = err.message) } }
            )
        }
    }

    fun sendPvPInvite(toUid: String, toName: String, arena: Arena, level: GameLevel) {
        val currentUid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            profileRepository.getUserProfile(currentUid).first()?.let { myProfile ->
                val result = inviteRepository.sendInvite(
                    fromUid = currentUid,
                    fromName = myProfile.displayName,
                    toUid = toUid,
                    toName = toName,
                    arena = arena,
                    level = level
                )
                result.fold(
                    onSuccess = { _uiState.update { it.copy(isLoading = false) } },
                    onFailure = { err -> _uiState.update { it.copy(isLoading = false, error = err.message) } }
                )
            } ?: run {
                _uiState.update { it.copy(isLoading = false, error = "Failed to fetch user profile") }
            }
        }
    }

    fun acceptPvPInvite(invite: PvPInvite) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = inviteRepository.acceptInvite(invite)
            result.fold(
                onSuccess = { matchId ->
                    _uiState.update { it.copy(isLoading = false, activeMatchId = matchId) }
                },
                onFailure = { err ->
                    _uiState.update { it.copy(isLoading = false, error = err.message) }
                }
            )
        }
    }

    fun rejectPvPInvite(inviteId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = inviteRepository.rejectInvite(inviteId)
            result.fold(
                onSuccess = { _uiState.update { it.copy(isLoading = false) } },
                onFailure = { err -> _uiState.update { it.copy(isLoading = false, error = err.message) } }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSearchedFriend() {
        _uiState.update { it.copy(searchedFriend = null) }
    }

    fun clearActiveMatch() {
        _uiState.update { it.copy(activeMatchId = null) }
    }

    override fun onCleared() {
        super.onCleared()
        jobs.forEach { it.cancel() }
        jobs.clear()
    }
}
