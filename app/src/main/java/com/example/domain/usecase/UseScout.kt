package com.example.domain.usecase

import com.example.domain.model.Position
import com.example.domain.model.ScoutResult
import com.example.domain.model.Token
import javax.inject.Inject

class UseScout @Inject constructor() {
    fun execute(position: Position, aiPlacements: Map<Position, Token>): ScoutResult {
        return if (aiPlacements.containsKey(position)) {
            ScoutResult.CONTESTED
        } else {
            ScoutResult.CLEAR
        }
    }
}
