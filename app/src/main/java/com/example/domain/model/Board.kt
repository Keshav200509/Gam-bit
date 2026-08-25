package com.example.domain.model

data class Board(
    val cells: List<List<Cell>> = List(5) { List(5) { Cell(value = 1) } }
) {
    init {
        require(cells.size == 5 && cells.all { it.size == 5 }) {
            "Board must be a 5x5 grid"
        }
    }

    fun get(position: Position): Cell {
        return cells[position.row][position.col]
    }

    fun set(position: Position, cell: Cell): Board {
        val newCells = cells.mapIndexed { r, rowList ->
            if (r == position.row) {
                rowList.mapIndexed { c, existingCell ->
                    if (c == position.col) cell else existingCell
                }
            } else {
                rowList
            }
        }
        return Board(newCells)
    }

    fun neighborsOf(position: Position): List<Position> {
        return position.neighbors()
    }
}
