package com.example.presentation.screens.arena

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.PlayerProgressRepository
import com.example.domain.model.PlayerProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArenaMapViewModel @Inject constructor(
    private val playerProgressRepository: PlayerProgressRepository
) : ViewModel() {

    val progress: StateFlow<PlayerProgress> = playerProgressRepository.progress
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PlayerProgress()
        )

    fun resetProgress() {
        viewModelScope.launch {
            playerProgressRepository.resetProgress()
        }
    }
}
