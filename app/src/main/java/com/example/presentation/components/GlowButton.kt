package com.example.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.SoundManager
import com.example.presentation.GambitHapticFeedback
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SoundAndHapticEntryPoint {
    fun soundManager(): SoundManager
    fun hapticFeedback(): GambitHapticFeedback
}

@Composable
fun GlowButton(
    text: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glowColor: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true
) {
    val context = LocalContext.current.applicationContext
    val entryPoint = remember(context) {
        EntryPointAccessors.fromApplication(context, SoundAndHapticEntryPoint::class.java)
    }
    val soundManager = entryPoint.soundManager()
    val hapticFeedback = entryPoint.hapticFeedback()

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    // Handle press/hover visual states (scale and glow depth)
    val scale by animateFloatAsState(
        targetValue = when {
            !enabled -> 1.0f
            isPressed -> 0.96f
            isHovered -> 1.04f
            else -> 1.0f
        },
        animationSpec = tween(150),
        label = "button_scale"
    )

    val glowAlpha by animateFloatAsState(
        targetValue = when {
            !enabled -> 0.0f
            isPressed -> 0.5f
            isHovered -> 0.4f
            else -> 0.2f
        },
        animationSpec = tween(150),
        label = "button_glow"
    )

    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .drawBehind {
                if (enabled) {
                    // Soft outer glow / shadow drawn underneath
                    for (i in 1..4) {
                        drawRoundRect(
                            color = glowColor.copy(alpha = glowAlpha / (i * 1.5f)),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
                            size = size.copy(
                                width = size.width + (i * 4.dp.toPx()),
                                height = size.height + (i * 4.dp.toPx())
                            ),
                            topLeft = androidx.compose.ui.geometry.Offset(
                                x = -(i * 2.dp.toPx()),
                                y = -(i * 2.dp.toPx())
                            )
                        )
                    }
                }
            }
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = enabled,
                onClick = {
                    soundManager.playMenuClick()
                    hapticFeedback.menuClick()
                    onClick()
                }
            )
            .hoverable(interactionSource = interactionSource, enabled = enabled)
    ) {
        Surface(
            shape = shape,
            color = if (enabled) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.5.dp,
                color = if (enabled) glowColor.copy(alpha = 0.8f) else Color.Gray.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (enabled) glowColor else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else Color.Gray
                )
            }
        }
    }
}
