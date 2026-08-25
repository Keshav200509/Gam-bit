package com.example.presentation.screens.daily

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firestore.UserProfileRepository
import com.example.data.model.DailyChallenge
import com.example.data.model.DailyChallengeResult
import com.example.data.repository.DailyChallengeRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class DailyChallengeUiState(
    val todayChallenge: DailyChallenge? = null,
    val hasCompleted: Boolean = false,
    val userResult: DailyChallengeResult? = null,
    val leaderboard: List<DailyChallengeResult> = emptyList(),
    val userRank: Int? = null,
    val streak: Int = 0,
    val countdown: String = "00:00:00",
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class DailyChallengeViewModel @Inject constructor(
    private val dailyRepository: DailyChallengeRepository,
    private val profileRepository: UserProfileRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(DailyChallengeUiState())
    val uiState: StateFlow<DailyChallengeUiState> = _uiState.asStateFlow()

    init {
        loadData()
        startCountdownTimer()
    }

    private fun loadData() {
        val uid = auth.currentUser?.uid ?: return
        val today = dailyRepository.getTodayDateString()

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // 1. Get streak
            val streak = dailyRepository.getUserStreak(uid)
            _uiState.update { it.copy(streak = streak) }

            // 2. Observe today's challenge
            dailyRepository.getTodayChallenge()
                .catch { e -> _uiState.update { it.copy(error = e.message) } }
                .onEach { challenge ->
                    _uiState.update { it.copy(todayChallenge = challenge) }
                }
                .launchIn(viewModelScope)

            // 3. Observe user's daily result
            dailyRepository.getUserDailyResult(uid, today)
                .catch { e -> _uiState.update { it.copy(error = e.message) } }
                .onEach { result ->
                    _uiState.update {
                        it.copy(
                            userResult = result,
                            hasCompleted = result != null
                        )
                    }
                }
                .launchIn(viewModelScope)

            // 4. Observe leaderboard
            dailyRepository.getDailyLeaderboard(today)
                .catch { e -> _uiState.update { it.copy(error = e.message) } }
                .onEach { leaderboard ->
                    val userResultInLeaderboard = leaderboard.find { it.uid == uid }
                    _uiState.update {
                        it.copy(
                            leaderboard = leaderboard,
                            userRank = userResultInLeaderboard?.rank,
                            isLoading = false
                        )
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    private fun startCountdownTimer() {
        viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                val now = Calendar.getInstance()
                val tomorrow = Calendar.getInstance().apply {
                    add(Calendar.DATE, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val diff = tomorrow.timeInMillis - now.timeInMillis
                if (diff > 0) {
                    val hours = diff / (3600 * 1000)
                    val minutes = (diff % (3600 * 1000)) / (60 * 1000)
                    val seconds = (diff % (60 * 1000)) / 1000
                    val formatted = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                    _uiState.update { it.copy(countdown = formatted) }
                } else {
                    _uiState.update { it.copy(countdown = "00:00:00") }
                }
                delay(1000L)
            }
        }
    }
}
