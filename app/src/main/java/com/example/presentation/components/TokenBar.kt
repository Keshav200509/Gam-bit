package com.example.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Background
import com.example.ui.theme.PlayerTeal

@Composable
fun TokenBar(
    selectedToken: Int?,
    usedTokens: Set<Int>,
    onTokenClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (tokenValue in 1..5) {
            val isUsed = usedTokens.contains(tokenValue)
            val isSelected = selectedToken == tokenValue

            TokenSlot(
                value = tokenValue,
                isSelected = isSelected,
                isUsed = isUsed,
                onClick = {
                    if (!isUsed) {
                        onTokenClick(tokenValue)
                    }
                }
            )
        }
    }
}

@Composable
fun TokenSlot(
    value: Int,
    isSelected: Boolean,
    isUsed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scaleFactor by animateFloatAsState(
        targetValue = if (isSelected) 1.12f else 1.0f,
        label = "token_scale"
    )

    val opacity = if (isUsed) 0.25f else 1.0f

    val containerColor = when {
        isSelected -> PlayerTeal
        else -> Color.Transparent
    }

    val textColor = when {
        isSelected -> Background
        else -> PlayerTeal
    }

    val borderStroke = when {
        isSelected -> null
        else -> BorderStroke(1.5.dp, PlayerTeal)
    }

    Box(
        modifier = modifier
            .size(56.dp) // Generous sizing for 48dp minimum touch target
            .scale(scaleFactor)
            .alpha(opacity)
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .then(
                if (borderStroke != null) {
                    Modifier.border(borderStroke, shape = RoundedCornerShape(12.dp))
                } else Modifier
            )
            .then(
                if (!isUsed) {
                    Modifier.clickable(onClick = onClick)
                } else Modifier
            )
            .testTag("token_slot_$value"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value.toString(),
            color = textColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black
        )
    }
}
