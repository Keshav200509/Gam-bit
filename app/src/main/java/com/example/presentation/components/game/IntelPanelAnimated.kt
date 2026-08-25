package com.example.presentation.components.game

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.RoundState
import com.example.domain.model.ScoutResult
import com.example.presentation.components.getRegionFromIntel
import com.example.ui.theme.AICrimson
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.IntelPurple
import com.example.ui.theme.PlayerTeal
import com.example.ui.theme.TextPrimary
import kotlinx.coroutines.delay

@Composable
fun IntelPanelAnimated(
    roundState: RoundState,
    modifier: Modifier = Modifier,
    accuracyCount: Int = 0,
    totalCount: Int = 0
) {
    val roundNumber = roundState.roundNumber
    val intelText = roundState.intelHint.ifEmpty { "Satellite scan active." }
    val baseReliability = roundState.intelConfidence.ifEmpty { "UNKNOWN" }

    // State for brief flash on scout corroboration/contradiction
    val flashAlpha = remember { Animatable(0f) }

    // Determining reliability and corroboration state
    var isCorroborated by remember { mutableStateOf<Boolean?>(null) }
    
    val reliability = remember(roundState.scoutedPosition, roundState.scoutResult, roundState.roundNumber, baseReliability) {
        if (roundState.scoutedPosition != null && roundState.scoutResult != null) {
            val scoutRegion = getCellRegion(roundState.scoutedPosition.row, roundState.scoutedPosition.col)
            val intelRegion = getRegionFromIntel(roundState.intel)
            val correct = if (scoutRegion == intelRegion) {
                roundState.scoutResult == ScoutResult.CONTESTED
            } else {
                roundState.scoutResult == ScoutResult.CONTESTED
            }
            isCorroborated = correct
            if (correct) "CONFIRMED" else "CONTRADICTED"
        } else {
            isCorroborated = null
            when {
                baseReliability.contains("HIGH", ignoreCase = true) -> "HIGH RELIABILITY"
                baseReliability.contains("MODERATE", ignoreCase = true) -> "MODERATE"
                baseReliability.contains("LOW", ignoreCase = true) -> "LOW CONFIDENCE"
                baseReliability.contains("SUSPECT", ignoreCase = true) -> "SUSPECT / BLUFF"
                else -> baseReliability
            }
        }
    }

    val labelColor = remember(reliability, isCorroborated) {
        when {
            isCorroborated == true -> PlayerTeal
            isCorroborated == false -> AICrimson
            reliability.contains("HIGH", ignoreCase = true) || reliability.contains("CONFIRMED", ignoreCase = true) -> PlayerTeal
            reliability.contains("MODERATE", ignoreCase = true) -> GoldAccent
            reliability.contains("LOW", ignoreCase = true) || reliability.contains("CONTRADICTED", ignoreCase = true) -> AICrimson
            else -> Color(0xFFE040FB)
        }
    }

    LaunchedEffect(isCorroborated) {
        isCorroborated?.let {
            flashAlpha.animateTo(0.3f, animationSpec = tween(120))
            delay(120)
            flashAlpha.animateTo(0f, animationSpec = tween(200))
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .border(
                BorderStroke(1.dp, IntelPurple.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(8.dp)
            )
            .testTag("intel_compact_panel"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0E101A))
    ) {
        val overlayColor = when (isCorroborated) {
            true -> PlayerTeal.copy(alpha = flashAlpha.value)
            false -> AICrimson.copy(alpha = flashAlpha.value)
            null -> Color.Transparent
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(overlayColor)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Intel Pill Tag
            Box(
                modifier = Modifier
                    .background(IntelPurple.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp))
                    .border(1.dp, IntelPurple.copy(alpha = 0.6f), shape = RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "INTEL",
                    color = IntelPurple,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }

            // Tactical Hint Text
            Text(
                text = formatIntelText(intelText),
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Reliability Status Chip
            Box(
                modifier = Modifier
                    .background(labelColor.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp))
                    .border(1.dp, labelColor.copy(alpha = 0.5f), shape = RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = reliability,
                    color = labelColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
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
