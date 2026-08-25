package com.example.domain.model

enum class IntelType {
    TOKEN_REGION,
    STRATEGY,
    CELL_VALUE
}

sealed interface IntelActualData {
    data class TokenRegion(val tokenValue: Int, val region: String) : IntelActualData
    data class Strategy(val strategyName: String) : IntelActualData
    data class CellValue(val region: String, val cellValue: Int) : IntelActualData
}

data class IntelHint(
    val text: String,
    val isTrue: Boolean,
    val reliabilityLabel: String,
    val type: IntelType,
    val actualData: IntelActualData
)
