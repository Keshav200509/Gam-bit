package com.example.domain.usecase

import com.example.domain.model.Board
import com.example.domain.model.Cell
import com.example.domain.model.CellModifier
import com.example.domain.model.CellOwner
import com.example.domain.model.Position
import com.example.domain.model.LateGameEventType
import java.util.Random
import javax.inject.Inject

class ApplyLateGameEvents @Inject constructor(
    private val random: Random
) {
    /**
     * Applies the appropriate late-game event based on round number.
     * Round 8: Golden Cell — pick random neutral cell, set modifier
     * Round 10: Volatile — pick random owned cell, set modifier
     * Round 11: Double Stakes — set round multiplier to 2
     * Round 12: Final Gambit — remove all locks (player and AI)
     */
    fun apply(
        board: Board,
        roundNumber: Int,
        playerLocks: List<Position>,
        aiLocks: List<Position>
    ): LateGameEvent {
        return when (roundNumber) {
            8 -> {
                // Find neutral cells
                val neutralCells = mutableListOf<Position>()
                for (r in 0 until 5) {
                    for (c in 0 until 5) {
                        val pos = Position(r, c)
                        if (board.get(pos).owner == CellOwner.NONE) {
                            neutralCells.add(pos)
                        }
                    }
                }
                
                val targetPos = if (neutralCells.isNotEmpty()) {
                    neutralCells[random.nextInt(neutralCells.size)]
                } else {
                    // Fallback to any cell
                    Position(random.nextInt(5), random.nextInt(5))
                }
                
                val originalCell = board.get(targetPos)
                val newCell = originalCell.copy(
                    value = 4,
                    modifier = CellModifier.GoldenCell
                )
                val modifiedBoard = board.set(targetPos, newCell)
                
                LateGameEvent(
                    type = LateGameEventType.GOLDEN_CELL,
                    modifiedBoard = modifiedBoard,
                    modifiedPlayerLocks = playerLocks,
                    modifiedAiLocks = aiLocks,
                    incomeMultiplier = 1,
                    message = "GOLDEN CELL APPEARED at (${targetPos.row},${targetPos.col}) — worth double points"
                )
            }
            10 -> {
                // Find owned cells (player or AI)
                val ownedCells = mutableListOf<Position>()
                for (r in 0 until 5) {
                    for (c in 0 until 5) {
                        val pos = Position(r, c)
                        if (board.get(pos).owner != CellOwner.NONE) {
                            ownedCells.add(pos)
                        }
                    }
                }
                
                val targetPos = if (ownedCells.isNotEmpty()) {
                    ownedCells[random.nextInt(ownedCells.size)]
                } else {
                    // Fallback to any cell
                    Position(random.nextInt(5), random.nextInt(5))
                }
                
                val originalCell = board.get(targetPos)
                val newCell = originalCell.copy(
                    modifier = CellModifier.Volatile
                )
                val modifiedBoard = board.set(targetPos, newCell)
                
                LateGameEvent(
                    type = LateGameEventType.VOLATILE,
                    modifiedBoard = modifiedBoard,
                    modifiedPlayerLocks = playerLocks,
                    modifiedAiLocks = aiLocks,
                    incomeMultiplier = 1,
                    message = "VOLATILE CELL at (${targetPos.row},${targetPos.col}) — 2× points if held at game end"
                )
            }
            11 -> {
                LateGameEvent(
                    type = LateGameEventType.DOUBLE_STAKES,
                    modifiedBoard = board,
                    modifiedPlayerLocks = playerLocks,
                    modifiedAiLocks = aiLocks,
                    incomeMultiplier = 2,
                    message = "DOUBLE STAKES — all income doubled this round"
                )
            }
            12 -> {
                LateGameEvent(
                    type = LateGameEventType.FINAL_GAMBIT,
                    modifiedBoard = board,
                    modifiedPlayerLocks = emptyList(), // Remove all player locks
                    modifiedAiLocks = emptyList(),     // Remove all AI locks
                    incomeMultiplier = 1,
                    message = "FINAL GAMBIT — all locks removed"
                )
            }
            else -> {
                LateGameEvent(
                    type = LateGameEventType.NONE,
                    modifiedBoard = board,
                    modifiedPlayerLocks = playerLocks,
                    modifiedAiLocks = aiLocks,
                    incomeMultiplier = 1,
                    message = ""
                )
            }
        }
    }
}

data class LateGameEvent(
    val type: LateGameEventType,
    val modifiedBoard: Board,
    val modifiedPlayerLocks: List<Position>,
    val modifiedAiLocks: List<Position>,
    val incomeMultiplier: Int,
    val message: String
)


