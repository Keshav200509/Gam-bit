package com.example.domain.model

sealed class CellModifier {
    object None : CellModifier()
    object GoldenCell : CellModifier() // Worth 2× points
    object Volatile : CellModifier() // 2× at game end, stealable with token 1
    object Battleground : CellModifier() // +1 to defender (3+ clashes)
}
