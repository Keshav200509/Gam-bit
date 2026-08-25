package com.example.domain.usecase

import com.example.domain.model.Board
import com.example.domain.model.Cell
import com.example.domain.model.Position
import java.util.Random
import javax.inject.Inject

class GenerateBoard @Inject constructor(
    private val random: Random
) {
    operator fun invoke(): Board {
        var attempts = 0
        while (attempts < 100) {
            attempts++
            val gridValues = Array(5) { IntArray(5) { 1 } }
            
            // 1. Generate 3-point cluster of size 3
            val cluster3 = mutableSetOf<Position>()
            val start3Row = random.nextInt(5)
            val start3Col = random.nextInt(5)
            val start3 = Position(start3Row, start3Col)
            cluster3.add(start3)
            
            while (cluster3.size < 3) {
                val adjacentCandidates = cluster3.flatMap { it.neighbors() }
                    .filter { it !in cluster3 }
                    .distinct()
                if (adjacentCandidates.isEmpty()) break
                val next = adjacentCandidates[random.nextInt(adjacentCandidates.size)]
                cluster3.add(next)
            }
            
            if (cluster3.size < 3) continue // retry

            // 2. Generate 2-point region of size 5
            val region2 = mutableSetOf<Position>()
            val validStart2Cells = mutableListOf<Position>()
            for (r in 0 until 5) {
                for (c in 0 until 5) {
                    val pos = Position(r, c)
                    if (pos !in cluster3) {
                        validStart2Cells.add(pos)
                    }
                }
            }
            if (validStart2Cells.isEmpty()) continue
            
            val start2 = validStart2Cells[random.nextInt(validStart2Cells.size)]
            region2.add(start2)
            
            var failedToGrowRegion = false
            while (region2.size < 5) {
                val adjacentCandidates = region2.flatMap { it.neighbors() }
                    .filter { it !in cluster3 && it !in region2 }
                    .distinct()
                if (adjacentCandidates.isEmpty()) {
                    failedToGrowRegion = true
                    break
                }
                val next = adjacentCandidates[random.nextInt(adjacentCandidates.size)]
                region2.add(next)
            }
            
            if (failedToGrowRegion || region2.size < 5) continue // retry

            // Apply values
            for (pos in cluster3) {
                gridValues[pos.row][pos.col] = 3
            }
            for (pos in region2) {
                gridValues[pos.row][pos.col] = 2
            }
            
            // 3. Rest are 1-point with scattered 2s (25% chance)
            for (r in 0 until 5) {
                for (c in 0 until 5) {
                    val pos = Position(r, c)
                    if (pos !in cluster3 && pos !in region2) {
                        if (random.nextDouble() < 0.25) {
                            gridValues[r][c] = 2
                        } else {
                            gridValues[r][c] = 1
                        }
                    }
                }
            }
            
            // Build cell objects
            val cellsList = List(5) { r ->
                List(5) { c ->
                    Cell(value = gridValues[r][c])
                }
            }
            return Board(cellsList)
        }
        
        // Final fallback in case of extremely rare failures
        val fallbackCells = List(5) { r ->
            List(5) { c ->
                val value = when {
                    (r == 2 && c in 1..3) -> 3 // 3-point cluster
                    (r == 0) -> 2 // 2-point region
                    else -> 1
                }
                Cell(value = value)
            }
        }
        return Board(fallbackCells)
    }
}
