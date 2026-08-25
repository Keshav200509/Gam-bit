package com.example.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.domain.model.Position
import com.example.domain.model.ProbabilityBreakdown
import com.example.ui.theme.AICrimson
import com.example.ui.theme.Border
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.PlayerTeal
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun ProbabilityTooltip(
    position: Position,
    playerToken: Int,
    cellValue: Int,
    playerStrength: Int,
    breakdown: ProbabilityBreakdown,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Popup(
        alignment = Alignment.Center,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(
            focusable = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = modifier
                .width(280.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Border),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Title: "TOKEN X → CELL (row,col) [Npt]"
                Text(
                    text = "TOKEN $playerToken → CELL (${position.row + 1},${position.col + 1}) [${cellValue}pt]",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldAccent,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                // 4 Rows with mini progress bars
                ProbabilityRow(label = "Uncontested", percentage = breakdown.uncontested, color = PlayerTeal)
                ProbabilityRow(label = "Win clash", percentage = breakdown.win, color = PlayerTeal)
                ProbabilityRow(label = "Lose clash", percentage = breakdown.lose, color = AICrimson)
                ProbabilityRow(label = "Tie", percentage = breakdown.tie, color = TextMuted)

                Spacer(modifier = Modifier.height(4.dp))

                // Bottom note
                Text(
                    text = "Your strength: $playerStrength + adjacency. Lock to guarantee uncontested.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ProbabilityRow(
    label: String,
    percentage: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$percentage%",
                fontSize = 12.sp,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
        LinearProgressIndicator(
            progress = { percentage / 100f },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = color,
            trackColor = color.copy(alpha = 0.15f),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}
