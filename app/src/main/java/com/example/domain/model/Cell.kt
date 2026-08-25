package com.example.domain.model

data class Cell(
    val value: Int,
    val owner: CellOwner = CellOwner.NONE,
    val fortify: Int = 0,
    val modifier: CellModifier = CellModifier.None
)
