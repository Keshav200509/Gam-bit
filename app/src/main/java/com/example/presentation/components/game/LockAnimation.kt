package com.example.presentation.components.game

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoldAccent

@Composable
fun LockAnimation(
    isLocked: Boolean,
    modifier: Modifier = Modifier
) {
    // Animates drawing progress of the border when locked
    val borderDrawProgress by animateFloatAsState(
        targetValue = if (isLocked) 1f else 0f,
        animationSpec = if (isLocked) tween(500, easing = LinearEasing) else tween(300),
        label = "lock_border_draw"
    )

    // Bounce animation for the "LOCK" badge
    val badgeScale by animateFloatAsState(
        targetValue = if (isLocked) 1f else 0f,
        animationSpec = if (isLocked) {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        } else {
            tween(200)
        },
        label = "lock_badge_scale"
    )

    val badgeAlpha by animateFloatAsState(
        targetValue = if (isLocked) 1f else 0f,
        animationSpec = tween(if (isLocked) 200 else 150),
        label = "lock_badge_alpha"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Gold border drawing effect around the cell edges
        if (borderDrawProgress > 0f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val pad = 2.dp.toPx()
                val w = size.width - pad * 2
                val h = size.height - pad * 2
                val r = 8.dp.toPx()

                // Create custom path for rounded rectangle
                val path = Path().apply {
                    addRoundRect(
                        androidx.compose.ui.geometry.RoundRect(
                            left = pad,
                            top = pad,
                            right = size.width - pad,
                            bottom = size.height - pad,
                            topLeftCornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
                            topRightCornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
                            bottomLeftCornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
                            bottomRightCornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r)
                        )
                    )
                }

                // Calculate subpath based on borderDrawProgress
                val pathMeasure = PathMeasure()
                pathMeasure.setPath(path, false)
                val length = pathMeasure.length
                val drawPath = Path()
                pathMeasure.getSegment(0f, length * borderDrawProgress, drawPath, true)

                drawPath(
                    path = drawPath,
                    color = GoldAccent,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        // LOCK badge in center
        if (badgeScale > 0f) {
            Box(
                modifier = Modifier
                    .scale(badgeScale)
                    .alpha(badgeAlpha)
                    .background(GoldAccent, shape = RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color(0xFF07080C),
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = "LOCK",
                        color = Color(0xFF07080C),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
