package com.example.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AICrimson
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.IntelPurple

@Composable
fun AIStateIndicator(
    capabilityName: String,
    capabilityModifier: Int,
    modifier: Modifier = Modifier
) {
    val indicatorColor = when (capabilityName.uppercase()) {
        "PEAK" -> GoldAccent
        "SHARP" -> AICrimson
        "STEADY" -> AICrimson.copy(alpha = 0.7f)
        "SLOPPY" -> IntelPurple
        else -> IntelPurple
    }

    Row(
        modifier = modifier
            .background(Color(0xFF151926), shape = RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFF1F2633), shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(indicatorColor, shape = RoundedCornerShape(50))
                .testTag("ai_indicator_dot")
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "AI: $capabilityName (${if (capabilityModifier >= 0) "+" else ""}$capabilityModifier%)",
            color = indicatorColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.testTag("ai_indicator_text")
        )
    }
}
