package com.example.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.RoundState
import com.example.domain.model.IntelHint
import com.example.domain.model.IntelActualData
import com.example.domain.model.ScoutResult
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.IntelPurple
import com.example.ui.theme.PlayerTeal
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun IntelPanel(
    roundState: RoundState,
    modifier: Modifier = Modifier,
    accuracyCount: Int = 0,
    totalCount: Int = 0
) {
    val roundNumber = roundState.roundNumber
    val intelText = roundState.intelHint.ifEmpty { "Feeds currently offline or scan in progress." }
    
    val baseReliability = roundState.intelConfidence.ifEmpty { "Source confidence: UNKNOWN" }
    val reliability = if (roundState.scoutedPosition != null && roundState.scoutResult != null) {
        val scoutRegion = getCellRegion(roundState.scoutedPosition.row, roundState.scoutedPosition.col)
        val intelRegion = getRegionFromIntel(roundState.intel)
        if (scoutRegion == intelRegion) {
            if (roundState.scoutResult == ScoutResult.CONTESTED) {
                "Source confidence: MODERATE (corroborated by scout)"
            } else {
                "Source confidence: LOW (contradicted by scout)"
            }
        } else {
            if (roundState.scoutResult == ScoutResult.CONTESTED) {
                "Source confidence: MODERATE (corroborated by scout)"
            } else {
                "Source confidence: LOW (contradicted by scout)"
            }
        }
    } else {
        baseReliability
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, IntelPurple.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min)
        ) {
            // Left accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(IntelPurple)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "INTEL HINT — ROUND $roundNumber",
                    color = IntelPurple,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Text(
                    text = formatIntelText(intelText),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = reliability,
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    if (totalCount > 0) {
                        Text(
                            text = "Intel accuracy: $accuracyCount/$totalCount this game",
                            color = IntelPurple.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun formatIntelText(text: String): AnnotatedString {
    return buildAnnotatedString {
        val words = text.split(" ")
        words.forEachIndexed { index, word ->
            val cleanWord = word.replace(",", "").replace(".", "").replace("'", "").replace("\"", "")
            val isNumber = cleanWord.toIntOrNull() != null
            val isRegion = cleanWord.lowercase() in listOf(
                "northwest", "northeast", "southwest", "southeast",
                "north", "south", "east", "west", "center"
            )
            val isStrategy = cleanWord.lowercase() in listOf(
                "aggressive", "expansion", "territory", "fortification",
                "stealing", "cells", "securing", "high", "value", "spreading", "thin", "building", "cluster"
            )

            val style = when {
                isNumber -> SpanStyle(color = GoldAccent, fontWeight = FontWeight.Bold)
                isRegion -> SpanStyle(color = PlayerTeal, fontWeight = FontWeight.Bold)
                isStrategy -> SpanStyle(color = IntelPurple, fontWeight = FontWeight.SemiBold)
                else -> SpanStyle(color = TextPrimary)
            }

            withStyle(style) {
                append(word)
            }
            if (index < words.lastIndex) {
                append(" ")
            }
        }
    }
}

fun getRegionFromIntel(intel: IntelHint?): String? {
    if (intel == null) return null
    return when (val data = intel.actualData) {
        is IntelActualData.TokenRegion -> data.region
        is IntelActualData.CellValue -> data.region
        is IntelActualData.Strategy -> {
            val regions = listOf("northwest", "northeast", "southwest", "southeast", "north", "south", "east", "west", "center")
            regions.firstOrNull { intel.text.lowercase().contains(it) }
        }
    }
}

private fun getCellRegion(row: Int, col: Int): String {
    return when {
        row == 2 && col == 2 -> "center"
        row < 2 && col < 2 -> "northwest"
        row < 2 && col > 2 -> "northeast"
        row > 2 && col < 2 -> "southwest"
        row > 2 && col > 2 -> "southeast"
        row < 2 -> "north"
        row > 2 -> "south"
        col < 2 -> "west"
        else -> "east"
    }
}
