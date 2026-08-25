package com.example.domain

import java.util.Calendar

object DailyChallenge {
    fun getCurrentSeed(): Long {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) // 0-indexed
        val day = cal.get(Calendar.DAY_OF_MONTH)
        // Combine into a deterministic seed
        return year * 10000L + (month + 1) * 100L + day
    }
}
