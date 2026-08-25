package com.example.domain.model

data class Position(
    val row: Int,
    val col: Int
) {
    fun neighbors(): List<Position> {
        val list = mutableListOf<Position>()
        // No diagonals, only direct orthogonal neighbors within 5x5 grid (0 to 4)
        if (row > 0) list.add(Position(row - 1, col))
        if (row < 4) list.add(Position(row + 1, col))
        if (col > 0) list.add(Position(row, col - 1))
        if (col < 4) list.add(Position(row, col + 1))
        return list
    }
}
