package com.example.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.GamePhase
import com.example.ui.theme.AICrimson
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.PlayerTeal

@Composable
fun StatusBar(
    phase: GamePhase,
    roundNumber: Int,
    overrideText: String? = null,
    modifier: Modifier = Modifier
) {
    val (baseText, color) = when (phase) {
        is GamePhase.Intel -> Pair(
            "ROUND $roundNumber — INTEL DECRYPTED. REVIEW AND CLICK BEGIN PLACING.",
            PlayerTeal
        )
        is GamePhase.Placing -> Pair(
            "ROUND $roundNumber — PLACE TOKENS, LOCK SAFE ONES (MAX 2), THEN COMMIT.",
            PlayerTeal
        )
        is GamePhase.Revealing -> Pair(
            "ROUND $roundNumber — REVEALING PLANNED COMBAT FORCES...",
            GoldAccent
        )
        is GamePhase.Resolving -> Pair(
            "ROUND $roundNumber — RESOLVING CLASHES SEQUENTIALLY...",
            AICrimson
        )
        is GamePhase.IncomeSummary -> Pair(
            "ROUND $roundNumber — DEPOSITING INCOMES FROM CONTROLLED CELLS...",
            GoldAccent
        )
        is GamePhase.RoundTransition -> Pair(
            "ROUND $roundNumber COMPLETED — SECTOR SECURED. PREPARING NEXT WAVE.",
            PlayerTeal
        )
        is GamePhase.GameOver -> Pair(
            "SIMULATION OVER — CONFLICT TERMINATED. VIEW RESULTS.",
            GoldAccent
        )
        else -> Pair(
            "ROUND $roundNumber — CHOOSE YOUR MOVES WISELY.",
            PlayerTeal
        )
    }

    val text = overrideText ?: baseText
    val animatedColor by animateColorAsState(targetValue = color, label = "status_bar_color")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .background(animatedColor.copy(alpha = 0.08f), shape = RoundedCornerShape(8.dp))
            .border(1.dp, animatedColor.copy(alpha = 0.35f), shape = RoundedCornerShape(8.dp))
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = animatedColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            letterSpacing = 0.5.sp,
            modifier = Modifier.testTag("status_bar_text")
        )
    }
}
